package com.sigepes.backend.reports;

/** AC-10.4 · Flujo de inspección: Recibida → En investigación → Cerrada (fundada / no fundada). */
public enum ReportStatus {
    RECEIVED,
    INVESTIGATING,
    CLOSED_FOUNDED,
    CLOSED_UNFOUNDED;

    public boolean isClosed() {
        return this == CLOSED_FOUNDED || this == CLOSED_UNFOUNDED;
    }

    public boolean canMoveTo(ReportStatus next) {
        return switch (this) {
            case RECEIVED -> next == INVESTIGATING;
            case INVESTIGATING -> next.isClosed();
            case CLOSED_FOUNDED, CLOSED_UNFOUNDED -> false;
        };
    }
}
