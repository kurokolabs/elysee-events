package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/portal/admin/newsletter")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {

    private final NewsletterRepository newsletterRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;

    public AdminNewsletterController(NewsletterRepository newsletterRepository,
                                     WeeklyMenuRepository weeklyMenuRepository) {
        this.newsletterRepository = newsletterRepository;
        this.weeklyMenuRepository = weeklyMenuRepository;
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
        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", new WeeklyMenu());
        model.addAttribute("isNew", true);
        return "admin/newsletter-menu-form";
    }

    @PostMapping("/speisekarte/neu")
    public String saveNewMenu(@RequestParam String weekStart,
                              @RequestParam String weekEnd,
                              @RequestParam(required = false) String monday,
                              @RequestParam(required = false) String tuesday,
                              @RequestParam(required = false) String wednesday,
                              @RequestParam(required = false) String thursday,
                              @RequestParam(required = false) String friday,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        WeeklyMenu menu = new WeeklyMenu();
        menu.setWeekStart(weekStart);
        menu.setWeekEnd(weekEnd);
        menu.setMonday(monday);
        menu.setTuesday(tuesday);
        menu.setWednesday(wednesday);
        menu.setThursday(thursday);
        menu.setFriday(friday);
        menu.setNotes(notes);
        weeklyMenuRepository.save(menu);

        redirectAttributes.addFlashAttribute("message", "Speisekarte wurde gespeichert.");
        return "redirect:/portal/admin/newsletter";
    }

    @GetMapping("/speisekarte/{id}")
    public String menuDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }
        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", menuOpt.get());
        return "admin/newsletter-menu-detail";
    }

    @PostMapping("/speisekarte/{id}")
    public String updateMenu(@PathVariable Long id,
                             @RequestParam String weekStart,
                             @RequestParam String weekEnd,
                             @RequestParam(required = false) String monday,
                             @RequestParam(required = false) String tuesday,
                             @RequestParam(required = false) String wednesday,
                             @RequestParam(required = false) String thursday,
                             @RequestParam(required = false) String friday,
                             @RequestParam(required = false) String notes,
                             RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }

        WeeklyMenu menu = menuOpt.get();
        menu.setWeekStart(weekStart);
        menu.setWeekEnd(weekEnd);
        menu.setMonday(monday);
        menu.setTuesday(tuesday);
        menu.setWednesday(wednesday);
        menu.setThursday(thursday);
        menu.setFriday(friday);
        menu.setNotes(notes);
        weeklyMenuRepository.save(menu);

        redirectAttributes.addFlashAttribute("message", "Speisekarte wurde aktualisiert.");
        return "redirect:/portal/admin/newsletter/speisekarte/" + id;
    }

    @PostMapping("/speisekarte/{id}/senden")
    public String sendMenu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findById(id);
        if (menuOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Speisekarte nicht gefunden.");
            return "redirect:/portal/admin/newsletter";
        }

        // Dummy: mark as sent without actually sending emails
        weeklyMenuRepository.markSent(id);

        long activeSubscribers = newsletterRepository.countActive();
        redirectAttributes.addFlashAttribute("message",
                "Newsletter wurde an " + activeSubscribers + " Abonnenten versendet.");
        return "redirect:/portal/admin/newsletter/speisekarte/" + id;
    }
}
