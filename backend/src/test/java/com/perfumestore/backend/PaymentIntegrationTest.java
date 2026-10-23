package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.perfumestore.backend.address.AddressDtos;
import com.perfumestore.backend.address.AddressService;
import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.order.OrderDtos;
import com.perfumestore.backend.order.OrderService;
import com.perfumestore.backend.payment.PaymentDtos;
import com.perfumestore.backend.payment.MockWebhookVerifier;
import com.perfumestore.backend.payment.PaymentAttemptStatus;
import com.perfumestore.backend.payment.PaymentRepository;
import com.perfumestore.backend.payment.PaymentService;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;
import com.perfumestore.backend.shipping.ShippingService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AddressService addresses;
    @Autowired ShippingService shipping;
    @Autowired OrderService orders;
    @Autowired PaymentService payments;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MockWebhookVerifier verifier;

    @AfterEach
    void clean() {
        jdbc.update("delete from perfumestore.payment_reconciliations");
        jdbc.update("delete from perfumestore.mock_payment_states");
        jdbc.update("delete from perfumestore.payment_attempt_history");
        jdbc.update("delete from perfumestore.payment_events");
        jdbc.update("delete from perfumestore.payment_attempts");
        jdbc.update("delete from perfumestore.order_status_history");
        jdbc.update("delete from perfumestore.order_reservations");
        jdbc.update("delete from perfumestore.order_address_snapshots");
        jdbc.update("delete from perfumestore.order_shipping_snapshots");
        jdbc.update("delete from perfumestore.order_items");
        jdbc.update("delete from perfumestore.inventory_movements");
        jdbc.update("delete from perfumestore.stock_reservations");
        jdbc.update("delete from perfumestore.orders");
    }

    @Test
    void createsCheckoutApprovesThroughSignedWebhookAndIgnoresDuplicate() throws Exception {
        PendingOrder pending = pendingOrder(2, new BigDecimal("40.00"), "payment-approval");
        MvcResult created = mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-001"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.provider").value("MOCK")).andReturn();
        String providerPaymentId = new ObjectMapper().readTree(created.getResponse().getContentAsString()).get("providerPaymentId").asText();
        mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.providerPaymentId").value(providerPaymentId));

        String body = event("payment-event-approval", "PAYMENT_APPROVED", providerPaymentId, pending.total.toPlainString());
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", verifier.sign(body, timestamp)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.processingStatus").value("PROCESSED"));
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", verifier.sign(body, timestamp)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

        assertThat(jdbc.queryForObject("select count(*) from perfumestore.payment_events", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isZero();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/me/orders/" + pending.publicNumber).with(user(pending.customer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void rejectsInvalidSignatureAndRoutesAmountMismatchToReview() throws Exception {
        PendingOrder pending = pendingOrder(2, new BigDecimal("40.00"), "payment-review");
        MvcResult created = mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-002"))
                .andExpect(status().isCreated()).andReturn();
        String providerPaymentId = new ObjectMapper().readTree(created.getResponse().getContentAsString()).get("providerPaymentId").asText();
        String body = event("payment-event-mismatch", "PAYMENT_APPROVED", providerPaymentId, "39.99");
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", "sha256=00"))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.payment_events", Integer.class)).isZero();
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", verifier.sign(body, timestamp)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.processingStatus").value("REVIEW"));
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isEqualTo(2);
    }

    @Test
    void lateApprovalMovesOrderToPaymentReviewWithoutRecreatingReservation() throws Exception {
        PendingOrder pending = pendingOrder(1, new BigDecimal("25.00"), "payment-late");
        MvcResult created = mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-003"))
                .andExpect(status().isCreated()).andReturn();
        String providerPaymentId = new ObjectMapper().readTree(created.getResponse().getContentAsString()).get("providerPaymentId").asText();
        jdbc.update("update perfumestore.orders set payment_expires_at = ? where public_number = ?",
                Timestamp.from(Instant.now().minusSeconds(5)), pending.publicNumber);
        assertThat(orders.expireDue("test-payment-expire")).isEqualTo(1);
        String body = event("payment-event-late", "PAYMENT_APPROVED", providerPaymentId, pending.total.toPlainString());
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", verifier.sign(body, timestamp)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.paymentStatus").value("APPROVED"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/me/orders/" + pending.publicNumber).with(user(pending.customer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAYMENT_REVIEW"));
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isZero();
    }

    @Test
    void duplicateWebhookProcessedSafelyUnderConcurrentTransactions() throws Exception {
        PendingOrder pending = pendingOrder(1, new BigDecimal("30.00"), "payment-concurrency");
        MvcResult created = mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-004"))
                .andExpect(status().isCreated()).andReturn();
        String providerPaymentId = new ObjectMapper().readTree(created.getResponse().getContentAsString()).get("providerPaymentId").asText();
        PaymentDtos.EventPayload payload = new PaymentDtos.EventPayload("payment-event-concurrent", "PAYMENT_APPROVED",
                providerPaymentId, Instant.now(), pending.total, "BRL");
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<PaymentDtos.WebhookResponse> first = executor.submit(() -> process(payload, ready, start));
        Future<PaymentDtos.WebhookResponse> second = executor.submit(() -> process(payload, ready, start));
        assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        start.countDown();
        PaymentDtos.WebhookResponse one = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
        PaymentDtos.WebhookResponse two = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdownNow();
        assertThat(one.processingStatus()).isIn("PROCESSED", "IGNORED");
        assertThat(two.processingStatus()).isIn("PROCESSED", "IGNORED");
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.payment_events", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where reservation_id is not null and movement_type = 'CONFIRMATION'", Integer.class)).isEqualTo(1);
    }

    @Test
    void reconciliationDiscoversApprovalAndRemainsIdempotent() throws Exception {
        PendingOrder pending = pendingOrder(1, new BigDecimal("35.00"), "payment-reconcile");
        MvcResult created = mockMvc.perform(post("/api/v1/me/orders/" + pending.publicNumber + "/payment-sessions")
                .with(user(pending.customer)).with(csrf()).header("Idempotency-Key", "payment-session-005"))
                .andExpect(status().isCreated()).andReturn();
        var paymentJson = new ObjectMapper().readTree(created.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(paymentJson.get("paymentId").asText());
        String providerPaymentId = paymentJson.get("providerPaymentId").asText();
        Instant occurredAt = Instant.now();
        paymentRepository.updateMockStateIfNewer(providerPaymentId, PaymentAttemptStatus.APPROVED, pending.total,
                "BRL", "mock-reconcile-approved", occurredAt, occurredAt);

        assertThat(payments.reconcile(paymentId, "payment-reconcile").result()).isEqualTo("APPLIED");
        assertThat(payments.reconcile(paymentId, "payment-reconcile-repeat").result()).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, pending.presentationId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where reservation_id is not null and movement_type = 'CONFIRMATION'", Integer.class)).isEqualTo(1);
    }

    private PaymentDtos.WebhookResponse process(PaymentDtos.EventPayload payload, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(10, java.util.concurrent.TimeUnit.SECONDS);
        return payments.processWebhook(payload, "a".repeat(64), true, "payment-concurrency");
    }

    private String event(String id, String type, String providerPaymentId, String amount) {
        return "{\"eventId\":\"" + id + "\",\"eventType\":\"" + type + "\",\"providerPaymentId\":\""
                + providerPaymentId + "\",\"occurredAt\":\"" + Instant.now() + "\",\"amount\":\"" + amount
                + "\",\"currency\":\"BRL\"}";
    }

    private PendingOrder pendingOrder(int physical, BigDecimal price, String suffix) throws Exception {
        Fixture fixture = fixture(physical, price, suffix);
        AppUserPrincipal customer = customer(suffix);
        AddressDtos.AddressResponse address = addresses.create(customer.id(), new AddressDtos.AddressRequest("Cliente Teste", "Casa", "01311-000", "Rua Teste", "10", null, "Centro", "São Paulo", "SP", null));
        UUID quote = shipping.quote(customer.id(), address.id()).quoteId();
        mockMvc.perform(post("/api/v1/cart/items").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
        MvcResult created = mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf())
                .header("Idempotency-Key", "order-" + suffix + "-001").contentType("application/json")
                .content("{\"addressId\":\"" + address.id() + "\",\"quoteId\":\"" + quote + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        var orderJson = new ObjectMapper().readTree(created.getResponse().getContentAsString());
        String number = orderJson.get("publicNumber").asText();
        BigDecimal total = new BigDecimal(orderJson.get("total").get("amount").asText());
        return new PendingOrder(number, customer, fixture.presentationId, total);
    }

    private Fixture fixture(int physical, BigDecimal price, String suffix) {
        UUID category = UUID.randomUUID(), product = UUID.randomUUID(), presentation = UUID.randomUUID();
        Instant now = Instant.now(); String slug = "payment-" + suffix + "-" + product.toString().replace("-", "").substring(0, 10);
        jdbc.update("insert into perfumestore.categories (id, slug, name, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)", category, slug, "Categoria " + slug, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.products (id, slug, name, brand, description, category_id, active, version, created_at, updated_at) values (?, ?, ?, 'Marca Teste', 'Descricao', ?, true, 0, ?, ?)", product, slug, "Produto " + slug, category, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_presentations (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version, created_at, updated_at) values (?, ?, ?, ?, 50.00, 'ML', ?, 'BRL', true, 0, ?, ?)", presentation, product, "SKU-" + slug, ("SKU-" + slug).toUpperCase(), price, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_images (id, product_id, storage_key, content_type, byte_size, width, height, alt_text, position, is_primary, created_at, updated_at) values (?, ?, ?, 'image/png', 10, 1, 1, 'Imagem Teste', 0, true, ?, ?)", UUID.randomUUID(), product, "payment-test/" + slug + ".png", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.inventory_balances (presentation_id, physical_quantity, reserved_quantity, version, created_at, updated_at) values (?, ?, 0, 0, ?, ?)", presentation, physical, Timestamp.from(now), Timestamp.from(now));
        return new Fixture(presentation);
    }

    private AppUserPrincipal customer(String suffix) {
        UUID id = UUID.randomUUID(); Instant now = Instant.now(); String email = suffix + "-" + id.toString().substring(0, 8) + "@test.local"; String hash = encoder.encode("StrongPassword123!");
        jdbc.update("insert into perfumestore.app_users (id, display_name, email, email_normalized, password_hash, role, status, created_at, updated_at, password_changed_at) values (?, ?, ?, ?, ?, 'CUSTOMER', 'ACTIVE', ?, ?, ?)", id, "Cliente " + suffix, email, email, hash, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return AppUserPrincipal.from(new AppUser(id, "Cliente " + suffix, email, email, hash, UserRole.CUSTOMER, UserStatus.ACTIVE, now, now, now));
    }

    private record Fixture(UUID presentationId) { }
    private record PendingOrder(String publicNumber, AppUserPrincipal customer, UUID presentationId, BigDecimal total) { }
}
