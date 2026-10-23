package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.payment.RefundDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminOrderDtos {
    private AdminOrderDtos() { }

    public record Summary(String publicNumber, UUID customerId, Instant createdAt, OrderStatus status,
            PaymentStatus paymentStatus, DeliveryStatus deliveryStatus, com.perfumestore.backend.shared.api.MoneyResponse total,
            int itemCount) { }

    public record Customer(UUID id, String displayName, String email) { }

    public record Logistics(String carrier, String trackingCode, Instant shippedAt, UUID shippedBy,
            Instant deliveredAt, UUID deliveredBy) { }

    public record History(String operation, UUID actorUserId, String fromOrderStatus, String toOrderStatus,
            String fromDeliveryStatus, String toDeliveryStatus, String reason, String externalReference,
            Instant createdAt) { }

    public record Reservation(String reservationKey, UUID presentationId, int quantity, String state) { }

    public record Detail(OrderDtos.OrderResponse order, Customer customer, Logistics logistics,
            List<History> administrationHistory, List<Reservation> reservations,
            List<RefundDtos.Summary> refunds, List<ReturnDtos.Summary> returns) { }

    public record PrepareRequest(@Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record ShipRequest(@NotBlank(message = "Informe a transportadora ou responsÃ¡vel pela entrega.")
            @Size(max = 120, message = "A transportadora deve ter no mÃ¡ximo 120 caracteres.") String carrier,
            @Size(min = 3, max = 120, message = "O rastreamento deve ter entre 3 e 120 caracteres.") String trackingCode,
            @Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record DeliverRequest(@Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }

    public record CancelRequest(@NotBlank(message = "Informe o motivo do cancelamento.")
            @Size(max = 255, message = "O motivo deve ter no mÃ¡ximo 255 caracteres.") String reason) { }
}
