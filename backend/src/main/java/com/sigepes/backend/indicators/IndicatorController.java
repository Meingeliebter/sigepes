package com.sigepes.backend.indicators;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.indicators.IndicatorDtos.IndicatorsResponse;
import com.sigepes.backend.indicators.IndicatorDtos.OwnIndicators;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** RF-12 · Tablero de indicadores de sostenibilidad. */
@RestController
@RequestMapping("/api/v1/indicadores")
@Tag(name = "Indicadores")
public class IndicatorController {

    private final IndicatorService service;

    public IndicatorController(IndicatorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSPECTOR','ANALYST','ADMIN')")
    @Operation(summary = "Indicadores de sostenibilidad por periodo, región y especie (solo autoridad, AC-12.6)")
    public IndicatorsResponse indicators(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "region", required = false) Region region,
            @RequestParam(name = "especieId", required = false) String speciesId) {
        return service.indicators(IndicatorService.filters(from, to, region, speciesId));
    }

    @GetMapping("/mios")
    @Operation(summary = "Cifras propias del pescador o armador autenticado")
    public OwnIndicators own(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "region", required = false) Region region,
            @RequestParam(name = "especieId", required = false) String speciesId) {
        return service.own(IndicatorService.filters(from, to, region, speciesId));
    }
}
