package com.ibetcha.notification.application;

import com.ibetcha.notification.domain.DeviceToken;
import com.ibetcha.notification.infrastructure.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Notification application service.
 *
 * Walking Skeleton 2: all notifications are logged. Real FCM sending is
 * deferred to Walking Skeleton 3 when the firebase-admin SDK is wired.
 *
 * NotificationService is a driven port consumer — BetService publishes
 * domain events to it, and it decides how to deliver them.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    // ── Device Token Registration ──────────────────────────────────────────

    @Transactional
    public DeviceToken registerDeviceToken(UUID userId, String token,
                                            DeviceToken.Platform platform, String deviceId) {
        return deviceTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(existing -> {
                    existing.updateToken(token);
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> deviceTokenRepository.save(
                        DeviceToken.create(userId, token, platform, deviceId)));
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> getTokensForUser(UUID userId) {
        return deviceTokenRepository.findByUserId(userId);
    }

    // ── Bet Lifecycle Notifications (stubs — log only for WS2) ────────────

    public void notifyBetCreated(UUID betId, UUID creatorId, List<UUID> participantIds) {
        log.info("[NOTIFICATION] BET_CREATED betId={} creator={} participants={}",
                betId, creatorId, participantIds);
        // WS3: send FCM push to each participant
    }

    public void notifyBetAccepted(UUID betId, UUID acceptedByUserId) {
        log.info("[NOTIFICATION] BET_ACCEPTED betId={} acceptedBy={}", betId, acceptedByUserId);
    }

    public void notifyBetActivated(UUID betId, List<UUID> participantIds) {
        log.info("[NOTIFICATION] BET_ACTIVATED betId={} participants={}", betId, participantIds);
    }

    public void notifyBetResolved(UUID betId, UUID winnerId) {
        log.info("[NOTIFICATION] BET_RESOLVED betId={} winner={}", betId, winnerId);
    }

    public void notifyJuryVerdictRequested(UUID betId, UUID juryId, UUID claimantId, UUID proposedWinnerId) {
        log.info("[NOTIFICATION] OUTCOME_CLAIMED betId={} jury={} claimant={} proposedWinner={}",
                betId, juryId, claimantId, proposedWinnerId);
    }

    public void notifyJuryRejected(UUID betId, UUID juryId) {
        log.info("[NOTIFICATION] JURY_REJECTED betId={} jury={}", betId, juryId);
    }

    public void notifyBetExpired(UUID betId, List<UUID> participantIds) {
        log.info("[NOTIFICATION] BET_EXPIRED betId={} participants={}", betId, participantIds);
    }

    public void notifyJuryTimedOut(UUID betId, UUID juryId, List<UUID> participantIds) {
        log.info("[NOTIFICATION] JURY_TIMED_OUT betId={} jury={} participants={}", betId, juryId, participantIds);
    }
}
