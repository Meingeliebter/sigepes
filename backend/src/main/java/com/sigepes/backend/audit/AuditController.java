package com.sigepes.backend.audit;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.audit.AuditDtos.AuditEventResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** RF-20 · Consulta de la bitácora de auditoría. Solo autoridad. */
@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoría")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    @Operation(summary = "Lista los eventos de auditoría más recientes")
    public List<AuditEventResponse> list(
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "limit", defaultValue = "200") int limit) {
        return auditService.latest(module, limit).stream().map(AuditEventResponse::from).toList();
    }
}
