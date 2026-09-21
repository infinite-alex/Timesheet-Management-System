package timesheet_management_system.repository;
import java.util.Optional;
import timesheet_management_system.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import timesheet_management_system.model.Role;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

    @Query("select count(e) from Employee e where e.role = :role and (e.active is null or e.active = true)")
    long countActiveByRole(@org.springframework.data.repository.query.Param("role") Role role);
}