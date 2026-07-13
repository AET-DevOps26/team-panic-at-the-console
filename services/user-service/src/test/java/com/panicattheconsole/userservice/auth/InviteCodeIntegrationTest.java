package com.panicattheconsole.userservice.auth;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
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

/** Registration on an instance configured with an invitation code. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "auth.invite-code=sesame-open-up")
@AutoConfigureTestRestTemplate
@Testcontainers
class InviteCodeIntegrationTest {

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

    @Test
    void register_withoutInviteCodeReturns403() {
        ResponseEntity<Map<String, Object>> response = register(
                "{\"email\":\"a@example.com\",\"password\":\"secret-password\",\"displayName\":\"A\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void register_withWrongInviteCodeReturns403() {
        ResponseEntity<Map<String, Object>> response = register(
                "{\"email\":\"b@example.com\",\"password\":\"secret-password\",\"displayName\":\"B\","
                        + "\"inviteCode\":\"wrong-code\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void register_withCorrectInviteCodeCreatesAccount() {
        ResponseEntity<Map<String, Object>> response = register(
                "{\"email\":\"c@example.com\",\"password\":\"secret-password\",\"displayName\":\"C\","
                        + "\"inviteCode\":\"sesame-open-up\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("email", "c@example.com");
    }

    private ResponseEntity<Map<String, Object>> register(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(
                "/auth/register", HttpMethod.POST, new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
