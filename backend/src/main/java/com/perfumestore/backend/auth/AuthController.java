package com.perfumestore.backend.auth;

import com.perfumestore.backend.config.RateLimitProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final UserService users;
    private final AuthenticationService authentication;
    private final PasswordRecoveryService recovery;
    private final AttemptRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public AuthController(UserService users, AuthenticationService authentication,
            PasswordRecoveryService recovery, AttemptRateLimiter rateLimiter, RateLimitProperties rateLimitProperties) {
        this.users = users;
        this.authentication = authentication;
        this.recovery = recovery;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.UserResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.check("register:" + remoteAddress(httpRequest), rateLimitProperties.getRegisterMaxAttempts(),
                rateLimitProperties.getRegisterWindow());
        return ResponseEntity.status(201)
                .body(AuthDtos.UserResponse.from(users.registerCustomer(request.name(), request.email(),
                        request.password())));
    }

    @PostMapping("/login")
    public AuthDtos.UserResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) {
        return authentication.login(request.email(), request.password(), httpRequest, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication principal, HttpServletRequest request,
            HttpServletResponse response) {
        authentication.logout(request, response, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(Authentication authentication) {
        return this.authentication.current(principal(authentication));
    }

    @PatchMapping("/me")
    public AuthDtos.UserResponse update(@Valid @RequestBody AuthDtos.UpdateProfileRequest request,
            Authentication authentication) {
        AppUserPrincipal principal = principal(authentication);
        return AuthDtos.UserResponse.from(users.updateDisplayName(principal.id(), request.name()));
    }

    @PostMapping("/password-recovery-requests")
    public ResponseEntity<AuthDtos.MessageResponse> requestRecovery(
            @Valid @RequestBody AuthDtos.PasswordRecoveryRequest request, HttpServletRequest httpRequest) {
        recovery.request(request.email(), remoteAddress(httpRequest));
        return ResponseEntity.accepted().body(new AuthDtos.MessageResponse(
                "Se o e-mail estiver cadastrado, enviaremos instruções para recuperar o acesso."));
    }

    @PostMapping("/password-resets")
    public ResponseEntity<Void> reset(@Valid @RequestBody AuthDtos.PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        recovery.reset(request.token(), request.password(), remoteAddress(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private AppUserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ApiRequestException(401, "AUTHENTICATION_REQUIRED",
                    "É necessário autenticar para acessar este recurso.");
        }
        return principal;
    }

    private String remoteAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
