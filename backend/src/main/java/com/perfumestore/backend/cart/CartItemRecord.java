package com.perfumestore.backend.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record CartItemRecord(UUID id, UUID cartId, UUID presentationId, int quantity, BigDecimal priceReference,
        long version, Instant createdAt, Instant updatedAt) { }
