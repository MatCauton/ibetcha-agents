package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.domain.OutcomeVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutcomeVoteRepository extends JpaRepository<OutcomeVote, UUID> {

    List<OutcomeVote> findByClaimId(UUID claimId);

    long countByClaimIdAndVote(UUID claimId, OutcomeVote.VoteType vote);

    boolean existsByClaimIdAndUserId(UUID claimId, UUID userId);
}
