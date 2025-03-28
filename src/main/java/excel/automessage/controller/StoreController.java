package excel.automessage.controller;

import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.service.store.StoreService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/automessage")
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({"messageForm"})
public class StoreController {

    private final StoreService storeService;

    // 새로운 상점 url
    @GetMapping("/new")
    public String store() {
        log.info("store controller");
        return "storeForm/storeSelect";
    }

    @GetMapping("/new/store")
    public String newStore(Model model, HttpSession session) {
        log.info("newStore controller");
        StoreListDTO storeListDTO = new StoreListDTO();
        storeListDTO.getStores().add(new StoreDTO());

        String message = (String) session.getAttribute("success");

        if (message != null) {
            model.addAttribute("success", message);
            log.info("newStore Controller success = {}", message);
            session.removeAttribute("success");
        }

        model.addAttribute("storeFormData", storeListDTO);
        return "storeForm/storeInput";
    }

    // 가게 신규 등록 (직접 입력)
    @PostMapping("/new/store")
    public String newStore(@Validated @ModelAttribute("storeFormData") StoreListDTO storeListDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            response.setStatus(400);
            return "storeForm/storeInput";
        }
        try {
            storeService.saveAll(storeListDTO);
        } catch (IllegalStateException e) {
            log.info("IllegalStateException 에러");
            response.setStatus(500);
            bindingResult.reject("storeSaveError", e.getMessage());
            return "storeForm/storeInput";
        }
        redirectAttributes.addFlashAttribute("success", "저장되었습니다.");
        return "redirect:/automessage/new/store";
    }

    // 새로운 상점 (엑셀)
    @GetMapping("/new/stores")
    public String excelStore() {
        log.info("excelStore controller");
        return "storeForm/storeUpload";
    }

    // 가게 신규 등록 (엑셀)
    @PostMapping("/new/stores")
    public String newStores(@RequestParam MultipartFile file, Model model, RedirectAttributes redirectAttributes) throws IOException {
        log.info("newStores (엑셀) Controller");

        if (file.isEmpty()) {
            log.info("file isEmpty");
            redirectAttributes.addFlashAttribute("errorMessage", "파일을 선택해주세요.");
            return "redirect:/automessage/new/stores";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            log.info("file is Not Excel");
            redirectAttributes.addFlashAttribute("errorMessage", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:/automessage/new/stores";
        }

        StoreListDTO storeListDTO = storeService.saveExcelStores(file);

        storeService.saveAll(storeListDTO);

        model.addAttribute("storeList", storeListDTO);
        model.addAttribute("success", "저장 완료");

        return "storeForm/storeSaveList";
    }

    // 등록되지 않은 가게 폼
    @GetMapping("/store/miss")
    public String missingStore(@ModelAttribute("missingStores") List<String> missingStores,
                               @ModelAttribute("messageForm") MessageListDTO messageListDTO,
                               Model model) {
        log.info("missingStore Controller");

        model.addAttribute("missingStores", missingStores);
        model.addAttribute("messageForm", messageListDTO);

        return "storeForm/missingStore";
    }

    // 등록되지 않는 가게
    @PostMapping("/store/miss")
    public String saveMissingStore(@ModelAttribute StoreListDTO storeListDTO,
                                   @ModelAttribute("messageForm") MessageListDTO messageListDTO,
                                   RedirectAttributes redirectAttributes) {

        // 미등록 가게를 저장
        MessageListDTO result = storeService.saveMissingStore(storeListDTO, messageListDTO);

        // messageListDTO 리다이렉트 속성에 추가
        redirectAttributes.addFlashAttribute("messageForm", result);

        return "redirect:/automessage/message/content";
    }

    // 가게 목록
    @GetMapping("/stores")
    public String getStores(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "all") String category,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        log.info("getStores Controller");

        int size = 10;

        try {
            Page<Store> storePage = storeService.searchStores(category, query, page - 1, size);
            int totalPages = storePage.getTotalPages();
            int currentPage = storePage.getNumber() + 1; // Thymeleaf에서 페이지는 1부터 시작
            int startPage = ((currentPage - 1)/ size) * size + 1;
            int endPage = Math.min(startPage + size - 1, totalPages);

            model.addAttribute("url", "stores");
            model.addAttribute("storePage", storePage);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", totalPages);
        } catch (IllegalArgumentException e) {
            log.info("store Controller errorMessage = {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/automessage/stores";
        }

        return "storeForm/storeList";
    }


    // 가게 목록 -> 가게 수정 이동
    @GetMapping("/stores/{id}")
    public String editStore(@PathVariable("id") Long id, Model model) {
        log.info("editStore");

        Store store = storeService.findById(id);

        StoreDTO.Update storeDTO = new StoreDTO.Update(store.getStoreId(), store.getStoreName(), store.getStorePhoneNumber());
        model.addAttribute("storeDTO", storeDTO);
        return "storeForm/storeUpdate";
    }

    // 가게 목록/가게 수정
    @PostMapping("/stores/{id}")
    public String updateStoreName(@RequestParam("id") Long id, @Validated @ModelAttribute("storeDTO") StoreDTO.Update storeDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        log.info("updateStoreName");

        if (bindingResult.hasErrors()) {
            log.error("bindingResult 에러");
            response.setStatus(400);
            return "storeForm/storeUpdate";
        }

        try {
            storeService.updateStore(id, storeDTO);
        } catch (IllegalArgumentException e) {
            log.error("중복 이름 에러");
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/automessage/stores/" + id;
        }

        redirectAttributes.addFlashAttribute("success", "수정 완료");
        return "redirect:/automessage/stores";
    }

    // 가게 목록/가게 삭제
    @PostMapping("/store/{id}")
    public String deleteStore(
            @PathVariable("id") Long id,
            @RequestParam(value = "category", defaultValue = "all") String category,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "storeName") String storeName,
            @RequestParam(value = "storePhoneNumber") String storePhoneNumber,
            RedirectAttributes redirectAttributes) {

        try {
            storeService.deleteStore(id, storeName, storePhoneNumber);
            log.info("deleteStore storeName = {} storePhoneNumber = {}", storeName, storePhoneNumber);
            redirectAttributes.addFlashAttribute("message", "삭제 성공");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }



        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // 현재 쿼리 파라미터를 포함하여 리다이렉트
        return "redirect:/automessage/stores?category=" + category + "&query=" + encodedQuery;
    }

    // 추가할 가게 선택
    @GetMapping("/stores_add")
    public String messageStoreList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "all") String category,
            @ModelAttribute("messageForm") MessageListDTO messageListDTO,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("messageStoreList Controller");
        log.info("messageStoreList Controller messageForm {}", messageListDTO.getMessageListDTO().size());

        int size = 10;

        try {
            Page<Store> storePage = storeService.searchStores(category, query, page - 1, size);
            int totalPages = storePage.getTotalPages();
            int currentPage = storePage.getNumber() + 1;
            int startPage = ((currentPage - 1)/ size) * size + 1;
            int endPage = Math.min(startPage + size - 1, totalPages);

            model.addAttribute("url", "stores_add");
            model.addAttribute("storePage", storePage);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", totalPages);

            model.addAttribute("messageForm", messageListDTO);
        } catch (IllegalArgumentException e) {
            log.info("store Controller errorMessage = {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/automessage/message";
        }

        return "storeForm/storeMessageList";
    }

    @PostMapping("/stores_add")
    public String messageStoreAdd(
            @RequestParam("storeName") String storeName,
            @RequestParam("storePhoneNumber") String storePhoneNumber,
            @ModelAttribute("messageForm") MessageListDTO messageListDTO,
            RedirectAttributes redirectAttributes) {

        log.info("messageAdd Controller MessageListDTO bf {}", messageListDTO.getMessageListDTO().size());

        // 선택한 가게 정보를 messageForm에 추가
        messageListDTO.addStore(storeName, storePhoneNumber);

        // 리다이렉트 시 데이터 유지
        redirectAttributes.addFlashAttribute("messageForm", messageListDTO);

        log.info("messageAdd Controller MessageListDTO af {}", messageListDTO.getMessageListDTO().size());

        return "redirect:/automessage/message/content";
    }

}
