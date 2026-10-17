package com.sigepes.backend.audit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.audit.AuditDtos.AuditEventResponse;
import com.sigepes.backend.common.AppClock;

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
    @Operation(summary = "Lista los eventos de auditoría, con filtros opcionales de fecha, acción, módulo y usuario (AC-20.4)")
    public List<AuditEventResponse> list(
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "actionType", required = false) AuditActionType actionType,
            @RequestParam(name = "actorId", required = false) String actorId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "limit", defaultValue = "200") int limit) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(AppClock.ZONE).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(AppClock.ZONE).toInstant();
        return auditService.search(module, actionType, actorId, fromInstant, toInstant, limit).stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
