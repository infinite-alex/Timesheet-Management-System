package timesheet_management_system.report;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class CsvExporter {

    private static final char SEPARATOR = ';';
    private static final Locale RO = Locale.forLanguageTag("ro-RO");

    private CsvExporter() {
    }

    public static byte[] toCsv(Report report) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(header(report.view())).append("\r\n");
        for (ReportGroup group : report.groups()) {
            for (ReportLine line : group.lines()) {
                csv.append(text(group.label())).append(SEPARATOR)
                    .append(text(line.label())).append(SEPARATOR)
                    .append(line.minutes()).append(SEPARATOR)
                    .append(hours(line.minutes())).append(SEPARATOR)
                    .append(line.entries()).append("\r\n");
            }
        }
        csv.append("TOTAL").append(SEPARATOR).append(SEPARATOR)
            .append(report.totalMinutes()).append(SEPARATOR)
            .append(hours(report.totalMinutes())).append(SEPARATOR)
            .append(report.totalEntries()).append("\r\n");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String header(ReportView view) {
        String group = switch (view) {
            case CLIENTI -> "Client";
            case SARCINI -> "Sarcină";
            default -> "Angajat";
        };
        String detail = view == ReportView.ANGAJATI ? "Client" : "Angajat";
        return group + SEPARATOR + detail + SEPARATOR + "Minute" + SEPARATOR + "Ore" + SEPARATOR + "Nr. pontaje";
    }

    static String hours(int minutes) {
        return String.format(RO, "%.2f", minutes / 60.0);
    }

    static String text(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        boolean needsQuotes = safe.indexOf(SEPARATOR) >= 0 || safe.indexOf('"') >= 0
            || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0;
        if (needsQuotes) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
