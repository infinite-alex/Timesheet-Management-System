package timesheet_management_system.report;

import java.util.List;

public record ReportGroup(String label, String note, int minutes, int entries, List<ReportLine> lines) {

    public String duration() {
        return Durations.format(minutes);
    }
}
