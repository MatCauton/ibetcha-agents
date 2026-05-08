--liquibase formatted sql

--changeset atlas:identity-001-create-users
CREATE TABLE users (
    id                      UUID            PRIMARY KEY,
    email                   VARCHAR(255)    NOT NULL,
    username                VARCHAR(30)     NOT NULL,
    display_name            VARCHAR(50),
    bio                     VARCHAR(150),
    avatar_url              VARCHAR(512),
    password_hash           VARCHAR(72),
    auth_provider           VARCHAR(20)     NOT NULL DEFAULT 'EMAIL',
    provider_id             VARCHAR(255),
    email_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    onboarding_completed    BOOLEAN         NOT NULL DEFAULT FALSE,
    terms_accepted_at       TIMESTAMPTZ,
    account_status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    deletion_scheduled_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_username_format CHECK (username ~ '^[a-zA-Z0-9_.]{3,30}$'),
    CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('EMAIL', 'GOOGLE', 'APPLE', 'SYSTEM')),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('ACTIVE', 'DELETION_PENDING', 'DELETED')),
    CONSTRAINT ck_users_bio_length CHECK (LENGTH(bio) <= 150),
    CONSTRAINT ck_users_password_or_oauth CHECK (
        (auth_provider = 'EMAIL' AND password_hash IS NOT NULL)
        OR (auth_provider IN ('GOOGLE', 'APPLE') AND provider_id IS NOT NULL)
        OR (auth_provider = 'SYSTEM')
    )
);

INSERT INTO users (id, email, username, display_name, auth_provider, email_verified, account_status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'deleted@ibetcha.internal', '_deleted_user_', '[Deleted User]', 'SYSTEM', TRUE, 'DELETED', NOW(), NOW());

CREATE INDEX idx_users_username_lower ON users (LOWER(username) varchar_pattern_ops);
CREATE UNIQUE INDEX idx_users_provider_lookup ON users (auth_provider, provider_id) WHERE provider_id IS NOT NULL;
CREATE INDEX idx_users_deletion_scheduled ON users (deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL;
CREATE INDEX idx_users_email_lower ON users (LOWER(email));
--rollback DROP TABLE IF EXISTS users CASCADE;

--changeset atlas:identity-002-create-refresh-tokens
CREATE TABLE refresh_tokens (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jti             VARCHAR(64)     NOT NULL,
    token_family    VARCHAR(64)     NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti)
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (token_family);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
--rollback DROP TABLE IF EXISTS refresh_tokens CASCADE;
