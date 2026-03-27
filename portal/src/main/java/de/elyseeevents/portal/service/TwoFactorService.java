package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Service
public class TwoFactorService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int TRUST_HOURS = 2;
    private static final String TRUST_COOKIE = "2fa_trusted";
    private static final String HMAC_SECRET = "e1y5ee-2fa-tru5t-k3y-s3cr3t";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void setTrustedDevice(Long userId, HttpServletResponse response) {
        long expiry = System.currentTimeMillis() + (TRUST_HOURS * 3600L * 1000L);
        String payload = userId + ":" + expiry;
        String hmac = hmacSha256(payload);
        String value = payload + ":" + hmac;

        Cookie cookie = new Cookie(TRUST_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/portal");
        cookie.setMaxAge(TRUST_HOURS * 3600);
        response.addCookie(cookie);
    }

    public boolean isDeviceTrusted(Long userId, HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        for (Cookie c : request.getCookies()) {
            if (TRUST_COOKIE.equals(c.getName())) {
                try {
                    String[] parts = c.getValue().split(":");
                    if (parts.length != 3) return false;
                    long cookieUserId = Long.parseLong(parts[0]);
                    long expiry = Long.parseLong(parts[1]);
                    String hmac = parts[2];
                    String expectedHmac = hmacSha256(parts[0] + ":" + parts[1]);
                    return cookieUserId == userId
                            && expiry > System.currentTimeMillis()
                            && hmac.equals(expectedHmac);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC failed", e);
        }
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
