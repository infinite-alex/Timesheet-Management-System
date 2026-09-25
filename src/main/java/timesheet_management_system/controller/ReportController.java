package timesheet_management_system.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import timesheet_management_system.exception.BadRequestException;
import timesheet_management_system.model.ActionCatalog;
import timesheet_management_system.report.CsvExporter;
import timesheet_management_system.report.ExcelExporter;
import timesheet_management_system.report.PeriodResolver;
import timesheet_management_system.report.PeriodResolver.DateRange;
import timesheet_management_system.report.Report;
import timesheet_management_system.report.ReportFilter;
import timesheet_management_system.report.ReportService;
import timesheet_management_system.report.ReportView;
import timesheet_management_system.service.ClientService;
import timesheet_management_system.service.EmployeeService;

@Controller
public class ReportController {

    private record Query(ReportView view, String period, DateRange range, ReportFilter filter) {
    }

    private final ReportService reportService;
    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final ActionCatalog actionCatalog;

    public ReportController(ReportService reportService, ClientService clientService,
            EmployeeService employeeService, ActionCatalog actionCatalog) {
        this.reportService = reportService;
        this.clientService = clientService;
        this.employeeService = employeeService;
        this.actionCatalog = actionCatalog;
    }

    @GetMapping("/rapoarte")
    public String page(@RequestParam(required = false) String view,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(name = "clientId", required = false) List<String> clientIds,
            @RequestParam(name = "employeeId", required = false) List<String> employeeIds,
            @RequestParam(required = false) String task,
            Authentication authentication, HttpServletRequest request, Model model) {

        ReportView reportView = ReportView.fromSlug(view);
        String periodKey = period == null || period.isBlank() ? "luna" : period;
        Set<Long> clients = toIds(clientIds);
        Set<Long> employees = toIds(employeeIds);

        Report report;
        String error = null;
        try {
            Query query = parse(view, periodKey, from, to, clients, employees, task);
            report = reportService.build(query.view(), query.filter());
        } catch (BadRequestException e) {
            error = e.getMessage();
            report = Report.empty(reportView);
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("view", reportView);
        model.addAttribute("views", ReportView.values());
        model.addAttribute("period", periodKey);
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("selectedClients", clients);
        model.addAttribute("selectedEmployees", employees);
        model.addAttribute("task", task == null ? "" : task);
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("taskGroups", actionCatalog.getGroups());
        model.addAttribute("report", report);
        model.addAttribute("error", error);
        model.addAttribute("queryString", request.getQueryString() == null ? "" : request.getQueryString());
        return "rapoarte";
    }

    @GetMapping("/rapoarte/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String view,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(name = "clientId", required = false) List<String> clientIds,
            @RequestParam(name = "employeeId", required = false) List<String> employeeIds,
            @RequestParam(required = false) String task) {
        return export(view, period, from, to, clientIds, employeeIds, task, "csv",
            new MediaType("text", "csv", StandardCharsets.UTF_8),
            (report, query) -> CsvExporter.toCsv(report));
    }

    @GetMapping("/rapoarte/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String view,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(name = "clientId", required = false) List<String> clientIds,
            @RequestParam(name = "employeeId", required = false) List<String> employeeIds,
            @RequestParam(required = false) String task) {
        return export(view, period, from, to, clientIds, employeeIds, task, "xlsx",
            MediaType.parseMediaType(ExcelExporter.CONTENT_TYPE),
            (report, query) -> ExcelExporter.toXlsx(report, periodLabel(query)));
    }

    private ResponseEntity<byte[]> export(String view, String period, String from, String to,
            List<String> clientIds, List<String> employeeIds, String task, String extension,
            MediaType contentType, BiFunction<Report, Query, byte[]> writer) {

        String periodKey = period == null || period.isBlank() ? "luna" : period;
        Query query;
        try {
            query = parse(view, periodKey, from, to, toIds(clientIds), toIds(employeeIds), task);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }

        Report report = reportService.build(query.view(), query.filter());
        String range = "tot".equals(query.period()) ? "tot" : query.range().from() + "_" + query.range().to();
        String filename = "raport-" + query.view().slug() + "-" + range + "." + extension;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(contentType)
            .body(writer.apply(report, query));
    }

    private String periodLabel(Query query) {
        if ("tot".equals(query.period())) {
            return "tot istoricul";
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return query.range().from().format(format) + " – " + query.range().to().format(format);
    }

    private Query parse(String view, String period, String from, String to,
            Set<Long> clients, Set<Long> employees, String task) {
        ReportView reportView = ReportView.fromSlug(view);
        DateRange range = PeriodResolver.resolve(period, from, to, LocalDate.now());
        ReportFilter filter = new ReportFilter(range.from(), range.to(), clients, employees,
            task == null ? "" : task);
        return new Query(reportView, period, range, filter);
    }

    private Set<Long> toIds(List<String> raw) {
        Set<Long> ids = new LinkedHashSet<>();
        if (raw == null) {
            return ids;
        }
        for (String value : new ArrayList<>(raw)) {
            try {
                ids.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // valorile invalide sunt ignorate
            }
        }
        return ids;
    }
}
