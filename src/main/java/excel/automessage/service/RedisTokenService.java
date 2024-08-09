package excel.automessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisTokenService implements PersistentTokenRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REMEMBER_MY_KEY = "rememberMe:token:";

    public RedisTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getRedisKey(String series) {
        return REMEMBER_MY_KEY + series;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        String redisKey = getRedisKey(token.getSeries());
        redisTemplate.opsForValue().set(redisKey, token, 3, TimeUnit.DAYS);
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
        return (PersistentRememberMeToken) redisTemplate.opsForValue().get(redisKey);
    }

    @Override
    public void removeUserTokens(String username) {
        Set<String> keys = redisTemplate.keys(REMEMBER_MY_KEY + "*");
        if (keys != null) {
            for (String key : keys) {

                Object tokenObject = redisTemplate.opsForValue().get(key);
                log.info("tokenObject {}", tokenObject);

                if (tokenObject != null) {
                    try {
                        PersistentRememberMeToken token = objectMapper.convertValue(tokenObject, PersistentRememberMeToken.class);
                        if (token.getUsername().equals(username)) {
                            redisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize token", e);
                    }
                }
            }
        }
    }


    public void removeToken(String series) {
        String redisKey = getRedisKey(series);
        redisTemplate.delete(redisKey);
    }
}
