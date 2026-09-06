package com.example.dynamicform.lottery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LotteryEntryRepository extends JpaRepository<LotteryEntryEntity, Long> {
    List<LotteryEntryEntity> findAllByLotteryIdOrderByIdAsc(Long lotteryId);
    long countByLotteryId(Long lotteryId);
}
