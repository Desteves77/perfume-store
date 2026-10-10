package com.perfumestore.backend.catalog;

import java.time.Instant;
import java.util.UUID;

record ProductRecord(UUID id, String slug, String name, String brand, String description,
        UUID categoryId, String categoryName, String categorySlug, boolean categoryActive, long categoryVersion,
        Instant categoryCreatedAt, Instant categoryUpdatedAt,
        boolean active, long version, Instant createdAt, Instant updatedAt) {

    boolean published(boolean hasPresentation, boolean hasPrimaryImage) {
        return active && categoryActive && hasPresentation && hasPrimaryImage;
    }
}
