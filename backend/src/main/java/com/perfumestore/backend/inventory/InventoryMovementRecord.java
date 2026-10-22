package com.perfumestore.backend.inventory;

import java.time.Instant;
import java.util.UUID;

record InventoryMovementRecord(UUID id, UUID presentationId, InventoryMovementType type, int physicalDelta,
        int reservedDelta, int physicalBefore, int physicalAfter, int reservedBefore, int reservedAfter,
        String reason, String operationKey, UUID reservationId, UUID actorUserId, String correlationId,
        Instant createdAt) { }
