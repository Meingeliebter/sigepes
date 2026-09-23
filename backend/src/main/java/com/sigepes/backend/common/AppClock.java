package com.sigepes.backend.common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * AC-05.2 · El "hoy" regulatorio de SIGEPES.
 *
 * <p>Colombia no observa horario de verano, así que UTC-5 es fijo: {@code America/Bogota} basta
 * para que la vigencia de una veda active/desactive a medianoche en la hora local, sin depender de
 * la zona horaria por defecto del servidor donde corra la JVM.
 */
public final class AppClock {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private AppClock() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
