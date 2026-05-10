package excel.automessage.excel.service;

import excel.automessage.excel.util.ExcelSheetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static excel.automessage.excel.util.ExcelSheetUtils.createOrderWorkSheet;
import static excel.automessage.excel.util.ExcelSheetUtils.createOrderWorkbooksByFactory;
import static excel.automessage.excel.util.ExcelSheetUtils.extractFactoryNames;
import static excel.automessage.excel.util.ExcelSheetUtils.extractReceiptNumbers;

@Slf4j
@Service
public class DownloadService {

    public byte[] downloadXls(MultipartFile file) throws IOException {

        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        LocalDate today = LocalDate.now();

        if (!checkDateValidate(worksheet, today)) {
            log.warn("주문리스트 접수일이 오늘 날짜({})와 다릅니다. 파일을 그대로 처리합니다.", today);
        }
        return createOrderWorkSheet(worksheet, today);
    }

    /**
     * 입력 파일에서 공장명 목록만 추출한다.
     */
    public List<String> listFactories(MultipartFile file) throws IOException {
        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);
        return extractFactoryNames(worksheet, Collections.emptySet());
    }

    /**
     * 입력 파일에서 단일 공장의 워크북 바이트만 반환한다. 해당 공장이 없으면 null.
     */
    public byte[] downloadXlsForFactory(MultipartFile file, String factory) throws IOException {
        if (factory == null || factory.trim().isEmpty()) return null;

        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        Map<String, byte[]> books = createOrderWorkbooksByFactory(worksheet, LocalDate.now(), Collections.emptySet());
        // extractFactoryRows 가 공장명을 toUpperCase 처리하므로 key 도 동일 규칙으로 조회
        return books.get(factory.toUpperCase());
    }

    /**
     * 공장별 단일 시트 워크북을 만들어 ZIP 으로 묶어 반환한다.
     */
    public byte[] downloadXlsZipByFactory(MultipartFile file) throws IOException {
        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        LocalDate today = LocalDate.now();
        if (!checkDateValidate(worksheet, today)) {
            log.warn("주문리스트 접수일이 오늘 날짜({})와 다릅니다. 파일을 그대로 처리합니다.", today);
        }

        Map<String, byte[]> workbooks = createOrderWorkbooksByFactory(worksheet, today, Collections.emptySet());
        if (workbooks.isEmpty()) return null;

        return zipWorkbooks(workbooks, today);
    }

    /**
     * 추가 주문장 다운로드.
     * 업로드된 엑셀에서, 기존 주문리스트.xls에 이미 존재하는 접수번호를 제외하고
     * 동일한 포멧팅으로 변환한 결과 바이트를 반환한다.
     *
     * @param uploadedFile 사용자가 업로드한 엑셀 파일
     * @param existingFile 서버에 저장된 주문리스트.xls (없으면 null 가능)
     */
    public byte[] downloadAdditionalXls(MultipartFile uploadedFile, MultipartFile existingFile) throws IOException {

        Workbook uploadedWorkbook = ExcelSheetUtils.getSheets(uploadedFile);
        Sheet uploadedSheet = uploadedWorkbook.getSheetAt(0);

        Set<String> excluded = collectExcludedReceiptNumbers(existingFile);

        return createOrderWorkSheet(uploadedSheet, LocalDate.now(), excluded);
    }

    /**
     * 추가 주문장을 공장별 분리 워크북 ZIP 으로 다운로드.
     */
    public byte[] downloadAdditionalXlsZipByFactory(MultipartFile uploadedFile, MultipartFile existingFile) throws IOException {
        Workbook uploadedWorkbook = ExcelSheetUtils.getSheets(uploadedFile);
        Sheet uploadedSheet = uploadedWorkbook.getSheetAt(0);

        Set<String> excluded = collectExcludedReceiptNumbers(existingFile);
        LocalDate today = LocalDate.now();

        Map<String, byte[]> workbooks = createOrderWorkbooksByFactory(uploadedSheet, today, excluded);
        if (workbooks.isEmpty()) return null;

        return zipWorkbooks(workbooks, today);
    }

    /**
     * 업로드된 추가 주문장을 공장별 워크북으로 미리 만들어 반환한다.
     * 컨트롤러는 이 결과를 세션에 보관해 ZIP / 개별 / 단일 다운로드에 재사용한다.
     */
    public Map<String, byte[]> prepareAdditionalWorkbooks(MultipartFile uploadedFile, MultipartFile existingFile) throws IOException {
        Workbook uploadedWorkbook = ExcelSheetUtils.getSheets(uploadedFile);
        Sheet uploadedSheet = uploadedWorkbook.getSheetAt(0);

        Set<String> excluded = collectExcludedReceiptNumbers(existingFile);
        return createOrderWorkbooksByFactory(uploadedSheet, LocalDate.now(), excluded);
    }

    /**
     * 이미 처리된 (공장 -> 바이트) 맵을 ZIP 으로 묶어 반환.
     */
    public byte[] zipFromMap(Map<String, byte[]> workbooks) throws IOException {
        if (workbooks == null || workbooks.isEmpty()) return null;
        return zipWorkbooks(workbooks, LocalDate.now());
    }

    private Set<String> collectExcludedReceiptNumbers(MultipartFile existingFile) {
        if (existingFile == null) return Collections.emptySet();
        try {
            Workbook existingWorkbook = ExcelSheetUtils.getSheets(existingFile);
            Sheet existingSheet = existingWorkbook.getSheetAt(0);
            return extractReceiptNumbers(existingSheet);
        } catch (Exception ignore) {
            // 기존 파일을 못 읽으면 제외 없이 진행
            return Collections.emptySet();
        }
    }

    /**
     * 공장명 -> xlsx 바이트 맵을 ZIP 으로 묶어 바이트 배열로 반환한다.
     * ZIP 안의 각 파일명은 "{공장명}_{날짜}.xlsx" 형식.
     */
    private byte[] zipWorkbooks(Map<String, byte[]> workbooks, LocalDate today) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : workbooks.entrySet()) {
                String safeName = sanitizeFileName(entry.getKey());
                String fileName = safeName + "_" + today + ".xlsx";
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private String sanitizeFileName(String factory) {
        if (factory == null || factory.trim().isEmpty()) return "공장";
        // 파일시스템에서 금지된 문자 치환
        return factory.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private boolean checkDateValidate(Sheet worksheet, LocalDate today) {

        Row row = worksheet.getRow(1);
        Cell cell = row.getCell(7);
        if (cell != null && cell.getCellType() == CellType.STRING) {
            String sellType = cell.getStringCellValue();
            return sellType.equals(today.toString());
        }
        return true;
    }

}