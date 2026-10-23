package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptRecord(UUID id, UUID orderId, String provider, String providerPaymentId,
        String checkoutSessionId, String idempotencyKey, String requestFingerprint, BigDecimal amount,
        String currency, PaymentAttemptStatus status, String errorCode, String errorMessage, Instant createdAt,
        Instant updatedAt, Instant completedAt, Instant expiresAt, Instant lastProviderCheckAt) { }
