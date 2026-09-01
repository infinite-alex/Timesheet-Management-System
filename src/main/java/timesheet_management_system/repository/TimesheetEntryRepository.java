package timesheet_management_system.repository;

import timesheet_management_system.model.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {
}