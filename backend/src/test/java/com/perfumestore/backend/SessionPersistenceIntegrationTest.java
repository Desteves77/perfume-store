package com.perfumestore.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perfumestore.backend.notification.CapturedEmailSender;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionPersistenceIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";
    private static final String NEW_PASSWORD = "NewStrongPassword456!";
    private static final Pattern TOKEN_PARAMETER = Pattern.compile("[?&]token=([^&]+)");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CapturedEmailSender emails;

    @Test
    void realHttpSessionIsPersistedRotatedAndRevoked() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String email = "real-session-" + java.util.UUID.randomUUID() + "@example.test";

        String csrf = getCsrf(client);
        String sessionBeforeLogin = cookie(cookies, "SESSION");
        HttpResponse<String> register = send(client, "POST", "/api/v1/auth/register", csrf,
                "{\"name\":\"Cliente\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(register.statusCode()).isEqualTo(201);

        csrf = cookie(cookies, "XSRF-TOKEN");
        HttpResponse<String> login = send(client, "POST", "/api/v1/auth/login", csrf,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(cookie(cookies, "SESSION")).isNotBlank();
        assertThat(cookie(cookies, "SESSION")).isNotEqualTo(sessionBeforeLogin);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.spring_session", Integer.class))
                .isEqualTo(1);

        HttpResponse<String> me = send(client, "GET", "/api/v1/auth/me", null, null);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(email);

        csrf = cookie(cookies, "XSRF-TOKEN");
        HttpResponse<String> recovery = send(client, "POST", "/api/v1/auth/password-recovery-requests", csrf,
                "{\"email\":\"" + email + "\"}");
        assertThat(recovery.statusCode()).isEqualTo(202);
        String link = emails.messages().get(0).resetLink();
        Matcher matcher = TOKEN_PARAMETER.matcher(link);
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        csrf = cookie(cookies, "XSRF-TOKEN");
        HttpResponse<String> reset = send(client, "POST", "/api/v1/auth/password-resets", csrf,
                "{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}");
        assertThat(reset.statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("select count(*) from perfumestore.spring_session", Integer.class))
                .isZero();

        HttpResponse<String> afterReset = send(client, "GET", "/api/v1/auth/me", null, null);
        assertThat(afterReset.statusCode()).isEqualTo(401);

        csrf = cookie(cookies, "XSRF-TOKEN");
        HttpResponse<String> oldLogin = send(client, "POST", "/api/v1/auth/login", csrf,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(oldLogin.statusCode()).isEqualTo(401);
        csrf = cookie(cookies, "XSRF-TOKEN");
        HttpResponse<String> newLogin = send(client, "POST", "/api/v1/auth/login", csrf,
                "{\"email\":\"" + email + "\",\"password\":\"" + NEW_PASSWORD + "\"}");
        assertThat(newLogin.statusCode()).isEqualTo(200);
    }

    private String getCsrf(HttpClient client) throws Exception {
        HttpResponse<String> response = send(client, "GET", "/api/v1/security/csrf", null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return cookie(((CookieManager) client.cookieHandler().orElseThrow()), "XSRF-TOKEN");
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String csrf, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json");
        if (csrf != null) {
            builder.header("X-XSRF-TOKEN", csrf);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String cookie(CookieManager manager, String name) {
        return manager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(java.net.HttpCookie::getValue)
                .findFirst()
                .orElse("");
    }
}
