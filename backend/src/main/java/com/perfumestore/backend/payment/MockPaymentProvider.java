package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.config.PaymentProperties;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockPaymentProvider implements PaymentProvider {
    private final PaymentRepository payments;
    private final PaymentProperties properties;

    public MockPaymentProvider(PaymentRepository payments, PaymentProperties properties) {
        this.payments = payments;
        this.properties = properties;
    }

    @Override
    public String code() { return "MOCK"; }

    @Override
    public ProviderCheckoutSession createCheckout(UUID attemptId, UUID orderId, BigDecimal amount, String currency,
            Instant expiresAt) {
        String providerPaymentId = "mock-pay-" + attemptId.toString().replace("-", "");
        String checkoutSessionId = "mock-session-" + attemptId.toString().replace("-", "");
        String base = properties.getMockCheckoutBaseUrl();
        String checkoutUrl = base.endsWith("/") ? base + checkoutSessionId : base + "/" + checkoutSessionId;
        payments.ensureMockState(providerPaymentId, attemptId, PaymentAttemptStatus.PENDING, amount, currency,
                Instant.now(), Instant.now());
        return new ProviderCheckoutSession(providerPaymentId, checkoutSessionId, checkoutUrl,
                PaymentAttemptStatus.PENDING, expiresAt);
    }

    @Override
    public ProviderPaymentSnapshot getPaymentStatus(String providerPaymentId) {
        return payments.findMockState(providerPaymentId)
                .map(state -> new ProviderPaymentSnapshot(providerPaymentId,
                        PaymentAttemptStatus.valueOf(state.status()), state.amount(), state.currency(),
                        state.occurredAt(), state.lastEventId()))
                .orElse(new ProviderPaymentSnapshot(providerPaymentId, PaymentAttemptStatus.UNKNOWN, null, null,
                        Instant.now(), null));
    }
}
