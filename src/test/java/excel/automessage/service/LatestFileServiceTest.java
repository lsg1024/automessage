package excel.automessage.service;

import excel.automessage.service.message.util.LatestFileService;
import excel.automessage.service.redis.ExcelRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class LatestFileServiceTest {

    @Mock
    private ExcelRedisService excelRedisService;

    private LatestFileService latestFileService;

    @BeforeEach
    void setUp() {
        latestFileService = new LatestFileService(excelRedisService);
        ReflectionTestUtils.setField(latestFileService, "FILE_PATH", "./test-files/");
    }

    @Test
    @DisplayName("Redis 응답이 success면 true를 반환한다")
    void whenRedisSuccess_thenReturnTrue() {
        given(excelRedisService.getTodayFileStatus()).willReturn("success");

        boolean result = latestFileService.messageAutoLoad();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 실패 + 파일 없음이면 false를 반환한다")
    void whenRedisFailAndNoFile_thenReturnFalse() {
        given(excelRedisService.getTodayFileStatus()).willReturn("fail");

        File file = new File("./test-files/판매관리.xls");
        if (file.exists()) file.delete();

        boolean result = latestFileService.messageAutoLoad();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("파일이 존재하면 true를 반환한다")
    void whenFileExists_thenReturnTrue() throws IOException {
        // given
        given(excelRedisService.getTodayFileStatus()).willReturn("fail");

        File testFile = new File("./test-files/판매관리.xls");
        testFile.getParentFile().mkdirs();
        testFile.createNewFile();

        boolean result = latestFileService.messageAutoLoad();

        assertThat(result).isTrue();

        testFile.delete(); // 테스트 후 정리
    }
}
