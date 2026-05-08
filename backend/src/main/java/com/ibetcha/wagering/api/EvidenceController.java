package com.ibetcha.wagering.api;

import com.ibetcha.shared.security.AuthenticatedUser;
import com.ibetcha.wagering.application.EvidenceService;
import com.ibetcha.wagering.domain.Evidence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bets")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/{betId}/evidence/upload-url")
    public ResponseEntity<Map<String, Object>> getUploadUrl(
            @PathVariable UUID betId,
            @RequestParam String contentType) {
        UUID requesterId = AuthenticatedUser.currentUserId();
        EvidenceService.UploadUrlResult result =
                evidenceService.generateUploadUrl(betId, requesterId, contentType);

        return ResponseEntity.ok(Map.of(
                "uploadUrl", result.uploadUrl(),
                "s3Key", result.s3Key(),
                "contentType", result.contentType(),
                "expiresIn", result.expiresIn()
        ));
    }

    @PostMapping("/{betId}/evidence")
    public ResponseEntity<Map<String, Object>> registerEvidence(
            @PathVariable UUID betId,
            @Valid @RequestBody RegisterEvidenceRequest request) {
        UUID uploadedBy = AuthenticatedUser.currentUserId();
        Evidence evidence = evidenceService.registerEvidence(
                betId, uploadedBy, request.s3Key(), request.contentType(), request.fileName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "evidenceId", evidence.getId(),
                "s3Key", evidence.getS3Key(),
                "contentType", evidence.getContentType(),
                "fileName", evidence.getFileName() != null ? evidence.getFileName() : "",
                "uploadedAt", evidence.getUploadedAt()
        ));
    }

    public record RegisterEvidenceRequest(
            @NotBlank @Size(max = 500) String s3Key,
            @NotBlank @Size(max = 50) String contentType,
            @Size(max = 255) String fileName
    ) {}
}
