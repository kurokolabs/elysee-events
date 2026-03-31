package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.AuditService;
import de.elyseeevents.portal.service.CustomerService;
import de.elyseeevents.portal.service.EmailService;
import de.elyseeevents.portal.service.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorService twoFactorService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final CustomerService customerService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         TwoFactorService twoFactorService, AuditService auditService,
                         EmailService emailService, CustomerService customerService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorService = twoFactorService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.customerService = customerService;
    }

    @GetMapping("/portal/register")
    public String registerPage(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return "redirect:/portal/dashboard";
        }
        return "auth/register";
    }

    @PostMapping("/portal/register")
    public String register(@RequestParam String firstName,
                          @RequestParam String lastName,
                          @RequestParam(required = false) String company,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          RedirectAttributes redirectAttributes) {
        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim().toLowerCase();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte alle Pflichtfelder ausfüllen.");
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("firstName", firstName);
            redirectAttributes.addFlashAttribute("lastName", lastName);
            redirectAttributes.addFlashAttribute("company", company);
            return "redirect:/portal/register";
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Bitte eine gültige E-Mail-Adresse eingeben.");
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("firstName", firstName);
            redirectAttributes.addFlashAttribute("lastName", lastName);
            redirectAttributes.addFlashAttribute("company", company);
            return "redirect:/portal/register";
        }

        if (password.length() < 8 || password.length() > 128
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            redirectAttributes.addFlashAttribute("error",
                    "Das Passwort muss 8-128 Zeichen, Gross-/Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten.");
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("firstName", firstName);
            redirectAttributes.addFlashAttribute("lastName", lastName);
            redirectAttributes.addFlashAttribute("company", company);
            return "redirect:/portal/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Die Passwörter stimmen nicht überein.");
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("firstName", firstName);
            redirectAttributes.addFlashAttribute("lastName", lastName);
            redirectAttributes.addFlashAttribute("company", company);
            return "redirect:/portal/register";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error",
                    "Registrierung fehlgeschlagen. Bitte prüfen Sie Ihre Eingaben oder melden Sie sich an.");
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("firstName", firstName);
            redirectAttributes.addFlashAttribute("lastName", lastName);
            redirectAttributes.addFlashAttribute("company", company);
            return "redirect:/portal/register";
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("CUSTOMER");
        user.setActive(false);
        user.setForcePwChange(false);
        user.setTwoFaEnabled(true);
        user = userRepository.save(user);

        // Customer-Profil anlegen
        Customer customer = new Customer();
        customer.setUserId(user.getId());
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setCompany(company != null ? company.trim() : null);
        customer.setEmail(email);
        customerService.save(customer);

        String token = java.util.UUID.randomUUID().toString().replace("-", "")
                     + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        userRepository.storeVerificationToken(user.getId(), token);

        String verifyUrl = "https://www.elysee-events.de/portal/verify-email?token=" + token;
        emailService.sendHtmlEmail(email, "Élysée Events - E-Mail bestätigen",
                "email/email-verification", java.util.Map.of("verifyUrl", verifyUrl, "email", email));

        auditService.log("USER_REGISTERED", "user", user.getId(), email);

        return "redirect:/portal/register-success";
    }

    @GetMapping("/portal/register-success")
    public String registerSuccess() {
        return "auth/register-success";
    }

    @GetMapping("/portal/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, HttpSession session,
                              Model model, RedirectAttributes redirectAttributes) {
        if (token == null || token.isBlank() || token.length() < 32) {
            redirectAttributes.addFlashAttribute("error", "Ungültiger Verifizierungslink.");
            return "redirect:/portal/login";
        }
        java.util.Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ungültiger oder abgelaufener Verifizierungslink.");
            return "redirect:/portal/login";
        }
        User user = userOpt.get();
        // Bereits verifiziert?
        if (user.isActive()) {
            redirectAttributes.addFlashAttribute("message", "Ihr Konto ist bereits verifiziert. Bitte melden Sie sich an.");
            return "redirect:/portal/login";
        }
        // Token-Expiry: 24 Stunden nach Erstellung
        if (user.getCreatedAt() != null) {
            try {
                java.time.LocalDateTime created = java.time.LocalDateTime.parse(user.getCreatedAt(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (java.time.LocalDateTime.now().isAfter(created.plusHours(24))) {
                    redirectAttributes.addFlashAttribute("error", "Der Verifizierungslink ist abgelaufen. Bitte registrieren Sie sich erneut.");
                    return "redirect:/portal/register";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Ungueltige Anfrage.");
                return "redirect:/portal/login";
            }
        }
        userRepository.activateUser(user.getId());
        userRepository.updateLastLogin(user.getId());
        auditService.log("EMAIL_VERIFIED", "user", user.getId(), user.getEmail());

        // Nicht auto-einloggen - User muss sich normal anmelden (mit Passwort + 2FA)
        redirectAttributes.addFlashAttribute("message", "E-Mail bestätigt. Sie können sich jetzt anmelden.");
        return "redirect:/portal/login";
    }

    @GetMapping("/portal/login")
    public String loginPage(jakarta.servlet.http.HttpServletResponse response,
                           org.springframework.security.core.Authentication authentication) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/portal/admin";
            }
            return "redirect:/portal/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/portal/2fa")
    public String twoFactorPage(HttpSession session, Model model) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        if (pending == null || !pending) {
            return "redirect:/portal/login";
        }
        String email = (String) session.getAttribute("2fa_email");
        model.addAttribute("email", email);
        return "auth/two-factor";
    }

    @PostMapping("/portal/2fa")
    public String verifyTwoFactor(@RequestParam String code,
                                  HttpSession session,
                                  jakarta.servlet.http.HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        if (pending == null || !pending) {
            return "redirect:/portal/login";
        }

        Long userId = (Long) session.getAttribute("2fa_user_id");
        String role = (String) session.getAttribute("2fa_role");
        Boolean isRegistration = (Boolean) session.getAttribute("2fa_registration");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return "redirect:/portal/login";
        }

        if (!twoFactorService.verifyCode(user, code)) {
            auditService.log("2FA_FAILED", "user", userId, "Falscher Code");
            redirectAttributes.addFlashAttribute("error", "Der Code ist ungültig oder abgelaufen.");
            return "redirect:/portal/2fa";
        }
        auditService.log("2FA_VERIFIED", "user", userId, null);
        twoFactorService.setTrustedDevice(userId, response);

        // 2FA verified - clear pending flags
        session.removeAttribute("2fa_pending");
        session.removeAttribute("2fa_user_id");
        session.removeAttribute("2fa_email");
        session.removeAttribute("2fa_role");
        session.removeAttribute("2fa_registration");

        // If this was a registration, programmatically log in the user
        if (Boolean.TRUE.equals(isRegistration)) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    user.getEmail(), null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            session.setAttribute("SPRING_SECURITY_CONTEXT",
                org.springframework.security.core.context.SecurityContextHolder.getContext());
            userRepository.updateLastLogin(user.getId());
            auditService.log("REGISTRATION_COMPLETE", "user", userId, user.getEmail());
            return "redirect:/portal/dashboard";
        }

        String target = "ADMIN".equals(role) ? "/portal/admin" : "/portal/dashboard";
        return "redirect:" + target;
    }

    @PostMapping("/portal/2fa/resend")
    public String resendCode(HttpSession session, RedirectAttributes redirectAttributes) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        Long userId = (Long) session.getAttribute("2fa_user_id");
        String email = (String) session.getAttribute("2fa_email");
        if (pending == null || !pending || userId == null) {
            return "redirect:/portal/login";
        }

        String code = twoFactorService.generateAndStoreCode(userId);
        emailService.sendTwoFactorCode(email, code);
        redirectAttributes.addFlashAttribute("message", "Neuer Code wurde gesendet.");
        return "redirect:/portal/2fa";
    }

    @GetMapping("/portal/passwort-aendern")
    public String changePasswordPage() {
        return "auth/change-password";
    }

    @PostMapping("/portal/passwort-aendern")
    public String changePassword(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/portal/login";
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/portal/login";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("error", "Aktuelles Passwort ist falsch.");
            return "redirect:/portal/passwort-aendern";
        }

        if (newPassword.length() < 8 || newPassword.length() > 128
                || !newPassword.matches(".*[A-Z].*")
                || !newPassword.matches(".*[a-z].*")
                || !newPassword.matches(".*[0-9].*")
                || !newPassword.matches(".*[^A-Za-z0-9].*")) {
            redirectAttributes.addFlashAttribute("error",
                    "Das Passwort muss 8-128 Zeichen, Gross-/Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten.");
            return "redirect:/portal/passwort-aendern";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Die Passwoerter stimmen nicht ueberein.");
            return "redirect:/portal/passwort-aendern";
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        auditService.log("PASSWORD_CHANGED", "user", user.getId(), null);

        // Clear force_pw_change session flag
        session.removeAttribute("force_pw_change");

        redirectAttributes.addFlashAttribute("message", "Passwort erfolgreich geaendert.");

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/portal/admin";
        }
        return "redirect:/portal/dashboard";
    }
}
