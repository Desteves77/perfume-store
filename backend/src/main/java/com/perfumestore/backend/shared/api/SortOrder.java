package com.perfumestore.backend.shared.api;

public record SortOrder(String property, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }
}
