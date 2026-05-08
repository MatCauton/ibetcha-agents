package com.ibetcha.wagering.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outcome_claims")
public class OutcomeClaim {

    @Id
    private UUID id;

    @Column(name = "bet_id", nullable = false)
    private UUID betId;

    @Column(name = "claimant_id", nullable = false)
    private UUID claimantId;

    @Column(name = "proposed_winner_id", nullable = false)
    private UUID proposedWinnerId;

    @Column(name = "is_concession", nullable = false)
    private boolean concession;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected OutcomeClaim() {}

    public static OutcomeClaim create(UUID betId, UUID claimantId, UUID proposedWinnerId) {
        OutcomeClaim claim = new OutcomeClaim();
        claim.id = UUID.randomUUID();
        claim.betId = betId;
        claim.claimantId = claimantId;
        claim.proposedWinnerId = proposedWinnerId;
        claim.concession = !claimantId.equals(proposedWinnerId);
        claim.status = ClaimStatus.PENDING;
        claim.createdAt = Instant.now();
        return claim;
    }

    public void approve() {
        this.status = ClaimStatus.APPROVED;
        this.resolvedAt = Instant.now();
    }

    public void reject() {
        this.status = ClaimStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBetId() { return betId; }
    public UUID getClaimantId() { return claimantId; }
    public UUID getProposedWinnerId() { return proposedWinnerId; }
    public boolean isConcession() { return concession; }
    public ClaimStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public enum ClaimStatus { PENDING, APPROVED, REJECTED }
}
