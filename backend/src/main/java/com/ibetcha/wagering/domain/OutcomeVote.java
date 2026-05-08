package com.ibetcha.wagering.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outcome_votes")
public class OutcomeVote {

    @Id
    private UUID id;

    @Column(name = "bet_id", nullable = false)
    private UUID betId;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private VoteType vote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OutcomeVote() {}

    public static OutcomeVote create(UUID betId, UUID claimId, UUID userId, VoteType vote) {
        OutcomeVote v = new OutcomeVote();
        v.id = UUID.randomUUID();
        v.betId = betId;
        v.claimId = claimId;
        v.userId = userId;
        v.vote = vote;
        v.createdAt = Instant.now();
        return v;
    }

    public UUID getId() { return id; }
    public UUID getBetId() { return betId; }
    public UUID getClaimId() { return claimId; }
    public UUID getUserId() { return userId; }
    public VoteType getVote() { return vote; }
    public Instant getCreatedAt() { return createdAt; }

    public enum VoteType { APPROVE, DISPUTE }
}
