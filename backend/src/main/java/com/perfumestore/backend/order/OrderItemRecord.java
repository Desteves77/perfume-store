package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.util.UUID;

record OrderItemRecord(UUID id, UUID productId, UUID presentationId, String sku, String productName, BigDecimal volume,
        String unit, int quantity, BigDecimal unitPrice, BigDecimal lineTotal, String currency) { }
