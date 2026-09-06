package com.example.dynamicform.lottery.infrastructure.persistence;

import com.example.dynamicform.lottery.domain.LotteryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface LotteryRepository extends JpaRepository<LotteryEntity, Long> {

    boolean existsByFormIdAndStatusIn(Long formId, Collection<LotteryStatus> statuses);

    Optional<LotteryEntity> findFirstByFormIdAndStatusInOrderByCreatedAtDesc(
            Long formId,
            Collection<LotteryStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LotteryEntity l where l.id = :lotteryId")
    Optional<LotteryEntity> findByIdForUpdate(@Param("lotteryId") Long lotteryId);
}
