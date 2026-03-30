package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.NewsletterSubscriber;
import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private final NewsletterRepository newsletterRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final EmailService emailService;

    public NewsletterService(NewsletterRepository newsletterRepository,
                             WeeklyMenuRepository weeklyMenuRepository,
                             EmailService emailService) {
        this.newsletterRepository = newsletterRepository;
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.emailService = emailService;
    }

    public int sendWeeklyMenuNewsletter(Long menuId) {
        WeeklyMenu menu = weeklyMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Speisekarte nicht gefunden: " + menuId));

        List<NewsletterSubscriber> subscribers = newsletterRepository.findActive();
        if (subscribers.isEmpty()) {
            log.info("Keine aktiven Abonnenten -- Newsletter wird nicht versendet.");
            return 0;
        }

        String subject = "Speisekarte " + menu.getWeekStart() + " bis " + menu.getWeekEnd();
        int sent = 0;

        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                Map<String, Object> variables = Map.of(
                        "menu", menu,
                        "subscriberName", subscriber.getName() != null ? subscriber.getName() : "",
                        "unsubscribeToken", subscriber.getToken()
                );
                emailService.sendHtmlEmail(subscriber.getEmail(), subject,
                        "email/weekly-menu-newsletter", variables);
                sent++;
            } catch (Exception e) {
                log.error("Newsletter-Versand fehlgeschlagen an {}: {}", subscriber.getEmail(), e.getMessage());
            }
        }

        weeklyMenuRepository.markSent(menuId);
        log.info("Newsletter an {} von {} Abonnenten versendet (Speisekarte ID: {})", sent, subscribers.size(), menuId);
        return sent;
    }
}
