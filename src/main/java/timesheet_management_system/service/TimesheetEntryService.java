package timesheet_management_system.service;

import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.repository.TimesheetEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimesheetEntryService {

    private final TimesheetEntryRepository timesheetEntryRepository;

    public TimesheetEntryService(TimesheetEntryRepository timesheetEntryRepository) {
        this.timesheetEntryRepository = timesheetEntryRepository;
    }

    public List<TimesheetEntry> findAll() {
        return timesheetEntryRepository.findAll();
    }

    public TimesheetEntry save(TimesheetEntry timesheetEntry) {
        return timesheetEntryRepository.save(timesheetEntry);
    }
}