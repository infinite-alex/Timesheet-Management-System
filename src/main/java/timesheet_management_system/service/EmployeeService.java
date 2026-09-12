package timesheet_management_system.service;

import timesheet_management_system.dto.EmployeeDto;
import timesheet_management_system.model.Employee;
import timesheet_management_system.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<EmployeeDto> findAll() {
        List<EmployeeDto> employeeDto = new ArrayList<>();
        List<Employee> employees = employeeRepository.findAll();
        for( Employee employee: employees){
            employeeDto.add(toDto(employee));
        }
        return employeeDto;
    }

    public EmployeeDto save(EmployeeDto dto) {
        Employee savedemployee = employeeRepository.save(toEntity(dto));
        return toDto(savedemployee);
    }
    
    private EmployeeDto toDto(Employee employee){
        return new EmployeeDto(employee.getId(), employee.getName());
    }

    private Employee toEntity(EmployeeDto employeeDto){
        return new Employee(employeeDto.name());
    }

}