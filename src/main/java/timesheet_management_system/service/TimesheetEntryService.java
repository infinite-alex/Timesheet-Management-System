package timesheet_management_system.service;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;
import org.springframework.stereotype.Service;

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
        List<TimesheetEntryDto> dtos = new ArrayList<>();
        List<TimesheetEntry> entries = timesheetEntryRepository.findAll();
        for (TimesheetEntry entry : entries) {
            dtos.add(toDto(entry));
        }
        return dtos;
    }

    public TimesheetEntryDto save(TimesheetEntryDto dto) {
        TimesheetEntry saved = timesheetEntryRepository.save(toEntity(dto));
        return toDto(saved);
    }
    private TimesheetEntryDto toDto(TimesheetEntry entry) {
        Long clientId;
        if (entry.getClient() != null) {
            clientId = entry.getClient().getId();
        } else {
            clientId = null;
        }
        return new TimesheetEntryDto(
            entry.getId(),
            entry.getDate(),
            entry.getEmployee().getId(),
            clientId,
            entry.getWorkingMonth(),
            entry.getTotalMinutes(),
            entry.getActions(),
            entry.getExtraNote()
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