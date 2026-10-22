package com.perfumestore.backend.shared.api;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiErrorResponses {

    private ApiErrorResponses() {
    }

    public static ApiErrorResponse create(HttpServletRequest request, int status, String code, String title,
            String message, List<FieldErrorResponse> fieldErrors) {
        String correlationId = CorrelationIdFilter.currentId(request);
        String type = "https://api.perfumestore.local/problems/"
                + code.toLowerCase(Locale.ROOT).replace('_', '-');
        return new ApiErrorResponse(type, title, status, code, message, correlationId, fieldErrors);
    }

    public static String titleFor(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 401 -> "Autenticação necessária";
            case 403 -> "Acesso negado";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 415 -> "Tipo de mídia não suportado";
            case 422 -> "Dados inválidos";
            case 429 -> "Muitas tentativas";
            case 503 -> "Serviço indisponível";
            default -> status >= 500 ? "Erro interno" : "Erro na requisição";
        };
    }
}
