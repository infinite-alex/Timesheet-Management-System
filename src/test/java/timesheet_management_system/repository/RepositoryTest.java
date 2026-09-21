package timesheet_management_system.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

@DataJpaTest
class RepositoryTest {

    @Autowired EmployeeRepository employees;
    @Autowired ClientRepository clients;
    @Autowired TimesheetEntryRepository entries;
    @Autowired EntityManager em;

    private Employee employee(String username) {
        return employees.saveAndFlush(new Employee("Nume " + username, username, "hash", Role.ANGAJAT));
    }

    private TimesheetEntry entry(Employee e, Client c, String date, int minutes, List<String> actions) {
        return new TimesheetEntry(LocalDate.parse(date), e, c, WorkingMonth.SEPTEMBRIE, minutes, actions, "n");
    }

    @Test
    void findByUsername_findsExactMatchOnly() {
        employee("ana");

        assertThat(employees.findByUsername("ana")).isPresent();
        assertThat(employees.findByUsername("ANA")).isEmpty();
        assertThat(employees.findByUsername("an")).isEmpty();
        assertThat(employees.findByUsername("")).isEmpty();
    }

    @Test
    void existsByUsernameIgnoreCase_ignoresCase() {
        employee("Popescu");

        assertThat(employees.existsByUsernameIgnoreCase("popescu")).isTrue();
        assertThat(employees.existsByUsernameIgnoreCase("POPESCU")).isTrue();
        assertThat(employees.existsByUsernameIgnoreCase("popesc")).isFalse();
    }

    @Test
    void username_mustBeUnique() {
        employee("ana");

        assertThatThrownBy(() -> employees.saveAndFlush(new Employee("Alta", "ana", "h", Role.ANGAJAT)))
            .hasMessageContaining("");
    }

    @Test
    void username_cannotBeNull() {
        assertThatThrownBy(() -> employees.saveAndFlush(new Employee("Fara", null, "h", Role.ANGAJAT)))
            .isInstanceOf(Exception.class);
    }

    @Test
    void role_isStoredAsReadableText() {
        Employee saved = employee("rol");
        em.clear();

        Object stored = em.createNativeQuery("select role from employee where id = :id")
            .setParameter("id", saved.getId()).getSingleResult();

        assertThat(stored).isEqualTo("ANGAJAT");
    }

    @Test
    void findByEmployee_returnsOnlyThatEmployeesEntries() {
        Employee ana = employee("ana");
        Employee bob = employee("bob");
        entries.save(entry(ana, null, "2026-09-01", 10, List.of()));
        entries.save(entry(ana, null, "2026-09-02", 20, List.of()));
        entries.save(entry(bob, null, "2026-09-03", 30, List.of()));

        assertThat(entries.findByEmployee(ana)).hasSize(2).allMatch(e -> e.getEmployee().getId().equals(ana.getId()));
        assertThat(entries.findByEmployee(bob)).hasSize(1);
        assertThat(entries.findByEmployee(employee("nou"))).isEmpty();
    }

    @Test
    void entry_roundTripsActionsClientAndFields() {
        Employee ana = employee("ana");
        Client client = clients.save(new Client("Firma"));
        Long id = entries.saveAndFlush(entry(ana, client, "2026-09-15", 125, List.of("Salarii", "TVA"))).getId();
        em.clear();

        TimesheetEntry loaded = entries.findById(id).orElseThrow();

        assertThat(loaded.getDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(loaded.getTotalMinutes()).isEqualTo(125);
        assertThat(loaded.getWorkingMonth()).isEqualTo(WorkingMonth.SEPTEMBRIE);
        assertThat(loaded.getActions()).containsExactly("Salarii", "TVA");
        assertThat(loaded.getClient().getName()).isEqualTo("Firma");
        assertThat(loaded.getEmployee().getUsername()).isEqualTo("ana");
    }

    @Test
    void entry_withoutClient_isStored() {
        Long id = entries.saveAndFlush(entry(employee("ana"), null, "2026-09-15", 10, List.of())).getId();
        em.clear();

        assertThat(entries.findById(id).orElseThrow().getClient()).isNull();
    }

    @Test
    void month_isStoredAsReadableText() {
        Long id = entries.saveAndFlush(entry(employee("ana"), null, "2026-09-15", 10, List.of())).getId();
        em.clear();

        Object stored = em.createNativeQuery("select workingmonth from timesheet_entry where id = :id")
            .setParameter("id", id).getSingleResult();

        assertThat(stored).isEqualTo("SEPTEMBRIE");
    }

    @Test
    void entry_requiresAnEmployee() {
        assertThatThrownBy(() -> entries.saveAndFlush(entry(null, null, "2026-09-15", 10, List.of())))
            .isInstanceOf(Exception.class);
    }

    @Test
    void entry_requiresADate() {
        Employee ana = employee("ana");

        assertThatThrownBy(() -> entries.saveAndFlush(
            new TimesheetEntry(null, ana, null, WorkingMonth.MAI, 10, List.of(), "n")))
            .isInstanceOf(Exception.class);
    }

    @Test
    void clients_canBeSavedAndListed() {
        clients.save(new Client("A"));
        clients.save(new Client("B"));

        assertThat(clients.findAll()).extracting(Client::getName).containsExactlyInAnyOrder("A", "B");
    }
}
