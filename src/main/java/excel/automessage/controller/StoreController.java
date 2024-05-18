package excel.automessage.controller;

import excel.automessage.domain.Store;
import excel.automessage.dto.StoreListDTO;
import excel.automessage.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @PostMapping("/upload")
    public String uploadStoreData(@RequestParam MultipartFile file, Model model, RedirectAttributes redirectAttributes) throws IOException {
        log.info("uploadStoreData Controller");
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.");
            return "redirect:upload";
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("xlsx") && !extension.equalsIgnoreCase("xls"))) {
            redirectAttributes.addFlashAttribute("message", "엑셀 파일만 업로드 가능합니다.");
            return "redirect:upload";
        }

        Workbook workbook = extension.equalsIgnoreCase("xls") ? new HSSFWorkbook(file.getInputStream()) : new XSSFWorkbook(file.getInputStream());
        Sheet worksheet = workbook.getSheetAt(0);

        StoreListDTO storeListDTO = storeService.formattingValue(worksheet);
        storeService.saveAll(storeListDTO);

        workbook.close();
        model.addAttribute("storeList", storeListDTO);
        model.addAttribute("success", "저장 완료");

        return "storeForm/storeSaveList";
    }

    @PostMapping("/storeMissingCreate")
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
        return "redirect:/sms/content";
    }

    @GetMapping("/storeList")
    public String loadStores(@RequestParam(defaultValue = "1") int page, Model model) {

        int size = 10;
        Page<Store> storePage = storeService.getStores(page - 1, size);

        int totalPages = storePage.getTotalPages();
        int currentPage = storePage.getNumber() + 1;
        int startPage = (currentPage / size) * size + 1;
        int endPage = Math.min(startPage + size - 1, totalPages);

        model.addAttribute("storePage", storePage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

        log.info("storePage = {}", storePage);
        log.info("startPage = {}", startPage);
        log.info("endPage = {}", endPage);

        return "storeForm/storeList";
    }

}
