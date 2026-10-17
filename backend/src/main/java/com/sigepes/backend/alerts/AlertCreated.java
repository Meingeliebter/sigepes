package com.sigepes.backend.alerts;

import com.sigepes.backend.alerts.AlertDtos.AlertResponse;

/**
 * Una alerta quedó guardada. El canal en vivo la envía al destinatario solo después del commit,
 * de modo que nunca se notifica algo que luego se revierte (AC-11.5).
 */
public record AlertCreated(String recipientId, AlertResponse alert) {
}
