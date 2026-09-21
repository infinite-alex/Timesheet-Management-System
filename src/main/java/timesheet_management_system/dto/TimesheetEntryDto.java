package timesheet_management_system.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import timesheet_management_system.model.WorkingMonth;

public record TimesheetEntryDto(
        Long id,
        @NotNull(message = "Data este obligatorie.") LocalDate date,
        Long employeeId,
        Long clientId,
        @NotNull(message = "Luna este obligatorie.") WorkingMonth workingmonth,
        @Min(value = 1, message = "Durata trebuie să fie de cel puțin 1 minut.")
        @Max(value = 1440, message = "Durata nu poate depăși 24 de ore.") int totalMinutes,
        List<String> actions,
        @Size(max = 255, message = "Nota poate avea cel mult 255 de caractere.") String extranote,
        String employeeName,
        String clientName) {

}
