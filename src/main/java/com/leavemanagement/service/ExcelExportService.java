package com.leavemanagement.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes one-sheet Excel (.xlsx) files: bold frozen header row with an AutoFilter, auto-sized columns,
 * date / date-time / day-count formats, and an optional bold totals row below the data.
 * Uses the streaming SXSSF workbook, so large exports keep only a window of rows in memory.
 */
@Service
public class ExcelExportService {
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    /**
     * @param rows   cell values per row: String, Number (BigDecimal, Integer, Double...), LocalDate,
     *               LocalDateTime, Boolean or enum; null leaves the cell empty
     * @param totals optional last row (bold), e.g. "Total" followed by sums; null for none
     */
    public byte[] write(String sheetName, List<String> headers, List<List<?>> rows, List<?> totals) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Styles styles = new Styles(workbook);
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            sheet.trackAllColumnsForAutoSizing();

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(styles.header);
            }
            int r = 1;
            for (List<?> values : rows) {
                writeRow(sheet.createRow(r++), values, styles, false);
            }
            int lastDataRow = r - 1;
            if (totals != null) {
                writeRow(sheet.createRow(r), totals, styles, true);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(lastDataRow, 0), 0, headers.size() - 1));
            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
                // Room for the filter arrow; very long text (reasons, comments) is capped
                sheet.setColumnWidth(c, Math.min(sheet.getColumnWidth(c) + 3 * 256, MAX_COLUMN_WIDTH));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose(); // delete the streaming temp files
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the Excel file", e);
        }
    }

    private static void writeRow(Row row, List<?> values, Styles styles, boolean bold) {
        for (int c = 0; c < values.size(); c++) {
            Object value = values.get(c);
            if (value == null) {
                continue;
            }
            Cell cell = row.createCell(c);
            if (value instanceof LocalDateTime dt) {
                cell.setCellValue(dt);
                cell.setCellStyle(styles.dateTime);
            } else if (value instanceof LocalDate d) {
                cell.setCellValue(d);
                cell.setCellStyle(styles.date);
            } else if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue());
                cell.setCellStyle(bold ? styles.boldWhole : styles.whole);
            } else if (value instanceof Number n) {
                // Day counts: whole or half days, e.g. 12 or 11.5
                cell.setCellValue(n instanceof BigDecimal b ? b.doubleValue() : n.doubleValue());
                cell.setCellStyle(bold ? styles.boldDays : styles.days);
            } else if (value instanceof Boolean b) {
                cell.setCellValue(b ? "Yes" : "No");
            } else {
                cell.setCellValue(value.toString());
                if (bold) {
                    cell.setCellStyle(styles.boldText);
                }
            }
        }
    }

    private static class Styles {
        final CellStyle header, date, dateTime, whole, days, boldWhole, boldDays, boldText;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            date = style(workbook, format, "dd-mmm-yyyy", null);
            dateTime = style(workbook, format, "dd-mmm-yyyy hh:mm", null);
            whole = style(workbook, format, "0", null);
            days = style(workbook, format, "0.0#", null);
            boldWhole = style(workbook, format, "0", boldFont);
            boldDays = style(workbook, format, "0.0#", boldFont);
            boldText = workbook.createCellStyle();
            boldText.setFont(boldFont);
        }

        private static CellStyle style(Workbook workbook, DataFormat format, String pattern, Font font) {
            CellStyle style = workbook.createCellStyle();
            style.setDataFormat(format.getFormat(pattern));
            if (font != null) {
                style.setFont(font);
            }
            return style;
        }
    }
}
