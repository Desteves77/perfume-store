package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.address.AddressDtos;
import com.perfumestore.backend.address.AddressService;
import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.config.ShippingProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shipping.ShippingService;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRole;
import com.perfumestore.backend.user.UserStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class AddressShippingIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddressService addresses;

    @Autowired
    private ShippingService shipping;

    @Test
    void createsListsAndUpdatesOwnAddressWithNormalizedPostalCode() throws Exception {
        AppUserPrincipal principal = createUser("address-owner", UserRole.CUSTOMER);
        String body = """
                {"recipientName":"Cliente A","label":"Casa","postalCode":"01311-000",
                 "street":"Avenida Paulista","number":"1000","complement":"Apto 10",
                 "neighborhood":"Bela Vista","city":"São Paulo","state":"sp","reference":"Portaria"}
                """;

        String response = mockMvc.perform(post("/api/v1/me/addresses").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.postalCode").value("01311000"))
                .andExpect(jsonPath("$.state").value("SP")).andReturn().getResponse().getContentAsString();
        String addressId = new tools.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/me/addresses").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(addressId));
        mockMvc.perform(patch("/api/v1/me/addresses/" + addressId).with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("Avenida Paulista", "Rua Augusta")
                        .replace("\"state\":\"sp\"", "\"state\":\"SP\"")));
        long version = jdbc.queryForObject("select version from perfumestore.addresses where id = ?", Long.class,
                UUID.fromString(addressId));
        String update = body.replace("Avenida Paulista", "Rua Augusta").replace("\"state\":\"sp\"", "\"state\":\"SP\"")
                .replace("\"reference\":\"Portaria\"", "\"reference\":\"Portaria\",\"version\":" + version);
        mockMvc.perform(patch("/api/v1/me/addresses/" + addressId).with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua Augusta"));
    }

    @Test
    void rejectsInvalidAddressFields() throws Exception {
        AppUserPrincipal principal = createUser("address-invalid", UserRole.CUSTOMER);
        String body = "{\"recipientName\":\"A\",\"postalCode\":\"123\",\"street\":\"\",\"number\":\"1\","
                + "\"neighborhood\":\"Centro\",\"city\":\"Cidade\",\"state\":\"XX\"}";
        mockMvc.perform(post("/api/v1/me/addresses").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    @Test
    void isolatesAddressOwnershipAndUsesLogicalDeletion() throws Exception {
        AppUserPrincipal owner = createUser("address-owner-idor", UserRole.CUSTOMER);
        AppUserPrincipal other = createUser("address-other-idor", UserRole.CUSTOMER);
        AddressDtos.AddressResponse address = addresses.create(owner.id(), request("20000000"));

        mockMvc.perform(get("/api/v1/me/addresses/" + address.id()).with(user(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/me/addresses/" + address.id()).with(user(other)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(updateRequest(address.version())))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/me/addresses/" + address.id()).with(user(other)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/me/addresses/" + address.id()).with(user(owner)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me/addresses/" + address.id()).with(user(owner)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_at is not null from perfumestore.addresses where id = ?",
                Boolean.class, address.id())).isTrue();
    }

    @Test
    void protectsAddressEndpointsForAnonymousAndWithoutCsrf() throws Exception {
        String body = "{\"recipientName\":\"Cliente\",\"postalCode\":\"01311000\",\"street\":\"Rua A\","
                + "\"number\":\"1\",\"neighborhood\":\"Centro\",\"city\":\"Sao Paulo\",\"state\":\"SP\"}";
        mockMvc.perform(get("/api/v1/me/addresses")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/me/addresses").with(user(createUser("address-csrf", UserRole.CUSTOMER)))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }
    @Test
    void quotesServedDestinationWithBackendMoneyAndPersistsQuote() throws Exception {
        AppUserPrincipal principal = createUser("shipping-owner", UserRole.CUSTOMER);
        AddressDtos.AddressResponse address = addresses.create(principal.id(), request("01311000"));
        String body = "{\"addressId\":\"" + address.id() + "\"}";

        mockMvc.perform(post("/api/v1/shipping/quotes").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceable").value(true)).andExpect(jsonPath("$.amount").value("15.90"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.estimatedDelivery.minDays").value(3))
                .andExpect(jsonPath("$.estimatedDelivery.maxDays").value(6));
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.shipping_quotes where user_id = ?",
                Integer.class, principal.id())).isEqualTo(1);
    }

    @Test
    void rejectsUnservedDestinationAndInvalidPostalCode() throws Exception {
        AppUserPrincipal principal = createUser("shipping-invalid", UserRole.CUSTOMER);
        AddressDtos.AddressResponse outside = addresses.create(principal.id(), request("20000000"));
        mockMvc.perform(post("/api/v1/shipping/quotes").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":\"" + outside.id() + "\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code")
                        .value("SHIPPING_DESTINATION_UNSERVED"));
        mockMvc.perform(post("/api/v1/me/addresses").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"recipientName\":\"Cliente\",\"postalCode\":\"0000000\",\"street\":\"Rua A\","
                                + "\"number\":\"1\",\"neighborhood\":\"Centro\",\"city\":\"Cidade\",\"state\":\"SP\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test
    void shippingRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/shipping/quotes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
        AppUserPrincipal principal = createUser("shipping-csrf", UserRole.CUSTOMER);
        AddressDtos.AddressResponse address = addresses.create(principal.id(), request("01311000"));
        mockMvc.perform(post("/api/v1/shipping/quotes").with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":\"" + address.id() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidShippingConfigurationAndPreservesOwnerOnUpdate() {
        ShippingProperties invalid = new ShippingProperties();
        invalid.setEnabled(true);
        assertThatThrownBy(invalid::validateForQuote).isInstanceOf(IllegalStateException.class);

        AppUserPrincipal owner = createUser("shipping-owner-update", UserRole.CUSTOMER);
        AddressDtos.AddressResponse address = addresses.create(owner.id(), request("01311000"));
        assertThatThrownBy(() -> addresses.update(owner.id(), address.id(), new AddressDtos.AddressUpdateRequest(
                "Cliente", null, "01311000", "Rua Nova", "2", null, "Centro", "São Paulo", "SP", null,
                address.version() - 1))).isInstanceOf(ApiRequestException.class)
                .extracting("code").isEqualTo("CONCURRENT_UPDATE");
        assertThat(jdbc.queryForObject("select user_id from perfumestore.addresses where id = ?", UUID.class,
                address.id())).isEqualTo(owner.id());
    }

    private AddressDtos.AddressRequest request(String postalCode) {
        return new AddressDtos.AddressRequest("Cliente Teste", "Casa", postalCode, "Rua Teste", "10", null,
                "Centro", "Cidade Teste", "SP", null);
    }

    private String updateRequest(long version) {
        return ("{\"recipientName\":\"Cliente Atualizado\",\"postalCode\":\"01311000\","
                + "\"street\":\"Rua Nova\",\"number\":\"20\",\"neighborhood\":\"Centro\","
                + "\"city\":\"Sao Paulo\",\"state\":\"SP\",\"version\":%d}").formatted(version);
    }
    private AppUserPrincipal createUser(String suffix, UserRole role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                insert into perfumestore.app_users
                    (id, display_name, email, email_normalized, password_hash, role, status,
                     created_at, updated_at, password_changed_at)
                values (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)
                """, id, "Usuário " + suffix, suffix + "@test.local", suffix + "@test.local", "test-hash",
                role.name(), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return AppUserPrincipal.from(new AppUser(id, "Usuário " + suffix, suffix + "@test.local",
                suffix + "@test.local", "test-hash", role, UserStatus.ACTIVE, now, now, now));
    }
}
