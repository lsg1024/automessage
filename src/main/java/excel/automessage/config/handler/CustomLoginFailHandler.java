package excel.automessage.config.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomLoginFailHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = null;

         if (exception instanceof UsernameNotFoundException) {
            // 등록되지 않은 사용자
            errorMessage = "등록되지 않은 사용자입니다.";
        } else if (exception instanceof DisabledException) {
            // 비활성화된 사용자
            errorMessage = "계정이 비활성화되었습니다.";
        } else if (exception instanceof LockedException) {
            // 잠긴 계정
            errorMessage = "계정이 잠겨있습니다.";
        } else {
            // 기타 예외
            errorMessage = "로그인에 실패했습니다. 관리자에게 문의하세요.";
        }


        request.getSession().setAttribute("errorMessage",errorMessage);

        log.info("loginFailHandler = {}", errorMessage);

        response.sendRedirect("/login?error");
    }
}
