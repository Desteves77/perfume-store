package com.perfumestore.backend.inventory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockReservationRepository {

    private final JdbcTemplate jdbc;

    public StockReservationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(UUID id, String reservationKey, UUID presentationId, int quantity,
            UUID orderId, Instant expiresAt, Instant now) {
        int inserted = jdbc.update("""
                insert into perfumestore.stock_reservations
                    (id, reservation_key, presentation_id, quantity, state, order_id, expires_at,
                     created_at, updated_at, version)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, 0)
                on conflict (reservation_key) do nothing
                """, id, reservationKey, presentationId, quantity, orderId,
                expiresAt == null ? null : Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now));
        return inserted == 1;
    }

    public Optional<StockReservationRecord> findByKey(String reservationKey) {
        return query("select * from perfumestore.stock_reservations where reservation_key = ?", reservationKey)
                .stream().findFirst();
    }

    public Optional<StockReservationRecord> findForUpdate(String reservationKey) {
        return query("select * from perfumestore.stock_reservations where reservation_key = ? for update",
                reservationKey).stream().findFirst();
    }

    public int updateState(UUID id, ReservationState state, long version, Instant now) {
        return jdbc.update("""
                update perfumestore.stock_reservations
                   set state = ?, version = version + 1, updated_at = ?
                 where id = ? and version = ?
                """, state.name(), Timestamp.from(now), id, version);
    }

    private List<StockReservationRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new StockReservationRecord(
                rs.getObject("id", UUID.class), rs.getString("reservation_key"),
                rs.getObject("presentation_id", UUID.class), rs.getInt("quantity"),
                ReservationState.valueOf(rs.getString("state")), rs.getObject("order_id", UUID.class),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version")), args);
    }
}
