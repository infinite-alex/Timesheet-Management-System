package timesheet_management_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import timesheet_management_system.service.EmployeeService;
import timesheet_management_system.service.RegistrationException;

@Controller
public class RegisterController {

    private final EmployeeService employeeService;

    public RegisterController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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
            Model model) {
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
