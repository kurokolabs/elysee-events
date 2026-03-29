package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.model.BookingStatus;
import de.elyseeevents.portal.model.BookingType;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final de.elyseeevents.portal.service.EmailService emailService;

    public AdminBookingController(BookingService bookingService, CustomerService customerService,
                                  de.elyseeevents.portal.service.EmailService emailService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.emailService = emailService;
    }

    @GetMapping("/buchungen")
    public String list(@RequestParam(required = false) String type,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) String dateFrom,
                      @RequestParam(required = false) String dateTo,
                      Model model) {
        model.addAttribute("pageTitle", "Buchungen");
        model.addAttribute("activeNav", "buchungen");
        model.addAttribute("bookings", bookingService.findByFilters(type, status, dateFrom, dateTo));
        model.addAttribute("bookingTypes", BookingType.values());
        model.addAttribute("bookingStatuses", BookingStatus.values());
        model.addAttribute("filterType", type);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        return "admin/bookings";
    }

    @GetMapping("/buchung/neu")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Neue Buchung");
        model.addAttribute("activeNav", "buchungen");
        model.addAttribute("booking", new Booking());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("bookingTypes", BookingType.values());
        model.addAttribute("bookingStatuses", BookingStatus.values());
        return "admin/booking-form";
    }

    @PostMapping("/buchung/neu")
    public String create(@RequestParam Long customerId,
                        @RequestParam String bookingType,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String eventDate,
                        @RequestParam(required = false) String eventTimeSlot,
                        @RequestParam(required = false) Integer guestCount,
                        @RequestParam(required = false) Double budget,
                        @RequestParam(required = false) String menuSelection,
                        @RequestParam(required = false) String specialRequests,
                        @RequestParam(required = false) String adminNotes,
                        RedirectAttributes redirectAttributes) {
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setBookingType(bookingType);
        String validStatus = "ANFRAGE";
        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus.valueOf(status);
                validStatus = status;
            } catch (IllegalArgumentException ignored) {}
        }
        booking.setStatus(validStatus);
        booking.setEventDate(eventDate);
        booking.setEventTimeSlot(eventTimeSlot);
        booking.setGuestCount(guestCount);
        booking.setBudget(budget);
        booking.setMenuSelection(menuSelection);
        booking.setSpecialRequests(specialRequests);
        booking.setAdminNotes(adminNotes);

        booking = bookingService.save(booking);
        redirectAttributes.addFlashAttribute("message", "Buchung erfolgreich erstellt.");
        return "redirect:/portal/admin/buchung/" + booking.getId();
    }

    @GetMapping("/buchung/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Booking booking = bookingService.findById(id).orElse(null);
        if (booking == null) {
            return "redirect:/portal/admin/buchungen";
        }

        model.addAttribute("pageTitle", "Buchung #" + id);
        model.addAttribute("activeNav", "buchungen");
        model.addAttribute("booking", booking);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("bookingTypes", BookingType.values());
        model.addAttribute("bookingStatuses", BookingStatus.values());
        return "admin/booking-detail";
    }

    @PostMapping("/buchung/{id}")
    public String update(@PathVariable Long id,
                        @RequestParam Long customerId,
                        @RequestParam String bookingType,
                        @RequestParam String status,
                        @RequestParam(required = false) String eventDate,
                        @RequestParam(required = false) String eventTimeSlot,
                        @RequestParam(required = false) Integer guestCount,
                        @RequestParam(required = false) Double budget,
                        @RequestParam(required = false) String menuSelection,
                        @RequestParam(required = false) String specialRequests,
                        @RequestParam(required = false) String adminNotes,
                        RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(id).orElse(null);
        if (booking == null) {
            return "redirect:/portal/admin/buchungen";
        }

        try {
            BookingType.valueOf(bookingType);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Ungueltiger Buchungstyp.");
            return "redirect:/portal/admin/buchung/" + id;
        }
        try {
            BookingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Ungueltiger Status.");
            return "redirect:/portal/admin/buchung/" + id;
        }

        booking.setCustomerId(customerId);
        booking.setBookingType(bookingType);
        booking.setStatus(status);
        booking.setEventDate(eventDate);
        booking.setEventTimeSlot(eventTimeSlot);
        booking.setGuestCount(guestCount);
        booking.setBudget(budget);
        booking.setMenuSelection(menuSelection);
        booking.setSpecialRequests(specialRequests);
        booking.setAdminNotes(adminNotes);

        bookingService.save(booking);
        redirectAttributes.addFlashAttribute("message", "Buchung erfolgreich aktualisiert.");
        return "redirect:/portal/admin/buchung/" + id;
    }

    @Transactional
    @PostMapping("/buchung/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              RedirectAttributes redirectAttributes) {
        try {
            BookingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Ungueltiger Status.");
            return "redirect:/portal/admin/buchung/" + id;
        }
        bookingService.updateStatus(id, status);

        // Email bei relevanten Status-Änderungen
        try {
            var booking = bookingService.findById(id).orElse(null);
            if (booking != null && booking.getCustomerId() != null) {
                var customer = customerService.findById(booking.getCustomerId()).orElse(null);
                if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
                    String subject = null;
                    String statusColor = "#C9A84C";
                    String statusLabel = status;
                    if ("BESTAETIGT".equals(status)) {
                        subject = "Ihre Buchung #" + id + " wurde bestätigt";
                        statusColor = "#2E7D32"; statusLabel = "Bestätigt";
                    } else if ("STORNIERT".equals(status)) {
                        subject = "Ihre Buchung #" + id + " wurde storniert";
                        statusColor = "#C62828"; statusLabel = "Storniert";
                    } else if ("ABGESCHLOSSEN".equals(status)) {
                        subject = "Ihre Buchung #" + id + " ist abgeschlossen";
                        statusColor = "#6B6560"; statusLabel = "Abgeschlossen";
                    }
                    if (subject != null) {
                        emailService.sendHtmlEmail(customer.getEmail(), "Élysée Events - " + subject,
                            "email/booking-status-update",
                            java.util.Map.of("bookingId", String.valueOf(id), "statusLabel", statusLabel,
                                "statusColor", statusColor,
                                "bookingType", booking.getBookingType() != null ? booking.getBookingType() : "-",
                                "eventDate", booking.getEventDate() != null ? booking.getEventDate() : "-",
                                "timeSlot", booking.getEventTimeSlot() != null ? booking.getEventTimeSlot() : "-",
                                "guestCount", booking.getGuestCount() != null ? String.valueOf(booking.getGuestCount()) : "-",
                                "portalUrl", "https://www.elysee-events.de/portal/dashboard"));
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Buchungs-Status-Email Fehler: {}", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "Status erfolgreich geändert.");
        return "redirect:/portal/admin/buchungen";
    }
}
