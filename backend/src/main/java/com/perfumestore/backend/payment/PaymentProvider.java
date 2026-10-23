package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentProvider {
    String code();

    ProviderCheckoutSession createCheckout(UUID attemptId, UUID orderId, BigDecimal amount, String currency,
            Instant expiresAt);

    ProviderPaymentSnapshot getPaymentStatus(String providerPaymentId);

    default ProviderRefundResult createRefund(UUID refundId, String providerPaymentId, BigDecimal amount,
            String currency, Instant expiresAt) {
        throw new com.perfumestore.backend.shared.api.ApiRequestException(503, "REFUND_PROVIDER_UNAVAILABLE",
                "O provedor de reembolso nao esta disponivel neste ambiente.");
    }

    default ProviderRefundSnapshot getRefundStatus(String providerRefundId) {
        throw new com.perfumestore.backend.shared.api.ApiRequestException(503, "REFUND_PROVIDER_UNAVAILABLE",
                "O provedor de reembolso nao esta disponivel neste ambiente.");
    }

    default void simulateRefund(String providerRefundId, PaymentAttemptStatus status, BigDecimal amount,
            String currency) {
        throw new com.perfumestore.backend.shared.api.ApiRequestException(503, "REFUND_PROVIDER_UNAVAILABLE",
                "A simulacao de reembolso nao esta disponivel neste ambiente.");
    }

    record ProviderCheckoutSession(String providerPaymentId, String checkoutSessionId, String checkoutUrl,
            PaymentAttemptStatus status, Instant expiresAt) { }

    record ProviderPaymentSnapshot(String providerPaymentId, PaymentAttemptStatus status, BigDecimal amount,
            String currency, Instant occurredAt, String eventId) { }

    record ProviderRefundResult(String providerRefundId, PaymentAttemptStatus status, Instant expiresAt) { }

    record ProviderRefundSnapshot(String providerRefundId, PaymentAttemptStatus status, BigDecimal amount,
            String currency, Instant occurredAt) { }
}
