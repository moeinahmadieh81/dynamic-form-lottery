package com.example.dynamicform.lottery.api.dto;

import com.example.dynamicform.lottery.domain.LotteryStatus;

import java.time.Instant;
import java.util.List;

public record PublicLotteryResultResponse(
        Long id,
        Long formId,
        LotteryStatus status,
        Integer winnerCount,
        long participantCount,
        Instant completedAt,
        List<PublicLotteryWinnerResponse> winners
) {
}
