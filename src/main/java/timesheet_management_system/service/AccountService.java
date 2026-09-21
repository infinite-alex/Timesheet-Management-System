package timesheet_management_system.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.exception.ResourceNotFoundException;
import timesheet_management_system.model.Employee;
import timesheet_management_system.repository.EmployeeRepository;

@Service
public class AccountService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public EmployeeDto profile(String username) {
        Employee employee = find(username);
        return new EmployeeDto(employee.getId(), employee.getName(), employee.getUsername(), employee.getRole(), employee.isActive());
    }

    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        Employee employee = find(username);

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, employee.getPassword())) {
            throw new BadRequestException("Parola curentă nu este corectă.");
        }
        String problem = PasswordPolicy.violation(newPassword);
        if (problem != null) {
            throw new BadRequestException(problem);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Parolele noi nu coincid.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BadRequestException("Parola nouă trebuie să fie diferită de cea curentă.");
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
    }

    private Employee find(String username) {
        return employeeRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Contul nu există."));
    }
}
