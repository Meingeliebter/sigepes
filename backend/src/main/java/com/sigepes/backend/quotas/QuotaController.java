package com.sigepes.backend.quotas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.quotas.QuotaDtos.CreateQuotaRequest;
import com.sigepes.backend.quotas.QuotaDtos.QuotaResponse;
import com.sigepes.backend.quotas.QuotaDtos.UpdateQuotaRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-07 · Cuotas de pesca por especie. */
@RestController
@RequestMapping("/api/v1/cuotas")
@Tag(name = "Cuotas")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    @Operation(summary = "Lista las cuotas con su saldo")
    public List<QuotaResponse> list() {
        return quotaService.findAll();
    }

    @GetMapping("/saldo")
    @Operation(summary = "Saldo de la cuota aplicable a una especie, zona, fecha y embarcación (204 si no hay cuota)")
    public ResponseEntity<QuotaResponse> balance(@RequestParam String speciesId, @RequestParam String zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String vesselId) {
        return quotaService.balance(speciesId, zone, date == null ? AppClock.today() : date, vesselId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asigna una cuota a una especie, zona y periodo")
    public QuotaResponse create(@Valid @RequestBody CreateQuotaRequest request) {
        return QuotaResponse.from(quotaService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @Operation(summary = "Ajusta el total, el periodo o el estado de una cuota")
    public QuotaResponse update(@PathVariable String id, @Valid @RequestBody UpdateQuotaRequest request) {
        return QuotaResponse.from(quotaService.update(id, request));
    }
}
