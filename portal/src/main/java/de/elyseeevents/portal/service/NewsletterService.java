package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.NewsletterSubscriber;
import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private final NewsletterRepository newsletterRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final EmailService emailService;
    private final NewsletterTokenHasher tokenHasher;

    public NewsletterService(NewsletterRepository newsletterRepository,
                             WeeklyMenuRepository weeklyMenuRepository,
                             EmailService emailService,
                             NewsletterTokenHasher tokenHasher) {
        this.newsletterRepository = newsletterRepository;
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.emailService = emailService;
        this.tokenHasher = tokenHasher;
    }

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    /**
     * Sends the current week's menu to a single subscriber (Mo-Fr only).
     * On Saturday/Sunday, nothing is sent — the subscriber gets it next Monday via scheduler.
     */
    public void sendCurrentMenuToSubscriber(NewsletterSubscriber subscriber) {
        LocalDate today = LocalDate.now(BERLIN);
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return;
        }

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<WeeklyMenu> menus = weeklyMenuRepository.findByStatusAndWeekStart("VERSENDET", monday.toString());
        if (menus.isEmpty()) {
            menus = weeklyMenuRepository.findByStatusAndWeekStart("BESTAETIGT", monday.toString());
        }
        if (menus.isEmpty()) {
            return;
        }

        if (subscriber.getEmail() == null || subscriber.getEmail().isBlank()) {
            return;
        }

        WeeklyMenu menu = menus.get(0);
        try {
            String subject = "Speisekarte " + menu.getWeekStart() + " bis " + menu.getWeekEnd();
            emailService.sendHtmlEmail(subscriber.getEmail(), subject,
                    "email/weekly-menu-newsletter", Map.of(
                            "menu", menu,
                            "subscriberName", subscriber.getName() != null ? subscriber.getName() : "",
                            "subscriberId", subscriber.getId(),
                            "unsubscribeToken", tokenHasher.tokenFor(subscriber.getId())
                    ));
            log.info("Aktuelle Speisekarte an neuen Abonnenten {} gesendet.", subscriber.getEmail());
        } catch (Exception e) {
            log.error("Speisekarte an neuen Abonnenten {} fehlgeschlagen: {}", subscriber.getEmail(), e.getMessage());
        }
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
                        "subscriberId", subscriber.getId(),
                        "unsubscribeToken", tokenHasher.tokenFor(subscriber.getId())
                );
                emailService.sendHtmlEmail(subscriber.getEmail(), subject,
                        "email/weekly-menu-newsletter", variables);
                sent++;
            } catch (Exception e) {
                log.error("Newsletter-Versand fehlgeschlagen an {}: {}", subscriber.getEmail(), e.getMessage());
            }
        }

        if (sent > 0) {
            weeklyMenuRepository.markSent(menuId);
        }
        log.info("Newsletter an {} von {} Abonnenten versendet (Speisekarte ID: {})", sent, subscribers.size(), menuId);
        return sent;
    }
}
