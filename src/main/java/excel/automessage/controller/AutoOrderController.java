package excel.automessage.controller;

import excel.automessage.excel.service.DownloadService;
import excel.automessage.excel.util.LatestFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
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

    private final DownloadService downloadService;
    private final LatestFileService latestFileService;

    @GetMapping("/order/download")
    public Object downloadExcel(RedirectAttributes redirectAttributes) throws IOException {

        String fileName = "주문 리스트_" + LocalDate.now() + ".xlsx";

        boolean validateFile = latestFileService.autoOrderListLoad();

        if (validateFile) {
            MultipartFile file = latestFileService.getExcelFileAsMultipartOrderList();
            if (file != null) {
                byte[] excelBytes = downloadService.downloadXls(file);

                if (excelBytes == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "자동으로 저장된 파일이 없습니다.\n수동으로 기능을 사용해 주십시오.");
                    return "redirect:/automessage";
                }

                String encodedFileName = encodeFileName(fileName);

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
                headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

                return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            }
        }

        return "redirect:/automessage";
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

}
