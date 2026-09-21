package timesheet_management_system.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import timesheet_management_system.exception.BadRequestException;

public final class PeriodResolver {

    public static final LocalDate MIN = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAX = LocalDate.of(2999, 12, 31);

    public record DateRange(LocalDate from, LocalDate to) {
    }

    private PeriodResolver() {
    }

    public static DateRange resolve(String period, String fromText, String toText, LocalDate today) {
        String key = period == null ? "luna" : period;
        switch (key) {
            case "luna-trecuta": {
                YearMonth previous = YearMonth.from(today).minusMonths(1);
                return new DateRange(previous.atDay(1), previous.atEndOfMonth());
            }
            case "an":
                return new DateRange(LocalDate.of(today.getYear(), 1, 1), LocalDate.of(today.getYear(), 12, 31));
            case "tot":
                return new DateRange(MIN, MAX);
            case "interval": {
                LocalDate from = parse(fromText, MIN);
                LocalDate to = parse(toText, MAX);
                if (from.isAfter(to)) {
                    throw new BadRequestException("Data de început nu poate fi după data de sfârșit.");
                }
                return new DateRange(from, to);
            }
            default: {
                YearMonth current = YearMonth.from(today);
                return new DateRange(current.atDay(1), current.atEndOfMonth());
            }
        }
    }

    private static LocalDate parse(String text, LocalDate whenBlank) {
        if (text == null || text.isBlank()) {
            return whenBlank;
        }
        try {
            LocalDate date = LocalDate.parse(text.trim());
            if (date.isBefore(MIN) || date.isAfter(MAX)) {
                throw new BadRequestException("Data introdusă este în afara intervalului acceptat.");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Data introdusă nu este validă.");
        }
    }
}
