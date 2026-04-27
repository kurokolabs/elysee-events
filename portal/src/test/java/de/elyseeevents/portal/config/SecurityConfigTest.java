package de.elyseeevents.portal.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/portal/login"))
                .andExpect(status().isOk());
    }

    @Test
    void staticAssetsArePublic() throws Exception {
        // Static asset paths should not require auth (even if resource is missing, should not redirect to login)
        mockMvc.perform(get("/portal/css/test.css"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/portal/js/test.js"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/portal/fonts/test.woff2"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/portal/img/test.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardRedirectsToLoginWhenAnonymous() throws Exception {
        mockMvc.perform(get("/portal/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/portal/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminRoutesReturn403ForCustomerRole() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/buchungen"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/kunden"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/rechnungen"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoutesReturn200ForAdminRole() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/portal/admin/buchungen"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/portal/admin/kunden"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/portal/admin/rechnungen"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void securityHeadersPresent() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    // ── Pentest 2026-04-27 — Hardening: actuator exposure ─────────────────────

    @Test
    void actuatorHealthRemainsPublicForDockerHealthcheck() throws Exception {
        // Required by docker-compose healthcheck (`wget /actuator/health`). Anonymous
        // access must keep working. Note: Spring's actuator may report DOWN under tests,
        // but the response must NOT be a redirect-to-login or 401/403.
        MvcResult res = mockMvc.perform(get("/actuator/health")).andReturn();
        int status = res.getResponse().getStatus();
        assertTrue(status == 200 || status == 503,
                "/actuator/health must respond 2xx/5xx (not auth-redirected). Got: " + status);
    }

    @Test
    void otherActuatorEndpointsAreDeniedAnonymously() throws Exception {
        // Even if `management.endpoints.web.exposure.include` later widens, the
        // SecurityConfig denyAll for /actuator/** (except health) must keep them locked.
        // Anonymous denial under formLogin redirects to the login page (302) — that's
        // sufficient denial for our purposes; the actuator data is never disclosed.
        for (String path : new String[]{"/actuator/info", "/actuator/metrics",
                                        "/actuator/env", "/actuator/beans"}) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertTrue(status == 302 || status == 401 || status == 403,
                    path + " must deny anonymous access (got " + status + ")");
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actuatorEndpointsAreBlockedEvenForAdmin() throws Exception {
        // Defense-in-depth: even an authenticated admin should not be able to reach the
        // sensitive actuator endpoints over the application port. If management endpoints
        // are ever needed, expose them on a separate management port via
        // `management.server.port`.
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    // ── Pentest 2026-04-27 — Hardening: CSP nonce on portal pages ─────────────

    @Test
    void portalCspUsesNonceNoUnsafeInline() throws Exception {
        MvcResult res = mockMvc.perform(get("/portal/login")).andReturn();
        String csp = res.getResponse().getHeader("Content-Security-Policy");
        assertNotNull(csp, "CSP header must be present on portal page");
        assertTrue(csp.contains("'nonce-"), "portal CSP must include a nonce: " + csp);
        // script-src must not have unsafe-inline (use a substring around script-src).
        int idx = csp.indexOf("script-src");
        int end = csp.indexOf(";", idx);
        String scriptSrc = csp.substring(idx, end);
        assertFalse(scriptSrc.contains("'unsafe-inline'"),
                "script-src on portal page must NOT include unsafe-inline: " + scriptSrc);
    }

    @Test
    void cspNonceVariesPerRequest() throws Exception {
        String csp1 = mockMvc.perform(get("/portal/login")).andReturn().getResponse().getHeader("Content-Security-Policy");
        String csp2 = mockMvc.perform(get("/portal/login")).andReturn().getResponse().getHeader("Content-Security-Policy");
        assertNotNull(csp1);
        assertNotNull(csp2);
        // Extract the nonce token from each header for direct comparison.
        String n1 = csp1.replaceAll("(?s).*'nonce-([^']+)'.*", "$1");
        String n2 = csp2.replaceAll("(?s).*'nonce-([^']+)'.*", "$1");
        assertNotEquals(n1, n2, "two requests must produce different CSP nonces");
    }
}
