package com.sigepes.backend.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.dashboard.DashboardDtos.DashboardResponse;
import com.sigepes.backend.dashboard.DashboardDtos.NotificationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Resumen de inicio y notificaciones derivadas del MVP v1.0. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inicio")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Indicadores, actividad reciente y cumplimiento del usuario autenticado")
    public DashboardResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/notificaciones")
    @Operation(summary = "Alertas derivadas de vedas vigentes y permisos próximos a vencer")
    public List<NotificationResponse> notifications() {
        return dashboardService.notifications();
    }
}
