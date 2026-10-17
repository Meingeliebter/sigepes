package com.sigepes.backend.traceability;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.traceability.TraceabilityDtos.CreateLotRequest;
import com.sigepes.backend.traceability.TraceabilityDtos.EligibleCatch;
import com.sigepes.backend.traceability.TraceabilityDtos.LotResponse;
import com.sigepes.backend.traceability.TraceabilityDtos.PublicLotResponse;
import com.sigepes.backend.traceability.TraceabilityDtos.TransferRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-09 · Trazabilidad de la cadena de valor. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Trazabilidad")
public class TraceabilityController {

    private final TraceabilityService service;

    public TraceabilityController(TraceabilityService service) {
        this.service = service;
    }

    @GetMapping("/lotes")
    @Operation(summary = "Lotes del titular (o todos, para la autoridad)")
    public List<LotResponse> lots() {
        return service.lots();
    }

    @GetMapping("/lotes/{id}")
    @Operation(summary = "Detalle del lote con su cadena de custodia")
    public LotResponse lot(@PathVariable String id) {
        return service.lot(id);
    }

    @GetMapping("/faenas/{tripId}/capturas-para-lote")
    @Operation(summary = "Capturas de una faena con su verificación de origen legal y kilos disponibles")
    public List<EligibleCatch> eligible(@PathVariable String tripId) {
        return service.eligibleCatches(tripId);
    }

    @PostMapping("/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un lote con código y QR desde una captura legal (RN-18)")
    public LotResponse create(@Valid @RequestBody CreateLotRequest request) {
        return service.createLot(request);
    }

    @PostMapping("/lotes/{id}/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una entrega del lote en la cadena de custodia (solo inserción)")
    public LotResponse transfer(@PathVariable String id, @Valid @RequestBody TransferRequest request) {
        return service.transfer(id, request);
    }

    @GetMapping("/trazabilidad/{code}")
    @Operation(summary = "Verificación pública del QR: origen, legalidad y cadena, sin datos personales")
    public PublicLotResponse verify(@PathVariable String code) {
        return service.verify(code);
    }
}
