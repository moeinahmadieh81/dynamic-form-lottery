package com.example.dynamicform.lottery.infrastructure.persistence;

import com.example.dynamicform.lottery.domain.LotteryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "lotteries")
public class LotteryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LotteryStatus status;

    @Column(name = "winner_count", nullable = false)
    private Integer winnerCount;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected LotteryEntity() {
    }

    public static LotteryEntity ready(Long formId, Integer winnerCount, Long createdBy) {
        LotteryEntity lottery = new LotteryEntity();
        lottery.formId = formId;
        lottery.winnerCount = winnerCount;
        lottery.createdBy = createdBy;
        lottery.status = LotteryStatus.READY;
        return lottery;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void start() {
        if (status != LotteryStatus.READY) {
            throw new IllegalStateException("Only READY lotteries can be run");
        }
        status = LotteryStatus.RUNNING;
        startedAt = Instant.now();
    }

    public void complete() {
        if (status != LotteryStatus.RUNNING) {
            throw new IllegalStateException("Only RUNNING lotteries can be completed");
        }
        status = LotteryStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getFormId() { return formId; }
    public LotteryStatus getStatus() { return status; }
    public Integer getWinnerCount() { return winnerCount; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
