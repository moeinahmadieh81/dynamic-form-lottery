package com.example.dynamicform.lottery.api.dto;

import java.time.Instant;

public record PublicLotteryWinnerResponse(
        Integer position,
        String displayName,
        boolean currentUser,
        Instant selectedAt
) {
}
