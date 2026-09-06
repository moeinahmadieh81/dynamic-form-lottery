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
@Table(name = "lottery_winners")
public class LotteryWinnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lottery_id", nullable = false)
    private Long lotteryId;

    @Column(name = "lottery_entry_id", nullable = false)
    private Long lotteryEntryId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;

    protected LotteryWinnerEntity() {
    }

    public static LotteryWinnerEntity selected(Long lotteryId,
                                                Long lotteryEntryId,
                                                Integer position) {
        LotteryWinnerEntity winner = new LotteryWinnerEntity();
        winner.lotteryId = lotteryId;
        winner.lotteryEntryId = lotteryEntryId;
        winner.position = position;
        return winner;
    }

    @PrePersist
    void onCreate() {
        selectedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getLotteryId() { return lotteryId; }
    public Long getLotteryEntryId() { return lotteryEntryId; }
    public Integer getPosition() { return position; }
    public Instant getSelectedAt() { return selectedAt; }
}
