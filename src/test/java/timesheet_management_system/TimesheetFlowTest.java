package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import timesheet_management_system.model.Client;

class TimesheetFlowTest extends IntegrationTestBase {

    private Client client(String name) {
        return clients.save(new Client(name));
    }

    private String body(String url, org.springframework.test.web.servlet.request.RequestPostProcessor auth) throws Exception {
        return mvc.perform(get(url).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void employee_savesAnEntry_andSeesItWithNames() throws Exception {
        Client firma = client("Firma Alfa SRL");

        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, firma.getId(), "SEPTEMBRIE", 90, "nota"), asWorker())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value(worker.getId()))
            .andExpect(jsonPath("$.employeeName").value("Nume worker"))
            .andExpect(jsonPath("$.clientName").value("Firma Alfa SRL"))
            .andExpect(jsonPath("$.totalMinutes").value(90));

        mvc.perform(get("/api/timesheet-entries").with(asWorker()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].clientName").value("Firma Alfa SRL"));
    }

    @Test
    void employee_cannotSaveAnEntryForSomeoneElse() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", boss.getId(), null, "SEPTEMBRIE", 60, "spoof"), asWorker())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value(worker.getId()));

        assertThat(entries.findByEmployee(boss)).isEmpty();
        assertThat(entries.findByEmployee(worker)).hasSize(1);
    }

    @Test
    void employee_seesOnlyOwnEntries_andAdminSeesEverything() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 10, "a"), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-09-02", null, null, "SEPTEMBRIE", 20, "b"), asOther());
        postJson("/api/timesheet-entries", entryJson("2026-09-03", null, null, "SEPTEMBRIE", 30, "c"), asOther());

        mvc.perform(get("/api/timesheet-entries").with(asWorker())).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/timesheet-entries").with(asOther())).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/timesheet-entries").with(asAdmin())).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void admin_canSaveAnEntryForAnEmployee() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", worker.getId(), null, "SEPTEMBRIE", 45, "de admin"), asAdmin())
            .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(worker.getId()));

        assertThat(entries.findByEmployee(worker)).hasSize(1);
    }

    @Test
    void entryWithoutClient_isAllowed_andShownAsNoClient() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 30, ""), asWorker())
            .andExpect(status().isOk()).andExpect(jsonPath("$.clientId").doesNotExist());

        assertThat(body("/pontaj", asWorker())).contains("Fără client");
    }

    @Test
    void invalidMonth_isRejectedAsBadRequest() throws Exception {
        for (String month : new String[] {"FOO", "iulie", "", "13"}) {
            postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, month, 60, ""), asWorker())
                .andExpect(status().isBadRequest());
        }
        assertThat(entries.findByEmployee(worker)).isEmpty();
    }

    @Test
    void invalidDate_isRejectedAsBadRequest() throws Exception {
        for (String date : new String[] {"31-12-2026", "2026-13-40", "azi", "2026-02-30"}) {
            postJson("/api/timesheet-entries", entryJson(date, null, null, "SEPTEMBRIE", 60, ""), asWorker())
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void malformedJson_isRejectedAsBadRequest() throws Exception {
        postJson("/api/timesheet-entries", "{nu e json", asWorker()).andExpect(status().isBadRequest());
        postJson("/api/timesheet-entries", "", asWorker()).andExpect(status().isBadRequest());
    }

    @Test
    void wrongContentType_isRejected() throws Exception {
        mvc.perform(post("/api/timesheet-entries").with(asWorker()).contentType("text/plain").content("x"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void timesheetPage_showsOnlyTheUsersOwnEntries() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 10, "nota-worker"), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-09-02", null, null, "SEPTEMBRIE", 20, "nota-other"), asOther());

        String page = body("/pontaj", asWorker());

        assertThat(page).contains("nota-worker").doesNotContain("nota-other");
    }

    @Test
    void timesheetPage_listsNewestFirst() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-01-05", null, null, "IANUARIE", 10, ""), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-03-10", null, null, "MARTIE", 10, ""), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-02-01", null, null, "FEBRUARIE", 10, ""), asWorker());

        String page = body("/pontaj", asWorker());

        assertThat(page.indexOf("10.03.2026")).isPositive().isLessThan(page.indexOf("01.02.2026"));
        assertThat(page.indexOf("01.02.2026")).isLessThan(page.indexOf("05.01.2026"));
    }

    @Test
    void timesheetPage_showsTotalsFormattedAsHoursAndMinutes() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 60, ""), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-09-02", null, null, "SEPTEMBRIE", 75, ""), asWorker());

        String page = body("/pontaj", asWorker());

        assertThat(page).contains("2h 15m").contains("1h 0m").contains("1h 15m");
    }

    @Test
    void timesheetPage_withNoEntries_showsTheEmptyState() throws Exception {
        assertThat(body("/pontaj", asWorker())).contains("Niciun pontaj încă").contains("0h 0m");
    }

    @Test
    void timesheetPage_offersEveryClientAndEveryMonth() throws Exception {
        client("Firma Unu");
        client("Firma Doi");

        String page = body("/pontaj", asWorker());

        assertThat(page).contains("Firma Unu").contains("Firma Doi").contains("Ianuarie").contains("Decembrie");
    }

    @Test
    void adminPage_showsAllEntriesWithEmployeeNames() throws Exception {
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 10, ""), asWorker());
        postJson("/api/timesheet-entries", entryJson("2026-09-02", null, null, "SEPTEMBRIE", 20, ""), asOther());

        assertThat(body("/admin", asAdmin())).contains("Nume worker").contains("Nume other").contains("0h 30m");
    }

    @Test
    void timesheetPage_offersTheTaskCatalogGroupedByCategory() throws Exception {
        String page = body("/pontaj", asWorker());

        assertThat(page).contains("<optgroup label=\"Declaratii fiscale\">").contains("Decont TVA (D300)")
            .contains("<optgroup label=\"Administrativ / non-facturabil\">").contains("Alege sarcina");
    }

    @Test
    void savedTask_isShownOnBothPages() throws Exception {
        postJson("/api/timesheet-entries",
            "{\"date\":\"2026-09-01\",\"workingmonth\":\"SEPTEMBRIE\",\"totalMinutes\":30,\"actions\":[\"Fluturasi\"]}", asWorker())
            .andExpect(status().isOk());

        assertThat(body("/pontaj", asWorker())).contains("Fluturasi");
        assertThat(body("/admin", asAdmin())).contains("Fluturasi");
    }

    @Test
    void htmlInUserData_isEscapedOnEveryPage() throws Exception {
        String payload = "<script>alert('xss')</script>";
        Client evil = client(payload);
        employees.save(new timesheet_management_system.model.Employee(
            "<img src=x onerror=alert(1)>", "xssuser", "h", timesheet_management_system.model.Role.ANGAJAT));
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, evil.getId(), "SEPTEMBRIE", 10, payload), asWorker());

        for (String page : new String[] {body("/pontaj", asWorker()), body("/admin", asAdmin())}) {
            assertThat(page).doesNotContain(payload).doesNotContain("<img src=x").contains("&lt;script&gt;");
        }
    }

    @Test
    void quotesAndUnicodeInNotes_surviveTheRoundTrip() throws Exception {
        String note = "Închidere \"lună\" & audit – ăîșț 日本";
        postJson("/api/timesheet-entries", entryJson("2026-09-01", null, null, "SEPTEMBRIE", 10, note), asWorker())
            .andExpect(jsonPath("$.extranote").value(note));

        mvc.perform(get("/pontaj").with(asWorker()))
            .andExpect(content().string(containsString("&quot;lună&quot; &amp; audit")));
    }
}
