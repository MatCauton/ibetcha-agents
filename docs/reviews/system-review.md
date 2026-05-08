# System Design Review -- iBetcha

**Reviewer:** Architecture Review (automated)
**Date:** 2026-05-08
**Document reviewed:** `docs/system-design.md`
**Cross-referenced:** `docs/phase1-decisions.md`, `docs/phase3-decisions.md`, `docs/solution-architecture.md`

---

## Verdict: CONDITIONAL PASS

The system design is thorough, well-reasoned, and appropriate for the scale. The estimations are sound, service choices are defensible, and the cost model is realistic. However, there is one critical inconsistency with locked architecture decisions and several warnings that should be addressed before development proceeds.

---

## Critical Issues (Blockers)

### CRIT-1: EventBridge Scheduler vs Spring @Scheduled -- Document Contradicts Locked Decision

**Severity:** Blocker
**Location:** Section 7 (Bet Timeout Scheduling), Section 5 (Scalability -- stateless design table), ADR-004, Architecture Diagram (Section 2)

The system design document recommends EventBridge Scheduler as the primary timeout mechanism and explicitly calls `@Scheduled` a "poor fit." However, `phase3-decisions.md` locks the decision as **Spring @Scheduled**:

> **Choice:** `Spring @Scheduled` over EventBridge Scheduler
> **Rationale:** Simpler, no additional AWS service, sufficient for MVP with single Fargate instance

This contradiction appears in multiple places:
1. The architecture diagram (Section 2) shows an "EventBridge Scheduler" box
2. ADR-004 records EventBridge Scheduler as "Accepted"
3. The stateless design table (Section 5) says: "Scheduled tasks: Externalized to EventBridge Scheduler. Not `@Scheduled` in-process"
4. The cost estimate includes EventBridge Scheduler line items
5. The substrate probes (Appendix B) include an EventBridge Scheduler probe

**Required action:** The entire document must be updated to reflect the locked @Scheduled decision. Specifically:
- Remove EventBridge from the architecture diagram
- Rewrite Section 7 to use the @Scheduled fallback approach as the primary
- Update ADR-004
- Remove EventBridge cost line items (saves ~$1/month -- negligible)
- Remove SchedulerSubstrateProbe from Appendix B
- Fix the stateless design table to acknowledge @Scheduled with leader election
- Add leader election details (the document's own fallback section sketches advisory locks -- promote this)

**Implications of @Scheduled that the document must address:**
- **Leader election is mandatory at 2+ Fargate tasks.** The document already shows database advisory locks as the pattern. This needs to be the primary design, not a footnote.
- **Deployment gap:** During rolling deployments, the @Scheduled job may not run for 30-60s. For 48h/7d timeouts this is completely acceptable, but the document should acknowledge it.
- **The stateless claim weakens.** With @Scheduled, the application is not fully stateless -- one instance holds the scheduling lock. The document's own stateless verification checklist needs updating.
- **No orphaned schedule cleanup needed.** Unlike EventBridge, @Scheduled polls the DB, so there are no orphan schedules to clean up when bets transition early. This is actually simpler.
- **Cost impact:** Negligible. EventBridge was $0-1/month. The polling query adds trivial DB load (one indexed query per minute).

---

## Warnings

### WARN-1: NAT Gateway Single Point of Failure (MVP)

**Location:** Section 2.5 (Networking)
**Risk:** Medium

The document correctly identifies that MVP uses a single NAT Gateway in AZ-a. It notes that AZ-b Fargate tasks route through AZ-a's NAT. What is understated: if this NAT Gateway fails, **all outbound internet access stops** -- no push notifications (FCM), no S3 presigned URL generation (needs STS), no OAuth token verification. The app effectively goes read-only (DB queries still work).

**Mitigation already planned:** Add second NAT Gateway at public launch ($32/month). The VPC endpoint for S3 (free, mentioned in the document) partially mitigates by keeping S3 traffic off the NAT.

**Recommendation:** Ensure the S3 VPC Gateway endpoint is deployed from day one, not deferred. Also consider an SQS VPC endpoint ($7/month) to keep notification queue traffic off the NAT path.

### WARN-2: Win Card Storage Estimate Seems High

**Location:** Section 1 (Storage Needs)
**Risk:** Low

The estimate shows 7,200 Win Cards/day at 300 KB each = 2.16 GB/day. However, Win Cards are generated only on bet resolution, not on bet creation. At 9,000 bets created/day with a 60% completion rate (phase1-decisions target), that is 5,400 resolved bets/day, not 7,200. Also, not all resolved bets have a clear winner (some expire, some are cancelled). The actual number is likely lower.

At 300 KB each, this is ~1.6 GB/day instead of 2.16 GB/day. The difference is small and the estimate errs on the safe side, so this is not a problem -- just a note for cost accuracy.

### WARN-3: SQS Notification Consumer Runs In-Process

**Location:** Section 3 (Push Notification Pipeline)
**Risk:** Medium

The notification worker is described as running "within the same Fargate task, async consumer." This means:
- If the Fargate task is under CPU pressure, notification consumption competes with API request serving.
- During deployments, in-flight SQS messages may be interrupted. The 60s visibility timeout and 3-retry policy handle this, but there could be duplicate notifications during rolling deploys.

This is acceptable for MVP. At scale, consider a dedicated consumer task or a separate ECS service for notification processing.

### WARN-4: In-Memory Rate Limiting Split-Brain at 2+ Tasks

**Location:** Section 5 (Rate Limiting)
**Risk:** Low (MVP), Medium (Public Launch)

The document acknowledges that per-task in-memory rate limiting means effective limits are 2x configured values. This is fine for MVP. The document correctly plans Redis-backed rate limiting for public launch.

However, the document does not mention that the `bucket4j` library (listed in solution-architecture.md tech stack) supports Redis-backed distributed rate limiting out of the box. This should be noted as the migration path.

### WARN-5: db.t4g.micro Has Only 1 GB RAM

**Location:** Section 2.2 (Database)
**Risk:** Low

The document configures `shared_buffers = 256MB` (25% of 1 GB RAM). For PostgreSQL on a 1 GB instance, this is correct practice. However, with HikariCP connections, the JVM for each connection's work_mem (4 MB), and OS overhead, available memory is tight. A complex query with a sort operation could push memory usage high.

At MVP traffic (0.04 QPS), this will never be a problem. But during development, if anyone runs analytical queries directly against the prod DB, they could hit OOM. Consider using db.t4g.small from the start ($13/month vs $7/month difference) -- the cost is negligible.

### WARN-6: CloudFront Signed URLs vs Presigned S3 URLs -- Two Signing Mechanisms

**Location:** Sections 3 and 4

The evidence pipeline uses S3 presigned URLs for uploads and CloudFront signed URLs for downloads. This is correct but introduces two different signing mechanisms:
- S3 presigned URLs (IAM-based, for PUT operations)
- CloudFront signed URLs (key pair-based, for GET operations)

The CloudFront key pair needs to be managed (stored in Secrets Manager or as a CloudFront trusted key group). This key is not listed in Section 2.7 (Secrets Management). Add the CloudFront signing private key to the secrets inventory.

### WARN-7: 1 GB Memory for Fargate Task May Be Tight with Win Card Generation

**Location:** Sections 2.1 and solution-architecture.md (Win Card Generation)

The solution architecture specifies Win Cards at 1080x1920 pixels generated via Graphics2D. An uncompressed RGBA image at that resolution is ~8 MB in memory. With the template background, overlaid text, and evidence thumbnail, peak memory during rendering could be 20-30 MB. Combined with Spring Boot baseline (~400-500 MB with JVM), HikariCP connections, SQS consumer threads, and request handling -- 1 GB is workable but leaves little headroom.

The JVM is configured with `-Xmx768m`. If Win Card generation coincides with a burst of API requests, heap pressure could cause GC pauses. Monitor this during load testing. Consider bumping to 2 GB memory ($36/month instead of $18/month for a single task) if GC pauses appear.

---

## Observations (Non-Blocking)

### OBS-1: Estimation Quality Is Good

The back-of-envelope numbers are realistic:
- 30% DAU/registered ratio is standard for social apps in early stages.
- 0.3-0.5 bets per DAU per day is reasonable for a social betting app.
- 20% evidence upload rate is a fair assumption.
- The 3x peak multiplier is industry-standard.
- Storage estimates align with the usage assumptions.
- The conclusion that this is a "small system" at 27 peak QPS is correct. Infrastructure decisions appropriately avoid over-engineering.

### OBS-2: Fargate CPU Calculation Self-Correction Is Valuable

Section 2.1 shows the architect catching and correcting their own CPU calculation. The corrected math (2.7 tasks needed at absolute peak, covered by auto-scaling from 2 to 4) is accurate. This kind of transparent reasoning builds confidence in the estimates.

### OBS-3: Cost Estimates Are Reasonable

MVP at ~$96/month and public launch at ~$392/month are realistic for the described AWS architecture. The NAT Gateway dominating MVP costs ($35 of $96) is a well-known AWS pain point, and the suggestion to use public subnets for MVP ($61/month) is a pragmatic optimization.

The only missing cost: data transfer between AZs for Multi-AZ RDS (public launch). This is typically $0.01/GB for cross-AZ traffic. At the described write volumes, this adds ~$1-2/month. Negligible.

### OBS-4: Notification Pipeline Design Is Sound

The SQS-based async notification flow is the right choice. The analysis of synchronous vs. async is well-argued (FCM latency would blow 2s API SLA). The DLQ configuration, delivery tracking table, and 80% delivery rate alert threshold are all appropriate.

One enhancement to consider: the notification message schema includes `actions` (Accept/Decline buttons). Ensure the SQS consumer handles the case where the bet state has already changed by the time the notification is processed (e.g., user accepts via the app before the notification arrives). The consumer should still send the notification (the user may have multiple devices), but the deep link should handle "bet already accepted" gracefully on the client side.

### OBS-5: Presigned URL Upload Flow Is Correct

The evidence upload pipeline (presigned URL, direct-to-S3, confirm, Lambda thumbnail) is a well-established pattern. The multi-layer file size enforcement (client, presigned URL, S3 bucket policy, server confirmation) provides defense in depth.

### OBS-6: Substrate Probes Are a Strong Practice

The "Earned Trust" probe pattern (verify infrastructure at startup) is excellent. The distinction between "refuse to start" (critical path: DB, S3, SQS) and "degraded" (non-critical: FCM, EventBridge/scheduler, CloudWatch) is well-calibrated.

With the switch to @Scheduled, the EventBridge probe disappears, but consider adding a probe that verifies the advisory lock mechanism works (acquire lock, release lock) to catch permission issues early.

### OBS-7: Scaling Ladder Is Well-Structured

The 5-phase scaling ladder (MVP -> Early Growth -> Public Launch -> Growth -> Scale) is clear and each step has a concrete trigger ("add read replica at 200K users," "add ElastiCache at 100K users"). This avoids both premature optimization and reactive scrambling.

### OBS-8: Security Group Design Is Clean

The three-tier security group design (ALB -> Fargate -> RDS) with minimal port openings follows least-privilege principles. No issues found.

### OBS-9: Solution Architecture Consistency

The solution architecture document (`solution-architecture.md`) is consistent with the system design on all key points: Fargate, RDS, S3 presigned URLs, FCM, SQS notifications. The solution architecture correctly shows @Scheduled in its C4 container diagram, which matches the phase3 decision. Only the system design document is out of sync.

---

## Summary

| Category | Count |
|----------|-------|
| Critical (blockers) | 1 |
| Warnings | 7 |
| Observations | 9 |

The single blocker (CRIT-1) is a document consistency issue, not a design flaw. The @Scheduled approach described in the document's own fallback section is workable for MVP. The document just needs to be updated to promote that approach from fallback to primary, per the locked phase3 decision.

Once CRIT-1 is resolved, this document is ready to guide implementation.
