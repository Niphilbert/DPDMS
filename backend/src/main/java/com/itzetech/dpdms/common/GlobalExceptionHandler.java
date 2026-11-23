package com.itzetech.dpdms.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErrorBody(String code, String message, Map<String, String> fields) {
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ErrorBody(e.code(), e.getMessage(), e.fields()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_FAILED", "Please check the highlighted fields.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", "The request could not be read.", null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorBody> badParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", "The value for \"" + e.getName() + "\" is not valid.", null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorBody> missingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", "The value for \"" + e.getParameterName() + "\" is missing.", null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorBody> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ErrorBody("METHOD_NOT_ALLOWED", "This request method is not supported here.", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorBody("FORBIDDEN", "You do not have access to this.", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> other(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorBody("SERVER_ERROR", "Something went wrong. Please try again.", null));
    }
}
