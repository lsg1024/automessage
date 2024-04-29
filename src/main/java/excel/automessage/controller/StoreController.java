package excel.automessage.controller;

import excel.automessage.dto.StoreDTO;
import excel.automessage.dto.StoreListDTO;
import excel.automessage.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StoreController {

    private final StoreService storeService;

    //엑셀 값 읽기
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
        DataFormatter dataFormatter = new DataFormatter();

        StoreListDTO storeListDTO = new StoreListDTO();

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) continue;

            StoreDTO.Save data = new StoreDTO.Save();

            Cell cell = row.getCell(5); // 이름 셀
            if (cell != null) {
                data.setName(dataFormatter.formatCellValue(cell));
            }

            cell = row.getCell(16); // 전화번호 셀
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String phoneNumber = cell.getStringCellValue();
                if (phoneNumber.startsWith("010")) {
                    data.setPhone(phoneNumber);
                }
            }
            storeListDTO.getStores().add(data);

        }

        storeService.saveAll(storeListDTO);
        workbook.close();
        model.addAttribute("storeList", storeListDTO);

        return "storeForm/storeList";

    }

}
