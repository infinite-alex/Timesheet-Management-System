package timesheet_management_system.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.dto.TimesheetEntryDto;
import timesheet_management_system.model.ActionCatalog;
import timesheet_management_system.model.WorkingMonth;
import timesheet_management_system.service.ClientService;
import timesheet_management_system.service.TimesheetEntryService;

@Controller
public class TimesheetEntryPageController {

    private final TimesheetEntryService timesheetEntryService;
    private final ClientService clientService;
    private final ActionCatalog actionCatalog;

    public TimesheetEntryPageController(TimesheetEntryService timesheetEntryService, ClientService clientService,
            ActionCatalog actionCatalog) {
        this.timesheetEntryService = timesheetEntryService;
        this.clientService = clientService;
        this.actionCatalog = actionCatalog;
    }

    @GetMapping("/pontaj")
    public String myEntries(Model model, Authentication authentication) {
        List<TimesheetEntryDto> entries = timesheetEntryService.findAll().stream()
            .sorted(Comparator.comparing(TimesheetEntryDto::date).reversed())
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("entries", entries);
        model.addAttribute("totalMinutes", entries.stream().mapToInt(TimesheetEntryDto::totalMinutes).sum());
        model.addAttribute("clientCount", entries.stream()
            .map(TimesheetEntryDto::clientId)
            .filter(Objects::nonNull)
            .distinct()
            .count());
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("months", WorkingMonth.values());
        model.addAttribute("taskGroups", actionCatalog.getGroups());
        return "pontaj";
    }
}
