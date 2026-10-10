package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.perfumestore.backend.auth.PasswordRecoveryService;
import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.notification.CapturedEmailSender;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
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
@Import(TestApiConfiguration.class)
class AuthenticationIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";
    private static final String NEW_PASSWORD = "NewStrongPassword456!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CapturedEmailSender emails;

    @Autowired
    private PasswordRecoveryService recoveryService;

    @Autowired
    private UserRepository users;

    @AfterEach
    void clearCapturedMessages() {
        emails.clear();
        jdbc.update("delete from perfumestore.spring_session_attributes");
        jdbc.update("delete from perfumestore.spring_session");
        jdbc.update("delete from perfumestore.password_reset_tokens");
        jdbc.update("delete from perfumestore.app_users");
    }

    @Test
    void registersCustomerWithHashedPasswordAndCannotAssignAdmin() throws Exception {
        String email = uniqueEmail("register");
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Cliente\",\"email\":\"" + email
                        + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String storedHash = jdbc.queryForObject(
                "select password_hash from perfumestore.app_users where email_normalized = ?",
                String.class, email);
        assertThat(storedHash).startsWith("$2a$").doesNotContain(PASSWORD);
        assertThat(jdbc.queryForObject(
                "select role from perfumestore.app_users where email_normalized = ?", String.class, email))
                .isEqualTo("CUSTOMER");
    }

    @Test
    void normalizesEmailAndRejectsConcurrentStyleDuplicateByDatabaseConstraint() throws Exception {
        String email = uniqueEmail("duplicate");
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Outro\",\"email\":\"" + email.toUpperCase()
                        + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void loginPersistsSessionRotatesIdentifierAndSupportsAuthenticatedProfile() throws Exception {
        String email = uniqueEmail("login");
        register(email);
        MockHttpSession initial = new MockHttpSession();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .session(initial)
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();

        assertThat(jdbc.queryForObject("select count(*) from perfumestore.spring_session", Integer.class))
                .isEqualTo(1);
        AppUser registered = users.findByEmailNormalized(email).orElseThrow();
        AppUserPrincipal principal = AppUserPrincipal.from(registered);
        mockMvc.perform(get("/api/v1/auth/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(patch("/api/v1/auth/me")
                .with(csrf())
                .with(user(principal))
                .contentType("application/json")
                .content("{\"name\":\"Nome Atualizado\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Atualizado"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(get("/test-api/admin").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void invalidLoginIsGenericAndExcessiveAttemptsReturn429() throws Exception {
        String email = uniqueEmail("invalid-login");
        String firstBody = null;
        for (int i = 1; i <= 6; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andReturn();
            if (i == 1) {
                assertThat(result.getResponse().getStatus()).isEqualTo(401);
                firstBody = result.getResponse().getContentAsString();
                assertThat(firstBody).contains("INVALID_CREDENTIALS");
            }
            if (i == 6) {
                assertThat(result.getResponse().getStatus()).isEqualTo(429);
                assertThat(result.getResponse().getHeader("Retry-After")).isNotBlank();
                assertThat(result.getResponse().getContentAsString()).contains("TOO_MANY_ATTEMPTS");
            }
        }
        assertThat(firstBody).doesNotContain(email).doesNotContain("não cadastrado");
    }

    @Test
    void logoutRevokesPersistedSessionAndCanBeRepeated() throws Exception {
        String email = uniqueEmail("logout");
        register(email);
        MockHttpSession session = login(email, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")));

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void recoveryResponseIsGenericAndResetUsesOneTimeToken() throws Exception {
        String email = uniqueEmail("recovery");
        register(email);
        requestRecovery(email).andExpect(status().isAccepted());
        String link = emails.messages().get(0).resetLink();
        String token = URI.create(link).getQuery().substring("token=".length());

        assertThat(jdbc.queryForObject(
                "select count(*) from perfumestore.password_reset_tokens where token_hash = ?", Integer.class,
                token)).isZero();

        mockMvc.perform(post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_INVALID"));

        loginExpect(email, PASSWORD, 401);
        loginExpect(email, NEW_PASSWORD, 200);
    }

    @Test
    void recoveryDoesNotRevealUnknownEmailAndExpiredTokenIsRejected() throws Exception {
        String unknown = uniqueEmail("unknown");
        String known = uniqueEmail("expired");
        requestRecovery(unknown).andExpect(status().isAccepted());
        assertThat(emails.messages()).isEmpty();

        register(known);
        requestRecovery(known).andExpect(status().isAccepted());
        String token = URI.create(emails.messages().get(0).resetLink()).getQuery().substring("token=".length());
        jdbc.update("update perfumestore.password_reset_tokens set expires_at = ? where token_hash = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                com.perfumestore.backend.auth.TokenSecurity.sha256(token));

        mockMvc.perform(post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void mailFailureKeepsRecoveryResponseGeneric() throws Exception {
        String email = uniqueEmail("mail-failure");
        register(email);
        emails.failDelivery();

        requestRecovery(email)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(
                        "Se o e-mail estiver cadastrado, enviaremos instruções para recuperar o acesso."));
        assertThat(emails.messages()).isEmpty();
        assertThat(jdbc.queryForObject(
                "select count(*) from perfumestore.password_reset_tokens where user_id = "
                        + "(select id from perfumestore.app_users where email_normalized = ?)",
                Integer.class, email)).isZero();
    }

    @Test
    void passwordResetInvalidatesExistingSessions() throws Exception {
        String email = uniqueEmail("invalidate");
        register(email);
        MockHttpSession oldSession = login(email, PASSWORD);
        requestRecovery(email).andExpect(status().isAccepted());
        String token = URI.create(emails.messages().get(0).resetLink()).getQuery().substring("token=".length());

        mockMvc.perform(post("/api/v1/auth/password-resets")
                .with(csrf())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from perfumestore.spring_session", Integer.class))
                .isZero();
    }

    @Test
    void sameResetTokenCanOnlyBeConsumedOnceUnderConcurrency() throws Exception {
        String email = uniqueEmail("concurrent-reset");
        register(email);
        recoveryService.request(email, "test-a");
        String token = URI.create(emails.messages().get(0).resetLink()).getQuery().substring("token=".length());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> resetDirectly(token, "test-a"));
            Future<Boolean> second = executor.submit(() -> resetDirectly(token, "test-b"));
            assertThat(first.get()).isNotEqualTo(second.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean resetDirectly(String token, String remote) {
        try {
            recoveryService.reset(token, NEW_PASSWORD, remote);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Cliente\",\"email\":\"" + email
                        + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MockHttpSession initial = new MockHttpSession();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .session(initial)
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void loginExpect(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private org.springframework.test.web.servlet.ResultActions requestRecovery(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-recovery-requests")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + java.util.UUID.randomUUID() + "@example.test";
    }
}
