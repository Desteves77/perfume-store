package com.perfumestore.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.config.AdminProvisioningProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.user.EmailNormalizer;
import com.perfumestore.backend.user.UserRepository;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProvisioningService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminProvisioningService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void provisionFirstAdmin(AdminProvisioningProperties properties) {
        if (properties.getName() == null || properties.getEmail() == null || properties.getPassword() == null
                || properties.getName().isBlank() || properties.getEmail().isBlank()
                || properties.getPassword().isBlank()) {
            throw new IllegalStateException("Provisionamento exige nome, e-mail e senha fornecidos externamente");
        }
        PasswordPolicy.check(properties.getPassword());
        String normalized = EmailNormalizer.normalize(properties.getEmail());
        if (users.findByEmailNormalized(normalized).isPresent()) {
            if (users.findByEmailNormalized(normalized).orElseThrow().role() == UserRole.ADMIN) {
                return;
            }
            throw new IllegalStateException("O e-mail informado já pertence a um CUSTOMER; promoção automática bloqueada");
        }
        if (users.countAdmins() > 0) {
            throw new IllegalStateException("Já existe um administrador; o primeiro provisionamento está encerrado");
        }
        try {
            users.insert(UUID.randomUUID(), properties.getName().trim(), properties.getEmail().trim(), normalized,
                    passwordEncoder.encode(properties.getPassword()), UserRole.ADMIN, UserStatus.ACTIVE, Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("O e-mail informado já foi cadastrado por outra execução", exception);
        }
    }
}
