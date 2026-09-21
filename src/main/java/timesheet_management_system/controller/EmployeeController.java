package timesheet_management_system.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.dto.EmployeeUpdateDto;
import timesheet_management_system.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeDto> getAll() {
        return employeeService.findAll();
    }

    @PostMapping
    public EmployeeDto create(@Valid @RequestBody EmployeeCreateDto dto) {
        return employeeService.save(dto);
    }

    @PutMapping("/{id}")
    public EmployeeDto update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateDto dto,
            Authentication authentication) {
        return employeeService.update(id, dto, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        employeeService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
