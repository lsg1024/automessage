package excel.automessage.service.message.util;

import excel.automessage.service.redis.ExcelRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class LatestFileService {

    @Value("${FILE_PATH}")
    private String FILE_PATH;
    private final ExcelRedisService excelRedisService;

    public boolean messageAutoLoad() {
        String response = excelRedisService.getTodayFileStatus();

        File file = new File(FILE_PATH + "판매관리.xls");

        return "success".equals(response) || file.exists();
    }

    public MultipartFile getExcelFileAsMultipart() {
        File file = new File(FILE_PATH + "판매관리.xls");

        if (!file.exists()) {
            log.error("file.exists");
            return null;
        }

        return new CustomMultipartFile(file);
    }

}
