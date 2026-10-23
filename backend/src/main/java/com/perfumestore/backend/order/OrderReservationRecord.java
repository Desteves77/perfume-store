package com.perfumestore.backend.order;

import java.util.UUID;
record OrderReservationRecord(UUID reservationId, String reservationKey, UUID presentationId, int quantity) { }
