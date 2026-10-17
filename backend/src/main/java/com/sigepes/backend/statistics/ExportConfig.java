package com.sigepes.backend.statistics;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AC-18.5 · Hilos dedicados a las exportaciones largas: acotados para que nunca compitan con las
 * peticiones de la API (p95 < 500 ms para el resto de operaciones mientras se genera un reporte).
 */
@Configuration
class ExportConfig {

    @Bean(name = "exportExecutor")
    TaskExecutor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("export-");
        executor.initialize();
        return executor;
    }

    /** Un trabajo que quedó pendiente por un reinicio se retoma al arrancar. */
    @EventListener(ApplicationReadyEvent.class)
    void resumePending(ApplicationReadyEvent event) {
        OfficialReportService service = event.getApplicationContext().getBean(OfficialReportService.class);
        TaskExecutor executor = event.getApplicationContext().getBean("exportExecutor", TaskExecutor.class);
        executor.execute(service::runPending);
    }
}
