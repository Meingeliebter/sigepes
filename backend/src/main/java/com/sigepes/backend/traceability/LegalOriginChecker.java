package com.sigepes.backend.traceability;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.quotas.QuotaRepository;
import com.sigepes.backend.rules.ComplianceService;
import com.sigepes.backend.traceability.TraceabilityDtos.LegalCheck;
import com.sigepes.backend.trips.CatchEntity;
import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.trips.TripStatus;

/**
 * RN-18 · Puerta de origen legal: una captura solo se convierte en lote si la faena está cerrada y
 * validada, se pescó con permiso vigente, sobre la talla mínima, fuera de veda y dentro de cuota.
 *
 * <p>Las reglas se vuelven a evaluar contra el estado actual (p. ej. una veda declarada después con
 * efecto sobre esa fecha), no solo se confía en lo que se validó al registrar la captura.
 */
@Component
class LegalOriginChecker {

    private final ComplianceService compliance;
    private final QuotaRepository quotaRepository;

    LegalOriginChecker(ComplianceService compliance, QuotaRepository quotaRepository) {
        this.compliance = compliance;
        this.quotaRepository = quotaRepository;
    }

    List<LegalCheck> check(CatchEntity catchEntry) {
        FishingTripEntity trip = catchEntry.getTrip();
        LocalDate date = trip.getTripDate();
        List<LegalCheck> checks = new ArrayList<>();

        boolean closed = trip.getStatus() == TripStatus.CLOSED;
        checks.add(new LegalCheck("trip", "Faena cerrada y validada", closed,
                closed ? "Faena del " + date : "La faena aún no se ha cerrado."));

        PermitEntity permit = trip.getPermit();
        boolean permitOk = permit != null && permit.getStatus() == PermitStatus.APPROVED
                && !date.isBefore(permit.getIssuedAt()) && !date.isAfter(permit.getExpiresAt());
        checks.add(new LegalCheck("permit", "Permiso de pesca vigente", permitOk,
                permitOk ? "Permiso " + permit.getNumber() + " vigente el día de la faena."
                        : "No había un permiso aprobado y vigente el día de la faena."));

        var minimum = catchEntry.getMinSizeCmAtCapture();
        var size = catchEntry.getAverageSizeCm();
        boolean sizeOk = minimum == null || (size != null && size.compareTo(minimum) >= 0);
        checks.add(new LegalCheck("size", "Talla sobre la mínima legal", sizeOk,
                minimum == null ? "La especie no tiene talla mínima."
                        : size + " cm frente a un mínimo de " + minimum + " cm."));

        String closureDetail = "Sin veda para la especie, la zona y la fecha.";
        boolean closureOk = true;
        try {
            compliance.requireNoClosure(catchEntry.getSpecies(), date, trip.getZone(), permit);
        } catch (ApiException e) {
            closureOk = false;
            closureDetail = e.getMessage();
        }
        checks.add(new LegalCheck("closure", "Fuera de periodo de veda", closureOk, closureDetail));

        boolean quotaApplies = quotaRepository.findActiveFor(catchEntry.getSpecies().getId(), date).stream()
                .anyMatch(quota -> quota.appliesTo(catchEntry.getSpecies().getId(), trip.getZone(), date,
                        trip.getVessel().getId()));
        boolean quotaOk = !quotaApplies || catchEntry.getQuotaId() != null;
        checks.add(new LegalCheck("quota", "Dentro de la cuota", quotaOk,
                !quotaApplies ? "No hay cuota aplicable a esta captura."
                        : quotaOk ? "Descontada de la cuota vigente." : "La captura no se descontó de la cuota."));
        return checks;
    }
}
