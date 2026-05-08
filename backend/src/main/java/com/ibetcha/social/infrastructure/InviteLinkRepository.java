package com.ibetcha.social.infrastructure;

import com.ibetcha.social.domain.InviteLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteLinkRepository extends JpaRepository<InviteLink, UUID> {

    Optional<InviteLink> findByReferralCode(String referralCode);

    Optional<InviteLink> findByInviterId(UUID inviterId);
}
