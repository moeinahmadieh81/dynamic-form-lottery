package com.example.dynamicform.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionRulesIT extends IntegrationTestSupport {

    @Test
    void dynamicValidationAndDuplicateProtectionShouldBeEnforcedThroughHttpAndDatabase() {
        long formId = createForm("Submission Rules Form");
        publishForm(formId);
        RegisteredUser user = registerUser("rules.user@example.com", "Rules User");

        ResponseEntity<JsonNode> belowMinimum = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                user.token(),
                Map.of("answers", Map.of(
                        "fullName", "Rules User",
                        "age", 12,
                        "city", "tehran"
                ))
        );
        assertThat(belowMinimum.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(belowMinimum.getBody()).isNotNull();
        assertThat(belowMinimum.getBody().path("errors").get(0).path("code").asText())
                .isEqualTo("MIN_VALUE");

        ResponseEntity<JsonNode> unknownField = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                user.token(),
                Map.of("answers", Map.of(
                        "fullName", "Rules User",
                        "age", 28,
                        "city", "tehran",
                        "isWinner", true
                ))
        );
        assertThat(unknownField.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknownField.getBody()).isNotNull();
        assertThat(unknownField.getBody().path("errors").get(0).path("code").asText())
                .isEqualTo("UNKNOWN_FIELD");

        ResponseEntity<JsonNode> missingRequiredField = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                user.token(),
                Map.of("answers", Map.of(
                        "age", 28,
                        "city", "tehran"
                ))
        );
        assertThat(missingRequiredField.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingRequiredField.getBody()).isNotNull();
        assertThat(missingRequiredField.getBody().path("errors").get(0).path("code").asText())
                .isEqualTo("REQUIRED");

        submit(formId, user.token(), "Rules User", 28, "tehran");

        ResponseEntity<JsonNode> duplicate = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                user.token(),
                Map.of("answers", Map.of(
                        "fullName", "Rules User",
                        "age", 29,
                        "city", "tabriz"
                ))
        );
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody()).isNotNull();
        assertThat(duplicate.getBody().path("title").asText()).isEqualTo("Duplicate submission");

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from submissions where form_id = ? and user_id = ?",
                Integer.class,
                formId,
                user.id()
        );
        assertThat(count).isEqualTo(1);
    }
}
