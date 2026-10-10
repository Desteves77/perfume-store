package com.perfumestore.backend.shared.api;

import java.util.List;

public class ApiRequestException extends RuntimeException {

    private final int status;
    private final String code;
    private final String title;
    private final List<FieldErrorResponse> fieldErrors;

    public ApiRequestException(int status, String code, String message) {
        this(status, code, ApiErrorResponses.titleFor(status), message, List.of());
    }

    public ApiRequestException(int status, String code, String title, String message,
            List<FieldErrorResponse> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.title = title;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public List<FieldErrorResponse> fieldErrors() {
        return fieldErrors;
    }
}
