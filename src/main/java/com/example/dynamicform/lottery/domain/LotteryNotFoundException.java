package com.example.dynamicform.lottery.domain;

public class LotteryNotFoundException extends RuntimeException {
    public LotteryNotFoundException(Long lotteryId) {
        super("Lottery not found: " + lotteryId);
    }

    public LotteryNotFoundException(String context) {
        super("Lottery not found " + context);
    }
}
