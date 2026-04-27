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

    /**
     * Per-account login limit. Stricter than the per-IP limit so a botnet distributing 5
     * attempts across N IPs cannot escalate to 5*N attempts against a single account.
     */
    public static final int LOGIN_PER_USER_MAX = 10;

    private static final Map<String, String> URL_TO_ACTION = Map.of(
        "/portal/login", "LOGIN",
        "/portal/register", "REGISTER",
        "/portal/2fa", "2FA",
        "/portal/2fa/resend", "2FA_RESEND",
        "/portal/passwort-aendern", "PASSWORD",
        "/newsletter/subscribe", "NEWSLETTER",
        "/newsletter/api/subscribe", "NEWSLETTER"
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

        if (!rateLimiter.tryAcquire(action, ip)) {
            writeRateLimited(response);
            return;
        }

        // Defense against distributed brute force: per-IP rate limit (5/15min) is bypassable
        // with N IPs (5*N attempts/window). Add a per-username limit for LOGIN so a single
        // account cannot be hammered no matter how many source IPs are used.
        if ("LOGIN".equals(action)) {
            String username = request.getParameter("username");
            if (username != null) {
                String normalized = username.trim().toLowerCase();
                if (!normalized.isEmpty()
                        && !rateLimiter.tryAcquire("LOGIN_USER", normalized, LOGIN_PER_USER_MAX)) {
                    writeRateLimited(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='5;url=/portal/login'>" +
            "<link rel='stylesheet' href='/portal/css/portal.css'><link rel='stylesheet' href='/portal/css/fonts.css'></head>" +
            "<body><div class='login-page'><div class='login-card' style='text-align:center'>" +
            "<h1 class='login-card__title' style='font-size:20px;margin-bottom:16px'>Zu viele Versuche</h1>" +
            "<p style='color:var(--muted);font-size:14px'>Bitte warten Sie 15 Minuten.<br>Sie werden automatisch weitergeleitet.</p>" +
            "</div></div></body></html>");
    }

    private String resolveIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxy.equals(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Use the LAST IP in the chain — the one the trusted proxy appended.
                // The first IP is attacker-controlled and can be spoofed to bypass rate limiting.
                String[] ips = forwarded.split(",");
                return ips[ips.length - 1].trim();
            }
        }
        return remoteAddr;
    }
}
