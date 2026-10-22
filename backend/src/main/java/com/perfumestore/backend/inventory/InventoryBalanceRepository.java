package com.perfumestore.backend.inventory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryBalanceRepository {

    private final JdbcTemplate jdbc;

    public InventoryBalanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensure(UUID presentationId, Instant now) {
        jdbc.update("""
                insert into perfumestore.inventory_balances
                    (presentation_id, physical_quantity, reserved_quantity, version, created_at, updated_at)
                values (?, 0, 0, 0, ?, ?)
                on conflict (presentation_id) do nothing
                """, presentationId, Timestamp.from(now), Timestamp.from(now));
    }

    public Optional<InventoryBalanceRecord> findForUpdate(UUID presentationId) {
        return query("select * from perfumestore.inventory_balances where presentation_id = ? for update",
                presentationId).stream().findFirst();
    }

    public Optional<InventoryBalanceRecord> find(UUID presentationId) {
        return query("select * from perfumestore.inventory_balances where presentation_id = ?", presentationId)
                .stream().findFirst();
    }

    public int update(UUID presentationId, int physicalQuantity, int reservedQuantity, long version, Instant now) {
        return jdbc.update("""
                update perfumestore.inventory_balances
                   set physical_quantity = ?, reserved_quantity = ?, version = version + 1, updated_at = ?
                 where presentation_id = ? and version = ?
                """, physicalQuantity, reservedQuantity, Timestamp.from(now), presentationId, version);
    }

    public List<InventoryBalanceRecord> findForPresentations(Collection<UUID> presentationIds) {
        if (presentationIds.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>(presentationIds);
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return query("select * from perfumestore.inventory_balances where presentation_id in (" + placeholders + ")",
                ids.toArray());
    }

    private List<InventoryBalanceRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new InventoryBalanceRecord(
                rs.getObject("presentation_id", UUID.class), rs.getInt("physical_quantity"),
                rs.getInt("reserved_quantity"), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()), args);
    }
}
