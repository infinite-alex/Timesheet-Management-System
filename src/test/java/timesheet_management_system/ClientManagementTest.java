package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

class ClientManagementTest extends IntegrationTestBase {

    private ResultActions rename(Object id, String name, RequestPostProcessor auth) throws Exception {
        return mvc.perform(put("/api/clients/" + id).with(auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":" + quote(name) + "}"));
    }

    private ResultActions remove(Object id, RequestPostProcessor auth) throws Exception {
        return mvc.perform(delete("/api/clients/" + id).with(auth));
    }

    private Client clientWithEntry(String name) {
        Client client = clients.save(new Client(name));
        entries.save(new TimesheetEntry(LocalDate.of(2026, 9, 1), worker, client, WorkingMonth.SEPTEMBRIE, 60, List.of(), ""));
        return client;
    }

    @Test
    void adminRenamesAClient_andTheNameIsTrimmed() throws Exception {
        Client firma = clients.save(new Client("Firma Gresita"));

        rename(firma.getId(), "  Firma Corecta SRL  ", asAdmin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firma.getId()))
            .andExpect(jsonPath("$.name").value("Firma Corecta SRL"));

        assertThat(clients.findById(firma.getId()).orElseThrow().getName()).isEqualTo("Firma Corecta SRL");
    }

    @Test
    void renamingAClientWithEntries_keepsTheEntriesLinked() throws Exception {
        Client firma = clientWithEntry("Firma Veche");

        rename(firma.getId(), "Firma Noua", asAdmin()).andExpect(status().isOk());

        assertThat(entries.findAll()).hasSize(1);
        assertThat(entries.findInRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
            .extracting(e -> e.getClient().getName()).containsExactly("Firma Noua");
    }

    @Test
    void keepingTheSameName_isNotAConflict_butTakingAnotherClientsNameIs() throws Exception {
        Client alfa = clients.save(new Client("Alfa SRL"));
        clients.save(new Client("Beta SRL"));

        rename(alfa.getId(), "Alfa SRL", asAdmin()).andExpect(status().isOk());
        rename(alfa.getId(), "ALFA srl", asAdmin()).andExpect(status().isOk());
        rename(alfa.getId(), "beta srl", asAdmin())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(containsString("deja un client")));

        assertThat(clients.findById(alfa.getId()).orElseThrow().getName()).isEqualTo("ALFA srl");
    }

    @Test
    void blankOrTooLongNames_areRejected() throws Exception {
        Client firma = clients.save(new Client("Firma"));

        rename(firma.getId(), "   ", asAdmin()).andExpect(status().isBadRequest());
        rename(firma.getId(), "", asAdmin()).andExpect(status().isBadRequest());
        rename(firma.getId(), "x".repeat(256), asAdmin()).andExpect(status().isBadRequest());

        assertThat(clients.findById(firma.getId()).orElseThrow().getName()).isEqualTo("Firma");
    }

    @Test
    void renamingOrDeletingAMissingClient_isNotFound() throws Exception {
        rename(999999, "Oricare", asAdmin()).andExpect(status().isNotFound());
        remove(999999, asAdmin()).andExpect(status().isNotFound());
    }

    @Test
    void adminDeletesAClientWithoutEntries() throws Exception {
        Client firma = clients.save(new Client("De Sters"));

        remove(firma.getId(), asAdmin()).andExpect(status().isNoContent());

        assertThat(clients.findById(firma.getId())).isEmpty();
    }

    @Test
    void aClientWithEntries_cannotBeDeleted() throws Exception {
        Client firma = clientWithEntry("Cu Pontaje");

        remove(firma.getId(), asAdmin())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(containsString("are pontaje")));

        assertThat(clients.findById(firma.getId())).isPresent();
        assertThat(entries.findAll()).hasSize(1);
    }

    @Test
    void employees_cannotRenameOrDeleteClients() throws Exception {
        Client firma = clients.save(new Client("Firma"));

        rename(firma.getId(), "Alt Nume", asWorker()).andExpect(status().isForbidden());
        remove(firma.getId(), asWorker()).andExpect(status().isForbidden());

        assertThat(clients.findById(firma.getId()).orElseThrow().getName()).isEqualTo("Firma");
    }

    @Test
    void anonymousUsers_cannotRenameOrDeleteClients() throws Exception {
        Client firma = clients.save(new Client("Firma"));

        mvc.perform(put("/api/clients/" + firma.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(delete("/api/clients/" + firma.getId())).andExpect(status().is3xxRedirection());

        assertThat(clients.findById(firma.getId()).orElseThrow().getName()).isEqualTo("Firma");
    }

    @Test
    void adminPage_offersDeleteOnlyForClientsWithoutEntries() throws Exception {
        clients.save(new Client("Fara Pontaje Deloc"));
        clientWithEntry("Cu Pontaje Existente");

        String page = mvc.perform(get("/admin").with(asAdmin())).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-client-edit")))
            .andReturn().getResponse().getContentAsString();

        assertThat(page.split("data-client-delete", -1).length - 1).isEqualTo(1);
    }
}
