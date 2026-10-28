package ru.rbpo.museum.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * HTTP Basic + CSRF (double submit cookie): токен выдаётся в cookie XSRF-TOKEN и через GET /api/auth/csrf,
 * а запросы, изменяющие данные (POST/PATCH/PUT/DELETE), должны передавать его в заголовке X-XSRF-TOKEN.
 * Сессии не создаются: пользователь аутентифицируется в каждом запросе.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setHeaderName(CSRF_HEADER);
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").path("/"));

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        // Токен передаётся как есть (без XOR-маскирования), чтобы REST-клиенты могли взять его из cookie.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // По умолчанию токен сбрасывается при каждой успешной аутентификации (защита от фиксации
                        // для входа с сессией). При HTTP Basic аутентификация происходит в каждом запросе, и токен
                        // становился бы недействительным сразу после первого запроса. Сессий нет, поэтому сброс не нужен.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/users/**").hasAuthority("USER_MANAGE")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    /** 401: учётные данные не переданы или неверны. Причину (нет пользователя, заблокирован и т.п.) не раскрываем. */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"museum\"");
            ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Требуется аутентификация: учётные данные отсутствуют, неверны или учётная запись недоступна");
        };
    }

    /** 403: отдельно сообщаем об ошибке CSRF и о недостатке прав. */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            if (exception instanceof CsrfException) {
                ProblemResponses.write(response, HttpStatus.FORBIDDEN, "CSRF_TOKEN_INVALID",
                        "CSRF-токен отсутствует или недействителен; получите его через GET /api/auth/csrf");
            } else {
                ProblemResponses.write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "Недостаточно прав для выполнения операции");
            }
        };
    }
}
