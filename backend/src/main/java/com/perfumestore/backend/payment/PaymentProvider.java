package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentProvider {
    String code();

    ProviderCheckoutSession createCheckout(UUID attemptId, UUID orderId, BigDecimal amount, String currency,
            Instant expiresAt);

    ProviderPaymentSnapshot getPaymentStatus(String providerPaymentId);

    record ProviderCheckoutSession(String providerPaymentId, String checkoutSessionId, String checkoutUrl,
            PaymentAttemptStatus status, Instant expiresAt) { }

    record ProviderPaymentSnapshot(String providerPaymentId, PaymentAttemptStatus status, BigDecimal amount,
            String currency, Instant occurredAt, String eventId) { }
}
