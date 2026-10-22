package com.perfumestore.backend.address;

import java.time.Instant;
import java.util.UUID;

record AddressRecord(
        UUID id,
        UUID userId,
        String recipientName,
        String label,
        String postalCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String reference,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
