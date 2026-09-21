package timesheet_management_system.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Brute-force protection for /login and /register. Login failures are counted per username
 * and per client IP; registrations are counted per client IP.
 */
@Component
public class LoginThrottle {

    private final AttemptLimiter loginByUsername;
    private final AttemptLimiter loginByIp;
    private final AttemptLimiter registrationByIp;
    private final long lockoutMinutes;

    @Autowired
    public LoginThrottle(
            @Value("${app.security.max-login-failures:5}") int maxLoginFailures,
            @Value("${app.security.max-login-failures-per-ip:20}") int maxLoginFailuresPerIp,
            @Value("${app.security.max-registrations-per-ip:10}") int maxRegistrationsPerIp,
            @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this(maxLoginFailures, maxLoginFailuresPerIp, maxRegistrationsPerIp, lockoutMinutes, Clock.systemUTC());
    }

    LoginThrottle(int maxLoginFailures, int maxLoginFailuresPerIp, int maxRegistrationsPerIp,
            long lockoutMinutes, Clock clock) {
        Duration window = Duration.ofMinutes(lockoutMinutes);
        this.lockoutMinutes = lockoutMinutes;
        this.loginByUsername = new AttemptLimiter(maxLoginFailures, window, clock);
        this.loginByIp = new AttemptLimiter(maxLoginFailuresPerIp, window, clock);
        this.registrationByIp = new AttemptLimiter(maxRegistrationsPerIp, window, clock);
    }

    public long lockoutMinutes() {
        return lockoutMinutes;
    }

    public boolean isLoginBlocked(String username, String ip) {
        return loginByUsername.isBlocked(normalize(username)) || loginByIp.isBlocked(ip);
    }

    public void loginFailed(String username, String ip) {
        loginByUsername.recordFailure(normalize(username));
        loginByIp.recordFailure(ip);
    }

    public void loginSucceeded(String username) {
        loginByUsername.reset(normalize(username));
    }

    /** Counts the attempt and returns false when this IP has registered too often. */
    public boolean registrationAllowed(String ip) {
        if (registrationByIp.isBlocked(ip)) {
            return false;
        }
        registrationByIp.recordFailure(ip);
        return true;
    }

    public void clear() {
        loginByUsername.clear();
        loginByIp.clear();
        registrationByIp.clear();
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
