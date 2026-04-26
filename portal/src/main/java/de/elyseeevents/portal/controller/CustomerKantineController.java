package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.KantineReservation;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.KantineReservationRepository;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/portal/kantine")
public class CustomerKantineController {

    private static final int MAX_SEATS = 50;

    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final KantineReservationRepository reservationRepository;

    public CustomerKantineController(CustomerService customerService,
                                     UserRepository userRepository,
                                     KantineReservationRepository reservationRepository) {
        this.customerService = customerService;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/reservierung")
    public String reservationForm(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        String defaultName = ((customer.getFirstName() == null ? "" : customer.getFirstName()) + " "
                + (customer.getLastName() == null ? "" : customer.getLastName())).trim();

        model.addAttribute("activeNav", "kantine");
        model.addAttribute("customer", customer);
        model.addAttribute("defaultName", defaultName);
        model.addAttribute("reservations", reservationRepository.findByCustomerId(customer.getId()));
        return "customer/kantine-reservierung";
    }

    @PostMapping("/reservierung")
    public String submitReservation(@RequestParam String name,
                                    @RequestParam Integer seatCount,
                                    @RequestParam(required = false) String reservationDate,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Bitte geben Sie einen Namen an.");
            return "redirect:/portal/kantine/reservierung";
        }
        if (name.length() > 150) {
            redirectAttributes.addFlashAttribute("error", "Name darf maximal 150 Zeichen lang sein.");
            return "redirect:/portal/kantine/reservierung";
        }
        if (seatCount == null || seatCount < 1) {
            redirectAttributes.addFlashAttribute("error", "Sitzanzahl muss mindestens 1 betragen.");
            return "redirect:/portal/kantine/reservierung";
        }
        if (seatCount > MAX_SEATS) {
            redirectAttributes.addFlashAttribute("error",
                    "Bei Gruppen über " + MAX_SEATS + " Personen bitte direkt anfragen.");
            return "redirect:/portal/kantine/reservierung";
        }

        KantineReservation r = new KantineReservation();
        r.setCustomerId(customer.getId());
        r.setName(name.trim());
        r.setSeatCount(seatCount);
        r.setReservationDate(reservationDate == null || reservationDate.isBlank() ? null : reservationDate);
        r.setStatus("OFFEN");
        reservationRepository.save(r);

        redirectAttributes.addFlashAttribute("message",
                "Reservierung für " + seatCount + " Personen wurde gespeichert.");
        return "redirect:/portal/kantine/reservierung";
    }

    @PostMapping("/reservierung/{id}/stornieren")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        Optional<KantineReservation> opt = reservationRepository.findById(id);
        if (opt.isEmpty() || !opt.get().getCustomerId().equals(customer.getId())) {
            redirectAttributes.addFlashAttribute("error", "Reservierung nicht gefunden.");
            return "redirect:/portal/kantine/reservierung";
        }
        reservationRepository.updateStatus(id, "STORNIERT");
        redirectAttributes.addFlashAttribute("message", "Reservierung storniert.");
        return "redirect:/portal/kantine/reservierung";
    }

    private Customer getCustomer(Authentication authentication) {
        if (authentication == null) return null;
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return null;
        return customerService.findByUserId(user.getId()).orElse(null);
    }
}
