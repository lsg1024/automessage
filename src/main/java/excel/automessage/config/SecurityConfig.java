package excel.automessage.config;

import excel.automessage.config.handler.*;
import excel.automessage.service.member.CustomMemberDetailService;
import excel.automessage.service.redis.RedisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/automessage/admin").hasRole("ADMIN")
                        .requestMatchers("/automessage/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated());

        http.exceptionHandling((except) -> except
                .accessDeniedPage("/login"));

        http.exceptionHandling((e) -> e
                .accessDeniedHandler(customAccessDeniedHandler));

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

        @Bean
        @Order(0) // 내부망 모니터링
        public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {

            http.securityMatcher("/metrics/**");

            http.
                    authorizeHttpRequests((auth) -> auth
                            .requestMatchers("/metrics/**").permitAll()
                            .anyRequest().denyAll());

            http.
                    addFilterBefore(new InternalNetworkFilter(), BasicAuthenticationFilter.class);

            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> {})
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

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
