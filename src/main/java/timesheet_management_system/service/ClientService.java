package timesheet_management_system.service;

import timesheet_management_system.dto.ClientDto;
import timesheet_management_system.exception.ConflictException;
import timesheet_management_system.exception.ResourceNotFoundException;
import timesheet_management_system.model.Client;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;

    public ClientService(ClientRepository clientRepository, TimesheetEntryRepository timesheetEntryRepository) {
        this.clientRepository = clientRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
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

    public ClientDto update(Long id, ClientDto dto) {
        Client target = find(id);
        String name = dto.name().trim();
        if (clientRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Există deja un client cu această denumire.");
        }
        target.setName(name);
        return toDto(clientRepository.save(target));
    }

    public void delete(Long id) {
        Client target = find(id);
        if (timesheetEntryRepository.existsByClient(target)) {
            throw new ConflictException("Clientul are pontaje și nu poate fi șters. Redenumește-l în loc.");
        }
        clientRepository.delete(target);
    }

    private Client find(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Clientul nu există."));
    }

    private ClientDto toDto(Client client){
        return new ClientDto(client.getId(), client.getName());
    }

    private Client toEntity(ClientDto dto){
        return new Client(dto.name().trim());
    }



}
