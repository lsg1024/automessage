package excel.automessage.controller;

import excel.automessage.excel.service.DownloadService;
import excel.automessage.excel.util.ExcelSheetUtils;
import excel.automessage.excel.util.LatestFileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
public class AutoOrderController {

    private final DownloadService downloadService;
    private final LatestFileService latestFileService;

    /** 추가 주문장 처리 결과(공장 -> 바이트)를 보관하는 세션 키 */
    private static final String ADDITIONAL_BOOKS_SESSION_KEY = "additionalFactoryBooks";

    @GetMapping("/order/download")
    public Object downloadExcel(RedirectAttributes redirectAttributes) throws IOException {

        String fileName = "주문 리스트_" + LocalDate.now() + ".zip";

        boolean validateFile = latestFileService.autoOrderListLoad();

        if (validateFile) {
            MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
            if (file != null) {
                byte[] zipBytes = downloadService.downloadXlsZipByFactory(file);

                if (zipBytes == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "자동으로 저장된 파일이 없습니다.\n수동으로 기능을 사용해 주십시오.");
                    return "redirect:/automessage";
                }

                return zipResponse(zipBytes, fileName);
            }
        }

        return "redirect:/automessage";
    }

    /**
     * 현재 주문리스트.xls 의 공장(제조사) 이름 목록 + 파일 날짜 메타정보를 JSON 으로 반환.
     * 모달이 열릴 때 호출되어 다운로드 옵션 + 당일 여부 검증에 사용된다.
     */
    @GetMapping("/order/factories")
    @ResponseBody
    public Map<String, Object> listFactories() {
        Map<String, Object> response = new LinkedHashMap<>();
        String today = LocalDate.now().toString();
        response.put("factories", Collections.emptyList());
        response.put("fileDate", "");
        response.put("today", today);
        response.put("isToday", true);

        if (!latestFileService.autoOrderListLoad()) {
            return response;
        }
        MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
        if (file == null) return response;

        try {
            Workbook wb = ExcelSheetUtils.getSheets(file);
            Sheet sheet = wb.getSheetAt(0);

            List<String> factories = ExcelSheetUtils.extractFactoryNames(sheet, Collections.emptySet());
            String fileDate = ExcelSheetUtils.extractFileDate(sheet);

            response.put("factories", factories);
            response.put("fileDate", fileDate);
            response.put("isToday", fileDate.isEmpty() || fileDate.equals(today));
        } catch (Exception e) {
            log.error("listFactories error", e);
        }
        return response;
    }

    /**
     * 단일 공장 다운로드. ZIP 이 아닌 해당 공장 한 개의 .xlsx 만 받는다.
     */
    @GetMapping("/order/download/single")
    public Object downloadSingleFactory(@RequestParam("factory") String factory,
                                        RedirectAttributes redirectAttributes) throws IOException {
        if (factory == null || factory.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "공장 정보가 없습니다.");
            return "redirect:/automessage";
        }

        if (!latestFileService.autoOrderListLoad()) {
            redirectAttributes.addFlashAttribute("errorMessage", "자동으로 저장된 파일이 없습니다.");
            return "redirect:/automessage";
        }
        MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
        if (file == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "주문리스트 파일을 찾을 수 없습니다.");
            return "redirect:/automessage";
        }

        byte[] excelBytes = downloadService.downloadXlsForFactory(file, factory);
        if (excelBytes == null || excelBytes.length == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 공장 데이터가 없습니다: " + factory);
            return "redirect:/automessage";
        }

        String fileName = factory + "_" + LocalDate.now() + ".xlsx";
        String encodedFileName = encodeFileName(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    /**
     * 추가 주문장 업로드 폼
     */
    @GetMapping("/order/additional")
    public String additionalOrderPage() {
        return "messageForm/additionalOrderUpload";
    }

    /**
     * 추가 주문장 업로드.
     * 업로드된 엑셀을 처리해 공장별 워크북을 만들고 세션에 저장 후, 선택 페이지로 리다이렉트.
     * 실제 다운로드는 /order/additional/select 페이지에서 사용자가 옵션을 골라 발생시킨다.
     */
    @PostMapping("/order/additional")
    public String additionalOrder(@RequestParam("file") MultipartFile file,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "파일을 선택해주세요.");
            return "redirect:/automessage/order/additional";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:/automessage/order/additional";
        }

        try {
            MultipartFile existing = null;
            if (latestFileService.autoOrderListLoad()) {
                existing = latestFileService.getExcelFileAsMultipartOrderList();
            }

            Map<String, byte[]> books = downloadService.prepareAdditionalWorkbooks(file, existing);
            if (books == null || books.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "추가 주문장에 새로 추가된 항목이 없습니다.");
                return "redirect:/automessage/order/additional";
            }

            // HashMap 으로 복사해 직렬화 호환성 확보
            session.setAttribute(ADDITIONAL_BOOKS_SESSION_KEY, new LinkedHashMap<>(books));
            return "redirect:/automessage/order/additional/select";

        } catch (IllegalArgumentException e) {
            log.warn("additionalOrder error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/automessage/order/additional";
        } catch (Exception e) {
            log.error("additionalOrder unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", "추가 주문장 처리 중 오류가 발생했습니다.");
            return "redirect:/automessage/order/additional";
        }
    }

    /**
     * 추가 주문장 다운로드 옵션 선택 페이지.
     */
    @GetMapping("/order/additional/select")
    public String additionalOrderSelect(HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Map<String, byte[]> books = additionalBooksFromSession(session);
        if (books == null || books.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "먼저 추가 주문장 파일을 업로드해주세요.");
            return "redirect:/automessage/order/additional";
        }
        model.addAttribute("factories", new ArrayList<>(books.keySet()));
        return "messageForm/additionalOrderSelect";
    }

    /**
     * 세션에 보관된 공장 목록 JSON. "전체 개별" JS 가 사용한다.
     */
    @GetMapping("/order/additional/factories")
    @ResponseBody
    public List<String> additionalOrderFactories(HttpSession session) {
        Map<String, byte[]> books = additionalBooksFromSession(session);
        if (books == null) return Collections.emptyList();
        return new ArrayList<>(books.keySet());
    }

    /**
     * 세션에 보관된 공장별 워크북을 ZIP 으로 다운로드.
     */
    @GetMapping("/order/additional/download/zip")
    public Object additionalOrderDownloadZip(HttpSession session,
                                              RedirectAttributes redirectAttributes) throws IOException {
        Map<String, byte[]> books = additionalBooksFromSession(session);
        if (books == null || books.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "다운로드 데이터가 없습니다. 다시 업로드해주세요.");
            return "redirect:/automessage/order/additional";
        }
        byte[] zip = downloadService.zipFromMap(books);
        if (zip == null || zip.length == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "ZIP 생성에 실패했습니다.");
            return "redirect:/automessage/order/additional/select";
        }
        return zipResponse(zip, "추가 주문장_" + LocalDate.now() + ".zip");
    }

    /**
     * 세션에서 단일 공장 워크북만 받아 다운로드.
     */
    @GetMapping("/order/additional/download/single")
    public Object additionalOrderDownloadSingle(@RequestParam("factory") String factory,
                                                 HttpSession session,
                                                 RedirectAttributes redirectAttributes) {
        Map<String, byte[]> books = additionalBooksFromSession(session);
        if (books == null || books.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "다운로드 데이터가 없습니다. 다시 업로드해주세요.");
            return "redirect:/automessage/order/additional";
        }
        if (factory == null || factory.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "공장 정보가 없습니다.");
            return "redirect:/automessage/order/additional/select";
        }

        byte[] excelBytes = books.get(factory.toUpperCase());
        if (excelBytes == null || excelBytes.length == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 공장 데이터가 없습니다: " + factory);
            return "redirect:/automessage/order/additional/select";
        }

        String fileName = factory + "_" + LocalDate.now() + ".xlsx";
        String encodedFileName = encodeFileName(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private Map<String, byte[]> additionalBooksFromSession(HttpSession session) {
        Object value = session.getAttribute(ADDITIONAL_BOOKS_SESSION_KEY);
        if (value instanceof Map) {
            return (Map<String, byte[]>) value;
        }
        return null;
    }

    private ResponseEntity<byte[]> zipResponse(byte[] zipBytes, String fileName) {
        String encodedFileName = encodeFileName(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
        return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

}
