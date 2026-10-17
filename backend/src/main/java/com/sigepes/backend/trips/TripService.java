package com.sigepes.backend.trips;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.gears.GearService;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.quotas.QuotaService;
import com.sigepes.backend.rules.ComplianceService;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesService;
import com.sigepes.backend.trips.TripDtos.CloseTripRequest;
import com.sigepes.backend.trips.TripDtos.CreateCatchRequest;
import com.sigepes.backend.trips.TripDtos.CreateTripRequest;
import com.sigepes.backend.trips.TripDtos.SyncTripRequest;
import com.sigepes.backend.trips.TripDtos.TripResponse;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselService;
import com.sigepes.backend.common.AppClock;

/** RF-03 · Bitácora electrónica de faenas y capturas. */
@Service
public class TripService {

    private final FishingTripRepository repository;
    private final VesselService vesselService;
    private final SpeciesService speciesService;
    private final GearService gearService;
    private final UserService userService;
    private final ComplianceService compliance;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final QuotaService quotaService;

    public TripService(FishingTripRepository repository, VesselService vesselService, SpeciesService speciesService,
            GearService gearService, UserService userService, ComplianceService compliance, CurrentUser currentUser,
            AuditService auditService, QuotaService quotaService) {
        this.repository = repository;
        this.vesselService = vesselService;
        this.speciesService = speciesService;
        this.gearService = gearService;
        this.userService = userService;
        this.compliance = compliance;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.quotaService = quotaService;
    }

    /**
     * La autoridad ve todas las faenas; un operador solo las suyas.
     *
     * <p>El DTO se construye dentro de la transacción: con {@code open-in-view=false} las
     * asociaciones perezosas (el permiso, por ejemplo) ya no se pueden resolver en el controlador.
     */
    @Transactional(readOnly = true)
    public List<TripResponse> findVisible() {
        AuthenticatedUser user = currentUser.require();
        List<FishingTripEntity> trips = user.isAuthority()
                ? repository.findAllByOrderByTripDateDescCreatedAtDesc()
                : repository.findByOwnerIdOrderByTripDateDescCreatedAtDesc(user.id());
        return trips.stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse detail(String id) {
        return TripResponse.from(byId(id));
    }

    @Transactional(readOnly = true)
    public FishingTripEntity byId(String id) {
        FishingTripEntity trip = repository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.TRIP_NOT_FOUND));
        requireVisibility(trip);
        return trip;
    }

    /**
     * Abre una faena validando, en orden: embarcación, permiso vigente y arte.
     *
     * <p>El permiso que habilita la operación queda enlazado para poder demostrar después con qué
     * título se pescó.
     */
    @Transactional
    public TripResponse start(CreateTripRequest request) {
        AuthenticatedUser actor = currentUser.require();

        String ownerId = request.ownerId() != null && !request.ownerId().isBlank() && actor.isAdmin()
                ? request.ownerId()
                : actor.id();
        UserEntity owner = userService.byId(ownerId);

        String id = Ids.orGenerate(request.id(), "fae");
        if (repository.existsById(id)) {
            throw ApiException.of(ErrorCode.TRIP_ALREADY_REGISTERED);
        }

        LocalDate date = request.date() != null ? request.date() : AppClock.today();
        VesselEntity vessel = vesselService.byId(request.vesselId());

        compliance.requireUsableVessel(vessel, owner.getId());
        PermitEntity permit = compliance.requireValidPermit(owner.getId(), date);

        FishingTripEntity trip = new FishingTripEntity(id, owner, vessel, request.zone().trim(), date);
        trip.setOriginPort(request.originPort().trim());
        trip.setPermit(permit);
        trip.setDepartureTime(request.departureTime());
        trip.setArrivalTime(request.arrivalTime());
        trip.setNotes(request.notes());
        trip.setStatus(TripStatus.IN_PROGRESS);

        if (request.gearId() != null && !request.gearId().isBlank()) {
            FishingGearEntity gear = gearService.resolve(request.gearId());
            compliance.requireAllowedGear(gear);
            trip.setGear(gear);
        }

        FishingTripEntity saved = repository.save(trip);
        auditService.record(AuditActionType.CREATE, "TRIP_STARTED", AuditService.MODULE_TRIPS, "Faena", saved.getId(),
                "Apertura de faena en " + saved.getZone() + " con «" + vessel.getName() + "» al amparo del permiso "
                        + permit.getNumber());
        return TripResponse.from(saved);
    }

    /** Registra una captura aplicando especie, veda y talla mínima. */
    @Transactional
    public TripResponse addCatch(String tripId, CreateCatchRequest request) {
        FishingTripEntity trip = byId(tripId);
        requireOwnership(trip);

        if (!trip.isOpen()) {
            throw ApiException.of(ErrorCode.TRIP_ALREADY_CLOSED, "La faena ya está cerrada y no admite capturas.");
        }
        if (trip.isNullDeclaration()) {
            throw ApiException.of(ErrorCode.NULL_DECLARATION_WITH_CATCHES);
        }

        SpeciesEntity species = speciesService.byId(request.speciesId());
        compliance.requireActiveSpecies(species);
        compliance.requireNoClosure(species, trip.getTripDate(), trip.getZone(), trip.getPermit());
        compliance.requireMinimumSize(species, request.averageSizeCm());
        // RN-14 / AC-07.3: aviso temprano si la captura no cabe en la cuota disponible.
        BigDecimal pendingInTrip = trip.getCatches().stream()
                .filter(existing -> existing.getSpecies().getId().equals(species.getId()))
                .map(CatchEntity::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        quotaService.requireCapacity(species, trip.getZone(), trip.getTripDate(), trip.getVessel().getId(),
                pendingInTrip, request.weightKg());

        CatchEntity entry = new CatchEntity(Ids.orGenerate(request.id(), "cap"), species, request.quantity(),
                request.weightKg(), request.averageSizeCm(), species.getMinSizeCm());
        trip.addCatch(entry);

        FishingTripEntity saved = repository.save(trip);
        auditService.record(AuditActionType.CREATE, "CATCH_ADDED", AuditService.MODULE_TRIPS, "Faena", saved.getId(),
                "Captura de «" + species.getCommonName() + "»: " + request.weightKg() + " kg, talla "
                        + request.averageSizeCm() + " cm");
        return TripResponse.from(saved);
    }

    /** RN-06 · Cierra la faena solo con capturas o con declaración de captura nula. */
    @Transactional
    public TripResponse close(String tripId, CloseTripRequest request) {
        FishingTripEntity trip = byId(tripId);
        requireOwnership(trip);

        if (!trip.isOpen()) {
            throw ApiException.of(ErrorCode.TRIP_ALREADY_CLOSED);
        }

        boolean nullDeclaration = Boolean.TRUE.equals(request.nullDeclaration());
        compliance.requireClosable(trip.getCatches().size(), nullDeclaration);
        if (request.landingLatitude() == null || request.landingLongitude() == null) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "La ubicación GPS de desembarco es obligatoria para cerrar la faena.");
        }

        // AC-07.2: descuento definitivo de cada captura contra su cuota (con bloqueo de la fila). Si
        // alguna ya no cabe, el cierre completo se revierte y la faena sigue abierta.
        for (CatchEntity entry : trip.getCatches()) {
            if (entry.getQuotaId() == null) {
                entry.setQuotaId(quotaService.consume(entry.getSpecies().getId(), trip.getZone(),
                        trip.getTripDate(), trip.getVessel().getId(), trip.getOwner().getId(), entry.getWeightKg()));
            }
        }

        trip.setNullDeclaration(nullDeclaration);
        trip.setStatus(TripStatus.CLOSED);
        trip.setClosedAt(Instant.now());
        trip.setLandingLatitude(request.landingLatitude());
        trip.setLandingLongitude(request.landingLongitude());
        if (request.arrivalTime() != null) {
            trip.setArrivalTime(request.arrivalTime());
        }
        if (request.notes() != null && !request.notes().isBlank()) {
            trip.setNotes(request.notes());
        }

        FishingTripEntity saved = repository.save(trip);
        auditService.record(AuditActionType.UPDATE, "TRIP_CLOSED", AuditService.MODULE_TRIPS, "Faena", saved.getId(),
                nullDeclaration
                        ? "Cierre con declaración de captura nula"
                        : "Cierre con " + saved.getCatches().size() + " captura(s) registrada(s)");
        return TripResponse.from(saved);
    }

    /**
     * Corrección de una faena. Si ya estaba cerrada, solo la autoridad puede corregirla y el cambio
     * queda auditado con el estado anterior y el nuevo.
     */
    @Transactional
    public TripResponse correct(String tripId, CloseTripRequest request, String zone) {
        AuthenticatedUser actor = currentUser.require();
        FishingTripEntity trip = byId(tripId);
        boolean wasClosed = !trip.isOpen();

        if (wasClosed && !actor.isAuthority()) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "Solo la autoridad puede corregir una faena cerrada.");
        }
        requireOwnership(trip);

        // AC-03.5: toda corrección posterior al cierre debe llevar un motivo explícito.
        String reason = request.reason() == null ? null : request.reason().trim();
        if (wasClosed && (reason == null || reason.isBlank())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Debes indicar el motivo del cambio.");
        }

        String before = describe(trip);
        if (zone != null && !zone.isBlank()) {
            trip.setZone(zone.trim());
        }
        if (request.arrivalTime() != null) {
            trip.setArrivalTime(request.arrivalTime());
        }
        if (request.notes() != null) {
            trip.setNotes(request.notes());
        }
        if (request.landingLatitude() != null) {
            trip.setLandingLatitude(request.landingLatitude());
        }
        if (request.landingLongitude() != null) {
            trip.setLandingLongitude(request.landingLongitude());
        }

        FishingTripEntity saved = repository.save(trip);
        String detail = "Corrección de la faena " + saved.getId()
                + (reason == null || reason.isBlank() ? "" : ". Motivo: " + reason);
        auditService.recordChange("TRIP_CORRECTED", AuditService.MODULE_TRIPS, "Faena", saved.getId(), detail,
                before, describe(saved));
        return TripResponse.from(saved);
    }

    /**
     * Sincroniza faenas registradas sin conexión.
     *
     * <p>Es idempotente por identificador: si la faena ya está en el servidor se devuelve tal cual,
     * de modo que reintentar una sincronización no duplica registros.
     */
    @Transactional
    public List<TripResponse> synchronize(List<SyncTripRequest> pending) {
        List<TripResponse> result = new ArrayList<>();
        for (SyncTripRequest item : pending) {
            String id = Ids.orGenerate(item.id(), "fae");
            var existing = repository.findById(id);
            if (existing.isPresent()) {
                result.add(TripResponse.from(existing.get()));
                continue;
            }

            TripResponse trip = start(new CreateTripRequest(id, item.vesselId(), null, item.zone(),
                    item.originPort(), item.date(), null, null, null, item.ownerId()));
            if (item.captures() != null) {
                for (CreateCatchRequest capture : item.captures()) {
                    trip = addCatch(trip.id(), capture);
                }
            }
            if (item.status() == null || TripStatus.CLOSED.name().equals(item.status())) {
                trip = close(trip.id(), new CloseTripRequest(Boolean.TRUE.equals(item.nullDeclaration()), null, null,
                        item.landingLatitude(), item.landingLongitude(), null));
            }
            auditService.record(AuditActionType.CREATE, "TRIP_SYNCHRONIZED", AuditService.MODULE_TRIPS, "Faena",
                    trip.id(), "Sincronización de una faena registrada sin conexión");
            result.add(trip);
        }
        return result;
    }

    private void requireVisibility(FishingTripEntity trip) {
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAuthority() && !trip.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "La faena pertenece a otro usuario.");
        }
    }

    private void requireOwnership(FishingTripEntity trip) {
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAuthority() && !trip.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "La faena pertenece a otro usuario.");
        }
    }

    private static String describe(FishingTripEntity trip) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("zone", trip.getZone());
        fields.put("originPort", trip.getOriginPort());
        fields.put("status", trip.getStatus().name());
        fields.put("arrival", trip.getArrivalTime() == null ? null : trip.getArrivalTime().toString());
        fields.put("landingLatitude", trip.getLandingLatitude());
        fields.put("landingLongitude", trip.getLandingLongitude());
        fields.put("notes", trip.getNotes());
        fields.put("catches", trip.getCatches().size());
        return AuditService.toJson(fields);
    }
}
