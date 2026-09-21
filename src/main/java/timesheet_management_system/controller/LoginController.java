package timesheet_management_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.security.LoginThrottle;

@Controller
public class LoginController {

    private final LoginThrottle loginThrottle;

    public LoginController(LoginThrottle loginThrottle) {
        this.loginThrottle = loginThrottle;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("lockoutMinutes", loginThrottle.lockoutMinutes());
        return "login";
    }
}