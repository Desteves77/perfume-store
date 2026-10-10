package com.perfumestore.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration(proxyBeanMethods = false)
@EnableJdbcHttpSession(tableName = "SPRING_SESSION", cleanupCron = "0 * * * * *")
public class SessionConfiguration {

    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> sessionTimeoutCustomizer(
            @Value("${spring.session.timeout:30m}") java.time.Duration timeout) {
        return repository -> repository.setDefaultMaxInactiveInterval(timeout);
    }
}
