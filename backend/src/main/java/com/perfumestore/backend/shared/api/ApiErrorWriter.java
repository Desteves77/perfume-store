package com.perfumestore.backend.shared.api;

import java.io.IOException;

import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorWriter {

    private final JsonMapper objectMapper;

    public ApiErrorWriter(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String code,
            String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ApiErrorResponse body = ApiErrorResponses.create(request, status, code,
                ApiErrorResponses.titleFor(status), message, null);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(CorrelationIdFilter.HEADER_NAME, body.correlationId());
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
