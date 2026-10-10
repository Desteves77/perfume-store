package com.perfumestore.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.FieldErrorResponse;

public final class PasswordPolicy {

    private PasswordPolicy() { }

    public static void check(String password) {
        if (password == null || password.length() < 12 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiRequestException(422, "VALIDATION_ERROR", "Dados inválidos",
                    "Corrija os campos informados.", List.of(new FieldErrorResponse(
                            "password", "INVALID_LENGTH", "A senha deve ter entre 12 e 72 bytes UTF-8.")));
        }
    }
}
