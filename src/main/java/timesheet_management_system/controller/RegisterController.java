package timesheet_management_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import timesheet_management_system.security.LoginThrottle;
import timesheet_management_system.service.EmployeeService;
import timesheet_management_system.service.RegistrationException;

@Controller
public class RegisterController {

    private final EmployeeService employeeService;
    private final LoginThrottle loginThrottle;

    public RegisterController(EmployeeService employeeService, LoginThrottle loginThrottle) {
        this.employeeService = employeeService;
        this.loginThrottle = loginThrottle;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String confirmPassword,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!loginThrottle.registrationAllowed(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            model.addAttribute("error", "Prea multe încercări de înregistrare. Încearcă din nou peste "
                + loginThrottle.lockoutMinutes() + " de minute.");
            model.addAttribute("name", name);
            model.addAttribute("username", username);
            return "register";
        }
        try {
            employeeService.register(name, username, password, confirmPassword);
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("username", username);
            return "register";
        }
        return "redirect:/login?registered";
    }
}
