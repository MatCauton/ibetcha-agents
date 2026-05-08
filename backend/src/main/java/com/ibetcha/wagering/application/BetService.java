package com.ibetcha.wagering.application;

import com.ibetcha.notification.application.NotificationService;
import com.ibetcha.reputation.application.StatsService;
import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.social.application.FriendshipService;
import com.ibetcha.wagering.domain.*;
import com.ibetcha.wagering.infrastructure.BetParticipantRepository;
import com.ibetcha.wagering.infrastructure.BetRepository;
import com.ibetcha.wagering.infrastructure.OutcomeClaimRepository;
import com.ibetcha.wagering.infrastructure.OutcomeVoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BetService {

    private static final Logger log = LoggerFactory.getLogger(BetService.class);

    private final BetRepository betRepository;
    private final BetParticipantRepository participantRepository;
    private final OutcomeClaimRepository claimRepository;
    private final OutcomeVoteRepository voteRepository;
    private final FriendshipService friendshipService;
    private final StatsService statsService;
    private final NotificationService notificationService;

    public BetService(BetRepository betRepository,
                      BetParticipantRepository participantRepository,
                      OutcomeClaimRepository claimRepository,
                      OutcomeVoteRepository voteRepository,
                      FriendshipService friendshipService,
                      StatsService statsService,
                      NotificationService notificationService) {
        this.betRepository = betRepository;
        this.participantRepository = participantRepository;
        this.claimRepository = claimRepository;
        this.voteRepository = voteRepository;
        this.friendshipService = friendshipService;
        this.statsService = statsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Bet createBet(UUID creatorId, String description, String stake,
                         List<UUID> participantIds, String title, UUID juryId,
                         Instant deadline, boolean evidenceRequired) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw ApiException.badRequest("A bet must have at least one other participant");
        }

        // Validate all participants are friends
        for (UUID participantId : participantIds) {
            if (!friendshipService.areFriends(creatorId, participantId)) {
                throw ApiException.badRequest("You can only bet with friends");
            }
        }

        // Validate jury is not a participant
        if (juryId != null) {
            if (juryId.equals(creatorId) || participantIds.contains(juryId)) {
                throw ApiException.badRequest("Jury cannot be a participant in the bet");
            }
        }

        // Validate deadline
        if (deadline != null && deadline.isBefore(Instant.now())) {
            throw ApiException.badRequest("Deadline must be in the future");
        }

        Bet bet = Bet.create(creatorId, description, stake, title, juryId, deadline, evidenceRequired);
        betRepository.save(bet);

        // Creator is always an accepted participant
        BetParticipant creator = BetParticipant.createCreator(bet.getId(), creatorId);
        participantRepository.save(creator);

        // Add invitees
        for (UUID participantId : participantIds) {
            BetParticipant invitee = BetParticipant.createInvitee(bet.getId(), participantId);
            participantRepository.save(invitee);
        }

        return bet;
    }

    @Transactional
    public Bet acceptBet(UUID betId, UUID userId) {
        Bet bet = getBetOrThrow(betId);

        if (bet.getStatus() != BetStatus.PENDING_ACCEPTANCE) {
            if (bet.getStatus() == BetStatus.EXPIRED) {
                throw ApiException.conflict("This bet has expired");
            }
            throw ApiException.conflict("Bet is not in PENDING_ACCEPTANCE state");
        }

        BetParticipant participant = participantRepository.findByBetIdAndUserId(betId, userId)
                .orElseThrow(() -> ApiException.forbidden("You are not a participant in this bet"));

        if (!participant.isPending()) {
            throw ApiException.conflict("You have already responded to this bet");
        }

        participant.accept();
        participantRepository.save(participant);

        // Check if all invitees have accepted (no more PENDING)
        long pendingCount = participantRepository.countByBetIdAndResponseStatus(
                betId, BetParticipant.ResponseStatus.PENDING);

        if (pendingCount == 0) {
            // Check at least 2 participants remain (accepted)
            long acceptedCount = participantRepository.countByBetIdAndResponseStatus(
                    betId, BetParticipant.ResponseStatus.ACCEPTED);
            if (acceptedCount >= 2) {
                bet.activate();
                betRepository.save(bet);
            }
        }

        return betRepository.findById(betId).orElse(bet);
    }

    @Transactional
    public void declineBet(UUID betId, UUID userId) {
        Bet bet = getBetOrThrow(betId);
        if (bet.getStatus() != BetStatus.PENDING_ACCEPTANCE) {
            throw ApiException.conflict("Bet is not in PENDING_ACCEPTANCE state");
        }
        BetParticipant participant = participantRepository.findByBetIdAndUserId(betId, userId)
                .orElseThrow(() -> ApiException.forbidden("You are not a participant in this bet"));
        if (!participant.isPending()) {
            throw ApiException.conflict("You have already responded to this bet");
        }
        participant.decline();
        participantRepository.save(participant);

        // Cancel if no pending invitees remain and fewer than 2 accepted
        long acceptedCount = participantRepository.countByBetIdAndResponseStatus(betId, BetParticipant.ResponseStatus.ACCEPTED);
        long pendingCount  = participantRepository.countByBetIdAndResponseStatus(betId, BetParticipant.ResponseStatus.PENDING);
        if (pendingCount == 0 && acceptedCount < 2) {
            bet.cancel();
            betRepository.save(bet);
        }
    }

    @Transactional
    public void cancelBet(UUID betId, UUID userId) {
        Bet bet = getBetOrThrow(betId);
        if (!bet.getCreatorId().equals(userId)) {
            throw ApiException.forbidden("Only the creator can cancel a bet");
        }
        bet.cancel();
        betRepository.save(bet);
    }

    @Transactional
    public Bet concedeBet(UUID betId, UUID userId) {
        Bet bet = getBetOrThrow(betId);
        if (bet.getStatus() != BetStatus.ACTIVE) {
            throw ApiException.conflict("Bet is not active");
        }
        // Find the other accepted participant (the winner)
        UUID winnerId = participantRepository.findByBetId(betId).stream()
                .filter(BetParticipant::isAccepted)
                .map(BetParticipant::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Cannot determine opponent"));

        return completeBet(betId, userId, winnerId);
    }

    @Transactional
    public Bet completeBet(UUID betId, UUID claimantId, UUID winnerId) {
        Bet bet = getBetOrThrow(betId);

        if (bet.getStatus() != BetStatus.ACTIVE) {
            throw ApiException.conflict("Bet is not active");
        }

        // Verify claimant is a participant
        participantRepository.findByBetIdAndUserId(betId, claimantId)
                .filter(BetParticipant::isAccepted)
                .orElseThrow(() -> ApiException.forbidden("You are not a participant in this bet"));

        // Verify winner is a participant
        participantRepository.findByBetIdAndUserId(betId, winnerId)
                .filter(BetParticipant::isAccepted)
                .orElseThrow(() -> ApiException.badRequest("Winner must be a participant in the bet"));

        // Check for existing pending claim
        claimRepository.findByBetIdAndStatus(betId, OutcomeClaim.ClaimStatus.PENDING)
                .ifPresent(c -> {
                    throw ApiException.conflict("An outcome is already awaiting approval");
                });

        OutcomeClaim claim = OutcomeClaim.create(betId, claimantId, winnerId);
        claimRepository.save(claim);

        // Concession: claimant selects someone else as winner
        if (claim.isConcession()) {
            claim.approve();
            claimRepository.save(claim);
            bet.resolve(winnerId);
            betRepository.save(bet);
            updateStatsOnResolution(bet);
            return bet;
        }

        // No jury: move to PENDING_APPROVAL
        if (!bet.hasJury()) {
            bet.moveToPendingApproval();
            betRepository.save(bet);

            // Check if majority is already reached (e.g., 3-person bet where claimant + winner = 2 of 3)
            checkAndResolveMajority(bet, claim);

            return betRepository.findById(betId).orElse(bet);
        }

        // Has jury: move to PENDING_JURY_VERDICT
        bet.moveToPendingJuryVerdict();
        betRepository.save(bet);
        notificationService.notifyJuryVerdictRequested(bet.getId(), bet.getJuryId(), claimantId, winnerId);

        return betRepository.findById(betId).orElse(bet);
    }

    @Transactional
    public Bet submitJuryVerdict(UUID betId, UUID juryUserId, boolean approved, UUID winnerId) {
        Bet bet = getBetOrThrow(betId);

        if (bet.getStatus() != BetStatus.PENDING_JURY_VERDICT) {
            throw ApiException.conflict("Bet is not awaiting jury verdict");
        }

        if (!juryUserId.equals(bet.getJuryId())) {
            throw ApiException.forbidden("Only the designated jury can submit a verdict");
        }

        OutcomeClaim claim = claimRepository.findByBetIdAndStatus(betId, OutcomeClaim.ClaimStatus.PENDING)
                .orElseThrow(() -> ApiException.conflict("No pending outcome claim"));

        if (approved) {
            // Validate proposed winner matches what was claimed, or jury selects the original proposed winner
            UUID resolvedWinner = winnerId != null ? winnerId : claim.getProposedWinnerId();
            participantRepository.findByBetIdAndUserId(betId, resolvedWinner)
                    .filter(BetParticipant::isAccepted)
                    .orElseThrow(() -> ApiException.badRequest("Winner must be a participant in the bet"));

            claim.approve();
            claimRepository.save(claim);
            bet.resolve(resolvedWinner);
            betRepository.save(bet);
            updateStatsOnResolution(bet);
            notificationService.notifyBetResolved(bet.getId(), resolvedWinner);
        } else {
            // Jury rejected: clear claim, return bet to ACTIVE
            claim.reject();
            claimRepository.save(claim);
            bet.returnToActive();
            betRepository.save(bet);
            notificationService.notifyJuryRejected(bet.getId(), juryUserId);
        }

        return betRepository.findById(betId).orElse(bet);
    }

    @Transactional
    public Bet voteOnOutcome(UUID betId, UUID voterId, OutcomeVote.VoteType voteType) {
        Bet bet = getBetOrThrow(betId);

        if (bet.getStatus() != BetStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Bet is not awaiting approval");
        }

        // Verify voter is a participant
        participantRepository.findByBetIdAndUserId(betId, voterId)
                .filter(BetParticipant::isAccepted)
                .orElseThrow(() -> ApiException.forbidden("You are not a participant in this bet"));

        OutcomeClaim claim = claimRepository.findByBetIdAndStatus(betId, OutcomeClaim.ClaimStatus.PENDING)
                .orElseThrow(() -> ApiException.conflict("No pending outcome claim"));

        // Prevent duplicate votes
        if (voteRepository.existsByClaimIdAndUserId(claim.getId(), voterId)) {
            throw ApiException.conflict("You have already voted on this outcome");
        }

        // Proposed winner and claimant have implicit approve votes -- they should not vote explicitly
        if (voterId.equals(claim.getProposedWinnerId())) {
            throw ApiException.conflict("Proposed winner has an implicit approval vote");
        }
        if (voterId.equals(claim.getClaimantId())) {
            throw ApiException.conflict("Claimant has an implicit approval vote");
        }

        OutcomeVote vote = OutcomeVote.create(betId, claim.getId(), voterId, voteType);
        voteRepository.save(vote);

        checkAndResolveMajority(bet, claim);

        return betRepository.findById(betId).orElse(bet);
    }

    private void checkAndResolveMajority(Bet bet, OutcomeClaim claim) {
        List<BetParticipant> accepted = participantRepository.findByBetId(bet.getId()).stream()
                .filter(BetParticipant::isAccepted)
                .toList();
        int totalParticipants = accepted.size();
        int majorityThreshold = (totalParticipants / 2) + 1;

        // Count approvals: implicit (claimant + proposed winner if different) + explicit
        Set<UUID> approvers = new HashSet<>();
        approvers.add(claim.getClaimantId()); // Claimant implicitly approves
        approvers.add(claim.getProposedWinnerId()); // Proposed winner implicitly approves

        long explicitApprovals = voteRepository.countByClaimIdAndVote(
                claim.getId(), OutcomeVote.VoteType.APPROVE);
        long explicitDisputes = voteRepository.countByClaimIdAndVote(
                claim.getId(), OutcomeVote.VoteType.DISPUTE);

        long totalApprovals = approvers.size() + explicitApprovals;

        if (totalApprovals >= majorityThreshold) {
            // Majority reached -- resolve
            claim.approve();
            claimRepository.save(claim);

            // Reload bet in case it was modified
            Bet freshBet = betRepository.findById(bet.getId()).orElse(bet);
            freshBet.resolve(claim.getProposedWinnerId());
            betRepository.save(freshBet);
            updateStatsOnResolution(freshBet);
        } else if (totalParticipants == 2 && explicitDisputes > 0) {
            // 2-person deadlock
            Bet freshBet = betRepository.findById(bet.getId()).orElse(bet);
            freshBet.dispute();
            betRepository.save(freshBet);
        }
    }

    @Transactional(readOnly = true)
    public Bet getBet(UUID betId) {
        return getBetOrThrow(betId);
    }

    @Transactional(readOnly = true)
    public List<BetParticipant> getParticipants(UUID betId) {
        return participantRepository.findByBetId(betId);
    }

    @Transactional(readOnly = true)
    public List<Bet> getUserBets(UUID userId) {
        return betRepository.findBetsForUser(userId);
    }

    private Bet getBetOrThrow(UUID betId) {
        return betRepository.findById(betId)
                .orElseThrow(() -> ApiException.notFound("Bet not found"));
    }

    private void updateStatsOnResolution(Bet bet) {
        UUID winnerId = bet.getWinnerId();
        List<BetParticipant> participants = participantRepository.findByBetId(bet.getId());
        List<UUID> loserIds = participants.stream()
                .filter(BetParticipant::isAccepted)
                .map(BetParticipant::getUserId)
                .filter(id -> !id.equals(winnerId))
                .toList();

        statsService.updateOnBetResolved(winnerId, loserIds);
    }
}
