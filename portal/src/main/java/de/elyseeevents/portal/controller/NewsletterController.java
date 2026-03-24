package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.NewsletterSubscriber;
import de.elyseeevents.portal.repository.NewsletterRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/newsletter")
public class NewsletterController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private final NewsletterRepository newsletterRepository;

    public NewsletterController(NewsletterRepository newsletterRepository) {
        this.newsletterRepository = newsletterRepository;
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String email,
                            @RequestParam(required = false) String name,
                            Model model) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            model.addAttribute("error", "Bitte geben Sie eine gültige E-Mail-Adresse ein.");
            return "newsletter/subscribed";
        }

        String trimmedEmail = email.trim().toLowerCase();

        Optional<NewsletterSubscriber> existing = newsletterRepository.findByEmail(trimmedEmail);
        if (existing.isPresent()) {
            NewsletterSubscriber s = existing.get();
            if (!s.isActive()) {
                String newToken = UUID.randomUUID().toString();
                String newName = name != null ? name.trim() : s.getName();
                newsletterRepository.reactivate(s.getId(), newName, newToken);
            }
            // If already active, just show success without duplicate insert
            model.addAttribute("success", true);
            return "newsletter/subscribed";
        }

        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(trimmedEmail);
        subscriber.setName(name != null ? name.trim() : null);
        subscriber.setActive(true);
        subscriber.setToken(UUID.randomUUID().toString());
        newsletterRepository.save(subscriber);

        model.addAttribute("success", true);
        return "newsletter/subscribed";
    }

    @GetMapping("/abmelden")
    public String unsubscribe(@RequestParam String token, Model model) {
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
