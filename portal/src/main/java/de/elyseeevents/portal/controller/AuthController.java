package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.AuditService;
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

    @org.springframework.beans.factory.annotation.Value("${app.emailjs.public-key}")
    private String emailjsPublicKey;

    @org.springframework.beans.factory.annotation.Value("${app.emailjs.service-id}")
    private String emailjsServiceId;

    @org.springframework.beans.factory.annotation.Value("${app.emailjs.template-id}")
    private String emailjsTemplateId;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         TwoFactorService twoFactorService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorService = twoFactorService;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/elysee-events.html";
    }

    @GetMapping("/portal/login")
    public String loginPage(jakarta.servlet.http.HttpServletResponse response,
                           org.springframework.security.core.Authentication authentication) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        // Bereits eingeloggt? Direkt weiterleiten
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
        String code = (String) session.getAttribute("2fa_code");
        boolean demoMode = "YOUR_PUBLIC_KEY".equals(emailjsPublicKey);
        model.addAttribute("email", email);
        if (demoMode) {
            model.addAttribute("code", code);
        }
        model.addAttribute("demoMode", demoMode);
        model.addAttribute("emailjsPublicKey", emailjsPublicKey);
        model.addAttribute("emailjsServiceId", emailjsServiceId);
        model.addAttribute("emailjsTemplateId", emailjsTemplateId);
        return "auth/two-factor";
    }

    @PostMapping("/portal/2fa")
    public String verifyTwoFactor(@RequestParam String code,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        if (pending == null || !pending) {
            return "redirect:/portal/login";
        }

        Long userId = (Long) session.getAttribute("2fa_user_id");
        String role = (String) session.getAttribute("2fa_role");

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

        // 2FA verified - clear pending flag
        session.removeAttribute("2fa_pending");
        session.removeAttribute("2fa_user_id");
        session.removeAttribute("2fa_email");
        session.removeAttribute("2fa_code");
        session.removeAttribute("2fa_role");

        if ("ADMIN".equals(role)) {
            return "redirect:/portal/admin";
        }
        return "redirect:/portal/dashboard";
    }

    @PostMapping("/portal/2fa/resend")
    public String resendCode(HttpSession session, RedirectAttributes redirectAttributes) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        Long userId = (Long) session.getAttribute("2fa_user_id");
        if (pending == null || !pending || userId == null) {
            return "redirect:/portal/login";
        }

        String newCode = twoFactorService.generateAndStoreCode(userId);
        session.setAttribute("2fa_code", newCode);
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

        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Das neue Passwort muss mindestens 8 Zeichen lang sein.");
            return "redirect:/portal/passwort-aendern";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Die Passwörter stimmen nicht überein.");
            return "redirect:/portal/passwort-aendern";
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        auditService.log("PASSWORD_CHANGED", "user", user.getId(), null);
        redirectAttributes.addFlashAttribute("message", "Passwort erfolgreich geändert.");

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/portal/admin";
        }
        return "redirect:/portal/dashboard";
    }
}
