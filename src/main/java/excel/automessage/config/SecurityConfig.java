package excel.automessage.config;

import excel.automessage.config.handler.CustomLoginFailHandler;
import excel.automessage.config.handler.CustomLoginSuccessHandler;
import excel.automessage.config.handler.CustomLogoutSuccessHandler;
import excel.automessage.service.CustomMemberDetailService;
import excel.automessage.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomLoginFailHandler customLoginFailHandler;
    private final CustomMemberDetailService customMemberDetailService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/").hasRole("WAIT")
                        .requestMatchers("/automessage/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated());

        http.exceptionHandling((except) -> except
                .accessDeniedPage("/login"));

        http.formLogin((auth) -> auth
                .loginPage("/login")
                .usernameParameter("memberId")
                .passwordParameter("memberPassword")
                .loginProcessingUrl("/login")
                .successHandler(customLoginSuccessHandler)
                .failureHandler(customLoginFailHandler)
                .permitAll());

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

        http.rememberMe((remember) -> remember
                .rememberMeParameter("remember")
                .tokenValiditySeconds(3 * 24 * 60 * 60) // 3일 동안 유효한 쿠키
                .tokenRepository(persistentTokenRepository())
                .userDetailsService(customMemberDetailService));

        return http.build();
    }


    // Redis
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        return new RedisTokenService(redisTemplate);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
