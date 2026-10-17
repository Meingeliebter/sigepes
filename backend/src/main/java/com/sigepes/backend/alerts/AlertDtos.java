package com.sigepes.backend.alerts;

import java.time.Instant;
import java.util.List;

public final class AlertDtos {

    private AlertDtos() {
    }

    public record AlertResponse(
            String id,
            String type,
            String severity,
            String title,
            String message,
            String link,
            boolean read,
            /** RN-22: las críticas no se pueden archivar; la interfaz oculta esa acción. */
            boolean archivable,
            Instant createdAt) {

        public static AlertResponse from(AlertEntity alert) {
            return new AlertResponse(alert.getId(), alert.getAlertType().name(), alert.getSeverity().name(),
                    alert.getTitle(), alert.getMessage(), alert.getLink(), alert.getReadAt() != null,
                    alert.getSeverity() != AlertSeverity.CRITICAL, alert.getCreatedAt());
        }
    }

    /** Buzón: alertas visibles y número de no leídas para la campana (AC-11.2). */
    public record AlertInbox(long unread, List<AlertResponse> alerts) {
    }

    public record UnreadCount(long unread) {
    }
}
