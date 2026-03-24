package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.AuditService;
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

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         TwoFactorService twoFactorService, AuditService auditService,
                         EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorService = twoFactorService;
        this.auditService = auditService;
        this.emailService = emailService;
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
            redirectAttributes.addFlashAttribute("error", "Der Code ist ungueltig oder abgelaufen.");
            return "redirect:/portal/2fa";
        }
        auditService.log("2FA_VERIFIED", "user", userId, null);

        // 2FA verified - clear pending flag
        session.removeAttribute("2fa_pending");
        session.removeAttribute("2fa_user_id");
        session.removeAttribute("2fa_email");
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

        if (newPassword.length() < 8
                || !newPassword.matches(".*[A-Z].*")
                || !newPassword.matches(".*[a-z].*")
                || !newPassword.matches(".*[0-9].*")
                || !newPassword.matches(".*[^A-Za-z0-9].*")) {
            redirectAttributes.addFlashAttribute("error",
                    "Das Passwort muss mind. 8 Zeichen, Gross-/Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten.");
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
