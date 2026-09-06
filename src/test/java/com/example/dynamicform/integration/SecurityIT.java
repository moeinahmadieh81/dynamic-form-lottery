package com.example.dynamicform.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIT extends IntegrationTestSupport {

    @Test
    void adminWriteEndpointsShouldRejectAnonymousAndRegularUsers() {
        ResponseEntity<JsonNode> anonymousResponse = exchange(
                "/api/forms",
                HttpMethod.POST,
                null,
                defaultFormBody("Anonymous Form")
        );
        assertThat(anonymousResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        RegisteredUser user = registerUser("security.user@example.com", "Security User");
        ResponseEntity<JsonNode> userResponse = exchange(
                "/api/forms",
                HttpMethod.POST,
                user.token(),
                defaultFormBody("Forbidden Form")
        );
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void draftFormsShouldBeHiddenFromRegularUsers() {
        long formId = createForm("Hidden Draft Form");
        RegisteredUser user = registerUser("security.visibility@example.com", "Visibility User");

        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId,
                HttpMethod.GET,
                user.token(),
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminShouldNotBeAllowedToSubmitAsARegularParticipant() {
        long formId = createForm("Admin Submission Guard");
        publishForm(formId);

        ResponseEntity<JsonNode> response = exchange(
                "/api/forms/" + formId + "/submissions",
                HttpMethod.POST,
                adminToken,
                Map.of("answers", Map.of(
                        "fullName", "Integration Admin",
                        "age", 35,
                        "city", "tehran"
                ))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
