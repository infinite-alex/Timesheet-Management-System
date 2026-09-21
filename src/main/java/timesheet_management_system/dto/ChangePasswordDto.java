package timesheet_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordDto(
        @NotBlank(message = "Introdu parola curentă.") String currentPassword,
        @NotBlank(message = "Introdu parola nouă.") String newPassword,
        @NotBlank(message = "Confirmă parola nouă.") String confirmPassword) {
}
