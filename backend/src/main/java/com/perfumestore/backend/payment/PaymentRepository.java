package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<PaymentAttemptRecord> findByOrderAndKeyForUpdate(UUID orderId, String key) {
        return query("select * from perfumestore.payment_attempts where order_id = ? and idempotency_key = ? for update", orderId, key).stream().findFirst();
    }

    public Optional<PaymentAttemptRecord> findLatestForOrderForUpdate(UUID orderId) {
        return query("select * from perfumestore.payment_attempts where order_id = ? order by created_at desc, id desc limit 1 for update", orderId).stream().findFirst();
    }

    public Optional<PaymentAttemptRecord> findByIdForUpdate(UUID id) {
        return query("select * from perfumestore.payment_attempts where id = ? for update", id).stream().findFirst();
    }

    public Optional<PaymentAttemptRecord> findByProviderPaymentIdForUpdate(String provider, String providerPaymentId) {
        return query("select * from perfumestore.payment_attempts where provider = ? and provider_payment_id = ? for update", provider, providerPaymentId).stream().findFirst();
    }

    public Optional<PaymentAttemptRecord> findApprovedForOrderForUpdate(UUID orderId) {
        return query("select * from perfumestore.payment_attempts where order_id = ? and status = 'APPROVED' order by completed_at desc nulls last, id desc limit 1 for update", orderId).stream().findFirst();
    }

    public void ensureMockRefundState(String providerRef, UUID refundId, PaymentAttemptStatus status,
            BigDecimal amount, String currency, Instant now) {
        jdbc.update("""
                insert into perfumestore.mock_refund_states
                    (provider_ref, refund_id, status, amount, currency, occurred_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (provider_ref) do nothing
                """, providerRef, refundId, status.name(), amount, currency, Timestamp.from(now), Timestamp.from(now));
    }

    public void updateMockRefundState(String providerRef, PaymentAttemptStatus status, BigDecimal amount,
            String currency, Instant now) {
        jdbc.update("update perfumestore.mock_refund_states set status = ?, amount = ?, currency = ?, occurred_at = ?, updated_at = ? where provider_ref = ?",
                status.name(), amount, currency, Timestamp.from(now), Timestamp.from(now), providerRef);
    }

    public Optional<MockRefundStateRecord> findMockRefundState(String providerRef) {
        return jdbc.query("select * from perfumestore.mock_refund_states where provider_ref = ?", (rs, row) ->
                new MockRefundStateRecord(rs.getString("provider_ref"), rs.getObject("refund_id", UUID.class),
                        PaymentAttemptStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount"),
                        rs.getString("currency"), rs.getTimestamp("occurred_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()), providerRef).stream().findFirst();
    }

    public boolean insertAttempt(PaymentAttemptRecord attempt) {
        return jdbc.update("""
                insert into perfumestore.payment_attempts
                    (id, order_id, provider, provider_payment_id, checkout_session_id, idempotency_key,
                     request_fingerprint, amount, currency, status, error_code, error_message, created_at,
                     updated_at, completed_at, expires_at, last_provider_check_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (order_id, idempotency_key) do nothing
                """, attempt.id(), attempt.orderId(), attempt.provider(), attempt.providerPaymentId(),
                attempt.checkoutSessionId(), attempt.idempotencyKey(), attempt.requestFingerprint(), attempt.amount(),
                attempt.currency(), attempt.status().name(), attempt.errorCode(), attempt.errorMessage(),
                Timestamp.from(attempt.createdAt()), Timestamp.from(attempt.updatedAt()),
                attempt.completedAt() == null ? null : Timestamp.from(attempt.completedAt()), Timestamp.from(attempt.expiresAt()),
                attempt.lastProviderCheckAt() == null ? null : Timestamp.from(attempt.lastProviderCheckAt())) == 1;
    }

    public void attachProvider(UUID id, String providerPaymentId, String checkoutSessionId, Instant updatedAt) {
        jdbc.update("update perfumestore.payment_attempts set provider_payment_id = ?, checkout_session_id = ?, updated_at = ? where id = ?",
                providerPaymentId, checkoutSessionId, Timestamp.from(updatedAt), id);
    }

    public void updateStatus(UUID id, PaymentAttemptStatus status, String errorCode, String errorMessage,
            Instant updatedAt, Instant completedAt) {
        jdbc.update("update perfumestore.payment_attempts set status = ?, error_code = ?, error_message = ?, updated_at = ?, completed_at = ? where id = ?",
                status.name(), errorCode, errorMessage, Timestamp.from(updatedAt),
                completedAt == null ? null : Timestamp.from(completedAt), id);
    }

    public void updateProviderCheck(UUID id, Instant checkedAt) {
        jdbc.update("update perfumestore.payment_attempts set last_provider_check_at = ?, updated_at = ? where id = ?",
                Timestamp.from(checkedAt), Timestamp.from(checkedAt), id);
    }

    public void insertHistory(UUID id, UUID attemptId, String fromStatus, String toStatus, String event,
            String correlationId, Instant createdAt) {
        jdbc.update("insert into perfumestore.payment_attempt_history (id, payment_attempt_id, from_status, to_status, event, correlation_id, created_at) values (?, ?, ?, ?, ?, ?, ?)",
                id, attemptId, fromStatus, toStatus, event, correlationId, Timestamp.from(createdAt));
    }

    public Optional<PaymentEventRecord> findEvent(String provider, String eventId) {
        return eventQuery("select * from perfumestore.payment_events where provider = ? and provider_event_id = ?", provider, eventId).stream().findFirst();
    }

    public boolean insertEvent(PaymentEventRecord event) {
        return jdbc.update("""
                insert into perfumestore.payment_events
                    (id, provider, provider_event_id, event_type, provider_payment_id, occurred_at, received_at,
                     amount, currency, payload_hash, signature_valid, processing_status, processing_error_code, processed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provider, provider_event_id) do nothing
                """, event.id(), event.provider(), event.providerEventId(), event.eventType(), event.providerPaymentId(),
                Timestamp.from(event.occurredAt()), Timestamp.from(event.receivedAt()), event.amount(), event.currency(),
                event.payloadHash(), event.signatureValid(), event.processingStatus().name(), event.processingErrorCode(),
                event.processedAt() == null ? null : Timestamp.from(event.processedAt())) == 1;
    }

    public void updateEvent(String provider, String eventId, PaymentEventProcessingStatus status, String errorCode,
            Instant processedAt) {
        jdbc.update("update perfumestore.payment_events set processing_status = ?, processing_error_code = ?, processed_at = ? where provider = ? and provider_event_id = ?",
                status.name(), errorCode, processedAt == null ? null : Timestamp.from(processedAt), provider, eventId);
    }

    public void ensureMockState(String providerPaymentId, UUID attemptId, PaymentAttemptStatus status, BigDecimal amount,
            String currency, Instant occurredAt, Instant now) {
        jdbc.update("""
                insert into perfumestore.mock_payment_states
                    (provider_payment_id, payment_attempt_id, status, amount, currency, occurred_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provider_payment_id) do nothing
                """, providerPaymentId, attemptId, status.name(), amount, currency, Timestamp.from(occurredAt),
                Timestamp.from(now), Timestamp.from(now));
    }

    public void updateMockStateIfNewer(String providerPaymentId, PaymentAttemptStatus status, BigDecimal amount,
            String currency, String eventId, Instant occurredAt, Instant now) {
        jdbc.update("""
                insert into perfumestore.mock_payment_states
                    (provider_payment_id, payment_attempt_id, status, amount, currency, last_event_id, occurred_at, created_at, updated_at)
                select ?, id, ?, ?, ?, ?, ?, ?, ? from perfumestore.payment_attempts where provider_payment_id = ?
                on conflict (provider_payment_id) do update
                    set status = excluded.status, amount = excluded.amount, currency = excluded.currency,
                        last_event_id = excluded.last_event_id, occurred_at = excluded.occurred_at, updated_at = excluded.updated_at
                  where perfumestore.mock_payment_states.occurred_at <= excluded.occurred_at
                """, providerPaymentId, status.name(), amount, currency, eventId, Timestamp.from(occurredAt),
                Timestamp.from(now), Timestamp.from(now), providerPaymentId);
    }

    public Optional<MockPaymentStateRecord> findMockState(String providerPaymentId) {
        return jdbc.query("select * from perfumestore.mock_payment_states where provider_payment_id = ?", (rs, row) ->
                new MockPaymentStateRecord(rs.getString("provider_payment_id"), rs.getObject("payment_attempt_id", UUID.class),
                        rs.getString("status"), rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("last_event_id"),
                        rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()), providerPaymentId).stream().findFirst();
    }

    public void insertReconciliation(UUID id, UUID attemptId, String provider, String providerStatus, String result,
            String errorCode, Instant requestedAt, Instant completedAt) {
        jdbc.update("insert into perfumestore.payment_reconciliations (id, payment_attempt_id, provider, provider_status, result_status, error_code, requested_at, completed_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                id, attemptId, provider, providerStatus, result, errorCode, Timestamp.from(requestedAt),
                completedAt == null ? null : Timestamp.from(completedAt));
    }

    private List<PaymentAttemptRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new PaymentAttemptRecord(rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class),
                rs.getString("provider"), rs.getString("provider_payment_id"), rs.getString("checkout_session_id"),
                rs.getString("idempotency_key"), rs.getString("request_fingerprint"), rs.getBigDecimal("amount"),
                rs.getString("currency"), PaymentAttemptStatus.valueOf(rs.getString("status")), rs.getString("error_code"),
                rs.getString("error_message"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("last_provider_check_at") == null ? null : rs.getTimestamp("last_provider_check_at").toInstant()), args);
    }

    private List<PaymentEventRecord> eventQuery(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new PaymentEventRecord(rs.getObject("id", UUID.class), rs.getString("provider"),
                rs.getString("provider_event_id"), rs.getString("event_type"), rs.getString("provider_payment_id"),
                rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("received_at").toInstant(), rs.getBigDecimal("amount"),
                rs.getString("currency"), rs.getString("payload_hash"), rs.getBoolean("signature_valid"),
                PaymentEventProcessingStatus.valueOf(rs.getString("processing_status")), rs.getString("processing_error_code"),
                rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toInstant()), args);
    }
}
