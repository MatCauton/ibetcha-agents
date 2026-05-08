package com.ibetcha.wagering.domain;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bets")
public class Bet {

    private static final Duration ACCEPTANCE_TIMEOUT = Duration.ofHours(48);
    private static final Duration JURY_VERDICT_TIMEOUT = Duration.ofDays(7);

    @Id
    private UUID id;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 200)
    private String stake;

    @Column(nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private BetStatus status = BetStatus.PENDING_ACCEPTANCE;

    @Column(name = "jury_id")
    private UUID juryId;

    private Instant deadline;

    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired = false;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "acceptance_deadline", nullable = false)
    private Instant acceptanceDeadline;

    @Column(name = "jury_deadline")
    private Instant juryDeadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Bet() {}

    public static Bet create(UUID creatorId, String description, String stake,
                             String title, UUID juryId, Instant deadline, boolean evidenceRequired) {
        Bet bet = new Bet();
        bet.id = UUID.randomUUID();
        bet.creatorId = creatorId;
        bet.description = description;
        bet.stake = stake;
        bet.title = title;
        bet.juryId = juryId;
        bet.deadline = deadline;
        bet.evidenceRequired = evidenceRequired;
        bet.status = BetStatus.PENDING_ACCEPTANCE;
        bet.createdAt = Instant.now();
        bet.updatedAt = Instant.now();
        bet.acceptanceDeadline = bet.createdAt.plus(ACCEPTANCE_TIMEOUT);
        return bet;
    }

    public void activate() {
        if (this.status != BetStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Can only activate from PENDING_ACCEPTANCE");
        }
        this.status = BetStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void moveToPendingApproval() {
        if (this.status != BetStatus.ACTIVE) {
            throw new IllegalStateException("Can only move to PENDING_APPROVAL from ACTIVE");
        }
        this.status = BetStatus.PENDING_APPROVAL;
        this.updatedAt = Instant.now();
    }

    public void moveToPendingJuryVerdict() {
        if (this.status != BetStatus.ACTIVE) {
            throw new IllegalStateException("Can only move to PENDING_JURY_VERDICT from ACTIVE");
        }
        this.status = BetStatus.PENDING_JURY_VERDICT;
        this.juryDeadline = Instant.now().plus(JURY_VERDICT_TIMEOUT);
        this.updatedAt = Instant.now();
    }

    public void returnToActive() {
        if (this.status != BetStatus.PENDING_JURY_VERDICT) {
            throw new IllegalStateException("Can only return to ACTIVE from PENDING_JURY_VERDICT");
        }
        this.status = BetStatus.ACTIVE;
        this.juryDeadline = null;
        this.updatedAt = Instant.now();
    }

    public void escalateJuryTimeout() {
        if (this.status != BetStatus.PENDING_JURY_VERDICT) {
            throw new IllegalStateException("Can only escalate from PENDING_JURY_VERDICT");
        }
        this.status = BetStatus.PENDING_APPROVAL;
        this.juryDeadline = null;
        this.updatedAt = Instant.now();
    }

    public void resolve(UUID winnerId) {
        this.status = BetStatus.RESOLVED;
        this.winnerId = winnerId;
        this.resolvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void dispute() {
        if (this.status != BetStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Can only dispute from PENDING_APPROVAL");
        }
        this.status = BetStatus.DISPUTED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status != BetStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Can only cancel from PENDING_ACCEPTANCE");
        }
        this.status = BetStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        if (this.status != BetStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Can only expire from PENDING_ACCEPTANCE");
        }
        this.status = BetStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCreatorId() { return creatorId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStake() { return stake; }
    public BetStatus getStatus() { return status; }
    public UUID getJuryId() { return juryId; }
    public Instant getDeadline() { return deadline; }
    public boolean isEvidenceRequired() { return evidenceRequired; }
    public UUID getWinnerId() { return winnerId; }
    public Instant getAcceptanceDeadline() { return acceptanceDeadline; }
    public Instant getJuryDeadline() { return juryDeadline; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean hasJury() { return juryId != null; }
}
