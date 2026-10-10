package com.perfumestore.backend.user;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByEmailNormalized(String emailNormalized) {
        return query("select * from perfumestore.app_users where email_normalized = ?", emailNormalized);
    }

    public Optional<AppUser> findById(UUID id) {
        return query("select * from perfumestore.app_users where id = ?", id);
    }

    public AppUser insert(UUID id, String displayName, String email, String emailNormalized, String passwordHash,
            UserRole role, UserStatus status, Instant now) {
        jdbc.update("""
                insert into perfumestore.app_users
                    (id, display_name, email, email_normalized, password_hash, role, status,
                     created_at, updated_at, password_changed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, displayName, email, emailNormalized, passwordHash, role.name(), status.name(),
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return findById(id).orElseThrow();
    }

    public Optional<AppUser> updateDisplayName(UUID id, String displayName, Instant now) {
        jdbc.update("update perfumestore.app_users set display_name = ?, updated_at = ? where id = ?",
                displayName, Timestamp.from(now), id);
        return findById(id);
    }

    public Optional<AppUser> updatePassword(UUID id, String passwordHash, Instant now) {
        jdbc.update("""
                update perfumestore.app_users
                set password_hash = ?, password_changed_at = ?, updated_at = ?
                where id = ?
                """, passwordHash, Timestamp.from(now), Timestamp.from(now), id);
        return findById(id);
    }

    public long countAdmins() {
        Long count = jdbc.queryForObject(
                "select count(*) from perfumestore.app_users where role = 'ADMIN' and status = 'ACTIVE'", Long.class);
        return count == null ? 0 : count;
    }

    public boolean existsByEmailNormalized(String emailNormalized) {
        Boolean exists = jdbc.queryForObject(
                "select exists(select 1 from perfumestore.app_users where email_normalized = ?)", Boolean.class,
                emailNormalized);
        return Boolean.TRUE.equals(exists);
    }

    private Optional<AppUser> query(String sql, Object... args) {
        List<AppUser> users = jdbc.query(sql, (rs, rowNum) -> new AppUser(
                rs.getObject("id", UUID.class),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("email_normalized"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                UserStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("password_changed_at").toInstant()), args);
        return users.stream().findFirst();
    }
}
