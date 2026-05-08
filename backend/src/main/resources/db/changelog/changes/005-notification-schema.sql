--liquibase formatted sql

--changeset atlas:notification-001-create-notifications
CREATE TABLE notifications (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(30)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    body            VARCHAR(500)    NOT NULL,
    bet_id          UUID,
    payload_json    JSONB,
    delivery_status VARCHAR(15)     NOT NULL DEFAULT 'PENDING',
    fcm_message_id  VARCHAR(255),
    error_message   VARCHAR(500),
    attempt_count   SMALLINT        NOT NULL DEFAULT 0,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ,
    CONSTRAINT ck_notif_type CHECK (type IN (
        'BET_CREATED', 'BET_ACCEPTED', 'BET_ACTIVATED', 'BET_DECLINED',
        'BET_CANCELLED', 'BET_EXPIRED', 'OUTCOME_CLAIMED', 'JURY_APPROVED',
        'JURY_REJECTED', 'JURY_TIMED_OUT', 'BET_RESOLVED', 'BET_DISPUTED',
        'DISPUTE_JURY_APPOINTED',
        'FRIEND_REQUEST', 'FRIEND_ACCEPTED',
        'RIVALRY_MILESTONE',
        'ACCEPTANCE_REMINDER', 'JURY_REMINDER'
    )),
    CONSTRAINT ck_notif_delivery CHECK (delivery_status IN (
        'PENDING', 'QUEUED', 'SENT', 'DELIVERED', 'FAILED'
    ))
);

CREATE INDEX idx_notif_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notif_user_unread ON notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX idx_notif_failed ON notifications (delivery_status, created_at) WHERE delivery_status = 'FAILED';
CREATE INDEX idx_notif_pending ON notifications (delivery_status, created_at) WHERE delivery_status = 'PENDING';
--rollback DROP TABLE IF EXISTS notifications CASCADE;

--changeset atlas:notification-002-create-device-tokens
CREATE TABLE device_tokens (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(512)    NOT NULL,
    platform        VARCHAR(10)     NOT NULL,
    device_id       VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_dt_user_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_dt_platform CHECK (platform IN ('IOS', 'ANDROID'))
);

CREATE INDEX idx_dt_token ON device_tokens (token);
--rollback DROP TABLE IF EXISTS device_tokens CASCADE;

--changeset atlas:shared-001-create-domain-events
CREATE TABLE domain_events (
    id              UUID            PRIMARY KEY,
    aggregate_type  VARCHAR(30)     NOT NULL,
    aggregate_id    UUID            NOT NULL,
    event_type      VARCHAR(50)     NOT NULL,
    actor_id        UUID,
    payload         JSONB           NOT NULL,
    published       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_de_aggregate_type CHECK (aggregate_type IN (
        'BET', 'USER', 'FRIENDSHIP', 'BLOCK', 'INVITE_LINK', 'PLAYER_STATS'
    ))
);

CREATE INDEX idx_de_unpublished ON domain_events (created_at ASC) WHERE published = FALSE;
CREATE INDEX idx_de_aggregate ON domain_events (aggregate_type, aggregate_id, created_at DESC);
CREATE INDEX idx_de_actor ON domain_events (actor_id, created_at DESC) WHERE actor_id IS NOT NULL;
--rollback DROP TABLE IF EXISTS domain_events CASCADE;
