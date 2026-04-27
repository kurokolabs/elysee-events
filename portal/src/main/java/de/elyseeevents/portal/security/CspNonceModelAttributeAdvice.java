package de.elyseeevents.portal.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds {@code cspNonce} to every Spring MVC model so Thymeleaf templates can use
 * {@code th:attr="nonce=${cspNonce}"} directly. The matching nonce is also written into
 * the {@code Content-Security-Policy} header by {@link CspNonceHeaderWriter}; both
 * call the same helper so the values are guaranteed to agree.
 *
 * <p>Why a model attribute and not a request attribute lookup: Spring Security's
 * {@code HeaderWriterFilter} fires on response commit (after the controller has rendered
 * the template), so populating the request attribute exclusively from the writer is too
 * late for template resolution. {@code @ControllerAdvice} runs before the controller, so
 * the nonce is materialised early.</p>
 */
@ControllerAdvice
public class CspNonceModelAttributeAdvice {

    @ModelAttribute(CspNonceHeaderWriter.ATTR_NAME)
    public String cspNonce(HttpServletRequest request) {
        return CspNonceHeaderWriter.getOrCreateNonce(request);
    }
}
