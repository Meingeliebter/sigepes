package com.sigepes.backend.audit;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AuditDtos {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private AuditDtos() {
    }

    /**
     * Evento de auditoría tal y como lo consume la tabla de la pantalla "Auditoría".
     *
     * <p>{@code timestamp} va formateado porque la celda lo imprime tal cual; {@code occurredAt}
     * mantiene el ISO-8601 para consumo automático.
     */
    public record AuditEventResponse(
            String id,
            String timestamp,
            String occurredAt,
            String actor,
            String action,
            String module,
            String entity,
            String entityId,
            String detail,
            String ip) {

        public static AuditEventResponse from(AuditLogEntity entity) {
            return new AuditEventResponse(
                    entity.getId(),
                    DISPLAY.format(entity.getOccurredAt().atZone(ZONE)),
                    entity.getOccurredAt().toString(),
                    entity.getActorLabel(),
                    entity.getAction(),
                    entity.getModule(),
                    entity.getEntity(),
                    entity.getEntityId(),
                    entity.getDetail(),
                    entity.getIpAddress());
        }
    }
}
