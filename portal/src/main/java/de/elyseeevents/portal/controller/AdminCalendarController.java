package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCalendarController {

    private final BookingService bookingService;

    public AdminCalendarController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/kalender")
    public String calendar(@RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month,
                          Model model) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        if (m < 1 || m > 12) m = now.getMonthValue();
        if (y < 2020 || y > 2100) y = now.getYear();

        model.addAttribute("activeNav", "kalender");
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("events", bookingService.calendarData(y, m));
        return "admin/calendar";
    }
}
