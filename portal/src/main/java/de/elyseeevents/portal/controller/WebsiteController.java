package de.elyseeevents.portal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class WebsiteController {

    private final Path websitePath;

    public WebsiteController(@Value("${app.website.path:../elysee-events/}") String path) {
        this.websitePath = Paths.get(path).toAbsolutePath().normalize();
    }

    @GetMapping("/")
    public ResponseEntity<Resource> home() {
        return servePage("elysee-events.html");
    }

    @GetMapping("/kantine")
    public ResponseEntity<Resource> kantine() {
        return servePage("kantine.html");
    }

    @GetMapping("/hochzeit")
    public ResponseEntity<Resource> hochzeit() {
        return servePage("hochzeit.html");
    }

    @GetMapping("/corporate")
    public ResponseEntity<Resource> corporate() {
        return servePage("corporate.html");
    }

    @GetMapping("/impressum")
    public ResponseEntity<Resource> impressum() {
        return servePage("impressum.html");
    }

    @GetMapping("/datenschutz")
    public ResponseEntity<Resource> datenschutz() {
        return servePage("datenschutz.html");
    }

    @GetMapping("/agb")
    public ResponseEntity<Resource> agb() {
        return servePage("agb.html");
    }

    // 301 redirects from old .html URLs
    @GetMapping("/elysee-events.html")
    public ResponseEntity<Void> redirectHome() {
        return redirect("/");
    }

    @GetMapping("/kantine.html")
    public ResponseEntity<Void> redirectKantine() {
        return redirect("/kantine");
    }

    @GetMapping("/hochzeit.html")
    public ResponseEntity<Void> redirectHochzeit() {
        return redirect("/hochzeit");
    }

    @GetMapping("/corporate.html")
    public ResponseEntity<Void> redirectCorporate() {
        return redirect("/corporate");
    }

    @GetMapping("/impressum.html")
    public ResponseEntity<Void> redirectImpressum() {
        return redirect("/impressum");
    }

    @GetMapping("/datenschutz.html")
    public ResponseEntity<Void> redirectDatenschutz() {
        return redirect("/datenschutz");
    }

    @GetMapping("/agb.html")
    public ResponseEntity<Void> redirectAgb() {
        return redirect("/agb");
    }

    private ResponseEntity<Resource> servePage(String filename) {
        try {
            Path file = websitePath.resolve(filename).normalize();
            if (!file.startsWith(websitePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(301)
                .header("Location", location)
                .header("Cache-Control", "public, max-age=31536000")
                .build();
    }
}
