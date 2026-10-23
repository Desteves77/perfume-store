package com.perfumestore.backend.cart;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {

    private final JdbcTemplate jdbc;

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CartRecord> findByIdForUpdate(UUID id) {
        return query("select * from perfumestore.carts where id = ? for update", id).stream().findFirst();
    }

    public Optional<CartRecord> findActiveByUser(UUID userId) {
        return query("select * from perfumestore.carts where user_id = ? and state = 'ACTIVE'", userId)
                .stream().findFirst();
    }

    public Optional<CartRecord> findActiveByUserForUpdate(UUID userId) {
        return query("select * from perfumestore.carts where user_id = ? and state = 'ACTIVE' for update", userId)
                .stream().findFirst();
    }

    public Optional<CartRecord> findByTokenHashForUpdate(String tokenHash) {
        return query("select * from perfumestore.carts where visitor_token_hash = ? for update", tokenHash)
                .stream().findFirst();
    }

    public Optional<CartRecord> findByTokenHash(String tokenHash) {
        return query("select * from perfumestore.carts where visitor_token_hash = ?", tokenHash)
                .stream().findFirst();
    }

    public int markExpired(UUID id, long version, Instant now) {
        return jdbc.update("""
                update perfumestore.carts
                set state = 'EXPIRED', version = version + 1, updated_at = ?
                where id = ? and state = 'ACTIVE' and version = ?
                """, Timestamp.from(now), id, version);
    }

    public CartRecord insertCart(UUID id, UUID userId, String tokenHash, CartState state, Instant expiresAt,
            Instant now) {
        jdbc.update("""
                insert into perfumestore.carts
                    (id, user_id, visitor_token_hash, state, expires_at, last_activity_at, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 0, ?, ?)
                on conflict do nothing
                """, id, userId, tokenHash, state.name(), Timestamp.from(expiresAt), Timestamp.from(now),
                Timestamp.from(now), Timestamp.from(now));
        return findByIdForUpdate(id).orElseGet(() -> userId == null
                ? findByTokenHashForUpdate(tokenHash).orElseThrow()
                : findActiveByUserForUpdate(userId).orElseThrow());
    }

    public int touch(UUID id, Instant expiresAt, Instant now) {
        return jdbc.update("""
                update perfumestore.carts
                set expires_at = ?, last_activity_at = ?, updated_at = ?
                where id = ? and state = 'ACTIVE'
                """, Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now), id);
    }

    public int updateVersion(UUID id, long version, Instant expiresAt, Instant now) {
        return jdbc.update("""
                update perfumestore.carts
                set version = version + 1, expires_at = ?, last_activity_at = ?, updated_at = ?
                where id = ? and state = 'ACTIVE' and version = ?
                """, Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now), id, version);
    }

    public int transferToUser(UUID id, long version, UUID userId, Instant expiresAt, Instant now) {
        return jdbc.update("""
                update perfumestore.carts
                set user_id = ?, visitor_token_hash = null, state = 'ACTIVE', version = version + 1,
                    expires_at = ?, last_activity_at = ?, updated_at = ?
                where id = ? and state = 'ACTIVE' and version = ?
                """, userId, Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now), id, version);
    }

    public int markTransferred(UUID id, long version, UUID userId, Instant now) {
        return jdbc.update("""
                update perfumestore.carts
                set user_id = ?, visitor_token_hash = null, state = 'TRANSFERRED', version = version + 1,
                    updated_at = ?
                where id = ? and state = 'ACTIVE' and version = ?
                """, userId, Timestamp.from(now), id, version);
    }

    public int markExpiredBatch(Instant now, int batchSize) {
        return jdbc.update("""
                update perfumestore.carts c
                set state = 'EXPIRED', version = version + 1, updated_at = ?
                where c.id in (
                    select id from perfumestore.carts
                    where state = 'ACTIVE' and expires_at <= ?
                    order by expires_at asc, id asc
                    for update skip locked limit ?
                )
                """, Timestamp.from(now), Timestamp.from(now), batchSize);
    }

    public int deleteCleaned(Instant cutoff, int batchSize) {
        return jdbc.update("""
                delete from perfumestore.carts c
                where c.id in (
                    select id from perfumestore.carts
                    where state in ('EXPIRED', 'TRANSFERRED') and updated_at < ?
                    order by updated_at asc, id asc
                    for update skip locked limit ?
                )
                """, Timestamp.from(cutoff), batchSize);
    }

    public List<CartItemRecord> findItems(UUID cartId) {
        return itemQuery("select * from perfumestore.cart_items where cart_id = ? order by created_at asc, id asc", cartId);
    }

    public Optional<CartItemRecord> findItemForUpdate(UUID cartId, UUID presentationId) {
        return itemQuery("select * from perfumestore.cart_items where cart_id = ? and presentation_id = ? for update",
                cartId, presentationId).stream().findFirst();
    }

    public CartItemRecord insertItem(UUID id, UUID cartId, UUID presentationId, int quantity, BigDecimal price,
            Instant now) {
        jdbc.update("""
                insert into perfumestore.cart_items
                    (id, cart_id, presentation_id, quantity, price_reference, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, 0, ?, ?)
                """, id, cartId, presentationId, quantity, price, Timestamp.from(now), Timestamp.from(now));
        return findItemForUpdate(cartId, presentationId).orElseThrow();
    }

    public Optional<CartItemRecord> updateItem(UUID cartId, UUID presentationId, int quantity, BigDecimal price,
            long version, Instant now) {
        int updated = jdbc.update("""
                update perfumestore.cart_items
                set quantity = ?, price_reference = ?, version = version + 1, updated_at = ?
                where cart_id = ? and presentation_id = ? and version = ?
                """, quantity, price, Timestamp.from(now), cartId, presentationId, version);
        return updated == 0 ? Optional.empty() : findItemForUpdate(cartId, presentationId);
    }

    public int deleteItem(UUID cartId, UUID presentationId) {
        return jdbc.update("delete from perfumestore.cart_items where cart_id = ? and presentation_id = ?",
                cartId, presentationId);
    }

    public int countItems(UUID cartId) {
        Integer count = jdbc.queryForObject("select count(*) from perfumestore.cart_items where cart_id = ?", Integer.class,
                cartId);
        return count == null ? 0 : count;
    }

    public int deleteItems(UUID cartId) {
        return jdbc.update("delete from perfumestore.cart_items where cart_id = ?", cartId);
    }

    public Optional<CartMutationKeyRecord> findMutationKey(UUID cartId, String operationKey) {
        return jdbc.query("select operation_key, fingerprint from perfumestore.cart_mutation_keys where cart_id = ? and operation_key = ?",
                (rs, row) -> new CartMutationKeyRecord(rs.getString("operation_key"), rs.getString("fingerprint")),
                cartId, operationKey).stream().findFirst();
    }

    public boolean insertMutationKey(UUID id, UUID cartId, String operationKey, String fingerprint, Instant now) {
        return jdbc.update("""
                insert into perfumestore.cart_mutation_keys (id, cart_id, operation_key, fingerprint, created_at)
                values (?, ?, ?, ?, ?)
                on conflict (cart_id, operation_key) do nothing
                """, id, cartId, operationKey, fingerprint, Timestamp.from(now)) == 1;
    }

    private List<CartRecord> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new CartRecord(rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getString("visitor_token_hash"),
                CartState.valueOf(rs.getString("state")), rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("last_activity_at").toInstant(), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()), args);
    }

    private List<CartItemRecord> itemQuery(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new CartItemRecord(rs.getObject("id", UUID.class),
                rs.getObject("cart_id", UUID.class), rs.getObject("presentation_id", UUID.class),
                rs.getInt("quantity"), rs.getBigDecimal("price_reference"), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()), args);
    }
}
