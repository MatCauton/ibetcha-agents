package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BetRepository extends JpaRepository<Bet, UUID> {

    @Query("SELECT b FROM Bet b JOIN BetParticipant bp ON b.id = bp.betId " +
           "WHERE bp.userId = :userId AND bp.responseStatus != 'DECLINED' " +
           "ORDER BY b.updatedAt DESC")
    List<Bet> findBetsForUser(@Param("userId") UUID userId);

    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.acceptanceDeadline < :now")
    List<Bet> findByStatusAndAcceptanceDeadlineBefore(
            @Param("status") BetStatus status,
            @Param("now") Instant now);

    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.juryDeadline < :now")
    List<Bet> findByStatusAndJuryDeadlineBefore(
            @Param("status") BetStatus status,
            @Param("now") Instant now);

    /**
     * Test-only helper: sets acceptance_deadline to a past timestamp so the
     * scheduler can be triggered without waiting for real time to pass.
     * Not called from production code paths.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Bet b SET b.acceptanceDeadline = :deadline WHERE b.id = :betId")
    void setAcceptanceDeadlineForTest(@Param("betId") UUID betId, @Param("deadline") Instant deadline);

    /**
     * Test-only helper: sets jury_deadline to a past timestamp.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Bet b SET b.juryDeadline = :deadline WHERE b.id = :betId")
    void setJuryDeadlineForTest(@Param("betId") UUID betId, @Param("deadline") Instant deadline);
}
