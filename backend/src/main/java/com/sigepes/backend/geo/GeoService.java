package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.geo.GeoDtos.CreateZoneRequest;
import com.sigepes.backend.geo.GeoDtos.PositionBatchResponse;
import com.sigepes.backend.geo.GeoDtos.PositionRequest;
import com.sigepes.backend.geo.GeoDtos.PositionResponse;
import com.sigepes.backend.geo.GeoDtos.UpdateZoneRequest;
import com.sigepes.backend.geo.GeoDtos.ZoneEventResponse;
import com.sigepes.backend.geo.GeoDtos.ZoneResponse;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.trips.TripService;
import com.sigepes.backend.users.UserService;

/**
 * RF-04 · Geolocalización de la zona de pesca.
 *
 * <p>Cada posición se compara contra las zonas restringidas activas en dos pasos (AC-04.5): la
 * base de datos descarta por caja envolvente y solo las candidatas pasan la prueba exacta de punto
 * en polígono. Una intrusión deja un evento de control (RN-08) y publica
 * {@link RestrictedZoneEntered} para el módulo de alertas.
 */
@Service
public class GeoService {

    private final RestrictedZoneRepository zoneRepository;
    private final TripPositionRepository positionRepository;
    private final ZoneEventRepository eventRepository;
    private final TripService tripService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public GeoService(RestrictedZoneRepository zoneRepository, TripPositionRepository positionRepository,
            ZoneEventRepository eventRepository, TripService tripService, UserService userService,
            CurrentUser currentUser, AuditService auditService, ApplicationEventPublisher events) {
        this.zoneRepository = zoneRepository;
        this.positionRepository = positionRepository;
        this.eventRepository = eventRepository;
        this.tripService = tripService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Zonas restringidas (AC-04.2)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ZoneResponse> zones() {
        return zoneRepository.findAllByOrderByNameAsc().stream().map(ZoneResponse::from).toList();
    }

    @Transactional
    public ZoneResponse createZone(CreateZoneRequest request) {
        Polygon polygon = Polygon.validated(request.polygon());
        RestrictedZoneEntity zone = new RestrictedZoneEntity(Ids.generate("zon"), request.name().trim(),
                ZoneType.valueOf(request.zoneType()), polygon, request.resolutionNumber().trim());
        zone.setDescription(request.description());
        currentUser.find().ifPresent(user -> zone.setCreatedBy(userService.byId(user.id())));
        RestrictedZoneEntity saved = zoneRepository.save(zone);
        auditService.record(AuditActionType.CREATE, "ZONE_CREATED", AuditService.MODULE_GEO, "Zona restringida",
                saved.getId(), "Zona «" + saved.getName() + "» (" + saved.getZoneType() + ") con "
                        + polygon.vertices().size() + " vértices, " + saved.getResolutionNumber());
        return ZoneResponse.from(saved);
    }

    @Transactional
    public ZoneResponse updateZone(String id, UpdateZoneRequest request) {
        RestrictedZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.ZONE_NOT_FOUND));
        if (request.active() != null) {
            zone.setActive(request.active());
        }
        if (request.description() != null) {
            zone.setDescription(request.description());
        }
        RestrictedZoneEntity saved = zoneRepository.save(zone);
        auditService.record(AuditActionType.UPDATE, "ZONE_UPDATED", AuditService.MODULE_GEO, "Zona restringida",
                saved.getId(), "Zona «" + saved.getName() + "» " + (saved.isActive() ? "activa" : "desactivada"));
        return ZoneResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // Posiciones de la faena (AC-04.1, AC-04.3, AC-04.6)
    // ------------------------------------------------------------------

    /**
     * Registra un lote de posiciones de una faena. Es idempotente por identificador: un reintento
     * de sincronización no duplica posiciones ni eventos.
     *
     * <p>El evento de control se crea al <em>entrar</em> a una zona: posiciones consecutivas dentro
     * de la misma zona forman un solo ingreso.
     */
    @Transactional
    public PositionBatchResponse recordPositions(String tripId, List<PositionRequest> positions) {
        FishingTripEntity trip = tripService.byId(tripId);
        requireOwner(trip);

        int stored = 0;
        int duplicates = 0;
        List<ZoneEventResponse> intrusions = new ArrayList<>();
        String previousZoneId = positionRepository.findTopByTripIdOrderByRecordedAtDesc(tripId)
                .map(TripPositionEntity::getInsideZone)
                .map(RestrictedZoneEntity::getId)
                .orElse(null);
        List<PositionRequest> ordered = positions.stream()
                .sorted(Comparator.comparing(
                        (PositionRequest p) -> p.recordedAt() == null ? Instant.MAX : p.recordedAt()))
                .toList();
        for (PositionRequest request : ordered) {
            String id = Ids.orGenerate(request.id(), "pos");
            if (positionRepository.existsById(id)) {
                duplicates++;
                continue;
            }
            TripPositionEntity position = new TripPositionEntity(id, trip, request.latitude(), request.longitude(),
                    request.accuracyMeters(), request.recordedAt() == null ? Instant.now() : request.recordedAt());

            Optional<RestrictedZoneEntity> zone = locate(request.latitude().doubleValue(),
                    request.longitude().doubleValue());
            zone.ifPresent(position::setInsideZone);
            positionRepository.save(position);
            stored++;

            String zoneId = zone.map(RestrictedZoneEntity::getId).orElse(null);
            if (zoneId != null && !zoneId.equals(previousZoneId)) {
                intrusions.add(ZoneEventResponse.from(registerIntrusion(trip, zone.get(), position)));
            }
            previousZoneId = zoneId;
        }
        return new PositionBatchResponse(stored, duplicates, intrusions);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> positions(String tripId) {
        tripService.byId(tripId);
        return positionRepository.findByTripIdOrderByRecordedAtAsc(tripId).stream().map(PositionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneEventResponse> tripEvents(String tripId) {
        tripService.byId(tripId);
        return eventRepository.findByTripIdOrderByOccurredAtAsc(tripId).stream().map(ZoneEventResponse::from)
                .toList();
    }

    /** Bandeja de eventos de control para la autoridad. */
    @Transactional(readOnly = true)
    public List<ZoneEventResponse> recentEvents() {
        return eventRepository.findTop200ByOrderByOccurredAtDesc().stream().map(ZoneEventResponse::from).toList();
    }

    @Transactional
    public ZoneEventResponse reviewEvent(String eventId) {
        ZoneEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND, "El evento de zona no existe."));
        event.setStatus("REVIEWED");
        auditService.record(AuditActionType.UPDATE, "ZONE_EVENT_REVIEWED", AuditService.MODULE_GEO,
                "Evento de zona", event.getId(), "Revisión del ingreso a «" + event.getZone().getName() + "»");
        return ZoneEventResponse.from(eventRepository.save(event));
    }

    /** Zona restringida activa que contiene el punto, si hay alguna. */
    Optional<RestrictedZoneEntity> locate(double lat, double lon) {
        return zoneRepository.findActiveCandidates(BigDecimal.valueOf(lat), BigDecimal.valueOf(lon))
                .stream()
                .filter(zone -> zone.contains(lat, lon))
                .findFirst();
    }

    private ZoneEventEntity registerIntrusion(FishingTripEntity trip, RestrictedZoneEntity zone,
            TripPositionEntity position) {
        ZoneEventEntity event = eventRepository.save(new ZoneEventEntity(Ids.generate("evz"), trip, zone, position));
        auditService.record(AuditActionType.SECURITY, "ZONE_ENTERED", AuditService.MODULE_GEO, "Faena", trip.getId(),
                "Ingreso a la zona restringida «" + zone.getName() + "» en " + position.getLatitude() + ", "
                        + position.getLongitude());
        events.publishEvent(new RestrictedZoneEntered(event.getId(), trip.getId(), trip.getOwner().getId(),
                zone.getName(), zone.getZoneType().name(), trip.getVessel().getName(),
                position.getLatitude().doubleValue(), position.getLongitude().doubleValue()));
        return event;
    }

    private void requireOwner(FishingTripEntity trip) {
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAdmin() && !trip.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "Solo el titular de la faena puede registrar sus posiciones.");
        }
    }
}
