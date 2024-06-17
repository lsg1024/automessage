package excel.automessage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Slf4j
public class StoreServiceTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private StoreListDTO storeListDTO;

    @BeforeEach
    public void setup() {
        storeListDTO = new StoreListDTO();
        List<StoreDTO.Save> storeDTOList = new ArrayList<>();

        for (int i = 0; i < 900; i++) {  // 테스트 데이터 생성
            StoreDTO.Save storeDTO = new StoreDTO.Save();
            storeDTO.setName("Store" + i);
            storeDTO.setPhone("010-1234-" + String.format("%04d", i));
            storeDTOList.add(storeDTO);
        }

        storeListDTO.setStores(storeDTOList);
    }

    @Test
    @Transactional
    @DisplayName("DB 저장")
    public void testSaveAllToDatabase() {
        long startTime = System.currentTimeMillis();

        storeService.saveAll(storeListDTO);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("DB 저장 시간: {}ms", duration);
    }

    @Test
    @Transactional
    @DisplayName("Redis 저장")
    public void testSaveAllToRedisAndDatabaseAsync() {
        long redisStartTime = System.currentTimeMillis();

        String key = "testStoresKey";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String serializedData = objectMapper.writeValueAsString(storeListDTO);
            redisTemplate.opsForValue().set(key, serializedData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize StoreListDTO to JSON", e);
        }

        long redisEndTime = System.currentTimeMillis();
        long redisDuration = redisEndTime - redisStartTime;
        log.info("Redis 저장 시간: {}ms", redisDuration);

        // Redis에 저장된 데이터를 DB에 저장하는 비동기 처리 실행
        long dbStartTime = System.currentTimeMillis();
        CompletableFuture<Void> future = storeService.saveAllToDBAsync(key);

        // 비동기 작업 중 Redis 데이터에 접근
        String redisDataDuringDBSave = (String) redisTemplate.opsForValue().get(key);
        assertNotNull(redisDataDuringDBSave, "Redis 데이터에 접근할 수 있어야 합니다.");

        try {
            // DB 저장이 완료될 때까지 대기 5분
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to save data to DB", e);
        }

        long dbEndTime = System.currentTimeMillis();
        long dbDuration = dbEndTime - dbStartTime;
        log.info("DB 저장 시간: {}ms", dbDuration);

        // 전체 실행 시간을 계산하여 로그에 출력
        long totalTime = redisDuration + dbDuration;
        log.info("전체 실행 시간: {}ms", totalTime);

        // Redis 데이터가 삭제되었는지 확인
        String redisDataAfterDBSave = (String) redisTemplate.opsForValue().get(key);
        assertNull(redisDataAfterDBSave, "DB 저장 후 Redis 데이터는 삭제되어야 합니다.");
    }
}
