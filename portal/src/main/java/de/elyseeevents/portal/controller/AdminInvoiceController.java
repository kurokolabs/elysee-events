package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.InvoiceItem;
import de.elyseeevents.portal.repository.InvoiceItemRepository;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import de.elyseeevents.portal.service.EmailService;
import de.elyseeevents.portal.service.InvoicePdfService;
import org.springframework.beans.factory.annotation.Value;
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
public class AdminInvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final InvoicePdfService pdfService;
    private final EmailService emailService;

    @Value("${app.company.name}") private String companyName;
    @Value("${app.company.street}") private String companyStreet;
    @Value("${app.company.city}") private String companyCity;
    @Value("${app.company.phone}") private String companyPhone;
    @Value("${app.company.email}") private String companyEmail;
    @Value("${app.company.web}") private String companyWeb;
    @Value("${app.company.tax-id}") private String companyTaxId;
    @Value("${app.company.hrb}") private String companyHrb;
    @Value("${app.company.court}") private String companyCourt;
    @Value("${app.company.bank}") private String companyBank;
    @Value("${app.company.iban}") private String companyIban;
    @Value("${app.company.bic}") private String companyBic;
    @Value("${app.company.ceo}") private String companyCeo;

    public AdminInvoiceController(InvoiceRepository invoiceRepository, InvoiceItemRepository itemRepository,
                                  BookingService bookingService, CustomerService customerService,
                                  InvoicePdfService pdfService, EmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @GetMapping("/rechnungen")
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("pageTitle", "Rechnungen");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("invoices", q != null && !q.isBlank() ? invoiceRepository.search(q) : invoiceRepository.findAll());
        model.addAttribute("searchQuery", q);
        return "admin/invoices";
    }

    // Schritt 1: Kunde auswählen
    @GetMapping("/rechnung/neu")
    public String selectCustomer(Model model) {
        model.addAttribute("pageTitle", "Neue Rechnung");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customers", customerService.findAll());
        return "admin/invoice-select-customer";
    }

    // Schritt 2a: Positionen eingeben (mit Kunde)
    @GetMapping("/rechnung/neu/{customerId}")
    public String enterItems(@PathVariable Long customerId, Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/rechnung/neu";

        model.addAttribute("pageTitle", "Rechnung erstellen");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customer", customer);
        model.addAttribute("standalone", false);
        model.addAttribute("bookings", bookingService.findByCustomerId(customerId));
        return "admin/invoice-items";
    }

    // Schritt 2b: Unabhängige Rechnung (ohne Kunde)
    @GetMapping("/rechnung/neu/unabhaengig")
    public String enterItemsStandalone(Model model) {
        model.addAttribute("pageTitle", "Unabhängige Rechnung");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("standalone", true);
        model.addAttribute("bookings", java.util.List.of());
        return "admin/invoice-items";
    }

    // Steuerberechnung pro Posten-Typ
    private double[] calculateTax(List<InvoiceItem> items) {
        double netto7 = 0, netto19 = 0;
        for (InvoiceItem item : items) {
            double t = item.getTotal();
            switch (item.getTaxType()) {
                case "ESSEN" -> netto7 += t;
                case "BUEFFET" -> { netto7 += t * 0.75; netto19 += t * 0.25; }
                default -> netto19 += t; // GETRAENKE
            }
        }
        double tax7 = Math.round(netto7 * 0.07 * 100.0) / 100.0;
        double tax19 = Math.round(netto19 * 0.19 * 100.0) / 100.0;
        double netto = items.stream().mapToDouble(InvoiceItem::getTotal).sum();
        netto = Math.round(netto * 100.0) / 100.0;
        double totalTax = tax7 + tax19;
        double total = netto + totalTax;
        return new double[]{netto, tax7, tax19, totalTax, total};
    }

    // Schritt 3: Preview generieren
    @PostMapping("/rechnung/preview")
    public String preview(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam("itemTaxType") List<String> taxTypes,
                         @RequestParam(required = false) String dueDate,
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
        Customer customer = null;
        if (customerId != null) {
            customer = customerService.findById(customerId).orElse(null);
        }
        boolean standalone = (customer == null);

        List<InvoiceItem> items = new ArrayList<>();
        int itemCount = Math.min(descriptions.size(), Math.min(quantities.size(), Math.min(prices.size(), taxTypes.size())));
        for (int i = 0; i < itemCount; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            double qty = parseDouble(quantities.get(i), 1);
            double price = parseDouble(prices.get(i), 0);
            double total = Math.round(qty * price * 100.0) / 100.0;

            InvoiceItem item = new InvoiceItem();
            item.setDescription(desc);
            item.setQuantity(qty);
            item.setUnitPrice(price);
            item.setTotal(total);
            item.setTaxType(taxTypes.get(i));
            items.add(item);
        }

        double[] tax = calculateTax(items);

        model.addAttribute("pageTitle", "Vorschau");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customer", customer);
        model.addAttribute("standalone", standalone);
        model.addAttribute("recipientName", recipientName);
        model.addAttribute("recipientCompany", recipientCompany);
        model.addAttribute("recipientAddress", recipientAddress);
        model.addAttribute("recipientPostalCode", recipientPostalCode);
        model.addAttribute("recipientCity", recipientCity);
        model.addAttribute("recipientEmail", recipientEmail);
        String emailForSend = customer != null && customer.getEmail() != null ? customer.getEmail() : recipientEmail;
        model.addAttribute("hasEmail", emailForSend != null && !emailForSend.isBlank());
        model.addAttribute("items", items);
        model.addAttribute("netto", tax[0]);
        model.addAttribute("taxRate", 0.0); // Legacy compatibility
        model.addAttribute("taxAmount7", tax[1]);
        model.addAttribute("taxAmount19", tax[2]);
        model.addAttribute("taxAmount", tax[3]);
        model.addAttribute("total", tax[4]);
        model.addAttribute("dueDate", dueDate);
        model.addAttribute("notes", notes);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("servicePeriodFrom", servicePeriodFrom);
        model.addAttribute("servicePeriodTo", servicePeriodTo);
        model.addAttribute("introText", introText);
        model.addAttribute("invoiceNumber", invoiceRepository.nextInvoiceNumber());

        model.addAttribute("companyName", companyName);
        model.addAttribute("companyStreet", companyStreet);
        model.addAttribute("companyCity", companyCity);
        model.addAttribute("companyPhone", companyPhone);
        model.addAttribute("companyEmail", companyEmail);
        model.addAttribute("companyWeb", companyWeb);
        model.addAttribute("companyTaxId", companyTaxId);
        model.addAttribute("companyHrb", companyHrb);
        model.addAttribute("companyCourt", companyCourt);
        model.addAttribute("companyBank", companyBank);
        model.addAttribute("companyIban", companyIban);
        model.addAttribute("companyBic", companyBic);
        model.addAttribute("companyCeo", companyCeo);

        return "admin/invoice-preview";
    }

    // Schritt 4: Rechnung bestätigen und erstellen
    @Transactional
    @PostMapping("/rechnung/approve")
    public String approve(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam("itemTaxType") List<String> taxTypes,
                         @RequestParam Double netto,
                         @RequestParam Double taxAmount,
                         @RequestParam Double total,
                         @RequestParam(required = false) String dueDate,
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

        // Recalculate server-side - never trust client totals
        List<InvoiceItem> calcItems = new ArrayList<>();
        int calcCount = Math.min(descriptions.size(), Math.min(quantities.size(), Math.min(prices.size(), taxTypes.size())));
        for (int i = 0; i < calcCount; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            InvoiceItem item = new InvoiceItem();
            item.setDescription(desc);
            item.setQuantity(parseDouble(quantities.get(i), 1));
            item.setUnitPrice(parseDouble(prices.get(i), 0));
            item.setTotal(Math.round(item.getQuantity() * item.getUnitPrice() * 100.0) / 100.0);
            item.setTaxType(taxTypes.get(i));
            calcItems.add(item);
        }
        double[] tax = calculateTax(calcItems);

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setBookingId(bookingId);
        inv.setInvoiceNumber(invoiceRepository.nextInvoiceNumber());
        inv.setAmount(tax[0]);
        inv.setTaxRate(0.0);
        inv.setTaxAmount(tax[3]);
        inv.setTaxAmount7(tax[1]);
        inv.setTaxAmount19(tax[2]);
        inv.setTotal(tax[4]);
        inv.setStatus("OFFEN");
        inv.setDueDate(dueDate);
        inv.setNotes(notes);
        inv.setServicePeriodFrom(servicePeriodFrom != null && !servicePeriodFrom.isBlank() ? servicePeriodFrom : null);
        inv.setServicePeriodTo(servicePeriodTo != null && !servicePeriodTo.isBlank() ? servicePeriodTo : null);
        inv.setIntroText(introText);
        inv.setRecipientName(recipientName);
        inv.setRecipientCompany(recipientCompany);
        inv.setRecipientAddress(recipientAddress);
        inv.setRecipientPostalCode(recipientPostalCode);
        inv.setRecipientCity(recipientCity);
        inv.setRecipientEmail(recipientEmail);

        // Bei Standalone-Rechnung: Kunde automatisch anlegen (ohne Portal-Zugang)
        if (customerId == null && recipientName != null && !recipientName.isBlank()) {
            Customer newCustomer = new Customer();
            String[] nameParts = recipientName.trim().split("\\s+", 2);
            newCustomer.setFirstName(nameParts[0]);
            newCustomer.setLastName(nameParts.length > 1 ? nameParts[1] : "");
            newCustomer.setCompany(recipientCompany);
            newCustomer.setAddress(recipientAddress);
            newCustomer.setPostalCode(recipientPostalCode);
            newCustomer.setCity(recipientCity);
            newCustomer.setEmail(recipientEmail);
            newCustomer = customerService.save(newCustomer);
            inv.setCustomerId(newCustomer.getId());
        }

        inv = invoiceRepository.save(inv);

        // Positionen speichern
        for (InvoiceItem item : calcItems) {
            item.setInvoiceId(inv.getId());
            itemRepository.save(item);
        }

        // Rechnung per Email mit PDF-Anhang senden
        try {
            Customer customer = customerId != null ? customerService.findById(customerId).orElse(null) : null;
            String sendToEmail = customer != null && customer.getEmail() != null ? customer.getEmail() : recipientEmail;
            if (sendToEmail != null && !sendToEmail.isBlank()) {
                List<InvoiceItem> savedItems = itemRepository.findByInvoiceId(inv.getId());
                byte[] pdfBytes = customer != null ? pdfService.generate(inv, customer, savedItems) : pdfService.generate(inv, savedItems);
                String formattedNet = String.format("%,.2f EUR", tax[0]);
                String formattedTax = String.format("%,.2f EUR", tax[3]);
                String formattedTotal = String.format("%,.2f EUR", tax[4]);
                String displayName = customer != null ? (customer.getCompany() != null ? customer.getCompany() : customer.getEmail()) : (recipientCompany != null && !recipientCompany.isBlank() ? recipientCompany : recipientName);
                emailService.sendHtmlEmailWithAttachment(
                    sendToEmail,
                    "Élysée Events - Rechnung " + inv.getInvoiceNumber(),
                    "email/invoice-notification",
                    java.util.Map.of(
                        "customerName", displayName,
                        "invoiceNumber", inv.getInvoiceNumber(),
                        "netAmount", formattedNet,
                        "taxRate", "7/19",
                        "taxAmount", formattedTax,
                        "totalAmount", formattedTotal,
                        "dueDate", dueDate != null ? dueDate : "",
                        "portalUrl", "https://www.elysee-events.de/portal/dashboard"
                    ),
                    pdfBytes,
                    inv.getInvoiceNumber() + ".pdf"
                );
            }
        } catch (Exception e) {
            // Email-Fehler soll Rechnungserstellung nicht blockieren
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Rechnungs-Email konnte nicht gesendet werden: {}", e.getMessage());
        }

        String sendToEmail2 = null;
        try {
            Customer c2 = customerId != null ? customerService.findById(customerId).orElse(null) : null;
            sendToEmail2 = c2 != null && c2.getEmail() != null ? c2.getEmail() : recipientEmail;
        } catch (Exception ignored) {}
        boolean hatEmail = sendToEmail2 != null && !sendToEmail2.isBlank();
        redirectAttributes.addFlashAttribute("message",
                "Rechnung " + inv.getInvoiceNumber() + (hatEmail ? " erstellt und an den Kunden gesendet." : " erstellt und gespeichert."));
        return "redirect:/portal/admin/rechnung/" + inv.getId();
    }

    @GetMapping("/rechnung/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) return "redirect:/portal/admin/rechnungen";

        model.addAttribute("pageTitle", "Rechnung " + invoice.getInvoiceNumber());
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("invoice", invoice);
        model.addAttribute("items", itemRepository.findByInvoiceId(id));
        return "admin/invoice-detail";
    }

    @Transactional
    @PostMapping("/rechnung/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              RedirectAttributes redirectAttributes) {
        Invoice inv = invoiceRepository.findById(id).orElse(null);
        if (inv == null) return "redirect:/portal/admin/rechnungen";

        if (!java.util.Set.of("OFFEN", "BEZAHLT", "STORNIERT", "UEBERFAELLIG", "MAHNUNG").contains(status)) {
            redirectAttributes.addFlashAttribute("error", "Ungueltiger Status.");
            return "redirect:/portal/admin/rechnung/" + id;
        }

        inv.setStatus(status);
        if ("BEZAHLT".equals(status)) {
            inv.setPaidDate(java.time.LocalDate.now().toString());
        }
        invoiceRepository.save(inv);
        redirectAttributes.addFlashAttribute("message", "Status geändert.");
        return "redirect:/portal/admin/rechnung/" + id;
    }

    @GetMapping("/rechnung/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) return ResponseEntity.notFound().build();

        List<InvoiceItem> items = itemRepository.findByInvoiceId(id);
        Customer customer = invoice.getCustomerId() != null
                ? customerService.findById(invoice.getCustomerId()).orElse(null) : null;
        byte[] pdf = customer != null
                ? pdfService.generate(invoice, customer, items)
                : pdfService.generate(invoice, items);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private double parseDouble(String s, double fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return fallback; }
    }
}
