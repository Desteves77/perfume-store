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
public class ImageRepository {

    private final JdbcTemplate jdbc;

    public ImageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ImageRecord insert(UUID id, UUID productId, String storageKey, String contentType, long byteSize,
            int width, int height, String altText, int position, boolean primary, Instant now) {
        jdbc.update("""
                insert into perfumestore.product_images
                    (id, product_id, storage_key, content_type, byte_size, width, height, alt_text,
                     position, is_primary, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, productId, storageKey, contentType, byteSize, width, height, altText, position,
                primary, Timestamp.from(now), Timestamp.from(now));
        return findById(id).orElseThrow();
    }

    public Optional<ImageRecord> findById(UUID id) {
        return query("select * from perfumestore.product_images where id = ? and deleted_at is null", id)
                .stream().findFirst();
    }

    public Optional<ImageRecord> findAnyById(UUID id) {
        return query("select * from perfumestore.product_images where id = ?", id).stream().findFirst();
    }

    public Optional<ImageRecord> findByProductAndId(UUID productId, UUID imageId) {
        return query("select * from perfumestore.product_images where product_id = ? and id = ? and deleted_at is null",
                productId, imageId).stream().findFirst();
    }

    public List<ImageRecord> findByProduct(UUID productId, boolean onlyActive) {
        String where = onlyActive ? " and deleted_at is null" : "";
        return query("select * from perfumestore.product_images where product_id = ?" + where
                + " order by position asc, id asc", productId);
    }

    public Map<UUID, List<ImageRecord>> findByProducts(List<UUID> productIds, boolean onlyActive) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
        String where = onlyActive ? " and deleted_at is null" : "";
        List<ImageRecord> records = query("select * from perfumestore.product_images where product_id in ("
                + placeholders + ")" + where + " order by position asc, id asc", productIds.toArray());
        return records.stream().collect(Collectors.groupingBy(ImageRecord::productId));
    }

    public long countActiveByProduct(UUID productId) {
        Long count = jdbc.queryForObject("""
                select count(*) from perfumestore.product_images where product_id = ? and deleted_at is null
                """, Long.class, productId);
        return count == null ? 0 : count;
    }

    public long countPrimaryByProduct(UUID productId) {
        Long count = jdbc.queryForObject("""
                select count(*) from perfumestore.product_images
                where product_id = ? and deleted_at is null and is_primary = true
                """, Long.class, productId);
        return count == null ? 0 : count;
    }

    public void setPrimary(UUID productId, UUID imageId, Instant now) {
        jdbc.update("update perfumestore.product_images set is_primary = false, updated_at = ? "
                + "where product_id = ? and deleted_at is null", Timestamp.from(now), productId);
        jdbc.update("update perfumestore.product_images set is_primary = true, updated_at = ? "
                + "where product_id = ? and id = ? and deleted_at is null",
                Timestamp.from(now), productId, imageId);
    }

    public void reorder(UUID productId, List<UUID> imageIds, Instant now) {
        for (int index = 0; index < imageIds.size(); index++) {
            jdbc.update("update perfumestore.product_images set position = ?, updated_at = ? "
                    + "where product_id = ? and id = ? and deleted_at is null",
                    1000 + index, Timestamp.from(now), productId, imageIds.get(index));
        }
        for (int index = 0; index < imageIds.size(); index++) {
            jdbc.update("update perfumestore.product_images set position = ?, updated_at = ? "
                    + "where product_id = ? and id = ? and deleted_at is null",
                    index, Timestamp.from(now), productId, imageIds.get(index));
        }
    }

    public boolean softDelete(UUID productId, UUID imageId, Instant now) {
        return jdbc.update("""
                update perfumestore.product_images
                set deleted_at = ?, is_primary = false, updated_at = ?
                where product_id = ? and id = ? and deleted_at is null
                """, Timestamp.from(now), Timestamp.from(now), productId, imageId) > 0;
    }

    private List<ImageRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new ImageRecord(
                rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                rs.getString("storage_key"), rs.getString("content_type"), rs.getLong("byte_size"),
                rs.getInt("width"), rs.getInt("height"), rs.getString("alt_text"), rs.getInt("position"),
                rs.getBoolean("is_primary"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), args);
    }
}
