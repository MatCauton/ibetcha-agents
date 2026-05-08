package com.ibetcha.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "token_family", nullable = false, length = 64)
    private String tokenFamily;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public static RefreshToken create(UUID userId, String jti, String tokenFamily, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.userId = userId;
        token.jti = jti;
        token.tokenFamily = tokenFamily;
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getJti() { return jti; }
    public String getTokenFamily() { return tokenFamily; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
