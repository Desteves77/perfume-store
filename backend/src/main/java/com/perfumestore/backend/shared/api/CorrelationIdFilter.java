package com.perfumestore.backend.shared.api;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    private static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = accepted(request.getHeader(HEADER_NAME))
                ? request.getHeader(HEADER_NAME)
                : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(HEADER_NAME);
        }
    }

    public static String currentId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }

    private static boolean accepted(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!(character >= 'a' && character <= 'z')
                    && !(character >= 'A' && character <= 'Z')
                    && !(character >= '0' && character <= '9')
                    && character != '-' && character != '_' && character != '.' && character != ':') {
                return false;
            }
        }
        return true;
    }
}
