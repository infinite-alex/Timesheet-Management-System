package timesheet_management_system.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelExporter {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final int HEADER_ROW = 3;
    private static final int MAX_COLUMN_CHARS = 60;

    private ExcelExporter() {
    }

    // Aceleasi coloane ca exportul CSV. Numele sunt scrise ca text, deci nu pot deveni formule in Excel.
    public static byte[] toXlsx(Report report, String periodLabel) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Raport");
            Styles styles = new Styles(workbook);
            String[] header = header(report.view());
            int[] widths = new int[header.length];

            Row title = sheet.createRow(0);
            text(title, 0, "Raport " + report.view().title().toLowerCase(), styles.title, null);
            text(sheet.createRow(1), 0, "Perioada: " + periodLabel, styles.muted, null);

            Row head = sheet.createRow(HEADER_ROW);
            for (int i = 0; i < header.length; i++) {
                text(head, i, header[i], styles.header, widths);
            }

            int rowIndex = HEADER_ROW + 1;
            for (ReportGroup group : report.groups()) {
                for (ReportLine line : group.lines()) {
                    Row row = sheet.createRow(rowIndex++);
                    text(row, 0, group.label(), null, widths);
                    text(row, 1, line.label(), null, widths);
                    number(row, 2, line.minutes(), styles.integer);
                    number(row, 3, line.minutes() / 60.0, styles.hours);
                    number(row, 4, line.entries(), styles.integer);
                }
            }
            int lastDataRow = rowIndex - 1;

            Row total = sheet.createRow(rowIndex);
            text(total, 0, "TOTAL", styles.totalText, widths);
            text(total, 1, "", styles.totalText, null);
            number(total, 2, report.totalMinutes(), styles.totalInteger);
            number(total, 3, report.totalMinutes() / 60.0, styles.totalHours);
            number(total, 4, report.totalEntries(), styles.totalInteger);

            sheet.createFreezePane(0, HEADER_ROW + 1);
            if (lastDataRow > HEADER_ROW) {
                sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, lastDataRow, 0, header.length - 1));
            }
            for (int i = 0; i < header.length; i++) {
                int chars = Math.min(MAX_COLUMN_CHARS, Math.max(widths[i], 10) + 3);
                sheet.setColumnWidth(i, chars * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String[] header(ReportView view) {
        String group = switch (view) {
            case CLIENTI -> "Client";
            case SARCINI -> "Sarcină";
            default -> "Angajat";
        };
        String detail = view == ReportView.ANGAJATI ? "Client" : "Angajat";
        return new String[] { group, detail, "Minute", "Ore", "Nr. pontaje" };
    }

    private static void text(Row row, int column, String value, CellStyle style, int[] widths) {
        String safe = value == null ? "" : value;
        Cell cell = row.createCell(column);
        cell.setCellValue(safe);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (widths != null) {
            widths[column] = Math.max(widths[column], safe.length());
        }
    }

    private static void number(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static final class Styles {
        final CellStyle title;
        final CellStyle muted;
        final CellStyle header;
        final CellStyle integer;
        final CellStyle hours;
        final CellStyle totalText;
        final CellStyle totalInteger;
        final CellStyle totalHours;

        Styles(XSSFWorkbook workbook) {
            short integerFormat = workbook.createDataFormat().getFormat("0");
            short hoursFormat = workbook.createDataFormat().getFormat("0.00");

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            Font mutedFont = workbook.createFont();
            mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            muted = workbook.createCellStyle();
            muted.setFont(mutedFont);

            Font bold = workbook.createFont();
            bold.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            integer = workbook.createCellStyle();
            integer.setDataFormat(integerFormat);
            hours = workbook.createCellStyle();
            hours.setDataFormat(hoursFormat);

            totalText = workbook.createCellStyle();
            totalText.setFont(bold);
            totalText.setBorderTop(BorderStyle.THIN);
            totalInteger = workbook.createCellStyle();
            totalInteger.cloneStyleFrom(totalText);
            totalInteger.setDataFormat(integerFormat);
            totalHours = workbook.createCellStyle();
            totalHours.cloneStyleFrom(totalText);
            totalHours.setDataFormat(hoursFormat);
        }
    }
}
