package com.ibetcha.wagering.application;

import com.ibetcha.identity.infrastructure.UserRepository;
import com.ibetcha.reputation.domain.HeadToHeadStats;
import com.ibetcha.reputation.infrastructure.HeadToHeadStatsRepository;
import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetParticipant;
import com.ibetcha.wagering.domain.BetStatus;
import com.ibetcha.wagering.infrastructure.BetParticipantRepository;
import com.ibetcha.wagering.infrastructure.BetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WinCardService {

    private final BetRepository betRepository;
    private final BetParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final HeadToHeadStatsRepository h2hRepository;

    public WinCardService(BetRepository betRepository,
                          BetParticipantRepository participantRepository,
                          UserRepository userRepository,
                          HeadToHeadStatsRepository h2hRepository) {
        this.betRepository = betRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.h2hRepository = h2hRepository;
    }

    @Transactional(readOnly = true)
    public WinCard buildWinCard(UUID betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> ApiException.notFound("Bet not found"));

        if (bet.getStatus() != BetStatus.RESOLVED) {
            throw ApiException.conflict("Win Card is only available for RESOLVED bets");
        }

        List<BetParticipant> accepted = participantRepository.findByBetId(betId).stream()
                .filter(BetParticipant::isAccepted)
                .toList();

        UUID winnerId = bet.getWinnerId();
        Map<UUID, String> displayNames = loadDisplayNames(accepted);

        ParticipantInfo winner = new ParticipantInfo(
                winnerId,
                displayNames.getOrDefault(winnerId, "Unknown")
        );

        List<UUID> loserIds = accepted.stream()
                .map(BetParticipant::getUserId)
                .filter(id -> !id.equals(winnerId))
                .toList();

        ParticipantInfo loser = loserIds.size() == 1
                ? new ParticipantInfo(loserIds.get(0), displayNames.getOrDefault(loserIds.get(0), "Unknown"))
                : null;

        List<ParticipantResult> allParticipants = accepted.stream()
                .map(p -> new ParticipantResult(
                        p.getUserId(),
                        displayNames.getOrDefault(p.getUserId(), "Unknown"),
                        p.getUserId().equals(winnerId)))
                .toList();

        HeadToHeadRecord h2hRecord = null;
        if (loser != null) {
            HeadToHeadStats h2h = h2hRepository.findByUserPair(winnerId, loser.userId())
                    .orElse(null);
            if (h2h != null) {
                h2hRecord = new HeadToHeadRecord(
                        h2h.getWinsFor(winnerId),
                        h2h.getLossesFor(winnerId)
                );
            } else {
                // This bet is the first one between them — stats haven't propagated yet
                // or stats are from a different pathway; default to 1-0
                h2hRecord = new HeadToHeadRecord(1, 0);
            }
        }

        String title = bet.getTitle() != null
                ? bet.getTitle()
                : truncate(bet.getDescription(), 60);

        return new WinCard(
                betId,
                title,
                bet.getDescription(),
                bet.getStake(),
                winner,
                loser,
                allParticipants,
                bet.getResolvedAt(),
                h2hRecord
        );
    }

    private Map<UUID, String> loadDisplayNames(List<BetParticipant> participants) {
        List<UUID> userIds = participants.stream().map(BetParticipant::getUserId).toList();
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(userIds)
                .forEach(u -> names.put(u.getId(), u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()));
        return names;
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    public record ParticipantInfo(UUID userId, String displayName) {}
    public record ParticipantResult(UUID userId, String displayName, boolean won) {}
    public record HeadToHeadRecord(int wins, int losses) {}

    public record WinCard(
            UUID betId,
            String title,
            String description,
            String stake,
            ParticipantInfo winner,
            ParticipantInfo loser,
            List<ParticipantResult> allParticipants,
            Instant resolvedAt,
            HeadToHeadRecord headToHeadRecord
    ) {}
}
