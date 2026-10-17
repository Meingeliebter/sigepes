package com.sigepes.backend.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.closures.ClosureType;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesStatus;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/**
 * Reglas de negocio del MVP v1.0, centralizadas.
 *
 * <p>Ningún controlador valida por su cuenta: todas las reglas de cumplimiento normativo viven
 * aquí, y cada una devuelve un {@link ErrorCode} concreto que el frontend puede interpretar.
 *
 * <ul>
 *   <li>RN-04 · artes destructivas o prohibidas</li>
 *   <li>RN-06 · cierre de faena con capturas o declaración nula</li>
 *   <li>RN-10 · especie en veda por zona y periodo</li>
 *   <li>RN-12 · talla mínima legal</li>
 *   <li>RN-16 · permiso vigente, salvo subsistencia</li>
 * </ul>
 */
@Service
public class ComplianceService {

    private final PermitRepository permitRepository;
    private final ClosureRepository closureRepository;
    private final Set<String> subsistenceTypes;

    public ComplianceService(PermitRepository permitRepository, ClosureRepository closureRepository,
            @Value("${sigepes.rules.subsistence-permit-types:SUBSISTENCIA}") String subsistenceTypes) {
        this.permitRepository = permitRepository;
        this.closureRepository = closureRepository;
        this.subsistenceTypes = Arrays.stream(subsistenceTypes.split(","))
                .map(type -> type.trim().toUpperCase(Locale.ROOT))
                .filter(type -> !type.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * RN-16 · El titular debe contar con un permiso aprobado y vigente en la fecha de la faena.
     *
     * <p>Excepción de subsistencia: un permiso aprobado cuyo tipo figure en
     * {@code sigepes.rules.subsistence-permit-types} habilita la operación aunque su vigencia haya
     * terminado, porque la pesca de subsistencia no está sujeta a la misma caducidad.
     *
     * @return el permiso que habilita la operación, para dejarlo enlazado en la faena
     */
    @Transactional(readOnly = true)
    public PermitEntity requireValidPermit(String ownerId, LocalDate date) {
        List<PermitEntity> valid = permitRepository.findValidFor(ownerId, date);
        if (!valid.isEmpty()) {
            return valid.get(0);
        }

        List<PermitEntity> allOfOwner = permitRepository.findByOwnerIdOrderByExpiresAtDesc(ownerId);

        Optional<PermitEntity> subsistence = allOfOwner.stream()
                .filter(permit -> permit.getStatus() == PermitStatus.APPROVED)
                .filter(this::isSubsistence)
                .findFirst();
        if (subsistence.isPresent()) {
            return subsistence.get();
        }

        // Sin permiso habilitante: se elige el error más informativo según lo que sí tenga.
        boolean expired = allOfOwner.stream()
                .anyMatch(permit -> permit.getStatus() == PermitStatus.APPROVED && permit.isExpiredOn(date));
        if (expired) {
            throw ApiException.of(ErrorCode.PERMIT_EXPIRED);
        }
        if (allOfOwner.stream().anyMatch(permit -> permit.getStatus() == PermitStatus.SUSPENDED)) {
            throw ApiException.of(ErrorCode.PERMIT_SUSPENDED);
        }
        if (allOfOwner.stream().anyMatch(permit -> permit.getStatus() == PermitStatus.PENDING)) {
            throw ApiException.of(ErrorCode.PERMIT_NOT_APPROVED,
                    "Tu solicitud de permiso todavía está pendiente de aprobación.");
        }
        throw ApiException.of(ErrorCode.PERMIT_REQUIRED);
    }

    /** La embarcación debe estar activa y pertenecer a quien registra la faena. */
    public void requireUsableVessel(VesselEntity vessel, String ownerId) {
        if (!vessel.getOwner().getId().equals(ownerId)) {
            throw ApiException.of(ErrorCode.VESSEL_NOT_OWNED);
        }
        if (vessel.getStatus() != VesselStatus.ACTIVE) {
            throw ApiException.of(ErrorCode.VESSEL_NOT_AVAILABLE,
                    "La embarcación «" + vessel.getName() + "» no está activa ("
                            + vessel.getStatus().name().toLowerCase(Locale.ROOT) + ").");
        }
    }

    /** RN-04 · El arte no puede ser destructiva ni estar bloqueada. */
    public void requireAllowedGear(FishingGearEntity gear) {
        if (!gear.isUsable()) {
            throw ApiException.of(ErrorCode.PROHIBITED_GEAR,
                    "El arte «" + gear.getName() + "» está clasificada como destructiva o prohibida.");
        }
    }

    /** La especie debe existir en el catálogo y no estar retirada. */
    public void requireActiveSpecies(SpeciesEntity species) {
        if (species.getStatus() == SpeciesStatus.INACTIVE) {
            throw ApiException.of(ErrorCode.SPECIES_INACTIVE,
                    "La especie «" + species.getCommonName() + "» no está habilitada en el catálogo.");
        }
    }

    /** RN-12 · La talla promedio no puede estar por debajo del mínimo legal. */
    public void requireMinimumSize(SpeciesEntity species, BigDecimal averageSizeCm) {
        if (averageSizeCm == null || averageSizeCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "La talla promedio debe ser mayor que cero.");
        }
        if (averageSizeCm.compareTo(species.getMinSizeCm()) < 0) {
            throw ApiException.of(ErrorCode.MINIMUM_SIZE_VIOLATION,
                    "La talla registrada (" + averageSizeCm + " cm) está por debajo del mínimo legal de «"
                            + species.getCommonName() + "» (" + species.getMinSizeCm() + " cm).");
        }
    }

    /**
     * RN-10 · No se admite captura de una especie en veda para esa zona y fecha.
     *
     * <p>AC-05.5: una veda {@code PARTIAL} deja pasar a quien opera al amparo de un permiso de
     * subsistencia (mismo catálogo de tipos que ya exime la vigencia del permiso en
     * {@link #requireValidPermit}); una veda {@code TOTAL} bloquea siempre, sin excepción.
     */
    @Transactional(readOnly = true)
    public void requireNoClosure(SpeciesEntity species, LocalDate date, String zone, PermitEntity permit) {
        Optional<ClosureEntity> blocking = closureRepository.findInForceForSpecies(species.getId(), date).stream()
                .filter(closure -> closure.coversZone(zone))
                .findFirst();
        if (blocking.isEmpty()) {
            return;
        }
        ClosureEntity closure = blocking.get();
        if (closure.getClosureType() == ClosureType.PARTIAL && isSubsistence(permit)) {
            return;
        }
        throw ApiException.of(ErrorCode.SPECIES_UNDER_CLOSURE,
                "«" + species.getCommonName() + "» está en veda " + closure.getClosureType().name().toLowerCase(Locale.ROOT)
                        + " en " + closure.getZone() + " del " + closure.getStartDate() + " al "
                        + closure.getEndDate() + " (Resolución " + closure.getResolutionNumber() + ").");
    }

    private boolean isSubsistence(PermitEntity permit) {
        return permit != null && subsistenceTypes.contains(permit.getType().toUpperCase(Locale.ROOT));
    }

    /**
     * RN-06 · Una faena solo puede cerrarse con al menos una captura registrada o con declaración
     * explícita de captura nula. Nunca las dos cosas a la vez.
     */
    public void requireClosable(int catchCount, boolean nullDeclaration) {
        if (nullDeclaration && catchCount > 0) {
            throw ApiException.of(ErrorCode.NULL_DECLARATION_WITH_CATCHES);
        }
        if (!nullDeclaration && catchCount == 0) {
            throw ApiException.of(ErrorCode.TRIP_CANNOT_CLOSE);
        }
    }
}
