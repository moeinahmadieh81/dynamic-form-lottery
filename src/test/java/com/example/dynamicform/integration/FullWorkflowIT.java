package com.example.dynamicform.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FullWorkflowIT extends IntegrationTestSupport {

    @Test
    void completeBusinessFlowShouldPersistWinnerSnapshotAndAuditTrail() {
        long formId = createForm("Integration Lottery Flow");
        publishForm(formId);

        RegisteredUser ali = registerUser("flow.ali@example.com", "Ali Flow");
        RegisteredUser sara = registerUser("flow.sara@example.com", "Sara Flow");

        long aliSubmissionId = submit(formId, ali.token(), "Ali Flow", 28, "tehran");
        long saraSubmissionId = submit(formId, sara.token(), "Sara Flow", 31, "tabriz");

        closeForm(formId);
        long lotteryId = createLottery(formId, 1);

        ResponseEntity<JsonNode> runResponse = exchange(
                "/api/lotteries/" + lotteryId + "/run",
                HttpMethod.POST,
                adminToken,
                null
        );

        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runResponse.getBody()).isNotNull();

        JsonNode lottery = runResponse.getBody();
        assertThat(lottery.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(lottery.path("participantCount").asLong()).isEqualTo(2);
        assertThat(lottery.path("winnerCount").asInt()).isEqualTo(1);
        assertThat(lottery.path("winners").size()).isEqualTo(1);

        JsonNode winner = lottery.path("winners").get(0);
        assertThat(winner.path("userId").asLong()).isIn(ali.id(), sara.id());
        assertThat(winner.path("submissionId").asLong()).isIn(aliSubmissionId, saraSubmissionId);
        assertThat(winner.path("position").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> formResponse = exchange(
                "/api/forms/" + formId,
                HttpMethod.GET,
                adminToken,
                null
        );
        assertThat(formResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(formResponse.getBody()).isNotNull();
        assertThat(formResponse.getBody().path("status").asText()).isEqualTo("DRAWN");

        ResponseEntity<JsonNode> aliResult = exchange(
                "/api/forms/" + formId + "/lottery",
                HttpMethod.GET,
                ali.token(),
                null
        );
        ResponseEntity<JsonNode> saraResult = exchange(
                "/api/forms/" + formId + "/lottery",
                HttpMethod.GET,
                sara.token(),
                null
        );

        assertThat(aliResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saraResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aliResult.getBody()).isNotNull();
        assertThat(saraResult.getBody()).isNotNull();

        boolean aliIsWinner = aliResult.getBody().path("winners").get(0).path("currentUser").asBoolean();
        boolean saraIsWinner = saraResult.getBody().path("winners").get(0).path("currentUser").asBoolean();
        assertThat(aliIsWinner ^ saraIsWinner).isTrue();

        Integer entryCount = jdbcTemplate.queryForObject(
                "select count(*) from lottery_entries where lottery_id = ?",
                Integer.class,
                lotteryId
        );
        Integer winnerCount = jdbcTemplate.queryForObject(
                "select count(*) from lottery_winners where lottery_id = ?",
                Integer.class,
                lotteryId
        );
        Integer auditCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from audit_logs
                where (action = 'FORM_CLOSED' and entity_type = 'FORM' and entity_id = ?)
                   or (action in ('LOTTERY_CREATED', 'LOTTERY_COMPLETED')
                       and entity_type = 'LOTTERY' and entity_id = ?)
                """,
                Integer.class,
                formId,
                lotteryId
        );

        assertThat(entryCount).isEqualTo(2);
        assertThat(winnerCount).isEqualTo(1);
        assertThat(auditCount).isEqualTo(3);
    }
}
