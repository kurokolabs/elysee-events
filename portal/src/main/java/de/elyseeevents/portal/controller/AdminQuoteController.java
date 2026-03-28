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
import org.springframework.transaction.annotation.Transactional;

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

    @org.springframework.beans.factory.annotation.Value("${app.company.name}") private String companyName;
    @org.springframework.beans.factory.annotation.Value("${app.company.street}") private String companyStreet;
    @org.springframework.beans.factory.annotation.Value("${app.company.city}") private String companyCity;
    @org.springframework.beans.factory.annotation.Value("${app.company.phone}") private String companyPhone;
    @org.springframework.beans.factory.annotation.Value("${app.company.email}") private String companyEmail;
    @org.springframework.beans.factory.annotation.Value("${app.company.web}") private String companyWeb;
    @org.springframework.beans.factory.annotation.Value("${app.company.tax-id}") private String companyTaxId;
    @org.springframework.beans.factory.annotation.Value("${app.company.hrb}") private String companyHrb;
    @org.springframework.beans.factory.annotation.Value("${app.company.court}") private String companyCourt;
    @org.springframework.beans.factory.annotation.Value("${app.company.ceo}") private String companyCeo;

    public AdminQuoteController(QuoteRepository quoteRepository, QuoteItemRepository itemRepository,
                                BookingService bookingService, CustomerService customerService,
                                QuotePdfService pdfService) {
        this.quoteRepository = quoteRepository;
        this.itemRepository = itemRepository;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.pdfService = pdfService;
    }

    // Steuerberechnung (identisch zu Invoice)
    private double[] calculateTax(List<QuoteItem> items) {
        double netto7 = 0, netto19 = 0;
        for (QuoteItem item : items) {
            double t = item.getTotal();
            switch (item.getTaxType()) {
                case "ESSEN" -> netto7 += t;
                case "BUEFFET" -> { netto7 += t * 0.75; netto19 += t * 0.25; }
                default -> netto19 += t;
            }
        }
        double tax7 = Math.round(netto7 * 0.07 * 100.0) / 100.0;
        double tax19 = Math.round(netto19 * 0.19 * 100.0) / 100.0;
        double netto = items.stream().mapToDouble(QuoteItem::getTotal).sum();
        netto = Math.round(netto * 100.0) / 100.0;
        return new double[]{netto, tax7, tax19, tax7 + tax19, netto + tax7 + tax19};
    }

    @GetMapping("/angebote")
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("pageTitle", "Angebote");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("quotes", q != null && !q.isBlank() ? quoteRepository.search(q) : quoteRepository.findAll());
        model.addAttribute("searchQuery", q);
        return "admin/quotes";
    }

    @GetMapping("/angebot/neu")
    public String selectCustomer(Model model) {
        model.addAttribute("pageTitle", "Neues Angebot");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customers", customerService.findAll());
        return "admin/quote-select-customer";
    }

    @GetMapping("/angebot/neu/{customerId}")
    public String enterItems(@PathVariable Long customerId, Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/angebot/neu";
        model.addAttribute("pageTitle", "Angebot erstellen");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customer", customer);
        model.addAttribute("standalone", false);
        model.addAttribute("bookings", bookingService.findByCustomerId(customerId));
        return "admin/quote-items";
    }

    @GetMapping("/angebot/neu/unabhaengig")
    public String enterItemsStandalone(Model model) {
        model.addAttribute("pageTitle", "Unabhängiges Angebot");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("standalone", true);
        model.addAttribute("bookings", java.util.List.of());
        return "admin/quote-items";
    }

    @PostMapping("/angebot/preview")
    public String preview(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam("itemTaxType") List<String> taxTypes,
                         @RequestParam(required = false) String validUntil,
                         @RequestParam(required = false) String notes,
                         @RequestParam(required = false) String servicePeriodFrom,
                         @RequestParam(required = false) String servicePeriodTo,
                         @RequestParam(required = false) String introText,
                         @RequestParam(required = false) String recipientName,
                         @RequestParam(required = false) String recipientCompany,
                         @RequestParam(required = false) String recipientAddress,
                         @RequestParam(required = false) String recipientPostalCode,
                         @RequestParam(required = false) String recipientCity,
                         @RequestParam(required = false) String recipientEmail,
                         Model model) {
        Customer customer = customerId != null ? customerService.findById(customerId).orElse(null) : null;
        boolean standalone = (customer == null);

        List<QuoteItem> items = new ArrayList<>();
        int count = Math.min(descriptions.size(), Math.min(quantities.size(), Math.min(prices.size(), taxTypes.size())));
        for (int i = 0; i < count; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            QuoteItem item = new QuoteItem();
            item.setDescription(desc);
            item.setQuantity(parseDouble(quantities.get(i), 1));
            item.setUnitPrice(parseDouble(prices.get(i), 0));
            item.setTotal(Math.round(item.getQuantity() * item.getUnitPrice() * 100.0) / 100.0);
            item.setTaxType(taxTypes.get(i));
            items.add(item);
        }

        double[] tax = calculateTax(items);

        model.addAttribute("pageTitle", "Vorschau");
        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customer", customer);
        model.addAttribute("standalone", standalone);
        model.addAttribute("items", items);
        model.addAttribute("netto", tax[0]);
        model.addAttribute("taxAmount7", tax[1]);
        model.addAttribute("taxAmount19", tax[2]);
        model.addAttribute("taxAmount", tax[3]);
        model.addAttribute("total", tax[4]);
        model.addAttribute("validUntil", validUntil);
        model.addAttribute("notes", notes);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("servicePeriodFrom", servicePeriodFrom);
        model.addAttribute("servicePeriodTo", servicePeriodTo);
        model.addAttribute("introText", introText);
        model.addAttribute("recipientName", recipientName);
        model.addAttribute("recipientCompany", recipientCompany);
        model.addAttribute("recipientAddress", recipientAddress);
        model.addAttribute("recipientPostalCode", recipientPostalCode);
        model.addAttribute("recipientCity", recipientCity);
        model.addAttribute("recipientEmail", recipientEmail);
        model.addAttribute("quoteNumber", quoteRepository.nextQuoteNumber());
        model.addAttribute("companyName", companyName);
        model.addAttribute("companyStreet", companyStreet);
        model.addAttribute("companyCity", companyCity);
        model.addAttribute("companyPhone", companyPhone);
        model.addAttribute("companyEmail", companyEmail);
        model.addAttribute("companyWeb", companyWeb);
        model.addAttribute("companyTaxId", companyTaxId);
        model.addAttribute("companyHrb", companyHrb);
        model.addAttribute("companyCourt", companyCourt);
        model.addAttribute("companyCeo", companyCeo);
        return "admin/quote-preview";
    }

    @Transactional
    @PostMapping("/angebot/approve")
    public String approve(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam("itemTaxType") List<String> taxTypes,
                         @RequestParam Double netto, @RequestParam Double taxAmount, @RequestParam Double total,
                         @RequestParam(required = false) String validUntil,
                         @RequestParam(required = false) String notes,
                         @RequestParam(required = false) String servicePeriodFrom,
                         @RequestParam(required = false) String servicePeriodTo,
                         @RequestParam(required = false) String introText,
                         @RequestParam(required = false) String recipientName,
                         @RequestParam(required = false) String recipientCompany,
                         @RequestParam(required = false) String recipientAddress,
                         @RequestParam(required = false) String recipientPostalCode,
                         @RequestParam(required = false) String recipientCity,
                         @RequestParam(required = false) String recipientEmail,
                         RedirectAttributes redirectAttributes) {

        List<QuoteItem> calcItems = new ArrayList<>();
        int count = Math.min(descriptions.size(), Math.min(quantities.size(), Math.min(prices.size(), taxTypes.size())));
        for (int i = 0; i < count; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            QuoteItem item = new QuoteItem();
            item.setDescription(desc);
            item.setQuantity(parseDouble(quantities.get(i), 1));
            item.setUnitPrice(parseDouble(prices.get(i), 0));
            item.setTotal(Math.round(item.getQuantity() * item.getUnitPrice() * 100.0) / 100.0);
            item.setTaxType(taxTypes.get(i));
            calcItems.add(item);
        }
        double[] tax = calculateTax(calcItems);

        Quote quote = new Quote();
        quote.setCustomerId(customerId);
        quote.setBookingId(bookingId);
        quote.setQuoteNumber(quoteRepository.nextQuoteNumber());
        quote.setAmount(tax[0]);
        quote.setTaxRate(0.0);
        quote.setTaxAmount(tax[3]);
        quote.setTaxAmount7(tax[1]);
        quote.setTaxAmount19(tax[2]);
        quote.setTotal(tax[4]);
        quote.setStatus("OFFEN");
        quote.setValidUntil(validUntil);
        quote.setNotes(notes);
        quote.setServicePeriodFrom(servicePeriodFrom);
        quote.setServicePeriodTo(servicePeriodTo);
        quote.setIntroText(introText);
        quote.setRecipientName(recipientName);
        quote.setRecipientCompany(recipientCompany);
        quote.setRecipientAddress(recipientAddress);
        quote.setRecipientPostalCode(recipientPostalCode);
        quote.setRecipientCity(recipientCity);
        quote.setRecipientEmail(recipientEmail);
        quote = quoteRepository.save(quote);

        for (QuoteItem item : calcItems) {
            item.setQuoteId(quote.getId());
            itemRepository.save(item);
        }

        redirectAttributes.addFlashAttribute("message", "Angebot " + quote.getQuoteNumber() + " erstellt.");
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

    // Angebot in Rechnung übernehmen
    @GetMapping("/angebot/{id}/zu-rechnung")
    public String convertToInvoice(@PathVariable Long id, Model model) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return "redirect:/portal/admin/angebote";

        List<QuoteItem> items = itemRepository.findByQuoteId(id);

        // Weiterleitung zum Rechnungsformular mit vorbefüllten Daten
        if (quote.getCustomerId() != null) {
            Customer customer = customerService.findById(quote.getCustomerId()).orElse(null);
            model.addAttribute("customer", customer);
            model.addAttribute("standalone", false);
            model.addAttribute("bookings", bookingService.findByCustomerId(quote.getCustomerId()));
        } else {
            model.addAttribute("standalone", true);
            model.addAttribute("bookings", java.util.List.of());
        }

        model.addAttribute("pageTitle", "Rechnung aus Angebot " + quote.getQuoteNumber());
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("prefillItems", items);
        model.addAttribute("prefillIntroText", quote.getIntroText());
        model.addAttribute("prefillServicePeriodFrom", quote.getServicePeriodFrom());
        model.addAttribute("prefillServicePeriodTo", quote.getServicePeriodTo());
        model.addAttribute("prefillNotes", "Übernahme aus Angebot " + quote.getQuoteNumber());
        model.addAttribute("prefillRecipientName", quote.getRecipientName());
        model.addAttribute("prefillRecipientCompany", quote.getRecipientCompany());
        model.addAttribute("prefillRecipientAddress", quote.getRecipientAddress());
        model.addAttribute("prefillRecipientPostalCode", quote.getRecipientPostalCode());
        model.addAttribute("prefillRecipientCity", quote.getRecipientCity());
        model.addAttribute("prefillRecipientEmail", quote.getRecipientEmail());
        model.addAttribute("fromQuote", true);
        return "admin/invoice-items";
    }

    @Transactional
    @PostMapping("/angebot/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirectAttributes) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return "redirect:/portal/admin/angebote";
        if (!java.util.Set.of("OFFEN", "ANGENOMMEN", "ABGELEHNT", "ABGELAUFEN", "STORNIERT").contains(status)) {
            redirectAttributes.addFlashAttribute("error", "Ungültiger Status.");
            return "redirect:/portal/admin/angebot/" + id;
        }
        quote.setStatus(status);
        quoteRepository.save(quote);
        redirectAttributes.addFlashAttribute("message", "Status geändert.");
        return "redirect:/portal/admin/angebot/" + id;
    }

    @GetMapping("/angebot/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Quote quote = quoteRepository.findById(id).orElse(null);
        if (quote == null) return ResponseEntity.notFound().build();

        Customer customer = quote.getCustomerId() != null ? customerService.findById(quote.getCustomerId()).orElse(null) : null;
        List<QuoteItem> items = itemRepository.findByQuoteId(id);
        byte[] pdf = customer != null ? pdfService.generate(quote, customer, items) : pdfService.generate(quote, items);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + quote.getQuoteNumber().replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private double parseDouble(String s, double fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return fallback; }
    }
}
