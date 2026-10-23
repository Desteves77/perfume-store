package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderRecord(UUID id, String publicNumber, UUID userId, UUID cartId, String idempotencyKey,
        String requestFingerprint, OrderStatus status, PaymentStatus paymentStatus, DeliveryStatus deliveryStatus,
        BigDecimal subtotal, BigDecimal shippingAmount, BigDecimal total, String currency, Instant createdAt,
        Instant updatedAt, Instant paymentExpiresAt) { }
