package com.sigepes.backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sigepes.backend.common.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limitación de intentos en los endpoints sensibles de autenticación.
 *
 * <p>Ventana fija de un minuto por IP, en memoria. Suficiente para el MVP y sin dependencias
 * adicionales; en un despliegue con varias instancias habría que moverlo a un almacén compartido.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttemptsPerMinute;

    public RateLimitFilter(@Value("${sigepes.security.login-attempts-per-minute:10}") int maxAttemptsPerMinute) {
        this.maxAttemptsPerMinute = maxAttemptsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean sensitive = LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path);
        return !sensitive || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getRequestURI() + "|" + request.getRemoteAddr();
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key,
                (ignored, current) -> current == null || current.minute != minute
                        ? new Window(minute)
                        : current);

        if (window.counter.incrementAndGet() > maxAttemptsPerMinute) {
            reject(response, request.getRequestURI());
            return;
        }
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, String path) throws IOException {
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"timestamp":"%s","status":429,"path":"%s","code":"TOO_MANY_REQUESTS",\
                "message":"%s","error":{"code":"TOO_MANY_REQUESTS","message":"%s"}}"""
                .formatted(Instant.now(), path, ErrorCode.TOO_MANY_REQUESTS.defaultMessage(),
                        ErrorCode.TOO_MANY_REQUESTS.defaultMessage()));
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger counter = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
