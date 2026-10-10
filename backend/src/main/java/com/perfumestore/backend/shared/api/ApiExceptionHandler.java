package com.perfumestore.backend.shared.api;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiRequestException.class)
    ResponseEntity<ApiErrorResponse> handleApiRequest(ApiRequestException exception, HttpServletRequest request) {
        return response(request, exception.status(), exception.code(), exception.title(), exception.getMessage(),
                exception.fieldErrors(), exception.retryAfterSeconds());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleBodyValidation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.add(toFieldError(error)));
        exception.getBindingResult().getGlobalErrors().forEach(error -> errors.add(toGlobalError(error)));
        return response(request, 422, "VALIDATION_ERROR", "Dados inválidos",
                "Corrija os campos informados.", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();
            if (field == null || field.isBlank()) {
                field = "request";
            }
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new FieldErrorResponse(field, validationCode(error),
                        error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()));
            }
        }
        return response(request, 422, "VALIDATION_ERROR", "Dados inválidos",
                "Corrija os campos informados.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (message.contains("required request body is missing")) {
            return response(request, 400, "REQUEST_BODY_REQUIRED", "Corpo obrigatório",
                    "Envie o corpo da requisição.", null);
        }
        String code = hasCause(exception, InvalidFormatException.class)
                || hasCause(exception, MismatchedInputException.class)
                        ? "INVALID_JSON_TYPE"
                        : "MALFORMED_JSON";
        String clientMessage = "Envie um JSON válido no formato esperado.";
        return response(request, 400, code, "JSON inválido", clientMessage, null);
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class })
    ResponseEntity<ApiErrorResponse> handleParameterError(Exception exception, HttpServletRequest request) {
        return response(request, 400, "INVALID_PARAMETER", "Parâmetro inválido",
                "Verifique os parâmetros informados.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethod(HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(request, 405, "METHOD_NOT_ALLOWED", "Método não permitido",
                "O método HTTP não é permitido para este recurso.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(request, 415, "UNSUPPORTED_MEDIA_TYPE", "Tipo de mídia não suportado",
                "Use um tipo de conteúdo aceito pela API.", null);
    }

    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception, HttpServletRequest request) {
        return response(request, 404, "RESOURCE_NOT_FOUND", "Recurso não encontrado",
                "O recurso solicitado não foi encontrado.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        return response(request, 413, "IMAGE_TOO_LARGE", "Arquivo muito grande",
                "A imagem excede o tamanho máximo permitido.", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Falha interna correlationId={}", CorrelationIdFilter.currentId(request), exception);
        return response(request, 500, "INTERNAL_ERROR", "Erro interno",
                "Ocorreu um erro interno. Tente novamente mais tarde.", null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpServletRequest request, int status, String code,
            String title, String message, List<FieldErrorResponse> fieldErrors) {
        return response(request, status, code, title, message, fieldErrors, null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpServletRequest request, int status, String code,
            String title, String message, List<FieldErrorResponse> fieldErrors, Long retryAfterSeconds) {
        ApiErrorResponse body = ApiErrorResponses.create(request, status, code, title, message, fieldErrors);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(CorrelationIdFilter.HEADER_NAME, body.correlationId())
                .header("Cache-Control", "no-store");
        if (retryAfterSeconds != null) {
            builder.header("Retry-After", String.valueOf(retryAfterSeconds));
        }
        return builder.body(body);
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), validationCode(error.getCode()), error.getDefaultMessage());
    }

    private FieldErrorResponse toGlobalError(ObjectError error) {
        return new FieldErrorResponse("request", validationCode(error.getCode()), error.getDefaultMessage());
    }

    private String validationCode(String code) {
        if (code == null) {
            return "INVALID_VALUE";
        }
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
            case "Positive", "PositiveOrZero", "Min", "DecimalMin" -> "MUST_BE_POSITIVE";
            case "Max", "DecimalMax" -> "EXCEEDS_LIMIT";
            case "Size" -> "INVALID_LENGTH";
            case "Pattern", "Email" -> "INVALID_FORMAT";
            default -> "INVALID_VALUE";
        };
    }

    private String validationCode(MessageSourceResolvable error) {
        if (error.getCodes() != null) {
            for (String code : error.getCodes()) {
                String simpleCode = code.substring(code.lastIndexOf('.') + 1);
                String mapped = validationCode(simpleCode);
                if (!"INVALID_VALUE".equals(mapped)) {
                    return mapped;
                }
            }
        }
        return "INVALID_VALUE";
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
