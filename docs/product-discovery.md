# iBetcha -- Product Discovery Report

**Date:** 2026-05-08
**Phase:** Discovery & Clarity (Phase 1)
**Status:** Complete -- ready for Phase 2 handoff

---

## Table of Contents

1. [Problem Validation](#1-problem-validation)
2. [Assumption Risk Register](#2-assumption-risk-register)
3. [Competitive Landscape](#3-competitive-landscape)
4. [Opportunity Areas](#4-opportunity-areas)
5. [MVP vs. Later Phases](#5-mvp-vs-later-phases)
6. [Requirements Issues](#6-requirements-issues)
7. [Recommendations](#7-recommendations)
8. [Discovery Summary](#8-discovery-summary)

---

## 1. Problem Validation

### The Core Problem

People make informal bets with friends constantly -- during sports events, at parties, over lunch, in group chats. These bets are forgotten, disputed, or never settled. There is no lightweight tool purpose-built for tracking casual social bets between friends.

### Why This Problem is Real

- **Behavioral evidence:** Informal betting is universal in friend groups. People already bet on WhatsApp, Snapchat, and in person -- they just lose track. The behavior exists; the tooling does not.
- **Friction in current workarounds:** Friends use group chats, notes apps, or pure memory. All of these fail at accountability, evidence, and settlement.
- **Social motivation:** Betting is inherently social and competitive. The desire to prove "I told you so" is a powerful driver. The existence of apps like Venmo's social feed demonstrates that people enjoy making financial interactions social.

### Problem Statement (Validated)

Friends regularly make informal bets with each other but lack a dedicated, fair, and fun way to track, verify, and settle those bets. The result is forgotten bets, disputed outcomes, and missed opportunities for social engagement.

### Risk Assessment

| Factor | Rating | Notes |
|---|---|---|
| Problem frequency | High | Informal bets happen weekly+ in active friend groups |
| Problem severity | Low-Medium | This is entertainment, not pain relief. Nobody suffers without this. |
| Willingness to adopt a new app | Medium | The bar for "yet another app" is very high. The bet must be worth opening a separate app for. |
| Existing workaround adequacy | Medium | WhatsApp + memory works "well enough" for most people |

**Key tension:** The problem is real but mild. This means the app must be significantly more fun, fast, and social than the workaround (group chat). If creating a bet takes longer than typing "bet you 5 euros I can chug this faster," adoption will fail.

---

## 2. Assumption Risk Register

Each assumption is scored: Impact (1-5) x Likelihood of being wrong (1-5) = Risk Score. Higher = test first.

| # | Assumption | Impact | Wrong? | Risk | Recommendation |
|---|---|---|---|---|---|
| A1 | Friends will download a separate app just for betting | 5 | 4 | **20** | CRITICAL. This is the #1 risk. If friends will not install the app, nothing else matters. Bet creation must be under 15 seconds or it loses to WhatsApp. |
| A2 | The jury role will feel natural, not burdensome | 4 | 4 | **16** | HIGH. Being asked to judge your friends' bets is either fun or annoying. If jury members ignore notifications, the entire bet lifecycle stalls. |
| A3 | Users will bother uploading evidence | 3 | 4 | **12** | HIGH. Taking and uploading a video after winning a bet adds friction. Many users will just want to declare a winner without proof. |
| A4 | Push notifications will be sufficient for engagement | 4 | 3 | **12** | HIGH. Users routinely disable push notifications for non-essential apps. Without a fallback, bet invitations go unseen. |
| A5 | Users want to browse friends' bets on their profiles | 3 | 3 | **9** | MEDIUM. Profile-based bet browsing is passive. A social feed might drive more engagement, but adds complexity. Test which model users prefer. |
| A6 | The bet lifecycle (create > accept > complete > jury approve) is not too complex | 4 | 2 | **8** | MEDIUM. Four steps to settle a simple bet is a lot. Compare to "I bet you 5 euros" in WhatsApp (one step). |
| A7 | Multi-language support is needed for MVP | 2 | 3 | **6** | LOW. English-only for internal MVP is fine. Multi-language adds translation maintenance overhead for zero MVP benefit. |
| A8 | Username search is sufficient for friend discovery | 3 | 2 | **6** | LOW. WhatsApp deep link mitigates this. But cold-start (no friends on platform) remains a challenge. |
| A9 | 50MB evidence limit is appropriate | 2 | 2 | **4** | LOW. 50MB handles most compressed videos. This seems reasonable. |
| A10 | OAuth (Google/Apple) will cover most login scenarios | 2 | 1 | **2** | LOW. Standard approach, well-understood. |

### Top 3 Assumptions to Test First

1. **A1 -- App adoption friction.** If it takes more than 15 seconds to create a bet, users will default to WhatsApp. The entire value proposition depends on being faster and more fun than typing in a group chat.
2. **A2 -- Jury burden.** The jury is a single point of failure in the bet lifecycle. If they don't respond, the bet hangs forever. Need a timeout/fallback mechanism.
3. **A3 -- Evidence upload friction.** Evidence should be optional, not required. Most casual bets ("who pays for dinner") don't need video proof.

---

## 3. Competitive Landscape

### Direct Competitors

| App | Description | Status | Lessons |
|---|---|---|---|
| **BetBuddy / BetMate** (various) | Social betting apps that have appeared and disappeared from app stores | Most are dead or abandoned | Signals that this space is hard to monetize and hard to retain users. Every generation of "bet with friends" apps has failed to achieve critical mass. |
| **Betfair / FanDuel / DraftKings** | Real-money sports betting platforms | Active, massive | These serve a fundamentally different need (gambling for profit vs. social fun). But they prove people will bet on their phones. |
| **Venmo / Tikkie** | Payment apps with social layers | Active, massive | Social payment feeds are popular. "Ben paid Sarah -- pizza bet" is how bets are settled today. Venmo's social feed is closer to iBetcha's value prop than any betting app. |

### Adjacent Competitors (the real threat)

| Product | Why it matters |
|---|---|
| **WhatsApp / iMessage group chats** | This is the #1 competitor. People already make bets here. iBetcha must be dramatically better than typing "I bet you..." in a chat. |
| **Snapchat / Instagram Stories** | Social proof and evidence sharing already happens here. "Video of me winning" gets posted to Stories, not to a betting app. |
| **Strava / fitness apps** | For physical challenges (running, cycling), these already track and compare performance with friends. |

### Key Competitive Insight

**iBetcha's real competitor is not another betting app -- it is group chat + memory.** Every design decision should be evaluated against: "Is this faster and more fun than just texting my friends?" If the answer is no, that feature will not drive adoption.

### What We Can Learn

1. **Speed kills friction.** Venmo succeeded because sending money is faster than cash. iBetcha must make creating a bet faster than typing one out.
2. **Social proof drives engagement.** Venmo's public feed, Strava's leaderboards -- people engage when they can see what friends are doing. A passive profile page may not be enough.
3. **Previous social betting apps failed.** This is sobering. The graveyard of "bet with friends" apps suggests the market is real but the execution bar is very high. The difference-maker is likely going to be UX speed and viral mechanics, not features.

---

## 4. Opportunity Areas

These are capabilities NOT in the current requirements that could significantly improve the product.

### High-Value Opportunities

**O1. Quick Bet / One-Tap Bet Creation**
- The current flow (title, description, participants, price, date, jury) is too many fields for a casual bet. Add a "Quick Bet" mode: pick a friend, type the bet, set the stake. Done in under 10 seconds.
- **Rationale:** Directly addresses the #1 adoption risk. Speed is everything.

**O2. Bet Templates**
- Pre-built templates for common bet types: "Who drinks fastest," "Game outcome," "Fitness challenge," "Dare." User picks template, fills in names and stakes.
- **Rationale:** Reduces creation friction, makes the app feel purpose-built.

**O3. Streak / Stats Tracking**
- Win/loss record against specific friends. "You are 7-3 against Sarah." Streak tracking ("3 wins in a row").
- **Rationale:** Creates long-term engagement beyond individual bets. Gives users a reason to come back even when no active bets exist.

**O4. Bet Reminder / Deadline Notifications**
- Automated reminders as bet deadlines approach. "Your bet with Ben expires tomorrow -- who won?"
- **Rationale:** Solves the "forgotten bet" problem without requiring manual action.

**O5. Social Feed (Reconsider Decision)**
- The current decision is profile-based bet browsing. A lightweight social feed ("Ben just challenged Sarah to...") would drive significantly more engagement and viral discovery.
- **Rationale:** Every successful social app has a feed. Profile-only browsing is passive and requires deliberate action.

### Medium-Value Opportunities

**O6. Reaction / Comment on Bets**
- Friends not involved in a bet can react (emoji) or comment. Turns bets into social moments.
- **Rationale:** Increases engagement surface area. Bystanders become participants in the social layer.

**O7. Group Bets (Tournament Style)**
- Multiple participants competing in a bracket or leaderboard format. "Who runs the fastest 5K this month?"
- **Rationale:** Natural extension of the core concept. Supports larger friend groups.

**O8. Bet History Export**
- Let users export or share their bet history. "My 2026 betting record with friends."
- **Rationale:** Year-in-review style engagement, shareable content.

### Position

**O1 (Quick Bet) is mandatory for MVP.** Without it, the app loses to WhatsApp on its first interaction. O3 (Streaks) and O4 (Reminders) are strong candidates for MVP as well -- they solve the retention problem that killed previous social betting apps.

---

## 5. MVP vs. Later Phases

### MVP (Must Have for Launch)

| Feature | Rationale |
|---|---|
| Registration + OAuth (Google, Apple) | Standard. Required for any user interaction. |
| Username search + WhatsApp deep link invite | Friend discovery is table stakes. Deep link solves cold-start. |
| Quick bet creation (simplified flow) | The #1 differentiator. Must be under 15 seconds to create a bet. |
| Full bet creation (all fields) | For users who want detailed bets. Available but not the default flow. |
| Accept / decline bet | Core lifecycle. |
| Mark bet complete + declare winner | Core lifecycle. |
| Jury approval of outcome | Core differentiator -- fairness mechanism. But needs timeout fallback (see below). |
| Evidence upload (optional, photo + video) | Optional is key. Do not require evidence for casual bets. |
| Push notifications (all lifecycle events) | Primary engagement mechanism. |
| Cancel / close bet (pre-acceptance only) | Basic lifecycle management. |
| User profile (picture, bio, bet history, win/loss record) | Identity and basic stats. |
| View friend's bets via their profile | Social browsing as specified in requirements. |
| Friends list management | Core social feature. |
| English language only | Multi-language is unnecessary complexity for an internal MVP. |

### Phase 2 (Post-MVP, Pre-Public Launch)

| Feature | Rationale |
|---|---|
| Social feed (activity timeline) | Revisit the profile-only decision before public launch. A feed drives discovery and engagement. |
| Bet templates | Reduce friction further based on observed bet patterns. |
| Bet reminders / deadline notifications | Automated engagement. Solve forgotten bets. |
| Win/loss streaks and detailed stats | Retention mechanism. |
| Modify bet (pre-acceptance) | Nice to have but adds lifecycle complexity. |
| Reactions / comments on bets | Social layer expansion. |
| Multi-language support | Required for public launch in non-English markets. |

### Phase 3 (Post-Public Launch)

| Feature | Rationale |
|---|---|
| Real money payments (Stripe, Apple Pay) | Significant compliance, legal, and regulatory work. Only after product-market fit is proven. |
| Group bets / tournaments | Feature expansion after core loop is validated. |
| Bet history export / year-in-review | Engagement feature, not core. |
| Phone contact integration for friend discovery | Privacy implications, but useful at scale. |

### What I Recommend Cutting from MVP (Deferred)

1. **Bet modification (edit after creation).** The requirement says bets can be changed pre-acceptance with participant approval. This creates a complex notification-and-approval loop for an edge case. For MVP, if you want to change a bet, cancel it and create a new one. Ship modification later.
2. **Multi-language support infrastructure.** Ship English-only. Internationalize the codebase (use resource bundles, externalize strings), but do not translate anything for MVP.

---

## 6. Requirements Issues

### Contradictions and Ambiguities

**Issue 1: Jury Eligibility -- AMBIGUOUS (Flagged in Questions.md)**

The requirement states: "The jury can not be a participant in the bet, but they can be a friend of one of the participants."

Questions.md confirms jury must be a registered user, but the eligibility rule is marked as ambiguous -- whether the jury must be a friend of a participant or just any registered non-participant.

**Position:** The jury should be ANY registered non-participant. Requiring friendship adds unnecessary complexity and creates scenarios where no eligible jury exists. The creator should be able to pick any registered user they trust.

**Issue 2: Bet Acceptance Flow -- VAGUE**

Requirement: "The jury needs to approve the bet before it can be accepted by the participants."

This means the jury must approve the bet *creation* before participants can accept? This adds a pre-acceptance gate that could stall bet creation for hours or days. If the jury is slow, the moment passes.

**Position:** Split jury involvement into two roles:
- **Bet creation:** No jury approval needed. Creator sets up the bet, participants accept or decline immediately. Jury is designated but passive at this stage.
- **Bet completion:** Jury approves the *outcome* -- who won. This is where the jury adds value (fairness, dispute resolution).

If the original intent is that the jury must approve the bet terms before it goes live, this needs explicit confirmation because it significantly impacts UX speed.

**Issue 3: "All Participants Accepted" as a Gate -- UNCLEAR**

"A bet can be canceled or closed by the creator, but only if it has not been accepted by all participants yet."

What happens when 3 out of 4 participants have accepted? The bet is in limbo. The creator cannot cancel (some have accepted), and the bet is not active (not all accepted). What if participant #4 never responds?

**Position:** Add a bet expiration timer. If not all participants accept within 48 hours (configurable), the bet expires automatically. Participants who accepted are notified. This prevents zombie bets.

**Issue 4: Evidence as Proof vs. Entertainment -- UNCLEAR**

The requirement says "user needs to provide data like who won the bet and possible evidence (picture or movie)." Is evidence required or optional?

**Position:** Evidence must be optional. Most casual bets (who pays for dinner, who picks the restaurant) do not have photographic evidence. Requiring it would kill the completion flow. Evidence should be encouraged but never mandated.

**Issue 5: No-Jury Fallback -- UNDERSPECIFIED**

"If no jury is appointed, all participants need to approve the bet before it can be accepted."

This is about bet creation approval without a jury. But what about *completion* without a jury? If no jury is appointed, who approves the outcome? All participants? What if the loser refuses to approve?

**Position:** If no jury, the outcome requires majority approval from participants (not unanimous). Unanimous outcome approval gives the loser veto power, which creates a broken incentive ("I refuse to approve that I lost"). Alternatively, if the bet has no jury and participants dispute the outcome, the bet should have a "disputed" status that is visible on profiles.

**Issue 6: Push Notification as Single Channel -- RISKY**

The decision is mobile push only, no fallback. Push notification delivery is not guaranteed (disabled notifications, battery optimization, app uninstalled). For bet invitations and outcome approvals, a missed notification means a stalled lifecycle.

**Position:** Accept this for MVP, but design the notification system with a provider abstraction so email/SMS can be added later without architectural changes. Track notification delivery rates from day 1 -- if delivery drops below 80%, add a fallback channel immediately.

### Missing Requirements

| Gap | Impact | Recommendation |
|---|---|---|
| No bet expiration / timeout | Bets can hang forever if participants or jury do not respond | Add configurable expiration (default 48h for acceptance, 7 days for jury decision) |
| No dispute resolution | What happens when participants disagree about the outcome and there is no jury? | Add "disputed" status. For MVP, disputed bets are visible but unresolved. |
| No blocking/reporting | Social apps need safety mechanisms | Add user blocking for MVP. Reporting can wait for public launch. |
| No account deletion | GDPR and app store requirement | Must be in MVP. Apple and Google require account deletion. |
| No terms of service / privacy policy | Required for app store submission | Must exist before any app store submission. |
| No onboarding flow | New users need to understand the concept | Add a 3-screen onboarding: (1) Create a bet (2) Friends judge fairly (3) Track your record |
| No empty states | What does the app look like with zero bets and zero friends? | Design empty states that guide users to invite friends and create first bet |

---

## 7. Recommendations

### Strategic Recommendations

1. **Make bet creation absurdly fast.** This is the product's survival requirement. If creating a bet is slower than typing in WhatsApp, the app dies. Target: under 10 seconds for a quick bet. This means a "Quick Bet" flow with minimal fields (friend, bet description, stake) must be the primary creation path.

2. **Make the jury role lightweight.** The jury should receive a notification with a simple approve/reject action. No app opening required if possible (use actionable push notifications). If the jury does not respond within a configurable window (default 48h), auto-approve the outcome or escalate to participant vote.

3. **Plan for the social feed.** The current decision (profile-based browsing) is safe for MVP, but I expect it will be insufficient for retention. Architect the backend to support a timeline/feed query pattern from day 1, even if the UI ships as profile-based. Switching to a feed later should be a frontend change, not a data model change.

4. **Solve the cold-start problem.** The app is useless without friends on the platform. The WhatsApp deep link is good. Consider also: (a) allow bet creation with non-users (they receive an invite link and can claim the bet upon registration), and (b) pre-populate the first bet experience with a walkthrough bet against the app itself.

5. **Do not invest in real payments for MVP.** The compliance, legal, and regulatory work for money transmission is enormous and varies by jurisdiction. Keep it informal ("Ben owes Sarah 10 euros") and validate product-market fit first. If informal tracking shows that 60%+ of bets involve money, then payments become a Phase 3 priority.

6. **Instrument everything.** From day 1, track: bet creation time (seconds), bet completion rate (created vs. settled), jury response time, notification delivery/open rates, user retention (D1/D7/D30). These metrics will tell you whether the core loop works before you need customer interviews.

### Technical Recommendations for Product Decisions

1. **Actionable push notifications.** iOS and Android both support notification actions (buttons on the notification itself). "Accept Bet" / "Decline Bet" without opening the app would dramatically improve bet acceptance rates.

2. **Deep link architecture.** The WhatsApp invite already requires deep linking. Extend this to all bet-related sharing: "Share this bet" generates a deep link that non-users can tap to install and see the bet. This is the viral loop.

3. **Offline-first for bet viewing.** Users should be able to browse their bets, friends, and history without network. Only creation, acceptance, and evidence upload require connectivity.

---

## 8. Discovery Summary

### Go / No-Go Assessment

**GO -- with conditions.**

The problem is real (people make informal bets constantly), the solution direction is sound (mobile app with social features), and the technical decisions are pragmatic (React Native, Spring Boot, PostgreSQL, AWS).

**Conditions for success:**

1. Bet creation speed must be under 15 seconds or the app loses to WhatsApp.
2. The jury mechanism needs a timeout fallback or it becomes a lifecycle blocker.
3. Evidence must be optional or completion rates will tank.
4. Account deletion and privacy policy must be in MVP for app store compliance.
5. The cold-start problem (no friends on platform) must be solved with deep link invites and potentially allowing bets with non-users.

### Biggest Risk

**Assumption A1: Will people download and use a separate app for something they currently do in group chat?**

This is an execution risk, not a market risk. The market exists (people bet informally). The question is whether iBetcha provides enough incremental value over WhatsApp to justify a new app install. The answer depends entirely on speed of bet creation, quality of the social experience, and the satisfaction of having a permanent record of wins.

### What Makes This Succeed or Fail

| Success factor | Metric | Target |
|---|---|---|
| Bet creation speed | Time from app open to bet sent | Under 15 seconds |
| Bet completion rate | % of created bets that reach a settled outcome | Above 60% |
| Friend invitation conversion | % of WhatsApp invites that convert to registrations | Above 20% |
| Weekly active retention | % of users creating or participating in 1+ bet per week | Above 30% at D30 |
| Jury response rate | % of jury notifications responded to within 48h | Above 70% |

---

*This discovery report is ready for handoff to Phase 2 (UX & Product Definition). The product owner should use this as input for user journey mapping and story creation, paying particular attention to the assumption risk register and the MVP scope recommendations.*
