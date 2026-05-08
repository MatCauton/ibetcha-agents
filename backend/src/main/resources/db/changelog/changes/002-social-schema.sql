--liquibase formatted sql

--changeset atlas:social-001-create-friendships
CREATE TABLE friendships (
    id              UUID            PRIMARY KEY,
    user_id_lower   UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_higher  UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    requester_id    UUID            NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_friendships_pair UNIQUE (user_id_lower, user_id_higher),
    CONSTRAINT ck_friendships_ordering CHECK (user_id_lower < user_id_higher),
    CONSTRAINT ck_friendships_status CHECK (status IN ('PENDING', 'ACTIVE', 'REMOVED')),
    CONSTRAINT ck_friendships_different_users CHECK (user_id_lower != user_id_higher)
);

CREATE INDEX idx_friendships_lower_active ON friendships (user_id_lower, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_friendships_higher_active ON friendships (user_id_higher, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_friendships_pending ON friendships (status, requester_id) WHERE status = 'PENDING';
CREATE INDEX idx_friendships_pending_lower ON friendships (user_id_lower) WHERE status = 'PENDING';
CREATE INDEX idx_friendships_pending_higher ON friendships (user_id_higher) WHERE status = 'PENDING';
--rollback DROP TABLE IF EXISTS friendships CASCADE;

--changeset atlas:social-002-create-blocks
CREATE TABLE blocks (
    id              UUID            PRIMARY KEY,
    blocker_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blocks_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT ck_blocks_not_self CHECK (blocker_id != blocked_id)
);

CREATE INDEX idx_blocks_blocked ON blocks (blocked_id);
--rollback DROP TABLE IF EXISTS blocks CASCADE;

--changeset atlas:social-003-create-invite-links
CREATE TABLE invite_links (
    id              UUID            PRIMARY KEY,
    inviter_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referral_code   VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invite_links_code UNIQUE (referral_code)
);

CREATE INDEX idx_invite_links_inviter ON invite_links (inviter_id);
--rollback DROP TABLE IF EXISTS invite_links CASCADE;
