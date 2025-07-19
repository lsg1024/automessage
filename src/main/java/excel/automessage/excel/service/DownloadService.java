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

//        checkDateValidate(worksheet, today);
        return createOrderWorkSheet(worksheet, today);
    }

    private void checkDateValidate(Sheet worksheet, LocalDate today) {

        Row row = worksheet.getRow(1);
        Cell cell = row.getCell(7);
        if (cell != null && cell.getCellType() == CellType.STRING) {
            String sellType = cell.getStringCellValue();
            if (!sellType.equals(today.toString())) {
                throw new IllegalArgumentException("오늘 판매 데이터가 아닙니다.\n수동으로 입력해주세요.");
            }
        }
    }

}