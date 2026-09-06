package com.example.dynamicform.lottery.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateLotteryRequest(
        @NotNull @Min(1) Integer winnerCount
) {
}
