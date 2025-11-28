package excel.automessage.excel.util;

import excel.automessage.service.redis.ExcelRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class LatestFileService {

    @Value("${FILE_PATH}")
    private String FILE_PATH;
    private final ExcelRedisService excelRedisService;

    private File getFileFromPath(String fileName) {
        Path filePath = Path.of(FILE_PATH, fileName);
        return filePath.toFile();
    }

    public boolean messageAutoLoad() {
        String response = excelRedisService.getTodayMessageFileStatus();

        File file = getFileFromPath("판매관리.xls");

        if (!file.exists()) {
            log.warn("메시지 파일 (판매관리.xls)을 찾을 수 없음. 확인 경로: {}", file.getAbsolutePath());
        }

        return "success".equals(response) || file.exists();
    }

    public MultipartFile getExcelFileAsMultipart() {
        File file = getFileFromPath("판매관리.xls");

        if (exception(file)) return null;

        return new CustomMultipartFile(file);
    }

    public boolean autoOrderListLoad() {
        String response = excelRedisService.getTodayOrderFileStatus();

        File file = getFileFromPath("주문리스트.xls");

        if (!file.exists()) {
            log.warn("주문 파일 (주문리스트.xls)을 찾을 수 없음. 확인 경로: {}", file.getAbsolutePath());
        }

        return "success".equals(response) || file.exists();
    }

    public MultipartFile getExcelFileAsMultipartOrderList() {

        File file = getFileFromPath("주문리스트.xls");

        if (exception(file)) return null;

        return new CustomMultipartFile(file);
    }

    private boolean exception(File file) {
        if (!file.exists()) {
            log.error("파일을 찾을 수 없음. 예상 경로: {}", file.getAbsolutePath());
            return true;
        }
        return false;
    }

}