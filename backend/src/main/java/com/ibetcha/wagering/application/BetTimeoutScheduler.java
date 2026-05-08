package com.ibetcha.wagering.application;

import com.ibetcha.notification.application.NotificationService;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetParticipant;
import com.ibetcha.wagering.domain.BetStatus;
import com.ibetcha.wagering.infrastructure.BetParticipantRepository;
import com.ibetcha.wagering.infrastructure.BetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled jobs for bet timeout processing.
 *
 * Two independent jobs:
 * 1. Acceptance timeout  — every 5 minutes, expire PENDING_ACCEPTANCE bets past their deadline.
 * 2. Jury verdict timeout — every 10 minutes, escalate PENDING_JURY_VERDICT bets past jury_deadline
 *    to PENDING_APPROVAL (participant majority vote).
 *
 * Both jobs are guarded by the bet's own status check inside the domain object
 * so concurrent runs are idempotent.
 */
@Component
public class BetTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(BetTimeoutScheduler.class);

    private final BetRepository betRepository;
    private final BetParticipantRepository participantRepository;
    private final NotificationService notificationService;

    public BetTimeoutScheduler(BetRepository betRepository,
                                BetParticipantRepository participantRepository,
                                NotificationService notificationService) {
        this.betRepository = betRepository;
        this.participantRepository = participantRepository;
        this.notificationService = notificationService;
    }

    /**
     * Every 5 minutes: expire PENDING_ACCEPTANCE bets whose acceptance deadline has passed.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireAcceptanceTimeouts() {
        Instant now = Instant.now();
        List<Bet> timedOut = betRepository.findByStatusAndAcceptanceDeadlineBefore(
                BetStatus.PENDING_ACCEPTANCE, now);

        if (!timedOut.isEmpty()) {
            log.info("[SCHEDULER] Expiring {} acceptance-timeout bet(s)", timedOut.size());
        }

        for (Bet bet : timedOut) {
            try {
                bet.expire();
                betRepository.save(bet);

                List<UUID> participantIds = participantRepository.findByBetId(bet.getId()).stream()
                        .filter(p -> !p.isDeclined())
                        .map(BetParticipant::getUserId)
                        .toList();

                notificationService.notifyBetExpired(bet.getId(), participantIds);
                log.info("[SCHEDULER] Bet {} expired (acceptance timeout)", bet.getId());
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to expire bet {}: {}", bet.getId(), e.getMessage());
            }
        }
    }

    /**
     * Every 10 minutes: escalate PENDING_JURY_VERDICT bets whose jury_deadline has passed
     * to PENDING_APPROVAL (participant majority vote replaces jury).
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void escalateJuryTimeouts() {
        Instant now = Instant.now();
        List<Bet> timedOut = betRepository.findByStatusAndJuryDeadlineBefore(
                BetStatus.PENDING_JURY_VERDICT, now);

        if (!timedOut.isEmpty()) {
            log.info("[SCHEDULER] Escalating {} jury-timeout bet(s) to participant vote", timedOut.size());
        }

        for (Bet bet : timedOut) {
            try {
                UUID juryId = bet.getJuryId();
                bet.escalateJuryTimeout();
                betRepository.save(bet);

                List<UUID> participantIds = participantRepository.findByBetId(bet.getId()).stream()
                        .filter(BetParticipant::isAccepted)
                        .map(BetParticipant::getUserId)
                        .toList();

                notificationService.notifyJuryTimedOut(bet.getId(), juryId, participantIds);
                log.info("[SCHEDULER] Bet {} escalated to PENDING_APPROVAL (jury timeout)", bet.getId());
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to escalate jury timeout for bet {}: {}", bet.getId(), e.getMessage());
            }
        }
    }
}
