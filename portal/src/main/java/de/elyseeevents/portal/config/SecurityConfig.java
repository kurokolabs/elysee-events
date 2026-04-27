package de.elyseeevents.portal.config;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.security.CspNonceHeaderWriter;
import de.elyseeevents.portal.security.RateLimiter;
import de.elyseeevents.portal.service.EmailService;
import de.elyseeevents.portal.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import de.elyseeevents.portal.security.RateLimitFilter;
import de.elyseeevents.portal.security.TwoFaFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;
    private final RateLimiter rateLimiter;
    private final RateLimitFilter rateLimitFilter;
    private final TwoFaFilter twoFaFilter;
    private final EmailService emailService;
    private final de.elyseeevents.portal.repository.AuditLogRepository auditLogRepository;

    @Value("${app.trusted-proxy:127.0.0.1}")
    private String trustedProxy;

    /**
     * Force HTTPS via {@code requiresChannel().requiresSecure()} when behind a reverse proxy
     * that sets X-Forwarded-Proto. Default false so local dev on plain HTTP still works.
     * Production should set {@code app.security.require-https=true} and
     * {@code server.forward-headers-strategy=native} (already in application-prod.properties).
     */
    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    public SecurityConfig(UserRepository userRepository, TwoFactorService twoFactorService,
                          RateLimiter rateLimiter, RateLimitFilter rateLimitFilter,
                          TwoFaFilter twoFaFilter, EmailService emailService,
                          de.elyseeevents.portal.repository.AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.twoFactorService = twoFactorService;
        this.rateLimitFilter = rateLimitFilter;
        this.twoFaFilter = twoFaFilter;
        this.emailService = emailService;
        this.auditLogRepository = auditLogRepository;
        this.rateLimiter = rateLimiter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(twoFaFilter, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/**", "/newsletter/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/portal/login", "/portal/register", "/portal/register-success", "/portal/verify-email", "/portal/2fa", "/portal/2fa/resend").permitAll()
                .requestMatchers("/newsletter/**").permitAll()
                .requestMatchers("/api/weekly-menu/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/portal/css/**", "/portal/js/**", "/portal/fonts/**", "/portal/img/**").permitAll()
                .requestMatchers("/portal/admin/**").hasRole("ADMIN")
                .requestMatchers("/portal/**").authenticated()
                // Public-facing website pages (WebsiteController)
                .requestMatchers("/", "/kantine", "/hochzeit", "/corporate", "/eventlocation",
                                 "/catering", "/impressum", "/datenschutz", "/agb").permitAll()
                .requestMatchers("/elysee-events.html", "/kantine.html", "/hochzeit.html",
                                 "/corporate.html", "/impressum.html", "/datenschutz.html", "/agb.html").permitAll()
                .requestMatchers("/favicon.ico", "/favicon.svg", "/robots.txt", "/sitemap.xml",
                                 "/manifest.json", "/404.html", "/error").permitAll()
                .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()
                // Actuator hardening: /actuator/health stays public (Docker healthcheck);
                // every other actuator endpoint is denied to prevent reconnaissance.
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                // Deny-by-default: any new endpoint must be explicitly whitelisted above
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/portal/login")
                .loginProcessingUrl("/portal/login")
                .successHandler(loginSuccessHandler())
                .failureUrl("/portal/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/portal/logout")
                .logoutSuccessUrl("/portal/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(sf -> sf.changeSessionId())
                .maximumSessions(2)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // CSP via custom HeaderWriter so we can emit a per-request nonce. Replaces
                // the previous static `script-src 'self' 'unsafe-inline'` (CVE-class XSS
                // amplifier flagged by 2026-04-27 pentest).
                .addHeaderWriter(new CspNonceHeaderWriter())
                .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"))
            );

        if (requireHttps) {
            http.requiresChannel(rc -> rc.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) -> {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);

            // Reset BOTH rate-limit counters on successful login: per-IP (LOGIN) and
            // per-username (LOGIN_USER). The IP key uses the trusted-proxy-aware resolution
            // to match what RateLimitFilter records.
            rateLimiter.reset("LOGIN", resolveIp(request));
            if (email != null) {
                rateLimiter.reset("LOGIN_USER", email.trim().toLowerCase());
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userRepository.updateLastLogin(user.getId());
                auditLogRepository.log(user.getId(), "LOGIN_SUCCESS", "user", user.getId(), null);

                if (user.isForcePwChange()) {
                    request.getSession().setAttribute("force_pw_change", true);
                    response.sendRedirect("/portal/passwort-aendern");
                    return;
                }

                if (user.isTwoFaEnabled()) {
                    if (twoFactorService.isDeviceTrusted(user.getId(), request)) {
                        auditLogRepository.log(user.getId(), "2FA_SKIPPED_TRUSTED", "user", user.getId(), null);
                    } else {
                        String code = twoFactorService.generateAndStoreCode(user.getId());
                        emailService.sendTwoFactorCode(user.getEmail(), code);
                        request.getSession().setAttribute("2fa_pending", true);
                        request.getSession().setAttribute("2fa_user_id", user.getId());
                        request.getSession().setAttribute("2fa_email", user.getEmail());
                        request.getSession().setAttribute("2fa_role", user.getRole());
                        response.sendRedirect("/portal/2fa");
                        return;
                    }
                }
            }

            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                response.sendRedirect("/portal/admin");
            } else {
                response.sendRedirect("/portal/dashboard");
            }
        };
    }

    private String resolveIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxy.equals(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] ips = forwarded.split(",");
                return ips[ips.length - 1].trim();
            }
        }
        return remoteAddr;
    }
}
