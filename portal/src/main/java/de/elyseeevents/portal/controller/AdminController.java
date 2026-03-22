package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BookingService bookingService;
    private final CustomerService customerService;

    public AdminController(BookingService bookingService, CustomerService customerService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("totalBookings", bookingService.count());
        model.addAttribute("openInquiries", bookingService.countByStatus("ANFRAGE"));
        model.addAttribute("thisMonth", bookingService.countThisMonth());
        model.addAttribute("totalBudget", bookingService.totalBudget());
        model.addAttribute("totalCustomers", customerService.count());
        model.addAttribute("recentBookings", bookingService.findRecent(5));
        model.addAttribute("monthlyRevenue", bookingService.monthlyRevenue(12));
        return "admin/dashboard";
    }
}
