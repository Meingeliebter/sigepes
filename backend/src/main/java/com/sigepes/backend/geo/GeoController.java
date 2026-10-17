package com.sigepes.backend.geo;

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

import com.sigepes.backend.geo.GeoDtos.CreateZoneRequest;
import com.sigepes.backend.geo.GeoDtos.PositionBatchRequest;
import com.sigepes.backend.geo.GeoDtos.PositionBatchResponse;
import com.sigepes.backend.geo.GeoDtos.PositionResponse;
import com.sigepes.backend.geo.GeoDtos.UpdateZoneRequest;
import com.sigepes.backend.geo.GeoDtos.ZoneEventResponse;
import com.sigepes.backend.geo.GeoDtos.ZoneResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-04 · Zonas restringidas, posiciones de faena y eventos de control. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Geolocalización")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/zonas-restringidas")
    @Operation(summary = "Lista las zonas restringidas (AMP y zonas en veda) con su polígono")
    public List<ZoneResponse> zones() {
        return geoService.zones();
    }

    @PostMapping("/zonas-restringidas")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una zona restringida delimitada por un polígono")
    public ZoneResponse createZone(@Valid @RequestBody CreateZoneRequest request) {
        return geoService.createZone(request);
    }

    @PatchMapping("/zonas-restringidas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    @Operation(summary = "Activa o desactiva una zona restringida")
    public ZoneResponse updateZone(@PathVariable String id, @Valid @RequestBody UpdateZoneRequest request) {
        return geoService.updateZone(id, request);
    }

    @PostMapping("/faenas/{tripId}/posiciones")
    @Operation(summary = "Registra un lote de posiciones GPS de la faena; devuelve las intrusiones en zonas restringidas")
    public PositionBatchResponse recordPositions(@PathVariable String tripId,
            @Valid @RequestBody PositionBatchRequest request) {
        return geoService.recordPositions(tripId, request.positions());
    }

    @GetMapping("/faenas/{tripId}/posiciones")
    @Operation(summary = "Recorrido GPS registrado de una faena")
    public List<PositionResponse> positions(@PathVariable String tripId) {
        return geoService.positions(tripId);
    }

    @GetMapping("/faenas/{tripId}/eventos-zona")
    @Operation(summary = "Ingresos a zonas restringidas de una faena")
    public List<ZoneEventResponse> tripEvents(@PathVariable String tripId) {
        return geoService.tripEvents(tripId);
    }

    @GetMapping("/eventos-zona")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR','ANALYST')")
    @Operation(summary = "Eventos de control por ingreso a zonas restringidas (RN-08)")
    public List<ZoneEventResponse> events() {
        return geoService.recentEvents();
    }

    @PatchMapping("/eventos-zona/{id}/revisar")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    @Operation(summary = "Marca un evento de zona como revisado por el inspector")
    public ZoneEventResponse review(@PathVariable String id) {
        return geoService.reviewEvent(id);
    }
}
