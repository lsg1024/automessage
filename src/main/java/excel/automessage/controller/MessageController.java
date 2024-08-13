package excel.automessage.controller;

import excel.automessage.dto.message.*;
import excel.automessage.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.LocalDateTime.*;

@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
@Slf4j
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
    public String messageUpload(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
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

        // 메시지 폼 생성 및 미등록 가게 확인
        SmsFormDTO smsFormDTO = messageService.messageForm(productList);
        List<String> missingStores = smsFormDTO.getSmsFormDTO().stream()
                .flatMap(entry -> entry.getMissingStores().stream())
                .distinct()
                .toList();

        log.info("messageUpload smsFormDTO size {}",smsFormDTO.getSmsFormDTO().size());
        log.info("messageUpload missDTO {}", missingStores.size());

        if (!missingStores.isEmpty()) {
            log.info("messageUpload missingStores {}", missingStores);
            // 미등록 가게가 있으면 메시지 폼을 생성하지 않고 미등록 가게 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("missingStores", missingStores);
            redirectAttributes.addFlashAttribute("smsForm", smsFormDTO);
            return "redirect:/automessage/store/miss";
        }

        redirectAttributes.addFlashAttribute("smsForm", smsFormDTO);

        return "redirect:/automessage/message/content";
    }

    // 메시지 전송 폼
    @GetMapping("/message/content")
    public String messageContent(@ModelAttribute("smsForm") SmsFormDTO smsForm, Model model) {
        log.info("messageContent Controller");
        log.info("messageContent smsForm size {}", smsForm.getSmsFormDTO().size());
        model.addAttribute("smsForm", smsForm);

        return "messageForm/messageSendForm";
    }


    // 메시지 전송
    @PostMapping("/message/content")
    public String sendMessage(@ModelAttribute("smsForm") SmsFormDTO smsForm, RedirectAttributes redirectAttributes, Model model) {
        List<Integer> errorMessage = new ArrayList<>();

        log.info("sendMessage smsForm size {}",smsForm.getSmsFormDTO().size());
//      메시지 전송
        List<MessageResponseDTO> responses = messageService.processAndSendMessages(smsForm, errorMessage);

//      전송 결과를 모델에 추가
        redirectAttributes.addFlashAttribute("responses", responses);
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

        log.info("sendMessage response size {}", responses.size());
        log.info("sendMessage errorMessage {}", responses.size());

        // 전송 결과 페이지로 리다이렉트
        return "redirect:/automessage/message/result";
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

    // 결과 alert 페이지
    @GetMapping("/message/result")
    public String showResultPage(){

        // 결과 페이지를 반환
        return "common/alert";
    }

}
