package com.itzetech.dpdms.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** An error the client should see, with a stable code the frontend can react to. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fields;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    /** Field name to message, for {@code VALIDATION_FAILED} errors found by the service rather than by bean validation. */
    public Map<String, String> fields() {
        return fields;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    /** A 400 {@code VALIDATION_FAILED} that points at one field. */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please check the highlighted fields.", Map.of(field, message));
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
