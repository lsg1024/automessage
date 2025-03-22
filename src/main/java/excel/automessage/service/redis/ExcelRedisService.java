package excel.automessage.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExcelRedisService {

    private final StringRedisTemplate redisTemplate;

    public String getTodayFileStatus() {
        String key = "todayFile";
        return redisTemplate.opsForValue().get(key);
    }

}
