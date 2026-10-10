package com.perfumestore.backend.catalog;

import java.time.Instant;
import java.util.UUID;

record CategoryRecord(UUID id, String name, String slug, boolean active, long version,
        Instant createdAt, Instant updatedAt) { }
