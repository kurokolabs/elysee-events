package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.InvoiceItem;
import de.elyseeevents.portal.repository.InvoiceItemRepository;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
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
                                  InvoicePdfService pdfService) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.pdfService = pdfService;
    }

    @GetMapping("/rechnungen")
    public String list(Model model) {
        model.addAttribute("pageTitle", "Rechnungen");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("invoices", invoiceRepository.findAll());
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

    // Schritt 2: Positionen eingeben
    @GetMapping("/rechnung/neu/{customerId}")
    public String enterItems(@PathVariable Long customerId, Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/rechnung/neu";

        model.addAttribute("pageTitle", "Rechnung erstellen");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookingService.findByCustomerId(customerId));
        return "admin/invoice-items";
    }

    // Schritt 3: Preview generieren
    @PostMapping("/rechnung/preview")
    public String preview(@RequestParam Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam(required = false, defaultValue = "19.0") Double taxRate,
                         @RequestParam(required = false) String dueDate,
                         @RequestParam(required = false) String notes,
                         Model model) {
        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) return "redirect:/portal/admin/rechnung/neu";

        List<InvoiceItem> items = new ArrayList<>();
        double netto = 0;
        int itemCount = Math.min(descriptions.size(), Math.min(quantities.size(), prices.size()));
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
            items.add(item);
            netto += total;
        }

        double taxAmount = Math.round(netto * (taxRate / 100.0) * 100.0) / 100.0;
        double total = netto + taxAmount;

        model.addAttribute("pageTitle", "Vorschau");
        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customer", customer);
        model.addAttribute("items", items);
        model.addAttribute("netto", netto);
        model.addAttribute("taxRate", taxRate);
        model.addAttribute("taxAmount", taxAmount);
        model.addAttribute("total", total);
        model.addAttribute("dueDate", dueDate);
        model.addAttribute("notes", notes);
        model.addAttribute("bookingId", bookingId);
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
    public String approve(@RequestParam Long customerId,
                         @RequestParam(required = false) Long bookingId,
                         @RequestParam("itemDesc") List<String> descriptions,
                         @RequestParam("itemQty") List<String> quantities,
                         @RequestParam("itemPrice") List<String> prices,
                         @RequestParam Double taxRate,
                         @RequestParam Double netto,
                         @RequestParam Double taxAmount,
                         @RequestParam Double total,
                         @RequestParam(required = false) String dueDate,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {

        // Recalculate server-side - never trust client totals
        double calcNetto = 0;
        int calcCount = Math.min(descriptions.size(), Math.min(quantities.size(), prices.size()));
        for (int i = 0; i < calcCount; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;
            double qty = parseDouble(quantities.get(i), 1);
            double price = parseDouble(prices.get(i), 0);
            calcNetto += Math.round(qty * price * 100.0) / 100.0;
        }
        double calcTaxAmount = Math.round(calcNetto * (taxRate / 100.0) * 100.0) / 100.0;
        double calcTotal = calcNetto + calcTaxAmount;

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setBookingId(bookingId);
        inv.setInvoiceNumber(invoiceRepository.nextInvoiceNumber());
        inv.setAmount(calcNetto);
        inv.setTaxRate(taxRate);
        inv.setTaxAmount(calcTaxAmount);
        inv.setTotal(calcTotal);
        inv.setStatus("OFFEN");
        inv.setDueDate(dueDate);
        inv.setNotes(notes);
        inv = invoiceRepository.save(inv);

        // Positionen speichern
        int approveItemCount = Math.min(descriptions.size(), Math.min(quantities.size(), prices.size()));
        for (int i = 0; i < approveItemCount; i++) {
            String desc = descriptions.get(i);
            if (desc == null || desc.isBlank()) continue;

            InvoiceItem item = new InvoiceItem();
            item.setInvoiceId(inv.getId());
            item.setDescription(desc);
            item.setQuantity(parseDouble(quantities.get(i), 1));
            item.setUnitPrice(parseDouble(prices.get(i), 0));
            item.setTotal(Math.round(item.getQuantity() * item.getUnitPrice() * 100.0) / 100.0);
            itemRepository.save(item);
        }

        redirectAttributes.addFlashAttribute("message",
                "Rechnung " + inv.getInvoiceNumber() + " erstellt und an den Kunden gesendet.");
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

        Customer customer = customerService.findById(invoice.getCustomerId()).orElse(null);
        if (customer == null) return ResponseEntity.notFound().build();

        List<InvoiceItem> items = itemRepository.findByInvoiceId(id);
        byte[] pdf = pdfService.generate(invoice, customer, items);

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
