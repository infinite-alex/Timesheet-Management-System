package timesheet_management_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.model.Role;
import timesheet_management_system.service.AccountService;

@Controller
public class AccountPageController {

    private final AccountService accountService;

    public AccountPageController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/cont")
    public String account(Authentication authentication, Model model) {
        EmployeeDto profile = accountService.profile(authentication.getName());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("profile", profile);
        model.addAttribute("roleLabel", profile.role() == Role.ADMIN ? "Administrator" : "Angajat");
        return "cont";
    }
}
