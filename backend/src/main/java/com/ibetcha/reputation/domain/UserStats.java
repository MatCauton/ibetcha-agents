package com.ibetcha.reputation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "total_bets", nullable = false)
    private int totalBets = 0;

    @Column(nullable = false)
    private int wins = 0;

    @Column(nullable = false)
    private int losses = 0;

    @Column(name = "current_streak_type", length = 4)
    @Enumerated(EnumType.STRING)
    private StreakType currentStreakType;

    @Column(name = "current_streak_count", nullable = false)
    private int currentStreakCount = 0;

    @Column(name = "longest_win_streak", nullable = false)
    private int longestWinStreak = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserStats() {}

    public static UserStats createForUser(UUID userId) {
        UserStats stats = new UserStats();
        stats.userId = userId;
        stats.updatedAt = Instant.now();
        return stats;
    }

    public void recordWin() {
        this.totalBets++;
        this.wins++;
        if (this.currentStreakType == StreakType.WIN) {
            this.currentStreakCount++;
        } else {
            this.currentStreakType = StreakType.WIN;
            this.currentStreakCount = 1;
        }
        this.longestWinStreak = Math.max(this.longestWinStreak, this.currentStreakCount);
        this.updatedAt = Instant.now();
    }

    public void recordLoss() {
        this.totalBets++;
        this.losses++;
        if (this.currentStreakType == StreakType.LOSS) {
            this.currentStreakCount++;
        } else {
            this.currentStreakType = StreakType.LOSS;
            this.currentStreakCount = 1;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public int getTotalBets() { return totalBets; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public StreakType getCurrentStreakType() { return currentStreakType; }
    public int getCurrentStreakCount() { return currentStreakCount; }
    public int getLongestWinStreak() { return longestWinStreak; }
    public Instant getUpdatedAt() { return updatedAt; }

    public double getWinRate() {
        if (totalBets == 0) return 0.0;
        return (double) wins / totalBets;
    }

    public enum StreakType { WIN, LOSS }
}
