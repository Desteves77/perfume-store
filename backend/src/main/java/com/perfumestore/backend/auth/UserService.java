package com.perfumestore.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.EmailNormalizer;
import com.perfumestore.backend.user.UserRepository;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;
import com.perfumestore.backend.shared.api.ApiRequestException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerCustomer(String name, String email, String password) {
        PasswordPolicy.check(password);
        String normalized = EmailNormalizer.normalize(email);
        Instant now = Instant.now();
        try {
            return users.insert(UUID.randomUUID(), name, email.trim(), normalized,
                    passwordEncoder.encode(password), UserRole.CUSTOMER, UserStatus.ACTIVE, now);
        } catch (DuplicateKeyException exception) {
            throw new ApiRequestException(409, "EMAIL_ALREADY_REGISTERED",
                    "Este e-mail já está cadastrado.");
        }
    }

    @Transactional
    public AppUser updateDisplayName(UUID userId, String name) {
        return users.updateDisplayName(userId, name, Instant.now())
                .orElseThrow(() -> new ApiRequestException(404, "RESOURCE_NOT_FOUND",
                        "O usuário solicitado não foi encontrado."));
    }

    public AppUser findRequired(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiRequestException(401, "AUTHENTICATION_REQUIRED",
                        "É necessário autenticar para acessar este recurso."));
    }

    public AppUser findByEmail(String email) {
        return users.findByEmailNormalized(EmailNormalizer.normalize(email)).orElse(null);
    }

    @Transactional
    public AppUser changePassword(UUID userId, String password) {
        PasswordPolicy.check(password);
        return users.updatePassword(userId, passwordEncoder.encode(password), Instant.now())
                .orElseThrow(() -> new ApiRequestException(404, "RESOURCE_NOT_FOUND",
                        "O usuário solicitado não foi encontrado."));
    }

    public long countAdmins() {
        return users.countAdmins();
    }
}
