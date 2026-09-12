package timesheet_management_system.service;
import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.model.Employee;
import timesheet_management_system.repository.EmployeeRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<EmployeeDto> findAll() {
        List<EmployeeDto> employeeDto = new ArrayList<>();
        List<Employee> employees = employeeRepository.findAll();
        for( Employee employee: employees){
            employeeDto.add(toDto(employee));
        }
        return employeeDto;
    }

    public EmployeeDto save(EmployeeCreateDto dto) {
        Employee savedemployee = employeeRepository.save(toEntity(dto));
        return toDto(savedemployee);
    }
    
    private EmployeeDto toDto(Employee employee){
        return new EmployeeDto(employee.getId(),
         employee.getName(), employee.getUsername(), employee.getRole());
    }

    private Employee toEntity(EmployeeCreateDto employeeDto){
        String hashedPassword = passwordEncoder.encode(employeeDto.password());
        return new Employee(employeeDto.name(),employeeDto.username(),hashedPassword,employeeDto.role());
    }

}