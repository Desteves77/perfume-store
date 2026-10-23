package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record RefundRecord(UUID id, UUID orderId, UUID paymentAttemptId, BigDecimal amount, String currency, String reason,
        RefundStatus status, String idempotencyKey, String requestFingerprint, String providerRef, UUID requestedBy,
        Instant requestedAt, Instant updatedAt, Instant completedAt) { }
