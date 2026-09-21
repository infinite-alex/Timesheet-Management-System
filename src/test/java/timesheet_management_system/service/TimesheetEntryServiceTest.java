package timesheet_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.exception.ResourceNotFoundException;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;

@ExtendWith(MockitoExtension.class)
class TimesheetEntryServiceTest {

    @Mock TimesheetEntryRepository entryRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock ClientRepository clientRepository;

    TimesheetEntryService service;
    Employee admin;
    Employee worker;
    Employee other;

    @BeforeEach
    void setUp() {
        service = new TimesheetEntryService(entryRepository, employeeRepository, clientRepository);
        admin = employee(1L, "boss", Role.ADMIN);
        worker = employee(2L, "worker", Role.ANGAJAT);
        other = employee(3L, "other", Role.ANGAJAT);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Employee employee(long id, String username, Role role) {
        Employee e = new Employee("Nume " + username, username, "hash", role);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private void loginAs(Employee e) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(e.getUsername(), null, List.of()));
        when(employeeRepository.findByUsername(e.getUsername())).thenReturn(Optional.of(e));
    }

    private TimesheetEntry entryFor(Employee e, Client client, int minutes) {
        return new TimesheetEntry(LocalDate.of(2026, 9, 1), e, client, WorkingMonth.SEPTEMBRIE, minutes, List.of("a"), "nota");
    }

    private TimesheetEntryDto dto(Long employeeId, Long clientId, int minutes) {
        return new TimesheetEntryDto(null, LocalDate.of(2026, 9, 1), employeeId, clientId,
            WorkingMonth.SEPTEMBRIE, minutes, List.of(), "nota", null, null);
    }

    @Test
    void findAll_asAdmin_returnsEveryonesEntries() {
        loginAs(admin);
        when(entryRepository.findAll()).thenReturn(List.of(entryFor(worker, null, 60), entryFor(other, null, 30)));

        List<TimesheetEntryDto> result = service.findAll();

        assertThat(result).hasSize(2);
        verify(entryRepository, never()).findByEmployee(any());
    }

    @Test
    void findAll_asEmployee_returnsOnlyOwnEntries() {
        loginAs(worker);
        when(entryRepository.findByEmployee(worker)).thenReturn(List.of(entryFor(worker, null, 60)));

        List<TimesheetEntryDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).employeeName()).isEqualTo("Nume worker");
        verify(entryRepository, never()).findAll();
    }

    @Test
    void findAll_whenUserMissingFromDatabase_fails() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ghost", null, List.of()));
        when(employeeRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll()).hasMessageContaining("ghost");
    }

    @Test
    void save_asEmployee_ignoresSpoofedEmployeeId() {
        loginAs(worker);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.save(dto(1L, null, 60));

        ArgumentCaptor<TimesheetEntry> saved = ArgumentCaptor.forClass(TimesheetEntry.class);
        verify(entryRepository).save(saved.capture());
        assertThat(saved.getValue().getEmployee()).isSameAs(worker);
        verify(employeeRepository, never()).findById(1L);
    }

    @Test
    void save_asEmployee_ignoresNullEmployeeId() {
        loginAs(worker);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TimesheetEntryDto result = service.save(dto(null, null, 45));

        assertThat(result.employeeId()).isEqualTo(2L);
        assertThat(result.totalMinutes()).isEqualTo(45);
    }

    @Test
    void save_asAdmin_usesEmployeeIdFromRequest() {
        loginAs(admin);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.save(dto(2L, null, 60));

        ArgumentCaptor<TimesheetEntry> saved = ArgumentCaptor.forClass(TimesheetEntry.class);
        verify(entryRepository).save(saved.capture());
        assertThat(saved.getValue().getEmployee()).isSameAs(worker);
    }

    @Test
    void save_withClient_resolvesClientAndReturnsItsName() {
        loginAs(worker);
        Client client = new Client("Client SRL");
        ReflectionTestUtils.setField(client, "id", 7L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TimesheetEntryDto result = service.save(dto(null, 7L, 60));

        assertThat(result.clientId()).isEqualTo(7L);
        assertThat(result.clientName()).isEqualTo("Client SRL");
        assertThat(result.employeeName()).isEqualTo("Nume worker");
    }

    @Test
    void save_withoutClient_leavesClientEmpty() {
        loginAs(worker);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TimesheetEntryDto result = service.save(dto(null, null, 60));

        assertThat(result.clientId()).isNull();
        assertThat(result.clientName()).isNull();
        verify(clientRepository, never()).findById(any());
    }

    @Test
    void save_withUnknownClient_throwsAndSavesNothing() {
        loginAs(worker);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(dto(null, 99L, 60)))
            .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("99");
        verify(entryRepository, never()).save(any());
    }

    @Test
    void save_asAdminWithUnknownEmployee_throwsAndSavesNothing() {
        loginAs(admin);
        when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(dto(404L, null, 60)))
            .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("404");
        verify(entryRepository, never()).save(any());
    }

    @Test
    void save_asAdminWithoutEmployeeId_isABadRequest() {
        loginAs(admin);

        assertThatThrownBy(() -> service.save(dto(null, null, 60))).isInstanceOf(BadRequestException.class);
        verify(entryRepository, never()).save(any());
    }

    @Test
    void save_keepsActionsAndNoteAndMonth() {
        loginAs(worker);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        List<String> actions = new ArrayList<>(List.of("Salarii", "TVA"));
        TimesheetEntryDto input = new TimesheetEntryDto(null, LocalDate.of(2026, 1, 31), null, null,
            WorkingMonth.IANUARIE, 90, actions, "detalii", null, null);

        TimesheetEntryDto result = service.save(input);

        assertThat(result.actions()).containsExactly("Salarii", "TVA");
        assertThat(result.extranote()).isEqualTo("detalii");
        assertThat(result.workingmonth()).isEqualTo(WorkingMonth.IANUARIE);
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 1, 31));
    }
}
