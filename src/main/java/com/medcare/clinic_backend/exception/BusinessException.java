package com.medcare.clinic_backend.exception;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fieldErrors;

    public BusinessException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public BusinessException(HttpStatus status, String message, String code) {
        this(status, message, code, null);
    }

    public BusinessException(HttpStatus status, String message, String code, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fieldErrors));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
