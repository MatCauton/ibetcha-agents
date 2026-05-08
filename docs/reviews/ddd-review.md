# DDD Architecture Review

**Document:** `docs/ddd-architecture.md`
**Reviewer:** DDD Reviewer (Opus 4.6)
**Date:** 2026-05-08
**Verdict:** PASS (with warnings)

No critical blockers found. The DDD architecture is thorough, well-reasoned, and aligned with the locked product and architecture decisions. The issues below are warnings and observations that should be addressed before development begins but do not block handoff.

---

## Critical Issues (Blockers)

None.

---

## Warnings (Should Fix Before Development)

### W1. State Name Divergence Between DDD and Solution Architecture

The DDD doc uses one set of state names; the Solution Architecture uses different names for the same concepts:

| DDD Architecture | Solution Architecture | Data Architecture |
|---|---|---|
| PENDING_ACCEPTANCE | PENDING (+ transient CREATED) | PENDING_ACCEPTANCE |
| PENDING_JURY_VERDICT | JURY_REVIEW | PENDING_JURY_VERDICT |
| PENDING_APPROVAL | COMPLETING | PENDING_APPROVAL |

The Solution Architecture introduces a transient `CREATED` state ("not yet sent, only during creation") that does not appear in the DDD doc or the database CHECK constraint. It also renames three states using shorter names.

The Data Architecture's CHECK constraint follows the DDD naming, which is correct since the DDD doc is the domain authority.

**Impact:** If developers implement the state machine from the Solution Architecture's names, the enum values will not match the database constraints. This will cause runtime errors.

**Recommendation:** Align the Solution Architecture to use the DDD state names exactly. Remove the transient `CREATED` state from the Solution Architecture (it is an implementation detail of the CreateBet command handler, not a persisted state). The DDD doc's naming is more precise and descriptive -- keep it as the source of truth.

### W2. Synchronous Stats Update Contradicts CQRS Design

Phase 3 decisions state: "Stats updates: Synchronous (same transaction as bet resolution)." The Data Architecture confirms this with an explicit code flow showing `BetService.resolveBet()` calling `PlayerStatsService.updateOnBetResolved()` in the same transaction.

However, the DDD doc describes Reputation as a separate bounded context that "consumes BetResolved events from the Wagering Context" and explicitly calls this "lightweight CQRS" with the write model in Wagering and the read model in Reputation. The context map shows `BetResolved` flowing to Reputation via "Published Language" (async).

Synchronous, same-transaction updates across context boundaries violate the bounded context pattern. If Wagering directly calls Reputation's service within its transaction, the boundary is not real -- Reputation is just a package within Wagering.

**Impact:** Not a functional problem for MVP (it works), but it undermines the architectural integrity and makes future extraction harder.

**Recommendation:** Accept the pragmatic choice (synchronous in-process call for MVP) but document it explicitly as a conscious boundary violation. Use Spring's `@TransactionalEventListener` with `AFTER_COMMIT` phase instead of a direct service call. This keeps the Wagering transaction independent and lets Reputation react to the event in a separate transaction, while still being synchronous in-process. This preserves the boundary at minimal cost.

### W3. Decline After Partial Acceptance Creates Ambiguity in Multi-Party Bets

The DDD doc states: "If declines reduce participants below 2, bet auto-expires" (I12). The state machine handles `PENDING_ACCEPTANCE -> EXPIRED` on decline with <2 remaining.

However, the document does not address: what happens if a 4-person bet has 2 acceptances and 1 decline, leaving 3 people (2 accepted + 1 pending)? The bet stays in PENDING_ACCEPTANCE. But what if the last pending person also declines? Now 2 accepted participants remain (>=2), so the bet should become ACTIVE per I4. But the transition rule says "all remaining participants must have ACCEPTED status." This seems correct: with 2 accepted and 0 pending, the bet should activate.

The edge case is: a 3-person bet where 1 accepts and 1 declines. Now 2 remain (creator + 1 accepted). The creator is implicitly accepted. All pending participants have responded. Should the bet activate? The DDD doc says "all (non-declined) participants have accepted" triggers ACTIVE. The creator counts as accepted. So yes, it should activate.

**Impact:** This works correctly under the stated rules but developers may be confused about whether a decline can trigger activation. The event storming flows do not show this path.

**Recommendation:** Add a paragraph to section 5.3 ("Transition: PENDING_ACCEPTANCE -> ACTIVE") explicitly stating: "A decline can indirectly trigger activation if the declining participant was the last pending one and at least 2 participants (including the creator) have accepted." Add a DeclineBet -> ACTIVE transition to the state diagram.

### W4. Missing PENDING_ACCEPTANCE -> EXPIRED Path for Decline in State Diagram

The state diagram in section 5.2 shows `PENDING_ACCEPTANCE -> EXPIRED` only for `DeclineBet (< 2 remain)` and `48h Timeout`. The transition rules are correct in the text, but the Mermaid diagram uses the label "DeclineBet (< 2 remain)" which is correct. However, combining this with W3, the decline path should show three possible outcomes: stay in PENDING_ACCEPTANCE, move to EXPIRED, or move to ACTIVE. The diagram only shows the first two.

**Recommendation:** Add a transition line from PENDING_ACCEPTANCE to ACTIVE labeled "DeclineBet (all remaining accepted, >= 2 remain)" to the Mermaid diagram.

### W5. Package Structure Mismatch Between DDD and Solution Architecture

The DDD doc recommends this package structure:
```
com.ibetcha.wagering.domain.model
com.ibetcha.wagering.application
com.ibetcha.identity.domain
com.ibetcha.social.domain
com.ibetcha.reputation.domain
com.ibetcha.notification.domain
```

The Solution Architecture uses a different, flatter structure:
```
com.ibetcha.bet
com.ibetcha.auth
com.ibetcha.user
com.ibetcha.friend
com.ibetcha.jury
com.ibetcha.evidence
com.ibetcha.notification
com.ibetcha.wincard
com.ibetcha.scheduler
```

Key differences:
- DDD uses bounded context names (`wagering`, `social`, `identity`, `reputation`). Solution uses feature names (`bet`, `friend`, `auth`, `user`).
- DDD groups all wagering concepts (bet, jury, evidence, outcome claims) under `com.ibetcha.wagering`. Solution splits them into separate top-level modules (`bet`, `jury`, `evidence`).
- Solution's ArchUnit rule says "`bet` module may depend on `jury`, `evidence`, `wincard`." In the DDD model, these are all part of the same Wagering context and should share a consistency boundary.

**Impact:** Splitting jury and evidence into separate top-level modules with separate repositories risks breaking the Bet aggregate's transactional invariants. If `jury` has its own repository writing to the same bet row, two codepaths can modify bet state independently.

**Recommendation:** Adopt the DDD package structure. Jury operations, evidence metadata, and outcome claims are all part of the Bet aggregate and should live under `com.ibetcha.wagering`. The Solution Architecture's `jury` and `evidence` modules should be sub-packages within the wagering context, not separate top-level modules. Win Card generation can reasonably live in Reputation since it depends on stats data.

---

## Observations (Non-Blocking, For Consideration)

### O1. Friendship as Aggregate Root May Be Overly Granular

The Social Context has three aggregates: Friendship, Block, and InviteLink. All are small and justified. However, Friendship's invariant I4 ("Cannot send a friend request to a blocked user") requires querying the Block aggregate within the Friendship creation flow. Since both are in the same context, this is a cross-aggregate validation within a single bounded context.

This is fine for MVP. If performance becomes a concern, consider merging Block checking into the Friendship command handler as a domain service query rather than cross-aggregate communication.

### O2. PlayerStats Aggregate Terminology Mismatch

The DDD doc calls the Reputation aggregate `PlayerStats`, but across all other documents (solution architecture, data architecture, user stories), the concept is referenced as "user stats," "profile stats," or "win/loss record." The data table is named `user_stats`, not `player_stats`.

**Recommendation:** Rename the aggregate to `UserStats` for consistency with the ubiquitous language used everywhere else. "Player" is not in the glossary; "User" is.

### O3. Bet Aggregate Does Not Track Multi-Party Losers in BetResolved Event

The `BetResolved` event payload includes `winnerId, loserId(s)`. For multi-party bets, all non-winner participants are losers. This is implicitly derived from `participantIds - winnerId`.

The data model stores `winner_id` on the `bets` table but does not store loser IDs (they are derived from `bet_participants WHERE bet_id = ? AND user_id != winner_id`). This is correct and avoids redundancy, but the `BetResolved` event should carry the full loser list so downstream consumers (Reputation) do not need to query back.

The DDD doc's event payload already specifies `loserId(s)`, so this is correctly handled. No action needed.

### O4. No Explicit Domain Event for "Bet Activated After Decline"

If a decline triggers activation (W3), the system should emit both `BetDeclined` and `BetActivated` events. The command table for `DeclineBet` only lists `BetDeclined, (BetExpired if too few remain)`. It does not list `BetActivated` as a possible event.

**Recommendation:** Add `(BetActivated if decline leaves all remaining accepted and >= 2 remain)` to the DeclineBet command's events column.

### O5. ES/CQRS Assessment Is Sound

The recommendation of "no ES, lightweight CQRS for Reputation only" is well-reasoned and appropriate for MVP. The arguments are balanced, the risks are acknowledged, and the migration path is clear. The bet audit log via `domain_events` table provides the audit trail without ES overhead. Agreed with the assessment.

### O6. Head-to-Head Scaling in PlayerStats

The DDD doc notes that HeadToHeadRecord "could grow large for very active users" but dismisses it as "5-20 opponents." The Data Architecture separates head-to-head into its own table (`head_to_head_stats`), which is the correct approach. The DDD doc's description of HeadToHeadRecord as a "child value object collection" within PlayerStats is misleading -- in implementation, it is a separate table with its own lifecycle.

**Recommendation:** Update the DDD doc to note that HeadToHeadRecord, while conceptually part of the Reputation context, is stored in a separate table and queried independently. It is not loaded as part of the PlayerStats aggregate's consistency boundary.

### O7. Notification Aggregate Is Unusual

The Notification "aggregate" is essentially a write-once entity with a delivery status lifecycle. It has no real invariants to protect (I1 is an SLA, I2 is a validation rule). This is more of a record/log than a true aggregate.

This is fine -- not every entity in a system needs to be a rich aggregate. The DDD doc correctly identifies it as a generic subdomain. No change needed, but developers should understand this is a thin aggregate.

### O8. Dispute Resolution for 3+ Person Bets Needs Clarification

The DDD doc says for 3+ person bets with a split vote: "the vote stays open" and "participants can change their vote." However, the VoteOnOutcome command does not mention vote changing. The `outcome_votes` table has a unique constraint on `(claim_id, user_id)`, which prevents duplicate votes but does not support vote changes (would require an UPDATE, not an INSERT).

**Recommendation:** Either (a) explicitly allow vote changes and document the mechanism (UPDATE existing vote), or (b) state that votes are final and the bet stays in PENDING_APPROVAL until enough participants vote. Option (b) is simpler for MVP. Add clarification to the state machine section.

---

## Cross-Document Alignment Summary

| Aspect | DDD <-> Solution Arch | DDD <-> Data Arch | Verdict |
|---|---|---|---|
| Bounded contexts <-> Modules | Partial mismatch (W5) | Aligned | Fix W5 |
| State machine states | Name mismatch (W1) | Aligned | Fix W1 |
| State machine transitions | Semantically aligned | Aligned | OK |
| Aggregate <-> Table mapping | Aligned (7 aggregates, 15 tables) | Aligned | OK |
| Event names | Aligned | Aligned | OK |
| Domain event delivery | DDD says async, Phase 3 says sync (W2) | Data says sync | Clarify W2 |
| Ubiquitous language | "PlayerStats" vs "user_stats" (O2) | "user_stats" table | Rename O2 |

---

## Final Assessment

The DDD architecture is strong. The bounded context boundaries are well-chosen and justified. The Bet aggregate's size is correctly defended by transactional invariants. The state machine is comprehensive and covers the user stories. The ES/CQRS recommendation is sound.

The main risk is the naming and structural divergence between the DDD doc and the Solution Architecture (W1, W5). These should be reconciled before development begins to prevent the implementation from drifting from the domain model. The synchronous stats update (W2) is a pragmatic MVP choice that should be explicitly acknowledged as a boundary violation.

**Verdict: PASS** -- ready for development handoff after addressing W1 and W5.
