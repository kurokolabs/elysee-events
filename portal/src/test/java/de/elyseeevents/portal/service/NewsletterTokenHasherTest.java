package de.elyseeevents.portal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewsletterTokenHasherTest {

    private static NewsletterTokenHasher withPepper(String pepper) {
        return new NewsletterTokenHasher(pepper);
    }

    @Test
    void tokenIsDeterministicForSameIdAndPepper() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        assertEquals(h.tokenFor(42L), h.tokenFor(42L));
        assertEquals(h.tokenFor(1L), h.tokenFor(1L));
    }

    @Test
    void differentIdsProduceDifferentTokens() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        assertNotEquals(h.tokenFor(1L), h.tokenFor(2L));
        assertNotEquals(h.tokenFor(100L), h.tokenFor(101L));
    }

    @Test
    void differentPeppersProduceDifferentTokens() {
        NewsletterTokenHasher a = withPepper("pepper-A");
        NewsletterTokenHasher b = withPepper("pepper-B");
        assertNotEquals(a.tokenFor(42L), b.tokenFor(42L));
    }

    @Test
    void tokenIsHex64Chars() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        String token = h.tokenFor(42L);
        assertEquals(64, token.length(), "SHA-256 hex must be 64 chars");
        assertTrue(token.matches("[0-9a-f]+"), "must be lowercase hex");
    }

    @Test
    void verifyAcceptsValidToken() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        String token = h.tokenFor(42L);
        assertTrue(h.verify(42L, token));
    }

    @Test
    void verifyRejectsTokenForWrongId() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        String token = h.tokenFor(42L);
        assertFalse(h.verify(43L, token));
    }

    @Test
    void verifyRejectsNullToken() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        assertFalse(h.verify(42L, null));
    }

    @Test
    void verifyRejectsTamperedToken() {
        NewsletterTokenHasher h = withPepper("test-pepper-1");
        String token = h.tokenFor(42L);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("0") ? "1" : "0");
        assertFalse(h.verify(42L, tampered));
    }

    @Test
    void verifyRejectsTokenFromDifferentPepper() {
        NewsletterTokenHasher a = withPepper("pepper-A");
        NewsletterTokenHasher b = withPepper("pepper-B");
        assertFalse(a.verify(42L, b.tokenFor(42L)));
    }
}
