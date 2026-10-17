package com.sigepes.backend.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta {@link AlertSweeper} al arrancar y luego cada pocos minutos. Se puede desactivar con
 * {@code sigepes.alerts.sweep-enabled=false} (las pruebas lo invocan a mano).
 */
@Component
@ConditionalOnProperty(name = "sigepes.alerts.sweep-enabled", havingValue = "true", matchIfMissing = true)
public class AlertSweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertSweepScheduler.class);

    private final AlertSweeper sweeper;

    public AlertSweepScheduler(AlertSweeper sweeper) {
        this.sweeper = sweeper;
    }

    @Scheduled(initialDelayString = "${sigepes.alerts.sweep-initial-delay-ms:15000}",
            fixedDelayString = "${sigepes.alerts.sweep-interval-ms:300000}")
    public void run() {
        try {
            int created = sweeper.sweep();
            if (created > 0) {
                log.info("Revisión de alertas: {} alerta(s) nueva(s).", created);
            }
        } catch (RuntimeException e) {
            // Un fallo aquí no debe detener el programador; la próxima revisión lo reintenta.
            log.warn("La revisión de alertas falló: {}", e.getMessage());
        }
    }
}
