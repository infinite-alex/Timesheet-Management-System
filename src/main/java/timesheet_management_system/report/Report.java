package timesheet_management_system.report;

import java.util.List;

public record Report(ReportView view, List<ReportGroup> groups, int totalMinutes, int totalEntries) {

    public String duration() {
        return Durations.format(totalMinutes);
    }

    public int share(int minutes) {
        return totalMinutes == 0 ? 0 : (int) Math.round(minutes * 100.0 / totalMinutes);
    }

    public static Report empty(ReportView view) {
        return new Report(view, List.of(), 0, 0);
    }
}
