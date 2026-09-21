package timesheet_management_system.report;

public record ReportLine(String label, int minutes, int entries) {

    public String duration() {
        return Durations.format(minutes);
    }
}
