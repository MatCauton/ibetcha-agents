# Alignment Fixes Applied

**Date:** 2026-05-08
**Source of truth:** `docs/phase3-decisions.md`, `docs/ddd-architecture.md`, `docs/data-architecture.md`

All 8 cross-document alignment fixes have been applied. No design changes were made — these are documentation consistency corrections only.

---

## Fix 1 — State naming (canonical: DDD names)

**Files changed:** `docs/solution-architecture.md`

The solution-architecture.md used shortened, non-canonical state names (`PENDING`, `COMPLETING`, `JURY_REVIEW`, `CREATED`). These have been replaced throughout with the DDD canonical names (`PENDING_ACCEPTANCE`, `PENDING_APPROVAL`, `PENDING_JURY_VERDICT`). The transient `CREATED` state (which was never persisted and did not appear in the DB CHECK constraint) has been removed.

Specific locations updated:
- Section 4.3 state machine diagram and states table
- Section 4.3 transition rules table
- Section 5.4 bet creation response body (`"status": "PENDING"` → `"status": "PENDING_ACCEPTANCE"`)
- Section 5.4 bet list query param filter
- Section 5.4 bet cancel error message
- Section 5.4 accept response
- Section 5.4 complete response
- Section 5.4 vote response
- Section 5.4 appoint-jury response
- Section 6.4 authorization table
- Section 4.3 `pendingAction` field description

The data-architecture.md CHECK constraints already used the DDD names — no changes needed there.

---

## Fix 2 — System design doc: replace EventBridge with @Scheduled

**Files changed:** `docs/system-design.md`

The system-design.md recommended EventBridge Scheduler as the primary approach for bet timeout scheduling. `phase3-decisions.md` locks this as Spring `@Scheduled`. Updated:

- Section 2 architecture diagram: replaced "EventBridge Scheduler" box with "Spring @Scheduled with DB advisory lock"
- Section 5 stateless design table: updated the "Scheduled tasks" row to describe `@Scheduled` with leader election and acknowledge the non-stateless trade-off
- Section 7: completely rewrote from "EventBridge Scheduler (recommended)" to "Spring @Scheduled with DB advisory lock (primary)". The old EventBridge description is now under "Options Analysis (Historical Reference)" for context. The old "Fallback" section content (advisory lock code) is now the primary design.
- Section 8 MVP cost table: removed "EventBridge Scheduler" line item (~$0, was within free tier)
- Section 8 Public Launch cost table: removed "EventBridge Scheduler" line item (~$1/month)
- Section 8 cost breakdown: updated "Other" category
- Appendix A ADR-004: rewrote to record Spring @Scheduled as the accepted decision, superseding the earlier EventBridge draft
- Appendix B substrate probes: replaced "EventBridge Scheduler | CreateSchedule + DeleteSchedule" probe with "DB Advisory Lock (Scheduler) | pg_try_advisory_xact_lock" probe

---

## Fix 3 — Solution Architecture ER diagram alignment

**Files changed:** `docs/solution-architecture.md`

The ER diagram in Section 9.1 had two errors:
1. It showed a `friend_requests` table separate from `friendships`. The actual schema uses a single `friendships` table with status transitions (`PENDING` → `ACTIVE`).
2. It showed `declared_by` and `declared_at` on the `bets` table. These fields belong on the `outcome_claims` table.
3. Several tables were missing (`outcome_claims`, `domain_events`) and others had wrong column sets.

The entire ER diagram has been replaced with one matching the `data-architecture.md` canonical schema, including the correct `friendships` structure (with `user_id_lower`, `user_id_higher`, `requester_id`, `status`), the `outcome_claims` table (carrying `claimant_id`, `proposed_winner_id`, `declared_by`/`declared_at` semantics), and the new `domain_events` table. An explanatory note was added after the diagram.

Also corrected the infrastructure diagram in Section 10.1: `db.t4g.medium` → `db.t4g.micro` (matching system-design.md and phase3-decisions.md).

---

## Fix 4 — Sentinel user migration bug

**Files changed:** `docs/data-architecture.md`

The sentinel user INSERT used `auth_provider = 'EMAIL'` with no `password_hash`, which violates `ck_users_password_or_oauth`. Fixed by adding `'SYSTEM'` as a valid `auth_provider` value:

- `ck_users_auth_provider` CHECK constraint: added `'SYSTEM'` to the allowed values list (`'EMAIL', 'GOOGLE', 'APPLE', 'SYSTEM'`)
- `ck_users_password_or_oauth` CHECK constraint: updated to allow the `SYSTEM` case with neither password nor provider_id
- Section 5.4 sentinel INSERT: changed `auth_provider = 'EMAIL'` to `auth_provider = 'SYSTEM'` with an explanatory comment
- Appendix A Liquibase changeset: updated both the constraint definitions and the sentinel INSERT to match

---

## Fix 5 — Package structure note

**Files changed:** `docs/solution-architecture.md`

Added an explanatory note before the ArchUnit enforcement rules in Section 4.1 clarifying that `jury/` and `evidence/` are sub-packages of the wagering concern and must NOT write directly to bet state. All `bets` table mutations flow through `BetService`/`BetRepository` to protect the Bet aggregate's transactional invariant. A corresponding ArchUnit rule was added as rule #2.

---

## Fix 6 — Pending friendship index

**Files changed:** `docs/data-architecture.md`

The existing `idx_friendships_pending` index led on `(status, requester_id)`, serving "find pending requests I sent" but not "find pending requests sent to me" (which filters on `addressee_id` — whichever of `user_id_lower`/`user_id_higher` is not the `requester_id`).

Added two new partial indexes:
- `idx_friendships_pending_lower ON friendships (user_id_lower) WHERE status = 'PENDING'`
- `idx_friendships_pending_higher ON friendships (user_id_higher) WHERE status = 'PENDING'`

PostgreSQL can BitmapOr these two indexes to find all pending requests where the user appears on either side; the application then filters by `requester_id != userId` to isolate received requests. Updated the index rationale and the Liquibase changeset in Appendix A.

---

## Fix 7 — Jury timeout support

**Files changed:** `docs/data-architecture.md`

The scheduler needs to find bets in `PENDING_JURY_VERDICT` state past the 7-day deadline, but there was no column or index to support this efficiently.

Added:
- `jury_deadline TIMESTAMPTZ` (nullable) column to the `bets` table definition in Section 1.4. Set when the bet enters `PENDING_JURY_VERDICT` (calculated as `outcome_claim.created_at + 7 days`).
- `idx_bets_jury_deadline ON bets (jury_deadline) WHERE status = 'PENDING_JURY_VERDICT'` partial index
- Updated index rationale with explanation
- Updated Appendix A Liquibase changeset to include the new column and index

---

## Fix 8 — Invite link multi-use

**Files changed:** `docs/data-architecture.md`

The `invite_links` table had `redeemed_by UUID` and `redeemed_at TIMESTAMPTZ` columns, implying single-use redemption. The product intent is that a user shares one WhatsApp link that multiple new users can click.

Changed the table design to remove `redeemed_by` and `redeemed_at` columns. The table now has only `id`, `inviter_id`, `referral_code`, and `created_at`. An explanatory note was added describing the multi-use intent and noting that per-redemption tracking (if ever needed) should use a separate `invite_redemptions` table. Updated Appendix A Liquibase changeset accordingly.
