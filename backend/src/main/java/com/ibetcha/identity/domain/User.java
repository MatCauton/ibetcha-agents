package com.ibetcha.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(length = 150)
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "password_hash", length = 72)
    private String passwordHash;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.EMAIL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "account_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public static User createEmailUser(UUID id, String email, String username,
                                        String displayName, String passwordHash) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.username = username;
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        user.authProvider = AuthProvider.EMAIL;
        user.emailVerified = true; // For MVP walking skeleton, auto-verify
        user.accountStatus = AccountStatus.ACTIVE;
        user.termsAcceptedAt = Instant.now();
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    public static User createOAuthUser(UUID id, String email, String displayName,
                                        AuthProvider provider, String providerId) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.displayName = displayName;
        user.authProvider = provider;
        user.providerId = providerId;
        user.emailVerified = true;
        user.accountStatus = AccountStatus.ACTIVE;
        user.termsAcceptedAt = Instant.now();
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPasswordHash() { return passwordHash; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getProviderId() { return providerId; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.updatedAt = Instant.now();
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.updatedAt = Instant.now();
    }

    public enum AuthProvider {
        EMAIL, GOOGLE, APPLE, SYSTEM
    }

    public enum AccountStatus {
        ACTIVE, DELETION_PENDING, DELETED
    }
}
