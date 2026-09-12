package timesheet_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheet_management_system.dto.ClientDto; 
import java.util.List;
import timesheet_management_system.service.ClientService;

@RestController 
@RequestMapping ("/api/clients")
public class ClientController {
    
    private final ClientService clientService;

    public ClientController(ClientService clientService){
        this.clientService = clientService;
    }
    @GetMapping 
    public List<ClientDto> getAll(){
        return clientService.findAll();
    }
    @PostMapping 
    public ClientDto create(@RequestBody ClientDto dto){
        return clientService.save(dto);
    }
}
