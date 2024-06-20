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
import org.springframework.web.bind.annotation.*;
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


    // 메시지 양식 업로드 폼
    @GetMapping("/message")
    public String message() {
        log.info("message controller");
        return "messageForm/messageFileUpload";
    }

    // 메시지 양식 업로드 (엑셀)
    @PostMapping("/message")
    public String messageUpload(@RequestParam MultipartFile file, Model model, RedirectAttributes redirectAttributes) throws IOException {
        log.info("messageUpload Controller");

        // 파일 null 체크
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:sms";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        // 엑셀 타입만 허용
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:sms";
        }

        ProductDTO.ProductList productList = smsService.uploadSMS(file);
        SmsFormDTO smsFormDTO = smsService.smsForm(productList);

        // 엑셀에 등록되지 않은 가게 추가 등록
        if (!smsFormDTO.getMissingStores().isEmpty()) {
            redirectAttributes.addFlashAttribute("missingStores", smsFormDTO.getMissingStores());
            model.addAttribute("smsForm", smsFormDTO.getSmsForm());
            model.addAttribute("smsPhone", smsFormDTO.getSmsPhone());
            return "redirect:store/miss";
        }

        model.addAttribute("smsForm", smsFormDTO.getSmsForm());
        model.addAttribute("smsPhone", smsFormDTO.getSmsPhone());
        return "messageForm/messageSendForm";
    }

    // 메시지 전송 폼
    @GetMapping("/message/content")
    public String messageContent() {
        log.info("messageContent Controller");
        return "messageForm/messageSendForm";
    }

    // 메시지 전송
    @PostMapping("/message/content")
    public ResponseEntity<?> messageSend(@RequestBody List<MessageDTO> messageDto) {
        List<SmsResponseDTO> responses = new ArrayList<>();
        List<Integer> errorMessage = new ArrayList<>();
        for (int i = 0; i < messageDto.size(); i++) {
            MessageDTO messageDTO = messageDto.get(i);
            log.info("messageSend getContent = {}, getTo = {}", messageDTO.getContent(), messageDTO.getTo());

            if (!isNumberic(messageDTO.getTo())) {
                errorMessage.add(i + 1);
                continue;
            }

            try {
                 SmsResponseDTO response = smsService.sendSms(messageDTO);
                 responses.add(response);
                 log.info("messageSend 응답 = {}", response.getStatusCode());
            } catch (Exception e) {
                log.error("messageSend 번호 오류 {}: {}", i, e.getMessage());
                errorMessage.add(i + 1);
            }
        }

        if (!errorMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("전화번호 없음 : " + errorMessage);
        }

        return ResponseEntity.ok().body(responses);
    }

    // 숫자로만 이뤄져있는지 확인 코드
    private static boolean isNumberic(String str) {
        return str.chars().allMatch(Character::isDigit);
    }
}
