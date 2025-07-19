package excel.automessage.excel.service;

import excel.automessage.excel.util.ExcelSheetUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

import static excel.automessage.excel.util.ExcelSheetUtils.createOrderWorkSheet;

@Service
public class DownloadService {

    public byte[] downloadXls(MultipartFile file) throws IOException {

        Workbook sheets = ExcelSheetUtils.getSheets(file);
        Sheet worksheet = sheets.getSheetAt(0);

        LocalDate today = LocalDate.now();

        if (!checkDateValidate(worksheet, today)) return null;
        return createOrderWorkSheet(worksheet, today);
    }

    private boolean checkDateValidate(Sheet worksheet, LocalDate today) {

        Row row = worksheet.getRow(1);
        Cell cell = row.getCell(7);
        if (cell != null && cell.getCellType() == CellType.STRING) {
            String sellType = cell.getStringCellValue();
            return sellType.equals(today.toString());
        }
        return true;
    }

}