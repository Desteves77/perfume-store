package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.perfumestore.backend.inventory.InventoryDtos;
import com.perfumestore.backend.inventory.InventoryMovementType;
import com.perfumestore.backend.inventory.InventoryService;
import com.perfumestore.backend.shared.api.ApiRequestException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryService inventory;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsZeroBalanceLazilyAndKeepsPresentationsIndependent() {
        Fixture first = fixture(true);
        Fixture second = fixture(true);

        assertThat(jdbc.queryForObject(
                "select count(*) from perfumestore.flyway_schema_history where version = '4' and success = true",
                Integer.class)).isEqualTo(1);

        InventoryDtos.BalanceResponse before = inventory.balance(first.presentationId());
        assertThat(before.physicalQuantity()).isZero();
        assertThat(before.reservedQuantity()).isZero();
        assertThat(before.availableQuantity()).isZero();

        inventory.adjust(first.presentationId(), 4, InventoryMovementType.ENTRY, "Entrada inicial",
                "entry-" + first.suffix(), first.actorId(), "test-balance");
        assertThat(inventory.balance(first.presentationId()).availableQuantity()).isEqualTo(4);
        assertThat(inventory.balance(second.presentationId()).availableQuantity()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_balances where presentation_id = ?",
                Integer.class, first.presentationId())).isEqualTo(1);
    }

    @Test
    void recordsAdministrativeEntryWithIdempotencyAndProtectsHttpEndpoint() throws Exception {
        Fixture fixture = fixture(true);
        String key = "http-entry-" + fixture.suffix();
        String body = "{\"presentationId\":\"%s\",\"quantity\":3,\"type\":\"ENTRY\",\"reason\":\"Recebimento\"}"
                .formatted(fixture.presentationId());

        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                .with(user(fixture.actorId().toString()).roles("ADMIN")).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.physicalQuantity").value(3))
                .andExpect(jsonPath("$.availableQuantity").value(3));
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                .with(user(fixture.actorId().toString()).roles("ADMIN")).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.physicalQuantity").value(3));

        mockMvc.perform(get("/api/v1/admin/inventory/movements")
                .with(user(fixture.actorId().toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].actorUserId").value(fixture.actorId().toString()))
                .andExpect(jsonPath("$.items[0].reason").value("Recebimento"));
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                .with(user(fixture.actorId().toString()).roles("CUSTOMER")).with(csrf())
                .header("Idempotency-Key", "customer-" + fixture.suffix()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                .with(user(fixture.actorId().toString()).roles("ADMIN"))
                .header("Idempotency-Key", "csrf-" + fixture.suffix()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidQuantityAndIdempotencyReuse() {
        Fixture fixture = fixture(true);
        assertThatThrownBy(() -> inventory.adjust(fixture.presentationId(), 0, InventoryMovementType.ENTRY,
                "Entrada", "invalid-" + fixture.suffix(), fixture.actorId(), null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("INVENTORY_QUANTITY_INVALID");
        assertThatThrownBy(() -> inventory.adjust(fixture.presentationId(), -1, InventoryMovementType.ENTRY,
                "Correção", "negative-" + fixture.suffix(), fixture.actorId(), null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("INVENTORY_QUANTITY_INVALID");
        Fixture reserved = fixture(true);
        inventory.adjust(reserved.presentationId(), 5, InventoryMovementType.ENTRY,
                "Entrada reservada", "reserved-entry-" + reserved.suffix(), reserved.actorId(), null);
        inventory.reserve(reserved.presentationId(), 4, "reserved-" + reserved.suffix(), null, null, null, null);
        assertThatThrownBy(() -> inventory.adjust(reserved.presentationId(), -2, InventoryMovementType.ADJUSTMENT,
                "Baixa indevida", "below-reserved-" + reserved.suffix(), reserved.actorId(), null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("INVENTORY_BELOW_RESERVED");
        inventory.adjust(reserved.presentationId(), -1, InventoryMovementType.ADJUSTMENT,
                "Ajuste físico permitido", "negative-adjustment-" + reserved.suffix(), reserved.actorId(), null);
        assertThat(inventory.balance(reserved.presentationId()).physicalQuantity()).isEqualTo(4);
        assertThat(inventory.balance(reserved.presentationId()).reservedQuantity()).isEqualTo(4);
        Fixture overflow = fixture(true);
        inventory.adjust(overflow.presentationId(), Integer.MAX_VALUE, InventoryMovementType.ENTRY,
                "Limite", "overflow-entry-" + overflow.suffix(), overflow.actorId(), null);
        assertThatThrownBy(() -> inventory.adjust(overflow.presentationId(), 1, InventoryMovementType.ENTRY,
                "Overflow", "overflow-next-" + overflow.suffix(), overflow.actorId(), null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("INVENTORY_LIMIT_EXCEEDED");
        String key = "reuse-" + fixture.suffix();
        inventory.adjust(fixture.presentationId(), 1, InventoryMovementType.ENTRY, "Entrada", key,
                fixture.actorId(), null);
        assertThatThrownBy(() -> inventory.adjust(fixture.presentationId(), 2, InventoryMovementType.ENTRY,
                "Outra entrada", key, fixture.actorId(), null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void reservesConfirmsAndReleasesWithoutDuplicatingEffects() {
        Fixture confirmed = fixture(true);
        inventory.adjust(confirmed.presentationId(), 3, InventoryMovementType.ENTRY, "Entrada",
                "confirmed-entry-" + confirmed.suffix(), confirmed.actorId(), null);
        InventoryDtos.ReservationResponse reservation = inventory.reserve(confirmed.presentationId(), 2,
                "confirmed-reservation-" + confirmed.suffix(), null, null, null, null);
        assertThat(inventory.balance(confirmed.presentationId()).reservedQuantity()).isEqualTo(2);
        inventory.confirm(reservation.reservationKey(), null, null);
        inventory.confirm(reservation.reservationKey(), null, null);
        InventoryDtos.BalanceResponse confirmedBalance = inventory.balance(confirmed.presentationId());
        assertThat(confirmedBalance.physicalQuantity()).isEqualTo(1);
        assertThat(confirmedBalance.reservedQuantity()).isZero();

        Fixture released = fixture(true);
        inventory.adjust(released.presentationId(), 3, InventoryMovementType.ENTRY, "Entrada",
                "released-entry-" + released.suffix(), released.actorId(), null);
        InventoryDtos.ReservationResponse releasedReservation = inventory.reserve(released.presentationId(), 2,
                "released-reservation-" + released.suffix(), null, null, null, null);
        inventory.release(releasedReservation.reservationKey(), null, null);
        inventory.release(releasedReservation.reservationKey(), null, null);
        InventoryDtos.BalanceResponse releasedBalance = inventory.balance(released.presentationId());
        assertThat(releasedBalance.physicalQuantity()).isEqualTo(3);
        assertThat(releasedBalance.reservedQuantity()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where presentation_id = ?",
                Integer.class, released.presentationId())).isEqualTo(3);

        assertThatThrownBy(() -> inventory.confirm(releasedReservation.reservationKey(), null, null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("RESERVATION_ALREADY_RELEASED");
        assertThatThrownBy(() -> inventory.release(reservation.reservationKey(), null, null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("RESERVATION_ALREADY_CONFIRMED");
    }

    @Test
    void rejectsReservationAboveAvailabilityAndInactivePresentation() {
        Fixture fixture = fixture(true);
        inventory.adjust(fixture.presentationId(), 1, InventoryMovementType.ENTRY, "Entrada",
                "limited-entry-" + fixture.suffix(), fixture.actorId(), null);
        assertThatThrownBy(() -> inventory.reserve(fixture.presentationId(), 2,
                "insufficient-reservation-" + fixture.suffix(), null, null, null, null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("INSUFFICIENT_STOCK");

        jdbc.update("update perfumestore.product_presentations set active = false where id = ?", fixture.presentationId());
        assertThatThrownBy(() -> inventory.reserve(fixture.presentationId(), 1,
                "inactive-reservation-" + fixture.suffix(), null, null, null, null))
                .isInstanceOf(ApiRequestException.class).extracting("code").isEqualTo("PRESENTATION_INACTIVE");
    }

    @Test
    void allowsOnlyOneConcurrentReservationOfTheLastUnit() throws Exception {
        Fixture fixture = fixture(true);
        inventory.adjust(fixture.presentationId(), 1, InventoryMovementType.ENTRY,
                "Entrada concorrente", "concurrent-entry-" + fixture.suffix(), fixture.actorId(), null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            String key = "concurrent-reservation-" + fixture.suffix() + "-" + index;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                try {
                    return inventory.reserve(fixture.presentationId(), 1, key, null, null, null, null);
                } catch (RuntimeException exception) {
                    return exception;
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<Object> results = futures.stream().map(this::awaitResult).toList();
        executor.shutdownNow();
        assertThat(results.stream().filter(InventoryDtos.ReservationResponse.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ApiRequestException.class::isInstance)
                .map(ApiRequestException.class::cast).map(ApiRequestException::code))
                .containsExactly("INSUFFICIENT_STOCK");
        InventoryDtos.BalanceResponse balance = inventory.balance(fixture.presentationId());
        assertThat(balance.physicalQuantity()).isEqualTo(1);
        assertThat(balance.reservedQuantity()).isEqualTo(1);
        assertThat(balance.availableQuantity()).isZero();
    }

    @Test
    void serializesAvailabilityInPublicCatalogWithoutAdministrativeBalances() throws Exception {
        Fixture fixture = fixture(true);
        inventory.adjust(fixture.presentationId(), 4, InventoryMovementType.ENTRY,
                "Entrada pública", "public-entry-" + fixture.suffix(), fixture.actorId(), null);
        mockMvc.perform(get("/api/v1/catalog/products/" + fixture.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentations[0].availability.availableQuantity").value(4))
                .andExpect(jsonPath("$.presentations[0].availability.availableForPurchase").value(true))
                .andExpect(jsonPath("$.presentations[0].physicalQuantity").doesNotExist())
                .andExpect(jsonPath("$.presentations[0].reservedQuantity").doesNotExist());
    }

    @Test
    void confirmsOrReleasesOnlyOneTerminalTransitionWhenCalledConcurrently() throws Exception {
        Fixture fixture = fixture(true);
        inventory.adjust(fixture.presentationId(), 1, InventoryMovementType.ENTRY,
                "Entrada terminal", "terminal-entry-" + fixture.suffix(), fixture.actorId(), null);
        InventoryDtos.ReservationResponse reservation = inventory.reserve(fixture.presentationId(), 1,
                "terminal-reservation-" + fixture.suffix(), null, null, null, null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Object> confirm = executor.submit(() -> terminalCall(start, () -> inventory.confirm(
                reservation.reservationKey(), null, null)));
        Future<Object> release = executor.submit(() -> terminalCall(start, () -> inventory.release(
                reservation.reservationKey(), null, null)));
        start.countDown();
        Object first = awaitResult(confirm);
        Object second = awaitResult(release);
        executor.shutdownNow();
        assertThat(List.of(first, second).stream().filter(InventoryDtos.BalanceResponse.class::isInstance)).hasSize(1);
        assertThat(List.of(first, second).stream().filter(ApiRequestException.class::isInstance)).hasSize(1);
        assertThat(jdbc.queryForObject("select state from perfumestore.stock_reservations where id = ?",
                String.class, reservation.id())).isIn("CONFIRMED", "RELEASED");
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.inventory_movements where reservation_id = ?",
                Integer.class, reservation.id())).isEqualTo(2);
    }

    private Object terminalCall(CountDownLatch start, ThrowingSupplier supplier) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            return supplier.get();
        } catch (ApiRequestException exception) {
            return exception;
        }
    }

    private Object awaitResult(Future<Object> future) {
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            if (exception instanceof ExecutionException execution && execution.getCause() != null) {
                return execution.getCause();
            }
            throw new AssertionError(exception);
        }
    }

    private Fixture fixture(boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID actor = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        UUID presentation = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                insert into perfumestore.app_users
                    (id, display_name, email, email_normalized, password_hash, role, status,
                     created_at, updated_at, password_changed_at)
                values (?, ?, ?, ?, ?, 'ADMIN', 'ACTIVE', ?, ?, ?)
                """, actor, "Estoque Teste", "inventory-" + suffix + "@test.local", "inventory-" + suffix + "@test.local",
                "test-hash", Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into perfumestore.categories (id, name, slug, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)",
                category, "Categoria estoque " + suffix, "categoria-estoque-" + suffix, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into perfumestore.products
                    (id, slug, name, brand, description, category_id, active, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """, product, "produto-estoque-" + suffix, "Perfume Estoque " + suffix, "Marca Teste",
                "Descrição sintética de estoque.", category, active, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into perfumestore.product_presentations
                    (id, product_id, sku, sku_normalized, volume, unit, price, currency, active, version, created_at, updated_at)
                values (?, ?, ?, ?, 50, 'ML', 199.90, 'BRL', ?, 0, ?, ?)
                """, presentation, product, "SKU-ESTOQUE-" + suffix, "SKU-ESTOQUE-" + suffix,
                active, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into perfumestore.product_images
                    (id, product_id, storage_key, content_type, byte_size, width, height, alt_text, position, is_primary, created_at, updated_at)
                values (?, ?, ?, 'image/png', 10, 1, 1, 'Imagem sintética', 0, true, ?, ?)
                """, UUID.randomUUID(), product, "test/" + suffix + ".png", Timestamp.from(now), Timestamp.from(now));
        return new Fixture(suffix, actor, product, presentation, "produto-estoque-" + suffix);
    }

    private record Fixture(String suffix, UUID actorId, UUID productId, UUID presentationId, String slug) { }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Exception;
    }
}
