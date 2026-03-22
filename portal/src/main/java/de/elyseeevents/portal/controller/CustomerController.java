package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.*;
import de.elyseeevents.portal.repository.DocumentRepository;
import de.elyseeevents.portal.repository.QuoteItemRepository;
import de.elyseeevents.portal.repository.QuoteRepository;
import de.elyseeevents.portal.service.InvoicePdfService;
import de.elyseeevents.portal.service.QuotePdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.BookingService;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/portal")
public class CustomerController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final CustomerService customerService;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final de.elyseeevents.portal.repository.InvoiceItemRepository invoiceItemRepository;
    private final InvoicePdfService pdfService;
    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuotePdfService quotePdfService;
    private final DocumentRepository documentRepository;
    private final Path uploadPath;
    private final String emailjsPublicKey;
    private final String emailjsServiceId;
    private final String emailjsConfirmTemplateId;

    public CustomerController(CustomerService customerService, BookingService bookingService,
                             UserRepository userRepository, InvoiceRepository invoiceRepository,
                             de.elyseeevents.portal.repository.InvoiceItemRepository invoiceItemRepository,
                             InvoicePdfService pdfService,
                             QuoteRepository quoteRepository, QuoteItemRepository quoteItemRepository,
                             QuotePdfService quotePdfService, DocumentRepository documentRepository,
                             @Value("${app.upload.path:./uploads}") String uploadDir,
                             @Value("${app.emailjs.public-key:YOUR_PUBLIC_KEY}") String emailjsPublicKey,
                             @Value("${app.emailjs.service-id:YOUR_SERVICE_ID}") String emailjsServiceId,
                             @Value("${app.emailjs.confirm-template-id:YOUR_CONFIRM_TEMPLATE_ID}") String emailjsConfirmTemplateId) {
        this.customerService = customerService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.pdfService = pdfService;
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.quotePdfService = quotePdfService;
        this.documentRepository = documentRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.emailjsPublicKey = emailjsPublicKey;
        this.emailjsServiceId = emailjsServiceId;
        this.emailjsConfirmTemplateId = emailjsConfirmTemplateId;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookingService.findByCustomerId(customer.getId()));
        return "customer/dashboard";
    }

    @GetMapping("/buchung/{id}")
    public String bookingDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        Optional<Booking> bookingOpt = bookingService.findById(id);
        if (bookingOpt.isEmpty() || !bookingOpt.get().getCustomerId().equals(customer.getId())) {
            return "redirect:/portal/dashboard";
        }

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("booking", bookingOpt.get());
        return "customer/booking-detail";
    }

    // ── Verfuegbarkeit (JSON-API) ────────────────────────────
    @GetMapping("/api/availability")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> calendarAvailability(
            @RequestParam int year, @RequestParam int month,
            Authentication authentication) {
        if (authentication == null) return java.util.List.of();
        return bookingService.availabilityData(year, month);
    }

    // ── Neue Anfrage senden ────────────────────────────────
    @GetMapping("/anfrage")
    public String inquiryForm(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "anfrage");
        model.addAttribute("customer", customer);
        model.addAttribute("bookingTypes", BookingType.values());
        model.addAttribute("emailjsPublicKey", emailjsPublicKey);
        model.addAttribute("emailjsServiceId", emailjsServiceId);
        model.addAttribute("emailjsConfirmTemplateId", emailjsConfirmTemplateId);
        model.addAttribute("customerEmail", authentication.getName());
        model.addAttribute("customerName", customer.getFirstName() + " " + customer.getLastName());
        return "customer/inquiry-form";
    }

    @PostMapping("/anfrage")
    public String submitInquiry(@RequestParam String bookingType,
                               @RequestParam(required = false) String eventDate,
                               @RequestParam(required = false) String eventTimeSlot,
                               @RequestParam(required = false) Integer guestCount,
                               @RequestParam(required = false) Double budget,
                               @RequestParam(required = false) String menuSelection,
                               @RequestParam(required = false) String specialRequests,
                               @RequestParam(required = false) String deliveryAddress,
                               @RequestParam(required = false) String cateringPackage,
                               @RequestParam(required = false) String foodOption,
                               @RequestParam(required = false) String foodSubOption,
                               @RequestParam(required = false) String cuisineStyle,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        if (!Set.of("KANTINE", "HOCHZEIT", "CORPORATE").contains(bookingType)) {
            redirectAttributes.addFlashAttribute("error", "Ung\u00fcltiger Buchungstyp.");
            return "redirect:/portal/anfrage";
        }
        if (guestCount != null && guestCount < 1) {
            redirectAttributes.addFlashAttribute("error", "G\u00e4steanzahl muss mindestens 1 sein.");
            return "redirect:/portal/anfrage";
        }
        if (budget != null && budget < 0) {
            redirectAttributes.addFlashAttribute("error", "Budget darf nicht negativ sein.");
            return "redirect:/portal/anfrage";
        }

        Booking booking = new Booking();
        booking.setCustomerId(customer.getId());
        booking.setBookingType(bookingType);
        booking.setStatus("ANFRAGE");
        booking.setEventDate(eventDate);
        booking.setEventTimeSlot(eventTimeSlot);
        booking.setGuestCount(guestCount);
        booking.setBudget(budget);
        booking.setMenuSelection(menuSelection);
        booking.setSpecialRequests(specialRequests);
        booking.setDeliveryAddress(deliveryAddress);
        booking.setCateringPackage(cateringPackage);
        booking.setFoodOption(foodOption);
        booking.setFoodSubOption(foodSubOption);
        booking.setCuisineStyle(cuisineStyle);
        bookingService.save(booking);

        redirectAttributes.addFlashAttribute("message", "Ihre Anfrage wurde erfolgreich gesendet!");
        return "redirect:/portal/dashboard";
    }

    // ── Rechnungen ─────────────────────────────────────────
    @GetMapping("/rechnungen")
    public String invoices(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("customer", customer);
        model.addAttribute("invoices", invoiceRepository.findByCustomerId(customer.getId()));
        return "customer/invoices";
    }

    @GetMapping("/rechnung/{id}")
    public String invoiceDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        Optional<Invoice> invOpt = invoiceRepository.findById(id);
        if (invOpt.isEmpty() || !invOpt.get().getCustomerId().equals(customer.getId())) {
            return "redirect:/portal/rechnungen";
        }

        model.addAttribute("activeNav", "rechnungen");
        model.addAttribute("invoice", invOpt.get());
        return "customer/invoice-detail";
    }

    @GetMapping("/rechnung/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Authentication authentication) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return ResponseEntity.status(401).build();

        Optional<Invoice> invOpt = invoiceRepository.findById(id);
        if (invOpt.isEmpty() || !invOpt.get().getCustomerId().equals(customer.getId())) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invOpt.get();
        byte[] pdf = pdfService.generate(invoice, customer, invoiceItemRepository.findByInvoiceId(id));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Profil ─────────────────────────────────────────────
    @GetMapping("/profil")
    public String profile(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "profil");
        model.addAttribute("customer", customer);
        model.addAttribute("email", authentication.getName());
        return "customer/profile";
    }

    @PostMapping("/profil")
    public String updateProfile(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam(required = false) String company,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String postalCode,
                               @RequestParam(required = false) String city,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vor- und Nachname sind Pflichtfelder.");
            return "redirect:/portal/profil";
        }
        if (firstName.length() > 100 || lastName.length() > 100) {
            redirectAttributes.addFlashAttribute("error", "Name darf maximal 100 Zeichen lang sein.");
            return "redirect:/portal/profil";
        }

        customer.setFirstName(firstName.trim());
        customer.setLastName(lastName.trim());
        customer.setCompany(company);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setPostalCode(postalCode);
        customer.setCity(city);
        customerService.update(customer);

        redirectAttributes.addFlashAttribute("message", "Profil erfolgreich aktualisiert.");
        return "redirect:/portal/profil";
    }

    // -- Dokumente -------------------------------------------------------
    @GetMapping("/dokumente")
    public String documents(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "dokumente");
        model.addAttribute("customer", customer);
        model.addAttribute("documents", documentRepository.findByCustomerId(customer.getId()));
        return "customer/documents";
    }

    @PostMapping("/dokument/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String description,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte w\u00e4hlen Sie eine Datei aus.");
            return "redirect:/portal/dokumente";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            redirectAttributes.addFlashAttribute("error", "Die Datei darf maximal 10 MB gro\u00df sein.");
            return "redirect:/portal/dokumente";
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_TYPES.contains(extension)) {
            redirectAttributes.addFlashAttribute("error",
                    "Dateityp nicht erlaubt. Erlaubt: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX.");
            return "redirect:/portal/dokumente";
        }

        try {
            Path customerDir = uploadPath.resolve(String.valueOf(customer.getId()));
            Files.createDirectories(customerDir);

            String storedName = System.currentTimeMillis() + "_" + originalFilename;
            Path targetPath = customerDir.resolve(storedName);
            file.transferTo(targetPath.toFile());

            Document doc = new Document();
            doc.setCustomerId(customer.getId());
            doc.setUploadedBy("KUNDE");
            doc.setFileName(originalFilename);
            doc.setFilePath(targetPath.toString());
            doc.setFileSize(file.getSize());
            doc.setFileType(extension);
            doc.setDescription(description);
            documentRepository.save(doc);

            redirectAttributes.addFlashAttribute("message", "Dokument erfolgreich hochgeladen.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Hochladen: " + e.getMessage());
        }

        return "redirect:/portal/dokumente";
    }

    @GetMapping("/dokument/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Authentication authentication) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return ResponseEntity.status(401).build();

        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty() || !docOpt.get().getCustomerId().equals(customer.getId())) {
            return ResponseEntity.notFound().build();
        }

        Document doc = docOpt.get();
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // -- Angebote -------------------------------------------------------
    @GetMapping("/angebote")
    public String quotes(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        model.addAttribute("activeNav", "angebote");
        model.addAttribute("customer", customer);
        model.addAttribute("quotes", quoteRepository.findByCustomerId(customer.getId()));
        return "customer/quotes";
    }

    @GetMapping("/angebot/{id}")
    public String quoteDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        Optional<Quote> quoteOpt = quoteRepository.findById(id);
        if (quoteOpt.isEmpty() || !quoteOpt.get().getCustomerId().equals(customer.getId())) {
            return "redirect:/portal/angebote";
        }

        model.addAttribute("activeNav", "angebote");
        model.addAttribute("quote", quoteOpt.get());
        model.addAttribute("items", quoteItemRepository.findByQuoteId(id));
        return "customer/quote-detail";
    }

    @PostMapping("/angebot/{id}/annehmen")
    public String acceptQuote(@PathVariable Long id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        Optional<Quote> quoteOpt = quoteRepository.findById(id);
        if (quoteOpt.isEmpty() || !quoteOpt.get().getCustomerId().equals(customer.getId())) {
            return "redirect:/portal/angebote";
        }

        Quote quote = quoteOpt.get();
        quote.setStatus("ANGENOMMEN");
        quoteRepository.save(quote);

        redirectAttributes.addFlashAttribute("message", "Angebot angenommen.");
        return "redirect:/portal/angebot/" + id;
    }

    @GetMapping("/angebot/{id}/pdf")
    public ResponseEntity<byte[]> downloadQuotePdf(@PathVariable Long id, Authentication authentication) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return ResponseEntity.status(401).build();

        Optional<Quote> quoteOpt = quoteRepository.findById(id);
        if (quoteOpt.isEmpty() || !quoteOpt.get().getCustomerId().equals(customer.getId())) {
            return ResponseEntity.notFound().build();
        }

        Quote quote = quoteOpt.get();
        List<QuoteItem> items = quoteItemRepository.findByQuoteId(id);
        byte[] pdf = quotePdfService.generate(quote, customer, items);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + quote.getQuoteNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private Customer getCustomer(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;
        return customerService.findByUserId(user.getId()).orElse(null);
    }
}
