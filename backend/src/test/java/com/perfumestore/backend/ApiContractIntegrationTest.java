package com.perfumestore.backend;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiConfiguration.class)
class ApiContractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidJsonReturnsStableProblemWithCorrelation() throws Exception {
        mockMvc.perform(post("/test-api/validation")
                .header("X-Correlation-Id", "trace-123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", "trace-123"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.correlationId").value("trace-123"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void incompatibleJsonTypeGetsDifferentStableCode() throws Exception {
        mockMvc.perform(post("/test-api/validation")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Perfume\",\"nested\":{\"label\":\"50 ml\"},\"amount\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON_TYPE"));
    }

    @Test
    void nestedValidationReturnsFieldPathsInPortuguese() throws Exception {
        mockMvc.perform(post("/test-api/validation")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"nested\":{\"label\":\"\"},\"amount\":\"0.00\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("name", "nested.label", "amount")))
                .andExpect(jsonPath("$.message").value("Corrija os campos informados."));
    }

    @Test
    void validDtoSerializesMoneyAsDecimalStringAndDatesAsIso() throws Exception {
        mockMvc.perform(post("/test-api/validation")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Perfume\",\"nested\":{\"label\":\"50 ml\"},\"amount\":\"199.90\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money.amount").value("199.90"))
                .andExpect(jsonPath("$.money.currency").value("BRL"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-10T12:00:00Z"))
                .andExpect(jsonPath("$.civilDate").value("2026-10-10"));
    }

    @Test
    void paginationRejectsLimitsAndUnknownSortAndUsesStableTieBreaker() throws Exception {
        mockMvc.perform(get("/test-api/page").param("size", "101"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));

        mockMvc.perform(get("/test-api/page").param("sort", "secret,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));

        mockMvc.perform(get("/test-api/page").param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].id").value("item-3"))
                .andExpect(jsonPath("$.sort[0].property").value("price"))
                .andExpect(jsonPath("$.sort[1].property").value("id"));
    }

    @Test
    void invalidPathParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test-api/path/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void unexpectedErrorIsGenericAndCorrelated() throws Exception {
        mockMvc.perform(get("/test-api/internal-error").header("X-Correlation-Id", "server-error-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Correlation-Id", "server-error-1"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro interno. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal-marker"))));
    }

    @Test
    void protectedRouteReturnsJson401WithoutLoginRedirect() throws Exception {
        mockMvc.perform(get("/test-api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void authenticatedCustomerCannotAccessAdminFixture() throws Exception {
        mockMvc.perform(get("/test-api/admin").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void csrfEndpointReturnsCookieAndUnsafeRequestsNeedValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/security/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));

        mockMvc.perform(post("/test-api/protected/write").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(post("/test-api/protected/write")
                .with(user("customer").roles("CUSTOMER"))
                .with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(post("/test-api/protected/write")
                .with(user("customer").roles("CUSTOMER"))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void corsAllowsConfiguredOriginAndPreflightButNotUnknownOrigin() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", "http://allowed.test"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://allowed.test"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/actuator/health")
                .header("Origin", "http://allowed.test")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://allowed.test"));

        mockMvc.perform(get("/actuator/health").header("Origin", "http://denied.test"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void openApiAndHealthAreAvailableInTestProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"openapi\"")));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
