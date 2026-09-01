package com.pecuni.config;

import tools.jackson.databind.ObjectMapper;
import com.pecuni.common.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Stateless JWT security chain. Passwords are hashed with BCrypt (RNF01), never stored/logged in plain text. */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**",
        "/docs/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
        "/actuator/health", "/actuator/info"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(this::handleAuthenticationFailure)
                        .accessDeniedHandler(this::handleAccessDenied))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws IOException {
        writeProblemDetail(response, ProblemDetails.of(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado",
                "Token de acesso ausente, expirado ou inválido.", request.getRequestURI()));
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws IOException {
        writeProblemDetail(response, ProblemDetails.of(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado",
                "Você não tem permissão para acessar este recurso.", request.getRequestURI()));
    }

    private void writeProblemDetail(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
