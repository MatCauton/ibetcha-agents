package com.ibetcha.wagering.application;

import com.ibetcha.shared.exception.ApiException;
import com.ibetcha.wagering.domain.Bet;
import com.ibetcha.wagering.domain.BetStatus;
import com.ibetcha.wagering.domain.Evidence;
import com.ibetcha.wagering.infrastructure.BetParticipantRepository;
import com.ibetcha.wagering.infrastructure.BetRepository;
import com.ibetcha.wagering.infrastructure.EvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final int PRESIGNED_URL_EXPIRY_SECONDS = 900;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "video/mp4", "video/quicktime"
    );

    private final BetRepository betRepository;
    private final BetParticipantRepository participantRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceStoragePort storagePort;

    public EvidenceService(BetRepository betRepository,
                           BetParticipantRepository participantRepository,
                           EvidenceRepository evidenceRepository,
                           EvidenceStoragePort storagePort) {
        this.betRepository = betRepository;
        this.participantRepository = participantRepository;
        this.evidenceRepository = evidenceRepository;
        this.storagePort = storagePort;
    }

    @Transactional(readOnly = true)
    public UploadUrlResult generateUploadUrl(UUID betId, UUID requesterId, String contentType) {
        validateContentType(contentType);

        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> ApiException.notFound("Bet not found"));

        if (bet.getStatus() != BetStatus.ACTIVE && bet.getStatus() != BetStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Evidence can only be uploaded for ACTIVE or PENDING_APPROVAL bets");
        }

        requireParticipantOrCreator(betId, requesterId);

        String extension = extensionFor(contentType);
        String s3Key = "evidence/" + betId + "/" + UUID.randomUUID() + "." + extension;

        String uploadUrl = storagePort.generateUploadUrl(s3Key, contentType, PRESIGNED_URL_EXPIRY_SECONDS);

        return new UploadUrlResult(uploadUrl, s3Key, contentType, PRESIGNED_URL_EXPIRY_SECONDS);
    }

    @Transactional
    public Evidence registerEvidence(UUID betId, UUID uploadedBy,
                                     String s3Key, String contentType, String fileName) {
        validateContentType(contentType);

        betRepository.findById(betId)
                .orElseThrow(() -> ApiException.notFound("Bet not found"));

        requireParticipantOrCreator(betId, uploadedBy);

        Evidence evidence = Evidence.create(betId, uploadedBy, s3Key, contentType, fileName);
        return evidenceRepository.save(evidence);
    }

    private void requireParticipantOrCreator(UUID betId, UUID userId) {
        participantRepository.findByBetIdAndUserId(betId, userId)
                .filter(p -> p.isAccepted())
                .orElseThrow(() -> ApiException.forbidden("You are not a participant in this bet"));
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw ApiException.badRequest(
                    "Unsupported content type. Allowed: image/jpeg, image/png, video/mp4, video/quicktime");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "video/mp4" -> "mp4";
            case "video/quicktime" -> "mov";
            default -> "bin";
        };
    }

    public record UploadUrlResult(String uploadUrl, String s3Key,
                                  String contentType, int expiresIn) {}
}
