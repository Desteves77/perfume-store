package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEventRecord(UUID id, String provider, String providerEventId, String eventType,
        String providerPaymentId, Instant occurredAt, Instant receivedAt, BigDecimal amount, String currency,
        String payloadHash, boolean signatureValid, PaymentEventProcessingStatus processingStatus,
        String processingErrorCode, Instant processedAt) { }
