package com.perfumestore.backend.catalog;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PresentationRepository {

    private final JdbcTemplate jdbc;

    public PresentationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PresentationRecord insert(UUID id, UUID productId, String sku, String skuNormalized,
            java.math.BigDecimal volume, String unit, java.math.BigDecimal price, boolean active, Instant now) {
        jdbc.update("""
                insert into perfumestore.product_presentations
                    (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version,
                     created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, 'BRL', ?, 0, ?, ?)
                """, id, productId, sku, skuNormalized, volume, unit, price, active,
                Timestamp.from(now), Timestamp.from(now));
        return findById(id).orElseThrow();
    }

    public Optional<PresentationRecord> findById(UUID id) {
        return query("select * from perfumestore.product_presentations where id = ?", id).stream().findFirst();
    }

    public List<PresentationRecord> findByProduct(UUID productId, boolean onlyActive) {
        String where = onlyActive ? " and active = true" : "";
        return query("select * from perfumestore.product_presentations where product_id = ?" + where
                + " order by volume asc, id asc", productId);
    }

    public Map<UUID, List<PresentationRecord>> findByProducts(List<UUID> productIds, boolean onlyActive) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
        String where = onlyActive ? " and active = true" : "";
        List<PresentationRecord> records = query("select * from perfumestore.product_presentations where product_id in ("
                + placeholders + ")" + where + " order by volume asc, id asc", productIds.toArray());
        return records.stream().collect(Collectors.groupingBy(PresentationRecord::productId));
    }

    public long countActiveByProduct(UUID productId) {
        Long count = jdbc.queryForObject("""
                select count(*) from perfumestore.product_presentations where product_id = ? and active = true
                """, Long.class, productId);
        return count == null ? 0 : count;
    }

    public Optional<PresentationRecord> update(UUID id, UUID productId, String sku, String skuNormalized,
            java.math.BigDecimal volume, java.math.BigDecimal price, boolean active, long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.product_presentations
                set sku = ?, sku_normalized = ?, volume = ?, price = ?, active = ?,
                    version = version + 1, updated_at = ?
                where id = ? and product_id = ? and version = ?
                """, sku, skuNormalized, volume, price, active, Timestamp.from(now), id, productId, version);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    private List<PresentationRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new PresentationRecord(
                rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                rs.getString("sku"), rs.getString("sku_normalized"), rs.getBigDecimal("volume"),
                rs.getString("unit"), rs.getBigDecimal("price"), rs.getString("currency"),
                rs.getBoolean("active"), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), args);
    }
}
