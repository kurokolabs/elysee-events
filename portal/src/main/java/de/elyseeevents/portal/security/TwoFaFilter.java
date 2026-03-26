package de.elyseeevents.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
public class TwoFaFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_DURING_2FA = Set.of(
            "/portal/2fa",
            "/portal/2fa/resend",
            "/portal/logout",
            "/portal/login",
            "/portal/register",
            "/portal/register-success",
            "/portal/verify-email"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Normalisiere den Pfad um Path-Traversal zu verhindern
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        // Entferne doppelte Slashes und Path-Parameter
        path = path.replaceAll("//+", "/").replaceAll(";.*", "");

        if (!path.startsWith("/portal/") || path.startsWith("/portal/css/")
                || path.startsWith("/portal/js/") || path.startsWith("/portal/fonts/")
                || path.startsWith("/portal/img/")) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Boolean pending = (Boolean) session.getAttribute("2fa_pending");
            if (Boolean.TRUE.equals(pending) && !ALLOWED_DURING_2FA.contains(path)) {
                response.sendRedirect("/portal/2fa");
                return;
            }

            Boolean forcePw = (Boolean) session.getAttribute("force_pw_change");
            if (Boolean.TRUE.equals(forcePw) && !"/portal/passwort-aendern".equals(path)
                    && !"/portal/logout".equals(path)) {
                response.sendRedirect("/portal/passwort-aendern");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
