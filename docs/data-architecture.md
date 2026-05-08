# iBetcha -- Data Architecture

**Date:** 2026-05-08
**Author:** Atlas (Data Engineering Architect)
**Database:** PostgreSQL 16 on AWS RDS
**Migration Tool:** Liquibase (XML/YAML changesets)
**ORM:** Spring Data JPA / Hibernate 6.x
**Architecture:** Modular monolith, state-based persistence, lightweight CQRS for Reputation

---
# iBetcha -- Data Architecture
## Table of Contents

1. [Schema Design](#1-schema-design)
2. [Liquibase Migration Strategy](#2-liquibase-migration-strategy)
3. [Query Patterns](#3-query-patterns)
4. [Denormalization Strategy](#4-denormalization-strategy)
5. [Data Integrity and Constraints](#5-data-integrity-and-constraints)
6. [Performance Considerations](#6-performance-considerations)
7. [Security](#7-security)

---

## 1. Schema Design

### 1.1 Design Principles

- **Bounded context alignment:** Tables are logically grouped by DDD bounded context but share a single PostgreSQL schema (`public`) for MVP simplicity. Context ownership is documented per table. Cross-context joins are avoided in application code; the data engineer provides indexes that serve single-context queries.
- **UUID v7 primary keys:** All primary keys use `UUID` type with application-generated UUIDv7 (time-ordered). This provides natural chronological ordering for cursor-based pagination, prevents enumeration attacks, and avoids sequence contention under concurrent writes (ref: [IETF RFC 9562](https://www.rfc-editor.org/rfc/rfc9562)).
- **Timestamps in UTC:** All `TIMESTAMPTZ` columns store UTC. The application and client handle timezone conversion.
- **Enums as VARCHAR with CHECK constraints:** PostgreSQL custom `ENUM` types are problematic for schema evolution (adding values requires `ALTER TYPE`, which cannot be done inside a transaction pre-PG14 and is still restrictive). VARCHAR with CHECK constraints is safer for Liquibase-managed migrations. Trade-off: slightly more storage (varchar vs 4-byte enum OID), but negligible at our scale (<24 GB year 1).
- **No soft-delete columns on most tables:** Only the `users` table uses soft-delete (via `deletion_scheduled_at`). All other tables use hard delete or terminal states (RESOLVED, EXPIRED, CANCELLED). Rationale: soft-delete adds WHERE clauses to every query and complicates unique constraints. The bet state machine already captures terminal states.

### 1.2 Identity Context Tables

#### `users`

Owner: Identity Context

```sql
CREATE TABLE users (
    id                      UUID            PRIMARY KEY,
    email                   VARCHAR(255)    NOT NULL,
    username                VARCHAR(30)     NOT NULL,
    display_name            VARCHAR(50),
    bio                     VARCHAR(150),
    avatar_url              VARCHAR(512),
    password_hash           VARCHAR(72),    -- bcrypt output is 60 chars; 72 allows for future algos
    auth_provider           VARCHAR(20)     NOT NULL DEFAULT 'EMAIL',
    provider_id             VARCHAR(255),   -- OAuth provider's user ID
    email_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    onboarding_completed    BOOLEAN         NOT NULL DEFAULT FALSE,
    terms_accepted_at       TIMESTAMPTZ,
    account_status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    deletion_scheduled_at   TIMESTAMPTZ,    -- set when deletion requested; null = not pending
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT uq_users_username    UNIQUE (username),
    CONSTRAINT ck_users_username_format
        CHECK (username ~ '^[a-zA-Z0-9_.]{3,30}$'),
    CONSTRAINT ck_users_auth_provider
        CHECK (auth_provider IN ('EMAIL', 'GOOGLE', 'APPLE', 'SYSTEM')),
    CONSTRAINT ck_users_account_status
        CHECK (account_status IN ('ACTIVE', 'DELETION_PENDING', 'DELETED')),
    CONSTRAINT ck_users_bio_length
        CHECK (LENGTH(bio) <= 150),
    CONSTRAINT ck_users_password_or_oauth
        CHECK (
            (auth_provider = 'EMAIL' AND password_hash IS NOT NULL)
            OR (auth_provider IN ('GOOGLE', 'APPLE') AND provider_id IS NOT NULL)
            OR (auth_provider = 'SYSTEM')  -- sentinel/system accounts have neither password nor OAuth
        )
);
```

**Indexes:**

```sql
-- Username search: supports friend discovery (US-201)
-- btree is optimal for prefix matching with LIKE 'prefix%' when using C collation
CREATE INDEX idx_users_username_lower ON users (LOWER(username) varchar_pattern_ops);

-- OAuth lookup: find user by provider + provider_id during login
CREATE UNIQUE INDEX idx_users_provider_lookup ON users (auth_provider, provider_id)
    WHERE provider_id IS NOT NULL;

-- Account deletion job: find accounts past grace period
CREATE INDEX idx_users_deletion_scheduled ON users (deletion_scheduled_at)
    WHERE deletion_scheduled_at IS NOT NULL;

-- Email lookup for login
CREATE INDEX idx_users_email_lower ON users (LOWER(email));
```

**Index rationale:**
- `idx_users_username_lower`: The friend search (US-201) queries `WHERE LOWER(username) LIKE 'prefix%'`. `varchar_pattern_ops` enables btree prefix scan for LIKE patterns. Without this, PostgreSQL falls back to sequential scan.
- `idx_users_provider_lookup`: OAuth login looks up by `(auth_provider, provider_id)`. Partial index excludes email-only users.
- `idx_users_deletion_scheduled`: Scheduled job queries users with non-null `deletion_scheduled_at` past the 30-day grace period. Partial index keeps it small.

---

#### `refresh_tokens`

Owner: Identity Context

```sql
CREATE TABLE refresh_tokens (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jti             VARCHAR(64)     NOT NULL,
    token_family    VARCHAR(64)     NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked_at      TIMESTAMPTZ,    -- null = active; set = revoked
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti)
);
```

**Indexes:**

```sql
-- Token refresh: lookup by JTI during refresh flow
-- Already covered by the UNIQUE constraint on jti

-- Revocation: find all tokens in a family (for replay detection)
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (token_family);

-- Cleanup: delete expired tokens
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at)
    WHERE revoked_at IS NULL;

-- User logout: revoke all tokens for a user
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
```

**Index rationale:**
- `idx_refresh_tokens_family`: Refresh token rotation detection requires finding all tokens in the same family. If a revoked token is reused, all tokens in the family are revoked (ref: solution-architecture.md section 6.2).
- `idx_refresh_tokens_expires`: Cleanup job deletes expired tokens. Partial index excludes already-revoked tokens.

---

### 1.3 Social Context Tables

#### `friendships`

Owner: Social Context

Design: A single row represents the bidirectional friendship between two users. `user_id_lower` always holds the UUID that sorts first (lexicographic), ensuring uniqueness without duplicate rows.

```sql
CREATE TABLE friendships (
    id              UUID            PRIMARY KEY,
    user_id_lower   UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_higher  UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    requester_id    UUID            NOT NULL REFERENCES users(id),  -- who initiated
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_friendships_pair UNIQUE (user_id_lower, user_id_higher),
    CONSTRAINT ck_friendships_ordering
        CHECK (user_id_lower < user_id_higher),
    CONSTRAINT ck_friendships_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REMOVED')),
    CONSTRAINT ck_friendships_different_users
        CHECK (user_id_lower != user_id_higher)
);
```

**Indexes:**

```sql
-- My friends list: find all active friendships for a user (either side)
CREATE INDEX idx_friendships_lower_active ON friendships (user_id_lower, status)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_friendships_higher_active ON friendships (user_id_higher, status)
    WHERE status = 'ACTIVE';

-- Pending friend requests sent by a user (requester side)
CREATE INDEX idx_friendships_pending ON friendships (status, requester_id)
    WHERE status = 'PENDING';

-- Pending friend requests received by a user (addressee side)
-- Serves "show me my pending friend requests" which filters on the non-requester user.
-- Since the addressee is whichever of user_id_lower/user_id_higher is not the requester_id,
-- we add two partial indexes covering both sides for PENDING status.
CREATE INDEX idx_friendships_pending_lower ON friendships (user_id_lower)
    WHERE status = 'PENDING';
CREATE INDEX idx_friendships_pending_higher ON friendships (user_id_higher)
    WHERE status = 'PENDING';

-- Friendship check: are two users friends? (used by Wagering context on bet creation)
-- Covered by uq_friendships_pair (btree on user_id_lower, user_id_higher)
```

**Index rationale:**
- The friendship check query `WHERE (user_id_lower, user_id_higher) = (min(A,B), max(A,B)) AND status = 'ACTIVE'` is critical for bet creation validation (Wagering queries Social). The unique constraint index serves this.
- Two partial indexes for friend list queries: a user can appear in either `user_id_lower` or `user_id_higher`. Separate indexes allow index-only scans for each side, which PostgreSQL can UNION efficiently.
- `idx_friendships_pending_lower` / `idx_friendships_pending_higher`: The "show me my pending friend requests" query (received requests) filters on the addressee, not the requester. Since the addressee is whichever of `user_id_lower`/`user_id_higher` is NOT the `requester_id`, two partial pending indexes covering both columns serve this query efficiently. PostgreSQL can use a BitmapOr of both indexes to find all pending requests where the user appears on either side, then the application filters out rows where `requester_id = userId` to get only received requests.

---

#### `blocks`

Owner: Social Context

```sql
CREATE TABLE blocks (
    id              UUID            PRIMARY KEY,
    blocker_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_blocks_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT ck_blocks_not_self CHECK (blocker_id != blocked_id)
);
```

**Indexes:**

```sql
-- Check if user A has blocked user B (visibility check in search, bet creation)
-- Covered by uq_blocks_pair

-- Check if user B is blocked by anyone (for search result filtering)
CREATE INDEX idx_blocks_blocked ON blocks (blocked_id);
```

**Index rationale:**
- Friend search results must exclude users who have blocked the searcher. `idx_blocks_blocked` supports `WHERE blocked_id = ?` lookups for this exclusion filter.

---

#### `invite_links`

Owner: Social Context

Design: A link is shareable (e.g., posted to WhatsApp) and can be redeemed by multiple new users. Redemption tracking is not on the `invite_links` row itself; instead, the act of accepting a friend request after clicking the link constitutes the redemption. Links do not expire for MVP (DDD InviteLink invariant I2).

```sql
CREATE TABLE invite_links (
    id              UUID            PRIMARY KEY,
    inviter_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referral_code   VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_invite_links_code UNIQUE (referral_code)
);
```

**Indexes:**

```sql
-- Deep link resolution: lookup by referral code
-- Covered by uq_invite_links_code

-- Inviter's links (for profile/stats, low priority)
CREATE INDEX idx_invite_links_inviter ON invite_links (inviter_id);
```

**Multi-use rationale:** The original design had `redeemed_by` and `redeemed_at` columns, which limited each link to a single redemption. The intended use case is sharing a WhatsApp link that multiple friends can click. With the multi-use design, each new user who opens the invite link goes through the registration flow, and the system automatically sends a friend request from the inviter to the new user on account creation. If per-redemption tracking is needed in the future (e.g., to count how many signups a user generated), add an `invite_redemptions` table with `(invite_link_id, redeemed_by, redeemed_at)` as a separate concern.

---

### 1.4 Wagering Context Tables

#### `bets`

Owner: Wagering Context

```sql
CREATE TABLE bets (
    id                      UUID            PRIMARY KEY,
    creator_id              UUID            NOT NULL REFERENCES users(id),
    title                   VARCHAR(100),
    description             VARCHAR(500)    NOT NULL,
    stake                   VARCHAR(200)    NOT NULL,
    status                  VARCHAR(25)     NOT NULL DEFAULT 'PENDING_ACCEPTANCE',
    jury_id                 UUID            REFERENCES users(id),
    deadline                TIMESTAMPTZ,    -- optional completion deadline (informational)
    evidence_required       BOOLEAN         NOT NULL DEFAULT FALSE,
    winner_id               UUID            REFERENCES users(id),
    acceptance_deadline     TIMESTAMPTZ     NOT NULL,  -- created_at + 48h
    jury_deadline           TIMESTAMPTZ,               -- set when bet enters PENDING_JURY_VERDICT; created_at + 7d from claim
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at             TIMESTAMPTZ,

    CONSTRAINT ck_bets_status CHECK (status IN (
        'PENDING_ACCEPTANCE',
        'ACTIVE',
        'PENDING_JURY_VERDICT',
        'PENDING_APPROVAL',
        'RESOLVED',
        'DISPUTED',
        'EXPIRED',
        'CANCELLED'
    )),
    CONSTRAINT ck_bets_resolved_has_winner
        CHECK (
            (status = 'RESOLVED' AND winner_id IS NOT NULL AND resolved_at IS NOT NULL)
            OR (status != 'RESOLVED')
        ),
    CONSTRAINT ck_bets_description_length CHECK (LENGTH(description) BETWEEN 1 AND 500),
    CONSTRAINT ck_bets_stake_length CHECK (LENGTH(stake) BETWEEN 1 AND 200)
);
```

**Indexes:**

```sql
-- Home screen: user's active/pending bets (primary query, US-301, US-401)
-- This is the most important index in the system.
-- Query: SELECT b.* FROM bets b JOIN bet_participants bp ON b.id = bp.bet_id
--        WHERE bp.user_id = ? AND b.status IN (...) ORDER BY b.updated_at DESC
-- Strategy: index on bet_participants.user_id covers the join; index on bets.status covers the filter

CREATE INDEX idx_bets_status ON bets (status)
    WHERE status NOT IN ('RESOLVED', 'EXPIRED', 'CANCELLED');

-- Timeout job: find bets past acceptance deadline
CREATE INDEX idx_bets_acceptance_deadline ON bets (acceptance_deadline)
    WHERE status = 'PENDING_ACCEPTANCE';

-- Creator's bets (for cancel, management)
CREATE INDEX idx_bets_creator ON bets (creator_id, status);

-- Jury's pending reviews
CREATE INDEX idx_bets_jury_pending ON bets (jury_id)
    WHERE jury_id IS NOT NULL AND status IN ('PENDING_JURY_VERDICT');

-- Jury timeout scheduler: find bets in PENDING_JURY_VERDICT past the 7-day deadline
CREATE INDEX idx_bets_jury_deadline ON bets (jury_deadline)
    WHERE status = 'PENDING_JURY_VERDICT';

-- Resolved bets: for stats rebuild, history
CREATE INDEX idx_bets_resolved ON bets (resolved_at DESC)
    WHERE status = 'RESOLVED';

-- Updated_at for cursor pagination on home screen
CREATE INDEX idx_bets_updated ON bets (updated_at DESC);
```

**Index rationale:**
- `idx_bets_status`: Partial index excluding terminal states. Home screen queries filter by non-terminal status. This index is small (only active/pending bets) and fast.
- `idx_bets_acceptance_deadline`: The timeout scheduler queries `WHERE status = 'PENDING_ACCEPTANCE' AND acceptance_deadline < NOW()`. Partial index ensures only pending bets are scanned.
- `idx_bets_jury_pending`: Jury review screen queries pending verdicts for a specific jury user. Partial index keeps this extremely small.
- `idx_bets_jury_deadline`: The timeout scheduler queries `WHERE status = 'PENDING_JURY_VERDICT' AND jury_deadline < NOW()` every 60 seconds. This partial index (only rows in PENDING_JURY_VERDICT state) keeps the scan fast even at millions of bets. The `jury_deadline` column is set when the bet transitions to PENDING_JURY_VERDICT (calculated as `outcome_claim.created_at + 7 days`).

---

#### `bet_participants`

Owner: Wagering Context

```sql
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
```

**Indexes:**

```sql
-- Home screen: find all bets for a user (critical path, every app open)
CREATE INDEX idx_bp_user_status ON bet_participants (user_id, response_status);

-- Bet detail: load all participants for a bet
-- Covered by uq_bet_participants (bet_id, user_id) -- btree leading on bet_id

-- Count of pending/accepted for state transition checks
CREATE INDEX idx_bp_bet_response ON bet_participants (bet_id, response_status);
```

**Index rationale:**
- `idx_bp_user_status`: The home screen query starts by finding all bets where a user is a participant. This is the most frequent query in the system (every app open). Leading on `user_id` allows efficient lookup, and including `response_status` enables index-only scans for filtering.

---

#### `outcome_claims`

Owner: Wagering Context

```sql
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
```

**Indexes:**

```sql
-- Active claim for a bet (invariant I9: only one active claim at a time)
CREATE UNIQUE INDEX idx_oc_active_claim ON outcome_claims (bet_id)
    WHERE status = 'PENDING';

-- Claims by bet (history)
CREATE INDEX idx_oc_bet ON outcome_claims (bet_id, created_at DESC);
```

**Index rationale:**
- `idx_oc_active_claim`: This is a partial unique index enforcing invariant I9 at the database level. Only one row with `status = 'PENDING'` can exist per `bet_id`. This prevents concurrent claim creation race conditions that application-level checks alone cannot fully prevent.

---

#### `outcome_votes`

Owner: Wagering Context

```sql
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
```

**Indexes:**

```sql
-- Vote count per claim (majority calculation)
-- Covered by uq_ov_user_claim leading on claim_id

-- Votes by bet (for bet detail view)
CREATE INDEX idx_ov_bet ON outcome_votes (bet_id);
```

---

#### `bet_evidence`

Owner: Wagering Context

```sql
CREATE TABLE bet_evidence (
    id                  UUID            PRIMARY KEY,
    bet_id              UUID            NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
    uploaded_by         UUID            NOT NULL REFERENCES users(id),
    media_type          VARCHAR(10)     NOT NULL,
    s3_key              VARCHAR(512)    NOT NULL,
    file_size_bytes     BIGINT          NOT NULL,
    content_type        VARCHAR(50)     NOT NULL,
    thumbnail_s3_key    VARCHAR(512),   -- populated async by Lambda
    upload_confirmed    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_be_media_type CHECK (media_type IN ('PHOTO', 'VIDEO')),
    CONSTRAINT ck_be_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 52428800),
    CONSTRAINT ck_be_content_type CHECK (content_type IN (
        'image/jpeg', 'image/png', 'video/mp4'
    ))
);
```

**Indexes:**

```sql
-- Evidence for a bet (bet detail view)
CREATE INDEX idx_be_bet ON bet_evidence (bet_id) WHERE upload_confirmed = TRUE;
```

---

### 1.5 Reputation Context Tables

#### `user_stats`

Owner: Reputation Context

This is a **denormalized read model** updated atomically on bet resolution. See [Section 4: Denormalization Strategy](#4-denormalization-strategy) for update mechanics.

```sql
CREATE TABLE user_stats (
    user_id                 UUID            PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_bets              INTEGER         NOT NULL DEFAULT 0,
    wins                    INTEGER         NOT NULL DEFAULT 0,
    losses                  INTEGER         NOT NULL DEFAULT 0,
    current_streak_type     VARCHAR(4),     -- 'WIN' or 'LOSS' or null
    current_streak_count    INTEGER         NOT NULL DEFAULT 0,
    longest_win_streak      INTEGER         NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_us_streak_type CHECK (current_streak_type IN ('WIN', 'LOSS') OR current_streak_type IS NULL),
    CONSTRAINT ck_us_non_negative CHECK (wins >= 0 AND losses >= 0 AND total_bets >= 0),
    CONSTRAINT ck_us_total CHECK (total_bets = wins + losses)
);
```

**Indexes:**

No additional indexes needed. Primary key covers the only query pattern: `WHERE user_id = ?`.

---

#### `head_to_head_stats`

Owner: Reputation Context

```sql
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
```

**Indexes:**

```sql
-- Head-to-head lookup for a specific user (friend profile, bet acceptance screen)
CREATE INDEX idx_h2h_lower ON head_to_head_stats (user_id_lower);
CREATE INDEX idx_h2h_higher ON head_to_head_stats (user_id_higher);
```

**Index rationale:**
- A user's head-to-head records are queried from the friend profile view (US-204, Journey 6). Since the user can appear on either side, two single-column indexes allow efficient lookup. The query: `WHERE user_id_lower = ? OR user_id_higher = ?` can use a BitmapOr of both indexes.

---

### 1.6 Notification Context Tables

#### `notifications`

Owner: Notification Context

```sql
CREATE TABLE notifications (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(30)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    body            VARCHAR(500)    NOT NULL,
    bet_id          UUID,           -- optional FK, not enforced (bet may be deleted)
    payload_json    JSONB,          -- action buttons, deep link data
    delivery_status VARCHAR(15)     NOT NULL DEFAULT 'PENDING',
    fcm_message_id  VARCHAR(255),
    error_message   VARCHAR(500),
    attempt_count   SMALLINT        NOT NULL DEFAULT 0,
    read_at         TIMESTAMPTZ,    -- null = unread
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
```

**Indexes:**

```sql
-- Notification list for a user (notification screen, badge count)
CREATE INDEX idx_notif_user_created ON notifications (user_id, created_at DESC);

-- Unread count for badge
CREATE INDEX idx_notif_user_unread ON notifications (user_id)
    WHERE read_at IS NULL;

-- Failed notification retry/monitoring
CREATE INDEX idx_notif_failed ON notifications (delivery_status, created_at)
    WHERE delivery_status = 'FAILED';

-- Pending delivery (SQS consumer picking up work)
CREATE INDEX idx_notif_pending ON notifications (delivery_status, created_at)
    WHERE delivery_status = 'PENDING';
```

**Index rationale:**
- `idx_notif_user_created`: The notification list screen loads notifications for the current user, ordered by creation date. This composite index enables an index-only scan for pagination.
- `idx_notif_user_unread`: Badge count query `SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL`. Partial index is very small (only unread rows) and fast.
- `idx_notif_pending`/`idx_notif_failed`: Operational indexes for the notification delivery pipeline.

**Partitioning consideration (Phase 4, 500K+ users):** This table grows fastest (~32.4M rows/year at public launch). When row count exceeds ~50M, partition by `created_at` using monthly range partitions. This enables efficient partition pruning for the common `ORDER BY created_at DESC LIMIT 20` query and allows dropping old partitions for data retention.

---

#### `device_tokens`

Owner: Notification Context

```sql
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
```

**Indexes:**

```sql
-- Push notification dispatch: find all tokens for a user
-- Covered by uq_dt_user_device leading on user_id

-- Token cleanup: find by stale token value (when FCM reports UNREGISTERED)
CREATE INDEX idx_dt_token ON device_tokens (token);
```

---

### 1.7 Cross-Context Tables

#### `domain_events`

Owner: Shared (audit log / outbox pattern)

This table serves two purposes:
1. **Audit log:** Complete history of all state transitions for debugging and compliance.
2. **Outbox pattern:** Reliable domain event delivery to downstream consumers (Reputation, Notification). Events are written in the same transaction as the state change, then processed asynchronously by a poller or CDC mechanism.

```sql
CREATE TABLE domain_events (
    id              UUID            PRIMARY KEY,
    aggregate_type  VARCHAR(30)     NOT NULL,   -- 'BET', 'USER', 'FRIENDSHIP', 'BLOCK'
    aggregate_id    UUID            NOT NULL,
    event_type      VARCHAR(50)     NOT NULL,
    actor_id        UUID,                       -- user who triggered the event (null for system)
    payload         JSONB           NOT NULL,
    published       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_de_aggregate_type CHECK (aggregate_type IN (
        'BET', 'USER', 'FRIENDSHIP', 'BLOCK', 'INVITE_LINK', 'PLAYER_STATS'
    ))
);
```

**Indexes:**

```sql
-- Outbox polling: find unpublished events (processed by event relay)
CREATE INDEX idx_de_unpublished ON domain_events (created_at ASC)
    WHERE published = FALSE;

-- Audit trail: events for a specific aggregate (debugging, dispute investigation)
CREATE INDEX idx_de_aggregate ON domain_events (aggregate_type, aggregate_id, created_at DESC);

-- Actor audit: what did a specific user do? (GDPR data access, admin investigation)
CREATE INDEX idx_de_actor ON domain_events (actor_id, created_at DESC)
    WHERE actor_id IS NOT NULL;
```

**Index rationale:**
- `idx_de_unpublished`: The outbox poller runs every 1-5 seconds, querying `WHERE published = FALSE ORDER BY created_at ASC LIMIT 100`. This partial index only contains unpublished rows, making it tiny and fast. After events are published, the index shrinks automatically.
- `idx_de_aggregate`: For investigating a specific bet's history: `WHERE aggregate_type = 'BET' AND aggregate_id = ?`.
- `idx_de_actor`: For GDPR data access requests: "show me everything user X did."

**Partitioning consideration (Phase 4):** Partition by `created_at` monthly. Archive partitions older than 1 year to cold storage (pg_dump to S3). Keep recent partitions hot for audit queries.

---

### 1.8 Complete Table Summary

| # | Table | Context | Estimated Rows (Year 1, Public Launch) | Avg Row Size |
|---|-------|---------|----------------------------------------|-------------|
| 1 | `users` | Identity | 72,000 | 2 KB |
| 2 | `refresh_tokens` | Identity | ~200,000 (with cleanup) | 200 B |
| 3 | `friendships` | Social | 360,000 | 300 B |
| 4 | `blocks` | Social | ~5,000 | 100 B |
| 5 | `invite_links` | Social | ~50,000 | 200 B |
| 6 | `bets` | Wagering | 3,200,000 | 1 KB |
| 7 | `bet_participants` | Wagering | 8,100,000 | 200 B |
| 8 | `outcome_claims` | Wagering | ~2,000,000 | 200 B |
| 9 | `outcome_votes` | Wagering | ~3,000,000 | 150 B |
| 10 | `bet_evidence` | Wagering | ~640,000 | 300 B |
| 11 | `user_stats` | Reputation | 72,000 | 100 B |
| 12 | `head_to_head_stats` | Reputation | ~300,000 | 100 B |
| 13 | `notifications` | Notification | 32,400,000 | 500 B |
| 14 | `device_tokens` | Notification | ~100,000 | 300 B |
| 15 | `domain_events` | Shared | ~20,000,000 | 500 B |

**Total estimated database size at year 1:** ~24 GB (consistent with system-design.md estimates).

---

## 2. Liquibase Migration Strategy

### 2.1 Changelog Organization

Changesets are organized by bounded context, with a master changelog that includes context-specific changelogs in dependency order.

```
src/main/resources/db/changelog/
    db.changelog-master.yaml           -- root changelog
    001-identity/
        001-create-users.yaml
        002-create-refresh-tokens.yaml
    002-social/
        001-create-friendships.yaml
        002-create-blocks.yaml
        003-create-invite-links.yaml
    003-wagering/
        001-create-bets.yaml
        002-create-bet-participants.yaml
        003-create-outcome-claims.yaml
        004-create-outcome-votes.yaml
        005-create-bet-evidence.yaml
    004-reputation/
        001-create-user-stats.yaml
        002-create-head-to-head-stats.yaml
    005-notification/
        001-create-notifications.yaml
        002-create-device-tokens.yaml
    006-shared/
        001-create-domain-events.yaml
```

**Master changelog:**

```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/001-identity/
      relativeToChangelogFile: false
  - includeAll:
      path: db/changelog/002-social/
      relativeToChangelogFile: false
  - includeAll:
      path: db/changelog/003-wagering/
      relativeToChangelogFile: false
  - includeAll:
      path: db/changelog/004-reputation/
      relativeToChangelogFile: false
  - includeAll:
      path: db/changelog/005-notification/
      relativeToChangelogFile: false
  - includeAll:
      path: db/changelog/006-shared/
      relativeToChangelogFile: false
```

### 2.2 Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Changeset directory | `{NNN}-{context}/` | `003-wagering/` |
| Changeset file | `{NNN}-{action}-{table}.yaml` | `001-create-bets.yaml` |
| Changeset ID | `{context}-{NNN}-{description}` | `wagering-001-create-bets` |
| Author | Developer's name or `atlas` for generated | `atlas` |
| Post-MVP additions | `{context}/{NNN}-{description}.yaml` with next sequence | `003-wagering/006-add-bet-tags.yaml` |

### 2.3 Changeset Template

```yaml
databaseChangeLog:
  - changeSet:
      id: wagering-001-create-bets
      author: atlas
      preConditions:
        - onFail: MARK_RAN
        - not:
            tableExists:
              tableName: bets
      changes:
        - createTable:
            tableName: bets
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              # ... remaining columns
        - addCheckConstraint:
            tableName: bets
            constraintName: ck_bets_status
            condition: "status IN ('PENDING_ACCEPTANCE','ACTIVE','PENDING_JURY_VERDICT','PENDING_APPROVAL','RESOLVED','DISPUTED','EXPIRED','CANCELLED')"
        - createIndex:
            tableName: bets
            indexName: idx_bets_status
            columns:
              - column:
                  name: status
      rollback:
        - dropTable:
            tableName: bets
```

### 2.4 Rollback Strategy

| Scenario | Strategy |
|----------|----------|
| **New table** | `dropTable` rollback -- always safe |
| **New column** | `dropColumn` rollback -- safe if column has no dependencies |
| **Column type change** | Rollback restores original type. Test data compatibility before applying. |
| **New index** | `dropIndex` rollback |
| **Data migration** | Write a reverse migration SQL. Test on a copy of production data. |
| **Column removal** | Do NOT remove columns directly. Mark as deprecated (rename to `_deprecated_` prefix), deploy application code that ignores the column, then remove in a subsequent release. |

**Critical rule:** Every changeset MUST have a rollback block. Liquibase will reject changesets without rollback in CI (enforced by a custom Liquibase check).

### 2.5 Schema Evolution Strategy (Post-MVP)

1. **Additive changes only:** Adding columns, tables, and indexes are always backward-compatible. Prefer adding nullable columns with defaults over modifying existing columns.
2. **Expand-and-contract pattern:** For breaking changes (rename, type change, column removal):
   - **Expand:** Add new column/table alongside old one. Deploy code that writes to both.
   - **Migrate:** Backfill data from old to new.
   - **Contract:** Deploy code that reads from new only. Drop old in next release.
3. **Feature-gated migrations:** New feature tables are added in their own changeset file within the context directory, following the sequence numbering. The feature's application code checks for the table's existence or uses a feature flag.
4. **Blue-green deployment compatibility:** Migrations must be backward-compatible with the previous application version. The old application version must be able to run against the new schema (for rollback). This means: no column renames, no NOT NULL additions on existing columns without defaults, no type changes in a single step.

---

## 3. Query Patterns

### 3.1 Home Screen (Active Bets, Pending Bets)

**User story:** Every app open loads the home screen with the user's bets grouped by action needed.

```sql
-- Primary query: user's non-terminal bets, most recently updated first
SELECT b.id, b.title, b.description, b.stake, b.status,
       b.created_at, b.updated_at, b.acceptance_deadline,
       b.winner_id, b.jury_id
FROM bets b
JOIN bet_participants bp ON b.id = bp.bet_id
WHERE bp.user_id = :currentUserId
  AND bp.response_status != 'DECLINED'
  AND b.status NOT IN ('RESOLVED', 'EXPIRED', 'CANCELLED')
ORDER BY b.updated_at DESC
LIMIT 20;
```

**Performance path:** `idx_bp_user_status` -> `idx_bets_status` (partial, excludes terminal states).

**N+1 avoidance:** Load participants for all returned bets in a single batch query:

```sql
SELECT bp.bet_id, bp.user_id, bp.role, bp.response_status,
       u.display_name, u.avatar_url, u.username
FROM bet_participants bp
JOIN users u ON bp.user_id = u.id
WHERE bp.bet_id = ANY(:betIds);
```

**Application-level "pending action" enrichment:** For each bet, determine the user's pending action:
- `ACCEPT_DECLINE`: bet is `PENDING_ACCEPTANCE` and user's `response_status = 'PENDING'`
- `APPROVE_OUTCOME`: bet is `PENDING_APPROVAL` and user has not voted yet
- `JURY_REVIEW`: bet is `PENDING_JURY_VERDICT` and user is the jury
- `NONE`: no action required

This logic belongs in the application (BetService), not in SQL. It depends on the user's role and vote status, which is already loaded.

---

### 3.2 Friend's Profile (Stats, Head-to-Head, Bets)

**User story:** US-204, Journey 6. Viewing a friend's profile shows their stats, head-to-head with the viewer, and recent bets.

```sql
-- 1. Friend's overall stats (single row lookup)
SELECT * FROM user_stats WHERE user_id = :friendId;

-- 2. Head-to-head with viewer
SELECT * FROM head_to_head_stats
WHERE (user_id_lower, user_id_higher) = (LEAST(:viewerId, :friendId), GREATEST(:viewerId, :friendId));

-- 3. Friend's recent resolved bets (visible to friends)
SELECT b.id, b.description, b.status, b.winner_id, b.resolved_at
FROM bets b
JOIN bet_participants bp ON b.id = bp.bet_id
WHERE bp.user_id = :friendId
  AND b.status = 'RESOLVED'
ORDER BY b.resolved_at DESC
LIMIT 10;

-- 4. Friend's active bets (descriptions only, no stakes for privacy)
SELECT b.id, b.description, b.status
FROM bets b
JOIN bet_participants bp ON b.id = bp.bet_id
WHERE bp.user_id = :friendId
  AND b.status IN ('ACTIVE', 'PENDING_ACCEPTANCE')
ORDER BY b.created_at DESC
LIMIT 5;
```

**Performance notes:**
- Query 1: Primary key lookup on `user_stats`. O(1).
- Query 2: Unique index lookup on `head_to_head_stats`. O(1).
- Query 3 and 4: Use `idx_bp_user_status` to find the friend's bets, then join to `bets`. The bet index filters by status.

---

### 3.3 Bet Detail View

**User story:** US-401, Journey 4. Full bet detail with participants, evidence, votes.

```sql
-- 1. Bet details
SELECT * FROM bets WHERE id = :betId;

-- 2. Participants with user info
SELECT bp.*, u.display_name, u.avatar_url, u.username
FROM bet_participants bp
JOIN users u ON bp.user_id = u.id
WHERE bp.bet_id = :betId;

-- 3. Active outcome claim (if any)
SELECT * FROM outcome_claims
WHERE bet_id = :betId AND status = 'PENDING'
ORDER BY created_at DESC
LIMIT 1;

-- 4. Votes on active claim
SELECT ov.*, u.display_name
FROM outcome_votes ov
JOIN users u ON ov.user_id = u.id
WHERE ov.claim_id = :activeClaimId;

-- 5. Evidence
SELECT * FROM bet_evidence
WHERE bet_id = :betId AND upload_confirmed = TRUE;

-- 6. Head-to-head between viewer and primary opponent (for 2-person bets)
-- Same query as 3.2 query 2
```

**Performance:** All queries are primary key or indexed lookups. No sequential scans. Total: 5-6 queries, all O(1) or O(small N) with N = number of participants (max ~10).

---

### 3.4 Notification List

**User story:** Notification screen.

```sql
-- Paginated notification list
SELECT id, type, title, body, payload_json, read_at, created_at, bet_id
FROM notifications
WHERE user_id = :userId
ORDER BY created_at DESC
LIMIT 20
OFFSET 0;  -- or use cursor-based: WHERE created_at < :cursor

-- Unread badge count
SELECT COUNT(*) FROM notifications
WHERE user_id = :userId AND read_at IS NULL;
```

**Performance path:** `idx_notif_user_created` for the list, `idx_notif_user_unread` for the count.

**Cursor-based pagination** (preferred over OFFSET):

```sql
SELECT id, type, title, body, payload_json, read_at, created_at, bet_id
FROM notifications
WHERE user_id = :userId
  AND created_at < :cursorTimestamp
ORDER BY created_at DESC
LIMIT 20;
```

The cursor is an encoded `created_at` timestamp. UUIDv7 IDs provide tie-breaking if multiple notifications have the same timestamp (add `AND id < :cursorId` for exact cursor).

---

### 3.5 Friend Search

**User story:** US-201.

```sql
-- Search by username prefix (debounced, 300ms client-side)
SELECT u.id, u.username, u.display_name, u.avatar_url,
       CASE
           WHEN f.status = 'ACTIVE' THEN 'FRIENDS'
           WHEN f.status = 'PENDING' AND f.requester_id = :currentUserId THEN 'PENDING_SENT'
           WHEN f.status = 'PENDING' AND f.requester_id != :currentUserId THEN 'PENDING_RECEIVED'
           ELSE 'NONE'
       END AS friendship_status
FROM users u
LEFT JOIN friendships f ON (
    (f.user_id_lower = LEAST(u.id, :currentUserId) AND f.user_id_higher = GREATEST(u.id, :currentUserId))
    AND f.status IN ('ACTIVE', 'PENDING')
)
WHERE LOWER(u.username) LIKE LOWER(:query) || '%'
  AND u.id != :currentUserId
  AND u.account_status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM blocks b
      WHERE (b.blocker_id = u.id AND b.blocked_id = :currentUserId)
         OR (b.blocker_id = :currentUserId AND b.blocked_id = u.id)
  )
ORDER BY u.username
LIMIT 20;
```

**Performance path:**
- `idx_users_username_lower` with `varchar_pattern_ops` enables prefix scan.
- Block exclusion uses `idx_blocks_blocked` and `uq_blocks_pair`.
- Friendship status uses `uq_friendships_pair`.

**Attention:** The `NOT EXISTS` subquery for block checking could be slow if the blocks table is large. At our scale (~5,000 blocks), this is negligible. At 500K+ users, consider caching blocked user sets in Redis.

---

### 3.6 Stats Calculation (Win Rate, Streaks, Head-to-Head)

Stats are pre-computed in `user_stats` and `head_to_head_stats`. No aggregation queries needed at read time. See [Section 4](#4-denormalization-strategy).

**Rebuild query (if stats get out of sync):**

```sql
-- Rebuild user_stats from resolved bets
SELECT
    bp.user_id,
    COUNT(*) AS total_bets,
    COUNT(*) FILTER (WHERE b.winner_id = bp.user_id) AS wins,
    COUNT(*) FILTER (WHERE b.winner_id != bp.user_id) AS losses
FROM bets b
JOIN bet_participants bp ON b.id = bp.bet_id
WHERE b.status = 'RESOLVED'
  AND bp.response_status = 'ACCEPTED'
GROUP BY bp.user_id;

-- Rebuild head_to_head from resolved bets (for a specific user pair)
SELECT
    COUNT(*) AS total_bets,
    COUNT(*) FILTER (WHERE b.winner_id = :userA) AS user_a_wins,
    COUNT(*) FILTER (WHERE b.winner_id = :userB) AS user_b_wins
FROM bets b
JOIN bet_participants bpA ON b.id = bpA.bet_id AND bpA.user_id = :userA
JOIN bet_participants bpB ON b.id = bpB.bet_id AND bpB.user_id = :userB
WHERE b.status = 'RESOLVED';
```

**Note:** Streak calculation cannot be done in a single aggregation query; it requires ordered iteration over resolved bets. The rebuild process loads resolved bets ordered by `resolved_at` for each user and computes the streak programmatically.

---

### 3.7 Query Summary: Attention Zones

| Query | Complexity | Risk | Mitigation |
|-------|-----------|------|-----------|
| Home screen (bets for user) | JOIN + filter | N+1 trap if participants loaded per-bet | Batch-load participants for all bets in one query |
| Friend search with block filter | Subquery | Slow at scale if blocks table grows | Cache blocked user sets at 500K+ users |
| Stats rebuild | Full table scan + aggregation | Slow on large datasets | Run off-hours; use read replica at scale |
| Notification list | Simple indexed query | Large table growth | Partition by month at 50M+ rows |
| Bet timeout scan | Range scan on deadline | Must not miss deadlines | Partial index ensures small scan set |

---

## 4. Denormalization Strategy

### 4.1 What Is Denormalized

| Denormalized Table | Source Data | What It Stores |
|--------------------|------------|----------------|
| `user_stats` | `bets` (resolved) + `bet_participants` | Aggregate win/loss/streak per user |
| `head_to_head_stats` | `bets` (resolved) + `bet_participants` (pairs) | Win/loss between every pair of users who have bet against each other |

These tables exist because computing stats from normalized data requires scanning all resolved bets every time a profile is viewed. At 3.2M bets/year, this becomes expensive. Pre-computed stats make profile reads O(1).

### 4.2 When and How Stats Are Updated

**Trigger:** The `BetResolved` domain event.

**Update mechanism:** Synchronous, in the same database transaction as the bet resolution.

```
BetService.resolveBet(betId, winnerId):
  1. UPDATE bets SET status='RESOLVED', winner_id=:winnerId, resolved_at=NOW()
  2. Call PlayerStatsService.updateOnBetResolved(winnerId, loserIds)
     a. UPSERT user_stats for winner: wins++, total_bets++, update streak
     b. UPSERT user_stats for each loser: losses++, total_bets++, update streak
     c. UPSERT head_to_head_stats for every (winner, loser) pair
  3. INSERT domain_events (BetResolved event for outbox)
  4. COMMIT transaction
```

**Why synchronous (same transaction), not async?**

- **Consistency:** The user who just won a bet sees their updated stats immediately on the celebration screen (US-406). Eventual consistency would mean the celebration screen shows stale data, undermining the "bragging rights" core experience.
- **Simplicity:** No need for a separate event consumer, retry logic, or idempotency handling for stats updates.
- **Safety:** If the stats update fails, the entire transaction rolls back, including the bet resolution. The bet stays in its pre-resolution state. This is preferable to having a resolved bet with out-of-sync stats.
- **Performance:** The stats update is 2-3 UPSERTs (winner + losers + H2H pairs). For a typical 2-person bet, that is 4 UPSERTs. At ~9,000 bets resolved/day, this is ~36,000 UPSERTs/day or ~0.4/second. Negligible overhead.

**Trade-off acknowledged:** Synchronous stats update couples the Wagering and Reputation contexts at the database transaction level. This is an intentional pragmatic choice for MVP. If contexts are extracted to separate services later, stats updates would move to async event processing with eventual consistency.

### 4.3 UPSERT Strategy

```sql
-- Winner stats update
INSERT INTO user_stats (user_id, total_bets, wins, losses, current_streak_type, current_streak_count, longest_win_streak, updated_at)
VALUES (:winnerId, 1, 1, 0, 'WIN', 1, 1, NOW())
ON CONFLICT (user_id) DO UPDATE SET
    total_bets = user_stats.total_bets + 1,
    wins = user_stats.wins + 1,
    current_streak_type = 'WIN',
    current_streak_count = CASE
        WHEN user_stats.current_streak_type = 'WIN' THEN user_stats.current_streak_count + 1
        ELSE 1
    END,
    longest_win_streak = GREATEST(
        user_stats.longest_win_streak,
        CASE
            WHEN user_stats.current_streak_type = 'WIN' THEN user_stats.current_streak_count + 1
            ELSE 1
        END
    ),
    updated_at = NOW();

-- Head-to-head update (winner is user_id_lower)
INSERT INTO head_to_head_stats (id, user_id_lower, user_id_higher, lower_wins, higher_wins, total_bets, updated_at)
VALUES (:id, LEAST(:winnerId, :loserId), GREATEST(:winnerId, :loserId),
        CASE WHEN :winnerId < :loserId THEN 1 ELSE 0 END,
        CASE WHEN :winnerId > :loserId THEN 1 ELSE 0 END,
        1, NOW())
ON CONFLICT (user_id_lower, user_id_higher) DO UPDATE SET
    lower_wins = head_to_head_stats.lower_wins + CASE WHEN :winnerId < :loserId THEN 1 ELSE 0 END,
    higher_wins = head_to_head_stats.higher_wins + CASE WHEN :winnerId > :loserId THEN 1 ELSE 0 END,
    total_bets = head_to_head_stats.total_bets + 1,
    updated_at = NOW();
```

### 4.4 Consistency Guarantees

| Guarantee | Mechanism |
|-----------|-----------|
| Stats never diverge from resolved bets | Same-transaction update |
| Concurrent bet resolutions for the same user | PostgreSQL row-level locks on `user_stats` ensure serialized updates. UPSERT with `ON CONFLICT DO UPDATE` acquires a row lock on the conflicting row. |
| Streak accuracy | The streak computation in the UPSERT handles the current streak correctly for sequential resolutions. For concurrent resolutions of the same user (extremely rare -- two bets resolving in the same second), PostgreSQL's row lock serialization ensures correct ordering. |

### 4.5 Rebuild Strategy (If Stats Get Out of Sync)

A rebuild script can recompute all stats from source data. This is an operational procedure, not a regular process.

**When to rebuild:**
- Bug discovered in stats update logic
- Data migration after schema change
- After manual data correction in production

**Rebuild procedure:**

```sql
-- 1. Lock stats tables to prevent concurrent updates
BEGIN;
LOCK TABLE user_stats IN EXCLUSIVE MODE;
LOCK TABLE head_to_head_stats IN EXCLUSIVE MODE;

-- 2. Truncate and rebuild user_stats
TRUNCATE user_stats;
INSERT INTO user_stats (user_id, total_bets, wins, losses, current_streak_type, current_streak_count, longest_win_streak, updated_at)
SELECT
    bp.user_id,
    COUNT(*),
    COUNT(*) FILTER (WHERE b.winner_id = bp.user_id),
    COUNT(*) FILTER (WHERE b.winner_id != bp.user_id),
    NULL, 0, 0, -- streak requires programmatic computation
    NOW()
FROM bets b
JOIN bet_participants bp ON b.id = bp.bet_id
WHERE b.status = 'RESOLVED'
  AND bp.response_status = 'ACCEPTED'
GROUP BY bp.user_id;

-- 3. Streak computation: done programmatically (Java/Python script)
-- Load resolved bets per user ordered by resolved_at, compute streaks, update user_stats

-- 4. Truncate and rebuild head_to_head_stats
TRUNCATE head_to_head_stats;
INSERT INTO head_to_head_stats (id, user_id_lower, user_id_higher, lower_wins, higher_wins, total_bets, updated_at)
SELECT
    gen_random_uuid(),
    LEAST(bp1.user_id, bp2.user_id),
    GREATEST(bp1.user_id, bp2.user_id),
    COUNT(*) FILTER (WHERE b.winner_id = LEAST(bp1.user_id, bp2.user_id)),
    COUNT(*) FILTER (WHERE b.winner_id = GREATEST(bp1.user_id, bp2.user_id)),
    COUNT(*),
    NOW()
FROM bets b
JOIN bet_participants bp1 ON b.id = bp1.bet_id
JOIN bet_participants bp2 ON b.id = bp2.bet_id
WHERE b.status = 'RESOLVED'
  AND bp1.user_id < bp2.user_id
  AND bp1.response_status = 'ACCEPTED'
  AND bp2.response_status = 'ACCEPTED'
GROUP BY LEAST(bp1.user_id, bp2.user_id), GREATEST(bp1.user_id, bp2.user_id);

COMMIT;
```

**Rebuild time estimate:** At 3.2M resolved bets, the rebuild takes ~30 seconds on a db.t4g.small. Run during off-peak hours. At public launch, use a read replica for the rebuild computation and then apply the result to the primary.

---

## 5. Data Integrity and Constraints

### 5.1 Bet State Machine Enforcement

The bet state machine (8 states, 15+ transitions from ddd-architecture.md section 5) is enforced at **two levels**:

**Level 1: Database CHECK constraint on valid states**

The `ck_bets_status` constraint ensures only valid state values exist. This prevents bugs from writing an invalid state string.

**Level 2: Application-level transition validation**

State transitions are NOT enforced at the database level via triggers or additional constraints. Rationale:
- The transition rules are complex (depend on participant count, jury assignment, vote tallies). Encoding these in SQL triggers would be fragile, hard to test, and hard to evolve.
- The Bet aggregate in the application layer owns the state machine logic (ddd-architecture.md section 4.1.1). This is where invariants I1-I12 are enforced.
- The application uses optimistic locking (`@Version` in JPA / `version` column) to prevent concurrent state transitions:

```sql
ALTER TABLE bets ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
```

The JPA entity uses `@Version` on this column. Concurrent updates to the same bet result in an `OptimisticLockException`, which the application handles by retrying or returning a 409 conflict.

**Level 3: Database-level guard rails (defense in depth)**

```sql
-- Resolved bet must have a winner
CONSTRAINT ck_bets_resolved_has_winner CHECK (
    (status = 'RESOLVED' AND winner_id IS NOT NULL AND resolved_at IS NOT NULL)
    OR status != 'RESOLVED'
)

-- Only one active outcome claim per bet (invariant I9)
CREATE UNIQUE INDEX idx_oc_active_claim ON outcome_claims (bet_id)
    WHERE status = 'PENDING';
```

These database-level constraints act as safety nets. If a bug in the application layer bypasses the state machine, the database prevents data corruption.

### 5.2 Referential Integrity for the Friends Graph

| Relationship | FK Constraint | Cascade | Rationale |
|-------------|---------------|---------|-----------|
| Friendship -> User | `ON DELETE CASCADE` | If user deleted, friendships removed | GDPR compliance: no orphaned friendships |
| Block -> User | `ON DELETE CASCADE` | If user deleted, blocks removed | Clean deletion |
| Friendship uniqueness | `UNIQUE (user_id_lower, user_id_higher)` | N/A | Prevents duplicate friendships |
| Friendship ordering | `CHECK (user_id_lower < user_id_higher)` | N/A | Canonical ordering ensures uniqueness check works |

**Application-level integrity:**
- Before creating a bet, the application queries `friendships` to verify all participants are friends of the creator (invariant I1 context check).
- Before creating a bet, the application queries `blocks` to verify no participant-to-participant blocks exist.
- These checks are NOT enforced by foreign keys (there is no FK from `bet_participants.user_id` to `friendships`, because bet participants do not need to remain friends after the bet is created -- they only need to be friends at creation time).

### 5.3 Soft Delete vs Hard Delete

| Entity | Strategy | Rationale |
|--------|----------|-----------|
| Users | Soft delete (`account_status = 'DELETION_PENDING'`, `deletion_scheduled_at`) with hard delete after 30-day grace period | GDPR requires right to deletion with ability to cancel. 30-day grace allows re-login cancellation. |
| Friendships | Hard delete | No audit need. Removal is immediate and mutual. |
| Blocks | Hard delete on unblock | Block removal has no grace period. |
| Bets | Terminal states (`RESOLVED`, `EXPIRED`, `CANCELLED`) -- never deleted | Bet history is permanent. Even after user deletion, bets are anonymized (see 5.4), not deleted. |
| Notifications | Hard delete after 90 days (retention policy) | No business need for old notification records. |
| Domain events | Never deleted (audit log) | Compliance and debugging. Archive to cold storage after 1 year. |
| Refresh tokens | Hard delete on expiry or revocation | Security: stale tokens should not exist in the database. |

### 5.4 GDPR Account Deletion

When a user requests account deletion:

**Immediate (on deletion request):**
1. `users.account_status` set to `DELETION_PENDING`
2. `users.deletion_scheduled_at` set to `NOW() + 30 days`
3. All active bets where the user is a participant in `PENDING_ACCEPTANCE` status are cancelled (BetCancelled event emitted)
4. User can still log in during the 30-day grace period; logging in cancels the deletion

**After 30-day grace period (scheduled job):**

```sql
-- 1. Anonymize user in bet records (bets are kept for other participants' history)
UPDATE bet_participants SET user_id = '00000000-0000-0000-0000-000000000000'
    WHERE user_id = :deletedUserId;
UPDATE bets SET creator_id = '00000000-0000-0000-0000-000000000000'
    WHERE creator_id = :deletedUserId;
UPDATE bets SET jury_id = NULL WHERE jury_id = :deletedUserId;
UPDATE bets SET winner_id = '00000000-0000-0000-0000-000000000000'
    WHERE winner_id = :deletedUserId;
UPDATE outcome_claims SET claimant_id = '00000000-0000-0000-0000-000000000000'
    WHERE claimant_id = :deletedUserId;
UPDATE outcome_claims SET proposed_winner_id = '00000000-0000-0000-0000-000000000000'
    WHERE proposed_winner_id = :deletedUserId;
UPDATE outcome_votes SET user_id = '00000000-0000-0000-0000-000000000000'
    WHERE user_id = :deletedUserId;

-- 2. Delete all user-owned data (CASCADE handles most)
DELETE FROM users WHERE id = :deletedUserId;
-- CASCADE deletes: refresh_tokens, friendships, blocks, invite_links,
--                  user_stats, device_tokens, notifications, domain_events (actor)

-- 3. Delete evidence files from S3
-- Application code: list S3 objects with prefix evidence/{deletedUserId}/, delete all

-- 4. Delete avatar from S3
-- Application code: delete object at media/avatars/{deletedUserId}.*
```

**Sentinel UUID:** `00000000-0000-0000-0000-000000000000` represents `[Deleted User]` in the application layer. The `users` table contains a sentinel row for this UUID:

```sql
INSERT INTO users (id, email, username, display_name, auth_provider, email_verified, account_status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'deleted@ibetcha.internal', '_deleted_user_', '[Deleted User]', 'SYSTEM', TRUE, 'DELETED', NOW(), NOW());
-- auth_provider = 'SYSTEM' is exempt from the ck_users_password_or_oauth constraint (no password hash or OAuth needed)
```

**Trade-off:** Anonymization preserves bet history for other participants while removing all PII. The alternative (deleting all bets involving the user) would destroy other users' betting history and stats, which is not acceptable for the "bragging rights" core experience.

---

## 6. Performance Considerations

### 6.1 Connection Pooling (HikariCP)

| Parameter | MVP (1 task) | Public Launch (2-4 tasks) |
|-----------|-------------|--------------------------|
| `maximumPoolSize` | 10 | 20 |
| `minimumIdle` | 2 | 5 |
| `connectionTimeout` | 5000ms | 5000ms |
| `idleTimeout` | 300000ms (5 min) | 300000ms |
| `maxLifetime` | 1800000ms (30 min) | 1800000ms |
| `leakDetectionThreshold` | 30000ms (30s) | 15000ms (15s) |

**Total connections at public launch:** 4 tasks x 20 max = 80 connections. The db.t4g.small supports ~85 connections by default (`max_connections` derived from memory). This is a tight fit. Actions:
- Set `minimumIdle` to 5 (not all 20) to avoid unnecessarily holding connections
- Monitor `db.connection.pool.active` metric (from system-design.md section 6)
- If connection count exceeds 80%, introduce RDS Proxy or PgBouncer (scaling ladder Phase 3 at 200K users)

### 6.2 Indexes That Matter Most

Ranked by query frequency and impact:

| Rank | Index | Query It Serves | Frequency |
|------|-------|----------------|-----------|
| 1 | `idx_bp_user_status` | Home screen: user's bets | Every app open |
| 2 | `idx_bets_status` (partial) | Home screen: non-terminal bet filter | Every app open |
| 3 | `idx_notif_user_created` | Notification list | Every app open |
| 4 | `idx_notif_user_unread` | Badge count | Every app open |
| 5 | `idx_users_username_lower` | Friend search | Frequent |
| 6 | `uq_friendships_pair` | Friendship check on bet creation | Every bet creation |
| 7 | `idx_oc_active_claim` (unique partial) | Invariant enforcement | Every outcome claim |
| 8 | `idx_bets_acceptance_deadline` (partial) | Timeout scheduler | Every 60 seconds |
| 9 | `idx_de_unpublished` (partial) | Outbox event relay | Every 1-5 seconds |

### 6.3 N+1 Query Traps and Mitigations

| Trap | Where | Mitigation |
|------|-------|-----------|
| Loading participants per bet on home screen | `BetRepository.findByUser()` -> for each bet, load participants | Batch-load: `SELECT * FROM bet_participants WHERE bet_id IN (?)` using `@EntityGraph` or manual batch query |
| Loading user profile per participant in bet detail | For each participant, `SELECT * FROM users WHERE id = ?` | JOIN in the participant query: `SELECT bp.*, u.display_name, u.avatar_url FROM bet_participants bp JOIN users u ON bp.user_id = u.id WHERE bp.bet_id = ?` |
| Loading head-to-head per friend in friends list | For each friend, query `head_to_head_stats` | Batch-load: collect all friend IDs, query H2H stats in one query with `WHERE (user_id_lower, user_id_higher) IN (VALUES ...)` |
| Loading vote count per bet on home screen | For each bet in PENDING_APPROVAL, count votes | Include vote counts in the bet query using a subquery or batch-load votes |

**JPA strategy:** Use `@EntityGraph` annotations or `JOIN FETCH` in JPQL to prevent Hibernate's lazy-loading N+1 default:

```java
@EntityGraph(attributePaths = {"participants", "participants.user"})
List<Bet> findActiveBetsByUser(UUID userId);
```

### 6.4 Estimated Table Sizes

| Phase | Users | Bets | Bet Participants | Notifications | Domain Events | Total DB Size |
|-------|-------|------|-----------------|---------------|---------------|--------------|
| MVP (6 months) | 500 | 5,000 | 12,500 | 30,000 | 20,000 | <100 MB |
| Public Launch (1 year) | 72,000 | 3,200,000 | 8,100,000 | 32,400,000 | 20,000,000 | ~24 GB |
| Growth (2 years) | 200,000 | 8,000,000 | 20,000,000 | 80,000,000 | 50,000,000 | ~60 GB |

All sizes fit comfortably in RAM for the planned RDS instance (2 GB at public launch, expandable to 16 GB+ at Growth). PostgreSQL's buffer cache will serve most reads from memory.

### 6.5 Optimistic Locking

The `bets` table uses a `version` column for optimistic locking:

```sql
ALTER TABLE bets ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
```

This prevents lost updates when concurrent requests modify the same bet (e.g., two participants accepting simultaneously). Hibernate automatically increments the version and checks it on update:

```sql
UPDATE bets SET status = 'ACTIVE', version = version + 1, updated_at = NOW()
WHERE id = :betId AND version = :expectedVersion;
-- Returns 0 rows affected if version mismatch -> OptimisticLockException
```

---

## 7. Security

### 7.1 Column-Level Encryption

**Decision: No column-level encryption for MVP.**

Rationale:
- **At-rest encryption** is handled by RDS (AES-256, enabled by default per system-design.md). This encrypts the entire database volume, including all columns, indexes, logs, and backups.
- **In-transit encryption** is handled by TLS between the application and RDS (enforced via `sslmode=require` in the JDBC connection string).
- **Column-level encryption** (encrypting individual columns with application-managed keys) adds complexity: key management, inability to query encrypted columns, performance overhead on every read/write. The data in iBetcha (bet descriptions, stakes, usernames) is not classified as sensitive enough to warrant this. There are no credit card numbers, SSNs, or health records.
- **Password hashes** are stored using bcrypt, which is a one-way hash -- not encryption. This is appropriate and sufficient.

**Exception for future phases:** If Phase 3 introduces real-money payments (Stripe), payment-related data (last 4 digits of card, Stripe customer IDs) should use column-level encryption with AWS KMS-managed keys.

### 7.2 Row-Level Security Patterns

Row-level security is enforced at the **application layer**, not PostgreSQL RLS policies. Rationale: Spring Security's filter chain and per-endpoint authorization checks (solution-architecture.md section 6.4) provide the same guarantees. PostgreSQL RLS would require setting a session variable (`SET app.current_user_id = ?`) on every connection, which interacts poorly with connection pooling (HikariCP) and adds latency.

**Application-level enforcement:**

| Data | Visibility Rule | Implementation |
|------|----------------|----------------|
| User profile | Own profile: full access. Friend's profile: stats + public bets. Non-friend: 404. Blocked: 404. | `ProfileService` checks friendship status before returning data. Blocked users return 404 (not 403, to avoid revealing the block). |
| Bet detail | Participants, creator, jury, or friends of participants can view | `BetService.getBet()` checks membership. Returns 404 for unauthorized users. |
| Bet stakes (on friend profile) | Only bet description shown, not stakes | `ProfileService` strips `stake` field from bets shown on friend profiles |
| Notifications | Only own notifications | `NotificationService` always filters by `user_id = currentUser.id` |
| Evidence | Only bet participants and jury | `EvidenceService` generates CloudFront signed URLs only after verifying the requester is a participant or jury |
| Friend search results | Blocked users excluded | `UserSearchService` filters out blocked users via the `NOT EXISTS blocks` subquery |

### 7.3 SQL Injection Prevention

All database access goes through Spring Data JPA, which uses parameterized queries (PreparedStatement) by default. The following rules are enforced:

1. **No string concatenation in SQL:** All queries use JPA named parameters (`:paramName`), JPQL named parameters, or Spring Data derived query methods.
2. **No native SQL without `@Query` annotation:** Raw SQL is only used via `@Query(nativeQuery = true)` with named parameters.
3. **No dynamic ORDER BY from user input:** Sort columns are whitelisted in the application layer (enum-based), never passed as raw SQL.
4. **Input validation before persistence:** All DTOs validated with Jakarta Bean Validation annotations (`@Size`, `@Pattern`, `@NotBlank`). HTML tags stripped from free-text fields (description, stake, bio) using a sanitization utility.
5. **ArchUnit enforcement:** An ArchUnit test verifies that no class outside `repository` packages imports `java.sql.Connection`, `java.sql.Statement`, or `javax.persistence.EntityManager.createNativeQuery` without using parameterized queries.

### 7.4 Audit Logging Strategy

The `domain_events` table serves as the audit log. Every state-changing operation produces a domain event:

| Event Category | Events | Payload Contains |
|----------------|--------|-----------------|
| **Authentication** | `USER_REGISTERED`, `USER_LOGGED_IN`, `USER_LOGGED_OUT`, `PASSWORD_CHANGED`, `ACCOUNT_LOCKED` | User ID, IP address (hashed), auth provider, timestamp |
| **Account lifecycle** | `PROFILE_UPDATED`, `ACCOUNT_DELETION_INITIATED`, `ACCOUNT_DELETION_CANCELLED`, `ACCOUNT_PURGED` | User ID, changed fields (not values for PII), timestamp |
| **Bet lifecycle** | All 16 bet events from ddd-architecture.md Appendix B | Bet ID, actor ID, participants, state transition, timestamp |
| **Social** | `FRIEND_REQUEST_SENT`, `FRIEND_REQUEST_ACCEPTED`, `FRIEND_REQUEST_DECLINED`, `FRIEND_REMOVED`, `USER_BLOCKED`, `USER_UNBLOCKED` | Actor ID, target ID, timestamp |
| **Reputation** | `STATS_UPDATED`, `RIVALRY_MILESTONE_REACHED` | User ID, old values, new values |

**PII handling in audit logs:**
- User IDs (UUIDs) are stored -- these are pseudonymous identifiers, not PII themselves.
- IP addresses are hashed (SHA-256) before storage -- enables detecting patterns without storing raw IPs.
- Free-text fields (bet description, stake) are NOT stored in audit events -- they are accessible from the source table.
- On GDPR account deletion, domain events for the deleted user have their `actor_id` set to the sentinel UUID. The events themselves are retained (they are audit records for the system, not user data).

**Retention:**
- Domain events are retained indefinitely for audit compliance.
- At scale (Phase 4), events older than 1 year are archived to S3 (pg_dump per monthly partition) and dropped from the hot database.

### 7.5 Database Credentials and Access

| Concern | Implementation |
|---------|----------------|
| Credential storage | AWS Secrets Manager with auto-rotation (30-day cycle per system-design.md) |
| Application access | IAM role on Fargate task with `secretsmanager:GetSecretValue` permission scoped to specific secret ARNs |
| Developer access | No direct database access in production. Read-only access via a bastion host with MFA for debugging. |
| Connection encryption | `sslmode=require` in JDBC URL. RDS enforces TLS 1.2+ for all connections. |
| Database user privileges | Application user has `SELECT`, `INSERT`, `UPDATE`, `DELETE` on application tables only. No `CREATE`, `DROP`, `ALTER`. Schema migrations run under a separate Liquibase user with DDL privileges, used only during deployment. |

---

## Appendix A: Liquibase Changeset -- Complete MVP Schema

The following YAML changeset creates all 15 tables with constraints and indexes. This is the initial migration for MVP.

```yaml
databaseChangeLog:
  # ============================================================
  # Identity Context
  # ============================================================
  - changeSet:
      id: identity-001-create-users
      author: atlas
      changes:
        - sql:
            sql: |
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

              -- Sentinel row for deleted users (auth_provider = 'SYSTEM' bypasses ck_users_password_or_oauth)
              INSERT INTO users (id, email, username, display_name, auth_provider, email_verified, account_status, created_at, updated_at)
              VALUES ('00000000-0000-0000-0000-000000000000', 'deleted@ibetcha.internal', '_deleted_user_', '[Deleted User]', 'SYSTEM', TRUE, 'DELETED', NOW(), NOW());

              CREATE INDEX idx_users_username_lower ON users (LOWER(username) varchar_pattern_ops);
              CREATE UNIQUE INDEX idx_users_provider_lookup ON users (auth_provider, provider_id) WHERE provider_id IS NOT NULL;
              CREATE INDEX idx_users_deletion_scheduled ON users (deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL;
              CREATE INDEX idx_users_email_lower ON users (LOWER(email));
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS users CASCADE;

  - changeSet:
      id: identity-002-create-refresh-tokens
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS refresh_tokens CASCADE;

  # ============================================================
  # Social Context
  # ============================================================
  - changeSet:
      id: social-001-create-friendships
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS friendships CASCADE;

  - changeSet:
      id: social-002-create-blocks
      author: atlas
      changes:
        - sql:
            sql: |
              CREATE TABLE blocks (
                  id              UUID            PRIMARY KEY,
                  blocker_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                  blocked_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                  CONSTRAINT uq_blocks_pair UNIQUE (blocker_id, blocked_id),
                  CONSTRAINT ck_blocks_not_self CHECK (blocker_id != blocked_id)
              );

              CREATE INDEX idx_blocks_blocked ON blocks (blocked_id);
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS blocks CASCADE;

  - changeSet:
      id: social-003-create-invite-links
      author: atlas
      changes:
        - sql:
            sql: |
              CREATE TABLE invite_links (
                  id              UUID            PRIMARY KEY,
                  inviter_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                  referral_code   VARCHAR(20)     NOT NULL,
                  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                  CONSTRAINT uq_invite_links_code UNIQUE (referral_code)
              );
              -- Multi-use design: no redeemed_by/redeemed_at columns.
              -- A link can be shared (e.g. WhatsApp) and clicked by multiple new users.
              -- Redemption is tracked via the friendship created on registration, not here.

              CREATE INDEX idx_invite_links_inviter ON invite_links (inviter_id);
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS invite_links CASCADE;

  # ============================================================
  # Wagering Context
  # ============================================================
  - changeSet:
      id: wagering-001-create-bets
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS bets CASCADE;

  - changeSet:
      id: wagering-002-create-bet-participants
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS bet_participants CASCADE;

  - changeSet:
      id: wagering-003-create-outcome-claims
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS outcome_claims CASCADE;

  - changeSet:
      id: wagering-004-create-outcome-votes
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS outcome_votes CASCADE;

  - changeSet:
      id: wagering-005-create-bet-evidence
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS bet_evidence CASCADE;

  # ============================================================
  # Reputation Context
  # ============================================================
  - changeSet:
      id: reputation-001-create-user-stats
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS user_stats CASCADE;

  - changeSet:
      id: reputation-002-create-head-to-head-stats
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS head_to_head_stats CASCADE;

  # ============================================================
  # Notification Context
  # ============================================================
  - changeSet:
      id: notification-001-create-notifications
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS notifications CASCADE;

  - changeSet:
      id: notification-002-create-device-tokens
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS device_tokens CASCADE;

  # ============================================================
  # Shared (Audit / Outbox)
  # ============================================================
  - changeSet:
      id: shared-001-create-domain-events
      author: atlas
      changes:
        - sql:
            sql: |
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
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS domain_events CASCADE;
```

---

## Appendix B: Entity Relationship Diagram (Mermaid)

```mermaid
erDiagram
    users ||--o{ refresh_tokens : "has"
    users ||--o{ friendships : "participates (lower)"
    users ||--o{ friendships : "participates (higher)"
    users ||--o{ blocks : "blocks"
    users ||--o{ invite_links : "creates"
    users ||--o{ bet_participants : "participates"
    users ||--o| user_stats : "has stats"
    users ||--o{ device_tokens : "has devices"
    users ||--o{ notifications : "receives"

    bets ||--o{ bet_participants : "has"
    bets ||--o{ outcome_claims : "has"
    bets ||--o{ outcome_votes : "has"
    bets ||--o{ bet_evidence : "has"
    bets }o--|| users : "created by"
    bets }o--o| users : "jury"
    bets }o--o| users : "winner"

    outcome_claims ||--o{ outcome_votes : "has"
    outcome_claims }o--|| users : "claimant"
    outcome_claims }o--|| users : "proposed winner"

    head_to_head_stats }o--|| users : "user lower"
    head_to_head_stats }o--|| users : "user higher"

    users {
        UUID id PK
        VARCHAR email UK
        VARCHAR username UK
        VARCHAR display_name
        VARCHAR bio
        VARCHAR avatar_url
        VARCHAR password_hash
        VARCHAR auth_provider
        VARCHAR provider_id
        BOOLEAN email_verified
        VARCHAR account_status
        TIMESTAMPTZ deletion_scheduled_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    bets {
        UUID id PK
        UUID creator_id FK
        VARCHAR title
        VARCHAR description
        VARCHAR stake
        VARCHAR status
        UUID jury_id FK
        UUID winner_id FK
        TIMESTAMPTZ acceptance_deadline
        TIMESTAMPTZ deadline
        TIMESTAMPTZ resolved_at
        INTEGER version
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    bet_participants {
        UUID id PK
        UUID bet_id FK
        UUID user_id FK
        VARCHAR role
        VARCHAR response_status
        TIMESTAMPTZ responded_at
    }

    outcome_claims {
        UUID id PK
        UUID bet_id FK
        UUID claimant_id FK
        UUID proposed_winner_id FK
        BOOLEAN is_concession
        VARCHAR status
    }

    outcome_votes {
        UUID id PK
        UUID bet_id FK
        UUID claim_id FK
        UUID user_id FK
        VARCHAR vote
    }

    bet_evidence {
        UUID id PK
        UUID bet_id FK
        UUID uploaded_by FK
        VARCHAR media_type
        VARCHAR s3_key
        BIGINT file_size_bytes
        VARCHAR content_type
    }

    user_stats {
        UUID user_id PK_FK
        INTEGER total_bets
        INTEGER wins
        INTEGER losses
        VARCHAR current_streak_type
        INTEGER current_streak_count
        INTEGER longest_win_streak
    }

    head_to_head_stats {
        UUID id PK
        UUID user_id_lower FK
        UUID user_id_higher FK
        INTEGER lower_wins
        INTEGER higher_wins
        INTEGER total_bets
    }

    friendships {
        UUID id PK
        UUID user_id_lower FK
        UUID user_id_higher FK
        VARCHAR status
        UUID requester_id FK
        TIMESTAMPTZ accepted_at
    }

    blocks {
        UUID id PK
        UUID blocker_id FK
        UUID blocked_id FK
    }

    invite_links {
        UUID id PK
        UUID inviter_id FK
        VARCHAR referral_code UK
    }

    notifications {
        UUID id PK
        UUID user_id FK
        VARCHAR type
        VARCHAR title
        VARCHAR body
        UUID bet_id
        JSONB payload_json
        VARCHAR delivery_status
        TIMESTAMPTZ read_at
    }

    device_tokens {
        UUID id PK
        UUID user_id FK
        VARCHAR token
        VARCHAR platform
        VARCHAR device_id
    }

    refresh_tokens {
        UUID id PK
        UUID user_id FK
        VARCHAR jti UK
        VARCHAR token_family
        TIMESTAMPTZ expires_at
        TIMESTAMPTZ revoked_at
    }

    domain_events {
        UUID id PK
        VARCHAR aggregate_type
        UUID aggregate_id
        VARCHAR event_type
        UUID actor_id
        JSONB payload
        BOOLEAN published
    }
```

---

## Appendix C: Decision Log

| Decision | Choice | Trade-off | Revisit When |
|----------|--------|-----------|-------------|
| Enums as VARCHAR + CHECK vs PostgreSQL ENUM type | VARCHAR + CHECK | Slightly more storage, but easier to add values in migrations | Never -- this is standard practice for Liquibase-managed schemas |
| Stats updated synchronously vs async | Synchronous (same transaction) | Couples Wagering and Reputation at DB level, but guarantees immediate consistency | When extracting Reputation to a separate service |
| No PostgreSQL RLS | Application-level enforcement | Must implement access checks in every service method | When adding direct database access tools (analytics dashboards, admin panels) |
| No column-level encryption | Rely on RDS at-rest encryption | If data classification changes (payment data), add per-column encryption | Phase 3 (real-money payments) |
| Single schema (public) vs per-context schemas | Single schema | Simpler, but context boundaries are documentation-only at the DB level | When contexts are extracted to separate services with separate databases |
| Partial indexes for filtered queries | More indexes, but each is small and targeted | More DDL to manage, but better query performance and smaller index sizes | If index maintenance becomes a write bottleneck (unlikely at this scale) |
| Optimistic locking for bets | `version` column + JPA `@Version` | Retry logic needed for concurrent updates; occasional 409s for users | If contention becomes frequent (e.g., >1% of updates conflict) |
| Notifications table: no partitioning for MVP | Simple table, partition later | Table grows to 32M rows/year; OK for year 1, partition at 50M+ | 18 months post public launch |
