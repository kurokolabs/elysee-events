package de.elyseeevents.portal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class TwoFactorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip filter for non-portal paths, static assets, login, 2fa, and logout
        if (!path.startsWith("/portal/") ||
            path.startsWith("/portal/css/") ||
            path.startsWith("/portal/js/") ||
            path.startsWith("/portal/fonts/") ||
            path.startsWith("/portal/img/") ||
            path.equals("/portal/login") ||
            path.equals("/portal/2fa") ||
            path.equals("/portal/2fa/resend") ||
            path.equals("/portal/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Boolean pending = (Boolean) session.getAttribute("2fa_pending");
            if (pending != null && pending) {
                response.sendRedirect("/portal/2fa");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
