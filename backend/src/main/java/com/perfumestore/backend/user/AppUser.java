package com.perfumestore.backend.user;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
        UUID id,
        String displayName,
        String email,
        String emailNormalized,
        String passwordHash,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant passwordChangedAt) {
}
