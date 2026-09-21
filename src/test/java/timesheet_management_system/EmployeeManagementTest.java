package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

class EmployeeManagementTest extends IntegrationTestBase {

    private ResultActions edit(Employee target, String name, String role, Boolean active, String newPassword, RequestPostProcessor auth) throws Exception {
        String body = "{\"name\":%s,\"role\":%s,\"active\":%s,\"newPassword\":%s}"
            .formatted(quote(name), quote(role), active, quote(newPassword));
        return mvc.perform(put("/api/employees/" + target.getId()).with(auth).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions remove(Object id, RequestPostProcessor auth) throws Exception {
        return mvc.perform(delete("/api/employees/" + id).with(auth));
    }

    private void addEntry(Employee employee) {
        entries.save(new TimesheetEntry(LocalDate.of(2026, 9, 1), employee, null, WorkingMonth.SEPTEMBRIE, 60, List.of(), ""));
    }

    private void deactivate(Employee e) {
        e.setActive(false);
        employees.save(e);
    }

    @Test
    void adminEditsNameAndRole() throws Exception {
        edit(worker, "  Nume Nou Complet  ", "ADMIN", true, null, asAdmin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nume Nou Complet"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.username").value("worker"))
            .andExpect(jsonPath("$.password").doesNotExist());

        Employee saved = employees.findById(worker.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Nume Nou Complet");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void usernameCanNotBeChanged_evenIfTheBodyContainsOne() throws Exception {
        mvc.perform(put("/api/employees/" + worker.getId()).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Nume worker\",\"role\":\"ANGAJAT\",\"active\":true,\"username\":\"hacker\"}"))
            .andExpect(status().isOk());

        assertThat(employees.findById(worker.getId()).orElseThrow().getUsername()).isEqualTo("worker");
    }

    @Test
    void adminResetsAPassword_theEmployeeCanLogInWithTheNewOneOnly() throws Exception {
        edit(worker, "Nume worker", "ANGAJAT", true, "ResetDeAdmin-2026", asAdmin()).andExpect(status().isOk());

        mvc.perform(formLogin().user("worker").password("ResetDeAdmin-2026")).andExpect(authenticated().withUsername("worker"));
        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(unauthenticated());
        mvc.perform(formLogin().user("other").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void blankPasswordField_keepsTheCurrentPassword() throws Exception {
        edit(worker, "Alt Nume", "ANGAJAT", true, "", asAdmin()).andExpect(status().isOk());
        edit(worker, "Alt Nume Din Nou", "ANGAJAT", true, "     ", asAdmin()).andExpect(status().isOk());
        edit(worker, "Al Treilea", "ANGAJAT", true, null, asAdmin()).andExpect(status().isOk());

        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void weakResetPasswords_areRejected() throws Exception {
        edit(worker, "Nume worker", "ANGAJAT", true, "scurta", asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("cel puțin 8")));
        edit(worker, "Nume worker", "ANGAJAT", true, "a".repeat(73), asAdmin()).andExpect(status().isBadRequest());

        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void deactivatedEmployee_cannotLogIn_andCanBeReactivated() throws Exception {
        edit(worker, "Nume worker", "ANGAJAT", false, null, asAdmin())
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));

        edit(worker, "Nume worker", "ANGAJAT", true, null, asAdmin()).andExpect(status().isOk());
        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void deactivatedEmployee_isCutOffImmediately_evenWithAnOpenSession() throws Exception {
        mvc.perform(get("/pontaj").with(asWorker())).andExpect(status().isOk());

        deactivate(worker);

        mvc.perform(get("/pontaj").with(asWorker())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/timesheet-entries").with(asWorker()))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value(containsString("Sesiunea")));
    }

    @Test
    void deletedEmployee_isCutOffImmediately() throws Exception {
        Employee temp = createEmployee("temporar", Role.ANGAJAT);
        mvc.perform(get("/pontaj").with(user("temporar").roles("ANGAJAT"))).andExpect(status().isOk());

        remove(temp.getId(), asAdmin()).andExpect(status().isNoContent());

        mvc.perform(get("/pontaj").with(user("temporar").roles("ANGAJAT"))).andExpect(status().is3xxRedirection());
    }

    @Test
    void roleChange_takesEffectAtOnce_inBothDirections() throws Exception {
        worker.setRole(Role.ADMIN);
        employees.save(worker);
        mvc.perform(get("/admin").with(asWorker())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        mvc.perform(get("/admin").with(user("worker").roles("ADMIN"))).andExpect(status().isOk());

        other.setRole(Role.ANGAJAT);
        boss.setRole(Role.ANGAJAT);
        employees.save(boss);
        mvc.perform(get("/admin").with(asAdmin())).andExpect(status().is3xxRedirection());
    }

    @Test
    void demotedAdmin_cannotUseAdminFeaturesAnymore() throws Exception {
        edit(worker, "Nume worker", "ADMIN", true, null, asAdmin()).andExpect(status().isOk());
        mvc.perform(get("/rapoarte").with(user("worker").roles("ADMIN"))).andExpect(status().isOk());

        edit(worker, "Nume worker", "ANGAJAT", true, null, asAdmin()).andExpect(status().isOk());

        mvc.perform(get("/rapoarte").with(user("worker").roles("ADMIN"))).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin").with(asWorker())).andExpect(status().isForbidden());
    }

    @Test
    void anAdmin_cannotChangeTheirOwnRoleOrStatus_butCanRenameThemselves() throws Exception {
        edit(boss, "Nume boss", "ANGAJAT", true, null, asAdmin())
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("propriul rol")));
        edit(boss, "Nume boss", "ADMIN", false, null, asAdmin()).andExpect(status().isBadRequest());
        assertThat(employees.findById(boss.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(employees.findById(boss.getId()).orElseThrow().isActive()).isTrue();

        edit(boss, "Boss Redenumit", "ADMIN", true, null, asAdmin()).andExpect(status().isOk());
        assertThat(employees.findById(boss.getId()).orElseThrow().getName()).isEqualTo("Boss Redenumit");
    }

    @Test
    void anAdmin_canDeactivateAnotherAdmin_whileAnotherStaysActive() throws Exception {
        Employee second = createEmployee("boss2", Role.ADMIN);

        edit(second, "Nume boss2", "ADMIN", false, null, asAdmin()).andExpect(status().isOk());

        assertThat(employees.findById(second.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void invalidEdits_areRejected() throws Exception {
        edit(worker, "", "ANGAJAT", true, null, asAdmin()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
        edit(worker, "   ", "ANGAJAT", true, null, asAdmin()).andExpect(status().isBadRequest());
        edit(worker, "x".repeat(101), "ANGAJAT", true, null, asAdmin()).andExpect(status().isBadRequest());
        edit(worker, "Nume", null, true, null, asAdmin()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.role").exists());
        edit(worker, "Nume", "SUPERADMIN", true, null, asAdmin()).andExpect(status().isBadRequest());
        edit(worker, "Nume", "ANGAJAT", null, null, asAdmin()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.active").exists());

        assertThat(employees.findById(worker.getId()).orElseThrow().getName()).isEqualTo("Nume worker");
    }

    @Test
    void editingAnUnknownEmployee_isNotFound() throws Exception {
        mvc.perform(put("/api/employees/999999").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"X\",\"role\":\"ANGAJAT\",\"active\":true}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Angajatul nu există."));
        remove(999999L, asAdmin()).andExpect(status().isNotFound());
        remove("abc", asAdmin()).andExpect(status().isBadRequest());
    }

    @Test
    void employeesCannotEditOrDeleteEmployees() throws Exception {
        edit(other, "Furat", "ADMIN", true, "Hacker-Parola-1", asWorker()).andExpect(status().isForbidden());
        remove(other.getId(), asWorker()).andExpect(status().isForbidden());

        assertThat(employees.findById(other.getId()).orElseThrow().getRole()).isEqualTo(Role.ANGAJAT);
        mvc.perform(formLogin().user("other").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void anonymousUsersCannotEditOrDeleteEmployees() throws Exception {
        mvc.perform(delete("/api/employees/" + other.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(put("/api/employees/" + other.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().is3xxRedirection());
        assertThat(employees.findById(other.getId())).isPresent();
    }

    @Test
    void employeeWithoutEntries_canBeDeleted() throws Exception {
        remove(other.getId(), asAdmin()).andExpect(status().isNoContent());

        assertThat(employees.findById(other.getId())).isEmpty();
        mvc.perform(formLogin().user("other").password(PASSWORD)).andExpect(unauthenticated());
    }

    @Test
    void employeeWithEntries_cannotBeDeleted_butCanBeDeactivated() throws Exception {
        addEntry(worker);

        remove(worker.getId(), asAdmin())
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value(containsString("Dezactivează")));
        assertThat(employees.findById(worker.getId())).isPresent();
        assertThat(entries.findByEmployee(worker)).hasSize(1);

        edit(worker, "Nume worker", "ANGAJAT", false, null, asAdmin()).andExpect(status().isOk());
        assertThat(entries.findByEmployee(worker)).hasSize(1);
    }

    @Test
    void deactivatedEmployeeHours_stayInReports() throws Exception {
        Client firma = clients.save(new Client("Firma Raport"));
        entries.save(new TimesheetEntry(LocalDate.of(2026, 9, 1), worker, firma, WorkingMonth.SEPTEMBRIE, 90, List.of(), ""));
        edit(worker, "Nume worker", "ANGAJAT", false, null, asAdmin()).andExpect(status().isOk());

        String report = mvc.perform(get("/rapoarte").with(asAdmin()).param("period", "tot").param("view", "angajati"))
            .andReturn().getResponse().getContentAsString();

        assertThat(report).contains("Nume worker").contains("1h 30m");
    }

    @Test
    void anAdmin_cannotDeleteThemselves() throws Exception {
        remove(boss.getId(), asAdmin()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("propriul cont")));

        assertThat(employees.findById(boss.getId())).isPresent();
    }

    @Test
    void listingEmployees_includesTheStatus_andNeverThePassword() throws Exception {
        deactivate(other);

        mvc.perform(get("/api/employees").with(asAdmin()))
            .andExpect(jsonPath("$[?(@.username=='other')].active").value(false))
            .andExpect(jsonPath("$[?(@.username=='worker')].active").value(true))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("password"))));
    }

    @Test
    void legacyEmployeesWithoutTheFlag_countAsActive() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "active", null);
        employees.save(worker);

        assertThat(employees.findById(worker.getId()).orElseThrow().isActive()).isTrue();
        mvc.perform(get("/pontaj").with(asWorker())).andExpect(status().isOk());
        mvc.perform(formLogin().user("worker").password(PASSWORD)).andExpect(authenticated());
    }

    @Test
    void adminPage_showsStatusEntryCounts_andOffersDeleteOnlyWhenAllowed() throws Exception {
        addEntry(worker);
        deactivate(other);

        String html = mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Dezactivat").contains("Activ").contains("data-emp-edit").contains("id=\"empEditDialog\"")
            .contains("id=\"empDeleteDialog\"").contains("/js/employees.js");
        assertThat(rowOf(html, "worker")).doesNotContain("data-emp-delete");
        assertThat(rowOf(html, "boss")).doesNotContain("data-emp-delete").contains("data-emp-self=\"true\"");
        assertThat(rowOf(html, "other")).contains("data-emp-delete").contains("is-off");
    }

    @Test
    void employeeNamesInAttributes_areEscaped() throws Exception {
        employees.save(new Employee("\"><img src=x onerror=alert(1)>", "xssuser", "h", Role.ANGAJAT));

        String html = mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("<img src=x").doesNotContain("\"><img");
    }

    private static String rowOf(String html, String username) {
        int at = html.indexOf("data-emp-username=\"" + username + "\"");
        assertThat(at).as("rand pentru " + username).isPositive();
        int start = html.lastIndexOf("<tr", at);
        int end = html.indexOf("</tr>", at);
        return html.substring(start, end);
    }
}
