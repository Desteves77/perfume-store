package com.perfumestore.backend.cart;

import java.time.Instant;
import java.util.UUID;

record CartRecord(UUID id, UUID userId, String visitorTokenHash, CartState state, Instant expiresAt,
        Instant lastActivityAt, long version, Instant createdAt, Instant updatedAt) { }
