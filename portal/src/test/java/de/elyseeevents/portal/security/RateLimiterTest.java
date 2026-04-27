package de.elyseeevents.portal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void tryAcquireAllowsExactlyFiveAttempts() {
        String action = "login";
        String ip = "10.0.0.4";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(action, ip), "Attempt " + (i + 1) + " must be granted");
        }
        assertFalse(rateLimiter.tryAcquire(action, ip), "6th tryAcquire must be denied");
        assertFalse(rateLimiter.tryAcquire(action, ip), "7th tryAcquire must also be denied");
    }

    @Test
    void tryAcquireIsAtomicUnderConcurrency() throws InterruptedException {
        // 20 concurrent threads all calling tryAcquire on the same key:
        // exactly 5 must succeed, 15 must fail. Previously isAllowed/recordAttempt had a
        // check-then-act race that could allow more than MAX_ATTEMPTS to slip through.
        String action = "login";
        String ip = "10.0.0.5";

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger granted = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (rateLimiter.tryAcquire(action, ip)) granted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "threads must finish");
        pool.shutdown();

        assertEquals(5, granted.get(), "tryAcquire must grant exactly MAX_ATTEMPTS=5 slots");
    }

    @Test
    void tryAcquireWithCustomMaxAllowsExactlyThatMany() {
        // The per-action overload must apply the supplied threshold, NOT the default 5.
        // Used by RateLimitFilter to give per-username login a different (stricter) limit
        // than per-IP login.
        String action = "LOGIN_USER";
        String key = "alice@example.com";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(action, key, 10), "Attempt " + (i + 1) + " of 10 must be granted");
        }
        assertFalse(rateLimiter.tryAcquire(action, key, 10), "11th must be denied");
    }

    @Test
    void perUserAndPerIpKeysAreIndependent() {
        // Distributed brute-force scenario: 5 different IPs each hit the same account once.
        // Per-IP buckets stay under their 5/15min limit, BUT the per-user counter accumulates
        // across IPs.
        String username = "victim@example.com";

        // Five distinct IPs, each can login (under per-IP limit).
        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiter.tryAcquire("LOGIN", "203.0.113." + i),
                    "IP " + i + " under its own per-IP limit");
        }
        // But the same victim username, hit 5 times across those IPs, eats 5 per-user slots.
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire("LOGIN_USER", username, 10),
                    "Per-user attempt " + (i + 1) + " of 10 still allowed");
        }
        // Five more IPs add five more per-user attempts → 10 total → 11th must be blocked.
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire("LOGIN_USER", username, 10),
                    "Per-user attempt " + (i + 6) + " of 10 still allowed");
        }
        assertFalse(rateLimiter.tryAcquire("LOGIN_USER", username, 10),
                "11th per-user attempt must be blocked even with fresh IPs");
    }

    @Test
    void tryAcquireDoesNotIncrementOnDenial() {
        String action = "login";
        String ip = "10.0.0.6";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(action, ip));
        }
        // Multiple denials should not grow the internal deque indefinitely.
        // After reset, exactly 5 more acquires are allowed (window, not cumulative).
        for (int i = 0; i < 3; i++) {
            assertFalse(rateLimiter.tryAcquire(action, ip));
        }
        rateLimiter.reset(action, ip);
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(action, ip), "After reset attempt " + (i + 1) + " must succeed");
        }
        assertFalse(rateLimiter.tryAcquire(action, ip));
    }
}
