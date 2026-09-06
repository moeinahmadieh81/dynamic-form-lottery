package com.example.dynamicform.lottery.api.dto;

import java.time.Instant;

public record AdminLotteryWinnerResponse(
        Integer position,
        Long userId,
        String email,
        String displayName,
        Long submissionId,
        Instant selectedAt
) {
}
