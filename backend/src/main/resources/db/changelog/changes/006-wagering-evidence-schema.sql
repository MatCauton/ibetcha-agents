--liquibase formatted sql

--changeset atlas:wagering-006-create-evidence
CREATE TABLE evidence (
    id              UUID            PRIMARY KEY,
    bet_id          UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    uploaded_by     UUID            NOT NULL REFERENCES users(id),
    s3_key          VARCHAR(500)    NOT NULL UNIQUE,
    content_type    VARCHAR(50)     NOT NULL,
    file_name       VARCHAR(255),
    uploaded_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_evidence_content_type CHECK (content_type IN ('image/jpeg','image/png','video/mp4','video/quicktime'))
);

CREATE INDEX idx_evidence_bet ON evidence (bet_id, uploaded_at DESC);
--rollback DROP TABLE IF EXISTS evidence CASCADE;
