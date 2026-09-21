package timesheet_management_system.repository;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.TimesheetEntry;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {
    List<TimesheetEntry> findByEmployee(Employee employee);

    boolean existsByEmployee(Employee employee);

    boolean existsByClient(Client client);

    @Query("""
        select distinct e from TimesheetEntry e
        join fetch e.employee
        left join fetch e.client
        left join fetch e.actions
        where e.date >= :from and e.date <= :to
        """)
    List<TimesheetEntry> findInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
