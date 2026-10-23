package com.perfumestore.backend.payment;

import java.time.Instant;
import java.util.UUID;

public record RefundHistoryRecord(String fromStatus, String toStatus, String event, UUID actorUserId,
        String providerRef, String reason, Instant createdAt) { }
