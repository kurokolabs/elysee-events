package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailPreviewController {

    @GetMapping("/email-vorlagen")
    public String list(Model model) {
        model.addAttribute("activeNav", "email");
        return "admin/email-templates";
    }

    @GetMapping("/email-preview/{template}")
    public String preview(@PathVariable String template, Model model) {
        // Dummy data for preview
        switch (template) {
            case "weekly-menu" -> {
                WeeklyMenu menu = new WeeklyMenu();
                menu.setWeekStart("24.03.2026");
                menu.setWeekEnd("28.03.2026");
                menu.setMonday("Haehnchenbrust mit Kartoffelgratin, Beilagensalat");
                menu.setTuesday("Spaghetti Bolognese, Parmesan, gemischter Salat");
                menu.setWednesday("Gebratener Lachs, Wildreis, Zitronenbutter");
                menu.setThursday("Wiener Schnitzel, Pommes frites, Gurkensalat");
                menu.setFriday("Vegetarische Buddha Bowl, Hummus, Fladenbrot");
                menu.setNotes("Vegetarische Alternative täglich auf Anfrage verfügbar.");
                model.addAttribute("menu", menu);
                model.addAttribute("weekStart", menu.getWeekStart());
                model.addAttribute("weekEnd", menu.getWeekEnd());
                model.addAttribute("unsubscribeUrl", "#");
            }
            case "welcome-subscriber" -> {
                model.addAttribute("name", "Max Mustermann");
                model.addAttribute("unsubscribeUrl", "#");
            }
            case "two-factor-code" -> {
                model.addAttribute("code", "847291");
            }
            case "invoice-notification" -> {
                model.addAttribute("customerName", "Sophie Meier");
                model.addAttribute("invoiceNumber", "RE-2026-0001");
                model.addAttribute("netAmount", "10.504,20");
                model.addAttribute("taxRate", "19");
                model.addAttribute("taxAmount", "1.995,80");
                model.addAttribute("totalAmount", "12.500,00");
                model.addAttribute("dueDate", "20.04.2026");
                model.addAttribute("portalUrl", "#");
            }
            case "booking-status-update" -> {
                model.addAttribute("bookingId", "42");
                model.addAttribute("bookingType", "Hochzeit");
                model.addAttribute("statusLabel", "Bestaetigt");
                model.addAttribute("statusColor", "#2E7D32");
                model.addAttribute("eventDate", "20.09.2026");
                model.addAttribute("timeSlot", "Nachmittag (15-18 Uhr)");
                model.addAttribute("guestCount", "85");
                model.addAttribute("portalUrl", "#");
            }
        }
        return "email/" + template;
    }
}
