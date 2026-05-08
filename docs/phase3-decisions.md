# iBetcha — Phase 3 Architecture Decisions (Confirmed 2026-05-08)

## Conflict Resolution

### Timeout Scheduling: Spring @Scheduled (DECIDED)
- **Choice:** `Spring @Scheduled` over EventBridge Scheduler
- **Rationale:** Simpler, no additional AWS service, sufficient for MVP with single Fargate instance
- **Trade-off accepted:** Requires leader election if scaling to multiple instances. Revisit at public launch.
- **Covers:** 48h bet acceptance timeout, 7d jury verdict timeout, reminder notifications, account deletion queue

## Confirmed Architecture Summary

- **Architecture style:** Modular monolith
- **Mobile:** React Native with Expo managed workflow
- **Backend:** Java 25 + Spring Boot 3.5
- **API:** REST
- **Database:** PostgreSQL 16 (RDS) + Liquibase, UUID v7 PKs
- **Push notifications:** FCM via Firebase Admin SDK, queued via SQS
- **Evidence upload:** Presigned S3 URLs, on-device compression
- **Win Cards:** Server-side generation (Java Graphics2D), stored in S3, served via CloudFront
- **State management (mobile):** Zustand
- **Auth:** JWT (ES256), access + refresh token rotation
- **Timeout scheduling:** Spring @Scheduled
- **Event sourcing:** No (MVP). Lightweight CQRS for reputation read model only.
- **Stats updates:** Synchronous (same transaction as bet resolution)
- **Estimated cost:** ~$96/month MVP, ~$392/month public launch
