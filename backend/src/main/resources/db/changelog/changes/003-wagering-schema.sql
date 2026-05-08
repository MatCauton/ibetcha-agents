--liquibase formatted sql

--changeset atlas:wagering-001-create-bets
CREATE TABLE bets (
    id                      UUID            PRIMARY KEY,
    creator_id              UUID            NOT NULL REFERENCES users(id),
    title                   VARCHAR(100),
    description             VARCHAR(500)    NOT NULL,
    stake                   VARCHAR(200)    NOT NULL,
    status                  VARCHAR(25)     NOT NULL DEFAULT 'PENDING_ACCEPTANCE',
    jury_id                 UUID            REFERENCES users(id),
    deadline                TIMESTAMPTZ,
    evidence_required       BOOLEAN         NOT NULL DEFAULT FALSE,
    winner_id               UUID            REFERENCES users(id),
    acceptance_deadline     TIMESTAMPTZ     NOT NULL,
    jury_deadline           TIMESTAMPTZ,
    resolved_at             TIMESTAMPTZ,
    version                 INTEGER         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_bets_status CHECK (status IN (
        'PENDING_ACCEPTANCE', 'ACTIVE', 'PENDING_JURY_VERDICT',
        'PENDING_APPROVAL', 'RESOLVED', 'DISPUTED', 'EXPIRED', 'CANCELLED'
    )),
    CONSTRAINT ck_bets_resolved_has_winner CHECK (
        (status = 'RESOLVED' AND winner_id IS NOT NULL AND resolved_at IS NOT NULL)
        OR status != 'RESOLVED'
    ),
    CONSTRAINT ck_bets_description_length CHECK (LENGTH(description) BETWEEN 1 AND 500),
    CONSTRAINT ck_bets_stake_length CHECK (LENGTH(stake) BETWEEN 1 AND 200)
);

CREATE INDEX idx_bets_status ON bets (status) WHERE status NOT IN ('RESOLVED', 'EXPIRED', 'CANCELLED');
CREATE INDEX idx_bets_acceptance_deadline ON bets (acceptance_deadline) WHERE status = 'PENDING_ACCEPTANCE';
CREATE INDEX idx_bets_creator ON bets (creator_id, status);
CREATE INDEX idx_bets_jury_pending ON bets (jury_id) WHERE jury_id IS NOT NULL AND status IN ('PENDING_JURY_VERDICT');
CREATE INDEX idx_bets_jury_deadline ON bets (jury_deadline) WHERE status = 'PENDING_JURY_VERDICT';
CREATE INDEX idx_bets_resolved ON bets (resolved_at DESC) WHERE status = 'RESOLVED';
CREATE INDEX idx_bets_updated ON bets (updated_at DESC);
--rollback DROP TABLE IF EXISTS bets CASCADE;

--changeset atlas:wagering-002-create-bet-participants
CREATE TABLE bet_participants (
    id              UUID            PRIMARY KEY,
    bet_id          UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id),
    role            VARCHAR(10)     NOT NULL DEFAULT 'INVITEE',
    response_status VARCHAR(10)     NOT NULL DEFAULT 'PENDING',
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bet_participants UNIQUE (bet_id, user_id),
    CONSTRAINT ck_bp_role CHECK (role IN ('CREATOR', 'INVITEE')),
    CONSTRAINT ck_bp_response_status CHECK (response_status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

CREATE INDEX idx_bp_user_status ON bet_participants (user_id, response_status);
CREATE INDEX idx_bp_bet_response ON bet_participants (bet_id, response_status);
--rollback DROP TABLE IF EXISTS bet_participants CASCADE;

--changeset atlas:wagering-003-create-outcome-claims
CREATE TABLE outcome_claims (
    id                  UUID            PRIMARY KEY,
    bet_id              UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    claimant_id         UUID            NOT NULL REFERENCES users(id),
    proposed_winner_id  UUID            NOT NULL REFERENCES users(id),
    is_concession       BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ,
    CONSTRAINT ck_oc_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX idx_oc_active_claim ON outcome_claims (bet_id) WHERE status = 'PENDING';
CREATE INDEX idx_oc_bet ON outcome_claims (bet_id, created_at DESC);
--rollback DROP TABLE IF EXISTS outcome_claims CASCADE;

--changeset atlas:wagering-004-create-outcome-votes
CREATE TABLE outcome_votes (
    id              UUID            PRIMARY KEY,
    bet_id          UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    claim_id        UUID            NOT NULL REFERENCES outcome_claims(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id),
    vote            VARCHAR(10)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ov_user_claim UNIQUE (claim_id, user_id),
    CONSTRAINT ck_ov_vote CHECK (vote IN ('APPROVE', 'DISPUTE'))
);

CREATE INDEX idx_ov_bet ON outcome_votes (bet_id);
--rollback DROP TABLE IF EXISTS outcome_votes CASCADE;

--changeset atlas:wagering-005-create-bet-evidence
CREATE TABLE bet_evidence (
    id                  UUID            PRIMARY KEY,
    bet_id              UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    uploaded_by         UUID            NOT NULL REFERENCES users(id),
    media_type          VARCHAR(10)     NOT NULL,
    s3_key              VARCHAR(512)    NOT NULL,
    file_size_bytes     BIGINT          NOT NULL,
    content_type        VARCHAR(50)     NOT NULL,
    thumbnail_s3_key    VARCHAR(512),
    upload_confirmed    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_be_media_type CHECK (media_type IN ('PHOTO', 'VIDEO')),
    CONSTRAINT ck_be_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 52428800),
    CONSTRAINT ck_be_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'video/mp4'))
);

CREATE INDEX idx_be_bet ON bet_evidence (bet_id) WHERE upload_confirmed = TRUE;
--rollback DROP TABLE IF EXISTS bet_evidence CASCADE;
