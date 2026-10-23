package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.order.AdminOrderService;
import com.perfumestore.backend.order.OrderService;
import com.perfumestore.backend.payment.MockWebhookVerifier;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

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
class PostSaleIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired MockWebhookVerifier verifier;
    @Autowired AdminOrderService adminOrders;
    @Autowired OrderService orders;

    @AfterEach
    void clean() {
        jdbc.update("delete from perfumestore.return_restock_operations");
        jdbc.update("delete from perfumestore.return_history");
        jdbc.update("delete from perfumestore.returns");
        jdbc.update("delete from perfumestore.mock_refund_states");
        jdbc.update("delete from perfumestore.refund_history");
        jdbc.update("delete from perfumestore.refunds");
        jdbc.update("delete from perfumestore.admin_order_history");
        jdbc.update("delete from perfumestore.order_logistics");
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
    void adminCanPrepareShipAndDeliverAnApprovedOrder() throws Exception {
        Fixture fixture = approvedOrder(3, "admin-flow");
        AppUserPrincipal admin = principalFor("admin-flow", UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/orders").with(user(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].publicNumber").value(fixture.publicNumber));
        mockMvc.perform(get("/api/v1/admin/orders/" + fixture.publicNumber).with(user(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.publicNumber").value(fixture.publicNumber));
        mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/prepare").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "prepare-flow-001").contentType("application/json")
                .content("{\"reason\":\"Separacao aprovada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.deliveryStatus").value("PREPARING"));
        mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/ship").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "ship-flow-001").contentType("application/json")
                .content("{\"carrier\":\"Operacao Local\",\"trackingCode\":\"BR12345\",\"reason\":\"Despachado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.deliveryStatus").value("SHIPPED"));
        mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/deliver").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "deliver-flow-001").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.deliveryStatus").value("DELIVERED"));
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.admin_order_history where order_id = ?", Integer.class, fixture.orderId)).isEqualTo(3);
    }

    @Test
    void repeatedPreparationIsIdempotentUnderConcurrentPostgresTransactions() throws Exception {
        Fixture fixture = approvedOrder(2, "admin-concurrency");
        AppUserPrincipal admin = principalFor("admin-concurrency", UserRole.ADMIN);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit((java.util.concurrent.Callable<Void>) () -> {
            prepareConcurrently(fixture.publicNumber, admin.id(), ready, start);
            return null;
        });
        Future<?> second = executor.submit((java.util.concurrent.Callable<Void>) () -> {
            prepareConcurrently(fixture.publicNumber, admin.id(), ready, start);
            return null;
        });
        assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        start.countDown();
        first.get(20, java.util.concurrent.TimeUnit.SECONDS);
        second.get(20, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdownNow();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.admin_order_history where order_id = ? and operation = 'PREPARE'", Integer.class, fixture.orderId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select delivery_status from perfumestore.orders where id = ?", String.class, fixture.orderId)).isEqualTo("PREPARING");
    }

    @Test
    void pendingOrderCancellationReleasesReservationExactlyOnce() throws Exception {
        Fixture fixture = pendingOrder(2, "cancel-flow");
        AppUserPrincipal admin = principalFor("cancel-flow", UserRole.ADMIN);
        mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/cancel").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "cancel-flow-001").contentType("application/json")
                .content("{\"reason\":\"Solicitacao administrativa\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/cancel").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "cancel-flow-001").contentType("application/json")
                .content("{\"reason\":\"Solicitacao administrativa\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.status").value("CANCELLED"));
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isZero();
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where movement_type = 'RELEASE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void mockRefundRequiresProviderResultAndDoesNotRestock() throws Exception {
        Fixture fixture = approvedOrder(3, "refund-flow");
        AppUserPrincipal admin = principalFor("refund-flow", UserRole.ADMIN);
        MvcResult requested = mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/refunds").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "refund-flow-001").contentType("application/json")
                .content("{\"reason\":\"Cancelamento aprovado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("PENDING_PROVIDER")).andReturn();
        String refundId = new ObjectMapper().readTree(requested.getResponse().getContentAsString()).get("summary").get("id").asText();
        mockMvc.perform(post("/api/v1/dev/payment/refunds/" + refundId + "/simulate").with(user(admin)).with(csrf())
                .contentType("application/json").content("{\"outcome\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/v1/admin/orders/" + fixture.publicNumber).with(user(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order.paymentStatus").value("REFUNDED"));
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where presentation_id = ? and movement_type = 'RESTOCK'", Integer.class, fixture.presentationId)).isZero();
    }

    @Test
    void returnInspectionAndRestockAreAuditedAndIdempotent() throws Exception {
        Fixture fixture = approvedOrder(2, "return-flow");
        AppUserPrincipal admin = principalFor("return-flow", UserRole.ADMIN);
        prepareShipDeliver(fixture.publicNumber, admin);
        MvcResult requested = mockMvc.perform(post("/api/v1/admin/orders/" + fixture.publicNumber + "/returns").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "return-flow-001").contentType("application/json")
                .content("{\"orderItemId\":\"" + fixture.orderItemId + "\",\"quantity\":1,\"reason\":\"Devolucao para inspecao\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("REQUESTED")).andReturn();
        String returnId = new ObjectMapper().readTree(requested.getResponse().getContentAsString()).get("summary").get("id").asText();
        mockMvc.perform(post("/api/v1/admin/returns/" + returnId + "/receive").with(user(admin)).with(csrf())
                .contentType("application/json").content("{\"quantity\":1,\"conditionNote\":\"Recebido lacrado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("AWAITING_INSPECTION"));
        mockMvc.perform(post("/api/v1/admin/returns/" + returnId + "/inspect").with(user(admin)).with(csrf())
                .contentType("application/json").content("{\"approvedQuantity\":1,\"rejectedQuantity\":0,\"reason\":\"Aprovado para revenda\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("INSPECTED"));
        mockMvc.perform(post("/api/v1/admin/returns/" + returnId + "/restock").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "restock-flow-001").contentType("application/json").content("{\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("RESTOCKED"));
        mockMvc.perform(post("/api/v1/admin/returns/" + returnId + "/restock").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "restock-flow-001").contentType("application/json").content("{\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.status").value("RESTOCKED"));
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.return_restock_operations where return_id = ?", Integer.class, UUID.fromString(returnId))).isEqualTo(1);
    }

    @Test
    void adminSurfacePreservesAuthorizationAndCsrf() throws Exception {
        AppUserPrincipal customer = principalFor("security-customer", UserRole.CUSTOMER);
        AppUserPrincipal admin = principalFor("security-admin", UserRole.ADMIN);
        mockMvc.perform(get("/api/v1/admin/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/orders").with(user(customer))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/orders/unknown/prepare").with(user(admin)).contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    private void prepareConcurrently(String publicNumber, UUID actor, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(10, java.util.concurrent.TimeUnit.SECONDS);
        adminOrders.prepare(publicNumber, "prepare-concurrent-001", actor, "Separacao concorrente", "phase10-test");
    }

    private void prepareShipDeliver(String publicNumber, AppUserPrincipal admin) throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/" + publicNumber + "/prepare").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "prepare-return-001").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + publicNumber + "/ship").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "ship-return-001").contentType("application/json")
                .content("{\"carrier\":\"Operacao Local\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + publicNumber + "/deliver").with(user(admin)).with(csrf())
                .header("Idempotency-Key", "deliver-return-001").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }

    private Fixture approvedOrder(int physical, String suffix) throws Exception {
        Fixture fixture = pendingOrder(physical, suffix);
        AppUserPrincipal customer = fixture.customer;
        MvcResult session = mockMvc.perform(post("/api/v1/me/orders/" + fixture.publicNumber + "/payment-sessions")
                .with(user(customer)).with(csrf()).header("Idempotency-Key", "payment-" + suffix + "-001"))
                .andExpect(status().isCreated()).andReturn();
        String providerPaymentId = new ObjectMapper().readTree(session.getResponse().getContentAsString()).get("providerPaymentId").asText();
        String body = "{\"eventId\":\"event-" + suffix + "\",\"eventType\":\"PAYMENT_APPROVED\",\"providerPaymentId\":\""
                + providerPaymentId + "\",\"occurredAt\":\"" + Instant.now() + "\",\"amount\":\"" + fixture.total.toPlainString() + "\",\"currency\":\"BRL\"}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        mockMvc.perform(post("/api/v1/webhooks/payments/mock").contentType("application/json").content(body)
                .header("X-Mock-Timestamp", timestamp).header("X-Mock-Signature", verifier.sign(body, timestamp)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.processingStatus").value("PROCESSED"));
        return fixture;
    }

    private Fixture pendingOrder(int physical, String suffix) throws Exception {
        UUID category = UUID.randomUUID(), product = UUID.randomUUID(), presentation = UUID.randomUUID();
        Instant now = Instant.now();
        String slug = "phase10-" + suffix + "-" + product.toString().replace("-", "").substring(0, 10);
        jdbc.update("insert into perfumestore.categories (id, slug, name, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)", category, slug, "Categoria " + slug, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.products (id, slug, name, brand, description, category_id, active, version, created_at, updated_at) values (?, ?, ?, 'Marca Teste', 'Descricao', ?, true, 0, ?, ?)", product, slug, "Produto " + slug, category, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_presentations (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version, created_at, updated_at) values (?, ?, ?, ?, 50.00, 'ML', ?, 'BRL', true, 0, ?, ?)", presentation, product, "SKU-" + slug, ("SKU-" + slug).toUpperCase(), new BigDecimal("40.00"), Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_images (id, product_id, storage_key, content_type, byte_size, width, height, alt_text, position, is_primary, created_at, updated_at) values (?, ?, ?, 'image/png', 10, 1, 1, 'Imagem Teste', 0, true, ?, ?)", UUID.randomUUID(), product, "phase10-test/" + slug + ".png", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.inventory_balances (presentation_id, physical_quantity, reserved_quantity, version, created_at, updated_at) values (?, ?, 0, 0, ?, ?)", presentation, physical, Timestamp.from(now), Timestamp.from(now));
        AppUserPrincipal customer = principalFor(suffix, UserRole.CUSTOMER);
        MvcResult address = mockMvc.perform(post("/api/v1/me/addresses").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"recipientName\":\"Cliente Teste\",\"label\":\"Casa\",\"postalCode\":\"01311-000\",\"street\":\"Rua Teste\",\"number\":\"10\",\"neighborhood\":\"Centro\",\"city\":\"Sao Paulo\",\"state\":\"SP\"}"))
                .andExpect(status().isCreated()).andReturn();
        String addressId = new ObjectMapper().readTree(address.getResponse().getContentAsString()).get("id").asText();
        MvcResult quote = mockMvc.perform(post("/api/v1/shipping/quotes").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"addressId\":\"" + addressId + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String quoteId = new ObjectMapper().readTree(quote.getResponse().getContentAsString()).get("quoteId").asText();
        mockMvc.perform(post("/api/v1/cart/items").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"presentationId\":\"" + presentation + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
        MvcResult order = mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf())
                .header("Idempotency-Key", "order-" + suffix + "-001").contentType("application/json")
                .content("{\"addressId\":\"" + addressId + "\",\"quoteId\":\"" + quoteId + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        var json = new ObjectMapper().readTree(order.getResponse().getContentAsString());
        String number = json.get("publicNumber").asText();
        UUID orderItemId = jdbc.queryForObject("select id from perfumestore.order_items where order_id = (select id from perfumestore.orders where public_number = ?)", UUID.class, number);
        UUID orderId = jdbc.queryForObject("select id from perfumestore.orders where public_number = ?", UUID.class, number);
        return new Fixture(number, orderId, customer, presentation, orderItemId, new BigDecimal(json.get("total").get("amount").asText()));
    }

    private AppUserPrincipal principalFor(String suffix, UserRole role) {
        UUID id = UUID.randomUUID(); Instant now = Instant.now(); String email = "phase10-" + suffix + "-" + id.toString().substring(0, 8) + "@test.local";
        String hash = encoder.encode("StrongPassword123!");
        jdbc.update("insert into perfumestore.app_users (id, display_name, email, email_normalized, password_hash, role, status, created_at, updated_at, password_changed_at) values (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)", id, "Fase 10 " + suffix, email, email, hash, role.name(), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return AppUserPrincipal.from(new AppUser(id, "Fase 10 " + suffix, email, email, hash, role, UserStatus.ACTIVE, now, now, now));
    }

    private record Fixture(String publicNumber, UUID orderId, AppUserPrincipal customer, UUID presentationId, UUID orderItemId, BigDecimal total) { }
}
