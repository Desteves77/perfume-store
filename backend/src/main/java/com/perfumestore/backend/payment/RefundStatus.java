package com.perfumestore.backend.payment;

public enum RefundStatus {
    REQUESTED,
    PENDING_PROVIDER,
    CONFIRMED,
    FAILED,
    REVIEW_REQUIRED
}
