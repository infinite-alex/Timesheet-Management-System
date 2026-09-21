package timesheet_management_system.service;
import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.dto.EmployeeUpdateDto;
import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.exception.ConflictException;
import timesheet_management_system.exception.ResourceNotFoundException;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimesheetEntryRepository timesheetEntryRepository;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
            TimesheetEntryRepository timesheetEntryRepository) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.timesheetEntryRepository = timesheetEntryRepository;
    }

    public List<EmployeeDto> findAll() {
        List<EmployeeDto> employeeDto = new ArrayList<>();
        List<Employee> employees = employeeRepository.findAll();
        for( Employee employee: employees){
            employeeDto.add(toDto(employee));
        }
        return employeeDto;
    }

    public EmployeeDto save(EmployeeCreateDto dto) {
        if (employeeRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new ConflictException("Username-ul este deja folosit.");
        }
        Employee savedemployee = employeeRepository.save(toEntity(dto));
        return toDto(savedemployee);
    }

    public EmployeeDto register(String name, String username, String password, String confirmPassword) {
        String cleanName = name == null ? "" : name.trim();
        String cleanUsername = username == null ? "" : username.trim();

        if (cleanName.isEmpty() || cleanName.length() > 100) {
            throw new RegistrationException("Introdu numele complet (maximum 100 de caractere).");
        }
        if (!cleanUsername.matches("[A-Za-z0-9._-]{3,30}")) {
            throw new RegistrationException("Username-ul trebuie să aibă 3-30 de caractere: litere, cifre, punct, minus sau underscore.");
        }
        String passwordProblem = PasswordPolicy.violation(password);
        if (passwordProblem != null) {
            throw new RegistrationException(passwordProblem);
        }
        if (!password.equals(confirmPassword)) {
            throw new RegistrationException("Parolele nu coincid.");
        }
        if (employeeRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new RegistrationException("Username-ul este deja folosit. Alege altul.");
        }

        try {
            Employee saved = employeeRepository.save(
                new Employee(cleanName, cleanUsername, passwordEncoder.encode(password), Role.ANGAJAT));
            return toDto(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RegistrationException("Username-ul este deja folosit. Alege altul.");
        }
    }
    
    public EmployeeDto update(Long id, EmployeeUpdateDto dto, String currentUsername) {
        Employee target = find(id);
        boolean self = target.getUsername().equals(currentUsername);
        boolean roleOrStatusChanges = dto.role() != target.getRole() || dto.active() != target.isActive();

        if (self && roleOrStatusChanges) {
            throw new BadRequestException("Nu îți poți schimba propriul rol sau starea contului.");
        }
        boolean losesAdminAccess = target.getRole() == Role.ADMIN && target.isActive()
            && (dto.role() != Role.ADMIN || !dto.active());
        if (losesAdminAccess && employeeRepository.countActiveByRole(Role.ADMIN) <= 1) {
            throw new BadRequestException("Trebuie să rămână cel puțin un administrator activ.");
        }
        boolean changesPassword = dto.newPassword() != null && !dto.newPassword().isBlank();
        if (changesPassword) {
            String problem = PasswordPolicy.violation(dto.newPassword());
            if (problem != null) {
                throw new BadRequestException(problem);
            }
        }

        target.setName(dto.name().trim());
        target.setRole(dto.role());
        target.setActive(dto.active());
        if (changesPassword) {
            target.setPassword(passwordEncoder.encode(dto.newPassword()));
        }
        return toDto(employeeRepository.save(target));
    }

    public void delete(Long id, String currentUsername) {
        Employee target = find(id);
        if (target.getUsername().equals(currentUsername)) {
            throw new BadRequestException("Nu îți poți șterge propriul cont.");
        }
        if (target.getRole() == Role.ADMIN && target.isActive() && employeeRepository.countActiveByRole(Role.ADMIN) <= 1) {
            throw new BadRequestException("Trebuie să rămână cel puțin un administrator activ.");
        }
        if (timesheetEntryRepository.existsByEmployee(target)) {
            throw new ConflictException("Angajatul are pontaje și nu poate fi șters. Dezactivează-l în loc.");
        }
        employeeRepository.delete(target);
    }

    private Employee find(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Angajatul nu există."));
    }

    private EmployeeDto toDto(Employee employee){
        return new EmployeeDto(employee.getId(),
         employee.getName(), employee.getUsername(), employee.getRole(), employee.isActive());
    }

    private Employee toEntity(EmployeeCreateDto employeeDto){
        String hashedPassword = passwordEncoder.encode(employeeDto.password());
        return new Employee(employeeDto.name(),employeeDto.username(),hashedPassword,employeeDto.role());
    }

}