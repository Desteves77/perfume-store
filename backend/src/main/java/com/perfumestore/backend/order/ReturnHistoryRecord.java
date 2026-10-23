package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.UUID;

public record ReturnHistoryRecord(String fromStatus, String toStatus, String event, UUID actorUserId, String reason,
        Instant createdAt) { }
