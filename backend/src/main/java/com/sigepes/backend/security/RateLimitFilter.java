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
 * Limitación de intentos en los endpoints sensibles: autenticación y los dos endpoints públicos de
 * denuncias (radicar, contra el spam; consultar por código, contra la adivinación de códigos).
 *
 * <p>Ventana fija de un minuto por IP, en memoria. La IP solo vive en este mapa durante la ventana:
 * nunca se escribe en la base de datos ni en la auditoría, lo que preserva el anonimato de las
 * denuncias (AC-10.5). Suficiente para el MVP y sin dependencias
 * adicionales; en un despliegue con varias instancias habría que moverlo a un almacén compartido.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String REPORTS_PATH = "/api/v1/denuncias";
    private static final String TRACKING_PREFIX = "/api/v1/denuncias/seguimiento/";
    private static final String LOT_PREFIX = "/api/v1/trazabilidad/";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttemptsPerMinute;
    private final int maxPublicRequestsPerMinute;

    public RateLimitFilter(@Value("${sigepes.security.login-attempts-per-minute:10}") int maxAttemptsPerMinute,
            @Value("${sigepes.security.public-requests-per-minute:20}") int maxPublicRequestsPerMinute) {
        this.maxAttemptsPerMinute = maxAttemptsPerMinute;
        this.maxPublicRequestsPerMinute = maxPublicRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return bucket(request) == null;
    }

    /** Nombre del contador al que pertenece la petición, o null si no se limita. */
    private static String bucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)
                && (LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path) || REPORTS_PATH.equals(path))) {
            return path;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith(TRACKING_PREFIX)) {
            return TRACKING_PREFIX;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith(LOT_PREFIX)) {
            return LOT_PREFIX;
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucket(request);
        boolean publicEndpoint = bucket.startsWith(REPORTS_PATH) || bucket.equals(LOT_PREFIX);
        int limit = publicEndpoint ? maxPublicRequestsPerMinute : maxAttemptsPerMinute;
        String key = bucket + "|" + request.getRemoteAddr();
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key,
                (ignored, current) -> current == null || current.minute != minute
                        ? new Window(minute)
                        : current);

        if (window.counter.incrementAndGet() > limit) {
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
