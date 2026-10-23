package com.perfumestore.backend.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.shared.api.MoneyResponse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() { }

    public record AddItemRequest(
            @NotNull(message = "Informe a apresentação.") UUID presentationId,
            @NotNull(message = "Informe a quantidade.") @Min(value = 1, message = "A quantidade deve ser positiva.") Integer quantity) { }

    public record UpdateItemRequest(
            @NotNull(message = "Informe a quantidade.") @Min(value = 1, message = "A quantidade deve ser positiva.") Integer quantity,
            @NotNull(message = "Informe a versão atual do item.") Long version) { }

    public record Availability(String status, int availableQuantity) { }

    public record Issue(String code, String message) { }

    public record ItemResponse(UUID id, UUID presentationId, UUID productId, String productName, String productSlug,
            String sku, BigDecimal volume, String unit, int quantity, MoneyResponse unitPrice, MoneyResponse lineTotal,
            Availability availability, List<Issue> issues, long version) { }

    public record CartResponse(UUID id, CartState state, List<ItemResponse> items, MoneyResponse subtotal,
            boolean canCheckout, List<Issue> issues, long version, Instant expiresAt, Instant lastActivityAt) {
        public static CartResponse empty() {
            return new CartResponse(null, CartState.ACTIVE, List.of(), new MoneyResponse(BigDecimal.ZERO.setScale(2), "BRL"),
                    false, List.of(), 0, null, null);
        }
    }
}
