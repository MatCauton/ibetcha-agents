package com.ibetcha.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_links")
public class InviteLink {

    @Id
    private UUID id;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;

    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InviteLink() {}

    public static InviteLink create(UUID inviterId, String code) {
        InviteLink link = new InviteLink();
        link.id = UUID.randomUUID();
        link.inviterId = inviterId;
        link.referralCode = code;
        link.createdAt = Instant.now();
        return link;
    }

    public UUID getId() { return id; }
    public UUID getInviterId() { return inviterId; }
    public String getReferralCode() { return referralCode; }
    public Instant getCreatedAt() { return createdAt; }
}
