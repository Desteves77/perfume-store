package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;
    public OrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<OrderRecord> findByUserAndKey(UUID userId, String key) {
        return query("select * from perfumestore.orders where user_id = ? and idempotency_key = ?", userId, key)
                .stream().findFirst();
    }
    public Optional<OrderRecord> findByPublicNumberForUser(String number, UUID userId) {
        return query("select * from perfumestore.orders where public_number = ? and user_id = ?", number, userId)
                .stream().findFirst();
    }
    public Optional<OrderRecord> findByIdForUpdate(UUID id) {
        return query("select * from perfumestore.orders where id = ? for update", id).stream().findFirst();
    }
    public List<OrderRecord> findDueForUpdate(Instant now, int limit) {
        return query("select * from perfumestore.orders where order_status = 'PENDING_PAYMENT' and payment_expires_at <= ? order by id for update skip locked limit " + Math.max(1, Math.min(limit, 1000)), Timestamp.from(now));
    }
    public List<OrderRecord> findPage(UUID userId, int page, int size, String column, String direction) {
        int offset = Math.multiplyExact(page, size);
        return query("select * from perfumestore.orders where user_id = ? order by " + column + " " + direction + ", id desc limit ? offset ?", userId, size, offset);
    }
    public long count(UUID userId) {
        Long value = jdbc.queryForObject("select count(*) from perfumestore.orders where user_id = ?", Long.class, userId);
        return value == null ? 0 : value;
    }
    public boolean insertIfAbsent(OrderRecord o) {
        return jdbc.update("""
                insert into perfumestore.orders
                    (id, public_number, user_id, cart_id, idempotency_key, request_fingerprint, order_status,
                     payment_status, delivery_status, subtotal, shipping_amount, total, currency, created_at,
                     updated_at, payment_expires_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (user_id, idempotency_key) do nothing
                """, o.id(), o.publicNumber(), o.userId(), o.cartId(), o.idempotencyKey(), o.requestFingerprint(),
                o.status().name(), o.paymentStatus().name(), o.deliveryStatus().name(), o.subtotal(), o.shippingAmount(),
                o.total(), o.currency(), Timestamp.from(o.createdAt()), Timestamp.from(o.updatedAt()),
                Timestamp.from(o.paymentExpiresAt())) == 1;
    }
    public void updateCommercial(UUID id, UUID cartId, BigDecimal subtotal, BigDecimal shippingAmount, BigDecimal total,
            Instant updatedAt, Instant paymentExpiresAt) {
        jdbc.update("update perfumestore.orders set cart_id = ?, subtotal = ?, shipping_amount = ?, total = ?, updated_at = ?, payment_expires_at = ? where id = ?",
                cartId, subtotal, shippingAmount, total, Timestamp.from(updatedAt), Timestamp.from(paymentExpiresAt), id);
    }
    public void insertItem(UUID id, UUID orderId, OrderItemRecord item) {
        jdbc.update("""
                insert into perfumestore.order_items
                    (id, order_id, product_id, presentation_id, sku, product_name, volume, unit, quantity, unit_price, line_total, currency)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, orderId, item.productId(), item.presentationId(), item.sku(), item.productName(), item.volume(),
                item.unit(), item.quantity(), item.unitPrice(), item.lineTotal(), item.currency());
    }
    public void insertAddress(UUID orderId, OrderAddressRecord a) {
        jdbc.update("insert into perfumestore.order_address_snapshots (order_id, recipient_name, label, postal_code, street, number, complement, neighborhood, city, state, reference_text) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", orderId, a.recipientName(), a.label(), a.postalCode(), a.street(), a.number(), a.complement(), a.neighborhood(), a.city(), a.state(), a.reference());
    }
    public void insertShipping(UUID orderId, OrderShippingRecord s) {
        jdbc.update("insert into perfumestore.order_shipping_snapshots (order_id, quote_id, method, amount, currency, estimated_min_days, estimated_max_days, day_type, rule_version, postal_code, city, state) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", orderId, s.quoteId(), s.method(), s.amount(), s.currency(), s.minDays(), s.maxDays(), s.dayType(), s.ruleVersion(), s.postalCode(), s.city(), s.state());
    }
    public void insertReservation(UUID id, UUID orderId, OrderReservationRecord r, Instant now) {
        jdbc.update("insert into perfumestore.order_reservations (id, order_id, reservation_id, reservation_key, presentation_id, quantity, created_at) values (?, ?, ?, ?, ?, ?, ?)", id, orderId, r.reservationId(), r.reservationKey(), r.presentationId(), r.quantity(), Timestamp.from(now));
    }
    public List<OrderReservationRecord> reservations(UUID orderId) {
        return jdbc.query("select reservation_id, reservation_key, presentation_id, quantity from perfumestore.order_reservations where order_id = ? order by presentation_id", (rs, row) -> new OrderReservationRecord(rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, UUID.class), rs.getInt(4)), orderId);
    }
    public void insertHistory(UUID id, UUID orderId, String from, String to, String event, UUID actor, String correlation, Instant now) {
        jdbc.update("insert into perfumestore.order_status_history (id, order_id, from_status, to_status, event, actor_user_id, correlation_id, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)", id, orderId, from, to, event, actor, correlation, Timestamp.from(now));
    }
    public int updateState(UUID id, OrderStatus status, PaymentStatus payment, Instant now) {
        return jdbc.update("update perfumestore.orders set order_status = ?, payment_status = ?, updated_at = ? where id = ?", status.name(), payment.name(), Timestamp.from(now), id);
    }
    public List<OrderItemRecord> items(UUID orderId) {
        return jdbc.query("select id, product_id, presentation_id, sku, product_name, volume, unit, quantity, unit_price, line_total, currency from perfumestore.order_items where order_id = ? order by id", (rs, row) -> new OrderItemRecord(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5), rs.getBigDecimal(6), rs.getString(7), rs.getInt(8), rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getString(11)), orderId);
    }
    public OrderAddressRecord address(UUID orderId) {
        return jdbc.query("select recipient_name, label, postal_code, street, number, complement, neighborhood, city, state, reference_text from perfumestore.order_address_snapshots where order_id = ?", (rs, row) -> new OrderAddressRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10)), orderId).stream().findFirst().orElseThrow();
    }
    public OrderShippingRecord shipping(UUID orderId) {
        return jdbc.query("select quote_id, method, amount, currency, estimated_min_days, estimated_max_days, day_type, rule_version, postal_code, city, state from perfumestore.order_shipping_snapshots where order_id = ?", (rs, row) -> new OrderShippingRecord(rs.getObject(1, UUID.class), rs.getString(2), rs.getBigDecimal(3), rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)), orderId).stream().findFirst().orElseThrow();
    }
    public List<OrderHistoryRecord> history(UUID orderId) {
        return jdbc.query("select from_status, to_status, event, created_at from perfumestore.order_status_history where order_id = ? order by created_at, id", (rs, row) -> new OrderHistoryRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant()), orderId);
    }
    private List<OrderRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new OrderRecord(rs.getObject("id", UUID.class), rs.getString("public_number"), rs.getObject("user_id", UUID.class), rs.getObject("cart_id", UUID.class), rs.getString("idempotency_key"), rs.getString("request_fingerprint"), OrderStatus.valueOf(rs.getString("order_status")), PaymentStatus.valueOf(rs.getString("payment_status")), DeliveryStatus.valueOf(rs.getString("delivery_status")), rs.getBigDecimal("subtotal"), rs.getBigDecimal("shipping_amount"), rs.getBigDecimal("total"), rs.getString("currency"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), rs.getTimestamp("payment_expires_at").toInstant()), args);
    }
}
