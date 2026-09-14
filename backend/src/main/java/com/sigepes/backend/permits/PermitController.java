package com.sigepes.backend.permits;

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

import com.sigepes.backend.permits.PermitDtos.CreatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.EvaluatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.PermitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-08 · Permisos y licencias. */
@RestController
@RequestMapping("/api/v1/permisos")
@Tag(name = "Permisos")
public class PermitController {

    private final PermitService permitService;

    public PermitController(PermitService permitService) {
        this.permitService = permitService;
    }

    @GetMapping
    @Operation(summary = "Lista los permisos visibles para el usuario autenticado")
    public List<PermitResponse> list() {
        return permitService.findVisible();
    }

    @GetMapping("/validar/{number}")
    @Operation(summary = "Verifica la constancia de un permiso por su número")
    public PermitResponse validate(@PathVariable String number) {
        return permitService.validateByNumber(number);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita un permiso de pesca (nace PENDING)")
    public PermitResponse request(@Valid @RequestBody CreatePermitRequest request) {
        return permitService.request(request);
    }

    @PatchMapping("/{id}/evaluar")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Aprueba, rechaza o suspende un permiso (exige motivo en rechazo y suspensión)")
    public PermitResponse evaluate(@PathVariable String id, @Valid @RequestBody EvaluatePermitRequest request) {
        return permitService.evaluate(id, request);
    }
}
