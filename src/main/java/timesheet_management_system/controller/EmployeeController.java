package timesheet_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import timesheet_management_system.dto.EmployeeCreateDto;
import timesheet_management_system.dto.EmployeeDto;

import java.util.List;

import timesheet_management_system.service.EmployeeService;

@RestController 
@RequestMapping ("/api/employees")

public class EmployeeController {
    
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService){
        this.employeeService = employeeService;
    }
    @GetMapping
    public List<EmployeeDto> getAll(){
        return employeeService.findAll();
    }
    @PostMapping
    public EmployeeDto create(@RequestBody EmployeeCreateDto dto){
        return employeeService.save(dto);
    }
}
