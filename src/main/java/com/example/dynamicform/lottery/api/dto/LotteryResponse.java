package com.example.dynamicform.lottery.api.dto;

import com.example.dynamicform.lottery.domain.LotteryStatus;

import java.time.Instant;
import java.util.List;

public record LotteryResponse(
        Long id,
        Long formId,
        LotteryStatus status,
        Integer winnerCount,
        long participantCount,
        Long createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        List<LotteryWinnerResponse> winners
) {
}
