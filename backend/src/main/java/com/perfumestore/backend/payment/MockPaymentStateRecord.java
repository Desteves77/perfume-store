package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record MockPaymentStateRecord(String providerPaymentId, java.util.UUID paymentAttemptId, String status,
        BigDecimal amount, String currency, String lastEventId, Instant occurredAt, Instant createdAt,
        Instant updatedAt) { }
