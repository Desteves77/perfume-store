package com.perfumestore.backend.shared.api;

import java.util.List;
import java.util.Map;

public final class SortParser {

    private SortParser() {
    }

    public static SortOrder parse(String raw, Map<String, String> allowedFields, String tieBreaker) {
        if (raw == null || raw.isBlank()) {
            return new SortOrder(tieBreaker, SortOrder.Direction.ASC);
        }
        String[] pieces = raw.split(",", -1);
        if (pieces.length != 2 || pieces[0].isBlank() || pieces[1].isBlank()) {
            throw invalid(raw);
        }
        String property = allowedFields.get(pieces[0].trim());
        if (property == null) {
            throw invalid(raw);
        }
        SortOrder.Direction direction;
        try {
            direction = SortOrder.Direction.valueOf(pieces[1].trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw invalid(raw);
        }
        return new SortOrder(property, direction);
    }

    public static List<SortOrder> withTieBreaker(SortOrder requested, String tieBreaker) {
        if (requested.property().equals(tieBreaker)) {
            return List.of(requested);
        }
        return List.of(requested, new SortOrder(tieBreaker, SortOrder.Direction.ASC));
    }

    private static ApiRequestException invalid(String raw) {
        return new ApiRequestException(400, "INVALID_SORT",
                "A ordenação solicitada não é permitida.");
    }
}
