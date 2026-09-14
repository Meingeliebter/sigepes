package com.sigepes.backend.vessels;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.vessels.VesselDtos.CreateVesselRequest;
import com.sigepes.backend.vessels.VesselDtos.UpdateVesselRequest;
import com.sigepes.backend.vessels.VesselDtos.VesselResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-02 · Embarcaciones. */
@RestController
@RequestMapping("/api/v1/embarcaciones")
@Tag(name = "Embarcaciones")
public class VesselController {

    private final VesselService vesselService;

    public VesselController(VesselService vesselService) {
        this.vesselService = vesselService;
    }

    @GetMapping
    @Operation(summary = "Lista las embarcaciones visibles para el usuario autenticado")
    public List<VesselResponse> list() {
        return vesselService.findVisible();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una embarcación")
    public VesselResponse get(@PathVariable String id) {
        return vesselService.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una embarcación (RN-04: rechaza artes prohibidas)")
    public VesselResponse create(@Valid @RequestBody CreateVesselRequest request) {
        return vesselService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @Operation(summary = "Actualiza estado, puerto o artes de una embarcación")
    public VesselResponse update(@PathVariable String id, @Valid @RequestBody UpdateVesselRequest request) {
        return vesselService.update(id, request);
    }
}
