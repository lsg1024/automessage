package excel.automessage.excel.service;

import excel.automessage.excel.util.ExcelSheetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class DownloadService {

    public byte[] downloadXls(MultipartFile file) throws IOException {

        log.info("DownloadService downloadXls {}", file.getOriginalFilename());
        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        List<String> targetHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "비고", "제조사");
        LocalDate today = LocalDate.now();

        checkDateValidate(worksheet, today);

        Row headerRow = worksheet.getRow(0);

        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = cell.getStringCellValue().trim();
            if (targetHeaders.contains(value)) {
                headerIndex.put(value, cell.getColumnIndex());
            }
        }

        log.info("DownloadService headerIndex {}", headerIndex);

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

        log.info("DownloadService factoryRows {}", factoryRows);

        Workbook newWorkbook = new XSSFWorkbook();
        List<String> outputHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "비고");

        for (String factory : factoryRows.keySet()) {
            Sheet factorySheet = newWorkbook.createSheet(factory);

            // === [0] 행(1번째): 빈 행 (headerIndex 한 칸 내리기 목적)
            factorySheet.createRow(0);

            log.info("0번 완료");

            // === [1] 행(2번째): 제조사 이름
            Row firstRow = factorySheet.createRow(0);
            Cell factoryCell = firstRow.createCell(1);
            factoryCell.setCellValue(factory);

            // 제조사명 스타일 (왼쪽 정렬, 굵게, 20px 폰트)
            CellStyle factoryStyle = newWorkbook.createCellStyle();
            Font factoryFont = newWorkbook.createFont();
            factoryFont.setBold(true);
            factoryFont.setFontHeightInPoints((short) 20); // 약 20~24px
            factoryStyle.setFont(factoryFont);
            factoryStyle.setAlignment(HorizontalAlignment.CENTER);
            factoryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            factoryCell.setCellStyle(factoryStyle);

            // === 매장명(24px, bold, 칸)
            Cell storeCell = firstRow.createCell(4);
            storeCell.setCellValue("칸"); // 매장명 하드코딩, 필요하면 동적으로 변경

            CellStyle storeStyle = newWorkbook.createCellStyle();
            Font storeFont = newWorkbook.createFont();
            storeFont.setBold(true);
            storeFont.setFontHeightInPoints((short) 24); // 24px
            storeStyle.setFont(storeFont);
            storeStyle.setAlignment(HorizontalAlignment.CENTER);
            storeStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            storeCell.setCellStyle(storeStyle);

            // === 오늘 날짜(24px, bold)
            Cell dateCell = firstRow.createCell(5);
            dateCell.setCellValue(today.toString());
            CellStyle dateStyle = newWorkbook.createCellStyle();
            Font dateFont = newWorkbook.createFont();
            dateFont.setBold(true);
            dateFont.setFontHeightInPoints((short) 24);
            dateStyle.setFont(dateFont);
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            dateStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dateCell.setCellStyle(dateStyle);

            log.info("5번 완료");
            // === [6] 행(7번째): 헤더
            Row header = factorySheet.createRow(1);

            // 헤더 스타일 (가운데 정렬, 12pt, bold)
            CellStyle headerStyle = newWorkbook.createCellStyle();
            Font headerFont = newWorkbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
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

            int[] widths = {5 * 256, 15 * 256, 5 * 256, 5 * 256, 24 * 256, 28 * 256};
            for (int i = 0; i < outputHeaders.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(outputHeaders.get(i));
                cell.setCellStyle(headerStyle);
                factorySheet.setColumnWidth(i, widths[i]); // 너비 px
            }
            header.setHeightInPoints(24); // 헤더 높이 24px

            log.info("6번 완료");
            // === [7] 행부터: 데이터
            CellStyle dataStyle = newWorkbook.createCellStyle();
            Font dataFont = newWorkbook.createFont();
            dataFont.setFontHeightInPoints((short) 12); // 12pt
            dataStyle.setFont(dataFont);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            int rowIdx = 2;
            for (List<String> dataRow : factoryRows.get(factory)) {
                Row excelRow = factorySheet.createRow(rowIdx);
                excelRow.setHeightInPoints(60); // 데이터 행 높이 50px
                for (int i = 0; i < dataRow.size(); i++) {
                    Cell cell = excelRow.createCell(i);
                    cell.setCellValue(dataRow.get(i));
                    cell.setCellStyle(dataStyle);
                }
                rowIdx++;
            }

            log.info("7번 완료");
        }

        log.info("DownloadService outputHeaders");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newWorkbook.write(outputStream);
        newWorkbook.close();

        return outputStream.toByteArray();
    }

    private void checkDateValidate(Sheet worksheet, LocalDate today) {

        Row row = worksheet.getRow(1);
        Cell cell = row.getCell(7);
        if (cell != null && cell.getCellType() == CellType.STRING) {
            String sellType = cell.getStringCellValue();
            log.info("checkDateValidate {} {}", sellType, today);
            if (!sellType.equals(today.toString())) {
                throw new IllegalArgumentException("오늘 판매 데이터가 아닙니다.\n수동으로 입력해주세요.");
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }
}