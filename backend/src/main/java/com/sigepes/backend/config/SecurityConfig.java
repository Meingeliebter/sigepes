package com.sigepes.backend.config;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.security.JwtAuthenticationFilter;
import com.sigepes.backend.security.RateLimitFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String[] allowedOrigins;

    public SecurityConfig(@Value("${sigepes.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }

    /** BCrypt: nunca se almacenan contraseñas en claro. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            RateLimitFilter rateLimitFilter) throws Exception {
        http.cors(Customizer.withDefaults())
                // API sin estado con token Bearer: no hay cookie de sesión que proteger con CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El canal de alertas (SSE) se autentica en la petición inicial; los despachos
                        // asíncronos y de error posteriores no traen el token de nuevo.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // RF-10: cualquier ciudadano puede denunciar y consultar su denuncia sin cuenta.
                        .requestMatchers(HttpMethod.POST, "/api/v1/denuncias").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/denuncias/seguimiento/*").permitAll()
                        // RF-09: cualquier comprador verifica el QR de un lote sin cuenta.
                        .requestMatchers(HttpMethod.GET, "/api/v1/trazabilidad/*").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> write(response,
                                ErrorCode.UNAUTHORIZED, request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> write(response, ErrorCode.FORBIDDEN,
                                request.getRequestURI())))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /** Error de seguridad con el mismo cuerpo que el resto de la API. */
    private static void write(HttpServletResponse response, ErrorCode code, String path) throws java.io.IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"timestamp":"%s","status":%d,"path":"%s","code":"%s","message":"%s",\
                "error":{"code":"%s","message":"%s"}}"""
                .formatted(Instant.now(), code.status().value(), path, code.name(), code.defaultMessage(),
                        code.name(), code.defaultMessage()));
    }
}
