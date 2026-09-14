package com.sigepes.backend.gears;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.gears.GearDtos.CreateGearRequest;
import com.sigepes.backend.gears.GearDtos.GearResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-02 · Artes de pesca. */
@RestController
@RequestMapping("/api/v1/artes-pesca")
@Tag(name = "Artes de pesca")
public class GearController {

    private final GearService gearService;

    public GearController(GearService gearService) {
        this.gearService = gearService;
    }

    @GetMapping
    @Operation(summary = "Lista el catálogo de artes de pesca")
    public List<GearResponse> list() {
        return gearService.findAll().stream().map(GearResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un arte de pesca en el catálogo")
    public GearResponse create(@Valid @RequestBody CreateGearRequest request) {
        return GearResponse.from(gearService.create(request));
    }
}
