package timesheet_management_system.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    private static final String STRONG = "Correct-Horse-Battery-42";

    @Mock EmployeeRepository repository;
    PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AdminSeeder seeder(String configured) {
        return new AdminSeeder(repository, encoder, configured, false);
    }

    private AdminSeeder resettingSeeder(String configured) {
        return new AdminSeeder(repository, encoder, configured, true);
    }

    private String capturedConsole(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }

    @Test
    void missingAdmin_withConfiguredPassword_isCreatedWithThatPassword_andNeverPrintsIt() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());
        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);

        String console = capturedConsole(() -> seeder(STRONG).run());

        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches(STRONG, saved.getValue().getPassword())).isTrue();
        assertThat(saved.getValue().getPassword()).isNotEqualTo(STRONG);
        assertThat(console).doesNotContain(STRONG);
    }

    @Test
    void missingAdmin_withoutConfiguredPassword_getsARandomStrongPassword_shownOnce() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());
        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);

        String console = capturedConsole(() -> seeder("").run());

        verify(repository).save(saved.capture());
        String generated = console.substring(console.lastIndexOf(": ") + 2).trim();
        assertThat(generated).hasSize(24).doesNotContain("admin123");
        assertThat(encoder.matches(generated, saved.getValue().getPassword())).isTrue();
        assertThat(encoder.matches("admin123", saved.getValue().getPassword())).isFalse();
    }

    @Test
    void generatedPasswords_differEveryTime() {
        Set<String> passwords = IntStream.range(0, 50).mapToObj(i -> AdminSeeder.randomPassword())
            .collect(Collectors.toSet());

        assertThat(passwords).hasSize(50);
        assertThat(passwords).allMatch(p -> p.matches("[A-Za-z0-9]{24}"));
    }

    @Test
    void existingAdmin_withDifferentConfiguredPassword_isLeftUntouchedByDefault() {
        Employee admin = new Employee("Admin", "admin", encoder.encode("parolaSchimbataDinUI1"), Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        seeder(STRONG).run();

        verify(repository, never()).save(any());
        assertThat(encoder.matches("parolaSchimbataDinUI1", admin.getPassword())).isTrue();
        assertThat(encoder.matches(STRONG, admin.getPassword())).isFalse();
    }

    @Test
    void existingAdmin_withResetFlag_getsItsPasswordReplaced() {
        Employee admin = new Employee("Admin", "admin", encoder.encode("admin123"), Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        String console = capturedConsole(() -> resettingSeeder(STRONG).run());

        verify(repository).save(admin);
        assertThat(encoder.matches(STRONG, admin.getPassword())).isTrue();
        assertThat(encoder.matches("admin123", admin.getPassword())).isFalse();
        assertThat(console).doesNotContain(STRONG);
    }

    @Test
    void existingAdmin_alreadyUsingTheConfiguredPassword_isLeftUntouched() {
        Employee admin = new Employee("Admin", "admin", encoder.encode(STRONG), Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        resettingSeeder(STRONG).run();

        verify(repository, never()).save(any());
    }

    @Test
    void resetFlag_withoutAConfiguredPassword_changesNothing() {
        Employee admin = new Employee("Admin", "admin", encoder.encode("altaParola123"), Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        resettingSeeder("").run();

        verify(repository, never()).save(any());
    }

    @Test
    void existingAdmin_withoutConfiguredPassword_isLeftUntouched() {
        Employee admin = new Employee("Admin", "admin", encoder.encode("altaParola123"), Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        seeder("").run();

        verify(repository, never()).save(any());
        assertThat(encoder.matches("altaParola123", admin.getPassword())).isTrue();
    }

    @Test
    void tooShortConfiguredPassword_refusesToStart() {
        assertThatThrownBy(() -> seeder("admin123").run())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("12");
        verify(repository, never()).save(any());
    }

    @Test
    void blankConfiguredPassword_isTreatedAsNotConfigured() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        capturedConsole(() -> seeder("   ").run());

        verify(repository).save(any());
    }

    @Test
    void nullConfiguredPassword_doesNotCrash() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        capturedConsole(() -> seeder(null).run());

        verify(repository).save(any());
    }
}
