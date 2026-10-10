package com.perfumestore.backend.catalog;

import java.time.Instant;
import java.util.UUID;

record ImageRecord(UUID id, UUID productId, String storageKey, String contentType, long byteSize,
        int width, int height, String altText, int position, boolean primary,
        Instant createdAt, Instant updatedAt) { }
