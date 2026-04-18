package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Document;
import de.elyseeevents.portal.repository.DocumentRepository;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.Optional;
import java.util.Set;

import de.elyseeevents.portal.util.FileUtil;

@Controller
@RequestMapping("/portal/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final DocumentRepository documentRepository;
    private final CustomerService customerService;
    private final Path uploadPath;

    public AdminDocumentController(DocumentRepository documentRepository,
                                   CustomerService customerService,
                                   @Value("${app.upload.path:./uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.customerService = customerService;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/dokumente")
    public String list(Model model) {
        model.addAttribute("pageTitle", "Dokumente");
        model.addAttribute("activeNav", "dokumente");
        model.addAttribute("documents", documentRepository.findAll());
        model.addAttribute("customers", customerService.findAll());
        return "admin/documents";
    }

    @PostMapping("/dokument/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                        @RequestParam Long customerId,
                        @RequestParam(required = false) Long bookingId,
                        @RequestParam(required = false) String description,
                        RedirectAttributes redirectAttributes) {

        // Validierung
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte w\u00e4hlen Sie eine Datei aus.");
            return "redirect:/portal/admin/dokumente";
        }

        if (file.getSize() > FileUtil.MAX_FILE_SIZE) {
            redirectAttributes.addFlashAttribute("error", "Die Datei darf maximal 10 MB gro\u00df sein.");
            return "redirect:/portal/admin/dokumente";
        }

        String originalFilename = FileUtil.sanitizeFilename(file.getOriginalFilename());
        String extension = FileUtil.getFileExtension(originalFilename).toLowerCase();
        if (!FileUtil.ALLOWED_TYPES.contains(extension)) {
            redirectAttributes.addFlashAttribute("error",
                    "Dateityp nicht erlaubt. Erlaubt: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX.");
            return "redirect:/portal/admin/dokumente";
        }

        if (!FileUtil.isAllowedMimeType(file.getContentType())) {
            redirectAttributes.addFlashAttribute("error",
                    "Dateityp nicht erlaubt. Erlaubt: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX.");
            return "redirect:/portal/admin/dokumente";
        }

        try {
            // Verzeichnis erstellen
            Path customerDir = uploadPath.resolve(String.valueOf(customerId));
            Files.createDirectories(customerDir);

            // Dateiname: timestamp_originalname
            String storedName = System.currentTimeMillis() + "_" + originalFilename;
            Path targetPath = customerDir.resolve(storedName).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                redirectAttributes.addFlashAttribute("error", "Ungueltiger Dateiname.");
                return "redirect:/portal/admin/dokumente";
            }
            file.transferTo(targetPath.toFile());

            // Datenbank-Eintrag
            Document doc = new Document();
            doc.setCustomerId(customerId);
            doc.setBookingId(bookingId);
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

        return "redirect:/portal/admin/dokumente";
    }

    @GetMapping("/dokument/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Document doc = docOpt.get();
        try {
            Path filePath = Paths.get(doc.getFilePath()).toAbsolutePath().normalize();
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.status(403).build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_") + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/dokument/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            // Datei vom Dateisystem l\u00f6schen
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException e) {
                    // Datei konnte nicht geloescht werden - wird als Warnung geloggt
                redirectAttributes.addFlashAttribute("error",
                        "Dokument aus Datenbank entfernt, aber Datei konnte nicht geloescht werden.");
            }
            documentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Dokument gel\u00f6scht.");
        }
        return "redirect:/portal/admin/dokumente";
    }

}
