package com.ibetcha.social.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friendships")
public class Friendship {

    @Id
    private UUID id;

    @Column(name = "user_id_lower", nullable = false)
    private UUID userIdLower;

    @Column(name = "user_id_higher", nullable = false)
    private UUID userIdHigher;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Friendship() {}

    public static Friendship createRequest(UUID requesterId, UUID targetId) {
        Friendship f = new Friendship();
        f.id = UUID.randomUUID();
        f.requesterId = requesterId;

        // Ensure lower < higher for canonical ordering
        if (requesterId.compareTo(targetId) < 0) {
            f.userIdLower = requesterId;
            f.userIdHigher = targetId;
        } else {
            f.userIdLower = targetId;
            f.userIdHigher = requesterId;
        }

        f.status = FriendshipStatus.PENDING;
        f.createdAt = Instant.now();
        f.updatedAt = Instant.now();
        return f;
    }

    public void accept() {
        if (this.status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Can only accept a PENDING friendship request");
        }
        this.status = FriendshipStatus.ACTIVE;
        this.acceptedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void decline() {
        if (this.status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Can only decline a PENDING friendship request");
        }
        this.status = FriendshipStatus.REMOVED;
        this.updatedAt = Instant.now();
    }

    public boolean involves(UUID userId) {
        return userIdLower.equals(userId) || userIdHigher.equals(userId);
    }

    public UUID getOtherUser(UUID userId) {
        if (userIdLower.equals(userId)) return userIdHigher;
        if (userIdHigher.equals(userId)) return userIdLower;
        throw new IllegalArgumentException("User is not part of this friendship");
    }

    public UUID getId() { return id; }
    public UUID getUserIdLower() { return userIdLower; }
    public UUID getUserIdHigher() { return userIdHigher; }
    public FriendshipStatus getStatus() { return status; }
    public UUID getRequesterId() { return requesterId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum FriendshipStatus {
        PENDING, ACTIVE, REMOVED
    }
}
