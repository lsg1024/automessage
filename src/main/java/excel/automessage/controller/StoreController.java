package excel.automessage.controller;

import excel.automessage.entity.Store;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.service.StoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({"smsForm", "smsPhone"})
public class StoreController {

    private final StoreService storeService;

    @ModelAttribute("smsForm")
    public Map<String, List<String>> initSmsForm() {
        return new HashMap<>();
    }

    @ModelAttribute("smsPhone")
    public Map<String, String> initSmsPhone() {
        return new HashMap<>();
    }

    // 새로운 상점 url
    @GetMapping("/new")
    public String store() {
        log.info("store controller");
        return "storeForm/storeSelect";
    }

    @GetMapping("/new/store")
    public String newStore(Model model) {
        log.info("newStore controller");
        StoreListDTO storeListDTO = new StoreListDTO();
        storeListDTO.getStores().add(new StoreDTO());
        model.addAttribute("storeFormData", storeListDTO);
        return "storeForm/storeInput";
    }

    // 가게 신규 등록 (직접 입력)
    @PostMapping("/new/store")
    public String newStore(@Validated @ModelAttribute("storeFormData") StoreListDTO storeListDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            log.error("bindingResult 에러");
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
        redirectAttributes.addFlashAttribute("message", "저장되었습니다.");
        return "redirect:/new/store";
    }

    // 새로운 상점 (엑셀 입력)
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
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:store/excelStore";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:store/excelStore";
        }

        StoreListDTO storeListDTO = storeService.saveStores(file);
        log.info("newStores storeListDTO = {}", storeListDTO.getStores().get(0).getName());
        storeService.saveAll(storeListDTO);

        model.addAttribute("storeList", storeListDTO);
        model.addAttribute("success", "저장 완료");

        return "storeForm/storeSaveList";
    }

    // 등록되지 않은 가게 폼
    @GetMapping("/store/miss")
    public String missingStore() {
        log.info("missingStore Controller");
        return "storeForm/missingStore";
    }

    // 등록되지 않는 가게
    @PostMapping("/store/miss")
    public String saveMissingStore(@ModelAttribute StoreListDTO storeListDTO,
                                   @SessionAttribute("smsForm") Map<String, List<String>> smsForm,
                                   @SessionAttribute("smsPhone") Map<String, String> smsPhone,
                                   RedirectAttributes redirectAttributes, SessionStatus sessionStatus) {

        log.info("saveMissingStore StoreListDTO = {}", storeListDTO.getStores().size());

        storeService.saveAll(storeListDTO);

        storeListDTO.getStores().forEach(store -> {
            if (store.getPhone() != null) {
                smsPhone.put(store.getName(), store.getPhone());
            }
        });

        log.info("saveMissingStore smsForm = {}", smsForm.size());
        log.info("saveMissingStore smsPhone = {}", smsPhone.size());

        redirectAttributes.addFlashAttribute("smsForm", smsForm);
        redirectAttributes.addFlashAttribute("smsPhone", smsPhone);

        sessionStatus.setComplete(); // 세션 종료
        return "redirect:/message/content";
    }

    // 가게 목록
    @GetMapping("/stores")
    public String getStores(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "all") String category,
                            Model model) {

        log.info("getStores Controller");

        int size = 10;
        Page<Store> storePage = storeService.searchStores(category, query, page - 1, size);

        int totalPages = storePage.getTotalPages();
        int currentPage = storePage.getNumber() + 1; // Thymeleaf에서 페이지는 1부터 시작
        int startPage = ((currentPage - 1)/ size) * size + 1;
        int endPage = Math.min(startPage + size - 1, totalPages);

        model.addAttribute("storePage", storePage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

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
        log.info("storeId = {}", id);

        if (bindingResult.hasErrors()) {
            log.error("bindingResult 에러");
            response.setStatus(400);
            return "storeForm/storeUpdate";
        }

        storeService.updateStore(id, storeDTO);
        redirectAttributes.addFlashAttribute("success", "수정 완료");
        return "redirect:/stores";
    }

    // 가게 목록/가게 삭제
    @DeleteMapping("stores/{id}")
    @ResponseBody
    public void deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
    }


}
