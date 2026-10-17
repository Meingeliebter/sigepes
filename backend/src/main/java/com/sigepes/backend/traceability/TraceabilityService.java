package com.sigepes.backend.traceability;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.Codes;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.traceability.ChainEventEntity.EventType;
import com.sigepes.backend.traceability.TraceabilityDtos.ChainEventResponse;
import com.sigepes.backend.traceability.TraceabilityDtos.CreateLotRequest;
import com.sigepes.backend.traceability.TraceabilityDtos.EligibleCatch;
import com.sigepes.backend.traceability.TraceabilityDtos.LegalCheck;
import com.sigepes.backend.traceability.TraceabilityDtos.LotResponse;
import com.sigepes.backend.traceability.TraceabilityDtos.PublicChainEvent;
import com.sigepes.backend.traceability.TraceabilityDtos.PublicLotResponse;
import com.sigepes.backend.traceability.TraceabilityDtos.TransferRequest;
import com.sigepes.backend.trips.CatchEntity;
import com.sigepes.backend.trips.CatchRepository;
import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.trips.TripService;

/**
 * RF-09 · Trazabilidad de la cadena de valor: lotes con origen legal y cadena de custodia
 * encadenada por hash, verificable por cualquiera con el QR.
 */
@Service
public class TraceabilityService {

    /** AC-09.6: la página pública se sirve de caché; una transferencia la invalida al instante. */
    private static final Duration PUBLIC_CACHE_TTL = Duration.ofMinutes(5);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<LegalCheck>> CHECKS = new TypeReference<>() {
    };

    private final LotRepository lotRepository;
    private final ChainEventRepository eventRepository;
    private final CatchRepository catchRepository;
    private final TripService tripService;
    private final LegalOriginChecker checker;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final Map<String, CachedLot> publicCache = new ConcurrentHashMap<>();

    private record CachedLot(PublicLotResponse value, Instant expiresAt) {
    }

    public TraceabilityService(LotRepository lotRepository, ChainEventRepository eventRepository,
            CatchRepository catchRepository, TripService tripService, LegalOriginChecker checker,
            CurrentUser currentUser, AuditService auditService) {
        this.lotRepository = lotRepository;
        this.eventRepository = eventRepository;
        this.catchRepository = catchRepository;
        this.tripService = tripService;
        this.checker = checker;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Creación de lotes (AC-09.1, AC-09.2)
    // ------------------------------------------------------------------

    /** Capturas de una faena propia con su legalidad y los kilos que aún no están en lotes. */
    @Transactional(readOnly = true)
    public List<EligibleCatch> eligibleCatches(String tripId) {
        FishingTripEntity trip = tripService.byId(tripId);
        requireTripOwner(trip);
        return trip.getCatches().stream().map(catchEntry -> {
            List<LegalCheck> checks = checker.check(catchEntry);
            return new EligibleCatch(catchEntry.getId(), catchEntry.getSpecies().getCommonName(),
                    catchEntry.getWeightKg(), available(catchEntry), checks.stream().allMatch(LegalCheck::passed),
                    checks);
        }).toList();
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request) {
        CatchEntity catchEntry = catchRepository.findById(request.catchId())
                .orElseThrow(() -> ApiException.of(ErrorCode.TRIP_NOT_FOUND, "La captura no existe."));
        requireTripOwner(catchEntry.getTrip());

        List<LegalCheck> checks = checker.check(catchEntry);
        LegalCheck failed = checks.stream().filter(check -> !check.passed()).findFirst().orElse(null);
        if (failed != null) {
            throw ApiException.of(ErrorCode.LOT_NOT_LEGAL, failed.label() + ": " + failed.detail());
        }
        if (request.weightKg().compareTo(available(catchEntry)) > 0) {
            throw ApiException.of(ErrorCode.LOT_WEIGHT_EXCEEDED,
                    "Solo quedan " + available(catchEntry) + " kg de esta captura sin asignar a un lote.");
        }

        String code = newCode();
        LotEntity lot = lotRepository.save(new LotEntity(Ids.generate("lot"), code, catchEntry,
                request.weightKg(), toJson(checks)));
        FishingTripEntity trip = catchEntry.getTrip();
        append(lot, EventType.CREATED, PartyType.FISHER,
                trip.getVessel().getName(), null, trip.getOriginPort(), lot.getLandingDate());
        auditService.record(AuditActionType.CREATE, "LOT_CREATED", AuditService.MODULE_TRACEABILITY, "Lote",
                lot.getId(), "Lote " + code + ": " + lot.getWeightKg() + " kg de " + lot.getSpecies().getCommonName());
        return toResponse(lot);
    }

    // ------------------------------------------------------------------
    // Cadena de custodia (AC-09.3, AC-09.5)
    // ------------------------------------------------------------------

    @Transactional
    public LotResponse transfer(String lotId, TransferRequest request) {
        LotEntity lot = visibleLot(lotId);
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAuthority() && !lot.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "Solo el titular del lote registra sus entregas.");
        }
        LocalDate occurredOn = request.occurredOn() == null ? AppClock.today() : request.occurredOn();
        if (occurredOn.isBefore(lot.getLandingDate())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "La entrega no puede ser anterior al desembarco.");
        }
        append(lot, EventType.TRANSFER, PartyType.valueOf(request.partyType()), request.partyName().trim(),
                blankToNull(request.partyTaxId()), request.location().trim(), occurredOn);
        publicCache.remove(lot.getCode());
        auditService.record(AuditActionType.CREATE, "LOT_TRANSFERRED", AuditService.MODULE_TRACEABILITY, "Lote",
                lot.getId(), "Lote " + lot.getCode() + " entregado a " + request.partyType());
        return toResponse(lot);
    }

    private void append(LotEntity lot, EventType type, PartyType partyType, String partyName, String partyTaxId,
            String location, LocalDate occurredOn) {
        List<ChainEventEntity> chain = eventRepository.findByLotIdOrderBySequenceNoAsc(lot.getId());
        String previous = chain.isEmpty() ? ChainHasher.GENESIS : chain.get(chain.size() - 1).getHash();
        int sequence = chain.size() + 1;
        String hash = ChainHasher.hash(previous, lot, sequence, type, partyType, partyName, partyTaxId, location,
                occurredOn);
        eventRepository.save(new ChainEventEntity(Ids.generate("cad"), lot.getId(), sequence, type, partyType,
                partyName, partyTaxId, location, occurredOn, currentUser.require().id(), previous, hash));
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LotResponse> lots() {
        AuthenticatedUser actor = currentUser.require();
        List<LotEntity> lots = actor.isAuthority()
                ? lotRepository.findAllByOrderByCreatedAtDesc()
                : lotRepository.findByOwnerIdOrderByCreatedAtDesc(actor.id());
        return lots.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LotResponse lot(String id) {
        return toResponse(visibleLot(id));
    }

    /** AC-09.4 · Verificación pública por QR, servida de caché (AC-09.6). */
    @Transactional(readOnly = true)
    public PublicLotResponse verify(String code) {
        String normalized = code.trim().toUpperCase();
        CachedLot cached = publicCache.get(normalized);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }
        LotEntity lot = lotRepository.findByCode(normalized)
                .orElseThrow(() -> ApiException.of(ErrorCode.LOT_NOT_FOUND));
        List<ChainEventEntity> chain = eventRepository.findByLotIdOrderBySequenceNoAsc(lot.getId());
        List<LegalCheck> checks = fromJson(lot.getLegalChecks()).stream().map(TraceabilityService::forPublic).toList();
        PublicLotResponse response = new PublicLotResponse(lot.getCode(), lot.getSpecies().getCommonName(),
                lot.getSpecies().getScientificName(), lot.getWeightKg(), lot.getLandingDate(), lot.getZone(),
                lot.getVessel().getRegistration(), checks.stream().allMatch(LegalCheck::passed),
                ChainHasher.verify(lot, chain), checks,
                chain.stream().map(event -> new PublicChainEvent(event.getSequenceNo(), event.getEventType().name(),
                        event.getPartyType().name(), event.getLocation(), event.getOccurredOn())).toList());
        if (publicCache.size() > 5_000) {
            publicCache.clear();
        }
        publicCache.put(normalized, new CachedLot(response, Instant.now().plus(PUBLIC_CACHE_TTL)));
        return response;
    }

    // ------------------------------------------------------------------

    private LotEntity visibleLot(String id) {
        LotEntity lot = lotRepository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.LOT_NOT_FOUND));
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAuthority() && !lot.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.LOT_NOT_FOUND);
        }
        return lot;
    }

    private void requireTripOwner(FishingTripEntity trip) {
        if (!trip.getOwner().getId().equals(currentUser.require().id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "Solo el titular de la faena puede crear lotes de su captura.");
        }
    }

    private BigDecimal available(CatchEntity catchEntry) {
        return catchEntry.getWeightKg().subtract(lotRepository.lottedWeight(catchEntry.getId())).max(BigDecimal.ZERO);
    }

    private LotResponse toResponse(LotEntity lot) {
        List<ChainEventEntity> chain = eventRepository.findByLotIdOrderBySequenceNoAsc(lot.getId());
        ChainEventEntity last = chain.isEmpty() ? null : chain.get(chain.size() - 1);
        return new LotResponse(lot.getId(), lot.getCode(), lot.getTripId(), lot.getSpecies().getCommonName(),
                lot.getSpecies().getScientificName(), lot.getWeightKg(), lot.getLandingDate(), lot.getZone(),
                lot.getVessel().getName(), lot.getVessel().getRegistration(), lot.getOwner().getFullName(),
                last == null ? null : last.getPartyName(), ChainHasher.verify(lot, chain),
                fromJson(lot.getLegalChecks()), chain.stream().map(ChainEventResponse::from).toList(),
                lot.getCreatedAt());
    }

    private String newCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = Codes.random("LOT", 8);
            if (!lotRepository.existsByCode(code)) {
                return code;
            }
        }
        throw ApiException.of(ErrorCode.INTERNAL_ERROR, "No fue posible generar el código del lote.");
    }

    private static String toJson(List<LegalCheck> checks) {
        try {
            return JSON.writeValueAsString(checks);
        } catch (Exception e) {
            throw ApiException.of(ErrorCode.INTERNAL_ERROR);
        }
    }

    private static List<LegalCheck> fromJson(String json) {
        try {
            return JSON.readValue(json, CHECKS);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** El número de permiso identifica al pescador: en la página pública solo se dice si estaba vigente. */
    private static LegalCheck forPublic(LegalCheck check) {
        if (!"permit".equals(check.key())) {
            return check;
        }
        return new LegalCheck(check.key(), check.label(), check.passed(), check.passed()
                ? "Permiso aprobado y vigente el día de la faena."
                : "No había un permiso aprobado y vigente el día de la faena.");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
