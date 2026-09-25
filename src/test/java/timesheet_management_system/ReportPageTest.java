package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import timesheet_management_system.model.Client;
import timesheet_management_system.model.Employee;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.model.WorkingMonth;

class ReportPageTest extends IntegrationTestBase {

    Client alfa;
    Client beta;

    private void entry(LocalDate date, Employee employee, Client client, String task, int minutes) {
        entries.save(new TimesheetEntry(date, employee, client, WorkingMonth.SEPTEMBRIE, minutes,
            task == null ? List.of() : List.of(task), ""));
    }

    @BeforeEach
    void data() {
        alfa = clients.save(new Client("Alfa SRL"));
        beta = clients.save(new Client("Beta SRL"));
        entry(LocalDate.of(2026, 9, 1), worker, alfa, "Inchidere luna", 60);
        entry(LocalDate.of(2026, 9, 3), other, alfa, "Nota contabila", 120);
        entry(LocalDate.of(2026, 9, 4), worker, beta, null, 45);
    }

    private String page(String query) throws Exception {
        return mvc.perform(get("/rapoarte" + query).with(asAdmin())).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String csv(String query) throws Exception {
        MvcResult result = mvc.perform(get("/rapoarte/export.csv" + query).with(asAdmin())).andExpect(status().isOk()).andReturn();
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void reports_areForAdminsOnly() throws Exception {
        for (String url : List.of("/rapoarte", "/rapoarte?view=clienti", "/rapoarte/export.csv", "/rapoarte/export.xlsx")) {
            mvc.perform(get(url).with(asWorker())).andExpect(status().isForbidden());
            mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void adminSeesNavigationLinks_employeeDoesNot() throws Exception {
        assertThat(page("?period=tot")).contains("/rapoarte").contains("Panou");
        assertThat(mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString())
            .doesNotContain("Rapoarte");
    }

    @Test
    void employeeView_listsEachPersonWithTheirTotals() throws Exception {
        String html = page("?view=angajati&period=tot");

        assertThat(html).contains("Nume worker").contains("Nume other").contains("Alfa SRL").contains("Beta SRL");
        assertThat(html).contains("3h 45m");
    }

    @Test
    void clientView_showsWhoWorkedForEachClient() throws Exception {
        String html = page("?view=clienti&period=tot");

        assertThat(html).contains("Alfa SRL").contains("3h 0m").contains("Nume other");
        assertThat(html.indexOf("Alfa SRL")).isLessThan(html.indexOf("Beta SRL"));
    }

    @Test
    void taskView_showsTasksWithCategoryAndAnUntaggedBucket() throws Exception {
        String html = page("?view=sarcini&period=tot");

        assertThat(html).contains("Inchidere luna").contains("Nota contabila").contains("Fără sarcină")
            .contains("Verificare si inchidere luna");
    }

    @Test
    void unknownViewFallsBackToEmployees() throws Exception {
        assertThat(page("?view=ceva&period=tot")).contains("Pe angajați");
    }

    @Test
    void clientFilter_limitsWhatIsShown() throws Exception {
        String html = page("?view=angajati&period=tot&clientId=" + beta.getId());

        assertThat(html).contains("0h 45m").doesNotContain("3h 45m");
        assertThat(html).contains("value=\"" + beta.getId() + "\" checked");
    }

    @Test
    void employeeAndTaskFilters_work() throws Exception {
        assertThat(page("?view=clienti&period=tot&employeeId=" + other.getId())).contains("2h 0m").doesNotContain("0h 45m");
        String byTask = mvc.perform(get("/rapoarte").with(asAdmin()).param("view", "sarcini").param("period", "tot")
            .param("task", "Nota contabila")).andReturn().getResponse().getContentAsString();
        assertThat(byTask).contains("2h 0m").doesNotContain("3h 45m");
    }

    @Test
    void currentMonthIsTheDefault_andOlderDataIsExcluded() throws Exception {
        Client recent = clients.save(new Client("Doar Luna Curenta"));
        Client ancient = clients.save(new Client("Doar Anul Vechi"));
        entry(LocalDate.now(), worker, recent, "Inchidere luna", 33);
        entry(LocalDate.now().minusYears(2), worker, ancient, "Inchidere luna", 777);

        String html = page("");

        assertThat(html).contains("0h 33m").doesNotContain("12h 57m");
    }

    @Test
    void invalidInterval_showsAMessage_notAnErrorPage() throws Exception {
        assertThat(page("?period=interval&from=2026-09-30&to=2026-09-01")).contains("nu poate fi după data de sfârșit");
        assertThat(page("?period=interval&from=azi")).contains("nu este validă");
        assertThat(page("?period=interval&from=azi")).contains("Nu există date");
    }

    @Test
    void garbageParameters_doNotBreakThePage() throws Exception {
        mvc.perform(get("/rapoarte").with(asAdmin()).param("clientId", "abc", "", "1e9999", "-5")
            .param("employeeId", "x").param("task", "'; DROP TABLE x;--").param("period", "<script>"))
            .andExpect(status().isOk());
    }

    @Test
    void htmlInNames_isEscaped() throws Exception {
        Client evil = clients.save(new Client("<script>alert('x')</script>"));
        entry(LocalDate.of(2026, 9, 9), worker, evil, "Inchidere luna", 10);

        for (String view : List.of("angajati", "clienti", "sarcini")) {
            String html = page("?view=" + view + "&period=tot");
            assertThat(html).doesNotContain("<script>alert('x')</script>").contains("&lt;script&gt;");
        }
    }

    @Test
    void export_isAnAttachmentCsvWithBomAndTotals() throws Exception {
        mvc.perform(get("/rapoarte/export.csv").param("view", "clienti").param("period", "tot").with(asAdmin()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/csv")))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(header().string("Content-Disposition", containsString("raport-clienti-tot.csv")));

        String body = csv("?view=clienti&period=tot");
        assertThat(body).startsWith("﻿Client;Angajat;Minute;Ore;Nr. pontaje");
        assertThat(body).contains("Alfa SRL;Nume other;120;2,00;1").contains("Alfa SRL;Nume worker;60;1,00;1");
        assertThat(body).contains("Beta SRL;Nume worker;45;0,75;1");
        assertThat(body).contains("TOTAL;;225;3,75;3");
    }

    @Test
    void export_respectsFilters_andNamesTheFileByDates() throws Exception {
        String body = csv("?view=angajati&period=tot&employeeId=" + other.getId());
        assertThat(body).contains("Nume other").doesNotContain("Nume worker").contains("TOTAL;;120;");

        mvc.perform(get("/rapoarte/export.csv").param("period", "interval").param("from", "2026-09-01").param("to", "2026-09-30").with(asAdmin()))
            .andExpect(header().string("Content-Disposition", containsString("raport-angajati-2026-09-01_2026-09-30.csv")));
    }

    @Test
    void export_neutralisesSpreadsheetFormulas_andEscapesSeparators() throws Exception {
        Client formula = clients.save(new Client("=HYPERLINK(\"http://evil\";\"click\")"));
        Client semi = clients.save(new Client("Firma; cu punct si virgula"));
        entry(LocalDate.of(2026, 9, 9), worker, formula, "Inchidere luna", 10);
        entry(LocalDate.of(2026, 9, 9), worker, semi, "Inchidere luna", 10);

        String body = csv("?view=clienti&period=tot");

        assertThat(body).doesNotContain("\n=HYPERLINK").doesNotContain(";=HYPERLINK");
        assertThat(body).contains("'=HYPERLINK").contains("\"Firma; cu punct si virgula\"");
    }

    @Test
    void export_withAnInvalidInterval_isABadRequest() throws Exception {
        mvc.perform(get("/rapoarte/export.csv").param("period", "interval").param("from", "2026-09-30").param("to", "2026-09-01").with(asAdmin()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("după data de sfârșit")))
            .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void export_ofAnEmptyPeriod_stillReturnsTheHeaderAndZeroTotal() throws Exception {
        String body = csv("?view=angajati&period=interval&from=2030-01-01&to=2030-01-31");

        assertThat(body).contains("Angajat;Client;Minute;Ore;Nr. pontaje").contains("TOTAL;;0;0,00;0");
    }

    private Sheet xlsx(String query) throws Exception {
        MvcResult result = mvc.perform(get("/rapoarte/export.xlsx" + query).with(asAdmin())).andExpect(status().isOk()).andReturn();
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        return workbook.getSheetAt(0);
    }

    private List<String> rowValues(Row row) {
        List<String> values = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        row.forEach(cell -> values.add(formatter.formatCellValue(cell)));
        return values;
    }

    @Test
    void excelExport_isAnAttachmentNamedLikeTheCsv() throws Exception {
        mvc.perform(get("/rapoarte/export.xlsx").param("view", "clienti").param("period", "tot").with(asAdmin()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("spreadsheetml.sheet")))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(header().string("Content-Disposition", containsString("raport-clienti-tot.xlsx")));

        mvc.perform(get("/rapoarte/export.xlsx").param("period", "interval").param("from", "2026-09-01").param("to", "2026-09-30").with(asAdmin()))
            .andExpect(header().string("Content-Disposition", containsString("raport-angajati-2026-09-01_2026-09-30.xlsx")));
    }

    @Test
    void excelExport_hasTitleHeaderNumericRowsAndTotal() throws Exception {
        Sheet sheet = xlsx("?view=clienti&period=interval&from=2026-09-01&to=2026-09-30");

        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Raport pe clienți");
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Perioada: 01.09.2026 – 30.09.2026");
        assertThat(rowValues(sheet.getRow(3))).containsExactly("Client", "Angajat", "Minute", "Ore", "Nr. pontaje");
        assertThat(rowValues(sheet.getRow(4))).containsExactly("Alfa SRL", "Nume other", "120", "2.00", "1");
        assertThat(rowValues(sheet.getRow(5))).containsExactly("Alfa SRL", "Nume worker", "60", "1.00", "1");
        assertThat(rowValues(sheet.getRow(6))).containsExactly("Beta SRL", "Nume worker", "45", "0.75", "1");
        assertThat(rowValues(sheet.getRow(7))).containsExactly("TOTAL", "", "225", "3.75", "3");

        assertThat(sheet.getRow(4).getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(sheet.getRow(4).getCell(3).getNumericCellValue()).isEqualTo(2.0);
    }

    @Test
    void excelExport_respectsFilters() throws Exception {
        Sheet sheet = xlsx("?view=angajati&period=tot&employeeId=" + other.getId());

        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Perioada: tot istoricul");
        assertThat(rowValues(sheet.getRow(4))).containsExactly("Nume other", "Alfa SRL", "120", "2.00", "1");
        assertThat(rowValues(sheet.getRow(5))).containsExactly("TOTAL", "", "120", "2.00", "1");
        assertThat(sheet.getRow(6)).isNull();
    }

    @Test
    void excelExport_storesFormulaLikeNamesAsPlainText() throws Exception {
        Client formula = clients.save(new Client("=HYPERLINK(\"http://evil\";\"click\")"));
        entry(LocalDate.of(2026, 9, 9), worker, formula, "Inchidere luna", 10);

        Sheet sheet = xlsx("?view=clienti&period=tot");

        boolean found = false;
        for (Row row : sheet) {
            for (Cell cell : row) {
                assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);
                if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().startsWith("=HYPERLINK")) {
                    found = true;
                }
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void excelExport_ofAnEmptyPeriod_hasHeaderAndZeroTotal() throws Exception {
        Sheet sheet = xlsx("?view=sarcini&period=interval&from=2030-01-01&to=2030-01-31");

        assertThat(rowValues(sheet.getRow(3))).containsExactly("Sarcină", "Angajat", "Minute", "Ore", "Nr. pontaje");
        assertThat(rowValues(sheet.getRow(4))).containsExactly("TOTAL", "", "0", "0.00", "0");
    }

    @Test
    void excelExport_withAnInvalidInterval_isABadRequest() throws Exception {
        mvc.perform(get("/rapoarte/export.xlsx").param("period", "interval").param("from", "2026-09-30").param("to", "2026-09-01").with(asAdmin()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("după data de sfârșit")));
    }

    @Test
    void reportPage_offersBothExcelAndCsv() throws Exception {
        assertThat(page("?period=tot")).contains("/rapoarte/export.xlsx").contains("Descarcă Excel")
            .contains("/rapoarte/export.csv").contains("Descarcă CSV");
    }
}
