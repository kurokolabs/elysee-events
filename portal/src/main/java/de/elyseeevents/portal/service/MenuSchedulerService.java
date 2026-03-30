package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class MenuSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(MenuSchedulerService.class);
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final WeeklyMenuRepository weeklyMenuRepository;
    private final NewsletterService newsletterService;

    public MenuSchedulerService(WeeklyMenuRepository weeklyMenuRepository,
                                NewsletterService newsletterService) {
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.newsletterService = newsletterService;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Europe/Berlin")
    public void sendConfirmedMenus() {
        LocalDate today = LocalDate.now(BERLIN);
        LocalDate monday = today.getDayOfWeek() == DayOfWeek.MONDAY
                ? today
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        String weekStart = monday.toString();
        log.info("Scheduler: Suche bestätigte Speisekarten für Woche ab {}", weekStart);

        List<WeeklyMenu> menus = weeklyMenuRepository.findByStatusAndWeekStart("BESTAETIGT", weekStart);

        if (menus.isEmpty()) {
            log.info("Scheduler: Keine bestätigten Speisekarten für diese Woche gefunden.");
            return;
        }

        for (WeeklyMenu menu : menus) {
            try {
                int sent = newsletterService.sendWeeklyMenuNewsletter(menu.getId());
                log.info("Scheduler: Speisekarte {} versendet an {} Abonnenten.", menu.getId(), sent);
            } catch (Exception e) {
                log.error("Scheduler: Fehler beim Versenden von Speisekarte {}: {}", menu.getId(), e.getMessage());
            }
        }
    }
}
