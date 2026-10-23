package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RefundDtos {
    private RefundDtos() { }

    public record CreateRequest(@NotBlank(message = "Informe o motivo do reembolso.")
            @Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record Summary(UUID id, UUID orderId, UUID paymentAttemptId, MoneyResponse amount, RefundStatus status,
            String reason, String providerRef, UUID requestedBy, Instant requestedAt, Instant updatedAt,
            Instant completedAt) { }

    public record Detail(Summary summary, List<RefundHistoryRecord> history) { }

    public record SimulateRequest(String outcome) { }
}
