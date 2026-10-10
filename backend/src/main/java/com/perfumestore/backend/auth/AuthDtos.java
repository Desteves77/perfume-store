package com.perfumestore.backend.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank(message = "Informe seu nome.")
            @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.")
            String name,
            @NotBlank(message = "Informe seu e-mail.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 320, message = "O e-mail é muito longo.")
            String email,
            @NotBlank(message = "Informe uma senha.")
            @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres.")
            String password) { }

    public record LoginRequest(
            @NotBlank(message = "Informe seu e-mail.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 320, message = "O e-mail é muito longo.")
            String email,
            @NotBlank(message = "Informe sua senha.")
            @Size(max = 72, message = "A senha é muito longa.")
            String password) { }

    public record UpdateProfileRequest(
            @NotBlank(message = "Informe seu nome.")
            @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.")
            String name) { }

    public record PasswordRecoveryRequest(
            @NotBlank(message = "Informe seu e-mail.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 320, message = "O e-mail é muito longo.")
            String email) { }

    public record PasswordResetRequest(
            @NotBlank(message = "Informe o token de recuperação.")
            String token,
            @NotBlank(message = "Informe uma nova senha.")
            @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres.")
            String password) { }

    public record UserResponse(UUID id, String name, String email, String role, Instant createdAt) {
        static UserResponse from(com.perfumestore.backend.user.AppUser user) {
            return new UserResponse(user.id(), user.displayName(), user.email(), user.role().name(), user.createdAt());
        }

        static UserResponse from(AppUserPrincipal principal, Instant createdAt) {
            return new UserResponse(principal.id(), principal.displayName(), principal.email(), principal.role(),
                    createdAt);
        }
    }

    public record MessageResponse(String message) { }
}
