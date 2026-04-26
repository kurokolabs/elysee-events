package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.KantineReservation;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.KantineReservationRepository;
import de.elyseeevents.portal.repository.UserRepository;
import de.elyseeevents.portal.service.CustomerService;
import de.elyseeevents.portal.util.BavarianHolidayUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/portal/kantine")
public class CustomerKantineController {

    private static final int MAX_SEATS = 50;
    private static final ZoneId TZ = ZoneId.of("Europe/Berlin");
    private static final LocalTime OPEN = LocalTime.of(11, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(13, 30);
    private static final LocalTime CLOSE = LocalTime.of(14, 0);
    private static final List<String> TIME_SLOTS = List.of(
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30");

    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final KantineReservationRepository reservationRepository;
    private final BavarianHolidayUtil holidayUtil;

    public CustomerKantineController(CustomerService customerService,
                                     UserRepository userRepository,
                                     KantineReservationRepository reservationRepository,
                                     BavarianHolidayUtil holidayUtil) {
        this.customerService = customerService;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.holidayUtil = holidayUtil;
    }

    @GetMapping("/reservierung")
    public String reservationForm(Authentication authentication, Model model) {
        Customer customer = getCustomer(authentication);
        if (customer == null) return "redirect:/portal/login";

        String defaultName = ((customer.getFirstName() == null ? "" : customer.getFirstName()) + " "
                + (customer.getLastName() == null ? "" : customer.getLastName())).trim();

        LocalDate minDate = LocalDate.now(TZ);
        LocalDate maxDate = minDate.plusMonths(3);

        model.addAttribute("activeNav", "kantine");
        model.addAttribute("customer", customer);
        model.addAttribute("defaultName", defaultName);
        model.addAttribute("timeSlots", TIME_SLOTS);
        model.addAttribute("minDate", minDate.toString());
        model.addAttribute("maxDate", maxDate.toString());
        model.addAttribute("openingHours", "Mo–Fr 11:00–14:00 Uhr (an Feiertagen geschlossen)");
        model.addAttribute("reservations", reservationRepository.findByCustomerId(customer.getId()));
        return "customer/kantine-reservierung";
    }

    @PostMapping("/reservierung")
    public String submitReservation(@RequestParam String name,
                                    @RequestParam Integer seatCount,
                                    @RequestParam String reservationDate,
                                    @RequestParam String reservationTime,
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

        LocalDate date;
        try {
            date = LocalDate.parse(reservationDate);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Bitte ein gültiges Datum wählen.");
            return "redirect:/portal/kantine/reservierung";
        }
        LocalDate today = LocalDate.now(TZ);
        if (date.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Datum darf nicht in der Vergangenheit liegen.");
            return "redirect:/portal/kantine/reservierung";
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            redirectAttributes.addFlashAttribute("error",
                    "Die Kantine ist nur Mo–Fr geöffnet.");
            return "redirect:/portal/kantine/reservierung";
        }
        if (holidayUtil.isHoliday(date)) {
            redirectAttributes.addFlashAttribute("error",
                    "An diesem Tag (" + holidayUtil.getHolidayName(date) + ") bleibt die Kantine geschlossen.");
            return "redirect:/portal/kantine/reservierung";
        }

        if (!TIME_SLOTS.contains(reservationTime)) {
            redirectAttributes.addFlashAttribute("error",
                    "Bitte eine gültige Uhrzeit zwischen "
                            + OPEN + " und " + LAST_SLOT + " wählen.");
            return "redirect:/portal/kantine/reservierung";
        }
        LocalTime time = LocalTime.parse(reservationTime);
        if (time.isBefore(OPEN) || time.isAfter(LAST_SLOT) || !time.isBefore(CLOSE)) {
            redirectAttributes.addFlashAttribute("error",
                    "Reservierungen sind nur zwischen 11:00 und 13:30 Uhr möglich.");
            return "redirect:/portal/kantine/reservierung";
        }
        if (date.isEqual(today) && time.isBefore(LocalTime.now(TZ))) {
            redirectAttributes.addFlashAttribute("error",
                    "Die gewählte Uhrzeit liegt bereits in der Vergangenheit.");
            return "redirect:/portal/kantine/reservierung";
        }

        KantineReservation r = new KantineReservation();
        r.setCustomerId(customer.getId());
        r.setName(name.trim());
        r.setSeatCount(seatCount);
        r.setReservationDate(reservationDate);
        r.setReservationTime(reservationTime);
        r.setStatus("OFFEN");
        reservationRepository.save(r);

        redirectAttributes.addFlashAttribute("message",
                "Reservierung für " + seatCount + " Personen am " + reservationDate
                        + " um " + reservationTime + " Uhr wurde gespeichert.");
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
