package com.perfumestore.backend.shipping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShippingQuoteRecord(UUID id, UUID userId, UUID addressId, String postalCode, String city, String state,
        boolean serviceable, String method, BigDecimal amount, String currency, int minDays, int maxDays,
        String ruleVersion, Instant expiresAt, Instant createdAt) { }
