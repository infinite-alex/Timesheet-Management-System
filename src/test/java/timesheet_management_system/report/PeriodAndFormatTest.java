package timesheet_management_system.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.report.PeriodResolver.DateRange;

class PeriodAndFormatTest {

    private static DateRange resolve(String period, String from, String to, LocalDate today) {
        return PeriodResolver.resolve(period, from, to, today);
    }

    @Test
    void currentMonth_isTheDefault() {
        LocalDate today = LocalDate.of(2026, 9, 21);

        assertThat(resolve("luna", null, null, today)).isEqualTo(new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        assertThat(resolve(null, null, null, today)).isEqualTo(resolve("luna", null, null, today));
        assertThat(resolve("ceva-necunoscut", null, null, today)).isEqualTo(resolve("luna", null, null, today));
        assertThat(resolve("'; DROP TABLE x;--", null, null, today)).isEqualTo(resolve("luna", null, null, today));
    }

    @Test
    void previousMonth_handlesYearChangeAndLeapYears() {
        assertThat(resolve("luna-trecuta", null, null, LocalDate.of(2026, 1, 15)))
            .isEqualTo(new DateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)));
        assertThat(resolve("luna-trecuta", null, null, LocalDate.of(2026, 3, 31)))
            .isEqualTo(new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));
        assertThat(resolve("luna-trecuta", null, null, LocalDate.of(2024, 3, 10)))
            .isEqualTo(new DateRange(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)));
    }

    @Test
    void year_and_allHistory() {
        assertThat(resolve("an", null, null, LocalDate.of(2026, 9, 21)))
            .isEqualTo(new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        assertThat(resolve("tot", null, null, LocalDate.of(2026, 9, 21)))
            .isEqualTo(new DateRange(PeriodResolver.MIN, PeriodResolver.MAX));
    }

    @Test
    void customInterval_isParsed_andMayBeOpenEnded() {
        LocalDate today = LocalDate.of(2026, 9, 21);

        assertThat(resolve("interval", "2026-03-01", "2026-03-31", today))
            .isEqualTo(new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));
        assertThat(resolve("interval", "2026-03-01", "", today).to()).isEqualTo(PeriodResolver.MAX);
        assertThat(resolve("interval", "", "2026-03-31", today).from()).isEqualTo(PeriodResolver.MIN);
        assertThat(resolve("interval", null, null, today)).isEqualTo(new DateRange(PeriodResolver.MIN, PeriodResolver.MAX));
        assertThat(resolve("interval", "2026-03-05", "2026-03-05", today).from()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void invalidIntervals_areRejectedWithAClearMessage() {
        LocalDate today = LocalDate.of(2026, 9, 21);

        assertThatThrownBy(() -> resolve("interval", "2026-09-30", "2026-09-01", today))
            .isInstanceOf(BadRequestException.class).hasMessageContaining("după data de sfârșit");
        for (String bad : List.of("azi", "31-12-2026", "2026-13-01", "2026-02-30", "<script>")) {
            assertThatThrownBy(() -> resolve("interval", bad, "", today))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("nu este validă");
        }
        assertThatThrownBy(() -> resolve("interval", "1999-12-31", "", today)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> resolve("interval", "", "3000-01-01", today)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void durations_areFormattedAsHoursAndMinutes() {
        assertThat(Durations.format(0)).isEqualTo("0h 0m");
        assertThat(Durations.format(59)).isEqualTo("0h 59m");
        assertThat(Durations.format(60)).isEqualTo("1h 0m");
        assertThat(Durations.format(135)).isEqualTo("2h 15m");
        assertThat(Durations.format(1440)).isEqualTo("24h 0m");
        assertThat(Durations.format(6000)).isEqualTo("100h 0m");
    }

    @Test
    void share_isRoundedAndSafeWhenEmpty() {
        Report report = new Report(ReportView.ANGAJATI, List.of(), 300, 3);

        assertThat(report.share(100)).isEqualTo(33);
        assertThat(report.share(200)).isEqualTo(67);
        assertThat(report.share(300)).isEqualTo(100);
        assertThat(Report.empty(ReportView.CLIENTI).share(50)).isZero();
    }

    @Test
    void viewSlug_fallsBackToEmployees() {
        assertThat(ReportView.fromSlug("clienti")).isEqualTo(ReportView.CLIENTI);
        assertThat(ReportView.fromSlug("sarcini")).isEqualTo(ReportView.SARCINI);
        assertThat(ReportView.fromSlug("angajati")).isEqualTo(ReportView.ANGAJATI);
        assertThat(ReportView.fromSlug(null)).isEqualTo(ReportView.ANGAJATI);
        assertThat(ReportView.fromSlug("altceva")).isEqualTo(ReportView.ANGAJATI);
    }

    @Test
    void csvText_isEscapedAndProtectedAgainstFormulas() {
        assertThat(CsvExporter.text("Firma SRL")).isEqualTo("Firma SRL");
        assertThat(CsvExporter.text(null)).isEmpty();
        assertThat(CsvExporter.text("A; B")).isEqualTo("\"A; B\"");
        assertThat(CsvExporter.text("Spune \"salut\"")).isEqualTo("\"Spune \"\"salut\"\"\"");
        assertThat(CsvExporter.text("linia1\nlinia2")).isEqualTo("\"linia1\nlinia2\"");
        for (String start : List.of("=", "+", "-", "@")) {
            assertThat(CsvExporter.text(start + "cmd|' /C calc'!A0")).startsWith("'" + start);
        }
        assertThat(CsvExporter.text("\t=1+1")).startsWith("'");
    }

    @Test
    void csvHours_useTheRomanianDecimalComma() {
        assertThat(CsvExporter.hours(135)).isEqualTo("2,25");
        assertThat(CsvExporter.hours(0)).isEqualTo("0,00");
        assertThat(CsvExporter.hours(20)).isEqualTo("0,33");
    }

    @Test
    void csv_hasBomHeaderRowsAndATotal() {
        Report report = new Report(ReportView.CLIENTI, List.of(
            new ReportGroup("Alfa SRL", null, 135, 2, List.of(new ReportLine("Ana", 135, 2)))), 135, 2);

        String csv = new String(CsvExporter.toCsv(report), StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");

        assertThat(csv).startsWith("﻿");
        assertThat(lines[0]).isEqualTo("﻿Client;Angajat;Minute;Ore;Nr. pontaje");
        assertThat(lines[1]).isEqualTo("Alfa SRL;Ana;135;2,25;2");
        assertThat(lines[2]).isEqualTo("TOTAL;;135;2,25;2");
        assertThat(lines).hasSize(3);
    }

    @Test
    void csvHeader_followsTheView() {
        assertThat(new String(CsvExporter.toCsv(Report.empty(ReportView.ANGAJATI)), StandardCharsets.UTF_8))
            .contains("Angajat;Client;Minute");
        assertThat(new String(CsvExporter.toCsv(Report.empty(ReportView.SARCINI)), StandardCharsets.UTF_8))
            .contains("Sarcină;Angajat;Minute");
    }
}
