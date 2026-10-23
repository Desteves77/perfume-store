package com.perfumestore.backend.notification;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() { }

    public record Summary(UUID id, String eventType, String status, UUID orderId, UUID userId,
            String recipientEmail, int attemptCount, Instant nextAttemptAt, Instant createdAt, Instant sentAt,
            String lastErrorCode) { }
}
