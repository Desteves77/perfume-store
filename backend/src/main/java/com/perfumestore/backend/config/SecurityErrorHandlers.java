package com.perfumestore.backend.config;

import com.perfumestore.backend.shared.api.ApiErrorWriter;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandlers {

    private final ApiErrorWriter writer;

    public SecurityErrorHandlers(ApiErrorWriter writer) {
        this.writer = writer;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> writer.write(request, response, 401,
                "AUTHENTICATION_REQUIRED", "É necessário autenticar para acessar este recurso.");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            String code = exception instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN";
            String message = exception instanceof CsrfException
                    ? "O token de segurança da requisição é inválido ou ausente."
                    : "Você não tem permissão para acessar este recurso.";
            writer.write(request, response, 403, code, message);
        };
    }
}
