package com.perfumestore.backend.auth;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionInvalidationRepository {

    private final JdbcTemplate jdbc;

    public SessionInvalidationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int invalidateAllForPrincipal(UUID userId) {
        return jdbc.update("delete from perfumestore.spring_session where principal_name = ?",
                userId.toString());
    }
}
