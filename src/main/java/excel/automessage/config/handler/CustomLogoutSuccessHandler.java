package excel.automessage.config.handler;

import excel.automessage.service.RedisTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final RedisTokenService redisTokenService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        log.info("onLogoutSuccess");
        if (authentication != null) {

            log.info("memberId {}", authentication.getName());
            PersistentRememberMeToken token = (PersistentRememberMeToken) authentication.getDetails();

            if (token != null) {
                // redis 키 삭제 단일 객체
                redisTokenService.removeToken(token.getSeries());
            }

        }
        response.sendRedirect("/login");
    }
}
