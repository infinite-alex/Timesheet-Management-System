package timesheet_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.security.AccountStateFilter;
import timesheet_management_system.security.LoginThrottle;
import timesheet_management_system.security.LoginThrottleFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, EmployeeRepository employeeRepository,
            LoginThrottle loginThrottle) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler onSuccess = new SavedRequestAwareAuthenticationSuccessHandler();
        SimpleUrlAuthenticationFailureHandler onFailure = new SimpleUrlAuthenticationFailureHandler("/login?error");

        http
            .addFilterBefore(new LoginThrottleFilter(loginThrottle), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AccountStateFilter(employeeRepository), AuthorizationFilter.class)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin", "/rapoarte", "/rapoarte/**").hasRole("ADMIN")
                .requestMatchers("/api/employees", "/api/employees/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/clients").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    loginThrottle.loginSucceeded(authentication.getName());
                    onSuccess.onAuthenticationSuccess(request, response, authentication);
                })
                .failureHandler((request, response, exception) -> {
                    loginThrottle.loginFailed(request.getParameter("username"), request.getRemoteAddr());
                    onFailure.onAuthenticationFailure(request, response, exception);
                })
                .permitAll()
            );

        return http.build();
    }
}
