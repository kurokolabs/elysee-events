package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import de.elyseeevents.portal.service.MenuPdfService;
import de.elyseeevents.portal.service.NewsletterService;
import de.elyseeevents.portal.util.BavarianHolidayUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/portal/admin/newsletter")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {

    private final NewsletterRepository newsletterRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final NewsletterService newsletterService;
    private final MenuPdfService menuPdfService;
    private final BavarianHolidayUtil holidayUtil;

    public AdminNewsletterController(NewsletterRepository newsletterRepository,
                                     WeeklyMenuRepository weeklyMenuRepository,
                                     NewsletterService newsletterService,
                                     MenuPdfService menuPdfService,
                                     BavarianHolidayUtil holidayUtil) {
        this.newsletterRepository = newsletterRepository;
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.newsletterService = newsletterService;
        this.menuPdfService = menuPdfService;
        this.holidayUtil = holidayUtil;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("subscribers", newsletterRepository.findAll());
        model.addAttribute("subscriberCount", newsletterRepository.count());
        model.addAttribute("activeCount", newsletterRepository.countActive());
        model.addAttribute("menus", weeklyMenuRepository.findAll());
        return "admin/newsletter";
    }

    @GetMapping("/speisekarte/neu")
    public String newMenuForm(Model model) {
        WeeklyMenu menu = new WeeklyMenu();
        LocalDate nextMonday = holidayUtil.getNextWorkWeekMonday();
        LocalDate nextFriday = nextMonday.plusDays(4);
        menu.setWeekStart(nextMonday.toString());
        menu.setWeekEnd(nextFriday.toString());

        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", menu);
        model.addAttribute("isNew", true);
        model.addAttribute("holidays", holidayUtil.getHolidaysForWeek(nextMonday, nextFriday));
        model.addAttribute("weeks", holidayUtil.getUpcomingWeeks(12));
        return "admin/newsletter-menu-form";
    }

    @PostMapping("/speisekarte/neu")
    public String saveNewMenu(@RequestParam String weekStart, @RequestParam String weekEnd,
                              @RequestParam(required = false) String mondayMeat, @RequestParam(required = false) String mondayMeatPrice,
                              @RequestParam(required = false) String mondayVeg, @RequestParam(required = false) String mondayVegPrice,
                              @RequestParam(required = false) String tuesdayMeat, @RequestParam(required = false) String tuesdayMeatPrice,
                              @RequestParam(required = false) String tuesdayVeg, @RequestParam(required = false) String tuesdayVegPrice,
                              @RequestParam(required = false) String wednesdayMeat, @RequestParam(required = false) String wednesdayMeatPrice,
                              @RequestParam(required = false) String wednesdayVeg, @RequestParam(required = false) String wednesdayVegPrice,
                              @RequestParam(required = false) String thursdayMeat, @RequestParam(required = false) String thursdayMeatPrice,
                              @RequestParam(required = false) String thursdayVeg, @RequestParam(required = false) String thursdayVegPrice,
                              @RequestParam(required = false) String fridayMeat, @RequestParam(required = false) String fridayMeatPrice,
                              @RequestParam(required = false) String fridayVeg, @RequestParam(required = false) String fridayVegPrice,
                              @RequestParam(required = false) String notes,
                              @RequestParam(defaultValue = "ENTWURF") String status,
                              RedirectAttributes redirectAttributes) {
        WeeklyMenu menu = new WeeklyMenu();
        populateMenu(menu, weekStart, weekEnd, mondayMeat, mondayMeatPrice, mondayVeg, mondayVegPrice,
                tuesdayMeat, tuesdayMeatPrice, tuesdayVeg, tuesdayVegPrice,
                wednesdayMeat, wednesdayMeatPrice, wednesdayVeg, wednesdayVegPrice,
                thursdayMeat, thursdayMeatPrice, thursdayVeg, thursdayVegPrice,
                fridayMeat, fridayMeatPrice, fridayVeg, fridayVegPrice, notes, status);
        weeklyMenuRepository.save(menu);

        String msg = "BESTAETIGT".equals(status)
                ? "Speisekarte bestätigt. Newsletter wird am Montag um 09:00 Uhr automatisch versendet."
                : "Entwurf gespeichert.";
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/portal/admin/newsletter/speisekarte/" + menu.getId();
    }

    @GetMapping("/speisekarte/{id}")
    public String menuDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }
        WeeklyMenu menu = menuOpt.get();
        Map<String, String> holidays = computeHolidays(menu);

        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", menu);
        model.addAttribute("holidays", holidays);
        model.addAttribute("activeCount", newsletterRepository.countActive());
        return "admin/newsletter-menu-detail";
    }

    @GetMapping("/speisekarte/{id}/bearbeiten")
    public String editMenuForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }
        WeeklyMenu menu = menuOpt.get();
        Map<String, String> holidays = computeHolidays(menu);

        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", menu);
        model.addAttribute("isNew", false);
        model.addAttribute("holidays", holidays);
        model.addAttribute("weeks", holidayUtil.getUpcomingWeeks(12));
        return "admin/newsletter-menu-form";
    }

    @PostMapping("/speisekarte/{id}")
    public String updateMenu(@PathVariable Long id,
                             @RequestParam String weekStart, @RequestParam String weekEnd,
                             @RequestParam(required = false) String mondayMeat, @RequestParam(required = false) String mondayMeatPrice,
                             @RequestParam(required = false) String mondayVeg, @RequestParam(required = false) String mondayVegPrice,
                             @RequestParam(required = false) String tuesdayMeat, @RequestParam(required = false) String tuesdayMeatPrice,
                             @RequestParam(required = false) String tuesdayVeg, @RequestParam(required = false) String tuesdayVegPrice,
                             @RequestParam(required = false) String wednesdayMeat, @RequestParam(required = false) String wednesdayMeatPrice,
                             @RequestParam(required = false) String wednesdayVeg, @RequestParam(required = false) String wednesdayVegPrice,
                             @RequestParam(required = false) String thursdayMeat, @RequestParam(required = false) String thursdayMeatPrice,
                             @RequestParam(required = false) String thursdayVeg, @RequestParam(required = false) String thursdayVegPrice,
                             @RequestParam(required = false) String fridayMeat, @RequestParam(required = false) String fridayMeatPrice,
                             @RequestParam(required = false) String fridayVeg, @RequestParam(required = false) String fridayVegPrice,
                             @RequestParam(required = false) String notes,
                             @RequestParam(defaultValue = "ENTWURF") String status,
                             RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }

        WeeklyMenu menu = menuOpt.get();
        populateMenu(menu, weekStart, weekEnd, mondayMeat, mondayMeatPrice, mondayVeg, mondayVegPrice,
                tuesdayMeat, tuesdayMeatPrice, tuesdayVeg, tuesdayVegPrice,
                wednesdayMeat, wednesdayMeatPrice, wednesdayVeg, wednesdayVegPrice,
                thursdayMeat, thursdayMeatPrice, thursdayVeg, thursdayVegPrice,
                fridayMeat, fridayMeatPrice, fridayVeg, fridayVegPrice, notes, status);
        weeklyMenuRepository.save(menu);

        String msg = "BESTAETIGT".equals(status)
                ? "Speisekarte bestätigt. Newsletter wird am Montag um 09:00 Uhr automatisch versendet."
                : "Entwurf gespeichert.";
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/portal/admin/newsletter/speisekarte/" + id;
    }

    @PostMapping("/speisekarte/{id}/bestaetigen")
    public String confirmMenu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        weeklyMenuRepository.updateStatus(id, "BESTAETIGT");
        redirectAttributes.addFlashAttribute("message",
                "Speisekarte bestätigt. Newsletter wird am Montag um 09:00 Uhr automatisch versendet.");
        return "redirect:/portal/admin/newsletter/speisekarte/" + id;
    }

    @PostMapping("/speisekarte/{id}/senden")
    public String sendMenu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }

        int sent = newsletterService.sendWeeklyMenuNewsletter(id);
        redirectAttributes.addFlashAttribute("message",
                "Newsletter wurde an " + sent + " Abonnenten versendet.");
        return "redirect:/portal/admin/newsletter/speisekarte/" + id;
    }

    @GetMapping("/speisekarte/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        WeeklyMenu menu = weeklyMenuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Speisekarte nicht gefunden"));
        byte[] pdf = menuPdfService.generate(menu);
        String filename = "Speisekarte_" + menu.getWeekStart() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/speisekarte/{id}/pdf-vorschau")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id) {
        WeeklyMenu menu = weeklyMenuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Speisekarte nicht gefunden"));
        byte[] pdf = menuPdfService.generate(menu);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/speisekarte/dishes")
    @ResponseBody
    public List<String> getDishSuggestions() {
        return weeklyMenuRepository.findDistinctDishes();
    }

    @GetMapping("/speisekarte/feiertage")
    @ResponseBody
    public Map<String, String> getHolidays(@RequestParam String weekStart) {
        LocalDate monday = LocalDate.parse(weekStart);
        return holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
    }

    private Map<String, String> computeHolidays(WeeklyMenu menu) {
        try {
            LocalDate monday = LocalDate.parse(menu.getWeekStart());
            LocalDate friday = LocalDate.parse(menu.getWeekEnd());
            return holidayUtil.getHolidaysForWeek(monday, friday);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void populateMenu(WeeklyMenu menu, String weekStart, String weekEnd,
                              String mondayMeat, String mondayMeatPrice, String mondayVeg, String mondayVegPrice,
                              String tuesdayMeat, String tuesdayMeatPrice, String tuesdayVeg, String tuesdayVegPrice,
                              String wednesdayMeat, String wednesdayMeatPrice, String wednesdayVeg, String wednesdayVegPrice,
                              String thursdayMeat, String thursdayMeatPrice, String thursdayVeg, String thursdayVegPrice,
                              String fridayMeat, String fridayMeatPrice, String fridayVeg, String fridayVegPrice,
                              String notes, String status) {
        menu.setWeekStart(weekStart); menu.setWeekEnd(weekEnd);
        menu.setMondayMeat(mondayMeat); menu.setMondayMeatPrice(mondayMeatPrice);
        menu.setMondayVeg(mondayVeg); menu.setMondayVegPrice(mondayVegPrice);
        menu.setTuesdayMeat(tuesdayMeat); menu.setTuesdayMeatPrice(tuesdayMeatPrice);
        menu.setTuesdayVeg(tuesdayVeg); menu.setTuesdayVegPrice(tuesdayVegPrice);
        menu.setWednesdayMeat(wednesdayMeat); menu.setWednesdayMeatPrice(wednesdayMeatPrice);
        menu.setWednesdayVeg(wednesdayVeg); menu.setWednesdayVegPrice(wednesdayVegPrice);
        menu.setThursdayMeat(thursdayMeat); menu.setThursdayMeatPrice(thursdayMeatPrice);
        menu.setThursdayVeg(thursdayVeg); menu.setThursdayVegPrice(thursdayVegPrice);
        menu.setFridayMeat(fridayMeat); menu.setFridayMeatPrice(fridayMeatPrice);
        menu.setFridayVeg(fridayVeg); menu.setFridayVegPrice(fridayVegPrice);
        menu.setNotes(notes); menu.setStatus(status);
    }
}
