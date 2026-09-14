package com.sigepes.backend.trips;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.trips.TripDtos.CloseTripRequest;
import com.sigepes.backend.trips.TripDtos.CreateCatchRequest;
import com.sigepes.backend.trips.TripDtos.CreateTripRequest;
import com.sigepes.backend.trips.TripDtos.SyncTripRequest;
import com.sigepes.backend.trips.TripDtos.TripResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-03 · Bitácora de faenas y capturas. */
@RestController
@RequestMapping("/api/v1/faenas")
@Tag(name = "Faenas")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    @Operation(summary = "Lista las faenas visibles para el usuario autenticado")
    public List<TripResponse> list() {
        return tripService.findVisible();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una faena")
    public TripResponse get(@PathVariable String id) {
        return tripService.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una faena (valida embarcación, permiso vigente y arte)")
    public TripResponse start(@Valid @RequestBody CreateTripRequest request) {
        return tripService.start(request);
    }

    @PostMapping("/{id}/capturas")
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @Operation(summary = "Registra una captura (valida especie, veda y talla mínima)")
    public TripResponse addCatch(@PathVariable String id, @Valid @RequestBody CreateCatchRequest request) {
        return tripService.addCatch(id, request);
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @Operation(summary = "Cierra una faena con capturas o con declaración de captura nula")
    public TripResponse close(@PathVariable String id, @RequestBody(required = false) CloseTripRequest request) {
        return tripService.close(id, request == null ? new CloseTripRequest(false, null, null) : request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige una faena; si ya estaba cerrada, solo la autoridad y queda auditado")
    public TripResponse correct(@PathVariable String id,
            @RequestParam(required = false) String zone,
            @Valid @RequestBody CloseTripRequest request) {
        return tripService.correct(id, request, zone);
    }

    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('FISHER','SHIPOWNER','ADMIN')")
    @Operation(summary = "Sincroniza faenas registradas sin conexión (idempotente por identificador)")
    public List<TripResponse> synchronize(@RequestBody List<SyncTripRequest> pending) {
        return tripService.synchronize(pending);
    }
}
