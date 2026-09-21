package timesheet_management_system.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import timesheet_management_system.model.ActionCatalog;
import timesheet_management_system.model.TimesheetEntry;
import timesheet_management_system.repository.TimesheetEntryRepository;

@Service
public class ReportService {

    private record Key(String id, String label, String note) {
    }

    private final TimesheetEntryRepository entryRepository;
    private final ActionCatalog actionCatalog;

    public ReportService(TimesheetEntryRepository entryRepository, ActionCatalog actionCatalog) {
        this.entryRepository = entryRepository;
        this.actionCatalog = actionCatalog;
    }

    @Transactional(readOnly = true)
    public Report build(ReportView view, ReportFilter filter) {
        List<TimesheetEntry> entries = entryRepository.findInRange(filter.from(), filter.to()).stream()
            .filter(entry -> matches(entry, filter))
            .toList();

        Function<TimesheetEntry, Key> primary;
        Function<TimesheetEntry, Key> secondary;
        switch (view) {
            case CLIENTI -> {
                primary = this::clientKey;
                secondary = this::employeeKey;
            }
            case SARCINI -> {
                primary = this::taskKey;
                secondary = this::employeeKey;
            }
            default -> {
                primary = this::employeeKey;
                secondary = this::clientKey;
            }
        }

        Map<Key, Map<Key, int[]>> grouped = new LinkedHashMap<>();
        int totalMinutes = 0;
        for (TimesheetEntry entry : entries) {
            int[] cell = grouped
                .computeIfAbsent(primary.apply(entry), key -> new LinkedHashMap<>())
                .computeIfAbsent(secondary.apply(entry), key -> new int[2]);
            cell[0] += entry.getTotalMinutes();
            cell[1] += 1;
            totalMinutes += entry.getTotalMinutes();
        }

        List<ReportGroup> groups = new ArrayList<>();
        grouped.forEach((groupKey, lineMap) -> {
            List<ReportLine> lines = new ArrayList<>();
            int minutes = 0;
            int count = 0;
            for (Map.Entry<Key, int[]> line : lineMap.entrySet()) {
                lines.add(new ReportLine(line.getKey().label(), line.getValue()[0], line.getValue()[1]));
                minutes += line.getValue()[0];
                count += line.getValue()[1];
            }
            lines.sort(Comparator.comparingInt(ReportLine::minutes).reversed()
                .thenComparing(ReportLine::label, String.CASE_INSENSITIVE_ORDER));
            groups.add(new ReportGroup(groupKey.label(), groupKey.note(), minutes, count, lines));
        });
        groups.sort(Comparator.comparingInt(ReportGroup::minutes).reversed()
            .thenComparing(ReportGroup::label, String.CASE_INSENSITIVE_ORDER));

        return new Report(view, groups, totalMinutes, entries.size());
    }

    private boolean matches(TimesheetEntry entry, ReportFilter filter) {
        Set<Long> clientIds = filter.clientIds();
        if (clientIds != null && !clientIds.isEmpty()) {
            long clientId = entry.getClient() == null ? ReportFilter.NO_CLIENT : entry.getClient().getId();
            if (!clientIds.contains(clientId)) {
                return false;
            }
        }
        Set<Long> employeeIds = filter.employeeIds();
        if (employeeIds != null && !employeeIds.isEmpty() && !employeeIds.contains(entry.getEmployee().getId())) {
            return false;
        }
        String wanted = filter.task();
        if (wanted != null && !wanted.isBlank()) {
            String task = firstTask(entry);
            if (ReportFilter.NO_TASK.equals(wanted)) {
                return task == null;
            }
            return wanted.equals(task);
        }
        return true;
    }

    private String firstTask(TimesheetEntry entry) {
        List<String> actions = entry.getActions();
        return actions.isEmpty() ? null : actions.get(0);
    }

    private Key employeeKey(TimesheetEntry entry) {
        return new Key("e" + entry.getEmployee().getId(), entry.getEmployee().getName(), null);
    }

    private Key clientKey(TimesheetEntry entry) {
        if (entry.getClient() == null) {
            return new Key("c0", "Fără client", null);
        }
        return new Key("c" + entry.getClient().getId(), entry.getClient().getName(), null);
    }

    private Key taskKey(TimesheetEntry entry) {
        String task = firstTask(entry);
        if (task == null) {
            return new Key("t-", "Fără sarcină", null);
        }
        return new Key("t" + task, task, actionCatalog.categoryOf(task).orElse(null));
    }
}
