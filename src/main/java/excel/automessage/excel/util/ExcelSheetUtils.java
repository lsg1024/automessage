package excel.automessage.excel.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class ExcelSheetUtils {

    public static Workbook getSheets(MultipartFile file) throws IOException {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        Workbook workbook = null;

        if (extension == null) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }

        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (NotOfficeXmlFileException e) {
            workbook = convertHtmlToWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (workbook == null) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }
        return workbook;
    }

    // Html 데이터 테이블 읽어오기
    private static Workbook convertHtmlToWorkbook(MultipartFile htmlFile) throws IOException {
        Document htmlDoc = Jsoup.parse(htmlFile.getInputStream(), "UTF-8", "");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        Element table = htmlDoc.select("table").first();
        if (table != null) {
            Elements rows = table.select("tr");

            int rowIndex = 0;
            for (Element row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                Elements cells = row.select("td, th");
                int cellIndex = 0;
                for (Element cell : cells) {
                    Cell excelCell = excelRow.createCell(cellIndex++);
                    excelCell.setCellValue(cell.text());
                }
            }
        }

        return workbook;
    }

    // download excel worksheet 생성
    public static byte[] createOrderWorkSheet(Sheet worksheet, LocalDate today) throws IOException {

        List<String> targetHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "비고", "제조사");

        Row headerRow = worksheet.getRow(0);

        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = cell.getStringCellValue().trim();
            if (targetHeaders.contains(value)) {
                headerIndex.put(value, cell.getColumnIndex());
            }
        }

        Map<String, List<List<String>>> factoryRows = new LinkedHashMap<>();

        for (int r = 1; r <= worksheet.getLastRowNum(); r++) {
            Row row = worksheet.getRow(r);
            if (row == null) continue;
            String factory = getCellString(row.getCell(headerIndex.get("제조사"))).toUpperCase();
            List<String> rowData = new ArrayList<>();
            for (String col : Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "비고")) {
                rowData.add(getCellString(row.getCell(headerIndex.get(col))));
            }
            factoryRows.computeIfAbsent(factory, k -> new ArrayList<>()).add(rowData);
        }

        Workbook newWorkbook = new XSSFWorkbook();
        List<String> outputHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "비고");

        for (String factory : factoryRows.keySet()) {
            Sheet factorySheet = newWorkbook.createSheet(factory);

            factorySheet.createRow(0);

            // === 제조사 이름
            Row firstRow = factorySheet.createRow(0);
            Cell factoryCell = firstRow.createCell(1);
            factoryCell.setCellValue(factory);

            // 제조사명 스타일 (16px)
            CellStyle factoryStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 16, factoryStyle, factoryCell);

            // === 매장명(24px, 칸 -> 가변변경 가능)
            Cell storeCell = firstRow.createCell(4);
            storeCell.setCellValue("칸");

            CellStyle storeStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 24, storeStyle, storeCell);

            // === 오늘 날짜(24px)
            Cell dateCell = firstRow.createCell(5);
            dateCell.setCellValue(today.toString());
            CellStyle dateStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 20, dateStyle, dateCell);

            Row header = factorySheet.createRow(1);

            CellStyle headerStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, headerStyle);

            setWorkSheetHeader(outputHeaders, factorySheet, header, headerStyle);

            CellStyle dataStyle = newWorkbook.createCellStyle();
            dataSheetStyle(newWorkbook, dataStyle);

            int rowIdx = 2;
            setWorkSheetBody(factoryRows, factory, factorySheet, dataStyle, rowIdx);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newWorkbook.write(outputStream);
        newWorkbook.close();

        return outputStream.toByteArray();
    }

    private static void setWorkSheetHeader(List<String> outputHeaders, Sheet factorySheet, Row header, CellStyle headerStyle) {
        int[] widths = {5 * 256, 15 * 256, 5 * 256, 5 * 256, 20 * 256, 30 * 256};
        for (int i = 0; i < outputHeaders.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(outputHeaders.get(i));
            cell.setCellStyle(headerStyle);
            factorySheet.setColumnWidth(i, widths[i]); // 너비 px
        }
        header.setHeightInPoints(24); // 헤더 높이 24px
    }

    private static void setWorkSheetBody(Map<String, List<List<String>>> factoryRows, String factory, Sheet factorySheet, CellStyle dataStyle, int rowIdx) {
        for (List<String> dataRow : factoryRows.get(factory)) {
            Row excelRow = factorySheet.createRow(rowIdx);
            excelRow.setHeightInPoints(50); // 데이터 행 높이 60px
            for (int i = 0; i < dataRow.size(); i++) {
                Cell cell = excelRow.createCell(i);
                cell.setCellValue(dataRow.get(i));
                cell.setCellStyle(dataStyle);
            }
            rowIdx++;
        }
    }

    private static void headerSheetStyle(Workbook newWorkbook, int x, CellStyle cellStyle, Cell cell) {
        Font font = newWorkbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) x);
        cellStyle.setWrapText(true);
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(cellStyle);
    }

    private static void headerSheetStyle(Workbook newWorkbook, CellStyle headerStyle) {
        Font headerFont = newWorkbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        headerStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());

        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    private static void dataSheetStyle(Workbook newWorkbook, CellStyle dataStyle) {
        Font dataFont = newWorkbook.createFont();
        dataFont.setFontHeightInPoints((short) 12); // 12pt
        dataStyle.setFont(dataFont);
        dataStyle.setWrapText(true);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        dataStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

}
