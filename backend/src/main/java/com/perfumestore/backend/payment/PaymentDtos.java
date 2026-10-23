package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.shared.api.MoneyResponse;

public final class PaymentDtos {
    private PaymentDtos() { }

    public record PaymentResponse(UUID paymentId, String provider, String providerPaymentId, String status,
            MoneyResponse amount, String checkoutUrl, Instant createdAt, Instant updatedAt, Instant expiresAt) { }

    public record PaymentStatusResponse(UUID paymentId, String provider, String providerPaymentId, String status,
            MoneyResponse amount, Instant createdAt, Instant updatedAt, Instant expiresAt) { }

    public record WebhookResponse(String eventId, String paymentId, String processingStatus, String paymentStatus) { }

    public record ReconciliationResponse(UUID paymentId, String result, String providerStatus, String paymentStatus) { }

    public record EventPayload(String eventId, String eventType, String providerPaymentId, Instant occurredAt,
            BigDecimal amount, String currency) { }
}
