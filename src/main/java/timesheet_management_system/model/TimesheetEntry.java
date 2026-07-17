package timesheet_management_system.model;

import java.time.LocalDate;
import java.util.List;

public class TimesheetEntry {
    private LocalDate date;
    private Employee employee;
    private Client client;
    private String workingmonth;
    private int totalMinutes;
    private List<String> actions;
    private String extranote;
}
