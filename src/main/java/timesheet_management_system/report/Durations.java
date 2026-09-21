package timesheet_management_system.report;

public final class Durations {

    private Durations() {
    }

    public static String format(int minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
