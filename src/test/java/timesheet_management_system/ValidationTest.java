package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;

class ValidationTest extends IntegrationTestBase {

    private static final String ENTRY = "/api/timesheet-entries";

    private String employeeJson(String name, String username, String password, String role) {
        return "{\"name\":%s,\"username\":%s,\"password\":%s,\"role\":%s}"
            .formatted(quote(name), quote(username), quote(password), quote(role));
    }

    @Test
    void unknownClient_isNotFound_withAClearMessage() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, 99999L, "SEPTEMBRIE", 60, ""), asWorker())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Clientul cu id 99999 nu există."));
        assertThat(entries.findByEmployee(worker)).isEmpty();
    }

    @Test
    void unknownEmployeeAsAdmin_isNotFound() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", 99999L, null, "SEPTEMBRIE", 60, ""), asAdmin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Angajatul cu id 99999 nu există."));
    }

    @Test
    void adminWithoutEmployeeId_isABadRequest() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", 60, ""), asAdmin())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("alegi angajatul")));
    }

    @Test
    void nonPositiveMinutes_areRejected() throws Exception {
        for (int minutes : new int[] {0, -5, -100000}) {
            postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", minutes, ""), asWorker())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.totalMinutes").value(containsString("cel puțin 1 minut")));
        }
        assertThat(entries.findByEmployee(worker)).isEmpty();
    }

    @Test
    void moreThan24Hours_isRejected() throws Exception {
        for (int minutes : new int[] {1441, 5000, Integer.MAX_VALUE}) {
            postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", minutes, ""), asWorker())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.totalMinutes").value(containsString("24 de ore")));
        }
    }

    @Test
    void boundaryDurations_1And1440_areAccepted() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", 1, ""), asWorker()).andExpect(status().isOk());
        postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", 1440, ""), asWorker()).andExpect(status().isOk());
        assertThat(entries.findByEmployee(worker)).hasSize(2);
    }

    @Test
    void missingDate_isRejected() throws Exception {
        postJson(ENTRY, entryJson(null, null, null, "SEPTEMBRIE", 60, ""), asWorker())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.date").value("Data este obligatorie."));
    }

    @Test
    void missingMonth_isRejected() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, null, null, 60, ""), asWorker())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.workingmonth").value("Luna este obligatorie."));
    }

    @Test
    void note_isLimitedTo255Characters() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", 60, "x".repeat(256)), asWorker())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.extranote").exists());
        postJson(ENTRY, entryJson("2026-09-01", null, null, "SEPTEMBRIE", 60, "x".repeat(255)), asWorker())
            .andExpect(status().isOk());
    }

    @Test
    void missingActionsList_isTreatedAsEmpty() throws Exception {
        postJson(ENTRY, "{\"date\":\"2026-09-01\",\"workingmonth\":\"MAI\",\"totalMinutes\":30}", asWorker())
            .andExpect(status().isOk()).andExpect(jsonPath("$.actions.length()").value(0));
    }

    @Test
    void severalProblemsAtOnce_areAllReportedByField() throws Exception {
        postJson(ENTRY, entryJson(null, null, null, null, 0, ""), asWorker())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.date").exists())
            .andExpect(jsonPath("$.fields.workingmonth").exists())
            .andExpect(jsonPath("$.fields.totalMinutes").exists());
    }

    @Test
    void unreadableJson_returnsAJsonError_notAnEmptyBody() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, null, "FOO", 60, ""), asWorker())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        postJson(ENTRY, "{nu e json", asWorker())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void errorResponses_neverLeakInternals() throws Exception {
        postJson(ENTRY, entryJson("2026-09-01", null, 99999L, "SEPTEMBRIE", 60, ""), asWorker())
            .andExpect(content().string(not(containsString("Exception"))))
            .andExpect(content().string(not(containsString("org.springframework"))))
            .andExpect(content().string(not(containsString("SQL"))));
    }

    @Test
    void blankEmployeeFields_areRejected() throws Exception {
        postJson("/api/employees", employeeJson("", "", "", "ANGAJAT"), asAdmin())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists())
            .andExpect(jsonPath("$.fields.username").exists())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void employeeWithoutRole_isRejected() throws Exception {
        postJson("/api/employees", "{\"name\":\"Fara Rol\",\"username\":\"farol\",\"password\":\"secret123\"}", asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.role").value("Rolul este obligatoriu."));
        assertThat(employees.findByUsername("farol")).isEmpty();
    }

    @Test
    void employeeWithoutUsername_isRejected() throws Exception {
        postJson("/api/employees", "{\"name\":\"X\",\"password\":\"secret123\",\"role\":\"ANGAJAT\"}", asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.username").exists());
    }

    @Test
    void weakOrOddEmployeeData_isRejected() throws Exception {
        postJson("/api/employees", employeeJson("Ana", "ana", "scurta", "ANGAJAT"), asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.password").exists());
        postJson("/api/employees", employeeJson("Ana", "ana", "a".repeat(73), "ANGAJAT"), asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.password").exists());
        postJson("/api/employees", employeeJson("Ana", "a b", "secret123", "ANGAJAT"), asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.username").exists());
        postJson("/api/employees", employeeJson("Ana", "x'; DROP TABLE employee;--", "secret123", "ANGAJAT"), asAdmin())
            .andExpect(status().isBadRequest());
        postJson("/api/employees", employeeJson("Ana", "ana", "secret123", "SUPERADMIN"), asAdmin())
            .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsername_isAConflict_evenWithDifferentCase() throws Exception {
        postJson("/api/employees", employeeJson("A", "dup", "secret123", "ANGAJAT"), asAdmin()).andExpect(status().isOk());

        postJson("/api/employees", employeeJson("B", "dup", "secret123", "ANGAJAT"), asAdmin())
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Username-ul este deja folosit."));
        postJson("/api/employees", employeeJson("C", "DUP", "secret123", "ANGAJAT"), asAdmin())
            .andExpect(status().isConflict());
        assertThat(employees.findAll().stream().filter(e -> e.getUsername().equalsIgnoreCase("dup"))).hasSize(1);
    }

    @Test
    void cannotCreateAUserOverTheBuiltInNames() throws Exception {
        postJson("/api/employees", employeeJson("Fals", "boss", "secret123", "ADMIN"), asAdmin())
            .andExpect(status().isConflict());
    }

    @Test
    void blankOrHugeClientName_isRejected() throws Exception {
        long before = clients.count();

        postJson("/api/clients", "{\"name\":\"   \"}", asAdmin()).andExpect(status().isBadRequest());
        postJson("/api/clients", "{\"name\":\"\"}", asAdmin()).andExpect(status().isBadRequest());
        postJson("/api/clients", "{}", asAdmin()).andExpect(status().isBadRequest());
        postJson("/api/clients", "{\"name\":\"" + "x".repeat(256) + "\"}", asAdmin()).andExpect(status().isBadRequest());

        assertThat(clients.count()).isEqualTo(before);
    }

    @Test
    void clientNameOf255Characters_isAccepted() throws Exception {
        postJson("/api/clients", "{\"name\":\"" + "x".repeat(255) + "\"}", asAdmin()).andExpect(status().isOk());
    }

    @Test
    void employees_cannotListOtherEmployees() throws Exception {
        mvc.perform(get("/api/employees").with(asWorker())).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void employeeWithoutRole_cannotLogIn_butDoesNotCrash() throws Exception {
        employees.save(new Employee("Fara Rol", "farol2", encoder.encode(PASSWORD), null));

        mvc.perform(formLogin().user("farol2").password(PASSWORD)).andExpect(unauthenticated());
    }

    @Test
    void validationFailures_neverCreateRows() throws Exception {
        long entriesBefore = entries.count();
        long employeesBefore = employees.count();
        long clientsBefore = clients.count();

        postJson(ENTRY, entryJson(null, null, null, null, -1, ""), asWorker());
        postJson("/api/employees", employeeJson("", "", "", null), asAdmin());
        postJson("/api/clients", "{\"name\":\"\"}", asAdmin());

        assertThat(entries.count()).isEqualTo(entriesBefore);
        assertThat(employees.count()).isEqualTo(employeesBefore);
        assertThat(clients.count()).isEqualTo(clientsBefore);
        assertThat(clients.findAll()).extracting(Client::getName).doesNotContain("");
    }
}
