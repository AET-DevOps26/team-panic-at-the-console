package com.panicattheconsole.userservice.auth;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercises the full auth flow over real HTTP against a real Postgres:
 * register, duplicate register, login, session-guarded profile/directory,
 * and logout.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("users")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    private static final String EMAIL = "alex@example.com";
    private static final String PASSWORD = "super-secret-pw";

    private static String sessionCookie;

    @Test
    @Order(1)
    void register_createsAccountWithMemberRole() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/auth/register",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"Alex\"}",
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("email", EMAIL)
                .containsEntry("displayName", "Alex")
                .containsEntry("role", "MEMBER")
                .doesNotContainKeys("password", "passwordHash");
    }

    @Test
    @Order(2)
    void register_duplicateEmailReturns409() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/auth/register",
                "{\"email\":\"" + EMAIL.toUpperCase() + "\",\"password\":\"other-secret-pw\",\"displayName\":\"Imposter\"}",
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    void register_invalidBodyReturns400() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/auth/register",
                "{\"email\":\"not-an-email\",\"password\":\"short\",\"displayName\":\"\"}",
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void login_wrongPasswordReturns401() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"wrong-password\"}",
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @Order(5)
    void login_setsHttpOnlySessionCookie() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}",
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", EMAIL);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("session=").contains("HttpOnly").contains("SameSite=Strict");
        sessionCookie = setCookie.split(";", 2)[0];
    }

    @Test
    @Order(6)
    void usersMe_withSessionReturnsCurrentUser() {
        ResponseEntity<Map<String, Object>> response = getJson("/users/me", sessionCookie);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", EMAIL);
    }

    @Test
    @Order(7)
    void usersMe_withoutSessionReturns401() {
        assertThat(getJson("/users/me", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getJson("/users/me", "session=tampered.jwt.token").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(8)
    void users_listsRegisteredAccounts() {
        ResponseEntity<Map<String, Object>> response = getJson("/users?limit=10&offset=0", sessionCookie);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("total", 1);
        assertThat(response.getBody().get("items")).asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.LIST).hasSize(1);
    }

    @Test
    @Order(9)
    void logout_clearsSessionCookie() {
        ResponseEntity<Map<String, Object>> response = postJson("/auth/logout", null, sessionCookie);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    private ResponseEntity<Map<String, Object>> postJson(String path, String body, String cookie) {
        return exchange(HttpMethod.POST, path, body, cookie);
    }

    private ResponseEntity<Map<String, Object>> getJson(String path, String cookie) {
        return exchange(HttpMethod.GET, path, null, cookie);
    }

    private ResponseEntity<Map<String, Object>> exchange(
            HttpMethod method, String path, String body, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
        return rest.exchange(
                path, method, new HttpEntity<>(body, headers),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
