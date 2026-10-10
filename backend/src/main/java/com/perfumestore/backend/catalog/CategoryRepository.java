package com.perfumestore.backend.catalog;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbc;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CategoryRecord insert(UUID id, String name, String slug, boolean active, Instant now) {
        jdbc.update("""
                insert into perfumestore.categories (id, name, slug, active, version, created_at, updated_at)
                values (?, ?, ?, ?, 0, ?, ?)
                """, id, name, slug, active, Timestamp.from(now), Timestamp.from(now));
        return findById(id).orElseThrow();
    }

    public Optional<CategoryRecord> findById(UUID id) {
        return query("select * from perfumestore.categories where id = ?", id).stream().findFirst();
    }

    public Optional<CategoryRecord> findBySlug(String slug) {
        return query("select * from perfumestore.categories where slug = ?", slug).stream().findFirst();
    }

    public List<CategoryRecord> findPage(int page, int size, boolean onlyActive, String sortColumn,
            String direction) {
        String where = onlyActive ? " where active = true" : "";
        int offset = page * size;
        return query("select * from perfumestore.categories" + where + " order by " + sortColumn + " "
                + direction + ", id asc limit ? offset ?", size, offset);
    }

    public long count(boolean onlyActive) {
        String where = onlyActive ? " where active = true" : "";
        Long count = jdbc.queryForObject("select count(*) from perfumestore.categories" + where, Long.class);
        return count == null ? 0 : count;
    }

    public Optional<CategoryRecord> update(UUID id, String name, String slug, boolean active,
            long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.categories
                set name = ?, slug = ?, active = ?, version = version + 1, updated_at = ?
                where id = ? and version = ?
                """, name, slug, active, Timestamp.from(now), id, version);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    private List<CategoryRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new CategoryRecord(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
                rs.getBoolean("active"), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), args);
    }
}
