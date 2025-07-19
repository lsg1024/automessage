package excel.automessage.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExcelRedisService {

    private final StringRedisTemplate redisTemplate;

    public String getTodayMessageFileStatus() {
        String key = "todayMessageFile";
        return redisTemplate.opsForValue().get(key);
    }

    public String getTodayOrderFileStatus() {
        String key = "todayOrderFile";
        return redisTemplate.opsForValue().get(key);
    }

}
