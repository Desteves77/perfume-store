package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.perfumestore.backend.auth.AdminProvisioningService;
import com.perfumestore.backend.config.AdminProvisioningProperties;
import com.perfumestore.backend.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AdminProvisioningIntegrationTest {

    private static final String PASSWORD = "DedicatedAdminPassword123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private AdminProvisioningService provisioning;

    @Autowired
    private UserRepository users;

    @Test
    void provisionsFirstAdminIdempotentlyWithoutPromotingCustomer() {
        AdminProvisioningProperties properties = properties("admin@example.test", PASSWORD);
        provisioning.provisionFirstAdmin(properties);
        provisioning.provisionFirstAdmin(properties);

        assertThat(users.countAdmins()).isEqualTo(1);
        assertThat(users.findByEmailNormalized("admin@example.test")).get()
                .extracting(user -> user.role().name())
                .isEqualTo("ADMIN");
    }

    @Test
    void refusesToPromoteExistingCustomer() {
        AdminProvisioningProperties customer = properties("customer@example.test", PASSWORD);
        users.insert(java.util.UUID.randomUUID(), "Cliente", customer.getEmail(), customer.getEmail(),
                "hash", com.perfumestore.backend.user.UserRole.CUSTOMER,
                com.perfumestore.backend.user.UserStatus.ACTIVE, java.time.Instant.now());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> provisioning.provisionFirstAdmin(customer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("promoção automática bloqueada");
    }

    private AdminProvisioningProperties properties(String email, String password) {
        AdminProvisioningProperties properties = new AdminProvisioningProperties();
        properties.setName("Administrador Inicial");
        properties.setEmail(email);
        properties.setPassword(password);
        properties.setEnabled(true);
        return properties;
    }
}
