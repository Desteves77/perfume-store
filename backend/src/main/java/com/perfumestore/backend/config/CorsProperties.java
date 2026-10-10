package com.perfumestore.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("Accept", "Content-Type", "X-Correlation-Id", "X-CSRF-TOKEN",
            "Idempotency-Key");
    private List<String> exposedHeaders = List.of("X-Correlation-Id");
    private boolean allowCredentials;
    private long maxAge = 3600;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = List.copyOf(allowedMethods);
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = List.copyOf(allowedHeaders);
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = List.copyOf(exposedHeaders);
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
