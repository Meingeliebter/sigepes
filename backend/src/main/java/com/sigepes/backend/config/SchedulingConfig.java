package com.sigepes.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tareas programadas: revisión de alertas por calendario y latido del canal en vivo (RF-11). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
