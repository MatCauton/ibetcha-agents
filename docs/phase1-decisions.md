# iBetcha — Phase 1 Decisions (Confirmed 2026-05-08)

These decisions are locked and should guide all downstream agents.

---

## Design Direction

**Direction B: Bragging Rights Platform**

iBetcha is a bragging-rights platform where the bet is the means and your reputation is the product.
Build the structured bet lifecycle as the backbone, layer reputation and social proof as the differentiator.

Core elements:
- Win/loss records, streaks, head-to-head stats per friend
- Shareable "Win Cards" after bet resolution (designed for WhatsApp/Instagram sharing)
- Profile that shows your betting reputation
- Rivalry-style notifications ("You and Tom are now tied 4-4!")

---

## Requirements Amendments

### Jury Role — CHANGED
- Jury does NOT approve bet creation. Jury is passive at creation.
- Jury only approves bet OUTCOMES (who won).
- Bet goes live as soon as all participants accept — no jury gate at creation.

### Dispute Resolution — NEW
- When no jury is appointed, outcome requires MAJORITY approval from participants (not unanimous).
- This prevents loser veto power.
- For 2-person bets with no jury: if both participants claim they won, outcome is disputed until they agree or appoint a jury.

### Bet Timeouts — NEW
- 48 hours for participants to accept/decline a bet invitation. After timeout, bet expires.
- 7 days for jury to approve an outcome. After timeout, auto-approve or escalate to participant vote.

### Evidence — CLARIFIED
- Evidence (photo/video) is OPTIONAL for all bets.
- On-device compression before upload.
- Max 50MB per evidence item.

### Quick Bet Mode — NEW
- Primary bet creation path: pick a friend, type the bet, set the stake. Under 15 seconds.
- Full bet creation (all fields) available but secondary.

---

## MVP Scope

### In MVP
- Registration + OAuth (Google, Apple)
- Username search + WhatsApp deep link friend invite
- Quick Bet creation (simplified flow)
- Full Bet creation (all fields, secondary path)
- Accept / decline bet
- Mark bet complete + declare winner
- Jury approval of outcomes only
- Optional evidence upload (photo + video)
- Push notifications (all lifecycle events)
- Cancel / close bet (pre-acceptance only)
- User profile (picture, bio, bet history, win/loss record, streaks, head-to-head)
- View friend's bets via their profile
- Friends list management
- Shareable Win Cards
- Bet timeouts (48h acceptance, 7d jury)
- Majority outcome approval (no-jury flow)
- Account deletion
- Privacy policy + Terms of Service
- User blocking
- Onboarding flow
- Empty states
- English language only

### Deferred to Phase 2
- Bet modification (cancel and recreate for MVP)
- Social feed / activity timeline
- Bet templates
- Bet reminders / deadline notifications
- Detailed stats & badges
- Reactions / comments on bets
- Multi-language support

### Deferred to Phase 3
- Real money payments (Stripe, Apple Pay)
- Group bets / tournaments
- Bet history export / year-in-review
- Phone contact integration for friend discovery
- Public profiles / discoverability beyond friends

---

## Technical Decisions (Confirmed)

- **Mobile:** React Native (Expo vs bare TBD by architect)
- **Backend:** Java 25 + Spring Boot
- **Database:** PostgreSQL + Liquibase
- **Build:** Maven
- **Cloud:** AWS (fresh setup)
- **Evidence storage:** AWS S3
- **Compression:** On-device

Architect may propose alternatives with user approval.

---

## Key Metrics (Success Criteria)

| Metric | Target |
|---|---|
| Bet creation time | Under 15 seconds |
| Bet completion rate | Above 60% |
| Friend invitation conversion | Above 20% |
| Weekly active retention (D30) | Above 30% |
| Jury response rate (within 48h) | Above 70% |
