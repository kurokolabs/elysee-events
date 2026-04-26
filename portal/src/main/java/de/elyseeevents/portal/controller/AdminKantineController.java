package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.KantineReservation;
import de.elyseeevents.portal.repository.CustomerRepository;
import de.elyseeevents.portal.repository.KantineReservationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/portal/admin/kantine-reservierungen")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKantineController {

    private static final Set<String> ALLOWED_STATUS = Set.of("OFFEN", "BESTAETIGT", "STORNIERT");

    private final KantineReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;

    public AdminKantineController(KantineReservationRepository reservationRepository,
                                  CustomerRepository customerRepository) {
        this.reservationRepository = reservationRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<KantineReservation> reservations = reservationRepository.findAll();
        Map<Long, Customer> customers = new HashMap<>();
        for (KantineReservation r : reservations) {
            customers.computeIfAbsent(r.getCustomerId(),
                    id -> customerRepository.findById(id).orElse(null));
        }
        model.addAttribute("activeNav", "kantineReservierungen");
        model.addAttribute("reservations", reservations);
        model.addAttribute("customers", customers);
        return "admin/kantine-reservierungen";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        if (!ALLOWED_STATUS.contains(status)) {
            redirectAttributes.addFlashAttribute("error", "Ungültiger Status.");
            return "redirect:/portal/admin/kantine-reservierungen";
        }
        reservationRepository.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Status aktualisiert.");
        return "redirect:/portal/admin/kantine-reservierungen";
    }
}
