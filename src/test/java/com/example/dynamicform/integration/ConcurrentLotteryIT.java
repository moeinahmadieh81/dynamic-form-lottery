package com.example.dynamicform.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentLotteryIT extends IntegrationTestSupport {

    @Test
    void onlyOneConcurrentRunShouldCompleteTheLottery() throws Exception {
        long formId = createForm("Concurrent Lottery Form");
        publishForm(formId);

        RegisteredUser first = registerUser("concurrent.first@example.com", "Concurrent First");
        RegisteredUser second = registerUser("concurrent.second@example.com", "Concurrent Second");
        submit(formId, first.token(), "Concurrent First", 26, "tehran");
        submit(formId, second.token(), "Concurrent Second", 29, "tabriz");
        closeForm(formId);
        long lotteryId = createLottery(formId, 1);

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CompletableFuture<ResponseEntity<JsonNode>> firstRun = CompletableFuture.supplyAsync(
                    () -> runLotteryWhenReleased(start, lotteryId),
                    executor
            );
            CompletableFuture<ResponseEntity<JsonNode>> secondRun = CompletableFuture.supplyAsync(
                    () -> runLotteryWhenReleased(start, lotteryId),
                    executor
            );

            start.countDown();

            ResponseEntity<JsonNode> firstResponse = firstRun.get(15, TimeUnit.SECONDS);
            ResponseEntity<JsonNode> secondResponse = secondRun.get(15, TimeUnit.SECONDS);

            List<HttpStatus> statuses = List.of(
                    HttpStatus.valueOf(firstResponse.getStatusCode().value()),
                    HttpStatus.valueOf(secondResponse.getStatusCode().value())
            );

            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        }

        Integer winnerCount = jdbcTemplate.queryForObject(
                "select count(*) from lottery_winners where lottery_id = ?",
                Integer.class,
                lotteryId
        );
        String status = jdbcTemplate.queryForObject(
                "select status from lotteries where id = ?",
                String.class,
                lotteryId
        );

        assertThat(winnerCount).isEqualTo(1);
        assertThat(status).isEqualTo("COMPLETED");
    }

    private ResponseEntity<JsonNode> runLotteryWhenReleased(CountDownLatch start, long lotteryId) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent lottery test did not start in time");
            }
            return exchange(
                    "/api/lotteries/" + lotteryId + "/run",
                    HttpMethod.POST,
                    adminToken,
                    null
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent lottery test was interrupted", ex);
        }
    }
}
