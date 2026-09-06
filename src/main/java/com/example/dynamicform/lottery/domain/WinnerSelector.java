package com.example.dynamicform.lottery.domain;

import com.example.dynamicform.lottery.infrastructure.persistence.LotteryEntryEntity;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class WinnerSelector {

    private final SecureRandom secureRandom = new SecureRandom();

    public List<LotteryEntryEntity> select(List<LotteryEntryEntity> entries, int winnerCount) {
        if (winnerCount <= 0) {
            throw new IllegalArgumentException("winnerCount must be greater than zero");
        }
        if (winnerCount > entries.size()) {
            throw new IllegalArgumentException("winnerCount cannot exceed participant count");
        }

        List<LotteryEntryEntity> shuffled = new ArrayList<>(entries);

        // Partial Fisher-Yates shuffle. Only the first winnerCount positions need to be randomized.
        for (int i = 0; i < winnerCount; i++) {
            int selectedIndex = i + secureRandom.nextInt(shuffled.size() - i);
            Collections.swap(shuffled, i, selectedIndex);
        }

        return List.copyOf(shuffled.subList(0, winnerCount));
    }
}
