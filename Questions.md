# iBetcha — Open Questions

Answer each question below. Leave questions blank if not yet decided.

---

## 1. Platform
Is this a native mobile app (iOS + Android), a web app, or both?
Push notifications strongly imply mobile — does a web version matter for MVP?

**Answer:**
native mobile app
---

## 2. Money Bets
When the prize is money, is this tracked informally ("Ben owes Sarah €10") or does the app facilitate actual payments (Stripe, Apple Pay)?
Real payments add significant compliance and complexity.

**Answer:**
For the MVP, informal tracking is fine. But in the end we would like to add functionality for payment. So think about that during the planning.
---

## 3. Jury — Registered User or External?
Does the jury need to be a registered iBetcha user, or can they be an external person (e.g., invited via a link or email)?

**Answer:**
A registered iBetcha user.
---

## 4. Friend Discovery
How do users find friends? Username search only, or also phone contacts / email lookup?

**Answer:**
Username search is sufficient. But there should also be an option to send a friend invite (e.g. via whatsapp). The invite generates a shareable deep link with a referral code — when tapped, it opens iBetcha and auto-sends a friend request. (Confirmed)
---

## 5. Scale Expectations
Is this for internal/friend-group use or a public launch?
This determines infra sizing and whether we need a message queue for notifications.

**Answer:**
MVP is for internal use, but in the end we want to launch the app publicly
---

## 6. Evidence Compression
50MB max for photo/video evidence — is compression handled on-device before upload, or server-side?
Server-side compression has significant backend compute implications.

**Answer:**
on-device is fine.
---

## 7. MVP Scope
Is everything in Requirements.md in scope for Day 1, or is there a phased rollout?
For example: informal money tracking for v1, real payments later?

**Answer:**
Your agents can decide on that. We want a working MVP with enough working functionality to have a good user experience.
---

## 8. Tech Preferences
Any existing preference for:
- Mobile framework (React Native, Flutter, Expo)?
- Backend language (Node.js, Go, Python, .NET)?
- Existing AWS setup or starting fresh?

**Answer:**
Preference for React, Java 25 with Spring Boot, fresh AWS set-up (will link this to an existing account later), Maven, postgres, liquibase,..
But if your architects decide on something else and have good reason for it, they can do that after consulting me first and after my approval.
---

## 9. Bet Feed Visibility
"Users can check the bets going on between their friends" — is this a passive feed (like a timeline), or active filtering/search?
Should users see bets they are not part of?

**Answer:**
If a user clicks on the account of a friend, they should be able to see their bets and the status of those bets.
---

## 10. Notifications — Fallback
If a user has no mobile device registered, should push notifications fall back to email or SMS?

**Answer:**
No