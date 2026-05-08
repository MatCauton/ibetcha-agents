# iBetcha -- System Design Document

**Date:** 2026-05-08
**Author:** Titan (System Design Architect)
**Stack:** Java 25 + Spring Boot, PostgreSQL, AWS, React Native
**Scope:** Infrastructure-level architecture for MVP and public launch

---

## Table of Contents

1. [Back-of-Envelope Estimation](#1-back-of-envelope-estimation)
2. [AWS Infrastructure Design](#2-aws-infrastructure-design)
3. [Push Notification Pipeline](#3-push-notification-pipeline)
4. [Evidence Upload Pipeline](#4-evidence-upload-pipeline)
5. [Scalability Strategy](#5-scalability-strategy)
6. [Reliability and Observability](#6-reliability-and-observability)
7. [Bet Timeout Scheduling](#7-bet-timeout-scheduling)
8. [Cost Estimation](#8-cost-estimation)

---

## 1. Back-of-Envelope Estimation

### Assumptions

| Parameter | MVP (internal) | Public Launch |
|-----------|---------------|---------------|
| Registered users | 100-500 | 50,000-100,000 |
| Daily active users (DAU) | 50-150 | 15,000-30,000 |
| DAU/registered ratio | ~30% | ~30% |
| Bets created per DAU per day | 0.5 | 0.3 |
| Average participants per bet | 2.5 | 2.5 |
| Evidence upload rate per bet | 20% | 20% |
| Average evidence size (compressed) | 5 MB | 5 MB |
| Profile picture uploads per day | 2 | 200 |

### Requests Per Second

**MVP (150 DAU)**

| Operation | Daily count | QPS | Notes |
|-----------|------------|-----|-------|
| Read (home, bets, profiles, friends) | 3,000 | 0.03 | ~20 reads per active user per day |
| Write (create bet, accept, vote, etc.) | 300 | 0.003 | ~2 writes per active user per day |
| Push notifications sent | 500 | 0.006 | ~3 notifications per active user per day |
| Evidence uploads | 15 | negligible | 75 bets * 20% |
| **Total** | **~3,800** | **~0.04** | Single server handles this trivially |

Conclusion: MVP traffic is negligible. A single application instance handles all load with room for 100x growth.

**Public Launch (30,000 DAU)**

| Operation | Daily count | QPS | Peak QPS (3x avg) | Notes |
|-----------|------------|-----|--------------------|-------|
| Read (home, bets, profiles, friends) | 600,000 | 7 | 21 | ~20 reads per active user per day |
| Write (create bet, accept, vote) | 60,000 | 0.7 | 2.1 | ~2 writes per active user per day |
| Push notifications sent | 90,000 | 1 | 3 | ~3 notifications per active user per day |
| Evidence uploads | 1,800 | 0.02 | 0.06 | 9,000 bets * 20% |
| **Total** | **~750,000** | **~9** | **~27** | Still modest |

Conclusion: At peak, ~27 QPS total. This is a small system by infrastructure standards. A single well-configured server handles this. Two instances for availability, not for load.

### Storage Needs

**PostgreSQL Data**

| Data | Per record | Daily records (public launch) | Monthly growth | 1-year total |
|------|-----------|------------------------------|----------------|--------------|
| Users | 2 KB | 200 new/day | 6,000 | ~72,000 records, ~144 MB |
| Bets | 1 KB | 9,000/day | 270,000 | ~3.2M records, ~3.2 GB |
| Bet participants | 0.5 KB | 22,500/day | 675,000 | ~8.1M records, ~4 GB |
| Friendships | 0.3 KB | 1,000/day | 30,000 | ~360K records, ~108 MB |
| Notifications log | 0.5 KB | 90,000/day | 2,700,000 | ~32.4M records, ~16.2 GB |
| **Total DB** | | | | **~24 GB year 1** |

MVP: Under 500 MB total DB size for the first year.

**S3 Storage (Evidence + Media)**

| Asset | Size | Daily volume (public launch) | Monthly | 1-year total |
|-------|------|------------------------------|---------|--------------|
| Evidence (photo/video) | 5 MB avg | 1,800/day = 9 GB/day | 270 GB | ~3.2 TB |
| Thumbnails | 50 KB avg | 1,800/day = 90 MB/day | 2.7 GB | ~32 GB |
| Profile pictures | 200 KB avg | 200/day = 40 MB/day | 1.2 GB | ~14 GB |
| Win Cards | 300 KB avg | 7,200/day = 2.16 GB/day | 65 GB | ~780 GB |
| **Total S3** | | **~11 GB/day** | **~339 GB** | **~4 TB** |

MVP: Under 50 GB total S3 for the first year.

### Bandwidth

**Public Launch**

| Direction | Calculation | Bandwidth |
|-----------|-------------|-----------|
| Evidence upload | 1,800/day * 5 MB = 9 GB/day | ~0.8 Mbps avg, ~2.5 Mbps peak |
| Evidence download (viewing) | 5,000 views/day * 5 MB = 25 GB/day | ~2.3 Mbps avg, ~7 Mbps peak |
| API traffic (JSON) | 660K requests * 2 KB avg = 1.3 GB/day | ~0.12 Mbps avg |
| Win Card downloads | 10,000 shares/day * 300 KB = 3 GB/day | ~0.28 Mbps avg |
| **Total egress** | | **~38 GB/day, ~1.1 TB/month** |

MVP: Under 5 GB/month total bandwidth.

### Push Notification Throughput

| Metric | MVP | Public Launch |
|--------|-----|---------------|
| Notifications per day | 500 | 90,000 |
| Peak notifications per minute | 5 | 300 |
| Burst (viral bet with many participants) | 20 | 500 |

These volumes are well within FCM/APNs free tier and rate limits. FCM allows 1,500 messages/second per project. No throttling concerns at these scales.

---

## 2. AWS Infrastructure Design

### Architecture Diagram

```
                                 +------------------+
                                 |   Route 53       |
                                 |  api.ibetcha.app |
                                 +--------+---------+
                                          |
                                 +--------v---------+
                                 |   CloudFront     |
                                 |  (CDN for S3     |
                                 |   media only)    |
                                 +--------+---------+
                                          |
                        +-----------------+------------------+
                        |                                    |
               +--------v---------+                +--------v---------+
               |       ALB        |                |     S3 Buckets   |
               |  (Application    |                |  (evidence,      |
               |   Load Balancer) |                |   win-cards,     |
               +--------+---------+                |   profile-pics)  |
                        |                          +------------------+
              +---------+---------+
              |                   |
     +--------v------+  +--------v------+
     | ECS Fargate   |  | ECS Fargate   |      +-------------------+
     | Task 1        |  | Task 2        |      | Lambda            |
     | (Spring Boot) |  | (Spring Boot) |      | (thumbnail gen,   |
     |               |  |               |      |  win card gen)    |
     +-------+-------+  +-------+-------+      +-------------------+
              |                   |                      |
              +--------+--------+                       |
                       |                                |
              +--------v--------+               +-------v-------+
              |   RDS PostgreSQL|               |   S3 Event    |
              |   (primary)     |               |   Trigger     |
              +--------+--------+               +---------------+
                       |
              +--------v--------+
              |  RDS (standby/  |
              |  read replica)  |
              +------ ----------+

     +------------------+    +------------------+    +------------------+
     | SQS              |    | Secrets Manager  |    | CloudWatch       |
     | (notification    |    | (DB creds, FCM   |    | (metrics, logs,  |
     |  queue)          |    |  keys, OAuth     |    |  alarms)         |
     +------------------+    |  secrets)        |    +------------------+
                             +------------------+
     +------------------+
     | Spring           |
     | @Scheduled       |
     | (bet timeouts,   |
     |  DB advisory     |
     |  lock for leader |
     |  election)       |
     +------------------+
```

### 2.1 Compute: ECS Fargate (Recommended)

**Decision: ECS Fargate over EC2 and Lambda.**

| Option | Pros | Cons | Cost (public launch) |
|--------|------|------|---------------------|
| **ECS Fargate** | No server management, auto-scaling, pay per vCPU-second, Docker-native, good Spring Boot fit | Slightly higher cost vs EC2 at sustained load, cold start on scale-out (~30s for Spring Boot) | ~$70-150/mo |
| EC2 | Cheapest at sustained load, full OS control | Must manage OS patches, scaling groups, AMIs. Overkill ops burden for small team. | ~$40-80/mo |
| Lambda | Zero cost at zero traffic, scales to zero | Cold starts (5-15s for Java/Spring Boot), 15-min timeout, poor fit for stateful REST API, max 10GB memory | ~$30-60/mo |

**Why Fargate wins:**
- Spring Boot applications have 5-15 second cold starts on Lambda (unacceptable for 2s API SLA)
- EC2 requires managing OS, patching, and scaling -- operational overhead disproportionate for a small team
- Fargate gives container-native deployment with zero server management
- At ~27 peak QPS, even a single 0.5 vCPU / 1 GB task handles the load easily
- Trade-off: ~$30-70/month more than EC2, but saves significant ops time

**Fargate Task Configuration:**

| Parameter | MVP | Public Launch |
|-----------|-----|---------------|
| vCPU | 0.5 | 0.5 |
| Memory | 1 GB | 1 GB |
| Min tasks | 1 | 2 |
| Max tasks | 2 | 4 |
| Auto-scaling metric | CPU > 70% | CPU > 70% |
| Health check | /actuator/health | /actuator/health |
| Health check interval | 30s | 15s |
| Deregistration delay | 30s | 30s |
| Container image | ECR private repo | ECR private repo |
| Java flags | -Xmx768m -XX:+UseG1GC | -Xmx768m -XX:+UseG1GC |

**Why 0.5 vCPU is enough:** At 27 peak QPS with 50ms average processing time, CPU utilization = 27 * 0.05s = 1.35 seconds of CPU per second. That is 1.35 / 0.5 = 270% of a single 0.5 vCPU task. With 2 tasks that is 135% -- so we actually need 3 tasks at peak. However, peak is 3x average, and 3x average only happens for short bursts. With auto-scaling to 4, this covers peak comfortably with headroom.

Correction: re-calculating. 27 QPS * 50ms = 1.35 CPU-seconds per wall-clock second. 0.5 vCPU = 0.5 CPU-seconds per second. So we need 1.35 / 0.5 = 2.7 tasks at absolute peak. With min 2, max 4, and auto-scaling at CPU 70% threshold, the scale-out event fires at ~1.9 tasks worth of load (2 * 0.7), adding a third task. This handles peak comfortably.

### 2.2 Database: RDS PostgreSQL

**Instance Sizing:**

| Parameter | MVP | Public Launch |
|-----------|-----|---------------|
| Instance class | db.t4g.micro | db.t4g.small |
| vCPU | 2 (burstable) | 2 (burstable) |
| Memory | 1 GB | 2 GB |
| Storage | 20 GB gp3 | 50 GB gp3 |
| IOPS | 3,000 (gp3 baseline) | 3,000 (gp3 baseline) |
| Multi-AZ | No | Yes |
| Read replicas | 0 | 0 (add at ~200K users) |
| Backup retention | 7 days | 14 days |
| Automated snapshots | Daily | Daily |
| Encryption | At rest (AES-256) | At rest (AES-256) |
| Engine version | PostgreSQL 16 | PostgreSQL 16 |
| Parameter group | Custom (shared_buffers, work_mem tuned) | Custom |

**Why db.t4g.micro for MVP:**
- At 0.04 QPS, the database is essentially idle. A micro instance provides 2 burstable vCPUs and 1 GB RAM, which handles hundreds of concurrent connections
- Graviton (t4g) instances are ~20% cheaper than x86 equivalents
- 24 GB of data fits entirely in RAM even at year 1 public launch
- Trade-off: burstable instances throttle under sustained CPU load, but our DB CPU usage is negligible

**Why no read replica at public launch:**
- At 21 peak read QPS, the primary handles this trivially
- Read replicas add ~$30/month and replication lag complexity
- Add a read replica only when read QPS exceeds ~500 or when analytics queries compete with production reads
- This is the scaling ladder: solve the problem when it exists, not before

**Connection Pooling:**
- Use HikariCP (Spring Boot default) with:
  - `maximumPoolSize`: 10 (MVP), 20 (public launch)
  - `minimumIdle`: 2 (MVP), 5 (public launch)
  - `connectionTimeout`: 5000ms
  - `idleTimeout`: 300000ms (5 min)
- At 2-4 Fargate tasks * 20 connections = 40-80 max connections against the DB
- db.t4g.small supports ~85 connections by default (`max_connections` based on memory)
- No external connection pooler (PgBouncer) needed until we exceed ~200 connections

**Key PostgreSQL Parameters (custom parameter group):**

```
shared_buffers = 256MB          # 25% of 1GB RAM (MVP) / 512MB for 2GB (public)
effective_cache_size = 768MB    # 75% of RAM
work_mem = 4MB                  # per-sort/hash operation
maintenance_work_mem = 64MB     # for VACUUM, CREATE INDEX
random_page_cost = 1.1          # SSD-optimized (gp3)
log_min_duration_statement = 200 # log queries > 200ms
```

### 2.3 Storage: S3 Bucket Strategy

**Bucket Structure:**

| Bucket | Purpose | Access Pattern | Lifecycle |
|--------|---------|----------------|-----------|
| `ibetcha-evidence-{env}` | Photos/videos uploaded as bet evidence | Write-once, read-many via CloudFront | IA after 90 days, Glacier after 1 year |
| `ibetcha-media-{env}` | Profile pictures, Win Cards, thumbnails | Read-heavy, updated occasionally | Standard, no transition |
| `ibetcha-backups-{env}` | Database backups (if manual exports needed) | Write-only, rare reads | Glacier after 30 days, delete after 2 years |

**Why 2 buckets, not 1:**
- Evidence has different lifecycle (archival) vs profile pics (always hot)
- Separate IAM policies: evidence bucket has presigned-URL write access; media bucket has service-role-only write
- Cost optimization: evidence transitions to IA/Glacier; media stays Standard
- Trade-off: 2 buckets to manage vs 1, but prefix-based lifecycle policies are fragile and harder to audit

**S3 Configuration (all buckets):**
- Versioning: Enabled (evidence integrity, accidental overwrite protection)
- Encryption: SSE-S3 (AES-256, server-side)
- Public access: Blocked (all public access disabled)
- Access: CloudFront OAC (Origin Access Control) for reads; presigned URLs for writes
- CORS: Configured for the React Native app domain
- Object naming: `{type}/{userId}/{uuid}.{ext}` (e.g., `evidence/user-123/550e8400-e29b.mp4`)

**Lifecycle Policies (evidence bucket):**

| Transition | Days | Storage Class | Cost Reduction |
|------------|------|---------------|----------------|
| Initial | 0 | S3 Standard | Baseline |
| Transition 1 | 90 | S3 Standard-IA | ~40% cheaper storage |
| Transition 2 | 365 | S3 Glacier Instant Retrieval | ~68% cheaper storage |
| Expiration | 1825 (5 years) | Delete | Total cost elimination |

Rationale: Evidence is viewed frequently in the first ~3 months (while bet is recent), occasionally for 1 year (profile history), and rarely after that. Glacier Instant Retrieval maintains millisecond access but at deep-archive pricing.

### 2.4 CDN: CloudFront

**What goes through CloudFront:** Evidence media, profile pictures, win cards, thumbnails.

**What does NOT go through CloudFront:** API traffic. At <30 QPS, CloudFront in front of ALB adds latency (extra hop) and cost without benefit. CloudFront for API is justified at ~1,000+ QPS where edge caching reduces origin load.

**CloudFront Configuration:**

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Origin | S3 buckets via OAC | Secure, no direct S3 access |
| Price class | PriceClass_100 (US/EU only) | MVP targets EU. Cheaper than global. |
| TTL (Default) | 86400s (24h) | Media is immutable (UUID-named), long cache safe |
| TTL (Profile pics) | 3600s (1h) | Users may update profile pictures |
| Behaviors | /evidence/* -> evidence bucket, /media/* -> media bucket | Path-based routing |
| Viewer protocol | HTTPS only | Security requirement |
| Compress | Yes (gzip/brotli) | For thumbnails and win cards |
| WAF | No (MVP), Yes (public launch) | Cost-optimization for MVP |

**Cost at public launch:** ~38 GB/day egress = ~1.1 TB/month. CloudFront pricing at PriceClass_100: $0.085/GB for first 10 TB. Monthly cost: ~$94.

### 2.5 Networking: VPC Design

```
VPC: 10.0.0.0/16 (65,536 IPs)
Region: eu-west-1 (Ireland) -- closest to Belgian users

+----------------------------------------------------------+
|  VPC: 10.0.0.0/16                                        |
|                                                          |
|  +-- AZ eu-west-1a --+    +-- AZ eu-west-1b ----------+ |
|  |                    |    |                            | |
|  | Public Subnet      |    | Public Subnet              | |
|  | 10.0.1.0/24        |    | 10.0.2.0/24                | |
|  | - ALB              |    | - ALB                      | |
|  | - NAT Gateway      |    |                            | |
|  |                    |    |                            | |
|  | Private Subnet     |    | Private Subnet             | |
|  | 10.0.11.0/24       |    | 10.0.12.0/24               | |
|  | - Fargate tasks    |    | - Fargate tasks            | |
|  |                    |    |                            | |
|  | Data Subnet        |    | Data Subnet                | |
|  | 10.0.21.0/24       |    | 10.0.22.0/24               | |
|  | - RDS primary      |    | - RDS standby (Multi-AZ)   | |
|  +--------------------+    +----------------------------+ |
+----------------------------------------------------------+
```

**Key Networking Decisions:**

- **2 AZs, not 3:** At this scale, 2 AZs provide sufficient availability (99.99% for ALB). 3 AZs add NAT Gateway costs ($32/mo per additional AZ) without meaningful availability improvement for our SLA target.
- **1 NAT Gateway, not 2:** MVP uses 1 NAT Gateway in AZ-a. Fargate tasks in AZ-b route through AZ-a's NAT. Trade-off: single AZ failure takes out outbound internet for both AZs. At public launch, add a second NAT Gateway ($32/mo) for AZ-b. Fargate tasks can still process requests (ALB routes to healthy tasks) but cannot call external services (FCM, S3) if NAT is down.
- **VPC Endpoints:** Add S3 Gateway endpoint (free) and ECR endpoints to avoid NAT Gateway data charges for S3 and container image pulls.

**Security Groups:**

| Security Group | Inbound | Outbound | Attached To |
|----------------|---------|----------|-------------|
| `sg-alb` | 443 from 0.0.0.0/0 | All to `sg-fargate` | ALB |
| `sg-fargate` | 8080 from `sg-alb` | 5432 to `sg-rds`, 443 to 0.0.0.0/0 (FCM, S3) | Fargate tasks |
| `sg-rds` | 5432 from `sg-fargate` | None | RDS |

### 2.6 DNS and Load Balancing

**Route 53:**
- Zone: `ibetcha.app` (registered domain)
- A record: `api.ibetcha.app` -> ALB (alias record)
- A record: `cdn.ibetcha.app` -> CloudFront distribution (alias record)
- Health checks: HTTP health check on ALB target `/actuator/health`

**ALB Configuration:**

| Parameter | Value |
|-----------|-------|
| Scheme | Internet-facing |
| Listeners | HTTPS:443 (ACM certificate for *.ibetcha.app) |
| Target group | Fargate tasks, port 8080 |
| Health check path | /actuator/health |
| Health check interval | 15s |
| Healthy threshold | 2 |
| Unhealthy threshold | 3 |
| Deregistration delay | 30s |
| Idle timeout | 60s |
| Stickiness | Disabled (stateless backend) |
| Cross-zone LB | Enabled |

### 2.7 Secrets Management

**Decision: AWS Secrets Manager (not Parameter Store).**

| Secret | Type | Rotation |
|--------|------|----------|
| PostgreSQL master credentials | Secrets Manager | Auto-rotate every 30 days |
| FCM service account key | Secrets Manager | Manual (when key changes) |
| APNs auth key | Secrets Manager | Manual |
| Google OAuth client secret | Secrets Manager | Manual |
| Apple Sign-In private key | Secrets Manager | Manual |
| JWT signing key | Secrets Manager | Manual (rotate quarterly) |
| S3 presigned URL signing (IAM role) | IAM role attached to Fargate task | N/A (no static credential) |

**Why Secrets Manager over Parameter Store:**
- Automatic rotation for RDS credentials (built-in integration)
- Audit logging via CloudTrail
- Trade-off: $0.40/secret/month vs free for Parameter Store SecureString. At 6-7 secrets = ~$2.80/month. Cheap insurance for credential hygiene.
- Parameter Store SecureString lacks automatic rotation -- must build custom Lambda for rotation.

**Access pattern:** Fargate tasks use IAM task role with policy granting `secretsmanager:GetSecretValue` on specific secret ARNs. No static credentials in environment variables or config files.

---

## 3. Push Notification Pipeline

### Architecture

```
+----------+    +------------+    +---------+    +----------------+    +----------+
| Bet      |--->| Notif.     |--->| SQS     |--->| Notif. Worker  |--->| FCM /    |
| Service  |    | Event      |    | Queue   |    | (same Fargate  |    | APNs     |
| (app     |    | Publisher  |    |         |    |  task, async    |    |          |
|  code)   |    |            |    |         |    |  consumer)     |    |          |
+----------+    +------------+    +---------+    +-------+--------+    +----------+
                                                         |
                                                 +-------v--------+
                                                 | notification   |
                                                 | log table      |
                                                 | (PostgreSQL)   |
                                                 +----------------+
```

**Decision: Asynchronous via SQS (not synchronous).**

| Approach | Pros | Cons |
|----------|------|------|
| **SQS queue (recommended)** | Decouples bet logic from notification delivery. Retries on FCM/APNs failure. Bet creation latency unaffected by notification latency. | Additional component (SQS). Slightly more complex. ~5-30s delivery delay. |
| Synchronous (in-request) | Simple. No extra infrastructure. | Bet creation blocked if FCM is slow/down. No retry on failure. 2s API SLA at risk. |
| SNS -> SQS fanout | Built-in fanout to multiple subscribers. | Overkill -- we have one consumer. Adds SNS cost ($0.50/million). |

**Why SQS wins:**
- FCM/APNs calls take 100-500ms each. A bet with 4 participants = 4 push calls = 400-2000ms added to the request if synchronous. This blows the 2s API SLA.
- FCM has transient failures (~0.1% of requests). SQS retry handles this automatically.
- Trade-off: 5-30 second delivery delay vs real-time. The requirement says "within 5 seconds" -- SQS with 1-second polling meets this for normal cases. Occasional delays of 10-30s during retries are acceptable.

### SQS Queue Configuration

| Parameter | Value |
|-----------|-------|
| Queue type | Standard (not FIFO -- ordering not critical for notifications) |
| Visibility timeout | 60 seconds |
| Message retention | 4 days |
| Receive wait time | 5 seconds (long polling) |
| Redrive policy | 3 attempts, then DLQ |
| DLQ retention | 14 days |
| Encryption | SSE-SQS |

### Notification Message Schema

```json
{
  "type": "BET_CREATED",
  "betId": "uuid",
  "recipientUserId": "uuid",
  "title": "Tom challenged you!",
  "body": "\"I'll beat you on Sunday's ride\" -- Stake: Coffee",
  "data": {
    "betId": "uuid",
    "action": "ACCEPT_DECLINE",
    "deepLink": "ibetcha://bet/uuid"
  },
  "actions": [
    {"id": "ACCEPT", "label": "Accept"},
    {"id": "DECLINE", "label": "Decline"}
  ],
  "createdAt": "2026-05-08T14:30:00Z"
}
```

### Notification Types and Actions

| Event | Recipients | Title | Actionable? | Actions |
|-------|-----------|-------|-------------|---------|
| Bet created | All participants | "{creator} challenged you!" | Yes | Accept / Decline |
| Bet accepted | Creator + accepted participants | "{user} accepted the bet" | No | -- |
| All accepted ("Bet is ON!") | All participants | "Bet is ON!" | No | -- |
| Bet declined | Creator | "{user} declined your bet" | No | -- |
| Bet expired (48h timeout) | All participants | "Bet expired -- not all accepted" | No | -- |
| Winner declared | Jury or other participants | "{user} claims victory!" | Yes | Approve / Reject (jury) |
| Outcome approved | All participants | "Bet settled! {winner} wins!" | No | -- |
| Outcome disputed | Both participants | "Bet disputed" | No | -- |
| Jury reminder (24h before timeout) | Jury | "Reminder: Review {bet}" | Yes | Review Now |
| Jury timeout | All participants | "Jury timed out -- vote now" | Yes | Vote Now |
| Friend request | Recipient | "{user} wants to be friends" | Yes | Accept / Decline |
| Friend request accepted | Requester | "{user} accepted your friend request" | No | -- |
| Rivalry notification | Both | "You and {friend} are tied 4-4!" | No | -- |

### FCM / APNs Integration

**FCM (Android + iOS unified via Firebase):**
- Use Firebase Admin SDK for Java (server-side)
- Send via `FirebaseMessaging.send(Message)` 
- Store device FCM tokens per user in `device_tokens` table
- Handle token refresh: client sends updated token to `/api/devices` endpoint
- Handle unregistered tokens: FCM returns `UNREGISTERED` -- delete from DB

**APNs (iOS native):**
- FCM handles APNs delivery when sending to iOS devices via Firebase
- No direct APNs integration needed -- FCM acts as intermediary
- Configure APNs auth key in Firebase project settings

**Actionable Notifications:**
- FCM: Use `notification.click_action` + `data` payload with action buttons
- Android: Notification channels with action buttons (handled in React Native)
- iOS: UNNotificationAction with categories registered at app startup
- Actions call back to API: `POST /api/bets/{id}/accept` or `POST /api/bets/{id}/decline`

### Delivery Tracking

| Field | Purpose |
|-------|---------|
| `notification_id` | UUID, primary key |
| `user_id` | Recipient |
| `type` | Notification type enum |
| `status` | QUEUED, SENT, DELIVERED, FAILED, DLQ |
| `fcm_message_id` | FCM response ID for tracking |
| `created_at` | When event occurred |
| `sent_at` | When FCM accepted the message |
| `error` | Error message if FAILED |
| `attempt_count` | Retry count |

Track delivery rate: `COUNT(status=SENT) / COUNT(*)` per day. Alert if delivery rate drops below 80% (per discovery report recommendation).

### Notification Preferences and Throttling

**MVP:** No user-configurable preferences. All notifications enabled.

**Public Launch:** Add preferences table:

| Preference | Default | Options |
|------------|---------|---------|
| Bet invitations | ON | ON / OFF |
| Bet updates | ON | ON / OFF |
| Friend requests | ON | ON / OFF |
| Rivalry milestones | ON | ON / OFF |
| Quiet hours | OFF | Start/end time |

**Throttling:** Not needed at MVP volumes. At public launch, implement per-user throttle: max 20 notifications per hour. Group notifications if more than 3 in 5 minutes ("You have 5 new bet updates").

### Substrate Probe (Earned Trust)

The notification pipeline depends on FCM/APNs as external substrates. These can lie about delivery:

**Probe: `NotificationSubstrateProbe.probe()`**
- At startup, send a test notification to a known test device token
- Verify FCM returns a valid `message_id` (not a silent failure)
- Verify the DLQ is accessible and writable
- Verify SQS queue is accessible with `SendMessage` + `ReceiveMessage` + `DeleteMessage`
- If any probe fails: log `health.startup.degraded` with specific failure. Do not refuse to start (notifications are not on the critical path for API serving), but set a health flag that monitoring checks.

---

## 4. Evidence Upload Pipeline

### Architecture

```
                                     (1) Request presigned URL
+--------+    +----------+    +-------+    +-----------+    +------------------+
| Mobile |    | API      |    | S3    |    | Lambda    |    | CloudFront       |
| Client |--->| Server   |--->| (gen  |    | (trigger  |    | (serve           |
|        |    |          |    |  URL) |    |  on S3    |    |  thumbnails      |
|        |    +----------+    +---+---+    |  upload)  |    |  and evidence)   |
|        |                        |        +-----+-----+    +------------------+
|        |   (2) Upload direct    |              |
|        +------------------------+>  S3 Bucket  |
|        |                        |              |
|        |   (3) Confirm upload   |    (4) Generate thumbnail
|        +----> API Server -------+--> Lambda writes thumbnail to S3
+--------+
```

### Upload Flow (Step by Step)

**Step 1: Client requests presigned URL**
```
POST /api/evidence/upload-url
Request:  { "betId": "uuid", "contentType": "video/mp4", "fileSize": 12345678 }
Response: { "uploadUrl": "https://s3.../presigned", "evidenceId": "uuid", "expiresIn": 900 }
```

**Step 2: Client uploads directly to S3**
- Client uploads to the presigned URL using HTTP PUT
- No data flows through the API server (saves bandwidth and compute)
- Presigned URL is valid for 15 minutes

**Step 3: Client confirms upload**
```
POST /api/evidence/{evidenceId}/confirm
Response: { "status": "PROCESSING", "thumbnailUrl": null }
```
The API server verifies the object exists in S3 (HeadObject), records the evidence in the DB, and links it to the bet.

**Step 4: Thumbnail generation (async)**
- S3 event notification triggers Lambda function on `ObjectCreated` event
- Lambda generates a thumbnail (300x300 for images, first-frame for video)
- Lambda writes thumbnail to `ibetcha-media-{env}/thumbnails/{evidenceId}.jpg`
- Lambda updates the evidence record in DB with `thumbnail_url`

**Why presigned URLs over API proxy:**

| Approach | Pros | Cons |
|----------|------|------|
| **Presigned S3 URL (recommended)** | No API server bandwidth/memory consumed. Upload speed limited only by client internet. Server not a bottleneck. | Slightly more complex client code. Must handle presigned URL expiration. |
| API proxy (multipart upload) | Simple client code. Centralized validation. | 50MB file consumes 50MB server memory. Ties up a thread for minutes. At 2 concurrent uploads, that is 100MB RAM and 2 threads on a 1GB Fargate task. |

**Why presigned wins at our scale:** Even though upload volume is low (1,800/day at public launch), a single 50MB upload through the API proxy would consume 50% of our Fargate task memory. This is fragile. Presigned URLs cost nothing in API server resources.

### Client-Side Compression

**Recommended Libraries (React Native):**

| Format | Library | Target |
|--------|---------|--------|
| Photo (JPEG/PNG) | `react-native-image-picker` with `quality: 0.7` | Max 2 MB after compression |
| Video (MP4) | `react-native-video-compressor` or `FFmpegKit` | Max 10 MB after compression, 720p, 30fps |
| Thumbnails (client preview) | `react-native-image-resizer` | 300x300, quality 0.6, < 50 KB |

**Compression Targets:**

| Media Type | Raw Size (typical) | Target After Compression | Compression Ratio |
|------------|-------------------|--------------------------|-------------------|
| Photo (12MP) | 4-8 MB | 0.5-2 MB | ~75% reduction |
| Video (30s, 1080p) | 50-100 MB | 5-15 MB | ~80% reduction |
| Video (30s, 720p) | 30-60 MB | 3-8 MB | ~85% reduction |

**Max File Size Enforcement:**

| Layer | Check | Action |
|-------|-------|--------|
| Client (before compression) | Raw file > 100 MB | Show error: "File too large. Max 50 MB." |
| Client (after compression) | Compressed > 50 MB | Show error: "File too large even after compression." |
| Server (presigned URL) | Content-Length condition in presigned URL policy | S3 rejects upload > 50 MB |
| Server (confirmation) | HeadObject checks Content-Length | Reject and delete if > 50 MB |

Presigned URL with content-length constraint:
```java
PutObjectRequest.builder()
    .bucket("ibetcha-evidence-" + env)
    .key("evidence/" + userId + "/" + evidenceId + "." + ext)
    .contentType(contentType)
    .contentLength(fileSize)  // enforce exact size declared
    .build();
```

Additionally, use S3 bucket policy with a `Condition` on `s3:content-length-range` to enforce 50 MB max at the bucket level as a safety net.

### Thumbnail Generation Lambda

| Parameter | Value |
|-----------|-------|
| Runtime | Java 21 (GraalVM native image for fast cold start) or Python 3.12 (simpler, faster cold start) |
| Memory | 512 MB (video first-frame extraction needs memory) |
| Timeout | 60 seconds |
| Trigger | S3 ObjectCreated on `ibetcha-evidence-{env}` bucket, prefix `evidence/` |
| Output | Write to `ibetcha-media-{env}/thumbnails/{evidenceId}.jpg` |
| Libraries | Pillow (Python) for images, FFmpeg layer for video frames |

**Recommendation: Python 3.12 for the Lambda.**
- Trade-off: mixing languages (Java backend + Python Lambda). But Python cold start is <500ms vs 5-15s for Java.
- Thumbnail generation is a stateless image-processing task -- Python + Pillow + FFmpeg is the natural fit.
- The Lambda does not share code with the Spring Boot application, so language mismatch has zero coupling cost.

### CDN Delivery for Viewing Evidence

- All evidence and thumbnails served through CloudFront
- Signed URLs for evidence access (privacy -- only bet participants should view evidence)
- CloudFront signed URLs generated by the API server with 1-hour expiry
- Thumbnails use CloudFront signed URLs with 24-hour expiry (shown in bet lists)

**Signed URL flow:**
```
Client: GET /api/bets/{id}/evidence
Server: Verify user is participant/jury, generate CloudFront signed URL
Response: { "evidenceUrl": "https://cdn.ibetcha.app/evidence/...?Signature=...", "thumbnailUrl": "..." }
```

### Substrate Probe (Earned Trust)

**Probe: `S3SubstrateProbe.probe()`**
- At startup, upload a 1-byte test object to S3, read it back, verify contents match
- Verify presigned URL generation works (generate, then HEAD the presigned URL)
- Verify S3 event notification is configured (check bucket notification configuration)
- Verify the evidence bucket has the content-length policy enforced
- If S3 probe fails: refuse to start with `health.startup.refused: S3 substrate unavailable`
- Rationale: unlike notifications, evidence upload is on the critical path for bet completion. If S3 is misconfigured, evidence uploads silently fail.

---

## 5. Scalability Strategy

### Scaling Ladder: MVP to Public Launch

| Scale | Users | Changes Needed | Why This Step |
|-------|-------|---------------|---------------|
| MVP | 100-500 | Single Fargate task, db.t4g.micro, no cache, no CDN | Traffic is negligible. Optimize for cost and speed of development. |
| Early Growth | 1,000-5,000 | Add second Fargate task, enable Multi-AZ RDS, add CloudFront for media | Availability matters now. Users expect the app to be up. |
| Public Launch | 50K-100K | db.t4g.small, 2 NAT gateways, ElastiCache (optional), WAF, auto-scaling to 4 tasks | Real users in multiple timezones. Need reliability and some abuse protection. |
| Growth | 200K-500K | Read replica, ElastiCache Redis, database connection pooler (PgBouncer on RDS Proxy), 3 AZs | Read load justifies replica. Cache hot data. Connection count exceeds DB limits. |
| Scale | 500K-1M | Larger Fargate tasks (1 vCPU / 2 GB), RDS db.r7g.large, CDN for API responses, table partitioning | Real performance pressure. Need larger compute and optimized queries. |

### Database Scaling Path

**Phase 1 (MVP to 5K users): No special scaling needed.**
- HikariCP connection pool, 10 connections per task
- Standard indexes on foreign keys and common query patterns
- Monitor slow queries via `log_min_duration_statement = 200ms`

**Phase 2 (5K to 100K users): Optimize queries, add caching selectively.**
- Add composite indexes for hot queries (e.g., `bets by user + status`, `friendships by user`)
- Consider materialized views for profile stats (win rate, streak) -- compute once per bet resolution, not per profile view
- Query optimization: ensure `EXPLAIN ANALYZE` shows index scans, not seq scans

**Phase 3 (100K to 500K users): Read replica + cache.**
- Add RDS read replica for analytics and profile stats queries
- Route read-only endpoints to replica via Spring `@Transactional(readOnly = true)` + `AbstractRoutingDataSource`
- Add ElastiCache Redis (cache.t4g.micro, 0.5 GB) for:
  - User sessions (avoid DB lookup per request): TTL 24h
  - Profile stats (win rate, streaks): TTL 5 minutes (recomputed on bet resolution)
  - Friend lists: TTL 10 minutes
  - Hot bet details (active bets with many participants): TTL 1 minute

**Phase 4 (500K+ users): Partitioning.**
- Partition `notifications` table by `created_at` (monthly range partitions). This table grows fastest.
- Partition `bet_participants` by `bet_id` hash if join performance degrades
- Consider archiving resolved bets older than 1 year to a separate table/schema

### Stateless Backend Design

The Spring Boot application MUST be stateless to enable horizontal scaling:

| Concern | Stateless Approach |
|---------|--------------------|
| Sessions | JWT tokens. No server-side session storage. Token contains user ID, roles, expiry. |
| File uploads | Presigned S3 URLs. No local filesystem state. |
| WebSocket (future) | Not in MVP. When added, use Redis pub/sub for cross-instance message delivery. |
| Scheduled tasks | Spring `@Scheduled` with DB advisory lock for leader election (see Section 7). One instance holds the lock and processes timeouts; others skip. This is a deliberate trade-off: the application is not fully stateless when running multiple instances, but the polling approach is simpler than maintaining an external scheduling service. |
| Configuration | Environment variables injected via ECS task definition. Secrets from Secrets Manager. |
| Temp files | None. All processing in memory or direct to S3. |

**Verification:** The application should be deployable as N identical instances behind ALB with no shared state. Any request can be served by any instance.

### Caching Strategy (When Added)

**Not in MVP.** Add ElastiCache Redis at ~100K users when profile stats queries become a DB bottleneck.

| Cache Key | Data | TTL | Invalidation | Why |
|-----------|------|-----|--------------|-----|
| `user:stats:{userId}` | Win rate, total bets, current streak | 5 min | On bet resolution | Most-viewed data, expensive aggregate query |
| `user:friends:{userId}` | Friend list with avatars | 10 min | On friend add/remove | Loaded on every bet creation (friend picker) |
| `bet:detail:{betId}` | Full bet details | 1 min | On any bet state change | Hot bets viewed by multiple participants |
| `user:h2h:{userId}:{friendId}` | Head-to-head record | 5 min | On bet resolution between pair | Shown on friend profile and bet acceptance screen |
| `user:session:{token}` | Validated JWT claims | Token expiry | On logout | Avoid JWT parsing on every request (optional optimization) |

**ElastiCache Sizing (when added):**

| Parameter | Value |
|-----------|-------|
| Engine | Redis 7.x |
| Node type | cache.t4g.micro (0.5 GB) |
| Cluster mode | Disabled (single node) |
| Multi-AZ | No (at 100K users). Yes at 500K. |
| Estimated memory usage | ~50 MB (100K users * 500 bytes avg cached data) |
| Cost | ~$12/month |

### Rate Limiting

**MVP:** Basic rate limiting via Spring Boot filter.

| Endpoint Pattern | Limit | Window | Rationale |
|------------------|-------|--------|-----------|
| POST /api/bets | 10 requests | 1 minute per user | Prevent bet spam |
| POST /api/auth/* | 5 requests | 1 minute per IP | Prevent brute force |
| GET /api/users/search | 20 requests | 1 minute per user | Prevent scraping |
| POST /api/evidence/upload-url | 5 requests | 1 minute per user | Prevent S3 abuse |
| All other endpoints | 60 requests | 1 minute per user | General protection |

**Implementation:** In-memory `ConcurrentHashMap<String, RateLimitBucket>` with sliding window counter. At 1-2 Fargate tasks, each task enforces its own limits. This means effective limits are 2x the configured value (user could hit both tasks). This is acceptable at MVP scale.

**Public Launch:** Move to Redis-backed rate limiting (centralized counter across all tasks). Use `spring-boot-starter-data-redis` with a Lua script for atomic increment-and-check.

---

## 6. Reliability and Observability

### SLA Targets

| Metric | MVP | Public Launch |
|--------|-----|---------------|
| Availability | 99% (allows ~7h downtime/month) | 99.9% (allows ~43 min/month) |
| API latency (p50) | < 200ms | < 100ms |
| API latency (p99) | < 2s | < 500ms |
| Notification delivery | < 30s | < 10s |
| Evidence upload | Best effort | < 60s for 10 MB file |
| RTO (Recovery Time Objective) | 4 hours | 1 hour |
| RPO (Recovery Point Objective) | 24 hours | 1 hour |

### Health Checks

**Application Health Endpoint: `/actuator/health`**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL", "validationQuery": "SELECT 1" } },
    "sqs": { "status": "UP", "details": { "queue": "notification-queue" } },
    "s3": { "status": "UP", "details": { "bucket": "ibetcha-evidence-prod" } }
  }
}
```

**Health check types:**

| Check | Endpoint | Interval | Purpose |
|-------|----------|----------|---------|
| ALB health | /actuator/health | 15s | Route traffic away from unhealthy tasks |
| Deep health | /actuator/health (full) | 60s | Monitor all dependencies (DB, SQS, S3) |
| Readiness | /actuator/health/readiness | 10s | Fargate task readiness gate |
| Liveness | /actuator/health/liveness | 30s | Detect stuck tasks |

### CloudWatch Monitoring

**Custom Metrics (published via Micrometer -> CloudWatch):**

| Metric | Namespace | Alarm Threshold | Action |
|--------|-----------|----------------|--------|
| `api.latency.p99` | iBetcha/API | > 2s for 3 consecutive periods | SNS alert to team |
| `api.error.rate` | iBetcha/API | > 5% 5xx for 2 consecutive periods | SNS alert to team |
| `bet.created.count` | iBetcha/Business | < 1 in 24h (public launch only) | Warning -- possible outage |
| `notification.delivery.rate` | iBetcha/Notifications | < 80% over 1h | SNS alert -- FCM issue |
| `notification.dlq.count` | iBetcha/Notifications | > 0 | SNS alert -- investigate failures |
| `evidence.upload.failures` | iBetcha/Evidence | > 5 in 1h | SNS alert -- S3 issue |
| `db.connection.pool.active` | iBetcha/Database | > 80% of max | Warning -- approaching limit |

**AWS Native Metrics (CloudWatch built-in):**

| Service | Key Metrics |
|---------|-------------|
| RDS | CPUUtilization, FreeStorageSpace, DatabaseConnections, ReadLatency, WriteLatency |
| ECS | CPUUtilization, MemoryUtilization, RunningTaskCount |
| ALB | TargetResponseTime, HTTPCode_Target_5XX_Count, HealthyHostCount |
| SQS | NumberOfMessagesSent, ApproximateAgeOfOldestMessage, NumberOfMessagesInDLQ |

### Logging Strategy

**Structured JSON Logging (Logback + logstash-logback-encoder):**

```json
{
  "timestamp": "2026-05-08T14:30:00.123Z",
  "level": "INFO",
  "logger": "com.ibetcha.bet.BetService",
  "message": "Bet created",
  "betId": "uuid",
  "creatorId": "uuid",
  "participantCount": 3,
  "traceId": "abc123",
  "spanId": "def456",
  "environment": "production",
  "version": "1.2.0"
}
```

**Log Levels:**
- ERROR: Unrecoverable failures (DB connection lost, S3 unavailable)
- WARN: Recoverable issues (FCM delivery failed, retry scheduled, rate limit hit)
- INFO: Business events (bet created, bet accepted, evidence uploaded)
- DEBUG: Request/response details (disabled in production)

**Log Aggregation:**
- Fargate tasks send stdout/stderr to CloudWatch Logs (built-in via `awslogs` driver)
- Log group: `/ecs/ibetcha/{environment}`
- Log retention: 30 days (MVP), 90 days (public launch)
- CloudWatch Logs Insights for ad-hoc queries
- No ELK/Datadog for MVP -- CloudWatch Logs Insights is sufficient at this log volume

**Cost note:** At ~750K requests/day * ~500 bytes per log line = ~375 MB/day of logs. CloudWatch Logs ingestion: $0.50/GB. Monthly cost: ~$5.60. Retention (30 days * 11.25 GB): $0.03/GB/month = ~$0.34. Total: ~$6/month. Cheap.

### Distributed Tracing

**Decision: AWS X-Ray via Micrometer Tracing + OpenTelemetry bridge.**

| Component | Instrumentation |
|-----------|----------------|
| Spring Boot | `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` |
| HTTP requests | Auto-instrumented by Spring Web filter |
| Database queries | Auto-instrumented by Micrometer JDBC |
| SQS publish/consume | Manual span creation (SQS message attribute carries trace ID) |
| S3 operations | Auto-instrumented by AWS SDK v2 |
| External calls (FCM) | Manual span creation |

**Sampling:** 10% of requests in production (enough for debugging, keeps cost low). 100% in staging.

**Why X-Ray over Jaeger/Zipkin:**
- Zero infrastructure to manage (X-Ray is a managed AWS service)
- Native integration with ECS, ALB, SQS, Lambda
- Trade-off: vendor lock-in to AWS. But we are already all-in on AWS. The cost of running self-hosted Jaeger ($20-50/month for storage + compute) is not justified at this scale.
- X-Ray pricing: $5/million traces recorded. At 750K requests/day * 10% sampling = 75K traces/day = 2.25M/month = $11.25/month.

### Backup and Disaster Recovery

**Database Backups:**

| Strategy | MVP | Public Launch |
|----------|-----|---------------|
| Automated snapshots | Daily, 7-day retention | Daily, 14-day retention |
| Point-in-time recovery | Enabled (5-minute granularity) | Enabled (5-minute granularity) |
| Cross-region backup | No | No (add at 500K+ users) |
| Manual snapshots | Before major migrations | Before major migrations + weekly |

**S3 Data Protection:**
- Versioning enabled (accidental deletes are recoverable)
- Cross-region replication: No (add at 500K+ users when data is critical business asset)
- S3 Object Lock: No (evidence is not legally mandated for retention)

**Disaster Recovery Plan:**

| Scenario | Recovery |
|----------|----------|
| Single task failure | ALB routes to healthy tasks. ECS replaces failed task in ~60s. |
| AZ failure (MVP) | Full outage until AZ recovers (single-AZ setup). RTO: minutes to hours. |
| AZ failure (public launch) | ALB routes to healthy AZ. RDS Multi-AZ failover in ~60s. RTO: ~2 minutes. |
| Region failure | Full outage. Restore from RDS snapshot + S3 in another region. RTO: 2-4 hours. |
| Data corruption | Point-in-time restore to pre-corruption timestamp. RPO: 5 minutes. |
| Accidental S3 delete | Restore from S3 versioning. RPO: 0 (no data loss). |

---

## 7. Bet Timeout Scheduling

### Requirements

Two timeout scenarios:

1. **48h acceptance timeout:** If not all participants accept within 48 hours of bet creation, the bet expires. All participants notified.
2. **7d jury verdict timeout:** If jury does not approve/reject within 7 days of winner declaration, auto-escalate to participant majority vote. Jury and participants notified.

Additionally, reminders:
- 24h before acceptance timeout: reminder to pending participants
- 24h before jury timeout: reminder to jury

### Options Analysis

| Option | Pros | Cons | Fit |
|--------|------|------|-----|
| **EventBridge Scheduler (recommended)** | Serverless, no infra to manage. Per-schedule pricing ($1/million). Exact-time delivery. Survives deployments. | AWS-specific. 1-minute minimum granularity (fine for hour-scale timeouts). | Best fit |
| Spring `@Scheduled` poll | Simple. No external dependency. | Runs on a single task (or needs leader election for multi-task). Lost during deployments. Polling wastes CPU. Not scalable. | Poor fit |
| SQS Delay Queues | Up to 15-minute delay built in. Familiar pattern. | Max delay is 15 minutes. 48h timeout requires re-queuing or visibility timeout tricks. Hacky for long delays. | Poor fit |
| DynamoDB TTL + Streams | TTL-based expiration triggers a stream event. No polling. | Adds DynamoDB dependency (we use PostgreSQL). TTL deletion is best-effort (up to 48h delay). Unacceptable imprecision. | Poor fit |
| Database polling (Spring `@Scheduled` with DB query) | Simple. Uses existing DB. No new components. | Polling interval vs precision trade-off. N tasks = N polls (waste). Leader election needed. | Acceptable fallback |

### Recommended: EventBridge Scheduler

**How it works:**

```
Bet Created
    |
    v
API Server creates two EventBridge schedules:
  1. "bet-{id}-reminder" at T+24h  -> invokes Lambda/API
  2. "bet-{id}-expire"   at T+48h  -> invokes Lambda/API
    |
    v
At T+24h: reminder fires
  -> Check if bet is still PENDING
  -> If yes: send reminder notifications to pending participants
  -> If no (already all accepted or cancelled): delete both schedules (no-op)
    |
    v
At T+48h: expiry fires
  -> Check if bet is still PENDING
  -> If yes: transition bet to EXPIRED, notify all participants
  -> If no: no-op
  -> Delete both schedules (cleanup)
```

**Same pattern for jury timeout:**

```
Winner Declared (jury assigned)
    |
    v
API Server creates two EventBridge schedules:
  1. "jury-{betId}-reminder" at T+6d   -> reminder to jury
  2. "jury-{betId}-timeout"  at T+7d   -> escalate to participant vote
```

**EventBridge Scheduler Configuration:**

| Parameter | Value |
|-----------|-------|
| Schedule type | One-time (at specific datetime) |
| Target | SQS queue (same notification queue, different message type) |
| Retry policy | 3 retries with exponential backoff |
| DLQ | Same DLQ as notification queue |
| Flexible time window | OFF (exact delivery) |
| Schedule group | `ibetcha-bet-timeouts` |

**Why target SQS instead of directly invoking API/Lambda:**
- SQS provides at-least-once delivery with retry
- Same consumer infrastructure as notifications (no new component)
- If the API is temporarily down, the message waits in SQS (not lost)
- Trade-off: adds ~1-5s latency vs direct invocation. Acceptable for hour-scale timeouts.

**Message format for timeout events:**

```json
{
  "type": "BET_ACCEPTANCE_TIMEOUT",
  "betId": "uuid",
  "scheduledAt": "2026-05-10T14:30:00Z",
  "action": "EXPIRE_BET"
}
```

**Idempotency:** The timeout handler MUST check current bet state before acting. The bet may have been accepted/cancelled between schedule creation and execution. The handler:
1. Loads bet from DB
2. Checks if state is still PENDING (for acceptance timeout) or AWAITING_JURY (for jury timeout)
3. If state has changed: no-op, delete remaining schedules
4. If state matches: transition state, send notifications

**Schedule Cleanup:** When a bet is accepted (all participants), cancelled, or jury approves -- delete the associated EventBridge schedules to avoid orphaned schedules. Use the deterministic naming convention (`bet-{id}-expire`, `jury-{betId}-timeout`) for cleanup.

**Cost:** EventBridge Scheduler: $1.00 per million schedules created. At 9,000 bets/day * 2 schedules = 18,000/day = 540K/month. Cost: $0.54/month. Negligible.

### Substrate Probe (Earned Trust)

**Probe: `SchedulerSubstrateProbe.probe()`**
- At startup, create a test schedule 2 minutes in the future targeting SQS
- Verify the schedule was created successfully (API returns 200)
- Do NOT wait for execution (2 min is too long for startup)
- Verify the schedule group exists
- Verify IAM permissions allow CreateSchedule, DeleteSchedule, GetSchedule
- If probe fails: log `health.startup.degraded: EventBridge Scheduler unavailable`. Do not refuse to start -- bet creation still works, but timeouts will not fire. Set monitoring alert.

### Fallback: Database Polling (if EventBridge is rejected)

If the team prefers to avoid EventBridge Scheduler, the fallback is:

```java
@Scheduled(fixedRate = 60000) // every 60 seconds
public void processBetTimeouts() {
    List<Bet> expiredBets = betRepository.findByStatusAndExpiresAtBefore(
        BetStatus.PENDING, Instant.now()
    );
    for (Bet bet : expiredBets) {
        betService.expireBet(bet.getId());
    }
}
```

**With leader election via database advisory lock:**
```java
@Scheduled(fixedRate = 60000)
public void processBetTimeouts() {
    if (!lockService.tryAcquire("bet-timeout-processor")) {
        return; // another instance holds the lock
    }
    // ... process timeouts
}
```

**Trade-offs vs EventBridge:**
- Simpler (no new AWS service)
- But: polling every 60s on a table that grows to millions of rows is wasteful. Must add an index on `(status, expires_at)` and ensure it is used.
- Leader election adds complexity and failure modes
- Deployment gap: during rolling deployment, no instance may hold the lock for 30-60s. Timeouts are delayed but not lost.
- Verdict: acceptable for MVP if team wants fewer AWS services. Switch to EventBridge for public launch.

---

## 8. Cost Estimation

### MVP Phase (100-500 users)

| Service | Configuration | Monthly Cost |
|---------|--------------|-------------|
| **ECS Fargate** | 1 task, 0.5 vCPU, 1 GB, 730h | $18 |
| **RDS PostgreSQL** | db.t4g.micro, 20 GB gp3, single-AZ | $13 |
| **ALB** | 1 ALB, minimal LCU usage | $18 |
| **S3** | < 5 GB stored, minimal requests | $1 |
| **CloudFront** | < 5 GB/month transfer | $1 |
| **Route 53** | 1 hosted zone, minimal queries | $1 |
| **NAT Gateway** | 1 gateway, < 5 GB processed | $35 |
| **Secrets Manager** | 7 secrets | $3 |
| **SQS** | < 100K messages/month | $0 (free tier) |
| **EventBridge Scheduler** | < 10K schedules/month | $0 (free tier) |
| **CloudWatch** | Logs + metrics + alarms | $5 |
| **ECR** | < 2 GB images stored | $0 (free tier) |
| **Lambda** | < 1K invocations/month | $0 (free tier) |
| **X-Ray** | < 100K traces/month | $0 (free tier) |
| **ACM** | 1 certificate | $0 (free) |
| **Domain** | ibetcha.app (if .app TLD) | ~$14/year = $1.17/mo |
| | | |
| **Total MVP** | | **~$96/month** |

**Note:** NAT Gateway is the biggest cost at MVP ($35). To reduce this:
- Use VPC endpoints for S3 (free) and ECR (saves data processing charges)
- Consider: run without NAT Gateway entirely by placing Fargate tasks in public subnets with public IPs. Trade-off: tasks are directly addressable from internet (mitigated by security groups that only allow ALB inbound). This saves $35/month but weakens network isolation. Recommended for MVP cost savings, switch to private subnets + NAT at public launch.

**Cost-optimized MVP (public subnets, no NAT):** ~$61/month.

### Public Launch (50K-100K users)

| Service | Configuration | Monthly Cost |
|---------|--------------|-------------|
| **ECS Fargate** | 2-4 tasks, 0.5 vCPU, 1 GB each, avg 3 tasks | $54 |
| **RDS PostgreSQL** | db.t4g.small, 50 GB gp3, Multi-AZ | $52 |
| **ALB** | 1 ALB, moderate LCU usage | $25 |
| **S3** | ~500 GB stored, moderate requests | $12 |
| **CloudFront** | ~1.1 TB/month transfer | $94 |
| **Route 53** | 1 hosted zone, ~10M queries/month | $5 |
| **NAT Gateway** | 2 gateways, ~50 GB/month processed | $90 |
| **Secrets Manager** | 7 secrets | $3 |
| **SQS** | ~3M messages/month | $1 |
| **EventBridge Scheduler** | ~540K schedules/month | $1 |
| **CloudWatch** | Logs (~11 GB/day) + metrics + alarms + dashboards | $30 |
| **ECR** | < 5 GB images | $1 |
| **Lambda** | ~55K invocations/month (thumbnails) | $1 |
| **X-Ray** | ~2.25M traces/month (10% sampling) | $11 |
| **WAF** | 1 web ACL, 5 rules, ~10M requests/month | $11 |
| **ACM** | 1 certificate | $0 |
| **Domain** | ibetcha.app | $1 |
| | | |
| **Total Public Launch** | | **~$392/month** |

### Cost Breakdown by Category

```
MVP ($96/month):
  Compute (Fargate + ALB)       $36   37%
  Networking (NAT + Route53)    $36   38%  <-- NAT dominates
  Database (RDS)                $13   14%
  Storage + CDN (S3 + CF)       $2    2%
  Operations (CW, SM, etc)      $9    9%

Public Launch ($392/month):
  Networking (NAT + Route53)   $95   24%  <-- still the biggest
  CDN (CloudFront)             $94   24%  <-- evidence/media delivery
  Database (RDS Multi-AZ)      $52   13%
  Compute (Fargate + ALB)      $79   20%
  Operations (CW, X-Ray, WAF)  $52   13%
  Storage (S3, ECR)            $13    3%
  Other (SQS, EB, SM, etc)     $7    2%
```

### Cost Optimization Opportunities

| Optimization | Savings | Trade-off |
|-------------|---------|-----------|
| MVP: Public subnets (no NAT) | $35/month | Weaker network isolation. Acceptable for MVP. |
| Fargate Spot (MVP tasks) | ~30% on compute ($5-10/month) | Tasks can be interrupted. Not recommended for production. |
| Reserved RDS (1-year, public launch) | ~30% on RDS ($15/month) | 1-year commitment. Worth it only after product-market fit confirmed. |
| CloudFront savings with S3 IA transition | ~$5-10/month | Already planned via lifecycle policies |
| X-Ray sampling reduction (5%) | ~$5/month | Fewer traces for debugging |

---

## Appendix A: Infrastructure Decision Records

### ADR-001: Compute -- ECS Fargate

**Status:** Accepted
**Context:** Need to run Java 25 + Spring Boot as containerized workload on AWS.
**Decision:** ECS Fargate over EC2 and Lambda.
**Rationale:** Zero server management, Docker-native, acceptable cold start (not on critical path -- ALB routes to warm tasks). Lambda's 5-15s Java cold start violates 2s API SLA. EC2's operational overhead is disproportionate for team size.
**Trade-off:** ~$30-70/month more than equivalent EC2 at sustained load.
**Revisit when:** Compute costs exceed $500/month (indicates sufficient scale for EC2 optimization to matter).

### ADR-002: Notification Delivery -- Asynchronous via SQS

**Status:** Accepted
**Context:** Push notifications must be sent within 5 seconds of bet lifecycle events without blocking API responses.
**Decision:** Publish notification events to SQS queue, consume asynchronously within the same Fargate task.
**Rationale:** FCM calls take 100-500ms. Multiple recipients per bet event would blow 2s API SLA if synchronous. SQS provides retry on FCM failure.
**Trade-off:** 1-5s additional latency for notification delivery. SQS as additional component.
**Revisit when:** Need for real-time notifications (WebSocket) or notification volume exceeds 1M/day.

### ADR-003: Evidence Upload -- S3 Presigned URLs

**Status:** Accepted
**Context:** Users upload evidence (photos/videos) up to 50 MB.
**Decision:** Client uploads directly to S3 via presigned URL. API generates URL and confirms upload.
**Rationale:** Uploading through API server would consume 50 MB of RAM per upload on a 1 GB Fargate task. Presigned URLs offload bandwidth and memory to S3.
**Trade-off:** More complex client-side code. Must handle presigned URL expiration.
**Revisit when:** Need server-side virus scanning before storage (add Lambda trigger post-upload).

### ADR-004: Bet Timeout Scheduling -- EventBridge Scheduler

**Status:** Accepted
**Context:** Bets expire after 48h if not accepted. Jury verdicts timeout after 7 days.
**Decision:** EventBridge Scheduler creates one-time schedules targeting SQS.
**Rationale:** Precise timing, serverless (no polling), survives deployments, $0.54/month at public launch. Database polling wastes CPU and requires leader election for multi-instance.
**Trade-off:** Additional AWS service dependency. Schedules must be cleaned up when bets transition early.
**Revisit when:** Timeout precision requirements change to sub-minute (unlikely for this domain).

### ADR-005: No Cache Layer for MVP

**Status:** Accepted
**Context:** Profile stats and bet lists could benefit from caching.
**Decision:** No Redis/ElastiCache for MVP. Add at ~100K users.
**Rationale:** At 0.04 QPS (MVP) to 27 QPS (public launch), PostgreSQL handles all reads from buffer cache. Adding ElastiCache costs ~$12/month and adds operational complexity (cache invalidation, connection management) with no measurable benefit at this scale.
**Trade-off:** When cache is needed, adding it requires code changes (cache-aside pattern in service layer).
**Revisit when:** Database CPU > 50% sustained or p99 query latency > 200ms.

---

## Appendix B: Substrate Probes Summary

Per the Earned Trust principle, every infrastructure component must verify its substrate at startup.

| Component | Probe | Failure Mode Detected | On Failure |
|-----------|-------|----------------------|------------|
| PostgreSQL | SELECT 1, verify connection pool | DB unreachable, credentials invalid | Refuse to start |
| SQS | SendMessage + ReceiveMessage + DeleteMessage on test message | Queue missing, IAM denied | Refuse to start |
| S3 | PutObject + GetObject + HeadObject on test key | Bucket missing, IAM denied, encryption misconfigured | Refuse to start |
| S3 presigned URL | Generate presigned URL, HEAD it | Presigned URL policy misconfigured | Refuse to start |
| EventBridge Scheduler | CreateSchedule + DeleteSchedule | IAM denied, scheduler API unavailable | Degraded (log warning, do not refuse) |
| Secrets Manager | GetSecretValue for each secret | Secret missing, IAM denied | Refuse to start |
| FCM | Send test notification to known test token | FCM credentials invalid, project misconfigured | Degraded (log warning) |
| CloudWatch | PutMetricData with test metric | IAM denied, region misconfigured | Degraded (log warning) |

**Probe execution order:** Sequential during Spring Boot startup (`ApplicationRunner` bean). Total probe time target: < 10 seconds. If any "refuse to start" probe fails, application exits with code 1 and a structured log message naming the specific failure.

**Probe self-verification:** After dependency upgrades, run integration test suite that deliberately misconfigures each substrate (wrong IAM policy, wrong bucket name, wrong credentials) and verifies the corresponding probe rejects startup. This prevents probes from becoming stale after refactoring.
