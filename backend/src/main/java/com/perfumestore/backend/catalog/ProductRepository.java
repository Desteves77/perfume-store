package com.perfumestore.backend.catalog;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProductRecord insert(UUID id, String slug, String name, String brand, String description,
            UUID categoryId, boolean active, Instant now) {
        jdbc.update("""
                insert into perfumestore.products
                    (id, slug, name, brand, description, category_id, active, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """, id, slug, name, brand, description, categoryId, active, Timestamp.from(now), Timestamp.from(now));
        return findById(id).orElseThrow();
    }

    public Optional<ProductRecord> findById(UUID id) {
        return query("""
                select p.*, c.name category_name, c.slug category_slug, c.active category_active,
                       c.version category_version, c.created_at category_created_at, c.updated_at category_updated_at
                from perfumestore.products p join perfumestore.categories c on c.id = p.category_id
                where p.id = ?
                """, id).stream().findFirst();
    }

    public Optional<ProductRecord> findPublicBySlug(String slug) {
        return query("""
                select p.*, c.name category_name, c.slug category_slug, c.active category_active,
                       c.version category_version, c.created_at category_created_at, c.updated_at category_updated_at
                from perfumestore.products p join perfumestore.categories c on c.id = p.category_id
                where p.slug = ? and p.active = true and c.active = true
                  and exists (select 1 from perfumestore.product_presentations pp
                             where pp.product_id = p.id and pp.active = true)
                  and exists (select 1 from perfumestore.product_images pi
                             where pi.product_id = p.id and pi.deleted_at is null and pi.is_primary = true)
                """, slug).stream().findFirst();
    }

    public List<ProductRecord> findAdminPage(int page, int size, String query, Boolean active,
            String sortColumn, String direction) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select p.*, c.name category_name, c.slug category_slug, c.active category_active,
                       c.version category_version, c.created_at category_created_at, c.updated_at category_updated_at
                from perfumestore.products p join perfumestore.categories c on c.id = p.category_id
                where 1 = 1
                """);
        appendFilters(sql, args, query, active, null);
        sql.append(" order by ").append(sortColumn).append(' ').append(direction)
                .append(", p.id asc limit ? offset ?");
        args.add(size);
        args.add(page * size);
        return query(sql.toString(), args.toArray());
    }

    public List<ProductRecord> findPublicPage(int page, int size, String query, String categorySlug,
            String sortColumn, String direction) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select p.*, c.name category_name, c.slug category_slug, c.active category_active,
                       c.version category_version, c.created_at category_created_at, c.updated_at category_updated_at
                from perfumestore.products p join perfumestore.categories c on c.id = p.category_id
                where p.active = true and c.active = true
                  and exists (select 1 from perfumestore.product_presentations pp
                             where pp.product_id = p.id and pp.active = true)
                  and exists (select 1 from perfumestore.product_images pi
                             where pi.product_id = p.id and pi.deleted_at is null and pi.is_primary = true)
                """);
        appendFilters(sql, args, query, null, categorySlug);
        sql.append(" order by ").append(sortColumn).append(' ').append(direction)
                .append(", p.id asc limit ? offset ?");
        args.add(size);
        args.add(page * size);
        return query(sql.toString(), args.toArray());
    }

    public long countAdmin(String query, Boolean active) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select count(*) from perfumestore.products p where 1 = 1");
        appendFilters(sql, args, query, active, null);
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public long countPublic(String query, String categorySlug) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select count(*) from perfumestore.products p
                join perfumestore.categories c on c.id = p.category_id
                where p.active = true and c.active = true
                  and exists (select 1 from perfumestore.product_presentations pp
                             where pp.product_id = p.id and pp.active = true)
                  and exists (select 1 from perfumestore.product_images pi
                             where pi.product_id = p.id and pi.deleted_at is null and pi.is_primary = true)
                """);
        appendFilters(sql, args, query, null, categorySlug);
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public Optional<ProductRecord> update(UUID id, String slug, String name, String brand, String description,
            UUID categoryId, long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.products
                set slug = ?, name = ?, brand = ?, description = ?, category_id = ?,
                    version = version + 1, updated_at = ?
                where id = ? and version = ?
                """, slug, name, brand, description, categoryId, Timestamp.from(now), id, version);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    public Optional<ProductRecord> updateActive(UUID id, boolean active, long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.products set active = ?, version = version + 1, updated_at = ?
                where id = ? and version = ?
                """, active, Timestamp.from(now), id, version);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, String query, Boolean active,
            String categorySlug) {
        if (query != null && !query.isBlank()) {
            sql.append(" and lower(p.name) like lower(?)");
            args.add("%" + query.trim() + "%");
        }
        if (active != null) {
            sql.append(" and p.active = ?");
            args.add(active);
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            sql.append(" and c.slug = ?");
            args.add(categorySlug);
        }
    }

    private List<ProductRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new ProductRecord(
                rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("name"),
                rs.getString("brand"), rs.getString("description"), rs.getObject("category_id", UUID.class),
                rs.getString("category_name"), rs.getString("category_slug"), rs.getBoolean("category_active"),
                rs.getLong("category_version"), rs.getTimestamp("category_created_at").toInstant(),
                rs.getTimestamp("category_updated_at").toInstant(),
                rs.getBoolean("active"), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), args);
    }
}
