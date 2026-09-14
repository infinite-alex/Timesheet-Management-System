package timesheet_management_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import timesheet_management_system.service.TimesheetEntryService;

@Controller
public class TimesheetEntryPageController {

    private final TimesheetEntryService timesheetEntryService;

    public TimesheetEntryPageController(TimesheetEntryService timesheetEntryService) {
        this.timesheetEntryService = timesheetEntryService;
    }

    @GetMapping("/pontaj")
    public String myEntries(Model model) {
        model.addAttribute("entries", timesheetEntryService.findAll());
        return "pontaj";
    }
}
