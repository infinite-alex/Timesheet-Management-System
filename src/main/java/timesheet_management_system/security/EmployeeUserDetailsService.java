package timesheet_management_system.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import timesheet_management_system.model.Employee;
import timesheet_management_system.repository.EmployeeRepository;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Nu există utilizator cu username: " + username));

        return User.builder()
            .username(employee.getUsername())
            .password(employee.getPassword())
            .roles(employee.getRole().name())
            .build();
    }
}
