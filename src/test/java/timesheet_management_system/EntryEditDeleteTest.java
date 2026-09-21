package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

class EntryEditDeleteTest extends IntegrationTestBase {

    private static final String URL = "/api/timesheet-entries/";

    private Long create(RequestPostProcessor auth, Long employeeId, String date, int minutes, String note) throws Exception {
        String json = postJson("/api/timesheet-entries", entryJson(date, employeeId, null, "SEPTEMBRIE", minutes, note), auth)
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(json);
        assertThat(m.find()).isTrue();
        return Long.valueOf(m.group(1));
    }

    private ResultActions putEntry(Long id, String body, RequestPostProcessor auth) throws Exception {
        return mvc.perform(put(URL + id).with(auth).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions del(Object id, RequestPostProcessor auth) throws Exception {
        return mvc.perform(delete(URL + id).with(auth));
    }

    private String update(String date, Long employeeId, Long clientId, String month, int minutes, String task, String note) {
        return "{\"date\":%s,\"employeeId\":%s,\"clientId\":%s,\"workingmonth\":%s,\"totalMinutes\":%d,\"actions\":%s,\"extranote\":%s}"
            .formatted(quote(date), employeeId, clientId, quote(month), minutes, task == null ? "[]" : "[" + quote(task) + "]", quote(note));
    }

    private TimesheetEntry reload(Long id) {
        return entries.findById(id).orElseThrow();
    }

    @Test
    void employee_editsOwnEntry_andEveryEditableFieldChanges() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 60, "vechi");
        Client firma = clients.save(new Client("Firma Noua"));

        putEntry(id, update("2026-09-01", null, firma.getId(), "OCTOMBRIE", 135, "Fluturasi", "nou"), asWorker())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.clientName").value("Firma Noua"))
            .andExpect(jsonPath("$.workingmonth").value("OCTOMBRIE"))
            .andExpect(jsonPath("$.totalMinutes").value(135))
            .andExpect(jsonPath("$.actions[0]").value("Fluturasi"))
            .andExpect(jsonPath("$.extranote").value("nou"));

        TimesheetEntry saved = reload(id);
        assertThat(saved.getClient().getName()).isEqualTo("Firma Noua");
        assertThat(saved.getWorkingMonth()).isEqualTo(WorkingMonth.OCTOMBRIE);
        assertThat(saved.getTotalMinutes()).isEqualTo(135);
        assertThat(saved.getActions()).containsExactly("Fluturasi");
        assertThat(saved.getExtraNote()).isEqualTo("nou");
        assertThat(entries.findByEmployee(worker)).hasSize(1);
    }

    @Test
    void edit_neverChangesTheDateOrTheOwner_evenIfTheBodyTriesTo() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 60, "");

        putEntry(id, update("2027-12-24", boss.getId(), null, "SEPTEMBRIE", 30, null, ""), asWorker()).andExpect(status().isOk());

        TimesheetEntry saved = reload(id);
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(saved.getEmployee().getId()).isEqualTo(worker.getId());
        assertThat(saved.getTotalMinutes()).isEqualTo(30);
    }

    @Test
    void edit_canRemoveTheClientAndTheTask() throws Exception {
        Client firma = clients.save(new Client("Firma"));
        Long id = create(asWorker(), null, "2026-09-01", 60, "");
        putEntry(id, update("2026-09-01", null, firma.getId(), "SEPTEMBRIE", 60, "Fluturasi", ""), asWorker()).andExpect(status().isOk());

        putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", 60, null, ""), asWorker())
            .andExpect(status().isOk()).andExpect(jsonPath("$.clientId").doesNotExist());

        assertThat(reload(id).getClient()).isNull();
        assertThat(reload(id).getActions()).isEmpty();
    }

    @Test
    void employee_cannotEditSomeoneElsesEntry_andItStaysUntouched() throws Exception {
        Long id = create(asOther(), null, "2026-09-02", 90, "al lui other");

        putEntry(id, update("2026-09-02", null, null, "SEPTEMBRIE", 5, null, "furat"), asWorker())
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Pontajul nu există."));

        assertThat(reload(id).getTotalMinutes()).isEqualTo(90);
        assertThat(reload(id).getExtraNote()).isEqualTo("al lui other");
    }

    @Test
    void admin_canEditAnyonesEntry() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 60, "");

        putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", 200, "Nota salarii", "corectat"), asAdmin())
            .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(worker.getId()));

        assertThat(reload(id).getTotalMinutes()).isEqualTo(200);
        assertThat(reload(id).getEmployee().getId()).isEqualTo(worker.getId());
    }

    @Test
    void invalidEdits_areRejected_andChangeNothing() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 60, "original");

        for (int minutes : new int[] {0, -1, 1441}) {
            putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", minutes, null, ""), asWorker()).andExpect(status().isBadRequest());
        }
        putEntry(id, update("2026-09-01", null, null, null, 60, null, ""), asWorker()).andExpect(status().isBadRequest());
        putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", 60, null, "x".repeat(256)), asWorker()).andExpect(status().isBadRequest());
        putEntry(id, update(null, null, null, "SEPTEMBRIE", 60, null, ""), asWorker()).andExpect(status().isBadRequest());
        putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", 60, "Sarcina inventata", ""), asWorker()).andExpect(status().isBadRequest());
        putEntry(id, "{\"date\":\"2026-09-01\",\"workingmonth\":\"SEPTEMBRIE\",\"totalMinutes\":60,\"actions\":[\"Fluturasi\",\"TVA\"]}", asWorker())
            .andExpect(status().isBadRequest());
        putEntry(id, update("2026-09-01", null, 999999L, "SEPTEMBRIE", 61, null, "partial"), asWorker())
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value(containsString("Clientul")));
        putEntry(id, "{nu e json", asWorker()).andExpect(status().isBadRequest());

        TimesheetEntry saved = reload(id);
        assertThat(saved.getTotalMinutes()).isEqualTo(60);
        assertThat(saved.getExtraNote()).isEqualTo("original");
        assertThat(saved.getClient()).isNull();
    }

    @Test
    void editingAMissingOrMalformedId_isHandledCleanly() throws Exception {
        putEntry(999999L, update("2026-09-01", null, null, "SEPTEMBRIE", 60, null, ""), asWorker()).andExpect(status().isNotFound());
        mvc.perform(put(URL + "abc").with(asWorker()).contentType(MediaType.APPLICATION_JSON)
            .content(update("2026-09-01", null, null, "SEPTEMBRIE", 60, null, "")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Identificator invalid."));
        del("abc", asWorker()).andExpect(status().isBadRequest());
        del(999999L, asWorker()).andExpect(status().isNotFound());
    }

    @Test
    void employee_deletesOwnEntry() throws Exception {
        Long keep = create(asWorker(), null, "2026-09-01", 10, "pastreaza");
        Long gone = create(asWorker(), null, "2026-09-02", 20, "sterge");

        del(gone, asWorker()).andExpect(status().isNoContent()).andExpect(content().string(""));

        assertThat(entries.findById(gone)).isEmpty();
        assertThat(entries.findById(keep)).isPresent();
        mvc.perform(get("/api/timesheet-entries").with(asWorker())).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deletingTwice_secondTimeIsNotFound() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 10, "");

        del(id, asWorker()).andExpect(status().isNoContent());
        del(id, asWorker()).andExpect(status().isNotFound());
    }

    @Test
    void employee_cannotDeleteSomeoneElsesEntry() throws Exception {
        Long id = create(asOther(), null, "2026-09-02", 90, "");

        del(id, asWorker()).andExpect(status().isNotFound());

        assertThat(entries.findById(id)).isPresent();
    }

    @Test
    void admin_canDeleteAnyEntry() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 10, "");

        del(id, asAdmin()).andExpect(status().isNoContent());

        assertThat(entries.findById(id)).isEmpty();
    }

    @Test
    void anonymousUsers_cannotEditOrDelete() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 10, "");

        mvc.perform(delete(URL + id)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(put(URL + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        assertThat(entries.findById(id)).isPresent();
    }

    @Test
    void deletedEntries_disappearFromPagesAndReports() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 75, "nota-unica-xyz");

        assertThat(mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString()).contains("nota-unica-xyz");
        del(id, asWorker()).andExpect(status().isNoContent());

        assertThat(mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString()).doesNotContain("nota-unica-xyz");
        assertThat(mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString()).doesNotContain("nota-unica-xyz");
        String report = mvc.perform(get("/rapoarte").with(asAdmin()).param("period", "tot")).andReturn().getResponse().getContentAsString();
        assertThat(report).contains("Nu există date");
    }

    @Test
    void editedEntries_areReflectedInReports() throws Exception {
        Long id = create(asWorker(), null, "2026-09-01", 60, "");

        putEntry(id, update("2026-09-01", null, null, "SEPTEMBRIE", 150, null, ""), asWorker()).andExpect(status().isOk());

        String report = mvc.perform(get("/rapoarte").with(asAdmin()).param("period", "tot")).andReturn().getResponse().getContentAsString();
        assertThat(report).contains("2h 30m");
    }

    @Test
    void pagesOfferEditAndDeleteButtonsAndDialogs() throws Exception {
        create(asWorker(), null, "2026-09-01", 10, "");

        for (String html : List.of(
                mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString(),
                mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString())) {
            assertThat(html).contains("data-edit").contains("data-delete").contains("id=\"editDialog\"")
                .contains("id=\"deleteDialog\"").contains("/js/entries.js");
        }
    }

    @Test
    void userDataInRowAttributes_isEscaped() throws Exception {
        create(asWorker(), null, "2026-09-01", 10, "\"><img src=x onerror=alert(1)>");

        for (String html : List.of(
                mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString(),
                mvc.perform(get("/admin").with(asAdmin())).andReturn().getResponse().getContentAsString())) {
            assertThat(html).doesNotContain("<img src=x").doesNotContain("\"><img");
        }
    }

    @Test
    void editRowCarriesEverythingTheDialogNeeds() throws Exception {
        Client firma = clients.save(new Client("Firma Atribute"));
        String created = postJson("/api/timesheet-entries",
            "{\"date\":\"2026-09-05\",\"clientId\":" + firma.getId()
                + ",\"workingmonth\":\"SEPTEMBRIE\",\"totalMinutes\":95,\"actions\":[\"Fluturasi\"],\"extranote\":\"detalii\"}", asWorker())
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String html = mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString();

        assertThat(created).contains("Fluturasi");
        assertThat(html).contains("data-date=\"2026-09-05\"").contains("data-minutes=\"95\"").contains("data-task=\"Fluturasi\"")
            .contains("data-month=\"SEPTEMBRIE\"").contains("data-client-name=\"Firma Atribute\"").contains("data-note=\"detalii\"");
    }
}
