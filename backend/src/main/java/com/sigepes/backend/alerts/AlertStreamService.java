package com.sigepes.backend.alerts;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AC-11.6 · Canal en vivo (Server-Sent Events) para que una alerta crítica llegue en segundos.
 *
 * <p>Solo es un acelerador: si el canal cae, el navegador vuelve a consultar el buzón, que es la
 * fuente de verdad. Las conexiones viven en memoria de esta instancia; con varias instancias haría
 * falta un bus compartido (p. ej. LISTEN/NOTIFY de PostgreSQL).
 */
@Service
public class AlertStreamService {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final int MAX_STREAMS_PER_USER = 5;

    private final Map<String, List<SseEmitter>> streams = new ConcurrentHashMap<>();

    public SseEmitter open(String userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> own = streams.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());
        // Una pestaña olvidada no debe acaparar conexiones: se cierra la más antigua.
        while (own.size() >= MAX_STREAMS_PER_USER) {
            own.remove(0).complete();
        }
        own.add(emitter);
        Runnable remove = () -> own.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        send(emitter, SseEmitter.event().name("ready").data("ok"));
        return emitter;
    }

    /** Se envía después del commit: nunca se avisa de una alerta que luego se revierte. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onAlertCreated(AlertCreated event) {
        for (SseEmitter emitter : streams.getOrDefault(event.recipientId(), List.of())) {
            send(emitter, SseEmitter.event().name("alert").data(event.alert(), MediaType.APPLICATION_JSON));
        }
    }

    /** Latido para que proxies y balanceadores no corten la conexión por inactividad. */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        streams.values().forEach(list -> list.forEach(emitter -> send(emitter, SseEmitter.event().comment("ping"))));
    }

    int openStreams(String userId) {
        return streams.getOrDefault(userId, List.of()).size();
    }

    private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }
}
