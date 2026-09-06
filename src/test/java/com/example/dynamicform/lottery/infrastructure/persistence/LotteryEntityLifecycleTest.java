package com.example.dynamicform.lottery.infrastructure.persistence;

import com.example.dynamicform.lottery.domain.LotteryStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LotteryEntityLifecycleTest {

    @Test
    void movesFromReadyToRunningToCompleted() {
        LotteryEntity lottery = LotteryEntity.ready(1L, 2, 10L);

        lottery.start();
        assertThat(lottery.getStatus()).isEqualTo(LotteryStatus.RUNNING);
        assertThat(lottery.getStartedAt()).isNotNull();

        lottery.complete();
        assertThat(lottery.getStatus()).isEqualTo(LotteryStatus.COMPLETED);
        assertThat(lottery.getCompletedAt()).isNotNull();
    }

    @Test
    void completedLotteryCannotRunAgain() {
        LotteryEntity lottery = LotteryEntity.ready(1L, 1, 10L);
        lottery.start();
        lottery.complete();

        assertThatThrownBy(lottery::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("READY");
    }
}
