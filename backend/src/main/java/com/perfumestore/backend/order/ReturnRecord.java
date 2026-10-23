package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.UUID;

record ReturnRecord(UUID id, UUID orderId, UUID orderItemId, UUID presentationId, int quantity,
        int receivedQuantity, int approvedQuantity, int rejectedQuantity, String reason, ReturnStatus status,
        UUID requestedBy, UUID receivedBy, UUID inspectedBy, Instant requestedAt, Instant receivedAt,
        Instant inspectedAt, String conditionNote, String inspectionReason, String idempotencyKey, Instant updatedAt) { }
