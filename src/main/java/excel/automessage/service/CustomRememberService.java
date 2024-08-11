package excel.automessage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Slf4j
public class CustomRememberService extends PersistentTokenBasedRememberMeServices {

    public CustomRememberService(String key, UserDetailsService userDetailsService, PersistentTokenRepository tokenRepository) {
        super(key, userDetailsService, tokenRepository);
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        UserDetails userDetails = (UserDetails) successfulAuthentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        log.info("유저 권한 {}", role);
        // WAIT 권한인 경우 remember-me 토큰을 생성하지 않음
        if (!"ROLE_WAIT".equals(role)) {
            super.onLoginSuccess(request, response, successfulAuthentication);
        }
    }
}

