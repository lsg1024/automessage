package excel.automessage.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    public boolean isDuplicateRequest(String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IDEMPOTENCY_KEY_PREFIX + idempotencyKey));
    }

    public void saveIdempotencyKey(String idempotencyKey) {
        // 5분(300초) 동안 유효하도록 설정
        redisTemplate.opsForValue().set(IDEMPOTENCY_KEY_PREFIX + idempotencyKey, "true", 300, TimeUnit.SECONDS);
    }

}
