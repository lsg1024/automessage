package excel.automessage.controller;

import excel.automessage.dto.message.MessageResponseDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.message.log.MessageLogDetailDTO;
import excel.automessage.dto.message.log.MessageStorageDTO;
import excel.automessage.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        MessageListDTO messageListDTO = messageService.messageForm(productList);
        List<String> missingStores = messageListDTO.getMessageListDTO().stream()
                .flatMap(entry -> entry.getMissingStores().stream())
                .distinct()
                .toList();

        if (!missingStores.isEmpty()) {
            log.info("messageUpload missingStores {}", missingStores);
            // 미등록 가게가 있으면 메시지 폼을 생성하지 않고 미등록 가게 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("missingStores", missingStores);
            redirectAttributes.addFlashAttribute("messageForm", messageListDTO);
            return "redirect:/automessage/store/miss";
        }

        redirectAttributes.addFlashAttribute("messageForm", messageListDTO);

        return "redirect:/automessage/message/content";
    }

    // 메시지 전송 폼
    @GetMapping("/message/content")
    public String messageContent(@ModelAttribute("messageForm") MessageListDTO messageListDTO, Model model) {
        log.info("messageContent Controller");
        model.addAttribute("messageForm", messageListDTO);
        return "messageForm/messageSendForm";
    }


    // 메시지 전송
    @PostMapping("/message/content")
    public String sendMessage(@ModelAttribute("messageForm") MessageListDTO messageListDTO, RedirectAttributes redirectAttributes) {
        List<Integer> errorMessage = new ArrayList<>();

//      메시지 전송
        List<MessageResponseDTO> responses = messageService.processAndSendMessages(messageListDTO, errorMessage);

//      전송 결과를 모델에 추가
        redirectAttributes.addFlashAttribute("responses", responses);
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

        // 전송 결과 페이지로 리다이렉트
        return "redirect:/automessage/message/result";
    }

    // 메시지 로그 조회 폼
    @GetMapping("/message/log")
    public String messageLogPage(@RequestParam(defaultValue = "1") int page, Model model) {
        log.info("messageLogPage");

        int size = 10;
        String end = LocalDateTime.now().toString().substring(0, 10) + " " + "23:59:59";
        log.info("end = {}", end);
        Page<MessageStorageDTO> messageLog = messageService.searchMessageLog(end, page - 1, size);

        int totalPage = messageLog.getTotalPages();
        int currentPage = messageLog.getNumber() + 1;
        int startPage = ((currentPage - 1) / size) * size + 1;
        int endPage = Math.min(startPage + size - 1, totalPage);

        model.addAttribute("messageLog", messageLog);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("currentPage", currentPage);

        return "messageForm/messageLogForm";
    }

    // 메시지 상세 페이지
    @GetMapping("/message/log/{id}")
    public String messageLogDetailPage(@PathVariable("id") String id, Model model) {

        MessageLogDetailDTO.MessageLogsDTO messageLogs = messageService.searchMessageLogDetail(id);

        model.addAttribute("messageLogs", messageLogs);

        return "messageForm/messageLogDetailForm";
    }


    // 결과 alert 페이지
    @GetMapping("/message/result")
    public String showResultPage(){

        // 결과 페이지를 반환
        return "common/alert";
    }

}
