package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Document;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.DocumentRepository;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Set;

import de.elyseeevents.portal.util.FileUtil;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final Path uploadPath;

    public AdminCustomerController(CustomerService customerService, UserRepository userRepository,
                                   DocumentRepository documentRepository,
                                   @Value("${app.upload.path:./uploads}") String uploadDir) {
        this.customerService = customerService;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/kunden")
    public String list(@RequestParam(required = false) String q, Model model) {
        List<Customer> customers;
        if (q != null && !q.isBlank()) {
            customers = customerService.search(q);
            model.addAttribute("searchQuery", q);
        } else {
            customers = customerService.findAll();
        }
        model.addAttribute("pageTitle", "Kunden");
        model.addAttribute("activeNav", "kunden");
        model.addAttribute("customers", customers);
        return "admin/customers";
    }

    @GetMapping("/kunde/neu")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Neuer Kunde");
        model.addAttribute("activeNav", "kunden");
        model.addAttribute("customer", new Customer());
        return "admin/customer-form";
    }

    @PostMapping("/kunde/neu")
    public String create(@RequestParam String email,
                        @RequestParam String firstName,
                        @RequestParam String lastName,
                        @RequestParam(required = false) String company,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String address,
                        @RequestParam(required = false) String postalCode,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) String notes,
                        RedirectAttributes redirectAttributes) {
        // Validierung
        if (email == null || !email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            redirectAttributes.addFlashAttribute("error", "Bitte geben Sie eine gültige E-Mail-Adresse ein.");
            return "redirect:/portal/admin/kunde/neu";
        }
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vor- und Nachname sind Pflichtfelder.");
            return "redirect:/portal/admin/kunde/neu";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ein Benutzer mit dieser E-Mail existiert bereits.");
            return "redirect:/portal/admin/kunde/neu";
        }

        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setCompany(company);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setPostalCode(postalCode);
        customer.setCity(city);
        customer.setNotes(notes);

        CustomerService.CreateResult result = customerService.createWithAccount(customer, email);

        redirectAttributes.addFlashAttribute("message",
                "Kunde erfolgreich angelegt. Temporäres Passwort: " + result.temporaryPassword());
        redirectAttributes.addFlashAttribute("tempPassword", result.temporaryPassword());
        return "redirect:/portal/admin/kunde/" + result.customer().getId();
    }

    @GetMapping("/kunde/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id).orElse(null);
        if (customer == null) {
            return "redirect:/portal/admin/kunden";
        }

        User user = userRepository.findById(customer.getUserId()).orElse(null);

        model.addAttribute("pageTitle", customer.getFullName());
        model.addAttribute("activeNav", "kunden");
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        return "admin/customer-detail";
    }

    @PostMapping("/kunde/{id}")
    public String update(@PathVariable Long id,
                        @RequestParam String firstName,
                        @RequestParam String lastName,
                        @RequestParam(required = false) String company,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String address,
                        @RequestParam(required = false) String postalCode,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) String notes,
                        RedirectAttributes redirectAttributes) {
        Customer customer = customerService.findById(id).orElse(null);
        if (customer == null) {
            return "redirect:/portal/admin/kunden";
        }

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setCompany(company);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setPostalCode(postalCode);
        customer.setCity(city);
        customer.setNotes(notes);
        customerService.update(customer);

        redirectAttributes.addFlashAttribute("message", "Kundendaten erfolgreich aktualisiert.");
        return "redirect:/portal/admin/kunde/" + id;
    }

    // -- Kunden-Dokumente -----------------------------------------------

    @GetMapping("/kunde/{id}/dokumente")
    public String customerDocuments(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id).orElse(null);
        if (customer == null) return "redirect:/portal/admin/kunden";

        model.addAttribute("pageTitle", "Dokumente - " + customer.getFullName());
        model.addAttribute("activeNav", "kunden");
        model.addAttribute("customer", customer);
        model.addAttribute("documents", documentRepository.findByCustomerId(id));
        return "admin/customer-documents";
    }

    @PostMapping("/kunde/{id}/dokument/upload")
    public String uploadDocument(@PathVariable Long id,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        Customer customer = customerService.findById(id).orElse(null);
        if (customer == null) return "redirect:/portal/admin/kunden";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte w\u00e4hlen Sie eine Datei aus.");
            return "redirect:/portal/admin/kunde/" + id + "/dokumente";
        }

        if (file.getSize() > FileUtil.MAX_FILE_SIZE) {
            redirectAttributes.addFlashAttribute("error", "Die Datei darf maximal 10 MB gro\u00df sein.");
            return "redirect:/portal/admin/kunde/" + id + "/dokumente";
        }

        String originalFilename = FileUtil.sanitizeFilename(file.getOriginalFilename());
        String extension = FileUtil.getFileExtension(originalFilename).toLowerCase();
        if (!FileUtil.ALLOWED_TYPES.contains(extension)) {
            redirectAttributes.addFlashAttribute("error",
                    "Dateityp nicht erlaubt. Erlaubt: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX.");
            return "redirect:/portal/admin/kunde/" + id + "/dokumente";
        }

        try {
            Path customerDir = uploadPath.resolve(String.valueOf(id));
            Files.createDirectories(customerDir);

            String storedName = System.currentTimeMillis() + "_" + originalFilename;
            Path targetPath = customerDir.resolve(storedName).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                redirectAttributes.addFlashAttribute("error", "Ungueltiger Dateiname.");
                return "redirect:/portal/admin/kunde/" + id + "/dokumente";
            }
            file.transferTo(targetPath.toFile());

            Document doc = new Document();
            doc.setCustomerId(id);
            doc.setUploadedBy("ADMIN");
            doc.setFileName(originalFilename);
            doc.setFilePath(targetPath.toString());
            doc.setFileSize(file.getSize());
            doc.setFileType(extension);
            doc.setDescription(description);
            documentRepository.save(doc);

            redirectAttributes.addFlashAttribute("message", "Dokument erfolgreich hochgeladen.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Hochladen. Bitte versuchen Sie es erneut.");
        }

        return "redirect:/portal/admin/kunde/" + id + "/dokumente";
    }

}
