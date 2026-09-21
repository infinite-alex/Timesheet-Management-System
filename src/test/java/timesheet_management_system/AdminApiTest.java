package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import timesheet_management_system.config.AdminSeeder;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;

class AdminApiTest extends IntegrationTestBase {

    @Autowired AdminSeeder adminSeeder;

    @Test
    void admin_createsAnEmployee_andThePasswordIsNeverReturned() throws Exception {
        postJson("/api/employees", "{\"name\":\"Ion Pop\",\"username\":\"ion\",\"password\":\"secret123\",\"role\":\"ANGAJAT\"}", asAdmin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("ion"))
            .andExpect(jsonPath("$.role").value("ANGAJAT"))
            .andExpect(jsonPath("$.password").doesNotExist());

        Employee stored = employees.findByUsername("ion").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secret123").startsWith("$2");
    }

    @Test
    void employeeCreatedByAdmin_canLogIn() throws Exception {
        postJson("/api/employees", "{\"name\":\"Ion\",\"username\":\"ion2\",\"password\":\"secret123\",\"role\":\"ADMIN\"}", asAdmin());

        mvc.perform(formLogin().user("ion2").password("secret123")).andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void listingEmployees_neverLeaksPasswordHashes() throws Exception {
        mvc.perform(get("/api/employees").with(asAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("$2"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("password"))));
    }

    @Test
    void admin_createsAClient_andItAppearsInTheList() throws Exception {
        postJson("/api/clients", "{\"name\":\"Firma Noua SRL\"}", asAdmin())
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Firma Noua SRL"));

        mvc.perform(get("/api/clients").with(asWorker()))
            .andExpect(jsonPath("$[?(@.name=='Firma Noua SRL')]").exists());
    }

    @Test
    void adminPage_showsCountsForEmployeesAndClients() throws Exception {
        postJson("/api/clients", "{\"name\":\"C1\"}", asAdmin());
        postJson("/api/clients", "{\"name\":\"C2\"}", asAdmin());

        String page = mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString();

        assertThat(page).contains("C1").contains("C2").contains("Nume worker").contains("Nume boss");
    }

    @Test
    void seeder_createsExactlyOneAdmin_evenWhenRunTwice() {
        adminSeeder.run();
        adminSeeder.run();

        long admins = employees.findAll().stream().filter(e -> "admin".equals(e.getUsername())).count();
        assertThat(admins).isEqualTo(1);
        assertThat(employees.findByUsername("admin").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
