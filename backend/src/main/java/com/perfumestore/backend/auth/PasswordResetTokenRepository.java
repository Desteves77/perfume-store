package com.perfumestore.backend.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepository {

    private final JdbcTemplate jdbc;

    public PasswordResetTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void invalidateActiveForUser(UUID userId, Instant now) {
        jdbc.update("""
                update perfumestore.password_reset_tokens
                set used_at = ?
                where user_id = ? and used_at is null and expires_at > ?
                """, Timestamp.from(now), userId, Timestamp.from(now));
    }

    public void insert(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        jdbc.update("""
                insert into perfumestore.password_reset_tokens
                    (id, user_id, token_hash, expires_at, created_at)
                values (?, ?, ?, ?, ?)
                """, id, userId, tokenHash, Timestamp.from(expiresAt), Timestamp.from(createdAt));
    }

    public Optional<UUID> consume(String tokenHash, Instant now) {
        return jdbc.query("""
                update perfumestore.password_reset_tokens
                set used_at = ?
                where token_hash = ? and used_at is null and expires_at > ?
                returning user_id
                """, rs -> rs.next() ? Optional.of(rs.getObject("user_id", UUID.class)) : Optional.empty(),
                Timestamp.from(now), tokenHash, Timestamp.from(now));
    }

    public void invalidateAllForUser(UUID userId, Instant now) {
        jdbc.update("""
                update perfumestore.password_reset_tokens
                set used_at = coalesce(used_at, ?)
                where user_id = ? and used_at is null
                """, Timestamp.from(now), userId);
    }

    public void deleteByHash(String tokenHash) {
        jdbc.update("delete from perfumestore.password_reset_tokens where token_hash = ?", tokenHash);
    }
}
