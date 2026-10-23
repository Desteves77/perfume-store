package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.shared.api.ApiRequestException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test")
public class UnavailablePaymentProvider implements PaymentProvider {
    @Override
    public String code() { return "UNAVAILABLE"; }

    @Override
    public ProviderCheckoutSession createCheckout(UUID attemptId, UUID orderId, BigDecimal amount, String currency,
            Instant expiresAt) {
        throw new ApiRequestException(503, "PAYMENT_PROVIDER_UNAVAILABLE",
                "O provedor de pagamentos não está configurado neste ambiente.");
    }

    @Override
    public ProviderPaymentSnapshot getPaymentStatus(String providerPaymentId) {
        throw new ApiRequestException(503, "PAYMENT_PROVIDER_UNAVAILABLE",
                "O provedor de pagamentos não está configurado neste ambiente.");
    }
}
