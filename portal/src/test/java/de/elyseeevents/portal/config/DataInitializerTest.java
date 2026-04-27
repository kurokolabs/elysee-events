package de.elyseeevents.portal.config;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.BookingRepository;
import de.elyseeevents.portal.repository.CustomerRepository;
import de.elyseeevents.portal.repository.InvoiceItemRepository;
import de.elyseeevents.portal.repository.InvoiceRepository;
import de.elyseeevents.portal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DataInitializer initializer;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "bcrypt:" + inv.getArgument(0));

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[] { "test" });

        initializer = new DataInitializer(
                userRepository,
                mock(CustomerRepository.class),
                mock(BookingRepository.class),
                mock(InvoiceRepository.class),
                mock(InvoiceItemRepository.class),
                passwordEncoder,
                env);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@elysee-events.de");
        ReflectionTestUtils.setField(initializer, "adminBootstrapSecretPath",
                tmp.resolve("admin-bootstrap.txt").toString());
    }

    @Test
    void usesExplicitPasswordWhenSet() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "Explicit-Pass-2026!");
        when(userRepository.findByEmail("admin@elysee-events.de")).thenReturn(Optional.empty());

        initializer.bootstrapAdmin();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("admin@elysee-events.de", saved.getEmail());
        assertEquals("bcrypt:Explicit-Pass-2026!", saved.getPasswordHash(),
                "explicit password must be encoded as-is");
        assertTrue(saved.isForcePwChange(),
                "force_pw_change must always be true so first login replaces the bootstrap password");
        assertEquals("ADMIN", saved.getRole());

        // Bootstrap secret file must NOT be written when the password came from config.
        assertFalse(Files.exists(tmp.resolve("admin-bootstrap.txt")),
                "bootstrap file is only written for generated passwords");
    }

    @Test
    void generatesRandomPasswordWhenAdminPasswordBlank() throws Exception {
        ReflectionTestUtils.setField(initializer, "adminPassword", "");
        when(userRepository.findByEmail("admin@elysee-events.de")).thenReturn(Optional.empty());

        initializer.bootstrapAdmin();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        // The bcrypt-encoded hash exposes the input we generated (mock prefixes "bcrypt:").
        String generated = saved.getPasswordHash().substring("bcrypt:".length());
        assertTrue(generated.length() >= 24, "generated password must have substantial entropy");
        assertTrue(generated.matches("^[A-Za-z0-9_-]+$"),
                "URL-safe base64 alphabet only (no padding or ambiguous chars): " + generated);
        assertTrue(saved.isForcePwChange(), "force_pw_change must be true so it's replaced on first login");

        // Bootstrap file MUST exist and contain the generated password — operator picks it
        // up out-of-band, then deletes it.
        Path file = tmp.resolve("admin-bootstrap.txt");
        assertTrue(Files.exists(file), "bootstrap secret file must be written");
        String content = Files.readString(file);
        assertTrue(content.contains("password=" + generated),
                "bootstrap file must record the generated password literally");
        assertTrue(content.contains("admin@elysee-events.de"));
        assertTrue(content.contains("DELETE THIS FILE"),
                "bootstrap file must scream at the operator to delete it");
    }

    @Test
    void generatesRandomPasswordWhenAdminPasswordNull() {
        ReflectionTestUtils.setField(initializer, "adminPassword", (String) null);
        when(userRepository.findByEmail("admin@elysee-events.de")).thenReturn(Optional.empty());

        initializer.bootstrapAdmin();

        verify(userRepository).save(any(User.class));
        assertTrue(Files.exists(tmp.resolve("admin-bootstrap.txt")),
                "null adminPassword must trigger generation + bootstrap-file write");
    }

    @Test
    void doesNotRecreateExistingAdmin() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "Explicit-Pass-2026!");
        User existing = new User();
        existing.setEmail("admin@elysee-events.de");
        when(userRepository.findByEmail("admin@elysee-events.de")).thenReturn(Optional.of(existing));

        initializer.bootstrapAdmin();

        verify(userRepository, never()).save(any(User.class));
        assertFalse(Files.exists(tmp.resolve("admin-bootstrap.txt")),
                "bootstrap file must not be created for already-provisioned admin");
    }

    @Test
    void noEmailMeansNoBootstrap() {
        ReflectionTestUtils.setField(initializer, "adminEmail", "");
        ReflectionTestUtils.setField(initializer, "adminPassword", "");

        initializer.bootstrapAdmin();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void generateRandomPasswordEntropy() {
        // Two consecutive draws must differ. (Probability of collision with 192 bits of
        // SecureRandom is ~2^-192 — failure means a real bug, not flakiness.)
        String a = DataInitializer.generateRandomPassword();
        String b = DataInitializer.generateRandomPassword();
        assertNotEquals(a, b);
        assertTrue(a.length() >= 32, "Base64-encoded 24 bytes ≥ 32 chars");
        assertTrue(a.matches("^[A-Za-z0-9_-]+$"));
    }
}
