package com.perfumestore.backend.inventory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryMovementRepository {

    private final JdbcTemplate jdbc;

    public InventoryMovementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(InventoryMovementRecord movement) {
        int inserted = jdbc.update("""
                insert into perfumestore.inventory_movements
                    (id, presentation_id, movement_type, physical_delta, reserved_delta,
                     physical_before, physical_after, reserved_before, reserved_after, reason,
                     operation_key, reservation_id, actor_user_id, correlation_id, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (operation_key) do nothing
                """, movement.id(), movement.presentationId(), movement.type().name(), movement.physicalDelta(),
                movement.reservedDelta(), movement.physicalBefore(), movement.physicalAfter(),
                movement.reservedBefore(), movement.reservedAfter(), movement.reason(), movement.operationKey(),
                movement.reservationId(), movement.actorUserId(), movement.correlationId(),
                Timestamp.from(movement.createdAt()));
        return inserted == 1;
    }

    public Optional<InventoryMovementRecord> findByOperationKey(String operationKey) {
        return query("select * from perfumestore.inventory_movements where operation_key = ?", operationKey)
                .stream().findFirst();
    }

    public PageResponse<InventoryMovementRecord> findPage(PageQuery page, UUID presentationId,
            String sortColumn, String direction) {
        List<Object> args = new ArrayList<>();
        String where = presentationId == null ? "" : " where presentation_id = ?";
        if (presentationId != null) {
            args.add(presentationId);
        }
        args.add(page.getSize());
        args.add(page.getPage() * page.getSize());
        List<InventoryMovementRecord> items = query("select * from perfumestore.inventory_movements" + where
                + " order by " + sortColumn + " " + direction + ", id desc limit ? offset ?", args.toArray());
        Long count = jdbc.queryForObject("select count(*) from perfumestore.inventory_movements" + where,
                Long.class, presentationId == null ? new Object[0] : new Object[] { presentationId });
        long total = count == null ? 0 : count;
        return new PageResponse<>(items, page.getPage(), page.getSize(), total,
                PageResponse.totalPages(total, page.getSize()),
                List.of(new SortOrder(sortColumn, "desc".equals(direction) ? SortOrder.Direction.DESC
                        : SortOrder.Direction.ASC), new SortOrder("id", SortOrder.Direction.DESC)));
    }

    private List<InventoryMovementRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new InventoryMovementRecord(
                rs.getObject("id", UUID.class), rs.getObject("presentation_id", UUID.class),
                InventoryMovementType.valueOf(rs.getString("movement_type")), rs.getInt("physical_delta"),
                rs.getInt("reserved_delta"), rs.getInt("physical_before"), rs.getInt("physical_after"),
                rs.getInt("reserved_before"), rs.getInt("reserved_after"), rs.getString("reason"),
                rs.getString("operation_key"), rs.getObject("reservation_id", UUID.class),
                rs.getObject("actor_user_id", UUID.class), rs.getString("correlation_id"),
                rs.getTimestamp("created_at").toInstant()), args);
    }
}
