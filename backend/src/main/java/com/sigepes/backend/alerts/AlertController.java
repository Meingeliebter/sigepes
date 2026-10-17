package com.sigepes.backend.alerts;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sigepes.backend.alerts.AlertDtos.AlertInbox;
import com.sigepes.backend.alerts.AlertDtos.AlertResponse;
import com.sigepes.backend.alerts.AlertDtos.UnreadCount;
import com.sigepes.backend.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** RF-11 · Alertas y notificaciones de cumplimiento. Cada usuario solo ve las suyas (AC-11.4). */
@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas")
public class AlertController {

    private final AlertService alertService;
    private final AlertStreamService streamService;
    private final CurrentUser currentUser;

    public AlertController(AlertService alertService, AlertStreamService streamService, CurrentUser currentUser) {
        this.alertService = alertService;
        this.streamService = streamService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Buzón de alertas del usuario, de la más reciente a la más antigua")
    public AlertInbox inbox(@RequestParam(name = "limite", defaultValue = "100") int limit) {
        return alertService.inbox(limit);
    }

    @GetMapping("/no-leidas")
    @Operation(summary = "Número de alertas sin leer (campana)")
    public UnreadCount unread() {
        return new UnreadCount(alertService.unreadCount());
    }

    @PatchMapping("/{id}/leida")
    @Operation(summary = "Marca una alerta como leída")
    public AlertResponse markRead(@PathVariable String id) {
        return alertService.markRead(id);
    }

    @PatchMapping("/leidas")
    @Operation(summary = "Marca todas las alertas como leídas")
    public UnreadCount markAllRead() {
        return new UnreadCount(alertService.markAllRead());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archiva una alerta; las críticas no se pueden archivar (RN-22)")
    public void archive(@PathVariable String id) {
        alertService.archive(id);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Canal en vivo (Server-Sent Events) con las alertas nuevas del usuario")
    public SseEmitter stream() {
        return streamService.open(currentUser.require().id());
    }
}
