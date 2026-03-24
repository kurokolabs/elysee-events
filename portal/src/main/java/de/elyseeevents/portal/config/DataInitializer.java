package de.elyseeevents.portal.config;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.InvoiceItem;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.BookingRepository;
import de.elyseeevents.portal.repository.CustomerRepository;
import de.elyseeevents.portal.repository.InvoiceItemRepository;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final Environment environment;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, CustomerRepository customerRepository,
                          BookingRepository bookingRepository, InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository, PasswordEncoder passwordEncoder,
                          Environment environment) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        // Admin account
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setActive(true);
            admin.setForcePwChange(true);
            admin.setTwoFaEnabled(true);
            userRepository.save(admin);
        }

        // Demo data: nur in nicht-prod Umgebungen
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProd = java.util.Arrays.stream(activeProfiles).anyMatch("prod"::equalsIgnoreCase);
        if (isProd) return;

        // Demo customer account
        if (userRepository.findByEmail("demo@elysee-events.de").isEmpty()) {
            User demoUser = new User();
            demoUser.setEmail("demo@elysee-events.de");
            demoUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            demoUser.setRole("CUSTOMER");
            demoUser.setActive(true);
            demoUser.setForcePwChange(false);
            demoUser.setTwoFaEnabled(true);
            demoUser = userRepository.save(demoUser);

            Customer demoCustomer = new Customer();
            demoCustomer.setUserId(demoUser.getId());
            demoCustomer.setFirstName("Sophie");
            demoCustomer.setLastName("Meier");
            demoCustomer.setCompany("Meier Events GmbH");
            demoCustomer.setPhone("0821 555 1234");
            demoCustomer.setAddress("Maximilianstrasse 42");
            demoCustomer.setPostalCode("86150");
            demoCustomer.setCity("Augsburg");
            demoCustomer = customerRepository.save(demoCustomer);

            // Demo bookings
            Booking b1 = new Booking();
            b1.setCustomerId(demoCustomer.getId());
            b1.setBookingType("HOCHZEIT");
            b1.setStatus("BESTAETIGT");
            b1.setEventDate("2026-09-20");
            b1.setEventTimeSlot("Nachmittag (15-18 Uhr)");
            b1.setGuestCount(85);
            b1.setBudget(12500.0);
            b1.setMenuSelection("5-Gang-Menü, vegetarische Option, Dessertbuffet");
            b1.setSpecialRequests("Glutenfreie Optionen für 3 Gäste, Blumendekoration in Weiss/Gold");
            bookingRepository.save(b1);

            Booking b2 = new Booking();
            b2.setCustomerId(demoCustomer.getId());
            b2.setBookingType("CORPORATE");
            b2.setStatus("IN_PLANUNG");
            b2.setEventDate("2026-11-15");
            b2.setEventTimeSlot("Abend (18-23 Uhr)");
            b2.setGuestCount(150);
            b2.setBudget(8000.0);
            b2.setMenuSelection("Flying Buffet, Getränkepauschale");
            b2.setSpecialRequests("Firmenlogo auf Servietten, Bühne für Präsentation");
            bookingRepository.save(b2);

            Booking b3 = new Booking();
            b3.setCustomerId(demoCustomer.getId());
            b3.setBookingType("KANTINE");
            b3.setStatus("ANFRAGE");
            b3.setEventDate("2026-07-01");
            b3.setEventTimeSlot("Mittag (12-15 Uhr)");
            b3.setGuestCount(40);
            b3.setBudget(2500.0);
            b3.setMenuSelection("Saisonales Mittagsmenü");
            bookingRepository.save(b3);

            // Historische Buchungen für Umsatz-Diagramm
            String[][] hist = {
                {"2025-06-15", "HOCHZEIT", "ABGESCHLOSSEN", "9500", "2025-06-01"},
                {"2025-07-20", "CORPORATE", "ABGESCHLOSSEN", "6200", "2025-07-05"},
                {"2025-08-10", "KANTINE", "ABGESCHLOSSEN", "3100", "2025-08-01"},
                {"2025-09-25", "HOCHZEIT", "ABGESCHLOSSEN", "14000", "2025-09-10"},
                {"2025-10-12", "CORPORATE", "ABGESCHLOSSEN", "7800", "2025-10-02"},
                {"2025-11-05", "KANTINE", "ABGESCHLOSSEN", "4200", "2025-11-01"},
                {"2025-12-18", "CORPORATE", "ABGESCHLOSSEN", "11500", "2025-12-03"},
                {"2026-01-22", "HOCHZEIT", "ABGESCHLOSSEN", "8900", "2026-01-08"},
                {"2026-02-14", "CORPORATE", "BESTAETIGT", "5600", "2026-02-01"},
                {"2026-03-10", "KANTINE", "IN_PLANUNG", "3800", "2026-03-01"},
            };
            for (String[] h : hist) {
                Booking hb = new Booking();
                hb.setCustomerId(demoCustomer.getId());
                hb.setEventDate(h[0]);
                hb.setBookingType(h[1]);
                hb.setStatus(h[2]);
                hb.setBudget(Double.parseDouble(h[3]));
                hb.setGuestCount(50 + (int)(Math.random() * 100));
                hb.setEventTimeSlot("Abend (18-23 Uhr)");
                hb = bookingRepository.save(hb);
                bookingRepository.setCreatedAt(hb.getId(), h[4] + " 10:00:00");
            }

            // Demo-Rechnung 1: Hochzeit mit Positionen
            Invoice inv1 = new Invoice();
            inv1.setBookingId(b1.getId());
            inv1.setCustomerId(demoCustomer.getId());
            inv1.setInvoiceNumber(invoiceRepository.nextInvoiceNumber());
            inv1.setAmount(10504.20);
            inv1.setTaxRate(19.0);
            inv1.setTaxAmount(1995.80);
            inv1.setTotal(12500.00);
            inv1.setStatus("OFFEN");
            inv1.setDueDate("2026-08-20");
            inv1.setNotes("Zahlbar innerhalb von 14 Tagen nach Rechnungsdatum.");
            invoiceRepository.save(inv1);

            addItem(inv1.getId(), "Saalmiete (Großer Festsaal)", 1, 2800.00);
            addItem(inv1.getId(), "5-Gang-Menü (85 Personen)", 85, 58.50);
            addItem(inv1.getId(), "Getränkepauschale Premium", 85, 24.00);
            addItem(inv1.getId(), "Blumendekoration & Tischgestaltung", 1, 950.00);
            addItem(inv1.getId(), "DJ & Musikanlage", 1, 680.00);
            addItem(inv1.getId(), "Servicepersonal (8h)", 6, 145.70);

            // Demo-Rechnung 2: Corporate (bezahlt)
            Invoice inv2 = new Invoice();
            inv2.setBookingId(b2.getId());
            inv2.setCustomerId(demoCustomer.getId());
            inv2.setInvoiceNumber(invoiceRepository.nextInvoiceNumber());
            inv2.setAmount(6722.69);
            inv2.setTaxRate(19.0);
            inv2.setTaxAmount(1277.31);
            inv2.setTotal(8000.00);
            inv2.setStatus("BEZAHLT");
            inv2.setDueDate("2026-10-15");
            inv2.setPaidDate("2026-10-10");
            inv2.setNotes("Vielen Dank für Ihre Zahlung.");
            invoiceRepository.save(inv2);

            addItem(inv2.getId(), "Konferenzraum (Ganztags)", 1, 1500.00);
            addItem(inv2.getId(), "Flying Buffet (150 Personen)", 150, 28.50);
            addItem(inv2.getId(), "Getränkepauschale Standard", 150, 5.15);
            addItem(inv2.getId(), "Bühnentechnik & Beamer", 1, 450.00);
        }
    }

    private void addItem(Long invoiceId, String desc, double qty, double unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(invoiceId);
        item.setDescription(desc);
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setTotal(Math.round(qty * unitPrice * 100.0) / 100.0);
        invoiceItemRepository.save(item);
    }
}
