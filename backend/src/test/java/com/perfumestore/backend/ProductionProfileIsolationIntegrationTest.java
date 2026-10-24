package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.perfumestore.backend.payment.MockPaymentProvider;
import com.perfumestore.backend.payment.PaymentProvider;
import com.perfumestore.backend.payment.PaymentSimulationController;
import com.perfumestore.backend.payment.MockPaymentWebhookController;
import com.perfumestore.backend.payment.MockRefundController;
import com.perfumestore.backend.payment.UnavailablePaymentProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "server.port=0",
        "cors.allowed-origins=https://localhost:18680",
        "app.cors.allowed-origins=https://localhost:18680",
        "app.password-recovery.frontend-base-url=https://localhost:18680/reset-password",
        "app.password-recovery.sender-address=no-reply@invalid.local",
        "app.catalog.images.storage-root=target/prod-isolation-images",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=65530"
})
class ProductionProfileIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private ApplicationContext context;

    @Test
    void productionDoesNotRegisterMockPaymentComponentsOrControllers() {
        assertThat(context.getBeansOfType(MockPaymentProvider.class)).isEmpty();
        assertThat(context.getBeansOfType(PaymentSimulationController.class)).isEmpty();
        assertThat(context.getBeansOfType(MockPaymentWebhookController.class)).isEmpty();
        assertThat(context.getBeansOfType(MockRefundController.class)).isEmpty();
        assertThat(context.getBeansOfType(PaymentProvider.class)).hasSize(1);
        assertThat(context.getBean(PaymentProvider.class)).isInstanceOf(UnavailablePaymentProvider.class);
    }
}
