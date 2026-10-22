package com.perfumestore.backend.shipping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.shared.api.DecimalStringSerializer;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonSerialize;

public final class ShippingDtos {

    private ShippingDtos() {
    }

    public record QuoteRequest(@NotNull(message = "Informe o endereço de entrega.") UUID addressId) {
    }

    public record EstimatedDelivery(int minDays, int maxDays, String dayType) {
    }

    public record QuoteResponse(UUID quoteId, boolean serviceable, String method,
            @JsonSerialize(using = DecimalStringSerializer.class) BigDecimal amount, String currency,
            EstimatedDelivery estimatedDelivery, String ruleVersion, Instant expiresAt) {
    }
}
