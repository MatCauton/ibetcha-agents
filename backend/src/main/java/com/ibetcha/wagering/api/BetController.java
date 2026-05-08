package com.ibetcha.wagering.api;

import com.ibetcha.identity.domain.User;
import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.shared.security.AuthenticatedUser;
import com.ibetcha.wagering.application.BetService;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetParticipant;
import com.ibetcha.wagering.domain.BetStatus;
import com.ibetcha.wagering.domain.OutcomeVote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bets")
public class BetController {

    private final BetService betService;
    private final UserRepository userRepository;

    public BetController(BetService betService, UserRepository userRepository) {
        this.betService = betService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBet(@Valid @RequestBody CreateBetRequest request) {
        UUID creatorId = AuthenticatedUser.currentUserId();
        Bet bet = betService.createBet(
                creatorId,
                request.description(),
                request.stake(),
                request.participantIds(),
                request.title(),
                request.juryUserId(),
                request.deadline(),
                request.evidenceRequired() != null && request.evidenceRequired()
        );

        List<BetParticipant> participants = betService.getParticipants(bet.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildBetResponse(bet, participants));
    }

    @GetMapping("/{betId}")
    public ResponseEntity<Map<String, Object>> getBet(@PathVariable UUID betId) {
        Bet bet = betService.getBet(betId);
        List<BetParticipant> participants = betService.getParticipants(betId);
        return ResponseEntity.ok(buildBetResponse(bet, participants));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listBets() {
        UUID userId = AuthenticatedUser.currentUserId();
        List<Bet> bets = betService.getUserBets(userId);
        List<Map<String, Object>> betList = bets.stream()
                .map(bet -> {
                    List<BetParticipant> participants = betService.getParticipants(bet.getId());
                    return buildBetResponse(bet, participants);
                })
                .toList();
        return ResponseEntity.ok(Map.of("bets", betList));
    }

    @PostMapping("/{betId}/accept")
    public ResponseEntity<Map<String, Object>> acceptBet(@PathVariable UUID betId) {
        UUID userId = AuthenticatedUser.currentUserId();
        Bet bet = betService.acceptBet(betId, userId);
        return ResponseEntity.ok(Map.of(
                "betId", bet.getId(),
                "status", bet.getStatus().name(),
                "participantStatus", "ACCEPTED"
        ));
    }

    @PostMapping("/{betId}/decline")
    public ResponseEntity<Map<String, Object>> declineBet(@PathVariable UUID betId) {
        UUID userId = AuthenticatedUser.currentUserId();
        betService.declineBet(betId, userId);
        return ResponseEntity.ok(Map.of("betId", betId, "status", "DECLINED"));
    }

    @PostMapping("/{betId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBet(@PathVariable UUID betId) {
        UUID userId = AuthenticatedUser.currentUserId();
        betService.cancelBet(betId, userId);
        return ResponseEntity.ok(Map.of("betId", betId, "status", "CANCELLED"));
    }

    @PostMapping("/{betId}/complete")
    public ResponseEntity<Map<String, Object>> completeBet(
            @PathVariable UUID betId,
            @Valid @RequestBody CompleteBetRequest request) {
        UUID claimantId = AuthenticatedUser.currentUserId();
        Bet bet = betService.completeBet(betId, claimantId, request.winnerId());
        return ResponseEntity.ok(Map.of(
                "betId", bet.getId(),
                "status", bet.getStatus().name(),
                "outcome", Map.of(
                        "winnerId", request.winnerId(),
                        "declaredBy", claimantId,
                        "approvalStatus", bet.getStatus() == BetStatus.RESOLVED ? "APPROVED" : "PENDING"
                )
        ));
    }

    @PostMapping("/{betId}/concede")
    public ResponseEntity<Map<String, Object>> concedeBet(@PathVariable UUID betId) {
        UUID userId = AuthenticatedUser.currentUserId();
        Bet bet = betService.concedeBet(betId, userId);
        return ResponseEntity.ok(Map.of(
                "betId", bet.getId(),
                "status", bet.getStatus().name(),
                "winnerId", bet.getWinnerId() != null ? bet.getWinnerId() : ""
        ));
    }

    @PostMapping("/{betId}/jury/verdict")
    public ResponseEntity<Map<String, Object>> submitJuryVerdict(
            @PathVariable UUID betId,
            @Valid @RequestBody JuryVerdictRequest request) {
        UUID juryUserId = AuthenticatedUser.currentUserId();
        Bet bet = betService.submitJuryVerdict(betId, juryUserId, request.approved(), request.winnerId());
        return ResponseEntity.ok(Map.of(
                "betId", bet.getId(),
                "status", bet.getStatus().name(),
                "approved", request.approved()
        ));
    }

    @PostMapping("/{betId}/outcome/vote")
    public ResponseEntity<Map<String, Object>> voteOnOutcome(
            @PathVariable UUID betId,
            @Valid @RequestBody VoteRequest request) {
        UUID voterId = AuthenticatedUser.currentUserId();
        OutcomeVote.VoteType voteType = OutcomeVote.VoteType.valueOf(request.vote().toUpperCase());
        Bet bet = betService.voteOnOutcome(betId, voterId, voteType);
        return ResponseEntity.ok(Map.of(
                "betId", bet.getId(),
                "status", bet.getStatus().name(),
                "majorityReached", bet.getStatus() == BetStatus.RESOLVED
        ));
    }

    // ── Response builder ──────────────────────────────────────────────────────

    private Map<String, Object> buildBetResponse(Bet bet, List<BetParticipant> participants) {
        // Batch-fetch all users involved in this bet
        Set<UUID> userIds = new HashSet<>();
        participants.forEach(p -> userIds.add(p.getUserId()));
        if (bet.getJuryId() != null) userIds.add(bet.getJuryId());

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Split creator vs invitees
        BetParticipant creatorParticipant = participants.stream()
                .filter(p -> p.getRole() == BetParticipant.ParticipantRole.CREATOR)
                .findFirst()
                .orElse(null);

        List<BetParticipant> invitees = participants.stream()
                .filter(p -> p.getRole() == BetParticipant.ParticipantRole.INVITEE)
                .toList();

        // Build creator object
        Map<String, Object> creatorObj = new LinkedHashMap<>();
        if (creatorParticipant != null) {
            User creatorUser = userMap.get(creatorParticipant.getUserId());
            creatorObj.put("userId", creatorParticipant.getUserId());
            creatorObj.put("displayName", creatorUser != null ? creatorUser.getDisplayName() : "Unknown");
            if (creatorUser != null && creatorUser.getAvatarUrl() != null) {
                creatorObj.put("avatarUrl", creatorUser.getAvatarUrl());
            }
        }

        // Build invitee participant list
        List<Map<String, Object>> participantList = invitees.stream()
                .map(p -> {
                    User user = userMap.get(p.getUserId());
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("userId", p.getUserId());
                    pm.put("displayName", user != null ? user.getDisplayName() : "Unknown");
                    if (user != null && user.getAvatarUrl() != null) pm.put("avatarUrl", user.getAvatarUrl());
                    pm.put("status", p.getResponseStatus().name());
                    if (p.getRespondedAt() != null) pm.put("acceptedAt", p.getRespondedAt());
                    return pm;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("betId", bet.getId());
        response.put("description", bet.getDescription());
        response.put("stake", bet.getStake());
        response.put("status", bet.getStatus().name());
        response.put("createdAt", bet.getCreatedAt());
        response.put("creator", creatorObj);
        response.put("participants", participantList);

        if (bet.getTitle() != null) response.put("title", bet.getTitle());
        if (bet.getAcceptanceDeadline() != null) response.put("acceptanceDeadline", bet.getAcceptanceDeadline());
        if (bet.getWinnerId() != null) response.put("winnerId", bet.getWinnerId());
        if (bet.getResolvedAt() != null) response.put("resolvedAt", bet.getResolvedAt());

        // Jury
        if (bet.getJuryId() != null) {
            User juryUser = userMap.get(bet.getJuryId());
            Map<String, Object> juryObj = new LinkedHashMap<>();
            juryObj.put("userId", bet.getJuryId());
            juryObj.put("displayName", juryUser != null ? juryUser.getDisplayName() : "Unknown");
            if (juryUser != null && juryUser.getAvatarUrl() != null) juryObj.put("avatarUrl", juryUser.getAvatarUrl());
            response.put("jury", juryObj);
        }

        return response;
    }

    // ── Request records ───────────────────────────────────────────────────────

    public record CreateBetRequest(
            @NotBlank @Size(min = 1, max = 500) String description,
            @NotBlank @Size(min = 1, max = 200) String stake,
            @NotNull List<UUID> participantIds,
            String title,
            UUID juryUserId,
            Instant deadline,
            Boolean evidenceRequired
    ) {}

    public record CompleteBetRequest(@NotNull UUID winnerId) {}

    public record VoteRequest(@NotBlank String vote) {}

    public record JuryVerdictRequest(@NotNull Boolean approved, UUID winnerId) {}
}
