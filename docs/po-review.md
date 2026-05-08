# iBetcha -- Product Owner Output Review

**Reviewer:** Product Owner Reviewer (Hard Gate)
**Date:** 2026-05-08
**Document reviewed:** `docs/product-owner-output.md`
**Reference documents:** `docs/phase1-decisions.md`, `Requirements.md`, `docs/product-discovery.md`
**Gate:** Pre-Architecture (Phase 2 to Phase 3 handoff)

---

## VERDICT: PASS

The PO output is thorough, well-structured, and substantially compliant with the locked Phase 1 decisions. The 30 user stories across 12 epics cover all MVP features. The BDD scenarios are concrete and testable. Journey maps cover the critical user flows. No critical blockers were found.

There are several warnings and observations below that should be addressed before or during development, but none rise to the level of blocking the architecture handoff.

---

## Critical Issues

**None found.** The document clears the hard gate.

---

## Warnings (Should Fix, Do Not Block)

### W1: Jury role scope -- stories correctly limit jury to outcomes, but wording needs vigilance

Phase 1 decisions state: "Jury does NOT approve bet creation. Jury is passive at creation." The PO output correctly implements this -- US-501 covers jury approval of outcomes only, and US-302 correctly shows the jury as a passive designation at creation time (the jury is notified of their role but does not approve creation). However, the notification in US-302 scenario 3 says "Alex receives a push notification informing him he has been designated as jury for the bet." The architect and developers must ensure this notification is purely informational and does not imply a required action at creation time. The acceptance criteria in US-302 say "Jury is notified of their designation" -- this is correct but should be clarified during development to mean a passive, FYI-only notification.

**Recommendation:** Add an explicit note to US-302 AC or the system constraints clarifying that the jury designation notification at creation time is informational only, with no approve/reject action required.

### W2: Evidence toggle in Full Bet Creation may cause confusion

US-302 includes an "evidence required" toggle in the Full Bet form. Phase 1 decisions state: "Evidence (photo/video) is OPTIONAL for all bets." The toggle appears to let a creator make evidence mandatory for a specific bet. This is not explicitly prohibited by phase1-decisions.md (which says evidence is optional at the system level), but it creates a tension: if a creator toggles "evidence required = on," then evidence is no longer optional for that bet. This could be interpreted as violating the spirit of the decision.

**Recommendation:** Clarify whether the toggle means "I want to remind participants to upload evidence" (soft nudge) or "evidence is required to complete this bet" (hard gate). If it is a hard gate, confirm this is acceptable given the phase1-decisions.md stance. Alternatively, relabel the toggle to "Request evidence" (soft) rather than "Evidence required" (hard).

### W3: No explicit story for bet history view

Phase 1 decisions include "User profile (picture, bio, bet history, win/loss record, streaks, head-to-head)" in the MVP scope. US-801 (Own Profile) mentions "Recent bets section: last 10 resolved bets with outcome" in the acceptance criteria, and US-802 (Friend Profile) mentions "Recent resolved bets visible." However, there is no story for viewing a comprehensive bet history (all past bets, not just the last 10). The profile screens only show recent bets. A user with 50+ resolved bets has no way to browse their full history.

**Recommendation:** Either expand US-801 acceptance criteria to include a "View all bets" list accessible from the profile (paginated), or explicitly scope "bet history" as "recent 10 bets on profile" for MVP and note that full history browsing is deferred.

### W4: "View friend's bets" coverage is thin

Phase 1 decisions list "View friend's bets via their profile" as an MVP feature. US-802 covers this, but the acceptance criteria say "Active bets shown without stakes" and "Recent resolved bets visible." This means a user can see a friend's active bets and recent resolved bets, but there is no way to see a friend's pending bets or expired/cancelled bets. The coverage is adequate for MVP but could be more explicit about what "view friend's bets" includes and excludes.

**Recommendation:** Add a clarifying note in US-802 defining the scope of "friend's bets" visibility: active bets (without stakes), recent resolved bets (with outcomes). Explicitly state that pending, expired, and cancelled bets are not visible to friends.

### W5: Rivalry notifications mentioned in US-701 but lack dedicated scenarios

Phase 1 decisions include rivalry-style notifications as part of the Bragging Rights direction ("You and Tom are now tied 4-4!"). US-701 mentions this in the domain examples and has one BDD scenario for tied records. US-406 also mentions rivalry notifications in its acceptance criteria. However, the scenarios only cover the "tied record" case. Other rivalry notifications mentioned in phase1-decisions.md (like streak milestones) are referenced in US-406 ("streak: 2W notification") but not fully specified with dedicated BDD scenarios.

**Recommendation:** Add 1-2 more BDD scenarios to US-701 or US-406 covering streak milestone notifications (e.g., "3-bet winning streak!") to ensure these are testable.

### W6: US-803 Edit Profile has only 2 BDD scenarios

The Definition of Ready checklist shows US-803 has only 2 BDD scenarios. While the story is small and straightforward, the review standard across the document is 3+ scenarios per story. Missing scenarios might include: bio character limit enforcement, profile photo upload failure, or display name with edge-case characters.

**Recommendation:** Add 1-2 more BDD scenarios for edge cases (bio at max length, photo upload failure).

---

## Observations (Nice to Know)

### O1: Strong alignment with Bragging Rights direction

The PO output effectively weaves the Bragging Rights direction throughout the stories. Head-to-head records appear in bet acceptance (US-401), friend profiles (US-802), and bet resolution (US-406). Win Cards (US-901) are a first-class feature. Rivalry notifications are included. Streak tracking is baked into profile stats. The document demonstrates clear understanding of the design direction.

### O2: Quick Bet flow is well-specified with timing constraints

Journey 2 and US-301 are among the strongest parts of the document. The step-by-step timing (0-14 seconds), the three required fields, and the explicit constraint that "validation errors must NOT reset the form" show a deep understanding of the speed requirement. This gives the architect clear constraints to work with.

### O3: Dispute resolution is comprehensive

The dispute flow (Journey 8, US-1101, US-1102) correctly implements the phase1-decisions.md rules: majority approval for 3+ person bets, disputed state for 2-person bets, and fallback from jury timeout to participant vote. The edge cases (both claim victory, deadlock, concession, appointing jury mid-dispute) are well-covered.

### O4: Build sequence is practical

The suggested 7-week build sequence follows a walking skeleton approach, which aligns with the sprint delivery model. Phase A delivers the minimum end-to-end lifecycle. Compliance items (block, delete, ToS) are correctly placed last but not forgotten.

### O5: Screen inventory is thorough at 37 screens

The screen inventory covers all journeys and stories. The distinction between bet detail states (pending, active, resolved, disputed, expired, cancelled) shows careful attention to the bet lifecycle state machine. This will be valuable for the architect and UI designer.

### O6: The document correctly defers bet modification

Phase 1 decisions state that bet modification is deferred to Phase 2 ("cancel and recreate for MVP"). The PO output correctly omits any bet modification story and the cancel-and-recreate approach is implicit in US-404 (Cancel Bet).

### O7: Empty states are addressed

Phase 1 decisions include "Empty states" in the MVP scope. The PO output addresses this in US-1201 (Home Screen empty state), US-204 (Friends List empty state), and US-801 (Profile empty state for new users). Empty states are embedded within relevant stories rather than being a standalone story, which is a reasonable approach.

---

## Coverage Matrix

Cross-reference of every MVP feature from `phase1-decisions.md` against story coverage in the PO output.

| # | MVP Feature (phase1-decisions.md) | Story Coverage | Status |
|---|---|---|---|
| 1 | Registration + OAuth (Google, Apple) | US-101 (OAuth Registration), US-102 (Login) | COVERED |
| 2 | Username search + WhatsApp deep link friend invite | US-201 (Search Friends), US-203 (WhatsApp Invite) | COVERED |
| 3 | Quick Bet creation (simplified flow) | US-301 (Quick Bet Creation) | COVERED |
| 4 | Full Bet creation (all fields, secondary path) | US-302 (Full Bet Creation) | COVERED |
| 5 | Accept / decline bet | US-401 (Accept Bet), US-402 (Decline Bet) | COVERED |
| 6 | Mark bet complete + declare winner | US-405 (Declare Winner) | COVERED |
| 7 | Jury approval of outcomes only | US-501 (Jury Reviews Outcome) | COVERED |
| 8 | Optional evidence upload (photo + video) | US-601 (Upload Evidence) | COVERED |
| 9 | Push notifications (all lifecycle events) | US-701 (Push Notifications) | COVERED |
| 10 | Cancel / close bet (pre-acceptance only) | US-404 (Cancel Bet) | COVERED |
| 11 | User profile (picture, bio, bet history, win/loss record, streaks, head-to-head) | US-801 (Own Profile), US-803 (Edit Profile), US-104 (Profile Setup) | COVERED (see W3 re: full history) |
| 12 | View friend's bets via their profile | US-802 (Friend Profile) | COVERED (see W4) |
| 13 | Friends list management | US-204 (Friends List Management) | COVERED |
| 14 | Shareable Win Cards | US-901 (Win Card) | COVERED |
| 15 | Bet timeouts (48h acceptance, 7d jury) | US-403 (48h Timeout), US-502 (7d Jury Timeout) | COVERED |
| 16 | Majority outcome approval (no-jury flow) | US-1101 (Majority Approval), US-1102 (Two-Person Dispute) | COVERED |
| 17 | Account deletion | US-1002 (Delete Account) | COVERED |
| 18 | Privacy policy + Terms of Service | US-1003 (Privacy/ToS) | COVERED |
| 19 | User blocking | US-1001 (Block User) | COVERED |
| 20 | Onboarding flow | US-103 (Onboarding Walkthrough) | COVERED |
| 21 | Empty states | US-1201 (Home Screen), US-204 (Friends), US-801 (Profile) -- embedded in relevant stories | COVERED |
| 22 | English language only | System Constraints section | COVERED |

### Phase 1 Decision Compliance

| Decision | Compliance | Notes |
|---|---|---|
| Jury only approves outcomes, NOT creation | COMPLIANT | US-501 covers outcome approval only. US-302 designates jury at creation but requires no approval action. See W1 for minor wording vigilance. |
| Majority approval for no-jury disputes | COMPLIANT | US-1101 implements majority vote. US-1102 handles 2-person edge case with DISPUTED state. |
| Evidence is optional | COMPLIANT | US-601 explicitly states evidence is optional. See W2 regarding the "evidence required" toggle in US-302. |
| Bet timeouts (48h acceptance, 7d jury) | COMPLIANT | US-403 (48h), US-502 (7d) with reminders at appropriate intervals. |
| Quick Bet under 15 seconds | COMPLIANT | US-301 specifies 3-field flow, 15-second target, with step-by-step timing in Journey 2. |
| Bragging Rights direction (reputation, stats, head-to-head, win cards) | COMPLIANT | US-801/802 (profiles with stats, streaks, head-to-head), US-901 (Win Cards), US-406 (rivalry notifications). Thoroughly integrated. |

### Journey Map Coverage

| Journey | Primary Flows Covered | Gaps |
|---|---|---|
| J1: First-Time Onboarding | Install to first bet, deep link, OAuth, onboarding, empty state | None |
| J2: Quick Bet Creation | 15-second flow, FAB, 3 fields, defaults | None |
| J3: Full Bet Creation | Expanded form, jury selection, deadline, preview | None |
| J4: Accept/Decline Bet | Push notification actions, in-app review, multi-person, head-to-head | None |
| J5: Complete Bet | Declare winner, evidence, jury review, no-jury vote, Win Card | None |
| J6: View Friend Profile | Stats, head-to-head, active bets, challenge button | None |
| J7: Friend Invitation | Deep link generation, WhatsApp share, deferred deep linking, auto-connect | None |
| J8: Dispute Flow | 2-person dispute, concession, appoint jury, 3+ majority | None |

**Missing journeys (not critical, but noted):**
- No dedicated journey for "returning user daily check-in" (covered implicitly by US-1201 Home Screen)
- No dedicated journey for "blocking a user" (covered by US-1001 but no journey map)
- No dedicated journey for "deleting account" (covered by US-1002 but no journey map)

These are secondary flows and their absence from journey maps is acceptable. The 8 journeys cover all primary MVP flows.

---

## Summary

The PO output is production-quality work. 30 stories across 12 epics cover all 22 MVP features from phase1-decisions.md with no gaps. All locked decisions are respected. Journey maps cover the 8 critical user flows. BDD scenarios are specific, testable, and cover both happy paths and edge cases. The Definition of Ready checklists confirm INVEST compliance. The screen inventory of 37 screens is comprehensive.

The 6 warnings above are refinement-level items that can be addressed during sprint planning or the first sprint. None represent architectural risk or missing functionality.

**This document is approved for handoff to the Architecture phase.**
