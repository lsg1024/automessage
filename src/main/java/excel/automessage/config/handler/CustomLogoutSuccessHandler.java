package excel.automessage.config.handler;

import excel.automessage.service.redis.RedisTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final RedisTokenService redisTokenService;

    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        log.info("onLogoutSuccess");
        if (authentication != null) {
            // 인증 정보로부터 사용자 이름을 가져옴
            String username = authentication.getName();
            log.info("memberId {}", username);

            // 사용자 이름을 기반으로 Redis에서 토큰 삭제
            redisTokenService.removeUserTokens(username);
        }

        response.sendRedirect("/login");
    }
}
