package com.example.dynamicform.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("it")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_EMAIL = "admin@integration.test";
    protected static final String ADMIN_PASSWORD = "IntegrationAdmin123!";

    @Autowired
    protected TestRestTemplate http;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String adminToken;

    @BeforeEach
    void authenticateAdmin() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String email, String password) {
        ResponseEntity<JsonNode> response = http.postForEntity(
                "/api/auth/login",
                jsonEntity(Map.of(
                        "email", email,
                        "password", password
                )),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().path("accessToken").asText();
    }

    protected RegisteredUser registerUser(String email, String displayName) {
        ResponseEntity<JsonNode> response = http.postForEntity(
                "/api/auth/register",
                jsonEntity(Map.of(
                        "email", email,
                        "displayName", displayName,
                        "password", "StrongPass123!"
                )),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        JsonNode body = response.getBody();
        return new RegisteredUser(
                body.path("user").path("id").asLong(),
                body.path("accessToken").asText(),
                email
        );
    }

    protected long createForm(String name) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/forms",
                HttpMethod.POST,
                adminToken,
                defaultFormBody(name)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("DRAFT");
        return response.getBody().path("id").asLong();
    }

    protected void publishForm(long formId) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId + "/publish",
                HttpMethod.POST,
                adminToken,
                null
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("PUBLISHED");
    }

    protected long submit(long formId, String token, String fullName, int age, String city) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                token,
                Map.of("answers", Map.of(
                        "fullName", fullName,
                        "age", age,
                        "city", city
                ))
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().path("id").asLong();
    }

    protected void closeForm(long formId) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId + "/close",
                HttpMethod.POST,
                adminToken,
                null
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("CLOSED");
    }

    protected long createLottery(long formId, int winnerCount) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId + "/lotteries",
                HttpMethod.POST,
                adminToken,
                Map.of("winnerCount", winnerCount)
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("READY");
        return response.getBody().path("id").asLong();
    }

    protected ResponseEntity<JsonNode> exchange(String path,
                                                HttpMethod method,
                                                String bearerToken,
                                                Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        return http.exchange(
                path,
                method,
                new HttpEntity<>(body, headers),
                JsonNode.class
        );
    }

    protected HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(body, headers);
    }

    protected Map<String, Object> defaultFormBody(String name) {
        return Map.of(
                "name", name,
                "description", "Integration test form",
                "schema", Map.of(
                        "fields", List.of(
                                Map.of(
                                        "key", "fullName",
                                        "type", "TEXT",
                                        "label", "Full Name",
                                        "required", true,
                                        "order", 1,
                                        "validation", Map.of(
                                                "minLength", 3,
                                                "maxLength", 100
                                        ),
                                        "options", List.of()
                                ),
                                Map.of(
                                        "key", "age",
                                        "type", "NUMBER",
                                        "label", "Age",
                                        "required", true,
                                        "order", 2,
                                        "validation", Map.of(
                                                "min", 18,
                                                "max", 80
                                        ),
                                        "options", List.of()
                                ),
                                Map.of(
                                        "key", "city",
                                        "type", "SELECT",
                                        "label", "City",
                                        "required", true,
                                        "order", 3,
                                        "options", List.of(
                                                Map.of("value", "tehran", "label", "Tehran"),
                                                Map.of("value", "tabriz", "label", "Tabriz")
                                        )
                                )
                        )
                )
        );
    }

    protected record RegisteredUser(long id, String token, String email) {
    }
}
