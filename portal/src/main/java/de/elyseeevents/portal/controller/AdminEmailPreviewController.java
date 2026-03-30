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

    private static final java.util.Set<String> ALLOWED_TEMPLATES = java.util.Set.of(
            "weekly-menu", "weekly-menu-newsletter", "welcome-subscriber",
            "welcome-credentials", "two-factor-code", "invoice-notification",
            "booking-status-update", "email-verification");

    @GetMapping("/email-preview/{template}")
    public String preview(@PathVariable String template, Model model) {
        if (!ALLOWED_TEMPLATES.contains(template)) {
            return "redirect:/portal/admin/email-vorlagen";
        }
        switch (template) {
            case "weekly-menu" -> {
                WeeklyMenu menu = new WeeklyMenu();
                menu.setWeekStart("24.03.2026");
                menu.setWeekEnd("28.03.2026");
                menu.setMonday("Hähnchenbrust mit Kartoffelgratin, Beilagensalat");
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
            case "weekly-menu-newsletter" -> {
                WeeklyMenu menu = new WeeklyMenu();
                menu.setWeekStart("24.03.2026");
                menu.setWeekEnd("28.03.2026");
                menu.setMondayMeat("Hähnchenbrust in Rahmsoße mit Champignons und Butterreis");
                menu.setMondayMeatPrice("8,50");
                menu.setMondayVeg("Spinat-Ricotta-Lasagne");
                menu.setMondayVegPrice("8,00");
                menu.setTuesdayMeat("Rindergulasch mit Spätzle");
                menu.setTuesdayMeatPrice("8,50");
                menu.setTuesdayVeg("Gemüse-Käsespätzle");
                menu.setTuesdayVegPrice("8,00");
                menu.setWednesdayMeat("Gebratener Lachs mit Zitronenkruste und Ofenkartoffeln");
                menu.setWednesdayMeatPrice("8,50");
                menu.setWednesdayVeg("Pilzrisotto mit Parmesan");
                menu.setWednesdayVegPrice("8,00");
                menu.setThursdayMeat("Wiener Schnitzel mit Pommes frites");
                menu.setThursdayMeatPrice("8,50");
                menu.setThursdayVeg("Gnocchi in Gorgonzola-Spinatsoße");
                menu.setThursdayVegPrice("8,00");
                menu.setFridayMeat("Seelachsfilet paniert mit Kartoffelsalat");
                menu.setFridayMeatPrice("8,50");
                menu.setFridayVeg("Gemüse-Risotto mit Parmesan");
                menu.setFridayVegPrice("8,00");
                menu.setNotes("Beilagen separat erhältlich.");
                model.addAttribute("menu", menu);
                model.addAttribute("subscriberName", "Max Mustermann");
                model.addAttribute("unsubscribeToken", "demo-token");
            }
            case "welcome-subscriber" -> {
                model.addAttribute("name", "Max Mustermann");
                model.addAttribute("unsubscribeUrl", "#");
            }
            case "welcome-credentials" -> {
                model.addAttribute("customerName", "Sophie Meier");
                model.addAttribute("email", "sophie.meier@beispiel.de");
                model.addAttribute("tempPassword", "Temp-2026-xK9m");
                model.addAttribute("loginUrl", "#");
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
                model.addAttribute("statusLabel", "Bestätigt");
                model.addAttribute("statusColor", "#2E7D32");
                model.addAttribute("eventDate", "20.09.2026");
                model.addAttribute("timeSlot", "Nachmittag (15-18 Uhr)");
                model.addAttribute("guestCount", "85");
                model.addAttribute("portalUrl", "#");
            }
            case "email-verification" -> {
                model.addAttribute("email", "max@beispiel.de");
                model.addAttribute("verifyUrl", "#");
            }
        }
        return "email/" + template;
    }
}
