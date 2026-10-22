package com.perfumestore.backend.inventory;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InventoryDtos {

    private InventoryDtos() { }

    public record AdjustmentRequest(
            @NotNull(message = "Informe a apresentação.") UUID presentationId,
            @NotNull(message = "Informe a quantidade.") Integer quantity,
            @NotNull(message = "Informe o tipo da movimentação.") InventoryMovementType type,
            @NotBlank(message = "Informe o motivo.")
            @Size(min = 3, max = 255, message = "O motivo deve ter entre 3 e 255 caracteres.") String reason) { }

    public record BalanceResponse(UUID presentationId, int physicalQuantity, int reservedQuantity,
            int availableQuantity, long version, Instant createdAt, Instant updatedAt) { }

    public record MovementResponse(UUID id, UUID presentationId, InventoryMovementType type, int physicalDelta,
            int reservedDelta, int physicalBefore, int physicalAfter, int reservedBefore, int reservedAfter,
            String reason, String operationKey, UUID reservationId, UUID actorUserId, String correlationId,
            Instant createdAt) { }

    public record ReservationResponse(UUID id, String reservationKey, UUID presentationId, int quantity,
            ReservationState state, UUID orderId, Instant expiresAt, Instant createdAt, Instant updatedAt,
            long version) { }
}
