package timesheet_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import timesheet_management_system.model.Role;

public record EmployeeCreateDto(
        @NotBlank(message = "Numele este obligatoriu.")
        @Size(max = 100, message = "Numele poate avea cel mult 100 de caractere.") String name,
        @NotNull(message = "Username-ul este obligatoriu.")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,30}",
            message = "Username-ul trebuie să aibă 3-30 de caractere: litere, cifre, punct, minus sau underscore.") String username,
        @NotBlank(message = "Parola este obligatorie.")
        @Size(min = 8, max = 72, message = "Parola trebuie să aibă între 8 și 72 de caractere.") String password,
        @NotNull(message = "Rolul este obligatoriu.") Role role) {
}
