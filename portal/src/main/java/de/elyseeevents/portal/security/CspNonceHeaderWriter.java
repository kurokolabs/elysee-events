package de.elyseeevents.portal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Writes a Content-Security-Policy header that varies by request path:
 *
 * <ul>
 *   <li><b>{@code /portal/**}</b> (Thymeleaf-rendered portal pages): emits a per-request
 *       nonce and a strict policy {@code script-src 'self' 'nonce-XYZ'}. Inline scripts
 *       in templates must reference the nonce via {@code th:attr="nonce=${cspNonce}"}.</li>
 *   <li><b>Static marketing site</b> ({@code /}, {@code /<page>.html}): keeps
 *       {@code 'unsafe-inline'} for {@code script-src} because those files are served as
 *       static HTML and cannot be rewritten with nonces at request time. Migration target:
 *       precompute SHA-256 hashes for each inline block and add them to the CSP at build
 *       time.</li>
 * </ul>
 *
 * The nonce is exposed on the request as attribute {@value #ATTR_NAME} so Thymeleaf can
 * reference it from any template render path.
 */
public class CspNonceHeaderWriter implements HeaderWriter {

    public static final String ATTR_NAME = "cspNonce";
    private static final String HEADER = "Content-Security-Policy";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getEncoder().withoutPadding();

    private static final String COMMON_TAIL = ""
            + "style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: blob:; "
            + "font-src 'self'; "
            + "connect-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'";

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        boolean isPortal = path != null && path.startsWith("/portal");

        String csp;
        if (isPortal) {
            csp = "default-src 'self'; "
                    + "script-src 'self' 'nonce-" + getOrCreateNonce(request) + "'; "
                    + COMMON_TAIL;
        } else {
            // Legacy CSP for the static marketing site. TODO: remove 'unsafe-inline' once
            // all inline scripts in elysee-events/*.html are externalised or hashed.
            csp = "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline'; "
                    + COMMON_TAIL;
        }
        response.setHeader(HEADER, csp);
    }

    /**
     * Reuses a nonce already on the request (forwards/includes share one rendered HTML body)
     * or generates a fresh 128-bit value otherwise. Static so a {@code @ControllerAdvice}
     * can call it before the controller renders the template — the security HeaderWriter
     * fires only on response commit, which is too late for {@code ${cspNonce}} resolution.
     */
    public static String getOrCreateNonce(HttpServletRequest request) {
        String nonce = (String) request.getAttribute(ATTR_NAME);
        if (nonce == null) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            nonce = B64.encodeToString(bytes);
            request.setAttribute(ATTR_NAME, nonce);
        }
        return nonce;
    }
}
