package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.NewsletterRepository;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import de.elyseeevents.portal.service.NewsletterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/portal/admin/newsletter")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {

    private final NewsletterRepository newsletterRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final NewsletterService newsletterService;

    public AdminNewsletterController(NewsletterRepository newsletterRepository,
                                     WeeklyMenuRepository weeklyMenuRepository,
                                     NewsletterService newsletterService) {
        this.newsletterRepository = newsletterRepository;
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.newsletterService = newsletterService;
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
                              @RequestParam(required = false) String mondayMeat,
                              @RequestParam(required = false) String mondayVeg,
                              @RequestParam(required = false) String tuesdayMeat,
                              @RequestParam(required = false) String tuesdayVeg,
                              @RequestParam(required = false) String wednesdayMeat,
                              @RequestParam(required = false) String wednesdayVeg,
                              @RequestParam(required = false) String thursdayMeat,
                              @RequestParam(required = false) String thursdayVeg,
                              @RequestParam(required = false) String fridayMeat,
                              @RequestParam(required = false) String fridayVeg,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        WeeklyMenu menu = new WeeklyMenu();
        menu.setWeekStart(weekStart);
        menu.setWeekEnd(weekEnd);
        menu.setMondayMeat(mondayMeat);
        menu.setMondayVeg(mondayVeg);
        menu.setTuesdayMeat(tuesdayMeat);
        menu.setTuesdayVeg(tuesdayVeg);
        menu.setWednesdayMeat(wednesdayMeat);
        menu.setWednesdayVeg(wednesdayVeg);
        menu.setThursdayMeat(thursdayMeat);
        menu.setThursdayVeg(thursdayVeg);
        menu.setFridayMeat(fridayMeat);
        menu.setFridayVeg(fridayVeg);
        menu.setNotes(notes);
        weeklyMenuRepository.save(menu);

        redirectAttributes.addFlashAttribute("message",
                "Speisekarte wurde gespeichert. Bitte prüfen Sie die Vorschau und versenden Sie den Newsletter.");
        return "redirect:/portal/admin/newsletter/speisekarte/" + menu.getId();
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
        model.addAttribute("activeNav", "newsletter");
        model.addAttribute("menu", menuOpt.get());
        model.addAttribute("isNew", false);
        return "admin/newsletter-menu-form";
    }

    @PostMapping("/speisekarte/{id}")
    public String updateMenu(@PathVariable Long id,
                             @RequestParam String weekStart,
                             @RequestParam String weekEnd,
                             @RequestParam(required = false) String mondayMeat,
                             @RequestParam(required = false) String mondayVeg,
                             @RequestParam(required = false) String tuesdayMeat,
                             @RequestParam(required = false) String tuesdayVeg,
                             @RequestParam(required = false) String wednesdayMeat,
                             @RequestParam(required = false) String wednesdayVeg,
                             @RequestParam(required = false) String thursdayMeat,
                             @RequestParam(required = false) String thursdayVeg,
                             @RequestParam(required = false) String fridayMeat,
                             @RequestParam(required = false) String fridayVeg,
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
        menu.setMondayMeat(mondayMeat);
        menu.setMondayVeg(mondayVeg);
        menu.setTuesdayMeat(tuesdayMeat);
        menu.setTuesdayVeg(tuesdayVeg);
        menu.setWednesdayMeat(wednesdayMeat);
        menu.setWednesdayVeg(wednesdayVeg);
        menu.setThursdayMeat(thursdayMeat);
        menu.setThursdayVeg(thursdayVeg);
        menu.setFridayMeat(fridayMeat);
        menu.setFridayVeg(fridayVeg);
        menu.setNotes(notes);
        weeklyMenuRepository.save(menu);

        redirectAttributes.addFlashAttribute("message",
                "Speisekarte wurde aktualisiert. Bitte prüfen Sie die Vorschau und versenden Sie den Newsletter.");
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

    @GetMapping("/speisekarte/dishes")
    @ResponseBody
    public List<String> getDishSuggestions() {
        return weeklyMenuRepository.findDistinctDishes();
    }
}
