package com.sigepes.backend.common;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo único de error de la API.
 *
 * <p>Mantiene a la vez la forma anidada {@code error: {code, message}} y los campos planos
 * {@code code} / {@code message} / {@code fieldErrors}, que son los que lee el cliente existente
 * ({@code shared/errors/AppError.ts} y las páginas de login y registro).
 */
public record ApiErrorResponse(
        boolean success,
        String timestamp,
        int status,
        String path,
        String code,
        String message,
        List<FieldError> fieldErrors,
        ErrorBody error) {

    public record FieldError(String field, String message) {
    }

    public record ErrorBody(String code, String message) {
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path, List<FieldError> fieldErrors) {
        String finalMessage = message == null || message.isBlank() ? code.defaultMessage() : message;
        return new ApiErrorResponse(
                false,
                Instant.now().toString(),
                code.status().value(),
                path,
                code.name(),
                finalMessage,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors,
                new ErrorBody(code.name(), finalMessage));
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return of(code, message, path, null);
    }
}
