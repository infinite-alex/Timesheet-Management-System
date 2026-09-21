package timesheet_management_system.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed attempts per key. After {@code maxAttempts} failures within {@code window},
 * the key is blocked for {@code window}. State is in memory: it resets when the app restarts.
 */
public class AttemptLimiter {

    private static final int PRUNE_THRESHOLD = 10_000;

    private static final class State {
        int failures;
        Instant windowStart;
        Instant blockedUntil;
    }

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        State state = states.get(key);
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return state.blockedUntil != null && state.blockedUntil.isAfter(clock.instant());
        }
    }

    public void recordFailure(String key) {
        if (states.size() > PRUNE_THRESHOLD) {
            prune();
        }
        Instant now = clock.instant();
        State state = states.computeIfAbsent(key, k -> new State());
        synchronized (state) {
            if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
                return;
            }
            if (state.windowStart == null || !state.windowStart.plus(window).isAfter(now)) {
                state.failures = 0;
                state.windowStart = now;
                state.blockedUntil = null;
            }
            state.failures++;
            if (state.failures >= maxAttempts) {
                state.blockedUntil = now.plus(window);
            }
        }
    }

    public void reset(String key) {
        states.remove(key);
    }

    public void clear() {
        states.clear();
    }

    private void prune() {
        Instant now = clock.instant();
        states.entrySet().removeIf(entry -> {
            State state = entry.getValue();
            synchronized (state) {
                boolean blocked = state.blockedUntil != null && state.blockedUntil.isAfter(now);
                return !blocked && (state.windowStart == null || !state.windowStart.plus(window).isAfter(now));
            }
        });
    }
}
