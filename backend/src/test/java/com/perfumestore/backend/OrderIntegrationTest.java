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

import com.perfumestore.backend.address.AddressDtos;
import com.perfumestore.backend.address.AddressService;
import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.order.OrderDtos;
import com.perfumestore.backend.order.OrderService;
import com.perfumestore.backend.shipping.ShippingService;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AddressService addresses;
    @Autowired ShippingService shipping;
    @Autowired OrderService orders;

    @AfterEach
    void cleanOrderFixtures() {
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
    void createsOrderWithSnapshotsReservationAndIdempotentReplay() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("120.00"));
        AppUserPrincipal customer = customer("order-create");
        AddressDtos.AddressResponse address = addresses.create(customer.id(), addressRequest());
        UUID quote = shipping.quote(customer.id(), address.id()).quoteId();
        mockMvc.perform(post("/api/v1/cart/items").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":2}"))
                .andExpect(status().isOk());
        MvcResult created = mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf())
                .header("Idempotency-Key", "order-key-001").contentType("application/json")
                .content("{\"addressId\":\"" + address.id() + "\",\"quoteId\":\"" + quote + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.subtotal.amount").value("240.00")).andExpect(jsonPath("$.shipping.amount").value("15.90"))
                .andExpect(jsonPath("$.total.amount").value("255.90")).andReturn();
        String number = new tools.jackson.databind.ObjectMapper().readTree(created.getResponse().getContentAsString()).get("publicNumber").asText();
        mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf()).header("Idempotency-Key", "order-key-001")
                .contentType("application/json").content("{\"addressId\":\"" + address.id() + "\",\"quoteId\":\"" + quote + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.publicNumber").value(number));
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.orders", Integer.class)).isEqualTo(1);
        mockMvc.perform(post("/api/v1/dev/payment/orders/" + number).with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"outcome\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(3);
    }

    @Test
    void rollsBackAllReservationsWhenOneItemIsInsufficient() throws Exception {
        Fixture first = fixture(5, new BigDecimal("20.00"));
        Fixture second = fixture(0, new BigDecimal("30.00"));
        AppUserPrincipal customer = customer("order-rollback");
        AddressDtos.AddressResponse address = addresses.create(customer.id(), addressRequest());
        UUID quote = shipping.quote(customer.id(), address.id()).quoteId();
        add(customer, first.presentationId, 1);
        add(customer, second.presentationId, 1);
        mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf()).header("Idempotency-Key", "order-key-rollback")
                .contentType("application/json").content("{\"addressId\":\"" + address.id() + "\",\"quoteId\":\"" + quote + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.orders", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, first.presentationId)).isEqualTo(0);
    }

    @Test
    void protectsCheckoutAndOrderOwnership() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/orders").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        AppUserPrincipal a = customer("order-owner-a");
        AppUserPrincipal b = customer("order-owner-b");
        assertThat(orders.list(a.id(), new com.perfumestore.backend.shared.api.PageQuery()).items()).isEmpty();
        mockMvc.perform(get("/api/v1/me/orders").with(user(a))).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        assertThat(b.id()).isNotEqualTo(a.id());
    }

    @Test
    void expiresPendingOrderAndReleasesReservation() throws Exception {
        Fixture fixture = fixture(2, new BigDecimal("40.00"));
        AppUserPrincipal customer = customer("order-expire");
        AddressDtos.AddressResponse address = addresses.create(customer.id(), addressRequest());
        UUID quote = shipping.quote(customer.id(), address.id()).quoteId();
        add(customer, fixture.presentationId, 1);
        MvcResult created = mockMvc.perform(post("/api/v1/checkout/orders").with(user(customer)).with(csrf()).header("Idempotency-Key", "order-key-expire")
                .contentType("application/json").content("{\"addressId\":\"" + address.id() + "\",\"quoteId\":\"" + quote + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String number = new tools.jackson.databind.ObjectMapper().readTree(created.getResponse().getContentAsString()).get("publicNumber").asText();
        jdbc.update("update perfumestore.orders set payment_expires_at = ? where public_number = ?", Timestamp.from(Instant.now().minusSeconds(5)), number);
        assertThat(orders.expireDue("test-expire")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(0);
        mockMvc.perform(get("/api/v1/me/orders/" + number).with(user(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(orders.expireDue("test-expire-again")).isZero();
    }

    @Test
    void concurrentSameIdempotencyKeyCreatesOneOrder() throws Exception {
        Fixture fixture = fixture(2, new BigDecimal("50.00"));
        AppUserPrincipal customer = customer("order-idempotent-concurrent");
        AddressDtos.AddressResponse address = addresses.create(customer.id(), addressRequest());
        UUID quote = shipping.quote(customer.id(), address.id()).quoteId();
        add(customer, fixture.presentationId, 1);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest(address.id(), quote);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<OrderDtos.OrderResponse> first = executor.submit(() -> concurrentCreate(customer.id(), request, start, ready));
        Future<OrderDtos.OrderResponse> second = executor.submit(() -> concurrentCreate(customer.id(), request, start, ready));
        assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(first.get(20, java.util.concurrent.TimeUnit.SECONDS).publicNumber())
                .isEqualTo(second.get(20, java.util.concurrent.TimeUnit.SECONDS).publicNumber());
        executor.shutdownNow();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.orders", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.stock_reservations", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentCustomersCannotReserveTheLastUnitTwice() throws Exception {
        Fixture fixture = fixture(1, new BigDecimal("60.00"));
        AppUserPrincipal a = customer("order-race-a"), b = customer("order-race-b");
        AddressDtos.AddressResponse addressA = addresses.create(a.id(), addressRequest());
        AddressDtos.AddressResponse addressB = addresses.create(b.id(), addressRequest());
        UUID quoteA = shipping.quote(a.id(), addressA.id()).quoteId();
        UUID quoteB = shipping.quote(b.id(), addressB.id()).quoteId();
        add(a, fixture.presentationId, 1); add(b, fixture.presentationId, 1);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<OrderDtos.OrderResponse> first = executor.submit(() -> concurrentCreate(a.id(), new OrderDtos.CheckoutRequest(addressA.id(), quoteA), start, ready, "race-a-001"));
        Future<OrderDtos.OrderResponse> second = executor.submit(() -> concurrentCreate(b.id(), new OrderDtos.CheckoutRequest(addressB.id(), quoteB), start, ready, "race-b-001"));
        assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); start.countDown();
        int successes = 0;
        try { first.get(20, java.util.concurrent.TimeUnit.SECONDS); successes++; } catch (Exception ignored) { }
        try { second.get(20, java.util.concurrent.TimeUnit.SECONDS); successes++; } catch (Exception ignored) { }
        executor.shutdownNow();
        assertThat(successes).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?", Integer.class, fixture.presentationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.orders", Integer.class)).isEqualTo(1);
    }

    private OrderDtos.OrderResponse concurrentCreate(UUID userId, OrderDtos.CheckoutRequest request,
            CountDownLatch start, CountDownLatch ready) throws Exception {
        return concurrentCreate(userId, request, start, ready, "same-key-001");
    }
    private OrderDtos.OrderResponse concurrentCreate(UUID userId, OrderDtos.CheckoutRequest request,
            CountDownLatch start, CountDownLatch ready, String key) throws Exception {
        ready.countDown(); start.await(10, java.util.concurrent.TimeUnit.SECONDS);
        return orders.create(userId, request, key, "test-concurrency");
    }

    private void add(AppUserPrincipal customer, UUID presentation, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").with(user(customer)).with(csrf()).contentType("application/json")
                .content("{\"presentationId\":\"" + presentation + "\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());
    }
    private AddressDtos.AddressRequest addressRequest() {
        return new AddressDtos.AddressRequest("Cliente Teste", "Casa", "01311-000", "Rua Teste", "10", null, "Centro", "São Paulo", "SP", null);
    }
    private Fixture fixture(int physical, BigDecimal price) {
        UUID category = UUID.randomUUID(), product = UUID.randomUUID(), presentation = UUID.randomUUID();
        Instant now = Instant.now(); String slug = "order-" + product.toString().replace("-", "").substring(0, 12);
        jdbc.update("insert into perfumestore.categories (id, slug, name, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)", category, slug, "Categoria " + slug, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.products (id, slug, name, brand, description, category_id, active, version, created_at, updated_at) values (?, ?, ?, 'Marca Teste', 'Descrição', ?, true, 0, ?, ?)", product, slug, "Produto " + slug, category, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_presentations (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version, created_at, updated_at) values (?, ?, ?, ?, 50.00, 'ML', ?, 'BRL', true, 0, ?, ?)", presentation, product, "SKU-" + slug, ("SKU-" + slug).toUpperCase(), price, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_images (id, product_id, storage_key, content_type, byte_size, width, height, alt_text, position, is_primary, created_at, updated_at) values (?, ?, ?, 'image/png', 10, 1, 1, 'Imagem Teste', 0, true, ?, ?)", UUID.randomUUID(), product, "order-test/" + slug + ".png", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.inventory_balances (presentation_id, physical_quantity, reserved_quantity, version, created_at, updated_at) values (?, ?, 0, 0, ?, ?)", presentation, physical, Timestamp.from(now), Timestamp.from(now));
        return new Fixture(presentation, product);
    }
    private AppUserPrincipal customer(String suffix) {
        UUID id = UUID.randomUUID(); Instant now = Instant.now(); String email = suffix + "@test.local"; String hash = encoder.encode("StrongPassword123!");
        jdbc.update("insert into perfumestore.app_users (id, display_name, email, email_normalized, password_hash, role, status, created_at, updated_at, password_changed_at) values (?, ?, ?, ?, ?, 'CUSTOMER', 'ACTIVE', ?, ?, ?)", id, "Cliente " + suffix, email, email, hash, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return AppUserPrincipal.from(new AppUser(id, "Cliente " + suffix, email, email, hash, UserRole.CUSTOMER, UserStatus.ACTIVE, now, now, now));
    }
    private record Fixture(UUID presentationId, UUID productId) { }
}
