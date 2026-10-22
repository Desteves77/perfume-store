package com.perfumestore.backend.inventory;

import java.time.Instant;
import java.util.UUID;

record InventoryBalanceRecord(UUID presentationId, int physicalQuantity, int reservedQuantity, long version,
        Instant createdAt, Instant updatedAt) {

    int availableQuantity() {
        return physicalQuantity - reservedQuantity;
    }
}
