package com.perfumestore.backend.order;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminOrderRepository {
    private final JdbcTemplate jdbc;

    public AdminOrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<AdminOrderHistoryRecord> findOperation(String operationKey) {
        return jdbc.query("select operation, actor_user_id, from_order_status, to_order_status, from_delivery_status, to_delivery_status, reason, external_reference, created_at from perfumestore.admin_order_history where operation_key = ?",
                (rs, row) -> new AdminOrderHistoryRecord(rs.getString(1), rs.getObject(2, UUID.class), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getTimestamp(9).toInstant()), operationKey).stream().findFirst();
    }

    public void insertHistory(UUID id, UUID orderId, String operation, String operationKey, UUID actor,
            String fromOrder, String toOrder, String fromDelivery, String toDelivery, String reason,
            String externalReference, String correlationId, Instant now) {
        jdbc.update("insert into perfumestore.admin_order_history (id, order_id, operation, operation_key, actor_user_id, from_order_status, to_order_status, from_delivery_status, to_delivery_status, reason, external_reference, correlation_id, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, orderId, operation, operationKey, actor, fromOrder, toOrder, fromDelivery, toDelivery,
                reason, externalReference, correlationId, Timestamp.from(now));
    }

    public List<AdminOrderHistoryRecord> history(UUID orderId) {
        return jdbc.query("select operation, actor_user_id, from_order_status, to_order_status, from_delivery_status, to_delivery_status, reason, external_reference, created_at from perfumestore.admin_order_history where order_id = ? order by created_at, id",
                (rs, row) -> new AdminOrderHistoryRecord(rs.getString(1), rs.getObject(2, UUID.class), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getTimestamp(9).toInstant()), orderId);
    }

    public Optional<AdminOrderDtos.Logistics> logistics(UUID orderId) {
        return jdbc.query("select carrier, tracking_code, shipped_at, shipped_by, delivered_at, delivered_by from perfumestore.order_logistics where order_id = ?",
                (rs, row) -> new AdminOrderDtos.Logistics(rs.getString(1), rs.getString(2),
                        rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant(), rs.getObject(4, UUID.class),
                        rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toInstant(), rs.getObject(6, UUID.class)), orderId).stream().findFirst();
    }

    public void insertShipping(UUID orderId, String carrier, String trackingCode, UUID actor, Instant now) {
        jdbc.update("insert into perfumestore.order_logistics (order_id, carrier, tracking_code, shipped_at, shipped_by, updated_at) values (?, ?, ?, ?, ?, ?)",
                orderId, carrier, trackingCode, Timestamp.from(now), actor, Timestamp.from(now));
    }

    public void markDelivered(UUID orderId, UUID actor, Instant now) {
        jdbc.update("update perfumestore.order_logistics set delivered_at = ?, delivered_by = ?, updated_at = ? where order_id = ?",
                Timestamp.from(now), actor, Timestamp.from(now), orderId);
    }

    public List<AdminOrderDtos.Reservation> reservations(UUID orderId) {
        return jdbc.query("select orr.reservation_key, orr.presentation_id, orr.quantity, sr.state from perfumestore.order_reservations orr join perfumestore.stock_reservations sr on sr.id = orr.reservation_id where orr.order_id = ? order by orr.presentation_id",
                (rs, row) -> new AdminOrderDtos.Reservation(rs.getString(1), rs.getObject(2, UUID.class), rs.getInt(3), rs.getString(4)), orderId);
    }
}
