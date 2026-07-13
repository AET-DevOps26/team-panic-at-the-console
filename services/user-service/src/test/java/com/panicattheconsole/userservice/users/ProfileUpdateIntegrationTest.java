package com.panicattheconsole.userservice.users;

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
 * Exercises self-service account management over real HTTP against a real
 * Postgres: display name and email updates ({@code PATCH /users/me}) and
 * password changes ({@code POST /users/me/password}), including the
 * current-password checks that guard email and password changes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileUpdateIntegrationTest {

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

    private static final String EMAIL = "casey@example.com";
    private static final String NEW_EMAIL = "casey.new@example.com";
    private static final String TAKEN_EMAIL = "taken@example.com";
    private static final String PASSWORD = "initial-secret-pw";
    private static final String NEW_PASSWORD = "rotated-secret-pw";

    private static String sessionCookie;

    @Test
    @Order(1)
    void setup_registerTwoAccountsAndLogin() {
        assertThat(postJson("/auth/register",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"Casey\"}",
                null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postJson("/auth/register",
                "{\"email\":\"" + TAKEN_EMAIL + "\",\"password\":\"someone-elses-pw\",\"displayName\":\"Other\"}",
                null).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map<String, Object>> login = postJson(
                "/auth/login", "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        sessionCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
    }

    @Test
    @Order(2)
    void patchMe_withoutSessionReturns401() {
        assertThat(patchJson("{\"displayName\":\"Nope\"}", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void patchMe_emptyBodyReturns400() {
        assertThat(patchJson("{}", sessionCookie).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void patchMe_displayNameOnlyNeedsNoPassword() {
        ResponseEntity<Map<String, Object>> response = patchJson("{\"displayName\":\"Casey R.\"}", sessionCookie);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("displayName", "Casey R.")
                .containsEntry("email", EMAIL);
    }

    @Test
    @Order(5)
    void patchMe_emailWithoutCurrentPasswordReturns400() {
        assertThat(patchJson("{\"email\":\"" + NEW_EMAIL + "\"}", sessionCookie).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    void patchMe_emailWithWrongCurrentPasswordReturns401() {
        assertThat(patchJson(
                "{\"email\":\"" + NEW_EMAIL + "\",\"currentPassword\":\"wrong-password\"}",
                sessionCookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(7)
    void patchMe_emailTakenByOtherAccountReturns409() {
        assertThat(patchJson(
                "{\"email\":\"" + TAKEN_EMAIL + "\",\"currentPassword\":\"" + PASSWORD + "\"}",
                sessionCookie).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(8)
    void patchMe_emailChangeSucceedsAndNewEmailLogsIn() {
        ResponseEntity<Map<String, Object>> response = patchJson(
                "{\"email\":\"" + NEW_EMAIL.toUpperCase() + "\",\"currentPassword\":\"" + PASSWORD + "\"}",
                sessionCookie);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", NEW_EMAIL);

        assertThat(postJson("/auth/login",
                "{\"email\":\"" + NEW_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}",
                null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(9)
    void changePassword_wrongCurrentPasswordReturns401() {
        assertThat(postJson("/users/me/password",
                "{\"currentPassword\":\"wrong-password\",\"newPassword\":\"" + NEW_PASSWORD + "\"}",
                sessionCookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(10)
    void changePassword_tooShortNewPasswordReturns400() {
        assertThat(postJson("/users/me/password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short\"}",
                sessionCookie).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(11)
    void changePassword_rotatesTheCredential() {
        assertThat(postJson("/users/me/password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}",
                sessionCookie).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(postJson("/auth/login",
                "{\"email\":\"" + NEW_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}",
                null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postJson("/auth/login",
                "{\"email\":\"" + NEW_EMAIL + "\",\"password\":\"" + NEW_PASSWORD + "\"}",
                null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> patchJson(String body, String cookie) {
        return exchange(HttpMethod.PATCH, "/users/me", body, cookie);
    }

    private ResponseEntity<Map<String, Object>> postJson(String path, String body, String cookie) {
        return exchange(HttpMethod.POST, path, body, cookie);
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
