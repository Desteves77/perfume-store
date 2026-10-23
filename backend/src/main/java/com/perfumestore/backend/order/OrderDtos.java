package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.shared.api.DecimalStringSerializer;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonSerialize;

public final class OrderDtos {
    private OrderDtos() { }

    public record CheckoutRequest(@NotNull(message = "Informe o endereço de entrega.") UUID addressId,
            @NotNull(message = "Informe a cotação de frete.") UUID quoteId) { }

    public record OrderItemResponse(UUID presentationId, UUID productId, String sku, String productName,
            BigDecimal volume, String unit, int quantity, MoneyResponse unitPrice, MoneyResponse lineTotal) { }

    public record AddressSnapshot(String recipientName, String label, String postalCode, String street, String number,
            String complement, String neighborhood, String city, String state, String reference) { }

    public record ShippingSnapshot(UUID quoteId, String method, MoneyResponse amount, int minDays, int maxDays,
            String dayType, String ruleVersion) { }

    public record HistoryResponse(String fromStatus, String toStatus, String event, Instant createdAt) { }

    public record OrderResponse(String publicNumber, Instant createdAt, Instant updatedAt, OrderStatus status,
            PaymentStatus paymentStatus, DeliveryStatus deliveryStatus, MoneyResponse subtotal, MoneyResponse shipping,
            MoneyResponse total, Instant paymentExpiresAt, List<OrderItemResponse> items, AddressSnapshot address,
            ShippingSnapshot shippingSnapshot, List<HistoryResponse> history) { }

    public record OrderSummary(String publicNumber, Instant createdAt, OrderStatus status, PaymentStatus paymentStatus,
            DeliveryStatus deliveryStatus, MoneyResponse total, int itemCount) { }

    public record SimulatePaymentRequest(SimulatedOutcome outcome) { }
    public enum SimulatedOutcome { APPROVED, REJECTED, FAILED }
}
