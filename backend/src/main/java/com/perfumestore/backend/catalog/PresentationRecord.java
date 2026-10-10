package com.perfumestore.backend.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record PresentationRecord(UUID id, UUID productId, String sku, String skuNormalized,
        BigDecimal volume, String unit, BigDecimal price, String currency, boolean active,
        long version, Instant createdAt, Instant updatedAt) { }
