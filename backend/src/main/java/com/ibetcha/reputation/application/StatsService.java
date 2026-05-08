package com.ibetcha.reputation.application;

import com.ibetcha.reputation.domain.HeadToHeadStats;
import com.ibetcha.reputation.domain.UserStats;
import com.ibetcha.reputation.infrastructure.HeadToHeadStatsRepository;
import com.ibetcha.reputation.infrastructure.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StatsService {

    private final UserStatsRepository userStatsRepository;
    private final HeadToHeadStatsRepository h2hRepository;

    public StatsService(UserStatsRepository userStatsRepository,
                        HeadToHeadStatsRepository h2hRepository) {
        this.userStatsRepository = userStatsRepository;
        this.h2hRepository = h2hRepository;
    }

    @Transactional
    public void updateOnBetResolved(UUID winnerId, List<UUID> loserIds) {
        // Update winner stats
        UserStats winnerStats = userStatsRepository.findById(winnerId)
                .orElseGet(() -> UserStats.createForUser(winnerId));
        winnerStats.recordWin();
        userStatsRepository.save(winnerStats);

        // Update loser stats
        for (UUID loserId : loserIds) {
            UserStats loserStats = userStatsRepository.findById(loserId)
                    .orElseGet(() -> UserStats.createForUser(loserId));
            loserStats.recordLoss();
            userStatsRepository.save(loserStats);

            // Update head-to-head for each winner-loser pair
            HeadToHeadStats h2h = h2hRepository.findByUserPair(winnerId, loserId)
                    .orElseGet(() -> HeadToHeadStats.create(winnerId, loserId));
            h2h.recordWin(winnerId);
            h2hRepository.save(h2h);
        }
    }

    @Transactional(readOnly = true)
    public UserStats getStats(UUID userId) {
        return userStatsRepository.findById(userId)
                .orElseGet(() -> UserStats.createForUser(userId));
    }

    @Transactional(readOnly = true)
    public HeadToHeadStats getHeadToHead(UUID userA, UUID userB) {
        return h2hRepository.findByUserPair(userA, userB)
                .orElseGet(() -> HeadToHeadStats.create(userA, userB));
    }
}
