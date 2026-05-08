--liquibase formatted sql

--changeset atlas:reputation-001-create-user-stats
CREATE TABLE user_stats (
    user_id                 UUID            PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_bets              INTEGER         NOT NULL DEFAULT 0,
    wins                    INTEGER         NOT NULL DEFAULT 0,
    losses                  INTEGER         NOT NULL DEFAULT 0,
    current_streak_type     VARCHAR(4),
    current_streak_count    INTEGER         NOT NULL DEFAULT 0,
    longest_win_streak      INTEGER         NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_us_streak_type CHECK (current_streak_type IN ('WIN', 'LOSS') OR current_streak_type IS NULL),
    CONSTRAINT ck_us_non_negative CHECK (wins >= 0 AND losses >= 0 AND total_bets >= 0),
    CONSTRAINT ck_us_total CHECK (total_bets = wins + losses)
);
--rollback DROP TABLE IF EXISTS user_stats CASCADE;

--changeset atlas:reputation-002-create-head-to-head-stats
CREATE TABLE head_to_head_stats (
    id              UUID            PRIMARY KEY,
    user_id_lower   UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_higher  UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lower_wins      INTEGER         NOT NULL DEFAULT 0,
    higher_wins     INTEGER         NOT NULL DEFAULT 0,
    total_bets      INTEGER         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_h2h_pair UNIQUE (user_id_lower, user_id_higher),
    CONSTRAINT ck_h2h_ordering CHECK (user_id_lower < user_id_higher),
    CONSTRAINT ck_h2h_non_negative CHECK (lower_wins >= 0 AND higher_wins >= 0),
    CONSTRAINT ck_h2h_total CHECK (total_bets = lower_wins + higher_wins)
);

CREATE INDEX idx_h2h_lower ON head_to_head_stats (user_id_lower);
CREATE INDEX idx_h2h_higher ON head_to_head_stats (user_id_higher);
--rollback DROP TABLE IF EXISTS head_to_head_stats CASCADE;
