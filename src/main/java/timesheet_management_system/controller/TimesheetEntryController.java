package timesheet_management_system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import timesheet_management_system.dto.TimesheetEntryDto;

import timesheet_management_system.service.TimesheetEntryService;

@RestController 
@RequestMapping ("/api/timesheet-entries")
public class TimesheetEntryController {
    
    private final TimesheetEntryService timesheetEntryService;

    public TimesheetEntryController(TimesheetEntryService timesheetEntryService){
        this.timesheetEntryService = timesheetEntryService;
    }
    @GetMapping
    public List<TimesheetEntryDto> getAll(){
        return timesheetEntryService.findAll();
    }
    @PostMapping
    public TimesheetEntryDto create(@RequestBody TimesheetEntryDto dto){
        return timesheetEntryService.save(dto);
    }
}

