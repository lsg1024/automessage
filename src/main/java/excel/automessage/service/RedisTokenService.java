package excel.automessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import excel.automessage.util.PersistentRememberMeTokenDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisTokenService implements PersistentTokenRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String REMEMBER_MY_KEY = "rememberMe:token:";

    public RedisTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PersistentRememberMeToken.class, new PersistentRememberMeTokenDeserializer());
        this.objectMapper.registerModule(module);
    }

    private String getRedisKey(String series) {
        return REMEMBER_MY_KEY + series;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        String redisKey = getRedisKey(token.getSeries());
        log.info("createKey redisKey {}", redisKey);
        log.info("createKey token {}", token.getTokenValue());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String tokenJson = objectMapper.writeValueAsString(token);
            log.info("createKey tokenJson = {}", tokenJson);
            redisTemplate.opsForValue().set(redisKey, tokenJson, 3, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize token to JSON", e);
        }
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        PersistentRememberMeToken token = getTokenForSeries(series);
        if (token != null) {
            PersistentRememberMeToken newToken = new PersistentRememberMeToken(
                    token.getUsername(), series, tokenValue, lastUsed
            );
            createNewToken(newToken);
        }

    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        String redisKey = getRedisKey(seriesId);
        String tokenJson = (String) redisTemplate.opsForValue().get(redisKey);

        log.info("getTokenForSeries {} = {}", redisKey, tokenJson);

        try {
            return objectMapper.readValue(tokenJson, PersistentRememberMeToken.class);
        } catch (Exception e) {
            throw new RuntimeException("getTokenForSeries 역직렬화 실패", e);
        }
    }


    @Override
    public void removeUserTokens(String username) {
        Set<String> keys = redisTemplate.keys(REMEMBER_MY_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                log.info("key value {}", key);
                String tokenJson = (String) redisTemplate.opsForValue().get(key);

                if (tokenJson != null) {
                    try {
                        PersistentRememberMeToken token = objectMapper.readValue(tokenJson, PersistentRememberMeToken.class);
                        if (token.getUsername().equals(username)) {
                            redisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("removeUserTokens 역직렬화 실패", e);
                    }
                }
            }
        }
    }
}
