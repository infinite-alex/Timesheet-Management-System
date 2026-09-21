package timesheet_management_system.repository;

import timesheet_management_system.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}