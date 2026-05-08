package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.domain.BetParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetParticipantRepository extends JpaRepository<BetParticipant, UUID> {

    List<BetParticipant> findByBetId(UUID betId);

    Optional<BetParticipant> findByBetIdAndUserId(UUID betId, UUID userId);

    long countByBetIdAndResponseStatus(UUID betId, BetParticipant.ResponseStatus status);
}
