package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Quote;
import de.elyseeevents.portal.model.QuoteItem;
import de.elyseeevents.portal.repository.QuoteItemRepository;
import de.elyseeevents.portal.repository.QuoteRepository;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import de.elyseeevents.portal.service.QuotePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuoteController {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository itemRepository;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final QuotePdfService pdfService;

    public AdminQuoteController(QuoteRepository quoteRepository, QuoteItemRepository itemRepository,
                                BookingService bookingService, CustomerService customerService,
                                QuotePdfService pdfService) {
        this.quoteRepository = quoteRepository;
        this.itemRepository = itemRepository;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.pdfService = pdfService;
    }

    @GetMapping("/angebote")
    public String list(Model model) {
        model.addAttribute("pageTitle", "Angebote");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("quotes", quoteRepository.findAll());
        return "admin/quotes";
    }

    // Schritt 1: Kunde ausw\u00e4hlen
    @GetMapping("/angebot/neu")
    public String selectCustomer(Model model) {
        model.addAttribute("pageTitle", "Neues Angebot");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customers", customerService.findAll());
        return "admin/quote-select-customer";
    }

    // Schritt 2: Positionen eingeben
    @GetMapping("/angebot/neu/{customerId}")
    public String enterItems(@PathVariable Long customerId, Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/angebot/neu";

        model.addAttribute("pageTitle", "Angebot erstellen");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookingService.findByCustomerId(customerId));
        return "admin/quote-items";
    }

    // Schritt 3: Preview generieren
    @PostMapping("/angebot/preview")
    public String preview(@RequestParam Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam(required = false, defaultValue = "19.0") Double taxRate,
                         @RequestParam(required = false) String validUntil,
                         @RequestParam(required = false) String notes,
                         Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/angebot/neu";

        List<QuoteItem> items = new ArrayList<>();
        double netto = 0;
        for (int i = 0; i < descriptions.size(); i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            double qty = parseDouble(quantities.get(i), 1);
            double price = parseDouble(prices.get(i), 0);
            double total = Math.round(qty * price * 100.0) / 100.0;

            QuoteItem item = new QuoteItem();
            item.setDescription(desc);
            item.setQuantity(qty);
            item.setUnitPrice(price);
            item.setTotal(total);
            items.add(item);
            netto += total;
        }

        double taxAmount = Math.round(netto * (taxRate / 100.0) * 100.0) / 100.0;
        double total = netto + taxAmount;

        model.addAttribute("pageTitle", "Vorschau");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customer", customer);
        model.addAttribute("items", items);
        model.addAttribute("netto", netto);
        model.addAttribute("taxRate", taxRate);
        model.addAttribute("taxAmount", taxAmount);
        model.addAttribute("total", total);
        model.addAttribute("validUntil", validUntil);
        model.addAttribute("notes", notes);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("quoteNumber", quoteRepository.nextQuoteNumber());

        return "admin/quote-preview";
    }

    // Schritt 4: Angebot best\u00e4tigen und erstellen
    @PostMapping("/angebot/approve")
    public String approve(@RequestParam Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam Double taxRate,
                         @RequestParam Double netto,
                         @RequestParam Double taxAmount,
                         @RequestParam Double total,
                         @RequestParam(required = false) String validUntil,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {

        Quote quote = new Quote();
        quote.setCustomerId(customerId);
        quote.setBookingId(bookingId);
        quote.setQuoteNumber(quoteRepository.nextQuoteNumber());
        quote.setAmount(netto);
        quote.setTaxRate(taxRate);
        quote.setTaxAmount(taxAmount);
        quote.setTotal(total);
        quote.setStatus("OFFEN");
        quote.setValidUntil(validUntil);
        quote.setNotes(notes);
        quote = quoteRepository.save(quote);

        // Positionen speichern
        for (int i = 0; i < descriptions.size(); i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;

            QuoteItem item = new QuoteItem();
            item.setQuoteId(quote.getId());
            item.setDescription(desc);
            item.setQuantity(parseDouble(quantities.get(i), 1));
            item.setUnitPrice(parseDouble(prices.get(i), 0));
            item.setTotal(Math.round(item.getQuantity() * item.getUnitPrice() * 100.0) / 100.0);
            itemRepository.save(item);
        }

        redirectAttributes.addFlashAttribute("message",
                "Angebot " + quote.getQuoteNumber() + " erstellt.");
        return "redirect:/portal/admin/angebot/" + quote.getId();
    }

    @GetMapping("/angebot/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return "redirect:/portal/admin/angebote";

        model.addAttribute("pageTitle", "Angebot " + quote.getQuoteNumber());
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("quote", quote);
        model.addAttribute("items", itemRepository.findByQuoteId(id));
        return "admin/quote-detail";
    }

    @PostMapping("/angebot/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              RedirectAttributes redirectAttributes) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return "redirect:/portal/admin/angebote";

        quote.setStatus(status);
        quoteRepository.save(quote);
        redirectAttributes.addFlashAttribute("message", "Status ge\u00e4ndert.");
        return "redirect:/portal/admin/angebot/" + id;
    }

    @GetMapping("/angebot/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return ResponseEntity.notFound().build();

        Customer customer = customerService.findById(quote.getCustomerId()).orElse(null);
        if (customer == null) return ResponseEntity.notFound().build();

        List<QuoteItem> items = itemRepository.findByQuoteId(id);
        byte[] pdf = pdfService.generate(quote, customer, items);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + quote.getQuoteNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private double parseDouble(String s, double fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return fallback; }
    }
}
