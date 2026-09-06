package com.example.dynamicform.lottery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LotteryWinnerRepository extends JpaRepository<LotteryWinnerEntity, Long> {
    List<LotteryWinnerEntity> findAllByLotteryIdOrderByPositionAsc(Long lotteryId);
}
