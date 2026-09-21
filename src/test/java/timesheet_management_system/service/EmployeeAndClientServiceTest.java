package timesheet_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import timesheet_management_system.dto.ClientDto;
import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeAndClientServiceTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock ClientRepository clientRepository;
    @Mock TimesheetEntryRepository entryRepository;

    PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void employeePassword_isHashedBeforeSaving() {
        EmployeeService service = new EmployeeService(employeeRepository, encoder, entryRepository);
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.save(new EmployeeCreateDto("Ana", "ana", "secret123", Role.ANGAJAT));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isNotEqualTo("secret123").startsWith("$2");
        assertThat(encoder.matches("secret123", saved.getValue().getPassword())).isTrue();
        assertThat(encoder.matches("wrong", saved.getValue().getPassword())).isFalse();
    }

    @Test
    void samePassword_producesDifferentHashesForDifferentUsers() {
        EmployeeService service = new EmployeeService(employeeRepository, encoder, entryRepository);
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.save(new EmployeeCreateDto("A", "a", "same", Role.ANGAJAT));
        service.save(new EmployeeCreateDto("B", "b", "same", Role.ANGAJAT));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getPassword()).isNotEqualTo(saved.getAllValues().get(1).getPassword());
    }

    @Test
    void savingAnExistingUsername_isAConflict_andNothingIsSaved() {
        EmployeeService service = new EmployeeService(employeeRepository, encoder, entryRepository);
        when(employeeRepository.existsByUsernameIgnoreCase("ana")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.save(new EmployeeCreateDto("Ana", "ana", "secret123", Role.ANGAJAT)))
            .isInstanceOf(timesheet_management_system.exception.ConflictException.class);
        org.mockito.Mockito.verify(employeeRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void employeeDto_neverExposesThePassword() {
        List<String> fields = Arrays.stream(EmployeeDto.class.getRecordComponents())
            .map(RecordComponent::getName).toList();

        assertThat(fields).doesNotContain("password");
    }

    @Test
    void employeeSave_returnsNameUsernameAndRole() {
        EmployeeService service = new EmployeeService(employeeRepository, encoder, entryRepository);
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeDto result = service.save(new EmployeeCreateDto("Ana Pop", "ana", "pw", Role.ADMIN));

        assertThat(result.name()).isEqualTo("Ana Pop");
        assertThat(result.username()).isEqualTo("ana");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void employeeFindAll_mapsEveryEmployee() {
        EmployeeService service = new EmployeeService(employeeRepository, encoder, entryRepository);
        when(employeeRepository.findAll()).thenReturn(List.of(
            new Employee("A", "a", "h", Role.ADMIN), new Employee("B", "b", "h", Role.ANGAJAT)));

        assertThat(service.findAll()).extracting(EmployeeDto::username).containsExactly("a", "b");
    }

    @Test
    void clientSave_keepsTheName() {
        ClientService service = new ClientService(clientRepository, entryRepository);
        when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ClientDto result = service.save(new ClientDto(null, "Firma SRL"));

        assertThat(result.name()).isEqualTo("Firma SRL");
    }

    @Test
    void clientFindAll_mapsEveryClient() {
        ClientService service = new ClientService(clientRepository, entryRepository);
        when(clientRepository.findAll()).thenReturn(List.of(new Client("A"), new Client("B")));

        assertThat(service.findAll()).extracting(ClientDto::name).containsExactly("A", "B");
    }
}
