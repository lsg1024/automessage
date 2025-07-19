package excel.automessage.controller;

import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.message.MessageResponseDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.dto.message.log.MessageLogDetailDTO;
import excel.automessage.dto.message.log.MessageStorageDTO;
import excel.automessage.excel.util.LatestFileService;
import excel.automessage.service.message.MessageService;
import excel.automessage.service.redis.IdempotencyRedisService;
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
import java.util.UUID;

@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({"messageForm"})
public class MessageController {

    private final MessageService messageService;
    private final LatestFileService latestFileService;
    private final IdempotencyRedisService idempotencyRedisService;

    @GetMapping("/message")
    public String messageMenu() {
        log.info("message controller");
        return "messageForm/messageMenu";
    }

    // 메시지 양식 자동 호출
    @GetMapping("/message/auto_send")
    public String messageAuto(RedirectAttributes redirectAttributes) throws IOException {
        log.info("message Auto Controller");

        boolean autoFile = latestFileService.messageAutoLoad();

        //파일 업로드 메서드 호출
        if (autoFile) {
            MultipartFile file = latestFileService.getExcelFileAsMultipart();
            if (file != null) {
                return messageUpload(file, redirectAttributes, true);
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", "자동으로 저장된 파일이 없습니다.\n수동으로 기능을 사용해 주십시오.");

        return "redirect:/automessage/message";
    }

    // 메시지 양식 수동 작성
    @GetMapping("/message/manual_send")
    public String messageManual(Model model) {
        log.info("message Manual Controller");
        model.addAttribute("messageForm", new MessageListDTO());
        return "messageForm/messageSendForm";
    }

    // 메시지 양식 업로드 폼
    @GetMapping("/message/file_send")
    public String messageFile() {
        log.info("message File Controller");
        return "messageForm/messageFileUpload";
    }

    // 메시지 양식 업로드 (엑셀)
    @PostMapping("/message/file_send")
    public String messageUpload(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes, boolean option) throws IOException {
        log.info("messageUpload Controller {}", file.getOriginalFilename());

        // 파일 null 체크
        if (file.isEmpty()) {
            log.info("messageUpload file null");
            redirectAttributes.addFlashAttribute("errorMessage", "파일을 선택해주세요.");
            return "redirect:/automessage/message";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        // 엑셀 타입만 허용
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            log.info("messageUpload file type miss match");
            redirectAttributes.addFlashAttribute("errorMessage", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:/automessage/message";
        }

        try {
            ProductDTO.ProductList productList = messageService.messageUpload(file, option);

            // 메시지 폼 생성 및 미등록 가게 확인
            MessageListDTO messageListDTO = messageService.messageForm(productList);

            // 중복되는 미등록 가게 이름 하나로 통합 -> [가게1, 가게1] -> [가게1]
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

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/automessage/message";
        }

    }

    // 메시지 전송 폼
    @GetMapping("/message/content")
    public String messageContent(@ModelAttribute("messageForm") MessageListDTO messageListDTO, Model model) {
        log.info("messageContent Controller");

        // 멱등성을 위한 키 생성
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("messageContent idempotencyKey = {}", idempotencyKey);
        model.addAttribute("messageForm", messageListDTO);
        model.addAttribute("idempotencyKey", idempotencyKey);
        return "messageForm/messageSendForm";
    }

    // 메시지 전송
    @PostMapping("/message/content")
    public String sendMessage(@ModelAttribute("messageForm") MessageListDTO messageListDTO,
                              @RequestParam("idempotencyKey") String idempotencyKey,
                              RedirectAttributes redirectAttributes) {
        log.info("sendMessage Controller");

        log.info("idempotencyKey = {}", idempotencyKey);

        //중복 요청 체크
        if (idempotencyRedisService.isDuplicateRequest(idempotencyKey)) {
            log.info("중복 요청 감지: " + idempotencyKey);
            redirectAttributes.addFlashAttribute("responses", "중복데이터 발생");
            return "redirect:/automessage/message/result";
        }

        idempotencyRedisService.saveIdempotencyKey(idempotencyKey);

        List<Integer> errorMessage = new ArrayList<>();
//      메시지 전송
        List<MessageResponseDTO> responses = messageService.checkMessageSend(messageListDTO, errorMessage);

//      전송 결과를 모델에 추가
        redirectAttributes.addFlashAttribute("responses", responses);
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

        // 전송 결과 페이지로 리다이렉트
        return "redirect:/automessage/message/result";
    }

    // 메시지 추가
    @PostMapping("/message/content/add")
    public String messageAdd(@ModelAttribute("messageForm") MessageListDTO messageListDTO,
                             RedirectAttributes redirectAttributes) {

        log.info("messageAdd Controller");

        redirectAttributes.addFlashAttribute("messageForm", messageListDTO);

        log.info("messageAdd Controller messageFormDto {}", messageListDTO.getMessageListDTO().size());

        return "redirect:/automessage/stores_add";
    }

    // 메시지 로그 조회 폼
    @GetMapping("/message/log")
    public String messageLogPage(@RequestParam(defaultValue = "1") int page,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        log.info("messageLogPage");

        int size = 10;
        String end = LocalDateTime.now().toString().substring(0, 10) + " " + "23:59:59";
        log.info("end = {}", end);

        try {
            Page<MessageStorageDTO> messageLog = messageService.searchMessageLog(end, page - 1, size);

            int totalPage = messageLog.getTotalPages();
            int currentPage = messageLog.getNumber() + 1;
            int startPage = ((currentPage - 1) / size) * size + 1;
            int endPage = Math.min(startPage + size - 1, totalPage);

            if (page > totalPage) {
                redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 페이지 입니다.");
                return "redirect:/automessage/message/log?";
            }

            model.addAttribute("url", "message/log");
            model.addAttribute("messageLog", messageLog);
            model.addAttribute("totalPage", totalPage);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("currentPage", currentPage);
        } catch (Exception e) {
            log.info("messageLogPage error = {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 경로 입니다.");
            return "redirect:/automessage/message/log?";
        }

        return "messageForm/messageLogForm";
    }

    // 메시지 로그 상세 페이지
    @GetMapping("/message/log/{id}")
    public String messageLogDetailPage(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {

        try {
            MessageLogDetailDTO.MessageLogsDTO messageLogs = messageService.searchMessageLogDetail(id);

            log.info("messageLogs {}", messageLogs.getMessageLogs().entrySet());

            if (messageLogs.getMessageLogs().size() > 0) {
                model.addAttribute("messageLogs", messageLogs);
            }
            else {
                redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 경로 입니다.");
                return "redirect:/automessage/message/log?";
            }
        } catch (Exception e) {
            log.info("messageLogPage error = {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 경로 입니다.");
            return "redirect:/automessage/message/log?";
        }


        return "messageForm/messageLogDetailForm";
    }

    // 메시지 로그 삭제
    @PostMapping("/message/log/delete/{id}")
    public String messageLogDelete(@PathVariable("id") String id,  RedirectAttributes redirectAttributes) {

        try {
            messageService.deleteLog(id);
            redirectAttributes.addFlashAttribute("response", "삭제 완료");
            return "redirect:/automessage/message/log";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("response", e.getMessage());
            return "redirect:/automessage/message/log";
        }

    }

    // 결과 alert 페이지
    @GetMapping("/message/result")
    public String showResultPage(){

        // 결과 페이지를 반환
        return "common/alert";
    }

}
