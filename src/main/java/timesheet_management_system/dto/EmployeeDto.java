package timesheet_management_system.dto;

import timesheet_management_system.model.Role;

public record EmployeeDto(Long id, String name, String username, Role role, boolean active) {
}
