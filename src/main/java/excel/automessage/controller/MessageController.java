package excel.automessage.controller;

import excel.automessage.dto.message.MessageDTO;
import excel.automessage.dto.message.MessageFormDTO;
import excel.automessage.dto.message.MessageResponseDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.service.MessageService;
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
import java.util.Map;

@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({"smsForm", "smsPhone"})
public class MessageController {

    private final MessageService messageService;


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
            log.info("messageUpload file null");
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:/automessage/message";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        // 엑셀 타입만 허용
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            log.info("messageUpload file type miss match");
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:/automessage/message";
        }

        ProductDTO.ProductList productList = messageService.messageUpload(file);
        MessageFormDTO messageFormDTO = messageService.messageForm(productList);

        // 엑셀에 등록되지 않은 가게 추가 등록
        if (!messageFormDTO.getMissingStores().isEmpty()) {
            log.info("messageUpload missingStores");
            redirectAttributes.addFlashAttribute("missingStores", messageFormDTO.getMissingStores());
            redirectAttributes.addFlashAttribute("smsForm", messageFormDTO.getSmsForm());
            redirectAttributes.addFlashAttribute("smsPhone", messageFormDTO.getSmsPhone());
            return "redirect:/automessage/store/miss";
        }

        log.info("messageForm {}", messageFormDTO.getSmsForm());
        log.info("messagePhone {}", messageFormDTO.getSmsPhone());

        redirectAttributes.addFlashAttribute("smsForm", messageFormDTO.getSmsForm());
        redirectAttributes.addFlashAttribute("smsPhone", messageFormDTO.getSmsPhone());
        return "redirect:/automessage/message/content";
    }

    // 메시지 전송 폼
    @GetMapping("/message/content")
    public String messageContent(@ModelAttribute("smsForm") Map<String, List<String>> smsForm,
                                 @ModelAttribute("smsPhone") Map<String, String> smsPhone,
                                 Model model) {
        log.info("messageContent Controller");

        model.addAttribute("smsForm", smsForm);
        model.addAttribute("smsPhone", smsPhone);

        return "messageForm/messageSendForm";
    }


    // 메시지 전송
    @PostMapping("/message/content")
    public ResponseEntity<?> messageSend(@RequestParam Map<String, String[]> paramMap) {

        log.info("messageSend Controller");

        // 폼 데이터를 MessageDTO 리스트로 변환
        List<MessageDTO> messageDtoList = new ArrayList<>();
        String[] phones = paramMap.get("phoneNumbers[].phone");
        String[] contents = paramMap.get("phoneNumbers[].content");
        String[] sendSmsValues = paramMap.get("phoneNumbers[].sendSms");


        log.info("{} {} {}", phones[0], contents[0], sendSmsValues[0]);

        if (phones != null && contents != null && sendSmsValues != null) {
            for (int i = 0; i < phones.length; i++) {
                if ("true".equals(sendSmsValues[i])) { // 체크된 항목만 추가
                    MessageDTO messageDto = new MessageDTO();
                    messageDto.setTo(phones[i]);
                    messageDto.setContent(contents[i]);
                    log.info("message phone {}", phones[i]);
                    messageDtoList.add(messageDto);
                }
            }
        }

        List<Integer> errorMessage = new ArrayList<>();

        List<MessageResponseDTO> responses = messageService.messageSend(messageDtoList, errorMessage);

        if (!errorMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("전화번호 오류 발생: " + errorMessage);
        }

        return ResponseEntity.ok().body(responses);
    }

    // 메시지 로그 조회 폼
//    @GetMapping("/message/log")
//    public String messageLogPage(@ModelAttribute() ) {
//        log.info("messageLogPage");
//        return "messageForm/messageLogForm";
//    }

    // 메시지 로그 조회 -> 검색?
//    @PostMapping("/message/log")
//    public

}
