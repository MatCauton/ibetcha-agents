# Solution Architecture Review -- iBetcha

**Reviewer:** Architecture Review Agent
**Date:** 2026-05-08
**Document Reviewed:** `docs/solution-architecture.md`
**Cross-referenced:** `phase1-decisions.md`, `phase3-decisions.md`, `ddd-architecture.md`, `data-architecture.md`, `system-design.md`

---

## Verdict: PASS (conditional)

The solution architecture is thorough, well-structured, and production-ready for MVP. The tech stack is coherent, the API design is comprehensive, and security posture is solid. There are no blockers, but several issues must be resolved before development begins to avoid rework.

---

## Critical Issues (must fix before development)

### C1. Timeout scheduling: contradicts phase-3 decision

The **system-design.md** recommends EventBridge Scheduler for bet timeouts. The **phase3-decisions.md** explicitly locks the decision as **Spring @Scheduled** ("Choice: Spring @Scheduled over EventBridge Scheduler"). However, the **solution-architecture.md** container diagram (Section 2, Level 2) shows `Scheduled Jobs` as `Spring Boot @Scheduled` running within the API Server process, and Section 5 of system-design.md still presents EventBridge as "recommended."

**The phase-3 decision is the authority.** The system-design.md Section 7 recommendation for EventBridge Scheduler is now overridden. The solution architecture aligns with the locked decision (Spring @Scheduled), but system-design.md should be updated to mark the EventBridge recommendation as superseded. The stateless backend design table in system-design.md Section 5 also says "Scheduled tasks: Externalized to EventBridge Scheduler. Not @Scheduled in-process" -- this directly contradicts the locked decision and must be corrected.

**Action:** Update system-design.md Section 5 (Stateless Backend Design) and Section 7 to reflect the locked Spring @Scheduled decision, and note the leader-election trade-off for multi-instance scaling.

### C2. Bet state naming inconsistency between solution architecture and DDD/data model

The solution architecture uses different state names than the DDD architecture and data model:

| Solution Architecture | DDD / Data Architecture |
|---|---|
| `CREATED` (transient) | Not present |
| `PENDING` | `PENDING_ACCEPTANCE` |
| `COMPLETING` | `PENDING_APPROVAL` |
| `JURY_REVIEW` | `PENDING_JURY_VERDICT` |

The DDD document and data architecture use the same names (`PENDING_ACCEPTANCE`, `PENDING_APPROVAL`, `PENDING_JURY_VERDICT`). The solution architecture uses shortened versions. The data architecture's CHECK constraint on `bets.status` uses the DDD names.

**Action:** Align the solution architecture state names to match the DDD and data model exactly. The DDD names are more descriptive and are already codified in the database schema. Using different names will cause developer confusion and mapping bugs.

### C3. Solution architecture ER diagram diverges from data architecture schema

The ER diagram in solution-architecture.md Section 9.1 shows a `friend_requests` table separate from `friendships`. The data-architecture.md models friend requests as status transitions on the `friendships` table (status `PENDING` -> `ACTIVE`). The DDD architecture agrees with the data model (Friendship aggregate with status).

The ER diagram also shows `declared_by` and `declared_at` directly on the `bets` table, but the data architecture has a separate `outcome_claims` table. The ER diagram omits the `outcome_claims` and `domain_events` tables entirely.

**Action:** Update the ER diagram in Section 9.1 to match the data-architecture.md schema, which is the canonical source. The current ER diagram will mislead developers.

---

## Warnings (should fix, non-blocking)

### W1. RDS instance sizing inconsistency

Solution architecture Section 10.1 shows `db.t4g.medium` for the RDS instance. System-design.md and phase-3 decisions specify `db.t4g.micro` for MVP. The data-architecture.md does not specify instance size. The system-design.md is the infrastructure authority and specifies `db.t4g.micro`.

**Action:** Correct the solution architecture diagram to show `db.t4g.micro` for MVP.

### W2. Cost estimate inconsistency

Solution architecture Section 10.2 estimates MVP cost at "$100-150/month." Phase-3 decisions lock it at "~$96/month MVP." System-design.md calculates $96/month in detail. The solution architecture also lists different per-service costs (e.g., RDS at ~$30/month vs system-design's $13/month).

**Action:** Either remove the per-service cost table from the solution architecture and reference system-design.md, or align the numbers exactly.

### W3. SQS notification queue not shown in solution architecture

Phase-3 decisions confirm "Push notifications: FCM via Firebase Admin SDK, queued via SQS." System-design.md details the SQS-based notification pipeline. The solution architecture's container diagram (Level 2) shows the notification service calling FCM directly -- the SQS queue is absent from all diagrams and descriptions. Section 3.5 describes the notification service as a single service dispatching notifications with no mention of SQS.

**Action:** Add the SQS notification queue to the container diagram and describe the async notification dispatch pattern in Section 3.5. Without this, a developer reading only the solution architecture would implement synchronous FCM calls, violating the 2-second API SLA.

### W4. Module structure diverges from DDD bounded contexts

The DDD architecture defines 5 bounded contexts: Wagering, Identity, Social, Reputation, Notification. The solution architecture's package structure (Section 4.1) splits these differently:

- DDD's "Wagering Context" is split into `bet/`, `jury/`, `evidence/` modules
- DDD's "Identity Context" is split into `auth/`, `user/` modules
- DDD's "Reputation Context" maps to part of `user/` (stats are in profileSvc/profileCtrl)
- No distinct `reputation` package exists

The DDD document explicitly recommends package structure in Appendix C with `com.ibetcha.wagering`, `com.ibetcha.identity`, `com.ibetcha.social`, `com.ibetcha.reputation`, `com.ibetcha.notification`.

**Action:** Consider aligning the package structure to the DDD bounded contexts. The current split (especially merging Reputation into `user/`) blurs the context boundary that the DDD document deliberately establishes. The `jury/` package splitting from `bet/` also breaks the Bet aggregate's encapsulation -- jury logic is part of the Bet aggregate per the DDD design.

### W5. Rate limiting values inconsistent between documents

| Endpoint | Solution Architecture | System Design |
|---|---|---|
| Bet creation | 30/min/user | 10/min/user |
| Auth endpoints | 10/min/IP | 5/min/IP |
| Friend search | 60/min/user | 20/min/user |
| General API | 120/min/user | 60/min/user |

**Action:** Align rate limits. The system-design values are more conservative and likely more appropriate for MVP.

### W6. Deep link resolution endpoint auth inconsistency

Section 5.10 marks `GET /api/v1/deeplink/resolve` as "Auth: Required (called after registration)." Section 6.3's security config table marks the same endpoint as "Auth: No." These contradict each other.

**Action:** Decide which is correct. For deferred deep linking where a new user registers via invite, the endpoint needs to work pre-auth (to resolve the invite context during the registration flow) or immediately post-auth. Clarify the flow.

### W7. Email verification service not in infrastructure list

Section 5.2 includes email verification (`POST /api/v1/auth/register/verify`) and Section 10.2 lists AWS SES for transactional email. However, SES is not mentioned in system-design.md's infrastructure diagram or cost estimate, and phase-3 decisions do not mention it.

**Action:** Either add SES to the system-design infrastructure list or note that email verification is deferred to post-MVP (since OAuth is the primary registration path).

---

## Observations (informational, no action required)

### O1. Tech stack coherence is strong

Java 25 + Spring Boot 3.5 + PostgreSQL 16 + Liquibase + HikariCP + Spring Security is a well-established, battle-tested stack. All library versions are compatible. Virtual threads (Project Loom) in Java 25 are a good fit for I/O-bound operations. Expo SDK 53 with React Native is current. Zustand is a sound choice for the app's state complexity. No version conflicts detected.

### O2. API design is comprehensive and consistent

The API covers all MVP user stories from phase-1 decisions. Cursor-based pagination, consistent error format, UUID v7 IDs, and ISO 8601 timestamps are well-chosen conventions. The API is genuinely RESTful (resource-oriented, correct HTTP verbs, appropriate status codes). The `pendingAction` field on bet list responses is a smart UX optimization.

### O3. Security posture is solid

- ES256 JWT with access/refresh rotation and family-based replay detection is best practice.
- bcrypt at cost 12 with account lockout and rate limiting is appropriate for the threat model.
- S3 security (presigned URLs, no public access, CloudFront OAC, content-length enforcement) is thorough.
- OWASP Top 10 mitigations are addressed explicitly.
- GDPR considerations (EU data residency, right to deletion, data minimization) are well-handled.
- Token storage in expo-secure-store (Keychain/EncryptedSharedPreferences) is correct -- not AsyncStorage.

### O4. Expo managed workflow is the right call

The MVP feature set (camera, push, deep linking, secure storage, file system) is fully covered by Expo modules. No custom native code is needed. The ejection escape hatch is documented. EAS Build for CI/CD and OTA updates are valuable for iteration speed.

### O5. Win Card generation approach is pragmatic

Server-side Graphics2D rendering ensures brand consistency. The dual-format output (1080x1920 for Stories, 1200x630 for OG preview) is well-considered. The trade-off (template changes require backend deployment) is acceptable for MVP.

### O6. Contract test annotations are valuable

Section at the end listing external integration boundaries for contract testing (Google OAuth, Apple Sign-In, FCM, S3, CloudFront, SES) is a useful handoff artifact for the DevOps phase.

### O7. Caffeine cache is a pragmatic addition

The solution architecture adds Caffeine (in-memory cache) as a dependency, which is not mentioned in the system-design.md or phase-3 decisions. This is a reasonable addition for caching stats and profiles at the application level without introducing Redis. It bridges the gap between "no cache" (phase-3 for MVP) and "ElastiCache at 100K users" (system-design scaling plan).

### O8. Outbox pattern alignment

The data-architecture.md defines a `domain_events` table serving as both audit log and outbox. The solution architecture does not explicitly mention the outbox pattern for event delivery, but the data architecture covers it. The DDD document's Appendix B specifies "Async (outbox)" delivery for all cross-context events. These align well.

---

## Cross-Document Alignment Summary

| Aspect | Aligned? | Notes |
|---|---|---|
| Architecture style (modular monolith) | Yes | All documents agree |
| Tech stack (Java 25, Spring Boot 3.5, PG 16, Expo, Zustand) | Yes | All documents agree |
| Bet lifecycle states | **No** | Solution architecture uses shortened names (C2) |
| Database schema | **No** | ER diagram diverges from data architecture (C3) |
| Timeout scheduling | **Partial** | Solution arch matches phase-3; system-design contradicts (C1) |
| Push notifications (SQS) | **No** | Solution arch omits SQS queue (W3) |
| Package structure vs DDD contexts | **No** | Different module boundaries (W4) |
| Infrastructure sizing | **No** | RDS instance size mismatch (W1) |
| Cost estimates | **No** | Different numbers (W2) |
| Rate limiting | **No** | Different thresholds (W5) |
| Auth flow (JWT, OAuth, ES256) | Yes | All documents agree |
| Evidence upload (presigned S3) | Yes | All documents agree |
| Win Card generation (server-side) | Yes | All documents agree |
| CQRS for Reputation only | Yes | All documents agree |
| No event sourcing for MVP | Yes | All documents agree |
| UUID v7 primary keys | Yes | All documents agree |

---

## Recommendation

Address the 3 critical issues (state naming alignment, ER diagram sync, timeout scheduling contradiction) and the SQS notification queue omission (W3) before handing off to development. The remaining warnings are lower priority and can be resolved during sprint 0.

The architecture is well-designed for the MVP scope. The modular monolith approach, the comprehensive API surface, and the security design are all production-quality. The documents collectively form a solid foundation for development.
