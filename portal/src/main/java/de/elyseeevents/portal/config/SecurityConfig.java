package de.elyseeevents.portal.config;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.security.RateLimiter;
import de.elyseeevents.portal.service.EmailService;
import de.elyseeevents.portal.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
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
                .ignoringRequestMatchers("/newsletter/**", "/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/portal/login", "/portal/register", "/portal/register-success", "/portal/verify-email", "/portal/2fa", "/portal/2fa/resend").permitAll()
                .requestMatchers("/newsletter/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/portal/css/**", "/portal/js/**", "/portal/fonts/**", "/portal/img/**").permitAll()
                .requestMatchers("/portal/admin/**").hasRole("ADMIN")
                .requestMatchers("/portal/**").authenticated()
                .anyRequest().permitAll()
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
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'"
                ))
                .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"))
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) -> {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);

            // Reset rate limiter on successful login
            rateLimiter.reset("LOGIN", request.getRemoteAddr());

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
}
