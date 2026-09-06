package com.example.dynamicform.lottery.api.dto;

import java.time.Instant;

public record LotteryWinnerResponse(
        Integer position,
        Long userId,
        Long submissionId,
        Instant selectedAt
) {
}
