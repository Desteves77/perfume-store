package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.perfumestore.backend.notification.CapturedEmailSender;
import com.perfumestore.backend.notification.EmailMessage;
import com.perfumestore.backend.notification.NotificationDispatcher;
import com.perfumestore.backend.notification.NotificationService;
import com.perfumestore.backend.order.PaymentStatus;
import com.perfumestore.backend.user.UserRepository;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired private NotificationService notifications;
    @Autowired private NotificationDispatcher dispatcher;
    @Autowired private CapturedEmailSender emails;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private MockMvc mockMvc;

    @AfterEach
    void cleanup() {
        emails.clear();
        jdbc.update("delete from perfumestore.notifications");
        jdbc.update("delete from perfumestore.app_users");
    }

    @Test
    void notificationIsTransactionalDeduplicatedAndDeliveredOnce() {
        UUID userId = user("delivery");
        UUID orderId = null;
        NotificationService.PaymentApprovedData data = new NotificationService.PaymentApprovedData(orderId, userId,
                "PS-TEST-DELIVERY", Instant.now(), new BigDecimal("25.90"), "BRL",
                com.perfumestore.backend.order.OrderStatus.CONFIRMED,
                com.perfumestore.backend.order.PaymentStatus.APPROVED);

        notifications.enqueuePaymentApproved(data);
        notifications.enqueuePaymentApproved(data);
        assertThat(count()).isEqualTo(1);

        assertThat(dispatcher.dispatchOnce()).isEqualTo(1);
        assertThat(dispatcher.dispatchOnce()).isZero();
        assertThat(emails.deliveredMessages()).hasSize(1);
        assertThat(emails.deliveredMessages().get(0).subject()).contains("PS-TEST-DELIVERY");
        assertThat(jdbc.queryForObject("select status from perfumestore.notifications", String.class)).isEqualTo("SENT");
        assertThat(emails.deliveredMessages().get(0).html()).contains("<p>").doesNotContain("<script>");
    }

    @Test
    void htmlAlternativeEscapesDynamicCustomerContent() {
        UUID userId = insertUser("<Cliente>", "notification-html");
        notifications.enqueueOrderReceived(new NotificationService.OrderCreatedData(null, userId,
                "PS-HTML", Instant.now(), List.of(new NotificationService.Item("Perfume <A>", 1,
                        new BigDecimal("10.00"))), new BigDecimal("10.00"), BigDecimal.ZERO,
                new BigDecimal("10.00"), "BRL", PaymentStatus.PENDING));

        dispatcher.dispatchOnce();

        EmailMessage delivered = emails.deliveredMessages().get(0);
        assertThat(delivered.text()).contains("Perfume <A>");
        assertThat(delivered.html()).contains("&lt;Cliente&gt;").contains("Perfume &lt;A&gt;")
                .doesNotContain("<Cliente>");
    }

    @Test
    void failureIsRetriedWithoutChangingCommercialData() {
        UUID userId = user("retry");
        notifications.enqueueOrderShipped(new NotificationService.OrderShippedData(null, userId,
                "PS-TEST-RETRY", Instant.now(), "Entrega local", null));
        emails.failDelivery();

        assertThat(dispatcher.dispatchOnce()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from perfumestore.notifications", String.class))
                .isEqualTo("RETRY_PENDING");
        assertThat(jdbc.queryForObject("select attempt_count from perfumestore.notifications", Integer.class))
                .isEqualTo(1);

        jdbc.update("update perfumestore.notifications set next_attempt_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        emails.clear();
        assertThat(dispatcher.dispatchOnce()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from perfumestore.notifications", String.class)).isEqualTo("SENT");
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.notification_attempts", Integer.class)).isEqualTo(2);
    }

    @Test
    void rollbackDoesNotLeaveNotification() {
        UUID userId = user("rollback");
        assertThatThrownBy(() -> transactions.execute(status -> {
            notifications.enqueueOrderReceived(new NotificationService.OrderCreatedData(null, userId,
                    "PS-TEST-ROLLBACK", Instant.now(), List.of(new NotificationService.Item("Teste", 1,
                            new BigDecimal("10.00"))), new BigDecimal("10.00"), BigDecimal.ZERO,
                    new BigDecimal("10.00"), "BRL", com.perfumestore.backend.order.PaymentStatus.PENDING));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count()).isZero();
    }

    @Test
    void concurrentWorkersClaimOneNotification() throws Exception {
        UUID userId = user("concurrent");
        notifications.enqueuePaymentApproved(new NotificationService.PaymentApprovedData(null, userId,
                "PS-TEST-CONCURRENT", Instant.now(), new BigDecimal("12.00"), "BRL",
                com.perfumestore.backend.order.OrderStatus.CONFIRMED,
                com.perfumestore.backend.order.PaymentStatus.APPROVED));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(dispatcher::dispatchOnce);
            Future<Integer> second = executor.submit(dispatcher::dispatchOnce);
            first.get(); second.get();
        } finally {
            executor.shutdownNow();
        }
        assertThat(emails.deliveredMessages()).hasSize(1);
        assertThat(jdbc.queryForObject("select attempt_count from perfumestore.notifications", Integer.class)).isEqualTo(1);
    }

    @Test
    void notificationHistoryIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications")).andExpect(status().isUnauthorized());
    }

    private UUID user(String suffix) {
        return insertUser("Cliente", suffix);
    }

    private UUID insertUser(String displayName, String suffix) {
        UUID id = UUID.randomUUID();
        String email = "notification-" + suffix + "-" + id + "@example.test";
        users.insert(id, displayName, email, email.toLowerCase(), "hash", UserRole.CUSTOMER, UserStatus.ACTIVE, Instant.now());
        return id;
    }

    private int count() {
        return jdbc.queryForObject("select count(*) from perfumestore.notifications", Integer.class);
    }
}
