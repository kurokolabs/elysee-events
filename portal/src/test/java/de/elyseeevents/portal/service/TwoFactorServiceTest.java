package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TwoFactorService twoFactorService;

    @Test
    void generateAndStoreCodeReturns6DigitCode() {
        String code = twoFactorService.generateAndStoreCode(1L);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"), "Code should contain only digits");
    }

    @Test
    void generateAndStoreCodeStoresCodeWithFutureExpiry() {
        twoFactorService.generateAndStoreCode(1L);

        verify(userRepository).storeTwoFaCode(eq(1L), anyString(), argThat(expiresAt -> {
            LocalDateTime expires = LocalDateTime.parse(expiresAt, FMT);
            return expires.isAfter(LocalDateTime.now());
        }));
    }

    @Test
    void verifyCodeReturnsTrueForCorrectCode() {
        User user = new User();
        user.setId(1L);
        user.setTwoFaCode("123456");
        user.setTwoFaExpires(LocalDateTime.now().plusMinutes(5).format(FMT));

        assertTrue(twoFactorService.verifyCode(user, "123456"));
    }

    @Test
    void verifyCodeReturnsFalseForWrongCode() {
        User user = new User();
        user.setId(1L);
        user.setTwoFaCode("123456");
        user.setTwoFaExpires(LocalDateTime.now().plusMinutes(5).format(FMT));

        assertFalse(twoFactorService.verifyCode(user, "999999"));
    }

    @Test
    void verifyCodeReturnsFalseForExpiredCode() {
        User user = new User();
        user.setId(1L);
        user.setTwoFaCode("123456");
        user.setTwoFaExpires(LocalDateTime.now().minusMinutes(1).format(FMT));

        assertFalse(twoFactorService.verifyCode(user, "123456"));
    }

    @Test
    void verifyCodeReturnsFalseForNullCode() {
        User user = new User();
        user.setId(1L);
        user.setTwoFaCode(null);
        user.setTwoFaExpires(null);

        assertFalse(twoFactorService.verifyCode(user, "123456"));
    }

    @Test
    void verifyCodeReturnsFalseForBlankInput() {
        User user = new User();
        user.setId(1L);
        user.setTwoFaCode("123456");
        user.setTwoFaExpires(LocalDateTime.now().plusMinutes(5).format(FMT));

        assertFalse(twoFactorService.verifyCode(user, ""));
        assertFalse(twoFactorService.verifyCode(user, "   "));
        assertFalse(twoFactorService.verifyCode(user, null));
    }
}
