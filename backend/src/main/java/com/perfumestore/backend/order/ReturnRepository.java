package com.perfumestore.backend.order;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.perfumestore.backend.shared.api.PageQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReturnRepository {
    private final JdbcTemplate jdbc;
    public ReturnRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<ReturnRecord> findByIdForUpdate(UUID id) { return query("select * from perfumestore.returns where id = ? for update", id).stream().findFirst(); }
    public Optional<ReturnRecord> findById(UUID id) { return query("select * from perfumestore.returns where id = ?", id).stream().findFirst(); }
    public Optional<ReturnRecord> findByOrderAndKeyForUpdate(UUID orderId, String key) { return query("select * from perfumestore.returns where order_id = ? and idempotency_key = ? for update", orderId, key).stream().findFirst(); }
    public boolean insert(ReturnRecord r) {
        return jdbc.update("insert into perfumestore.returns (id, order_id, order_item_id, presentation_id, quantity, received_quantity, approved_quantity, rejected_quantity, reason, status, requested_by, received_by, inspected_by, requested_at, received_at, inspected_at, condition_note, inspection_reason, idempotency_key, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.id(), r.orderId(), r.orderItemId(), r.presentationId(), r.quantity(), r.receivedQuantity(), r.approvedQuantity(), r.rejectedQuantity(), r.reason(), r.status().name(), r.requestedBy(), r.receivedBy(), r.inspectedBy(), Timestamp.from(r.requestedAt()), r.receivedAt() == null ? null : Timestamp.from(r.receivedAt()), r.inspectedAt() == null ? null : Timestamp.from(r.inspectedAt()), r.conditionNote(), r.inspectionReason(), r.idempotencyKey(), Timestamp.from(r.updatedAt())) == 1;
    }
    public void receive(UUID id, int quantity, UUID actor, String condition, Instant now) {
        jdbc.update("update perfumestore.returns set received_quantity = ?, status = 'AWAITING_INSPECTION', received_by = ?, received_at = ?, condition_note = ?, updated_at = ? where id = ?",
                quantity, actor, Timestamp.from(now), condition, Timestamp.from(now), id);
    }
    public void inspect(UUID id, int approved, int rejected, UUID actor, String reason, Instant now) {
        jdbc.update("update perfumestore.returns set approved_quantity = ?, rejected_quantity = ?, status = 'INSPECTED', inspected_by = ?, inspected_at = ?, inspection_reason = ?, updated_at = ? where id = ?",
                approved, rejected, actor, Timestamp.from(now), reason, Timestamp.from(now), id);
    }
    public void markRestocked(UUID id, int totalRestocked, Instant now) {
        jdbc.update("update perfumestore.returns set status = case when approved_quantity = ? then 'RESTOCKED' else status end, updated_at = ? where id = ?",
                totalRestocked, Timestamp.from(now), id);
    }
    public void history(UUID id, String from, String to, String event, UUID actor, String reason, Instant now) {
        jdbc.update("insert into perfumestore.return_history (id, return_id, from_status, to_status, event, actor_user_id, reason, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), id, from, to, event, actor, reason, Timestamp.from(now));
    }
    public List<ReturnHistoryRecord> historyFor(UUID id) {
        return jdbc.query("select from_status, to_status, event, actor_user_id, reason, created_at from perfumestore.return_history where return_id = ? order by created_at, id",
                (rs, row) -> new ReturnHistoryRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getObject(4, UUID.class), rs.getString(5), rs.getTimestamp(6).toInstant()), id);
    }
    public List<ReturnRecord> findPage(PageQuery page, ReturnStatus status) {
        int offset = Math.multiplyExact(page.getPage(), page.getSize());
        String statusValue = status == null ? null : status.name();
        return query("select * from perfumestore.returns where (? is null or status = ?) order by requested_at desc, id desc limit ? offset ?", statusValue, statusValue, page.getSize(), offset);
    }
    public long count(ReturnStatus status) {
        String value = status == null ? null : status.name();
        Long count = jdbc.queryForObject("select count(*) from perfumestore.returns where (? is null or status = ?)", Long.class, value, value);
        return count == null ? 0 : count;
    }
    public List<ReturnRecord> findByOrder(UUID orderId) { return query("select * from perfumestore.returns where order_id = ? order by requested_at, id", orderId); }
    public int findByItem(UUID itemId) { Integer value = jdbc.queryForObject("select coalesce(sum(quantity),0) from perfumestore.returns where order_item_id = ?", Integer.class, itemId); return value == null ? 0 : value; }
    public int restockedQuantity(UUID returnId) { Integer value = jdbc.queryForObject("select coalesce(sum(quantity),0) from perfumestore.return_restock_operations where return_id = ?", Integer.class, returnId); return value == null ? 0 : value; }
    public Optional<RestockRecord> findRestockByKey(String key) { return jdbc.query("select id, return_id, presentation_id, quantity, operation_key, inventory_operation_key, actor_user_id, created_at from perfumestore.return_restock_operations where operation_key = ?", (rs, row) -> new RestockRecord(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getInt(4), rs.getString(5), rs.getString(6), rs.getObject(7, UUID.class), rs.getTimestamp(8).toInstant()), key).stream().findFirst(); }
    public void insertRestock(RestockRecord r) { jdbc.update("insert into perfumestore.return_restock_operations (id, return_id, presentation_id, quantity, operation_key, inventory_operation_key, actor_user_id, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)", r.id(), r.returnId(), r.presentationId(), r.quantity(), r.operationKey(), r.inventoryOperationKey(), r.actorUserId(), Timestamp.from(r.createdAt())); }
    private List<ReturnRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new ReturnRecord(rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class), rs.getObject("order_item_id", UUID.class), rs.getObject("presentation_id", UUID.class), rs.getInt("quantity"), rs.getInt("received_quantity"), rs.getInt("approved_quantity"), rs.getInt("rejected_quantity"), rs.getString("reason"), ReturnStatus.valueOf(rs.getString("status")), rs.getObject("requested_by", UUID.class), rs.getObject("received_by", UUID.class), rs.getObject("inspected_by", UUID.class), rs.getTimestamp("requested_at").toInstant(), rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toInstant(), rs.getTimestamp("inspected_at") == null ? null : rs.getTimestamp("inspected_at").toInstant(), rs.getString("condition_note"), rs.getString("inspection_reason"), rs.getString("idempotency_key"), rs.getTimestamp("updated_at").toInstant()), args);
    }
    public record RestockRecord(UUID id, UUID returnId, UUID presentationId, int quantity, String operationKey, String inventoryOperationKey, UUID actorUserId, Instant createdAt) { }
}
