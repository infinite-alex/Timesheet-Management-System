package timesheet_management_system.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import timesheet_management_system.IntegrationTestBase;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

class ReportServiceTest extends IntegrationTestBase {

    private static final String T1 = "Inchidere luna";
    private static final String T2 = "Nota contabila";
    private static final String T3 = "Diverse birou";

    @Autowired ReportService service;

    Client alfa;
    Client beta;

    private void entry(String date, Employee employee, Client client, String task, int minutes) {
        entries.save(new TimesheetEntry(LocalDate.parse(date), employee, client, WorkingMonth.SEPTEMBRIE, minutes,
            task == null ? List.of() : List.of(task), ""));
    }

    private ReportFilter all() {
        return new ReportFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), Set.of(), Set.of(), "");
    }

    private ReportFilter with(Set<Long> clientIds, Set<Long> employeeIds, String task) {
        return new ReportFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), clientIds, employeeIds, task);
    }

    @BeforeEach
    void data() {
        alfa = clients.save(new Client("Alfa SRL"));
        beta = clients.save(new Client("Beta SRL"));
        entry("2026-09-01", worker, alfa, T1, 60);
        entry("2026-09-02", worker, alfa, T2, 30);
        entry("2026-09-03", other, alfa, T1, 120);
        entry("2026-09-04", worker, beta, T1, 45);
        entry("2026-09-05", other, null, T3, 15);
        entry("2026-08-31", worker, alfa, T1, 500);
        entry("2026-09-06", other, beta, null, 10);
    }

    private ReportGroup group(Report report, String label) {
        return report.groups().stream().filter(g -> g.label().equals(label)).findFirst().orElseThrow();
    }

    private ReportLine line(ReportGroup group, String label) {
        return group.lines().stream().filter(l -> l.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void byEmployee_totalsEachPersonAndBreaksDownByClient() {
        Report report = service.build(ReportView.ANGAJATI, all());

        assertThat(report.totalMinutes()).isEqualTo(280);
        assertThat(report.totalEntries()).isEqualTo(6);
        assertThat(report.groups()).extracting(ReportGroup::label).containsExactly("Nume other", "Nume worker");

        ReportGroup w = group(report, "Nume worker");
        assertThat(w.minutes()).isEqualTo(135);
        assertThat(w.entries()).isEqualTo(3);
        assertThat(line(w, "Alfa SRL").minutes()).isEqualTo(90);
        assertThat(line(w, "Alfa SRL").entries()).isEqualTo(2);
        assertThat(line(w, "Beta SRL").minutes()).isEqualTo(45);

        ReportGroup o = group(report, "Nume other");
        assertThat(o.minutes()).isEqualTo(145);
        assertThat(line(o, "Alfa SRL").minutes()).isEqualTo(120);
        assertThat(line(o, "Fără client").minutes()).isEqualTo(15);
        assertThat(line(o, "Beta SRL").minutes()).isEqualTo(10);
    }

    @Test
    void byClient_showsWhoWorkedHowMuch_biggestFirst() {
        Report report = service.build(ReportView.CLIENTI, all());

        assertThat(report.groups()).extracting(ReportGroup::label).containsExactly("Alfa SRL", "Beta SRL", "Fără client");
        ReportGroup a = group(report, "Alfa SRL");
        assertThat(a.minutes()).isEqualTo(210);
        assertThat(a.lines()).extracting(ReportLine::label).containsExactly("Nume other", "Nume worker");
        assertThat(a.lines()).extracting(ReportLine::minutes).containsExactly(120, 90);
        assertThat(group(report, "Beta SRL").minutes()).isEqualTo(55);
        assertThat(group(report, "Fără client").minutes()).isEqualTo(15);
    }

    @Test
    void byTask_groupsByTask_withItsCategory_andAnUntaggedBucket() {
        Report report = service.build(ReportView.SARCINI, all());

        assertThat(report.groups()).extracting(ReportGroup::label).containsExactly(T1, T2, T3, "Fără sarcină");
        ReportGroup t1 = group(report, T1);
        assertThat(t1.minutes()).isEqualTo(225);
        assertThat(t1.note()).isEqualTo("Verificare si inchidere luna");
        assertThat(t1.lines()).extracting(ReportLine::label).containsExactly("Nume other", "Nume worker");
        assertThat(group(report, T3).note()).isEqualTo("Administrativ / non-facturabil");
        assertThat(group(report, "Fără sarcină").note()).isNull();
        assertThat(group(report, "Fără sarcină").minutes()).isEqualTo(10);
    }

    @Test
    void everyViewAddsUpToTheSameTotal() {
        for (ReportView view : ReportView.values()) {
            Report report = service.build(view, all());
            assertThat(report.groups().stream().mapToInt(ReportGroup::minutes).sum()).isEqualTo(280);
            assertThat(report.groups().stream().mapToInt(ReportGroup::entries).sum()).isEqualTo(6);
            for (ReportGroup g : report.groups()) {
                assertThat(g.lines().stream().mapToInt(ReportLine::minutes).sum()).isEqualTo(g.minutes());
            }
        }
    }

    @Test
    void dateRange_isInclusiveOnBothEnds() {
        ReportFilter oneDay = new ReportFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), Set.of(), Set.of(), "");
        assertThat(service.build(ReportView.ANGAJATI, oneDay).totalMinutes()).isEqualTo(60);

        ReportFilter withAugust = new ReportFilter(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 30), Set.of(), Set.of(), "");
        assertThat(service.build(ReportView.ANGAJATI, withAugust).totalMinutes()).isEqualTo(780);

        ReportFilter empty = new ReportFilter(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31), Set.of(), Set.of(), "");
        Report none = service.build(ReportView.CLIENTI, empty);
        assertThat(none.groups()).isEmpty();
        assertThat(none.totalMinutes()).isZero();
        assertThat(none.totalEntries()).isZero();
    }

    @Test
    void clientFilter_limitsTheReport_andZeroMeansNoClient() {
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(alfa.getId()), Set.of(), "")).totalMinutes()).isEqualTo(210);
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(0L), Set.of(), "")).totalMinutes()).isEqualTo(15);
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(alfa.getId(), 0L), Set.of(), "")).totalMinutes()).isEqualTo(225);
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(999999L), Set.of(), "")).groups()).isEmpty();
    }

    @Test
    void employeeFilter_limitsTheReport() {
        Report report = service.build(ReportView.CLIENTI, with(Set.of(), Set.of(worker.getId()), ""));

        assertThat(report.totalMinutes()).isEqualTo(135);
        assertThat(report.groups().stream().flatMap(g -> g.lines().stream()).map(ReportLine::label)).containsOnly("Nume worker");
    }

    @Test
    void taskFilter_limitsTheReport_andDashMeansNoTask() {
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(), Set.of(), T1)).totalMinutes()).isEqualTo(225);
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(), Set.of(), ReportFilter.NO_TASK)).totalMinutes()).isEqualTo(10);
        assertThat(service.build(ReportView.ANGAJATI, with(Set.of(), Set.of(), "Sarcina inventata")).groups()).isEmpty();
    }

    @Test
    void filtersCombine() {
        Report report = service.build(ReportView.ANGAJATI, with(Set.of(alfa.getId()), Set.of(worker.getId()), T1));

        assertThat(report.totalMinutes()).isEqualTo(60);
        assertThat(report.totalEntries()).isEqualTo(1);
    }

    @Test
    void entryWithSeveralLegacyTasks_isCountedOnceUnderTheFirstOne() {
        entries.save(new TimesheetEntry(LocalDate.of(2026, 9, 10), worker, alfa, WorkingMonth.SEPTEMBRIE, 40,
            List.of(T2, T1), ""));

        Report report = service.build(ReportView.SARCINI, all());

        assertThat(report.totalMinutes()).isEqualTo(320);
        assertThat(group(report, T2).minutes()).isEqualTo(70);
        assertThat(group(report, T1).minutes()).isEqualTo(225);
    }

    @Test
    void twoEmployeesWithTheSameName_areNotMerged() {
        Employee twin1 = employees.save(new Employee("Ion Popescu", "ion1", "h", Role.ANGAJAT));
        Employee twin2 = employees.save(new Employee("Ion Popescu", "ion2", "h", Role.ANGAJAT));
        entry("2026-09-11", twin1, null, T3, 20);
        entry("2026-09-12", twin2, null, T3, 25);

        Report report = service.build(ReportView.ANGAJATI, all());

        assertThat(report.groups().stream().filter(g -> g.label().equals("Ion Popescu"))).hasSize(2);
    }

    @Test
    void emptyDatabase_producesAnEmptyReport() {
        entries.deleteAll();

        for (ReportView view : ReportView.values()) {
            assertThat(service.build(view, all()).groups()).isEmpty();
        }
    }
}
