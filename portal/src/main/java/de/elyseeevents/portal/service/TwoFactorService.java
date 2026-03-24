package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TwoFactorService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateAndStoreCode(Long userId) {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        String codeStr = code.toString();
        String hashedCode = passwordEncoder.encode(codeStr);
        String expiresAt = LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES).format(FMT);
        userRepository.storeTwoFaCode(userId, hashedCode, expiresAt);
        userRepository.resetTwoFaAttempts(userId);
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

        // Check attempt limit
        int attempts = userRepository.getTwoFaAttempts(user.getId());
        if (attempts >= MAX_ATTEMPTS) {
            userRepository.clearTwoFaCode(user.getId());
            return false;
        }

        boolean valid = passwordEncoder.matches(inputCode.trim(), user.getTwoFaCode());
        if (valid) {
            userRepository.clearTwoFaCode(user.getId());
        } else {
            userRepository.incrementTwoFaAttempts(user.getId());
        }
        return valid;
    }
}
