package timesheet_management_system.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AttemptLimiterTest {

    /** A clock the test can move forward by hand. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-09-21T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final AttemptLimiter limiter = new AttemptLimiter(3, Duration.ofMinutes(15), clock);

    @Test
    void blocksOnlyAfterTheMaximumNumberOfFailures() {
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");
        assertThat(limiter.isBlocked("ana")).isFalse();

        limiter.recordFailure("ana");
        assertThat(limiter.isBlocked("ana")).isTrue();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("ana");
        }

        assertThat(limiter.isBlocked("ana")).isTrue();
        assertThat(limiter.isBlocked("bob")).isFalse();
    }

    @Test
    void blockEndsAfterTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("ana");
        }
        clock.advance(Duration.ofMinutes(14));
        assertThat(limiter.isBlocked("ana")).isTrue();

        clock.advance(Duration.ofMinutes(2));
        assertThat(limiter.isBlocked("ana")).isFalse();

        limiter.recordFailure("ana");
        assertThat(limiter.isBlocked("ana")).isFalse();
    }

    @Test
    void failuresOlderThanTheWindowDoNotAccumulate() {
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");
        clock.advance(Duration.ofMinutes(16));
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");

        assertThat(limiter.isBlocked("ana")).isFalse();
    }

    @Test
    void resetForgetsThePastFailures() {
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");
        limiter.reset("ana");
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");

        assertThat(limiter.isBlocked("ana")).isFalse();
    }

    @Test
    void failuresWhileBlockedDoNotExtendTheBlock() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("ana");
        }
        clock.advance(Duration.ofMinutes(10));
        limiter.recordFailure("ana");
        limiter.recordFailure("ana");

        clock.advance(Duration.ofMinutes(6));
        assertThat(limiter.isBlocked("ana")).isFalse();
    }
}
