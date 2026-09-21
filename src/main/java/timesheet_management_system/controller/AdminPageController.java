package timesheet_management_system.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.model.ActionCatalog;
import timesheet_management_system.model.WorkingMonth;
import timesheet_management_system.service.ClientService;
import timesheet_management_system.service.EmployeeService;
import timesheet_management_system.service.TimesheetEntryService;

@Controller
public class AdminPageController {

    private final TimesheetEntryService timesheetEntryService;
    private final EmployeeService employeeService;
    private final ClientService clientService;
    private final ActionCatalog actionCatalog;

    public AdminPageController(TimesheetEntryService timesheetEntryService,
            EmployeeService employeeService, ClientService clientService, ActionCatalog actionCatalog) {
        this.timesheetEntryService = timesheetEntryService;
        this.employeeService = employeeService;
        this.clientService = clientService;
        this.actionCatalog = actionCatalog;
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication) {
        List<TimesheetEntryDto> entries = timesheetEntryService.findAll().stream()
            .sorted(Comparator.comparing(TimesheetEntryDto::date).reversed())
            .toList();
        Map<Long, Long> entriesPerEmployee = entries.stream()
            .collect(Collectors.groupingBy(TimesheetEntryDto::employeeId, Collectors.counting()));

        model.addAttribute("username", authentication.getName());
        model.addAttribute("entries", entries);
        model.addAttribute("entriesPerEmployee", entriesPerEmployee);
        model.addAttribute("entriesPerClient", entries.stream()
            .filter(entry -> entry.clientId() != null)
            .collect(Collectors.groupingBy(TimesheetEntryDto::clientId, Collectors.counting())));
        model.addAttribute("totalMinutes", entries.stream().mapToInt(TimesheetEntryDto::totalMinutes).sum());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("months", WorkingMonth.values());
        model.addAttribute("taskGroups", actionCatalog.getGroups());
        return "admin";
    }
}
