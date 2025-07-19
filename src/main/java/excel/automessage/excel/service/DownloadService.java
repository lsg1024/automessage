package excel.automessage.excel.service;

import excel.automessage.excel.util.ExcelSheetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

import static excel.automessage.excel.util.ExcelSheetUtils.createOrderWorkSheet;

@Slf4j
@Service
public class DownloadService {

    public byte[] downloadXls(MultipartFile file) throws IOException {

        log.info("DownloadService start");
        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        LocalDate today = LocalDate.now();
        return createOrderWorkSheet(worksheet, today);
    }

}