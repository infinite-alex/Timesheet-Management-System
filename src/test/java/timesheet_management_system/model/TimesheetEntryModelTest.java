package timesheet_management_system.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimesheetEntryModelTest {

    private final Employee employee = new Employee("Ana", "ana", "h", Role.ANGAJAT);

    private TimesheetEntry entry(List<String> actions) {
        return new TimesheetEntry(LocalDate.of(2026, 9, 1), employee, null, WorkingMonth.SEPTEMBRIE, 60, actions, "n");
    }

    @Test
    void constructor_copiesTheActionsList() {
        List<String> source = new ArrayList<>(List.of("a"));
        TimesheetEntry e = entry(source);

        source.add("b");

        assertThat(e.getActions()).containsExactly("a");
    }

    @Test
    void getActions_returnsACopy() {
        TimesheetEntry e = entry(List.of("a"));

        e.getActions().add("hacked");

        assertThat(e.getActions()).containsExactly("a");
    }

    @Test
    void setActions_copiesTheList() {
        TimesheetEntry e = entry(List.of("a"));
        List<String> replacement = new ArrayList<>(List.of("x"));

        e.setActions(replacement);
        replacement.add("y");

        assertThat(e.getActions()).containsExactly("x");
    }

    @Test
    void mutableFields_canBeCorrected() {
        TimesheetEntry e = entry(List.of());
        Client client = new Client("C");

        e.setClient(client);
        e.setTotalMinutes(120);
        e.setExtraNote("nou");
        e.setWorkingMonth(WorkingMonth.OCTOMBRIE);

        assertThat(e.getClient()).isSameAs(client);
        assertThat(e.getTotalMinutes()).isEqualTo(120);
        assertThat(e.getExtraNote()).isEqualTo("nou");
        assertThat(e.getWorkingMonth()).isEqualTo(WorkingMonth.OCTOMBRIE);
    }

    @Test
    void dateAndEmployee_areFixedAtCreation() {
        TimesheetEntry e = entry(List.of());

        assertThat(e.getDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(e.getEmployee()).isSameAs(employee);
        assertThat(TimesheetEntry.class.getMethods()).extracting(m -> m.getName())
            .doesNotContain("setDate", "setEmployee");
    }

    @Test
    void workingMonth_hasAllTwelveMonthsInCalendarOrder() {
        assertThat(WorkingMonth.values()).hasSize(12);
        assertThat(WorkingMonth.values()[0]).isEqualTo(WorkingMonth.IANUARIE);
        assertThat(WorkingMonth.values()[8]).isEqualTo(WorkingMonth.SEPTEMBRIE);
        assertThat(WorkingMonth.values()[11]).isEqualTo(WorkingMonth.DECEMBRIE);
    }
}
