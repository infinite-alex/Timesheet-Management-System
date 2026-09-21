package timesheet_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDto(
        Long id,
        @NotBlank(message = "Denumirea clientului este obligatorie.")
        @Size(max = 255, message = "Denumirea poate avea cel mult 255 de caractere.") String name) {

}
