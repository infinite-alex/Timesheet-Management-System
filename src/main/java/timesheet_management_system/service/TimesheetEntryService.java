package timesheet_management_system.service;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@Service
public class TimesheetEntryService {

    private final TimesheetEntryRepository timesheetEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    public TimesheetEntryService(TimesheetEntryRepository timesheetEntryRepository,
         EmployeeRepository employeeRepository, ClientRepository clientRepository) {
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
    }

    public List<TimesheetEntryDto> findAll() {
        Employee currentEmployee = getCurrentEmployee();

        List<TimesheetEntry> entries;
        if (currentEmployee.getRole() == Role.ADMIN) {
            entries = timesheetEntryRepository.findAll();
        } else {
            entries = timesheetEntryRepository.findByEmployee(currentEmployee);
        }

        List<TimesheetEntryDto> dtos = new ArrayList<>();
        for (TimesheetEntry entry : entries) {
            dtos.add(toDto(entry));
        }
        return dtos;
    }

    private Employee getCurrentEmployee() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new RuntimeException("Employee not found for username: " + currentUsername));
    }

    public TimesheetEntryDto save(TimesheetEntryDto dto) {
        Employee currentEmployee = getCurrentEmployee();

        Long effectiveEmployeeId;
        if (currentEmployee.getRole() == Role.ADMIN) {
            effectiveEmployeeId = dto.employeeId();
        } else {
            effectiveEmployeeId = currentEmployee.getId();
        }

        TimesheetEntryDto effectiveDto = new TimesheetEntryDto(
            dto.id(),
            dto.date(),
            effectiveEmployeeId,
            dto.clientId(),
            dto.workingmonth(),
            dto.totalMinutes(),
            dto.actions(),
            dto.extranote(),
            null,
            null
        );

        TimesheetEntry saved = timesheetEntryRepository.save(toEntity(effectiveDto));
        return toDto(saved);
    }
    private TimesheetEntryDto toDto(TimesheetEntry entry) {
        Long clientId;
        String clientName;
        if (entry.getClient() != null) {
            clientId = entry.getClient().getId();
            clientName = entry.getClient().getName();
        } else {
            clientId = null;
            clientName = null;
        }
        return new TimesheetEntryDto(
            entry.getId(),
            entry.getDate(),
            entry.getEmployee().getId(),
            clientId,
            entry.getWorkingMonth(),
            entry.getTotalMinutes(),
            entry.getActions(),
            entry.getExtraNote(),
            entry.getEmployee().getName(),
            clientName
        );
    }
    private TimesheetEntry toEntity(TimesheetEntryDto dto) {
    Employee employee = employeeRepository.findById(dto.employeeId())
        .orElseThrow(() -> new RuntimeException("Employee not found: " + dto.employeeId()));

    Client client;
    if (dto.clientId() != null) {
        client = clientRepository.findById(dto.clientId())
            .orElseThrow(() -> new RuntimeException("Client not found: " + dto.clientId()));
    } else {
        client = null;
    }

    return new TimesheetEntry(
        dto.date(),
        employee,
        client,
        dto.workingmonth(),
        dto.totalMinutes(),
        dto.actions(),
        dto.extranote()
    );
}
    
}