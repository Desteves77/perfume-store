package com.perfumestore.backend.shared.inventory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Read-only port used by catalog responses without coupling catalog to inventory storage. */
public interface InventoryAvailabilityReader {

    Map<UUID, Integer> availableByPresentationIds(Collection<UUID> presentationIds);
}
