package com.ibetcha.wagering.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bet_participants")
public class BetParticipant {

    @Id
    private UUID id;

    @Column(name = "bet_id", nullable = false)
    private UUID betId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ParticipantRole role = ParticipantRole.INVITEE;

    @Column(name = "response_status", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ResponseStatus responseStatus = ResponseStatus.PENDING;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BetParticipant() {}

    public static BetParticipant createCreator(UUID betId, UUID userId) {
        BetParticipant p = new BetParticipant();
        p.id = UUID.randomUUID();
        p.betId = betId;
        p.userId = userId;
        p.role = ParticipantRole.CREATOR;
        p.responseStatus = ResponseStatus.ACCEPTED;
        p.respondedAt = Instant.now();
        p.createdAt = Instant.now();
        return p;
    }

    public static BetParticipant createInvitee(UUID betId, UUID userId) {
        BetParticipant p = new BetParticipant();
        p.id = UUID.randomUUID();
        p.betId = betId;
        p.userId = userId;
        p.role = ParticipantRole.INVITEE;
        p.responseStatus = ResponseStatus.PENDING;
        p.createdAt = Instant.now();
        return p;
    }

    public void accept() {
        if (this.responseStatus != ResponseStatus.PENDING) {
            throw new IllegalStateException("Can only accept a PENDING invitation");
        }
        this.responseStatus = ResponseStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline() {
        if (this.responseStatus != ResponseStatus.PENDING) {
            throw new IllegalStateException("Can only decline a PENDING invitation");
        }
        this.responseStatus = ResponseStatus.DECLINED;
        this.respondedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBetId() { return betId; }
    public UUID getUserId() { return userId; }
    public ParticipantRole getRole() { return role; }
    public ResponseStatus getResponseStatus() { return responseStatus; }
    public Instant getRespondedAt() { return respondedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isAccepted() { return responseStatus == ResponseStatus.ACCEPTED; }
    public boolean isPending() { return responseStatus == ResponseStatus.PENDING; }
    public boolean isDeclined() { return responseStatus == ResponseStatus.DECLINED; }

    public enum ParticipantRole { CREATOR, INVITEE }
    public enum ResponseStatus { PENDING, ACCEPTED, DECLINED }
}
