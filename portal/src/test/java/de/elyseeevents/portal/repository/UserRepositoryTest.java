package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveInsertsAndReturnsId() {
        User user = new User();
        user.setEmail("repo-test-" + System.nanoTime() + "@test.de");
        user.setPasswordHash("$2a$12$dummyhash");
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setForcePwChange(false);
        user.setTwoFaEnabled(false);

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    void findByEmailReturnsUser() {
        // DataInitializer creates admin@test.de
        Optional<User> user = userRepository.findByEmail("admin@test.de");

        assertTrue(user.isPresent());
        assertEquals("admin@test.de", user.get().getEmail());
        assertEquals("ADMIN", user.get().getRole());
    }

    @Test
    void findByEmailReturnsEmptyForUnknown() {
        Optional<User> user = userRepository.findByEmail("nonexistent@test.de");

        assertTrue(user.isEmpty());
    }

    @Test
    void updatePasswordChangesHash() {
        // Create a test user
        User user = new User();
        user.setEmail("pwchange-" + System.nanoTime() + "@test.de");
        user.setPasswordHash("old-hash");
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setForcePwChange(true);
        user.setTwoFaEnabled(false);
        user = userRepository.save(user);

        // Update password
        userRepository.updatePassword(user.getId(), "new-hash");

        // Verify
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("new-hash", updated.getPasswordHash());
        assertFalse(updated.isForcePwChange(), "force_pw_change should be cleared after password update");
    }

    @Test
    void storeTwoFaCodeAndClearTwoFaCode() {
        // Create a test user
        User user = new User();
        user.setEmail("twofa-" + System.nanoTime() + "@test.de");
        user.setPasswordHash("hash");
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setForcePwChange(false);
        user.setTwoFaEnabled(true);
        user = userRepository.save(user);

        // Store 2FA code
        userRepository.storeTwoFaCode(user.getId(), "654321", "2026-12-31 23:59:59");

        User withCode = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("654321", withCode.getTwoFaCode());
        assertEquals("2026-12-31 23:59:59", withCode.getTwoFaExpires());

        // Clear 2FA code
        userRepository.clearTwoFaCode(user.getId());

        User cleared = userRepository.findById(user.getId()).orElseThrow();
        assertNull(cleared.getTwoFaCode());
        assertNull(cleared.getTwoFaExpires());
    }
}
