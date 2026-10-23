package com.perfumestore.backend.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(UUID id, NotificationType eventType, String eventKey, UUID orderId, UUID userId,
        String recipientEmail, String templateName, String templateVersion, String subject, String bodyText, String bodyHtml,
        NotificationStatus status, int attemptCount, Instant nextAttemptAt, Instant createdAt, Instant updatedAt,
        Instant lastAttemptAt, Instant sentAt, Instant lockedUntil, String lastErrorCode) { }
