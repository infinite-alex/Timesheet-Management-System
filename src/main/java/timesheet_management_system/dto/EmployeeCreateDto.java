package timesheet_management_system.dto;

import timesheet_management_system.model.Role;

public record EmployeeCreateDto(String name, String username, String password, Role role) {
}