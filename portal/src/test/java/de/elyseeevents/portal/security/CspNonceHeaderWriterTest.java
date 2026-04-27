package de.elyseeevents.portal.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CspNonceHeaderWriterTest {

    private final CspNonceHeaderWriter writer = new CspNonceHeaderWriter();

    @Test
    void portalPathGetsNonceCsp() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/portal/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        writer.writeHeaders(req, res);

        String csp = res.getHeader("Content-Security-Policy");
        assertNotNull(csp, "CSP header must be present");
        assertTrue(csp.contains("script-src 'self' 'nonce-"),
                "portal CSP must use nonce: " + csp);
        assertFalse(csp.contains("'unsafe-inline'") && csp.contains("script-src 'self' 'unsafe-inline'"),
                "portal CSP must NOT have script-src 'unsafe-inline': " + csp);

        Object nonce = req.getAttribute(CspNonceHeaderWriter.ATTR_NAME);
        assertNotNull(nonce, "nonce attribute must be set on request");
        assertTrue(((String) nonce).length() >= 16,
                "nonce must have >= 128 bits of base64 entropy");
        assertTrue(csp.contains("'nonce-" + nonce + "'"),
                "header nonce must match request-attribute nonce");
    }

    @Test
    void staticSitePathKeepsLegacyCsp() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/elysee-events.html");
        MockHttpServletResponse res = new MockHttpServletResponse();

        writer.writeHeaders(req, res);

        String csp = res.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline'"),
                "static site CSP keeps unsafe-inline (no nonces possible): " + csp);
        assertFalse(csp.contains("'nonce-"),
                "static site CSP must not contain a nonce: " + csp);
    }

    @Test
    void rootPathTreatedAsStatic() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse res = new MockHttpServletResponse();

        writer.writeHeaders(req, res);

        String csp = res.getHeader("Content-Security-Policy");
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline'"),
                "root path is the static landing — keeps legacy CSP: " + csp);
    }

    @Test
    void noncesAreFreshPerRequest() {
        MockHttpServletRequest a = new MockHttpServletRequest("GET", "/portal/login");
        MockHttpServletRequest b = new MockHttpServletRequest("GET", "/portal/login");
        writer.writeHeaders(a, new MockHttpServletResponse());
        writer.writeHeaders(b, new MockHttpServletResponse());
        assertNotEquals(a.getAttribute(CspNonceHeaderWriter.ATTR_NAME),
                b.getAttribute(CspNonceHeaderWriter.ATTR_NAME),
                "two independent requests must get different nonces");
    }

    @Test
    void noncesAreStableWithinASingleRequest() {
        // Forwards / includes can call the writer twice on the same request — the nonce
        // must stay constant so the header in the response matches every nonce already
        // baked into rendered template fragments.
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/portal/dashboard");
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        writer.writeHeaders(req, first);
        Object firstNonce = req.getAttribute(CspNonceHeaderWriter.ATTR_NAME);

        writer.writeHeaders(req, second);
        Object secondNonce = req.getAttribute(CspNonceHeaderWriter.ATTR_NAME);

        assertEquals(firstNonce, secondNonce, "same-request reuse must yield the same nonce");
        assertTrue(second.getHeader("Content-Security-Policy").contains("'nonce-" + firstNonce + "'"));
    }

    @Test
    void noUnsafeInlineForScriptOnPortalPath() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/portal/admin/dashboard");
        MockHttpServletResponse res = new MockHttpServletResponse();
        writer.writeHeaders(req, res);

        String csp = res.getHeader("Content-Security-Policy");
        // Pull out just the script-src directive to be defensive against unrelated
        // 'unsafe-inline' for style-src that we still allow.
        int idx = csp.indexOf("script-src");
        int end = csp.indexOf(";", idx);
        String scriptSrc = csp.substring(idx, end);
        assertFalse(scriptSrc.contains("'unsafe-inline'"),
                "script-src directive must not contain unsafe-inline on portal paths: " + scriptSrc);
    }
}
