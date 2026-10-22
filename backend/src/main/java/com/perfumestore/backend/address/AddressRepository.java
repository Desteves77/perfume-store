package com.perfumestore.backend.address;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AddressRepository {

    private final JdbcTemplate jdbc;

    public AddressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AddressRecord insert(UUID id, UUID userId, String recipientName, String label, String postalCode,
            String street, String number, String complement, String neighborhood, String city, String state,
            String reference, Instant now) {
        jdbc.update("""
                insert into perfumestore.addresses
                    (id, user_id, recipient_name, label, postal_code, street, number, complement,
                     neighborhood, city, state, reference_text, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """, id, userId, recipientName, label, postalCode, street, number, complement, neighborhood, city,
                state, reference, Timestamp.from(now), Timestamp.from(now));
        return findByIdForUser(userId, id).orElseThrow();
    }

    public Optional<AddressRecord> findByIdForUser(UUID userId, UUID id) {
        return query("""
                select * from perfumestore.addresses
                where id = ? and user_id = ? and deleted_at is null
                """, id, userId).stream().findFirst();
    }

    public Optional<AddressRecord> findAnyByIdForUser(UUID userId, UUID id) {
        return query("select * from perfumestore.addresses where id = ? and user_id = ?", id, userId)
                .stream().findFirst();
    }

    public List<AddressRecord> findPageForUser(UUID userId, int page, int size, String sortColumn, String direction) {
        return query("""
                select * from perfumestore.addresses
                where user_id = ? and deleted_at is null
                order by %s %s, id asc limit ? offset ?
                """.formatted(sortColumn, direction), userId, size, page * size);
    }

    public long countForUser(UUID userId) {
        Long count = jdbc.queryForObject("""
                select count(*) from perfumestore.addresses
                where user_id = ? and deleted_at is null
                """, Long.class, userId);
        return count == null ? 0 : count;
    }

    public Optional<AddressRecord> update(UUID userId, UUID id, String recipientName, String label, String postalCode,
            String street, String number, String complement, String neighborhood, String city, String state,
            String reference, long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.addresses
                set recipient_name = ?, label = ?, postal_code = ?, street = ?, number = ?, complement = ?,
                    neighborhood = ?, city = ?, state = ?, reference_text = ?, version = version + 1,
                    updated_at = ?
                where id = ? and user_id = ? and deleted_at is null and version = ?
                """, recipientName, label, postalCode, street, number, complement, neighborhood, city, state,
                reference, Timestamp.from(now), id, userId, version);
        return updated == 0 ? Optional.empty() : findByIdForUser(userId, id);
    }

    public int softDelete(UUID userId, UUID id, Instant now) {
        return jdbc.update("""
                update perfumestore.addresses
                set deleted_at = ?, updated_at = ?, version = version + 1
                where id = ? and user_id = ? and deleted_at is null
                """, Timestamp.from(now), Timestamp.from(now), id, userId);
    }

    private List<AddressRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new AddressRecord(
                rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getString("recipient_name"), rs.getString("label"), rs.getString("postal_code"),
                rs.getString("street"), rs.getString("number"), rs.getString("complement"),
                rs.getString("neighborhood"), rs.getString("city"), rs.getString("state"),
                rs.getString("reference_text"), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant()), args);
    }
}
