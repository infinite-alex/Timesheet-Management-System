package timesheet_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import timesheet_management_system.dto.EmployeeUpdateDto;
import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.exception.ConflictException;
import timesheet_management_system.exception.ResourceNotFoundException;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;

@ExtendWith(MockitoExtension.class)
class AccountAndEmployeeRulesTest {

    @Mock EmployeeRepository employees;
    @Mock TimesheetEntryRepository entries;
    PasswordEncoder encoder = new BCryptPasswordEncoder();

    private Employee employee(long id, String username, Role role, boolean active) {
        Employee e = new Employee("Nume " + username, username, encoder.encode("parola-veche-1"), role);
        ReflectionTestUtils.setField(e, "id", id);
        e.setActive(active);
        return e;
    }

    private EmployeeService employeeService() {
        return new EmployeeService(employees, encoder, entries);
    }

    private AccountService accountService() {
        return new AccountService(employees, encoder);
    }

    @Test
    void passwordPolicy_acceptsAndRejectsTheRightValues() {
        assertThat(PasswordPolicy.violation("12345678")).isNull();
        assertThat(PasswordPolicy.violation("a".repeat(72))).isNull();
        assertThat(PasswordPolicy.violation("1234567")).contains("cel puțin 8");
        assertThat(PasswordPolicy.violation("")).isNotNull();
        assertThat(PasswordPolicy.violation(null)).isNotNull();
        assertThat(PasswordPolicy.violation("a".repeat(73))).contains("prea lungă");
        assertThat(PasswordPolicy.violation("ă".repeat(37))).contains("prea lungă");
    }

    @Test
    void changePassword_success_storesAHashOfTheNewPassword() {
        Employee ana = employee(1, "ana", Role.ANGAJAT, true);
        when(employees.findByUsername("ana")).thenReturn(Optional.of(ana));

        accountService().changePassword("ana", "parola-veche-1", "Parola-Noua-2", "Parola-Noua-2");

        verify(employees).save(ana);
        assertThat(encoder.matches("Parola-Noua-2", ana.getPassword())).isTrue();
        assertThat(encoder.matches("parola-veche-1", ana.getPassword())).isFalse();
    }

    @Test
    void changePassword_failures_neverSave() {
        Employee ana = employee(1, "ana", Role.ANGAJAT, true);
        when(employees.findByUsername("ana")).thenReturn(Optional.of(ana));
        AccountService service = accountService();

        assertThatThrownBy(() -> service.changePassword("ana", "gresita", "Parola-Noua-2", "Parola-Noua-2"))
            .isInstanceOf(BadRequestException.class).hasMessageContaining("curentă");
        assertThatThrownBy(() -> service.changePassword("ana", null, "Parola-Noua-2", "Parola-Noua-2"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.changePassword("ana", "parola-veche-1", "scurta", "scurta"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.changePassword("ana", "parola-veche-1", "Parola-Noua-2", "Alta-Parola-3"))
            .isInstanceOf(BadRequestException.class).hasMessageContaining("coincid");
        assertThatThrownBy(() -> service.changePassword("ana", "parola-veche-1", "parola-veche-1", "parola-veche-1"))
            .isInstanceOf(BadRequestException.class).hasMessageContaining("diferită");
        verify(employees, never()).save(any());
    }

    @Test
    void changePassword_forAnUnknownAccount_isNotFound() {
        when(employees.findByUsername("fantoma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService().changePassword("fantoma", "x", "Parola-Noua-2", "Parola-Noua-2"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anAdmin_cannotDemoteOrDeactivateThemselves() {
        Employee me = employee(1, "boss", Role.ADMIN, true);
        when(employees.findById(1L)).thenReturn(Optional.of(me));
        EmployeeService service = employeeService();

        assertThatThrownBy(() -> service.update(1L, new EmployeeUpdateDto("Boss", Role.ANGAJAT, true, null), "boss"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(1L, new EmployeeUpdateDto("Boss", Role.ADMIN, false, null), "boss"))
            .isInstanceOf(BadRequestException.class);
        verify(employees, never()).save(any());
    }

    @Test
    void theLastActiveAdmin_cannotBeDemotedOrDeactivatedByAnotherAdmin() {
        Employee target = employee(2, "unic", Role.ADMIN, true);
        when(employees.findById(2L)).thenReturn(Optional.of(target));
        when(employees.countActiveByRole(Role.ADMIN)).thenReturn(1L);
        EmployeeService service = employeeService();

        assertThatThrownBy(() -> service.update(2L, new EmployeeUpdateDto("Unic", Role.ANGAJAT, true, null), "altcineva"))
            .isInstanceOf(BadRequestException.class).hasMessageContaining("administrator activ");
        assertThatThrownBy(() -> service.update(2L, new EmployeeUpdateDto("Unic", Role.ADMIN, false, null), "altcineva"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.delete(2L, "altcineva")).isInstanceOf(BadRequestException.class);
        verify(employees, never()).save(any());
        verify(employees, never()).delete(any());
    }

    @Test
    void anAdmin_canBeDemotedWhenAnotherActiveAdminExists() {
        Employee target = employee(2, "doi", Role.ADMIN, true);
        when(employees.findById(2L)).thenReturn(Optional.of(target));
        when(employees.countActiveByRole(Role.ADMIN)).thenReturn(2L);
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));

        employeeService().update(2L, new EmployeeUpdateDto("Doi", Role.ANGAJAT, true, null), "altcineva");

        assertThat(target.getRole()).isEqualTo(Role.ANGAJAT);
    }

    @Test
    void anInactiveAdmin_doesNotCountAsTheLastOne() {
        Employee target = employee(3, "inactiv", Role.ADMIN, false);
        when(employees.findById(3L)).thenReturn(Optional.of(target));
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));

        employeeService().update(3L, new EmployeeUpdateDto("Inactiv", Role.ANGAJAT, false, null), "altcineva");

        verify(employees, never()).countActiveByRole(any());
        assertThat(target.getRole()).isEqualTo(Role.ANGAJAT);
    }

    @Test
    void update_hashesANewPassword_andTrimsTheName() {
        Employee target = employee(4, "ion", Role.ANGAJAT, true);
        when(employees.findById(4L)).thenReturn(Optional.of(target));
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));

        employeeService().update(4L, new EmployeeUpdateDto("  Ion Nou  ", Role.ANGAJAT, true, "Parola-Reset-9"), "boss");

        assertThat(target.getName()).isEqualTo("Ion Nou");
        assertThat(encoder.matches("Parola-Reset-9", target.getPassword())).isTrue();
    }

    @Test
    void update_withoutAPassword_keepsTheOldHash() {
        Employee target = employee(4, "ion", Role.ANGAJAT, true);
        String before = target.getPassword();
        when(employees.findById(4L)).thenReturn(Optional.of(target));
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));

        employeeService().update(4L, new EmployeeUpdateDto("Ion", Role.ANGAJAT, true, "   "), "boss");

        assertThat(target.getPassword()).isEqualTo(before);
    }

    @Test
    void delete_rules() {
        Employee free = employee(5, "liber", Role.ANGAJAT, true);
        Employee busy = employee(6, "ocupat", Role.ANGAJAT, true);
        when(employees.findById(5L)).thenReturn(Optional.of(free));
        when(employees.findById(6L)).thenReturn(Optional.of(busy));
        when(employees.findById(404L)).thenReturn(Optional.empty());
        when(entries.existsByEmployee(free)).thenReturn(false);
        when(entries.existsByEmployee(busy)).thenReturn(true);
        EmployeeService service = employeeService();

        service.delete(5L, "boss");
        verify(employees).delete(free);

        assertThatThrownBy(() -> service.delete(6L, "boss")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.delete(5L, "liber")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.delete(404L, "boss")).isInstanceOf(ResourceNotFoundException.class);
        verify(employees, never()).delete(busy);
    }

    @Test
    void employeeWithoutTheFlag_isActive_andSetterWorks() {
        Employee e = new Employee("A", "a", "h", Role.ANGAJAT);
        assertThat(e.isActive()).isTrue();

        ReflectionTestUtils.setField(e, "active", null);
        assertThat(e.isActive()).isTrue();

        e.setActive(false);
        assertThat(e.isActive()).isFalse();
    }
}
