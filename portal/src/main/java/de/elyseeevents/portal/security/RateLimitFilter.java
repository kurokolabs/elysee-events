package de.elyseeevents.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Value("${app.trusted-proxy:127.0.0.1}")
    private String trustedProxy;

    private static final Map<String, String> URL_TO_ACTION = Map.of(
        "/portal/login", "LOGIN",
        "/portal/2fa", "2FA",
        "/portal/2fa/resend", "2FA_RESEND",
        "/portal/passwort-aendern", "PASSWORD",
        "/newsletter/subscribe", "NEWSLETTER"
    );

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String action = URL_TO_ACTION.get(path);

        if (action == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveIp(request);

        if (!rateLimiter.isAllowed(action, ip)) {
            response.setStatus(429);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='5;url=/portal/login'>" +
                "<link rel='stylesheet' href='/portal/css/portal.css'><link rel='stylesheet' href='/portal/css/fonts.css'></head>" +
                "<body><div class='login-page'><div class='login-card' style='text-align:center'>" +
                "<h1 class='login-card__title' style='font-size:20px;margin-bottom:16px'>Zu viele Versuche</h1>" +
                "<p style='color:var(--muted);font-size:14px'>Bitte warten Sie 15 Minuten.<br>Sie werden automatisch weitergeleitet.</p>" +
                "</div></div></body></html>");
            return;
        }

        rateLimiter.recordAttempt(action, ip);
        filterChain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxy.equals(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
