package timesheet_management_system.config;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

    static final int MIN_PASSWORD_LENGTH = 12;
    private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredPassword;

    public AdminSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.password:}") String configuredPassword) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredPassword = configuredPassword == null ? "" : configuredPassword;
    }

    @Override
    public void run(String... args) {
        boolean configured = !configuredPassword.isBlank();
        if (configured && configuredPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "ADMIN_PASSWORD trebuie să aibă cel puțin " + MIN_PASSWORD_LENGTH + " caractere.");
        }

        Optional<Employee> existing = employeeRepository.findByUsername("admin");

        if (existing.isEmpty()) {
            String password = configured ? configuredPassword : randomPassword();
            employeeRepository.save(new Employee("Admin", "admin", passwordEncoder.encode(password), Role.ADMIN));
            if (configured) {
                System.out.println("Cont admin creat cu parola din ADMIN_PASSWORD.");
            } else {
                System.out.println("Cont admin creat. Parola generată (o vezi doar acum, salveaz-o): " + password);
            }
        } else if (configured && !passwordEncoder.matches(configuredPassword, existing.get().getPassword())) {
            Employee admin = existing.get();
            admin.setPassword(passwordEncoder.encode(configuredPassword));
            employeeRepository.save(admin);
            System.out.println("Parola contului admin a fost actualizată din ADMIN_PASSWORD.");
        }
    }

    static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
