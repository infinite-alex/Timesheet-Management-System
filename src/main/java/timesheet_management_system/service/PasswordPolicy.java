package timesheet_management_system.service;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static String violation(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Parola trebuie să aibă cel puțin " + MIN_LENGTH + " caractere.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return "Parola e prea lungă (maximum " + MAX_BYTES + " de caractere).";
        }
        return null;
    }
}
