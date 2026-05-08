package com.ibetcha.wagering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    private UUID id;

    @Column(name = "bet_id", nullable = false)
    private UUID betId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected Evidence() {}

    public static Evidence create(UUID betId, UUID uploadedBy,
                                  String s3Key, String contentType, String fileName) {
        Evidence e = new Evidence();
        e.id = UUID.randomUUID();
        e.betId = betId;
        e.uploadedBy = uploadedBy;
        e.s3Key = s3Key;
        e.contentType = contentType;
        e.fileName = fileName;
        e.uploadedAt = Instant.now();
        return e;
    }

    public UUID getId() { return id; }
    public UUID getBetId() { return betId; }
    public UUID getUploadedBy() { return uploadedBy; }
    public String getS3Key() { return s3Key; }
    public String getContentType() { return contentType; }
    public String getFileName() { return fileName; }
    public Instant getUploadedAt() { return uploadedAt; }
}
