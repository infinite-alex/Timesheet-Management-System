package timesheet_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import timesheet_management_system.model.Role;

public record EmployeeUpdateDto(
        @NotBlank(message = "Numele este obligatoriu.")
        @Size(max = 100, message = "Numele poate avea cel mult 100 de caractere.") String name,
        @NotNull(message = "Rolul este obligatoriu.") Role role,
        @NotNull(message = "Starea contului este obligatorie.") Boolean active,
        String newPassword) {
}
