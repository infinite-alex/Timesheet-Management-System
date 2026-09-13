package timesheet_management_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.findByUsername("admin").isEmpty()) {
            Employee admin = new Employee(
                "Admin",
                "admin",
                passwordEncoder.encode("admin123"),
                Role.ADMIN
            );
            employeeRepository.save(admin);
            System.out.println("Admin inițial creat: username=admin, parola=admin123");
        }
    }
}