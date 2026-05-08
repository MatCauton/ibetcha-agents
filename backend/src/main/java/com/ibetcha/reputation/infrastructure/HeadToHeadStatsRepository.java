package com.ibetcha.reputation.infrastructure;

import com.ibetcha.reputation.domain.HeadToHeadStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HeadToHeadStatsRepository extends JpaRepository<HeadToHeadStats, UUID> {

    @Query("SELECT h FROM HeadToHeadStats h WHERE " +
           "h.userIdLower = LEAST(:userA, :userB) AND h.userIdHigher = GREATEST(:userA, :userB)")
    Optional<HeadToHeadStats> findByUserPair(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
