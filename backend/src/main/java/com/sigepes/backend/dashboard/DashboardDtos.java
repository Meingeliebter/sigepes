package com.sigepes.backend.dashboard;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Tarjeta de indicador: valor grande + texto de apoyo, como las pinta la pantalla de inicio. */
    public record StatResponse(long value, String helper) {
    }

    /** Fila de "Actividad reciente": título, estado para el chip y fecha legible. */
    public record ActivityResponse(String id, String title, String status, String date) {
    }

    /** Fila del bloque "Cumplimiento normativo". */
    public record ComplianceResponse(String label, String status) {
    }

    public record DashboardResponse(
            String fullName,
            List<String> alerts,
            StatResponse vessels,
            StatResponse permits,
            StatResponse trips,
            StatResponse warnings,
            List<ActivityResponse> recent,
            List<ComplianceResponse> compliance) {
    }

    /**
     * Notificación derivada de datos reales del MVP (vedas vigentes y permisos por vencer).
     *
     * <p>{@code type} permite a la interfaz elegir el icono y el color que ya tiene definidos.
     * {@code severity} (AC-08.2) distingue qué tan urgente es (p. ej. un permiso a 5 días de vencer
     * es {@code CRITICAL}, a 15 días {@code WARNING}, a 30 días {@code INFO}).
     */
    public record NotificationResponse(
            String id,
            String type,
            String title,
            String body,
            String date,
            String severity) {
    }
}
