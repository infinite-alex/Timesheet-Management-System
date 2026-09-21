package timesheet_management_system.report;

import java.time.LocalDate;
import java.util.Set;

public record ReportFilter(LocalDate from, LocalDate to, Set<Long> clientIds, Set<Long> employeeIds, String task) {

    public static final long NO_CLIENT = 0L;
    public static final String NO_TASK = "-";
}
