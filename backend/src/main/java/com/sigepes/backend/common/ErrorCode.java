package com.sigepes.backend.common;

import org.springframework.http.HttpStatus;

/**
 * Catálogo único de errores de negocio de SIGEPES.
 *
 * <p>El código viaja al frontend en el campo {@code code} de la respuesta de error para que la UI
 * pueda reaccionar de forma específica; {@code message} es el texto por defecto mostrable al
 * usuario final (en español, igual que el resto de la interfaz).
 */
public enum ErrorCode {

    // --- Autenticación / autorización ---------------------------------
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para realizar esta acción."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "La cuenta está suspendida. Contacta al administrador."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera un momento e inténtalo de nuevo."),

    // --- Usuarios -----------------------------------------------------
    DOCUMENT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "La identificación ya está registrada."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "El correo ya está registrado."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "El usuario no existe."),

    // --- Embarcaciones y artes ----------------------------------------
    VESSEL_NOT_FOUND(HttpStatus.NOT_FOUND, "La embarcación no existe."),
    VESSEL_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "La embarcación no está activa."),
    VESSEL_NOT_OWNED(HttpStatus.FORBIDDEN, "La embarcación no está a tu nombre."),
    REGISTRATION_ALREADY_REGISTERED(HttpStatus.CONFLICT, "La matrícula ya está registrada."),
    GEAR_NOT_FOUND(HttpStatus.NOT_FOUND, "El arte de pesca no existe."),
    PROHIBITED_GEAR(HttpStatus.UNPROCESSABLE_ENTITY, "El arte de pesca está clasificado como destructivo o prohibido."),
    GEAR_ALREADY_REGISTERED(HttpStatus.CONFLICT, "El arte de pesca ya está registrado."),

    // --- Permisos -----------------------------------------------------
    PERMIT_NOT_FOUND(HttpStatus.NOT_FOUND, "El permiso no existe."),
    PERMIT_REQUIRED(HttpStatus.FORBIDDEN, "No cuentas con un permiso de pesca vigente."),
    PERMIT_EXPIRED(HttpStatus.FORBIDDEN, "El permiso de pesca está vencido."),
    PERMIT_NOT_APPROVED(HttpStatus.FORBIDDEN, "El permiso de pesca no está aprobado."),
    PERMIT_SUSPENDED(HttpStatus.FORBIDDEN, "El permiso de pesca está suspendido."),
    PERMIT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "El número de permiso ya está registrado."),
    INVALID_PERMIT_STATUS(HttpStatus.BAD_REQUEST, "El estado de permiso solicitado no es válido."),
    PERMIT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Debes indicar el motivo del rechazo o la suspensión."),
    PERMIT_ATTACHMENTS_REQUIRED(HttpStatus.BAD_REQUEST,
            "Debes adjuntar la copia de cédula y el certificado de propiedad de la embarcación."),
    PERMIT_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El adjunto solicitado no existe."),

    // --- Especies y vedas ---------------------------------------------
    SPECIES_NOT_FOUND(HttpStatus.NOT_FOUND, "La especie no existe."),
    SPECIES_ALREADY_REGISTERED(HttpStatus.CONFLICT, "La especie ya está registrada."),
    SPECIES_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "La especie no está habilitada en el catálogo."),
    MINIMUM_SIZE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "La talla está por debajo del mínimo legal."),
    SPECIES_UNDER_CLOSURE(HttpStatus.UNPROCESSABLE_ENTITY, "La especie está en veda para esta zona y fecha."),
    CLOSURE_NOT_FOUND(HttpStatus.NOT_FOUND, "La veda no existe."),
    INVALID_CLOSURE_PERIOD(HttpStatus.BAD_REQUEST, "El periodo de veda no es válido."),

    // --- Faenas -------------------------------------------------------
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "La faena no existe."),
    TRIP_ALREADY_CLOSED(HttpStatus.CONFLICT, "La faena ya está cerrada."),
    TRIP_CANNOT_CLOSE(HttpStatus.UNPROCESSABLE_ENTITY,
            "La faena no puede cerrarse: registra al menos una captura o declara captura nula."),
    TRIP_ALREADY_REGISTERED(HttpStatus.CONFLICT, "La faena ya fue registrada."),
    NULL_DECLARATION_WITH_CATCHES(HttpStatus.UNPROCESSABLE_ENTITY,
            "No puedes declarar captura nula en una faena que ya tiene capturas registradas."),

    // --- Cuotas (RF-07) -----------------------------------------------
    QUOTA_NOT_FOUND(HttpStatus.NOT_FOUND, "La cuota no existe."),
    QUOTA_EXHAUSTED(HttpStatus.UNPROCESSABLE_ENTITY, "La captura excede la cuota disponible."),
    INVALID_QUOTA_PERIOD(HttpStatus.BAD_REQUEST, "El periodo de la cuota no es válido."),

    // --- Geolocalización (RF-04) ---------------------------------------
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "La zona restringida no existe."),
    INVALID_ZONE_POLYGON(HttpStatus.BAD_REQUEST, "El polígono de la zona no es válido."),

    // --- Denuncias (RF-10) --------------------------------------------
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "La denuncia no existe."),
    INVALID_REPORT_TRANSITION(HttpStatus.UNPROCESSABLE_ENTITY,
            "La denuncia no puede pasar a ese estado desde el estado actual."),
    INVALID_REPORT_PHOTO(HttpStatus.BAD_REQUEST, "La foto adjunta no es válida."),
    TOO_MANY_REPORT_PHOTOS(HttpStatus.BAD_REQUEST, "Una denuncia admite como máximo 3 fotos."),

    // --- Trazabilidad (RF-09) -----------------------------------------
    LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "El lote no existe."),
    LOT_NOT_LEGAL(HttpStatus.UNPROCESSABLE_ENTITY, "La captura no cumple las condiciones de origen legal."),
    LOT_WEIGHT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "El peso supera los kilos disponibles de la captura."),

    // --- Documentos (RF-15) -------------------------------------------
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El documento no existe."),
    INVALID_DOCUMENT_FILE(HttpStatus.BAD_REQUEST,
            "El archivo debe ser un PDF, JPG o PNG real (se revisa su contenido, no la extensión)."),
    DOCUMENT_LINK_EXPIRED(HttpStatus.GONE, "El enlace de descarga caducó o no es válido. Genera uno nuevo."),

    // --- Alertas (RF-11) ----------------------------------------------
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "La alerta no existe."),
    ALERT_CRITICAL_PERSISTENT(HttpStatus.UNPROCESSABLE_ENTITY,
            "Las alertas críticas de cumplimiento no se pueden archivar."),

    // --- Genéricos ----------------------------------------------------
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Hay errores de validación en la solicitud."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "La operación no está permitida sobre este recurso."),
    CONFLICT(HttpStatus.CONFLICT, "El registro ya existe o fue modificado por otro usuario."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso solicitado no existe."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
