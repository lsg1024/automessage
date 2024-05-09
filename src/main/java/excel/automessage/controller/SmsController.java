package excel.automessage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import excel.automessage.dto.MessageDTO;
import excel.automessage.dto.ProductDTO;
import excel.automessage.dto.SmsFormDTO;
import excel.automessage.dto.SmsResponseDTO;
import excel.automessage.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/sms")
    public String uploadSmsData(@RequestParam MultipartFile file, Model model, RedirectAttributes redirectAttributes) throws IOException {
        log.info("uploadSmsData Controller");
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:sms";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:sms";
        }

        Workbook workbook = extension.equalsIgnoreCase("xls") ? new HSSFWorkbook(file.getInputStream()) : new XSSFWorkbook(file.getInputStream());
        Sheet worksheet = workbook.getSheetAt(0);
        DataFormatter dataFormatter = new DataFormatter();

        ProductDTO.ProductList productList = new ProductDTO.ProductList();

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) continue;

            ProductDTO productDTO = new ProductDTO();

            Cell cell = row.getCell(11); // 판매 정보
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String sellType = cell.getStringCellValue();
                if (!sellType.startsWith("판매")) {
                    continue;
                }
            }


            cell = row.getCell(14); // 상품 정보
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String productName = cell.getStringCellValue();
                if (!productName.startsWith("통상")) {
                    productDTO.setProductName(productName);
                } else {
                    continue;
                }
            }

            cell = row.getCell(9); // 이름 셀
            if (cell != null) {
                productDTO.setStoreName(dataFormatter.formatCellValue(cell));
            }

            productList.getProductDTOList().add(productDTO);
        }

        workbook.close();

        log.info("uploadSmsData SmsForm Data = {}", productList.getProductDTOList().get(0).getProductName());

        SmsFormDTO smsFormDTO = smsService.smsForm(productList);
        model.addAttribute("smsForm", smsFormDTO);

        return "smsForm/smsList";
    }

    @PostMapping("/sms/send")
    public ResponseEntity<?> sendSms(@RequestBody List<MessageDTO> messageDto) throws JsonProcessingException, RestClientException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {

        List<SmsResponseDTO> responses = new ArrayList<>();
        List<Integer> errorMessage = new ArrayList<>();
        for (int i = 0; i < messageDto.size(); i++) {
            MessageDTO messageDTO = messageDto.get(i);
            log.info("sendSms getContent = {}, getTo = {}", messageDTO.getContent(), messageDTO.getTo());

            if (messageDTO.getTo().equals("번호 없음")) {
                errorMessage.add(i);
                continue;
            }

            try {
                SmsResponseDTO response = smsService.sendSms(messageDTO);
                responses.add(response);
                log.info("sendSms response = {}", response.getStatusCode());
            } catch (Exception e) {
                log.error("Error sending SMS for index {}: {}", i, e.getMessage());
                errorMessage.add(i);
            }
        }

        if (!errorMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("전화번호 없음 : " + errorMessage);
        }

        return ResponseEntity.ok().body(responses);
    }

}
