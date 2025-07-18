package excel.automessage.controller;

import excel.automessage.excel.service.DownloadService;
import excel.automessage.excel.util.LatestFileService;
import excel.automessage.service.redis.IdempotencyRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
public class AutoOrderController {

    private final LatestFileService latestFileService;
    private final DownloadService downloadService;
    private final IdempotencyRedisService idempotencyRedisService;

    @GetMapping("/order/download")
    public ResponseEntity<?> downloadExcel() {

        log.info("downloadExcel");
        String fileName = "주문 리스트_" + LocalDate.now() + ".xlsx";

        byte[] excelBytes;
        try {
            MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
            log.info("latestFileService.getExcelFileAsMultipartOrderList() 완료");
            excelBytes = downloadService.downloadXls(file);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "엑셀 파일 생성 실패", e);
        }

        String encodedFileName = encodeFileName(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }


    @GetMapping("/order")
    public String autoOrderMenu() {
        log.info("auto-order controller");
        return "orderForm/orderListMenu";
    }

    // 외부에서 엑셀 값을 받아오는 것
    @GetMapping("/order/auto_order")
    public String autoOrderAuto(RedirectAttributes redirectAttributes) throws IOException {
        log.info("autoOrderAuto Controller");

        boolean autoFile = latestFileService.autoOrderListLoad();
        if (autoFile) {
//            MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
//            downloadService.downloadXls(file);
        }
        redirectAttributes.addFlashAttribute("errorMessage", "자동으로 저장된 파일이 없습니다.\n수동으로 기능을 사용해 주십시오.");

        return "redirect:/automessage";
    }
}
