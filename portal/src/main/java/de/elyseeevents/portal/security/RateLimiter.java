package de.elyseeevents.portal.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimiter {
    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_SECONDS = 900;
    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String action, String ip) {
        String key = action + ":" + ip;
        Deque<Instant> deque = attempts.get(key);
        if (deque == null) return true;
        pruneExpired(deque);
        return deque.size() < MAX_ATTEMPTS;
    }

    public void recordAttempt(String action, String ip) {
        String key = action + ":" + ip;
        attempts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(Instant.now());
        pruneExpired(attempts.get(key));
    }

    public void reset(String action, String ip) {
        attempts.remove(action + ":" + ip);
    }

    private void pruneExpired(Deque<Instant> deque) {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void cleanup() {
        Iterator<Map.Entry<String, Deque<Instant>>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Instant>> entry = it.next();
            pruneExpired(entry.getValue());
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }
}
