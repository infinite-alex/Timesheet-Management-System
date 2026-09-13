package timesheet_management_system.repository;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.TimesheetEntry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {
    List<TimesheetEntry> findByEmployee(Employee employee);
}