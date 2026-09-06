package com.example.dynamicform.lottery.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "lottery_entries")
public class LotteryEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lottery_id", nullable = false)
    private Long lotteryId;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LotteryEntryEntity() {
    }

    public static LotteryEntryEntity snapshot(Long lotteryId, Long submissionId, Long userId) {
        LotteryEntryEntity entry = new LotteryEntryEntity();
        entry.lotteryId = lotteryId;
        entry.submissionId = submissionId;
        entry.userId = userId;
        return entry;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getLotteryId() { return lotteryId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
}
