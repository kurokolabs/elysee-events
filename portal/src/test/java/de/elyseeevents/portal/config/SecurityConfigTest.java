package de.elyseeevents.portal.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
