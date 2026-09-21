package timesheet_management_system.service;
import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.exception.ConflictException;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (password == null || password.length() < 8) {
            throw new RegistrationException("Parola trebuie să aibă cel puțin 8 caractere.");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new RegistrationException("Parola e prea lungă (maximum 72 de caractere).");
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
    
    private EmployeeDto toDto(Employee employee){
        return new EmployeeDto(employee.getId(),
         employee.getName(), employee.getUsername(), employee.getRole());
    }

    private Employee toEntity(EmployeeCreateDto employeeDto){
        String hashedPassword = passwordEncoder.encode(employeeDto.password());
        return new Employee(employeeDto.name(),employeeDto.username(),hashedPassword,employeeDto.role());
    }

}