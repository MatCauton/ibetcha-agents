package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.domain.OutcomeClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OutcomeClaimRepository extends JpaRepository<OutcomeClaim, UUID> {

    Optional<OutcomeClaim> findByBetIdAndStatus(UUID betId, OutcomeClaim.ClaimStatus status);
}
