package excel.automessage.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/", "/login", "/signup", "/register", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/automessage/**").authenticated()
                        .anyRequest().authenticated());

        http.exceptionHandling((except) -> except
                .accessDeniedPage("/login"));

        http.formLogin((auth) -> auth.loginPage("/login")
                .defaultSuccessUrl("/automessage", true)
//                .loginProcessingUrl("/loginProc")
                .permitAll());

        http.rememberMe((remember) -> remember
                .rememberMeParameter("remember")
                .tokenValiditySeconds(84000 * 2));

        return http.build();
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
