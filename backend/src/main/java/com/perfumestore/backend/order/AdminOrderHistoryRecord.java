package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.UUID;

record AdminOrderHistoryRecord(String operation, UUID actorUserId, String fromOrderStatus, String toOrderStatus,
        String fromDeliveryStatus, String toDeliveryStatus, String reason, String externalReference,
        Instant createdAt) { }
