package com.perfumestore.backend.shared.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiErrorResponse(
        String type,
        String title,
        int status,
        String code,
        String message,
        String correlationId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorResponse> fieldErrors) {

    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
