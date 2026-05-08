# Data Architecture Review

**Reviewer:** Claude (Automated Architecture Review)
**Date:** 2026-05-08
**Document:** `docs/data-architecture.md`
**Verdict:** PASS (with warnings)

The data architecture is thorough, well-reasoned, and production-ready for MVP. No blocking issues found. The schema aligns with DDD aggregates, the index strategy is sound, and the GDPR deletion flow is carefully designed. The warnings below are improvements to address before public launch.

---

## Critical Issues

None.

---

## Warnings

### W-1: GDPR anonymization breaks FK constraints (severity: high)

The deletion procedure (section 5.4) updates `bet_participants.user_id`, `bets.creator_id`, `bets.winner_id`, `outcome_claims.claimant_id`, `outcome_claims.proposed_winner_id`, and `outcome_votes.user_id` to the sentinel UUID `00000000-...` **before** deleting the user row. However, `bet_participants.user_id` has `REFERENCES users(id)` with no `ON DELETE` action (defaults to RESTRICT), not CASCADE. The same applies to `bets.creator_id`, `bets.winner_id`, `outcome_claims.claimant_id`, `outcome_claims.proposed_winner_id`, and `outcome_votes.user_id`. The anonymization UPDATE must run first, then the DELETE. This ordering is documented correctly, but if the sentinel user row `00000000-...` is ever accidentally deleted, all anonymized rows become FK-orphan violations and future inserts referencing those rows will fail. **Recommendation:** Add a database-level trigger or CHECK constraint preventing deletion of the sentinel row, or use `ON DELETE SET DEFAULT` with a default of the sentinel UUID on those columns.

### W-2: `bet_participants.user_id` has no ON DELETE action (severity: high)

`bet_participants` references `users(id)` without an `ON DELETE` clause (line 345). This means if a user row is somehow deleted without prior anonymization (e.g., a manual database operation), PostgreSQL will block the DELETE with a foreign key violation. This is technically safe (prevents data loss) but inconsistent with other tables that use `ON DELETE CASCADE`. Since the documented flow requires anonymization before deletion, this is intentional by design, but it should be **explicitly documented** as deliberate. Consider adding a comment in the schema and the Liquibase changeset explaining why no cascade exists here (to force anonymization-first).

### W-3: `invite_links` allows only single-use redemption (severity: medium)

The `invite_links` table has `redeemed_by UUID` and `redeemed_at TIMESTAMPTZ`, implying each link can only be redeemed once. The DDD doc says "Links do not expire for MVP" (InviteLink invariant I2), but the table structure limits each link to a single redeemer. If the product intent is that a user shares one link on WhatsApp and multiple friends can use it, the current schema does not support that. **Recommendation:** Clarify with product. If multi-use is needed, move `redeemed_by` to a separate `invite_redemptions` table. If single-use is confirmed, document it explicitly.

### W-4: `friendships` pending index may not serve the intended query (severity: medium)

`idx_friendships_pending` is defined as `(status, requester_id) WHERE status = 'PENDING'`. The typical query for "show me my pending friend requests" would filter by the *recipient*, not the requester. Since the recipient is either `user_id_lower` or `user_id_higher` (whichever is not the `requester_id`), this index does not directly support finding pending requests *for* a user. The application would need to query `WHERE status = 'PENDING' AND requester_id != :userId AND (user_id_lower = :userId OR user_id_higher = :userId)`, which would use the partial indexes `idx_friendships_lower_active` / `idx_friendships_higher_active` -- but those are filtered to `status = 'ACTIVE'`, not `'PENDING'`. **Recommendation:** Add partial indexes for pending status on both `user_id_lower` and `user_id_higher`, or change the pending index to lead on the user columns.

### W-5: `version` column inconsistency (severity: medium)

The `bets` table schema in section 1.4 (line 263-296) does **not** include the `version` column. It is introduced later in section 5.1 as an ALTER TABLE statement. However, the Liquibase changeset in Appendix A (line 1663) **does** include `version INTEGER NOT NULL DEFAULT 0` inline. This is an internal inconsistency in the document. The changeset is correct; the schema definition in section 1.4 should include `version` for completeness.

### W-6: No index for jury timeout scheduler (severity: medium)

The jury timeout scheduler needs to find bets that have been in `PENDING_JURY_VERDICT` for 7+ days. The document does not define a column or index to support this query efficiently. The `outcome_claims.created_at` column could serve as the proxy (the claim timestamp starts the 7-day clock), but there is no index on `outcome_claims` filtered by `status = 'PENDING'` combined with `created_at`. **Recommendation:** Add a `jury_deadline` column to `bets` (analogous to `acceptance_deadline`) or add an index on `outcome_claims(created_at) WHERE status = 'PENDING'` for the scheduler to scan.

### W-7: Notification cursor pagination with OFFSET fallback (severity: low)

Section 3.4 shows both OFFSET-based and cursor-based pagination for notifications. The cursor-based approach is correctly preferred and well-documented. However, the OFFSET-based query is shown first, which may lead developers to use it by default. **Recommendation:** Remove or explicitly deprecate the OFFSET example, or add a comment marking it as "not recommended."

### W-8: `domain_events.actor_id` not a FK (severity: low)

`domain_events.actor_id` is a plain UUID with no foreign key to `users`. This is intentional (system events have NULL actor, and events must survive user deletion). However, the GDPR deletion section states "domain events for the deleted user have their actor_id set to the sentinel UUID" -- this means the actor_id column would need to reference the sentinel row if validated. Current design is correct (no FK), but the idx_de_actor index will accumulate rows with the sentinel UUID over time. At scale, queries for a specific actor's events could return sentinel rows as noise. **Recommendation:** Document this as a known trade-off; consider filtering `WHERE actor_id != '00000000-...'` in GDPR data access queries.

---

## Observations (Non-Blocking)

### O-1: Head-to-head stats update for multi-party bets

The UPSERT in section 4.3 shows the H2H update for a single (winner, loser) pair. For a bet with N participants, the winner beats N-1 losers, creating N-1 H2H updates. But the DDD doc says "Update head-to-head for all participant pairs" (section 4.4.1, BetResolved action). The data architecture only updates (winner, loser) pairs, which is correct -- non-winning participants did not beat each other. The DDD doc wording is slightly misleading. This is consistent behavior but the DDD doc could be clarified.

### O-2: Streak calculation correctness under concurrent resolution

The synchronous stats update correctly handles sequential bet resolutions. For concurrent resolutions (two different bets involving the same user resolving simultaneously), PostgreSQL row locks serialize the UPSERTs. However, the streak logic depends on the order of resolution. If bet A (win) and bet B (loss) resolve concurrently, the final streak depends on which lock is acquired first. This is acknowledged in section 4.4 and is acceptable -- the probability is negligible and the operational impact is trivial (streak off by one temporarily, corrected on next resolution).

### O-3: Sentinel user violates `ck_users_password_or_oauth`

The sentinel user INSERT (line 1534) uses `auth_provider = 'EMAIL'` but provides no `password_hash`. The CHECK constraint `ck_users_password_or_oauth` requires `password_hash IS NOT NULL` when `auth_provider = 'EMAIL'`. This INSERT will fail. **Recommendation:** Either set the sentinel's `auth_provider` to a special value (add `'SYSTEM'` to the CHECK constraint) or provide a dummy `password_hash`.

### O-4: Strong use of partial indexes

The index strategy makes excellent use of PostgreSQL partial indexes (e.g., `idx_bets_acceptance_deadline WHERE status = 'PENDING_ACCEPTANCE'`, `idx_oc_active_claim WHERE status = 'PENDING'`). These keep index sizes small and maintenance overhead low. The unique partial index on `outcome_claims` to enforce invariant I9 at the database level is a particularly good defensive design.

### O-5: DDD alignment is strong

Tables map cleanly to DDD aggregates:
- `bets` + `bet_participants` + `outcome_claims` + `outcome_votes` + `bet_evidence` = Bet aggregate
- `users` = User aggregate
- `friendships` = Friendship aggregate
- `blocks` = Block aggregate
- `invite_links` = InviteLink aggregate
- `user_stats` + `head_to_head_stats` = PlayerStats aggregate
- `notifications` = Notification aggregate
- `device_tokens` = DeviceToken value object (managed separately, per DDD doc)
- `domain_events` = Shared outbox/audit (cross-context, as documented)

No cross-context join queries are documented. Context boundaries are respected.

### O-6: Liquibase strategy is sound

The changelog organization by bounded context with dependency-ordered includes is correct. The use of `includeAll` with directory-per-context and sequential numbering is clean. The rollback strategy, expand-and-contract pattern, and blue-green compatibility rules are all well-considered. One minor note: using raw SQL changesets (`- sql:`) instead of Liquibase's cross-platform DDL tags is a pragmatic choice (PostgreSQL-specific features like partial indexes, CHECK constraints with regex, and `varchar_pattern_ops` require raw SQL), but it means the schema is not portable to other databases. This is acceptable given the PostgreSQL lock-in decision.

### O-7: `bets` table missing `ON DELETE` for `creator_id`, `jury_id`, `winner_id`

Similar to W-2, the `bets.creator_id`, `bets.jury_id`, and `bets.winner_id` FK references to `users(id)` have no explicit `ON DELETE` clause. The anonymization-first approach makes this safe in practice, but for `jury_id`, the current approach sets it to NULL during deletion (`UPDATE bets SET jury_id = NULL WHERE jury_id = :deletedUserId`), which is semantically correct (jury is optional). Adding `ON DELETE SET NULL` for `jury_id` would provide an additional safety net.

---

## Summary

| Category | Assessment |
|----------|-----------|
| Schema correctness | Good. Data types appropriate. O-3 (sentinel violates CHECK) needs fixing before first migration runs. |
| Index strategy | Excellent. Partial indexes are well-targeted. W-4 (pending friendships) and W-6 (jury timeout) need attention. |
| Denormalization | Sound. Synchronous update in same transaction is the right MVP trade-off. Rebuild procedure is documented. |
| DDD alignment | Strong. Clean mapping from aggregates to tables. No cross-context leakage. |
| GDPR compliance | Good design with sentinel approach. W-1 (sentinel protection) and W-2 (explicit FK documentation) should be addressed. |
| Query patterns | Well-documented with N+1 mitigations. W-7 (OFFSET deprecation) is minor. |
| Liquibase strategy | Sound. Context-organized, rollback-mandatory, expand-and-contract for evolution. |

**Overall: PASS.** Address O-3 before running the initial migration (it will fail). Address W-1, W-4, and W-6 before public launch.
