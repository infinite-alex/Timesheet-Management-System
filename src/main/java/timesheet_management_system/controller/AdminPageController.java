package timesheet_management_system.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.service.ClientService;
import timesheet_management_system.service.EmployeeService;
import timesheet_management_system.service.TimesheetEntryService;

@Controller
public class AdminPageController {

    private final TimesheetEntryService timesheetEntryService;
    private final EmployeeService employeeService;
    private final ClientService clientService;

    public AdminPageController(TimesheetEntryService timesheetEntryService,
            EmployeeService employeeService, ClientService clientService) {
        this.timesheetEntryService = timesheetEntryService;
        this.employeeService = employeeService;
        this.clientService = clientService;
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication) {
        List<TimesheetEntryDto> entries = timesheetEntryService.findAll().stream()
            .sorted(Comparator.comparing(TimesheetEntryDto::date).reversed())
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("entries", entries);
        model.addAttribute("totalMinutes", entries.stream().mapToInt(TimesheetEntryDto::totalMinutes).sum());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("clients", clientService.findAll());
        return "admin";
    }
}
