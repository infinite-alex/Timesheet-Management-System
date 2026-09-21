package timesheet_management_system.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import timesheet_management_system.model.Employee;
import timesheet_management_system.repository.EmployeeRepository;

public class AccountStateFilter extends OncePerRequestFilter {

    private final EmployeeRepository employeeRepository;

    public AccountStateFilter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Optional<Employee> employee = employeeRepository.findByUsername(authentication.getName());
            boolean valid = employee.isPresent() && employee.get().isActive()
                && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + employee.get().getRole()));
            if (!valid) {
                reject(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                "{\"error\":\"Sesiunea nu mai este validă. Autentifică-te din nou.\",\"fields\":{}}"
                    .getBytes(StandardCharsets.UTF_8));
        } else {
            response.sendRedirect("/login");
        }
    }
}
