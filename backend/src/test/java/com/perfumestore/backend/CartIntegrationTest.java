package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import java.util.concurrent.TimeUnit;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.cart.CartDtos;
import com.perfumestore.backend.cart.CartService;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.SpringBootTest;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CartService carts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsPersistentGuestCartWithOpaqueCookieWithoutReservingStock() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("120.00"));
        MvcResult empty = mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.items").isEmpty()).andReturn();
        assertThat(empty.getResponse().getHeader("Set-Cookie")).isNull();

        MvcResult added = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal.amount").value("240.00"))
                .andReturn();
        Cookie cookie = added.getResponse().getCookie("CART_TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).hasSizeGreaterThan(40);
        assertThat(added.getResponse().getHeader("Set-Cookie")).contains("HttpOnly", "Path=/api/v1", "SameSite=Lax");

        mockMvc.perform(get("/api/v1/cart").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?",
                Integer.class, fixture.presentationId)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select reserved_quantity from perfumestore.inventory_balances where presentation_id = ?",
                Integer.class, fixture.presentationId)).isEqualTo(0);
    }

    @Test
    void recalculatesCurrentPriceAndReportsLaterStockChange() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("120.00"));
        MvcResult added = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":2}"))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = added.getResponse().getCookie("CART_TOKEN");
        jdbc.update("update perfumestore.product_presentations set price = ? where id = ?", new BigDecimal("130.00"),
                fixture.presentationId);
        jdbc.update("update perfumestore.inventory_balances set physical_quantity = 1 where presentation_id = ?",
                fixture.presentationId);

        mockMvc.perform(get("/api/v1/cart").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice.amount").value("130.00"))
                .andExpect(jsonPath("$.items[0].issues[*].code").value(org.hamcrest.Matchers.hasItems("PRICE_CHANGED",
                        "QUANTITY_EXCEEDS_AVAILABLE")))
                .andExpect(jsonPath("$.canCheckout").value(false));
    }

    @Test
    void rejectsInvalidPublishedStateAndSupportsIdempotentAdd() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("99.90"));
        MvcResult first = mockMvc.perform(post("/api/v1/cart/items").with(csrf()).header("Idempotency-Key", "cart-op-001")
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        Cookie visitor = first.getResponse().getCookie("CART_TOKEN");
        MvcResult repeated = mockMvc.perform(post("/api/v1/cart/items").with(csrf()).header("Idempotency-Key", "cart-op-001")
                .cookie(visitor)
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(repeated.getResponse().getContentAsString()).contains("\"quantity\":1");
        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).header("Idempotency-Key", "cart-op-001")
                .cookie(visitor)
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        jdbc.update("update perfumestore.product_presentations set active = false where id = ?", fixture.presentationId);
        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).cookie(visitor)
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRESENTATION_NOT_ELIGIBLE"));
    }

    @Test
    void mergesVisitorCartOnRealLoginAndDoesNotDuplicateOnReplay() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("80.00"));
        String password = "CartPass123456!";
        AppUserPrincipal principal = customer("cart-login", password);
        mockMvc.perform(post("/api/v1/cart/items").with(user(principal)).with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
        MvcResult guest = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":2}"))
                .andExpect(status().isOk()).andReturn();
        Cookie visitor = guest.getResponse().getCookie("CART_TOKEN");

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).cookie(visitor)
                .contentType("application/json")
                .content("{\"email\":\"cart-login@test.local\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(login.getResponse().getHeaders("Set-Cookie")).anyMatch(value -> value.contains("CART_TOKEN="));
        Cookie session = login.getResponse().getCookie("SESSION");
        mockMvc.perform(get("/api/v1/cart").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(3));

        mockMvc.perform(post("/api/v1/auth/login").with(csrf()).cookie(visitor)
                .contentType("application/json")
                .content("{\"email\":\"cart-login@test.local\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.cart_items ci join perfumestore.carts c on c.id = ci.cart_id where c.user_id = ? and c.state = 'ACTIVE'",
                Integer.class, principal.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity from perfumestore.cart_items ci join perfumestore.carts c on c.id = ci.cart_id where c.user_id = ? and c.state = 'ACTIVE'",
                Integer.class, principal.id())).isEqualTo(3);
    }

    @Test
    void updatesAndRemovesItemsAndDoesNotTrustInvalidGuestToken() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("42.00"));
        mockMvc.perform(get("/api/v1/cart").cookie(new Cookie("CART_TOKEN", "invalid-token")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.items").isEmpty());

        MvcResult added = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        Cookie visitor = added.getResponse().getCookie("CART_TOKEN");
        mockMvc.perform(patch("/api/v1/cart/items/" + fixture.presentationId).with(csrf()).cookie(visitor)
                .contentType("application/json").content("{\"quantity\":3,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(3));
        mockMvc.perform(patch("/api/v1/cart/items/" + fixture.presentationId).with(csrf()).cookie(visitor)
                .contentType("application/json").content("{\"quantity\":4,\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
        mockMvc.perform(delete("/api/v1/cart/items/" + fixture.presentationId).with(csrf()).cookie(visitor))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/cart/items/" + fixture.presentationId).with(csrf()).cookie(visitor))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/cart").cookie(visitor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void isolatesCustomerCartsAndRequiresCsrfForMutation() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("50.00"));
        AppUserPrincipal owner = customer("cart-owner", "CartPass123456!");
        AppUserPrincipal other = customer("cart-other", "CartPass123456!");
        mockMvc.perform(post("/api/v1/cart/items").with(user(owner))
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/cart/items").with(user(owner)).with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cart").with(user(other))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void concurrentAddsKeepOneLineAndSumQuantity() throws Exception {
        Fixture fixture = fixture(10, new BigDecimal("40.00"));
        MvcResult guest = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = guest.getResponse().getCookie("CART_TOKEN");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit(() -> concurrentAdd(fixture.presentationId, cookie, ready, start));
        Future<?> second = executor.submit(() -> concurrentAdd(fixture.presentationId, cookie, ready, start));
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        first.get(20, TimeUnit.SECONDS);
        second.get(20, TimeUnit.SECONDS);
        executor.shutdownNow();
        UUID cartId = jdbc.queryForObject("select id from perfumestore.carts where visitor_token_hash is not null and state = 'ACTIVE' order by created_at desc limit 1",
                UUID.class);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.cart_items where cart_id = ? and presentation_id = ?",
                Integer.class, cartId, fixture.presentationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity from perfumestore.cart_items where cart_id = ? and presentation_id = ?",
                Integer.class, cartId, fixture.presentationId)).isEqualTo(3);
    }

    @Test
    void concurrentClaimsTransferVisitorCartOnlyOnce() throws Exception {
        Fixture fixture = fixture(5, new BigDecimal("40.00"));
        AppUserPrincipal principal = customer("cart-claim-concurrent", "CartPass123456!");
        mockMvc.perform(post("/api/v1/cart/items").with(user(principal)).with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
        MvcResult guest = mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                .contentType("application/json")
                .content("{\"presentationId\":\"" + fixture.presentationId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        Cookie visitor = guest.getResponse().getCookie("CART_TOKEN");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit(() -> concurrentClaim(principal.id(), visitor, ready, start));
        Future<?> second = executor.submit(() -> concurrentClaim(principal.id(), visitor, ready, start));
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        first.get(20, TimeUnit.SECONDS);
        second.get(20, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.carts where user_id = ? and state = 'ACTIVE'",
                Integer.class, principal.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity from perfumestore.cart_items ci join perfumestore.carts c on c.id = ci.cart_id where c.user_id = ? and c.state = 'ACTIVE' and ci.presentation_id = ?",
                Integer.class, principal.id(), fixture.presentationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.carts where state = 'TRANSFERRED'",
                Integer.class)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void expiresAndCleansAbandonedCartWithoutTouchingCatalogOrInventory() {
        Fixture fixture = fixture(4, new BigDecimal("35.00"));
        Instant old = Instant.now().minusSeconds(8 * 86_400L);
        UUID cartId = UUID.randomUUID();
        jdbc.update("""
                insert into perfumestore.carts
                    (id, visitor_token_hash, state, expires_at, last_activity_at, version, created_at, updated_at)
                values (?, ?, 'ACTIVE', ?, ?, 0, ?, ?)
                """, cartId, "a".repeat(64), Timestamp.from(old), Timestamp.from(old), Timestamp.from(old), Timestamp.from(old));
        jdbc.update("insert into perfumestore.cart_items (id, cart_id, presentation_id, quantity, price_reference, version, created_at, updated_at) values (?, ?, ?, 1, ?, 0, ?, ?)",
                UUID.randomUUID(), cartId, fixture.presentationId, new BigDecimal("35.00"), Timestamp.from(old), Timestamp.from(old));
        assertThat(carts.cleanup()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("select state from perfumestore.carts where id = ?", String.class, cartId))
                .isEqualTo("EXPIRED");
        jdbc.update("update perfumestore.carts set updated_at = ? where id = ?", Timestamp.from(old), cartId);
        assertThat(carts.cleanup()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.carts where id = ?", Integer.class, cartId)).isZero();
        assertThat(jdbc.queryForObject("select physical_quantity from perfumestore.inventory_balances where presentation_id = ?",
                Integer.class, fixture.presentationId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.product_presentations where id = ?",
                Integer.class, fixture.presentationId)).isEqualTo(1);
    }

    private void concurrentAdd(UUID presentationId, Cookie cookie, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(cookie);
            MockHttpServletResponse response = new MockHttpServletResponse();
            carts.add(null, request, response, new CartDtos.AddItemRequest(presentationId, 1), null);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void concurrentClaim(UUID userId, Cookie cookie, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(cookie);
            carts.claimVisitorCart(userId, request, new MockHttpServletResponse());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Fixture fixture(int physical, BigDecimal price) {
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        String slug = "cart-" + productId.toString().replace("-", "").substring(0, 12);
        Instant now = Instant.now();
        jdbc.update("insert into perfumestore.categories (id, slug, name, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)",
                categoryId, slug, "Categoria " + slug, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.products (id, slug, name, brand, description, category_id, active, version, created_at, updated_at) values (?, ?, ?, 'Marca Teste', 'Descrição', ?, true, 0, ?, ?)",
                productId, slug, "Produto " + slug, categoryId, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_presentations (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version, created_at, updated_at) values (?, ?, ?, ?, 50.00, 'ML', ?, 'BRL', true, 0, ?, ?)",
                presentationId, productId, "SKU-" + slug, ("SKU-" + slug).toUpperCase(), price, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.product_images (id, product_id, storage_key, content_type, byte_size, width, height, alt_text, position, is_primary, created_at, updated_at) values (?, ?, ?, 'image/png', 10, 1, 1, 'Imagem Teste', 0, true, ?, ?)",
                UUID.randomUUID(), productId, "cart-test/" + slug + ".png", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.inventory_balances (presentation_id, physical_quantity, reserved_quantity, version, created_at, updated_at) values (?, ?, 0, 0, ?, ?)",
                presentationId, physical, Timestamp.from(now), Timestamp.from(now));
        return new Fixture(presentationId, productId);
    }

    private AppUserPrincipal customer(String suffix, String password) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String email = suffix + "@test.local";
        String hash = passwordEncoder.encode(password);
        jdbc.update("""
                insert into perfumestore.app_users
                    (id, display_name, email, email_normalized, password_hash, role, status,
                     created_at, updated_at, password_changed_at)
                values (?, ?, ?, ?, ?, 'CUSTOMER', 'ACTIVE', ?, ?, ?)
                """, id, "Cliente " + suffix, email, email, hash, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        AppUser user = new AppUser(id, "Cliente " + suffix, email, email, hash, UserRole.CUSTOMER,
                UserStatus.ACTIVE, now, now, now);
        return AppUserPrincipal.from(user);
    }

    private record Fixture(UUID presentationId, UUID productId) { }
}
