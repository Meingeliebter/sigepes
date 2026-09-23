package com.sigepes.backend.vessels;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.gears.GearService;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.vessels.VesselDtos.CreateVesselRequest;
import com.sigepes.backend.vessels.VesselDtos.UpdateVesselRequest;
import com.sigepes.backend.vessels.VesselDtos.VesselResponse;

/** RF-02 · Embarcaciones y sus artes asociadas. */
@Service
public class VesselService {

    private final VesselRepository repository;
    private final GearService gearService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public VesselService(VesselRepository repository, GearService gearService, UserService userService,
            CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.gearService = gearService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    /**
     * La autoridad ve todas las embarcaciones; un operador solo las suyas.
     *
     * <p>El DTO se arma dentro de la transacción porque el propietario es una asociación perezosa
     * y {@code open-in-view} está desactivado.
     */
    @Transactional(readOnly = true)
    public List<VesselResponse> findVisible() {
        AuthenticatedUser user = currentUser.require();
        List<VesselEntity> vessels = user.isAuthority()
                ? repository.findAllByOrderByNameAsc()
                : repository.findByOwnerIdOrderByNameAsc(user.id());
        return vessels.stream().map(VesselResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VesselResponse detail(String id) {
        return VesselResponse.from(byId(id));
    }

    @Transactional(readOnly = true)
    public VesselEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.VESSEL_NOT_FOUND));
    }

    @Transactional
    public VesselResponse create(CreateVesselRequest request) {
        AuthenticatedUser actor = currentUser.require();

        String registration = request.registration().trim();
        if (repository.existsByRegistrationIgnoreCase(registration)) {
            throw ApiException.of(ErrorCode.REGISTRATION_ALREADY_REGISTERED);
        }

        // Solo un administrador puede registrar una embarcación a nombre de otra persona.
        String ownerId = request.ownerId() != null && !request.ownerId().isBlank() && actor.isAdmin()
                ? request.ownerId()
                : actor.id();
        UserEntity owner = userService.byId(ownerId);

        VesselEntity vessel = new VesselEntity(Ids.generate("emb"), request.name().trim(), registration,
                request.port().trim(), owner);
        vessel.setVesselType(request.type());
        vessel.setHullType(request.hullType().trim());
        vessel.setEnginePower(request.enginePower());
        vessel.setCapacityTons(request.capacityTons());
        if (request.status() != null && !request.status().isBlank()) {
            vessel.setStatus(VesselStatus.valueOf(request.status()));
        }
        vessel.setGears(resolveGears(request.gears()));

        VesselEntity saved = repository.save(vessel);
        auditService.record(AuditActionType.CREATE, "VESSEL_CREATED", AuditService.MODULE_VESSELS, "Embarcación",
                saved.getId(), "Alta de «" + saved.getName() + "» (matrícula " + saved.getRegistration() + ")");
        return VesselResponse.from(saved);
    }

    @Transactional
    public VesselResponse update(String id, UpdateVesselRequest request) {
        AuthenticatedUser actor = currentUser.require();
        VesselEntity vessel = byId(id);
        requireOwnershipOrAuthority(vessel, actor);

        String before = describe(vessel);
        if (request.status() != null && !request.status().isBlank()) {
            vessel.setStatus(VesselStatus.valueOf(request.status()));
        }
        if (request.port() != null && !request.port().isBlank()) {
            vessel.setPort(request.port().trim());
        }
        if (request.gears() != null) {
            vessel.setGears(resolveGears(request.gears()));
        }

        VesselEntity saved = repository.save(vessel);
        auditService.recordChange("VESSEL_UPDATED", AuditService.MODULE_VESSELS, "Embarcación", saved.getId(),
                "Actualización de «" + saved.getName() + "»", before, describe(saved));
        return VesselResponse.from(saved);
    }

    /**
     * RN-04: rechaza cualquier arte clasificada como destructiva o bloqueada, resolviendo cada
     * entrada por identificador o por nombre.
     */
    private Set<FishingGearEntity> resolveGears(List<String> requested) {
        Set<FishingGearEntity> resolved = new LinkedHashSet<>();
        if (requested == null) {
            return resolved;
        }
        for (String value : requested) {
            if (value == null || value.isBlank()) {
                continue;
            }
            FishingGearEntity gear = gearService.resolve(value);
            if (!gear.isUsable()) {
                throw ApiException.of(ErrorCode.PROHIBITED_GEAR,
                        "El arte «" + gear.getName() + "» está clasificada como destructiva o prohibida.");
            }
            resolved.add(gear);
        }
        return resolved;
    }

    private static void requireOwnershipOrAuthority(VesselEntity vessel, AuthenticatedUser actor) {
        if (!actor.isAuthority() && !vessel.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.VESSEL_NOT_OWNED);
        }
    }

    private static String describe(VesselEntity vessel) {
        return AuditService.toJson(Map.of(
                "status", vessel.getStatus().name(),
                "port", vessel.getPort(),
                "gears", vessel.getGears().stream().map(FishingGearEntity::getName).sorted().toList()));
    }
}
