package com.perfumestore.backend.shared.api;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages,
        List<SortOrder> sort) {

    public PageResponse {
        items = List.copyOf(items);
        sort = List.copyOf(sort);
    }

    public static int totalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }
}
