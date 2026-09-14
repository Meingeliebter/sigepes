package com.sigepes.backend.common;

/** Excepción de negocio con un {@link ErrorCode} asociado. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static ApiException of(ErrorCode code) {
        return new ApiException(code);
    }

    public static ApiException of(ErrorCode code, String message) {
        return new ApiException(code, message);
    }
}
