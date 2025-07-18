package excel.automessage.excel.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class ExcelSheetUtils {

    public static Workbook getSheets(MultipartFile file) throws IOException {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        Workbook workbook = null;

        if (extension == null) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }

        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (NotOfficeXmlFileException e) {
            workbook = convertHtmlToWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (workbook == null) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }
        return workbook;
    }

    // Html 데이터 테이블 읽어오기
    private static Workbook convertHtmlToWorkbook(MultipartFile htmlFile) throws IOException {
        Document htmlDoc = Jsoup.parse(htmlFile.getInputStream(), "UTF-8", "");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        Element table = htmlDoc.select("table").first();
        if (table != null) {
            Elements rows = table.select("tr");

            int rowIndex = 0;
            for (Element row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                Elements cells = row.select("td, th");
                int cellIndex = 0;
                for (Element cell : cells) {
                    Cell excelCell = excelRow.createCell(cellIndex++);
                    excelCell.setCellValue(cell.text());
                }
            }
        }

        return workbook;
    }

}
