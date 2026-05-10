package excel.automessage.excel.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return createOrderWorkSheet(worksheet, today, Collections.emptySet());
    }

    // 제외할 접수번호 셋을 받아 다운로드 워크시트 생성 (단일 워크북, 시트별로 공장 분리)
    public static byte[] createOrderWorkSheet(Sheet worksheet, LocalDate today, Set<String> excludedReceiptNumbers) throws IOException {

        Map<String, List<List<String>>> factoryRows = extractFactoryRows(worksheet, excludedReceiptNumbers);

        Workbook newWorkbook = new XSSFWorkbook();
        for (String factory : factoryRows.keySet()) {
            buildFactorySheet(newWorkbook, factory, factoryRows.get(factory), today);
        }
        return writeWorkbookBytes(newWorkbook);
    }

    /**
     * 입력 시트에서 공장(제조사) 이름 목록만 추출한다.
     */
    public static List<String> extractFactoryNames(Sheet worksheet, Set<String> excludedReceiptNumbers) {
        Map<String, List<List<String>>> factoryRows = extractFactoryRows(worksheet, excludedReceiptNumbers);
        return new ArrayList<>(factoryRows.keySet());
    }

    /**
     * 공장(제조사)별로 별개의 워크북을 생성한다.
     * 반환 맵의 key 는 공장명, value 는 해당 공장 한 시트만 들어있는 .xlsx 바이트.
     */
    public static Map<String, byte[]> createOrderWorkbooksByFactory(Sheet worksheet,
                                                                    LocalDate today,
                                                                    Set<String> excludedReceiptNumbers) throws IOException {

        Map<String, List<List<String>>> factoryRows = extractFactoryRows(worksheet, excludedReceiptNumbers);

        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String factory : factoryRows.keySet()) {
            Workbook wb = new XSSFWorkbook();
            buildFactorySheet(wb, factory, factoryRows.get(factory), today);
            result.put(factory, writeWorkbookBytes(wb));
        }
        return result;
    }

    /**
     * 입력 시트에서 헤더 인덱스를 찾고, (제외 접수번호) 필터 + 함량 정렬을 적용한
     * 공장 -> 행 데이터 맵을 만들어 반환한다.
     */
    private static Map<String, List<List<String>>> extractFactoryRows(Sheet worksheet,
                                                                      Set<String> excludedReceiptNumbers) {
        List<String> targetHeaders = Arrays.asList("No", "접수번호", "제조번호", "재질", "색상", "사이즈", "중심스톤", "비고", "제조사");

        Row headerRow = worksheet.getRow(0);
        Map<String, Integer> headerIndex = new HashMap<>();
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                String value = getCellString(cell).trim();
                if (targetHeaders.contains(value)) {
                    headerIndex.put(value, cell.getColumnIndex());
                }
            }
        }

        Map<String, List<List<String>>> factoryRows = new LinkedHashMap<>();
        Integer receiptNumberIdx = headerIndex.get("접수번호");

        for (int r = 1; r <= worksheet.getLastRowNum(); r++) {
            Row row = worksheet.getRow(r);
            if (row == null) continue;

            // 이미 처리된 접수번호 제외
            if (receiptNumberIdx != null && excludedReceiptNumbers != null && !excludedReceiptNumbers.isEmpty()) {
                String receiptNumber = getCellString(row.getCell(receiptNumberIdx)).trim();
                if (!receiptNumber.isEmpty() && excludedReceiptNumbers.contains(receiptNumber)) {
                    continue;
                }
            }

            String factory = getCellString(row.getCell(headerIndex.get("제조사"))).toUpperCase();
            List<String> rowData = new ArrayList<>();
            for (String col : Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "중심스톤", "비고")) {
                rowData.add(getCellString(row.getCell(headerIndex.get(col))));
            }
            factoryRows.computeIfAbsent(factory, k -> new ArrayList<>()).add(rowData);
        }

        // 제조사 별로 함량(재질) 기준 정렬 (예: 18K -> 14K -> 그 외)
        for (List<List<String>> rows : factoryRows.values()) {
            rows.sort(Comparator
                    .comparingInt((List<String> r) -> purityOrder(r.get(2)))
                    .thenComparing(r -> Objects.requireNonNullElse(r.get(2), "")));
        }
        return factoryRows;
    }

    /**
     * 주어진 워크북에 단일 공장 시트를 만들어 채운다.
     */
    private static void buildFactorySheet(Workbook newWorkbook, String factory, List<List<String>> rows, LocalDate today) {
        List<String> outputHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "사이즈", "중심스톤", "비고");

        Sheet factorySheet = newWorkbook.createSheet(factory);

        // === 0행: 공장명(B+C 병합) / 매장명(E) / 날짜(F+G 병합)
        Row firstRow = factorySheet.createRow(0);

        Cell factoryCell = firstRow.createCell(1);
        factoryCell.setCellValue(factory);
        firstRow.createCell(2);
        factorySheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));

        CellStyle factoryStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, 15, factoryStyle, factoryCell);

        Cell storeCell = firstRow.createCell(4);
        storeCell.setCellValue("칸");
        CellStyle storeStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, 23, storeStyle, storeCell);

        Cell dateCell = firstRow.createCell(5);
        dateCell.setCellValue(today.toString());
        firstRow.createCell(6);
        factorySheet.addMergedRegion(new CellRangeAddress(0, 0, 5, 6));
        CellStyle dateStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, 19, dateStyle, dateCell);

        // === 1행: 컬럼 헤더
        Row header = factorySheet.createRow(1);
        CellStyle headerStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, headerStyle);
        setWorkSheetHeader(outputHeaders, factorySheet, header, headerStyle);

        // === 2행~ : 데이터
        CellStyle dataStyle = newWorkbook.createCellStyle();
        dataSheetStyle(newWorkbook, dataStyle);

        int rowIdx = 2;
        int index = 1;
        for (List<String> dataRow : rows) {
            Row excelRow = factorySheet.createRow(rowIdx);
            excelRow.setHeightInPoints(50);
            for (int i = 0; i < dataRow.size(); i++) {
                Cell cell = excelRow.createCell(i);
                if (i == 0) {
                    cell.setCellValue(index++);
                } else {
                    cell.setCellValue(dataRow.get(i));
                }
                cell.setCellStyle(dataStyle);
            }
            rowIdx++;
        }

        // === 인쇄/팩스 한 페이지 폭에 맞추기 (세로 방향, A4)
        applyPrintSetup(newWorkbook, factorySheet);
    }

    private static byte[] writeWorkbookBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 팩스/PDF 변환 시 컬럼 폭이 살짝 넘쳐 빈 페이지가 추가로 발생하는 문제를 막기 위해
     * 가로 1페이지에 강제로 맞춘다.
     */
    private static void applyPrintSetup(Workbook workbook, Sheet sheet) {
        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);

        PrintSetup ps = sheet.getPrintSetup();
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setLandscape(false);       // 세로 방향
        ps.setFitWidth((short) 1);    // 가로 1페이지에 강제 맞춤
        ps.setFitHeight((short) 0);   // 세로는 자동 분할

        // 좌우 여백을 줄여 가용 폭 확보 (인치 단위)
        sheet.setMargin(Sheet.LeftMargin, 0.3);
        sheet.setMargin(Sheet.RightMargin, 0.3);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);

        // 인쇄 영역을 데이터 범위(A~G)로 명시 → 우측 빈 컬럼 때문에 페이지가 넘어가는 것 방지
        int lastRow = Math.max(sheet.getLastRowNum(), 1);
        workbook.setPrintArea(workbook.getSheetIndex(sheet), 0, 6, 0, lastRow);
    }

    private static void setWorkSheetHeader(List<String> outputHeaders, Sheet factorySheet, Row header, CellStyle headerStyle) {
        // 컬럼 순서: No, 제조번호, 재질, 색상, 사이즈, 중심스톤, 비고
        int[] widths = {5 * 256, 15 * 256, 5 * 256, 5 * 256, 9 * 256, 12 * 256, 28 * 256};
        for (int i = 0; i < outputHeaders.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(outputHeaders.get(i));
            cell.setCellStyle(headerStyle);

            factorySheet.setColumnWidth(i, widths[i]);
        }
        header.setHeightInPoints(24); // 헤더 높이 24px
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
        headerFont.setFontHeightInPoints((short) 11);
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
        dataFont.setFontHeightInPoints((short) 11); // 11pt
        dataFont.setBold(true); // 본문 텍스트 굵게
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

    /**
     * 시트의 첫 데이터 행(row 1)에서 "접수일" 컬럼 값을 꺼내 반환한다.
     * 헤더 또는 데이터 행이 없으면 빈 문자열.
     */
    public static String extractFileDate(Sheet worksheet) {
        if (worksheet == null) return "";

        Row headerRow = worksheet.getRow(0);
        if (headerRow == null) return "";

        Integer dateIdx = null;
        for (Cell cell : headerRow) {
            String value = getCellString(cell).trim();
            if ("접수일".equals(value)) {
                dateIdx = cell.getColumnIndex();
                break;
            }
        }
        if (dateIdx == null) return "";

        Row dataRow = worksheet.getRow(1);
        if (dataRow == null) return "";

        return getCellString(dataRow.getCell(dateIdx)).trim();
    }

    /**
     * 시트의 첫 행에서 "접수번호" 헤더 인덱스를 찾아 모든 접수번호를 Set 으로 반환한다.
     * 헤더가 없으면 빈 Set 을 반환한다.
     */
    public static Set<String> extractReceiptNumbers(Sheet worksheet) {
        Set<String> receiptNumbers = new HashSet<>();
        if (worksheet == null) return receiptNumbers;

        Row headerRow = worksheet.getRow(0);
        if (headerRow == null) return receiptNumbers;

        Integer receiptIdx = null;
        for (Cell cell : headerRow) {
            String value = getCellString(cell).trim();
            if ("접수번호".equals(value)) {
                receiptIdx = cell.getColumnIndex();
                break;
            }
        }

        if (receiptIdx == null) return receiptNumbers;

        for (int r = 1; r <= worksheet.getLastRowNum(); r++) {
            Row row = worksheet.getRow(r);
            if (row == null) continue;
            String value = getCellString(row.getCell(receiptIdx)).trim();
            if (!value.isEmpty()) {
                receiptNumbers.add(value);
            }
        }
        return receiptNumbers;
    }

    /**
     * 재질(함량) 정렬용 가중치를 반환한다.
     * 함량이 큰 순서로 정렬되도록 음수 값을 반환하며,
     * 숫자가 없는 값은 마지막으로 보낸다.
     * 예) "18K" -> -18, "14K" -> -14, "" -> Integer.MAX_VALUE
     */
    private static int purityOrder(String material) {
        if (material == null || material.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(material);
        if (matcher.find()) {
            try {
                return -Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignore) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

}
