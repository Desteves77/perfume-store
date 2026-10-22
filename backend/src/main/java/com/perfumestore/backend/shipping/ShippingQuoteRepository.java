package com.perfumestore.backend.shipping;

import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShippingQuoteRepository {

    private final JdbcTemplate jdbc;

    public ShippingQuoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID userId, UUID addressId, String postalCode, String city, String state,
            String method, BigDecimal amount, int minDays, int maxDays, String ruleVersion, Instant expiresAt,
            Instant createdAt) {
        jdbc.update("""
                insert into perfumestore.shipping_quotes
                    (id, user_id, address_id, postal_code, city, state, serviceable, method, amount, currency,
                     estimated_min_days, estimated_max_days, rule_version, expires_at, created_at)
                values (?, ?, ?, ?, ?, ?, true, ?, ?, 'BRL', ?, ?, ?, ?, ?)
                """, id, userId, addressId, postalCode, city, state, method, amount, minDays, maxDays, ruleVersion,
                Timestamp.from(expiresAt), Timestamp.from(createdAt));
    }
}
