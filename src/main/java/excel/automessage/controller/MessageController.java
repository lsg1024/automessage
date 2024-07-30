package excel.automessage.controller;

import excel.automessage.dto.message.MessageDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.dto.message.MessageFormDTO;
import excel.automessage.dto.message.MessageResponseDTO;
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

@Controller
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
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:sms";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        // 엑셀 타입만 허용
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:sms";
        }

        ProductDTO.ProductList productList = messageService.messageUpload(file);
        MessageFormDTO messageFormDTO = messageService.messageForm(productList);

        // 엑셀에 등록되지 않은 가게 추가 등록
        if (!messageFormDTO.getMissingStores().isEmpty()) {
            redirectAttributes.addFlashAttribute("missingStores", messageFormDTO.getMissingStores());
            model.addAttribute("smsForm", messageFormDTO.getSmsForm());
            model.addAttribute("smsPhone", messageFormDTO.getSmsPhone());
            return "redirect:store/miss";
        }

        model.addAttribute("smsForm", messageFormDTO.getSmsForm());
        model.addAttribute("smsPhone", messageFormDTO.getSmsPhone());
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
        List<Integer> errorMessage = new ArrayList<>();
        List<MessageResponseDTO> responses = messageService.messageSend(messageDto, errorMessage);

        if (!errorMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("전화번호 없음 : " + errorMessage);
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
