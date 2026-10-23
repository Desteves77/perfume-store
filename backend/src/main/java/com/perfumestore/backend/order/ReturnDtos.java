package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ReturnDtos {
    private ReturnDtos() { }

    public record CreateRequest(@NotNull(message = "Informe o item do pedido.") UUID orderItemId,
            @NotNull(message = "Informe a quantidade.") @Positive(message = "A quantidade deve ser positiva.") Integer quantity,
            @NotBlank(message = "Informe o motivo da devoluÃ§Ã£o.") @Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record ReceiveRequest(@NotNull(message = "Informe a quantidade recebida.") @Positive(message = "A quantidade deve ser positiva.") Integer quantity,
            @Size(max = 255, message = "A condiÃ§Ã£o deve ter no mÃ¡ximo 255 caracteres.") String conditionNote) { }

    public record InspectRequest(@NotNull(message = "Informe a quantidade aprovada.") @jakarta.validation.constraints.PositiveOrZero(message = "A quantidade aprovada nÃ£o pode ser negativa.") Integer approvedQuantity,
            @NotNull(message = "Informe a quantidade rejeitada.") @jakarta.validation.constraints.PositiveOrZero(message = "A quantidade rejeitada nÃ£o pode ser negativa.") Integer rejectedQuantity,
            @NotBlank(message = "Informe o resultado da inspeÃ§Ã£o.") @Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record RestockRequest(@NotNull(message = "Informe a quantidade a repor.") @Positive(message = "A quantidade deve ser positiva.") Integer quantity) { }

    public record Summary(UUID id, UUID orderId, UUID orderItemId, UUID presentationId, int quantity,
            int receivedQuantity, int approvedQuantity, int rejectedQuantity, ReturnStatus status,
            String reason, Instant requestedAt, Instant receivedAt, Instant inspectedAt) { }

    public record Detail(Summary summary, List<ReturnHistoryRecord> history, int restockedQuantity) { }
}
