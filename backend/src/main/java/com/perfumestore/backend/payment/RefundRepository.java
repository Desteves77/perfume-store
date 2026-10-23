package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.perfumestore.backend.shared.api.PageQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefundRepository {
    private final JdbcTemplate jdbc;
    public RefundRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<RefundRecord> findByIdForUpdate(UUID id) { return query("select * from perfumestore.refunds where id = ? for update", id).stream().findFirst(); }
    public Optional<RefundRecord> findByOrderForUpdate(UUID orderId) { return query("select * from perfumestore.refunds where order_id = ? for update", orderId).stream().findFirst(); }
    public boolean insert(RefundRecord r) {
        return jdbc.update("insert into perfumestore.refunds (id, order_id, payment_attempt_id, amount, currency, reason, status, idempotency_key, request_fingerprint, provider_ref, requested_by, requested_at, updated_at, completed_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.id(), r.orderId(), r.paymentAttemptId(), r.amount(), r.currency(), r.reason(), r.status().name(), r.idempotencyKey(), r.requestFingerprint(), r.providerRef(), r.requestedBy(), Timestamp.from(r.requestedAt()), Timestamp.from(r.updatedAt()), r.completedAt() == null ? null : Timestamp.from(r.completedAt())) == 1;
    }
    public void updateProvider(UUID id, String providerRef, RefundStatus status, Instant now) {
        jdbc.update("update perfumestore.refunds set provider_ref = ?, status = ?, updated_at = ? where id = ?", providerRef, status.name(), Timestamp.from(now), id);
    }
    public void updateStatus(UUID id, RefundStatus status, Instant now, Instant completedAt) {
        jdbc.update("update perfumestore.refunds set status = ?, updated_at = ?, completed_at = ? where id = ?", status.name(), Timestamp.from(now), completedAt == null ? null : Timestamp.from(completedAt), id);
    }
    public void insertHistory(UUID id, UUID refundId, String from, String to, String event, UUID actor,
            String providerRef, String reason, String correlation, Instant now) {
        jdbc.update("insert into perfumestore.refund_history (id, refund_id, from_status, to_status, event, actor_user_id, provider_ref, reason, correlation_id, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, refundId, from, to, event, actor, providerRef, reason, correlation, Timestamp.from(now));
    }
    public List<RefundHistoryRecord> history(UUID refundId) {
        return jdbc.query("select from_status, to_status, event, actor_user_id, provider_ref, reason, created_at from perfumestore.refund_history where refund_id = ? order by created_at, id",
                (rs, row) -> new RefundHistoryRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getObject(4, UUID.class), rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant()), refundId);
    }
    public List<RefundRecord> findPage(PageQuery page, String status) {
        String sort = switch (page.getSort() == null ? "requestedAt" : page.getSort().split(",")[0]) {
            case "updatedAt" -> "updated_at";
            case "status" -> "status";
            default -> "requested_at";
        };
        String direction = page.getSort() != null && page.getSort().endsWith(",desc") ? "desc" : "asc";
        int offset = Math.multiplyExact(page.getPage(), page.getSize());
        return query("select * from perfumestore.refunds where (? is null or status = ?) order by " + sort + " " + direction + ", id desc limit ? offset ?",
                status, status, page.getSize(), offset);
    }
    public long count(String status) {
        Long value = jdbc.queryForObject("select count(*) from perfumestore.refunds where (? is null or status = ?)", Long.class, status, status);
        return value == null ? 0 : value;
    }
    public List<RefundRecord> findByOrder(UUID orderId) { return query("select * from perfumestore.refunds where order_id = ? order by requested_at, id", orderId); }
    private List<RefundRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new RefundRecord(rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class),
                rs.getObject("payment_attempt_id", UUID.class), rs.getBigDecimal("amount"), rs.getString("currency"),
                rs.getString("reason"), RefundStatus.valueOf(rs.getString("status")), rs.getString("idempotency_key"),
                rs.getString("request_fingerprint"), rs.getString("provider_ref"), rs.getObject("requested_by", UUID.class),
                rs.getTimestamp("requested_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant()), args);
    }
}
