package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.Quote;
import de.elyseeevents.portal.repository.BookingRepository;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.repository.QuoteRepository;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

    private final CustomerService customerService;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;

    public AdminSearchController(CustomerService customerService, BookingRepository bookingRepository,
                                 InvoiceRepository invoiceRepository, QuoteRepository quoteRepository) {
        this.customerService = customerService;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.quoteRepository = quoteRepository;
    }

    @GetMapping("/suche")
    public String search(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        model.addAttribute("pageTitle", "Suche");
        model.addAttribute("activeNav", "suche");
        model.addAttribute("query", q);

        if (q.isBlank()) {
            return "admin/search";
        }

        // SQL-basierte Suche statt In-Memory-Filterung
        List<Customer> customers = customerService.search(q);
        List<Booking> bookings = bookingRepository.search(q);
        List<Invoice> invoices = invoiceRepository.search(q);
        List<Quote> quotes = quoteRepository.search(q);

        model.addAttribute("customers", customers);
        model.addAttribute("bookings", bookings);
        model.addAttribute("invoices", invoices);
        model.addAttribute("quotes", quotes);
        model.addAttribute("totalResults",
                customers.size() + bookings.size() + invoices.size() + quotes.size());

        return "admin/search";
    }
}
