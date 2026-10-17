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
}
