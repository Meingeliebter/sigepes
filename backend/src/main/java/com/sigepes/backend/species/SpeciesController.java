package com.sigepes.backend.species;

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

import com.sigepes.backend.species.SpeciesDtos.CreateSpeciesRequest;
import com.sigepes.backend.species.SpeciesDtos.SpeciesResponse;
import com.sigepes.backend.species.SpeciesDtos.SpeciesVersionResponse;
import com.sigepes.backend.species.SpeciesDtos.UpdateSpeciesRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-06 · Especies y tallas mínimas. */
@RestController
@RequestMapping("/api/v1/especies")
@Tag(name = "Especies")
public class SpeciesController {

    private final SpeciesService speciesService;

    public SpeciesController(SpeciesService speciesService) {
        this.speciesService = speciesService;
    }

    @GetMapping
    @Operation(summary = "Lista el catálogo de especies con su talla mínima y estado de veda")
    public List<SpeciesResponse> list() {
        return speciesService.listWithClosureState();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una especie")
    public SpeciesResponse get(@PathVariable String id) {
        return speciesService.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una especie en el catálogo")
    public SpeciesResponse create(@Valid @RequestBody CreateSpeciesRequest request) {
        return SpeciesResponse.from(speciesService.create(request), false);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @Operation(summary = "Actualiza una especie, incluida su talla mínima legal")
    public SpeciesResponse update(@PathVariable String id, @Valid @RequestBody UpdateSpeciesRequest request) {
        speciesService.update(id, request);
        return speciesService.detail(id);
    }

    @GetMapping("/{id}/versiones")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','INSPECTOR')")
    @Operation(summary = "Historial de versiones de una especie (AC-06.4)")
    public List<SpeciesVersionResponse> versions(@PathVariable String id) {
        return speciesService.versions(id);
    }
}
