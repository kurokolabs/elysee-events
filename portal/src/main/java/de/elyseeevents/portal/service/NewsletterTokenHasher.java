package de.elyseeevents.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Stateless, deterministic unsubscribe-token computation: HMAC-SHA256(pepper, subscriberId).
 *
 * Unsubscribe URL format: /newsletter/abmelden?id=<id>&token=<hmac>
 * The DB stores no secret material — a DB breach yields only subscriber IDs, which are not
 * actionable without the server-side pepper. Weekly senders can always recompute the token
 * from subscriber.id without reading any per-row secret.
 */
@Component
public class NewsletterTokenHasher {

    private final String pepper;

    public NewsletterTokenHasher(@Value("${app.newsletter.token-pepper}") String pepper) {
        this.pepper = pepper;
    }

    public String tokenFor(long subscriberId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(Long.toString(subscriberId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    public boolean verify(long subscriberId, String token) {
        if (token == null) return false;
        String expected = tokenFor(subscriberId);
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
