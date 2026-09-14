package com.sigepes.backend.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Traduce toda excepción a la forma de error única de la API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApi(ApiException exception, HttpServletRequest request) {
        return respond(exception.code(), exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        String message = fieldErrors.isEmpty()
                ? ErrorCode.VALIDATION_ERROR.defaultMessage()
                : fieldErrors.get(0).field() + ": " + fieldErrors.get(0).message();
        return respond(ErrorCode.VALIDATION_ERROR, message, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        // El detalle no viaja al cliente, pero sin él es imposible depurar un 400 de este tipo.
        log.warn("Cuerpo ilegible en {} {}: {}", request.getMethod(), request.getRequestURI(),
                exception.getMostSpecificCause().getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, "El cuerpo de la solicitud no es válido.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return respond(ErrorCode.FORBIDDEN, null, request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception,
            HttpServletRequest request) {
        return respond(ErrorCode.UNAUTHORIZED, null, request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return respond(ErrorCode.NOT_FOUND, null, request, null);
    }

    /** La bitácora de auditoría, por ejemplo, solo admite GET: el resto debe ser un 405 limpio. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethod(HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED,
                "El método " + exception.getMethod() + " no está permitido sobre este recurso.", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_ERROR,
                "Falta el parámetro obligatorio «" + exception.getParameterName() + "».", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_ERROR,
                "El valor del parámetro «" + exception.getName() + "» no es válido.", request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond(ErrorCode.INTERNAL_ERROR, null, request, null);
    }

    private ResponseEntity<ApiErrorResponse> respond(ErrorCode code, String message, HttpServletRequest request,
            List<ApiErrorResponse.FieldError> fieldErrors) {
        HttpStatus status = code.status();
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code, message, request.getRequestURI(), fieldErrors));
    }
}
