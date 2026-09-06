package com.example.dynamicform.lottery.domain;

import com.example.dynamicform.lottery.infrastructure.persistence.LotteryEntryEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinnerSelectorTest {

    private final WinnerSelector selector = new WinnerSelector();

    @Test
    void selectsRequestedNumberWithoutDuplicates() {
        List<LotteryEntryEntity> entries = List.of(
                LotteryEntryEntity.snapshot(1L, 10L, 100L),
                LotteryEntryEntity.snapshot(1L, 11L, 101L),
                LotteryEntryEntity.snapshot(1L, 12L, 102L),
                LotteryEntryEntity.snapshot(1L, 13L, 103L)
        );

        List<LotteryEntryEntity> winners = selector.select(entries, 3);

        assertThat(winners).hasSize(3).doesNotHaveDuplicates();
        assertThat(entries).containsAll(winners);
    }

    @Test
    void rejectsWinnerCountGreaterThanParticipants() {
        List<LotteryEntryEntity> entries = List.of(
                LotteryEntryEntity.snapshot(1L, 10L, 100L)
        );

        assertThatThrownBy(() -> selector.select(entries, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed participant count");
    }
}
