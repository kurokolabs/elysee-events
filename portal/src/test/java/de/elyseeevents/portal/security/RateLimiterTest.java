package de.elyseeevents.portal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void firstAttemptIsAllowed() {
        assertTrue(rateLimiter.isAllowed("login", "192.168.1.1"));
    }

    @Test
    void fiveAttemptsAllAllowed() {
        String action = "login";
        String ip = "192.168.1.2";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordAttempt(action, ip);
        }
        // After 5 recorded attempts, the 6th check should be blocked,
        // but during the 5 attempts the checks should still pass up to the limit
        // Re-check: isAllowed checks size < 5, so after 5 records size==5, isAllowed returns false
        // Let's test that the first 5 are allowed by checking before each record
        RateLimiter fresh = new RateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(fresh.isAllowed(action, ip), "Attempt " + (i + 1) + " should be allowed");
            fresh.recordAttempt(action, ip);
        }
    }

    @Test
    void sixthAttemptIsBlocked() {
        String action = "login";
        String ip = "192.168.1.3";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordAttempt(action, ip);
        }

        assertFalse(rateLimiter.isAllowed(action, ip), "6th attempt should be blocked");
    }

    @Test
    void differentIpsTrackedSeparately() {
        String action = "login";
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordAttempt(action, ip1);
        }

        assertFalse(rateLimiter.isAllowed(action, ip1), "ip1 should be blocked after 5 attempts");
        assertTrue(rateLimiter.isAllowed(action, ip2), "ip2 should still be allowed");
    }

    @Test
    void differentActionsTrackedSeparately() {
        String ip = "10.0.0.1";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordAttempt("login", ip);
        }

        assertFalse(rateLimiter.isAllowed("login", ip), "login action should be blocked");
        assertTrue(rateLimiter.isAllowed("register", ip), "register action should still be allowed");
    }

    @Test
    void resetClearsAttempts() {
        String action = "login";
        String ip = "10.0.0.3";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordAttempt(action, ip);
        }
        assertFalse(rateLimiter.isAllowed(action, ip));

        rateLimiter.reset(action, ip);

        assertTrue(rateLimiter.isAllowed(action, ip), "Should be allowed after reset");
    }
}
