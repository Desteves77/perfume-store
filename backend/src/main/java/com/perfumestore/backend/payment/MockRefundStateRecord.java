package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record MockRefundStateRecord(String providerRef, UUID refundId, PaymentAttemptStatus status, BigDecimal amount,
        String currency, Instant occurredAt, Instant updatedAt) { }
