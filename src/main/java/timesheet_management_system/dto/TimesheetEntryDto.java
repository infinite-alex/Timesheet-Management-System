package timesheet_management_system.dto;

import java.time.LocalDate;
import java.util.List;

import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

public record TimesheetEntryDto(Long id, LocalDate date,Long employeeId,Long clientId,WorkingMonth workingmonth,
        int totalMinutes,List<String> actions, String extranote) {
    

}