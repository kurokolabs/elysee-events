package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.Quote;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.repository.QuoteRepository;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

    private final CustomerService customerService;
    private final BookingService bookingService;
    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;

    public AdminSearchController(CustomerService customerService, BookingService bookingService,
                                 InvoiceRepository invoiceRepository, QuoteRepository quoteRepository) {
        this.customerService = customerService;
        this.bookingService = bookingService;
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

        String term = q.toLowerCase().trim();

        // Kunden durchsuchen (Name, Firma, E-Mail, Stadt)
        List<Customer> customers = customerService.findAll().stream()
                .filter(c -> matches(c.getFirstName(), term)
                        || matches(c.getLastName(), term)
                        || matches(c.getCompany(), term)
                        || matches(c.getEmail(), term)
                        || matches(c.getCity(), term))
                .collect(Collectors.toList());

        // Buchungen durchsuchen (Kundenname, Typ)
        List<Booking> bookings = bookingService.findAll().stream()
                .filter(b -> matches(b.getCustomerName(), term)
                        || matches(b.getBookingType(), term))
                .collect(Collectors.toList());

        // Rechnungen durchsuchen (Nummer, Kundenname)
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> matches(i.getInvoiceNumber(), term)
                        || matches(i.getCustomerName(), term))
                .collect(Collectors.toList());

        // Angebote durchsuchen (Nummer, Kundenname)
        List<Quote> quotes = quoteRepository.findAll().stream()
                .filter(qo -> matches(qo.getQuoteNumber(), term)
                        || matches(qo.getCustomerName(), term))
                .collect(Collectors.toList());

        model.addAttribute("customers", customers);
        model.addAttribute("bookings", bookings);
        model.addAttribute("invoices", invoices);
        model.addAttribute("quotes", quotes);
        model.addAttribute("totalResults",
                customers.size() + bookings.size() + invoices.size() + quotes.size());

        return "admin/search";
    }

    private boolean matches(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
