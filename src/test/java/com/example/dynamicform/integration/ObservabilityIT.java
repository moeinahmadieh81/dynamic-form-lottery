package com.example.dynamicform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityIT extends IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealthShouldBePublicAndUp() {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/actuator/health",
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .contains("\"status\":\"UP\"");
    }

    @Test
    void livenessProbeShouldBeAvailable() {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/actuator/health/liveness",
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .contains("\"status\":\"UP\"");
    }

    @Test
    void readinessProbeShouldBeAvailable() {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/actuator/health/readiness",
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .contains("\"status\":\"UP\"");
    }

    @Test
    void openApiSpecificationShouldBeAvailable() {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/v3/api-docs",
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .contains("\"openapi\"")
                .contains("\"Dynamic Form Lottery API\"")
                .contains("\"bearerAuth\"");
    }
}