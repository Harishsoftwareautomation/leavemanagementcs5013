package com.leavemanagement.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generic .xlsx writer: header, formats, totals row, filter and frozen pane
 */
class ExcelExportServiceTest {
    enum Colour { DARK_BLUE }

    private final ExcelExportService excel = new ExcelExportService();

    private XSSFWorkbook write(List<List<?>> rows, List<?> totals) throws Exception {
        byte[] bytes = excel.write("Report", List.of("Name", "Days", "Count", "Date", "When", "Flag", "Kind"), rows, totals);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 2)).isEqualTo("PK"); // .xlsx is a zip file
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private List<List<?>> sampleRows() {
        return List.of(
                Arrays.asList("Asha", new BigDecimal("11.5"), 3, LocalDate.of(2026, 3, 2),
                        LocalDateTime.of(2026, 3, 2, 14, 30), true, Colour.DARK_BLUE),
                Arrays.asList("Ravi", 2.0d, 7L, null, null, false, null));
    }

    @Test
    void headerIsBoldFrozenAndFiltered() throws Exception {
        try (XSSFWorkbook wb = write(sampleRows(), null)) {
            XSSFSheet sheet = wb.getSheet("Report");
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Kind");
            assertThat(wb.getFontAt(header.getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:G3");
        }
    }

    @Test
    void valuesKeepTheirTypesAndFormats() throws Exception {
        try (XSSFWorkbook wb = write(sampleRows(), null)) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(11.5);
            assertThat(row.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("0.0#");
            assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(3);
            assertThat(row.getCell(2).getCellStyle().getDataFormatString()).isEqualTo("0");
            assertThat(DateUtil.isCellDateFormatted(row.getCell(3))).isTrue();
            assertThat(row.getCell(3).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(row.getCell(4).getCellStyle().getDataFormatString()).isEqualTo("dd-mmm-yyyy hh:mm");
            assertThat(row.getCell(5).getStringCellValue()).isEqualTo("Yes");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("DARK_BLUE");

            Row second = wb.getSheetAt(0).getRow(2);
            assertThat(second.getCell(3)).as("null leaves the cell empty").isNull();
            assertThat(second.getCell(5).getStringCellValue()).isEqualTo("No");
        }
    }

    @Test
    void totalsRowIsBoldAndOutsideTheFilter() throws Exception {
        try (XSSFWorkbook wb = write(sampleRows(), Arrays.asList("Total", new BigDecimal("13.5"), 10))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            Row totals = sheet.getRow(3);
            assertThat(totals.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(totals.getCell(1).getNumericCellValue()).isEqualTo(13.5);
            assertThat(wb.getFontAt(totals.getCell(1).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(CellRangeAddress.valueOf(sheet.getCTWorksheet().getAutoFilter().getRef()).getLastRow()).isEqualTo(2);
        }
    }

    @Test
    void emptyReportStillHasItsHeader() throws Exception {
        try (XSSFWorkbook wb = write(List.of(), null)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
        }
    }

    @Test
    void columnsAreSizedButLongTextIsCapped() throws Exception {
        List<List<?>> rows = List.of(Arrays.asList("x".repeat(500), 1, 1, null, null, null, null));
        try (XSSFWorkbook wb = write(rows, null)) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getColumnWidth(0)).isEqualTo(60 * 256);
            assertThat(sheet.getColumnWidth(1)).isGreaterThan(256);
        }
    }
}
