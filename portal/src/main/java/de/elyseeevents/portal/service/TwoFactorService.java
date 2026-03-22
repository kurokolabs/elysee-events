package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TwoFactorService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;

    public TwoFactorService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateAndStoreCode(Long userId) {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        String codeStr = code.toString();
        String expiresAt = LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES).format(FMT);
        userRepository.storeTwoFaCode(userId, codeStr, expiresAt);
        return codeStr;
    }

    public boolean verifyCode(User user, String inputCode) {
        if (user.getTwoFaCode() == null || user.getTwoFaExpires() == null) {
            return false;
        }
        if (inputCode == null || inputCode.isBlank()) {
            return false;
        }

        // Check expiry
        LocalDateTime expires = LocalDateTime.parse(user.getTwoFaExpires(), FMT);
        if (LocalDateTime.now().isAfter(expires)) {
            userRepository.clearTwoFaCode(user.getId());
            return false;
        }

        boolean valid = user.getTwoFaCode().equals(inputCode.trim());
        if (valid) {
            userRepository.clearTwoFaCode(user.getId());
        }
        return valid;
    }
}
