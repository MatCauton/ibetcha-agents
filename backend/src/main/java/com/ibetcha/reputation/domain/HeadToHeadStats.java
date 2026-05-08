package com.ibetcha.reputation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "head_to_head_stats")
public class HeadToHeadStats {

    @Id
    private UUID id;

    @Column(name = "user_id_lower", nullable = false)
    private UUID userIdLower;

    @Column(name = "user_id_higher", nullable = false)
    private UUID userIdHigher;

    @Column(name = "lower_wins", nullable = false)
    private int lowerWins = 0;

    @Column(name = "higher_wins", nullable = false)
    private int higherWins = 0;

    @Column(name = "total_bets", nullable = false)
    private int totalBets = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HeadToHeadStats() {}

    public static HeadToHeadStats create(UUID userA, UUID userB) {
        HeadToHeadStats h2h = new HeadToHeadStats();
        h2h.id = UUID.randomUUID();
        if (userA.compareTo(userB) < 0) {
            h2h.userIdLower = userA;
            h2h.userIdHigher = userB;
        } else {
            h2h.userIdLower = userB;
            h2h.userIdHigher = userA;
        }
        h2h.updatedAt = Instant.now();
        return h2h;
    }

    public void recordWin(UUID winnerId) {
        if (winnerId.equals(userIdLower)) {
            lowerWins++;
        } else if (winnerId.equals(userIdHigher)) {
            higherWins++;
        } else {
            throw new IllegalArgumentException("Winner is not part of this head-to-head");
        }
        totalBets++;
        updatedAt = Instant.now();
    }

    public int getWinsFor(UUID userId) {
        if (userId.equals(userIdLower)) return lowerWins;
        if (userId.equals(userIdHigher)) return higherWins;
        throw new IllegalArgumentException("User is not part of this head-to-head");
    }

    public int getLossesFor(UUID userId) {
        if (userId.equals(userIdLower)) return higherWins;
        if (userId.equals(userIdHigher)) return lowerWins;
        throw new IllegalArgumentException("User is not part of this head-to-head");
    }

    public UUID getId() { return id; }
    public UUID getUserIdLower() { return userIdLower; }
    public UUID getUserIdHigher() { return userIdHigher; }
    public int getLowerWins() { return lowerWins; }
    public int getHigherWins() { return higherWins; }
    public int getTotalBets() { return totalBets; }
    public Instant getUpdatedAt() { return updatedAt; }
}
