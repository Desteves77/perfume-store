package com.perfumestore.backend.inventory;

import java.time.Instant;
import java.util.UUID;

record StockReservationRecord(UUID id, String reservationKey, UUID presentationId, int quantity,
        ReservationState state, UUID orderId, Instant expiresAt, Instant createdAt, Instant updatedAt, long version) { }
