package timesheet_management_system.service;

import timesheet_management_system.dto.ClientDto;
import timesheet_management_system.model.Client;
import timesheet_management_system.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientDto> findAll() {
        List<ClientDto> clientDto = new ArrayList<>();
        List<Client> clients = clientRepository.findAll();
        for( Client client: clients){
            clientDto.add(toDto(client));
        }
        return clientDto;
    }

    public ClientDto save(ClientDto clientdto) {
        Client savedclient = clientRepository.save(toEntity(clientdto));
        return toDto(savedclient);
        
    }

    private ClientDto toDto(Client client){
        return new ClientDto(client.getId(), client.getName());
    }

    private Client toEntity(ClientDto dto){
        return new Client(dto.name());
    }



}