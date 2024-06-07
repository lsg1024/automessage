package excel.automessage.controller;

import excel.automessage.dto.sms.MessageDTO;
import excel.automessage.dto.sms.ProductDTO;
import excel.automessage.dto.sms.SmsFormDTO;
import excel.automessage.dto.sms.SmsResponseDTO;
import excel.automessage.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({"smsForm", "smsPhone"})
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

        ProductDTO.ProductList productList = smsService.uploadSMS(file);
        SmsFormDTO smsFormDTO = smsService.smsForm(productList);

        if (!smsFormDTO.getMissingStores().isEmpty()) {
            redirectAttributes.addFlashAttribute("missingStores", smsFormDTO.getMissingStores());
            model.addAttribute("smsForm", smsFormDTO.getSmsForm());
            model.addAttribute("smsPhone", smsFormDTO.getSmsPhone());
            return "redirect:/store/missingStore";
        }

        model.addAttribute("smsForm", smsFormDTO.getSmsForm());
        model.addAttribute("smsPhone", smsFormDTO.getSmsPhone());
        return "smsForm/smsSendForm";
    }

    @PostMapping("/sms/send")
    public ResponseEntity<?> sendSms(@RequestBody List<MessageDTO> messageDto) {
        List<SmsResponseDTO> responses = new ArrayList<>();
        List<Integer> errorMessage = new ArrayList<>();
        for (int i = 0; i < messageDto.size(); i++) {
            MessageDTO messageDTO = messageDto.get(i);
            log.info("sendSms getContent = {}, getTo = {}", messageDTO.getContent(), messageDTO.getTo());

            if (!isNumberic(messageDTO.getTo())) {
                errorMessage.add(i + 1);
                continue;
            }

            try {
                 SmsResponseDTO response = smsService.sendSms(messageDTO);
                 responses.add(response);
                 log.info("sendSms response = {}", response.getStatusCode());
            } catch (Exception e) {
                log.error("Error sending SMS for index {}: {}", i, e.getMessage());
                errorMessage.add(i + 1);
            }
        }

        if (!errorMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("전화번호 없음 : " + errorMessage);
        }

        return ResponseEntity.ok().body(responses);
    }

    public static boolean isNumberic(String str) {
        return str.chars().allMatch(Character::isDigit);
    }
}
