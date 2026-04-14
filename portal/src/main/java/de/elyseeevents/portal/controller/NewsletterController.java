package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.NewsletterSubscriber;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.service.EmailService;
import de.elyseeevents.portal.service.NewsletterService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/newsletter")
public class NewsletterController {

    private static final Logger log = LoggerFactory.getLogger(NewsletterController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private final NewsletterRepository newsletterRepository;
    private final EmailService emailService;
    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterRepository newsletterRepository, EmailService emailService,
                                NewsletterService newsletterService) {
        this.newsletterRepository = newsletterRepository;
        this.emailService = emailService;
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String email,
                            @RequestParam(required = false) String name,
                            Model model) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            model.addAttribute("error", "Bitte geben Sie eine gültige E-Mail-Adresse ein.");
            return "newsletter/subscribed";
        }

        doSubscribe(email.trim().toLowerCase(), name != null ? name.trim() : null);
        model.addAttribute("success", true);
        return "newsletter/subscribed";
    }

    @PostMapping("/api/subscribe")
    @ResponseBody
    public ResponseEntity<?> subscribeApi(@RequestParam String email,
                                          @RequestParam(required = false) String name) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Bitte geben Sie eine gültige E-Mail-Adresse ein."));
        }

        doSubscribe(email.trim().toLowerCase(), name != null ? name.trim() : null);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Sie erhalten ab sofort unsere wöchentliche Speisekarte per E-Mail."));
    }

    private void doSubscribe(String email, String name) {
        Optional<NewsletterSubscriber> existing = newsletterRepository.findByEmail(email);
        if (existing.isPresent()) {
            NewsletterSubscriber s = existing.get();
            if (!s.isActive()) {
                String newToken = UUID.randomUUID().toString();
                newsletterRepository.reactivate(s.getId(), name != null ? name : s.getName(), newToken);
                s.setToken(newToken);
                s.setActive(true);
                sendWelcome(email, name != null ? name : s.getName(), newToken);
                newsletterService.sendCurrentMenuToSubscriber(s);
            }
            return;
        }

        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        subscriber.setName(name);
        subscriber.setActive(true);
        subscriber.setToken(UUID.randomUUID().toString());
        newsletterRepository.save(subscriber);
        sendWelcome(email, name, subscriber.getToken());
        newsletterService.sendCurrentMenuToSubscriber(subscriber);
    }

    private void sendWelcome(String email, String name, String token) {
        try {
            emailService.sendHtmlEmail(email, "Willkommen zum Speisekarten-Newsletter",
                    "email/welcome-subscriber", Map.of(
                            "name", name != null ? name : "",
                            "unsubscribeUrl", "https://www.elysee-events.de/newsletter/abmelden?token=" + token));
        } catch (Exception e) {
            log.error("Welcome-Email an {} fehlgeschlagen: {}", email, e.getMessage());
        }
    }

    @GetMapping("/abmelden")
    public String unsubscribe(@RequestParam String token, Model model) {
        if (token == null || token.length() < 10 || token.length() > 64) {
            model.addAttribute("error", "Ungültiger Abmelde-Link.");
            return "newsletter/unsubscribed";
        }
        Optional<NewsletterSubscriber> subscriber = newsletterRepository.findByToken(token);
        if (subscriber.isPresent()) {
            newsletterRepository.unsubscribe(token);
            model.addAttribute("success", true);
        } else {
            model.addAttribute("error", "Ungültiger Abmelde-Link.");
        }
        return "newsletter/unsubscribed";
    }
}
