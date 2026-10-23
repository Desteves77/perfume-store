package com.perfumestore.backend.order;

import java.time.Instant;
record OrderHistoryRecord(String fromStatus, String toStatus, String event, Instant createdAt) { }
