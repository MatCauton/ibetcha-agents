<!-- markdownlint-disable MD024 -->
# iBetcha -- Product Owner Output

**Date:** 2026-05-08
**Design Direction:** B -- Bragging Rights Platform
**Phase:** DISCUSS (UX Journey Maps + BDD User Stories)
**Status:** Ready for architecture and development handoff

---

## Table of Contents

1. [User Journey Maps](#1-user-journey-maps)
2. [BDD User Stories by Epic](#2-bdd-user-stories-by-epic)
3. [Screen Inventory](#3-screen-inventory)
4. [Definition of Ready Checklist](#4-definition-of-ready-checklist)

---

## 1. User Journey Maps

### Journey 1: First-Time User Onboarding (Install to First Bet)

**Persona:** Maria Santos, 27, social runner, heard about iBetcha from her friend Tom who sent a WhatsApp invite link.

**Emotional Arc:** Curious --> Cautious --> Welcomed --> Empowered --> Excited

```
  INSTALL           OPEN APP         REGISTER          ONBOARDING          FRIEND FOUND        FIRST BET
  --------         ----------       ----------         -----------         ------------        ----------
  | Tap link |---->| Splash   |---->| Sign up  |----->| 3 screens |----->| Tom added  |----->| Quick Bet |
  | from     |     | screen   |     | Google/  |      | - Create  |      | as friend  |      | created!  |
  | WhatsApp |     | iBetcha  |     | Apple/   |      | - Judge   |      | (auto from |      | Waiting   |
  |          |     | logo     |     | email+pw |      | - Brag    |      |  deep link)|      | for Tom   |
  ----------       ----------       ----------         -----------         ------------        ----------
  Emotion:         Emotion:         Emotion:           Emotion:           Emotion:            Emotion:
  Curious          Intrigued        Cautious           "Oh, I get it"     Connected           Excited
  "What's this?"   "Clean, fast"    "Is this safe?"    "This is fun"      "Tom's here!"       "Game on!"
```

**Steps:**

| Step | Action | Screen | Emotion | Pain Points | Delight Moments |
|------|--------|--------|---------|-------------|-----------------|
| 1 | Taps WhatsApp deep link | App Store / Play Store | Curious | Must download a new app -- friction | Link pre-fills friend connection |
| 2 | Opens app for first time | Splash screen | Intrigued | None -- must be instant | Clean branding, fast load |
| 3 | Registers account | Registration screen | Cautious | Password requirements, form fatigue | OAuth (Google/Apple) = 1 tap |
| 4 | Sets up profile | Profile setup (name, picture) | Invested | Choosing a username, photo upload | Optional photo -- low friction |
| 5 | Sees onboarding carousel | 3-screen walkthrough | Educated | Skippable -- some users will skip | Shows the value: create, judge, brag |
| 6 | Auto-connected to referrer | Friends list (with Tom) | Connected | If deep link fails, empty friends list | Instant social connection -- not alone |
| 7 | Sees empty state on home | Home screen (no bets) | Slightly lost | "What do I do now?" | CTA: "Challenge Tom to your first bet!" |
| 8 | Creates first quick bet | Quick Bet creation | Excited | First time -- may not know what to bet | Pre-filled suggestion: "Who buys coffee next?" |
| 9 | Bet sent, waiting for Tom | Home screen (1 pending bet) | Anticipation | Waiting is boring | Notification sent to Tom, status visible |

**Key Design Decisions:**
- Deep link MUST auto-add the referrer as a friend after registration
- Onboarding is 3 screens max, skippable, focused on emotional hook not feature list
- Empty state on home screen must guide to first bet creation with a prominent CTA
- Quick Bet is the default creation flow -- not the full form

---

### Journey 2: Quick Bet Creation (Under 15 Seconds)

**Persona:** Tom Janssen, 30, competitive cyclist, wants to bet Maria he will beat her on the next weekend ride.

**Emotional Arc:** Impulse --> Action --> Anticipation --> Satisfaction

```
  HOME SCREEN       PICK FRIEND       TYPE BET          SET STAKE         SEND!
  -----------       -----------       ----------        ----------        --------
  | [+] Bet  |---->| Select    |---->| "I'll beat|---->| Loser buys|---->| Bet sent |
  | button   |     | Maria     |     |  you on   |     | coffee    |     | to Maria |
  | (FAB)    |     | (friends  |     |  Sunday's |     |           |     |          |
  |          |     |  list)    |     |  ride"    |     | [No jury] |     | [Done!]  |
  -----------       -----------       ----------        ----------        --------
  Time: 0s         Time: 2s          Time: 7s          Time: 12s         Time: 14s
  Emotion:         Emotion:          Emotion:          Emotion:          Emotion:
  Impulsive        Targeting         Crafting          Sealing deal      Triumphant
```

**Steps (timed):**

| Step | Time | Action | Interaction | Notes |
|------|------|--------|-------------|-------|
| 1 | 0-2s | Tap FAB "+" button on home | Single tap | Always visible, prominent |
| 2 | 2-4s | Select friend from list | Tap on Maria's avatar | Recent friends shown first, search available |
| 3 | 4-9s | Type bet description | Free text, 1 line | "I'll beat you on Sunday's ride" |
| 4 | 9-12s | Set stake | Free text | "Loser buys coffee" |
| 5 | 12-13s | Optional: set deadline, add jury | Skip for quick bet | Collapsed by default |
| 6 | 13-14s | Tap "Send Bet" | Single tap | Confirmation with haptic feedback |

**Critical Constraints:**
- Quick Bet requires only: 1 friend + description + stake = 3 fields
- No jury required (defaults to participant majority approval)
- No deadline required (defaults to 48h acceptance window)
- No evidence requirement
- Total flow: 4 taps + 2 text inputs = under 15 seconds
- Validation errors must NOT reset the form

---

### Journey 3: Full Bet Creation (All Fields)

**Persona:** Sarah De Vries, 25, organizes group challenges, wants to set up a structured bet with multiple friends about who runs a 5K fastest this weekend.

**Emotional Arc:** Intentional --> Methodical --> Thorough --> Confident

```
  HOME         CREATE BET       ADD FRIENDS      SET DETAILS       SET JURY        REVIEW & SEND
  ------       ----------       -----------      -----------       --------        -------------
  | [+] |---->| Full Bet  |---->| Sarah adds|---->| Title,    |---->| Pick    |---->| Preview   |
  |     |     | (toggle   |     | Tom, Maria|     | desc,     |     | Alex as |     | all fields|
  |     |     |  to full  |     | Ben       |     | stake,    |     | jury    |     | [Send]    |
  |     |     |  form)    |     |           |     | deadline  |     |         |     |           |
  ------       ----------       -----------      -----------       --------        -------------
```

**Fields Available:**

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| Friends (participants) | Yes | -- | Multi-select from friends list |
| Bet description | Yes | -- | Free text |
| Stake | Yes | -- | Free text (beer, pizza, 10 euros, etc.) |
| Title | No | Auto-generated from description | Short label for the bet |
| Deadline | No | 48h acceptance, no completion deadline | Date picker |
| Jury | No | Majority approval among participants | Single friend (non-participant) |
| Evidence required | No | Optional | Toggle |

**Key Design Decisions:**
- Full form is accessible via a "More options" toggle from Quick Bet
- Quick Bet and Full Bet are NOT separate screens -- Full Bet expands from Quick Bet
- Preview/review screen before sending (full form has more fields = more to verify)
- Multi-friend selection with avatars, not a text list

---

### Journey 4: Receiving and Accepting/Declining a Bet

**Persona:** Maria Santos receives Tom's cycling bet via push notification.

**Emotional Arc:** Surprised --> Intrigued --> Evaluating --> Committed (or Relieved if declined)

```
  PUSH                OPEN APP            BET DETAIL           ACCEPT              HOME
  NOTIFICATION        (or tap notif)      SCREEN               (or DECLINE)        SCREEN
  -----------         ----------          -----------          ---------           --------
  | "Tom bet  |----->| Deep link |------->| See terms |----->| [Accept] |-------->| Bet now  |
  |  you can't|      | to bet    |        | Tom vs You|      | or       |         | in Active|
  |  beat him"|      | detail    |        | Stake:    |      | [Decline]|         | Bets list|
  |           |      |           |        | coffee    |      |          |         |          |
  | [Accept]  |      |           |        | Deadline: |      |          |         | "You vs  |
  | [Decline] |      |           |        | Sunday    |      |          |         |  Tom: 3-2"|
  -----------         ----------          -----------          ---------           --------
  Emotion:           Emotion:            Emotion:             Emotion:            Emotion:
  Surprised,         Engaged             Evaluating           Committed!          Fired up
  flattered                              "Can I win this?"    (or relieved)
```

**Steps:**

| Step | Action | Interaction | Emotion |
|------|--------|-------------|---------|
| 1 | Receives push notification | Actionable notification with Accept/Decline buttons | Surprised, intrigued |
| 2a | Taps Accept on notification | No app open needed -- actionable push | Quick, decisive |
| 2b | OR taps notification body | Opens app to bet detail screen | Wants more info |
| 3 | Reviews bet details | Sees description, stake, participants, deadline, jury (if any) | Evaluating |
| 4 | Sees head-to-head record | "You vs Tom: 3 wins, 2 losses" | Competitive fire |
| 5a | Taps Accept | Bet moves to Active, all participants notified | Committed, excited |
| 5b | OR taps Decline | Bet removed from view, creator notified | Relieved, no guilt |
| 6 | Returns to home | Active bet visible in list with countdown/status | Anticipation |

**Key Design Decisions:**
- Actionable push notifications (Accept/Decline without opening app) are critical for speed
- Bet detail screen shows head-to-head record to fuel competitive motivation
- Declining is guilt-free -- no shaming, simple confirmation
- 48h timeout: if no response, bet expires automatically. Reminder at 24h.
- When all participants accept, everyone gets a "Bet is ON!" notification

---

### Journey 5: Completing a Bet (Declare Winner, Evidence, Jury)

**Persona:** Tom Janssen won the cycling bet against Maria. He wants to declare victory and share the result.

**Emotional Arc:** Triumphant --> Documenting --> Awaiting Judgment --> Celebrating

```
  ACTIVE BET         DECLARE WINNER     ADD EVIDENCE        JURY REVIEW         RESOLVED!
  ----------         --------------     ------------        -----------         ---------
  | Tap bet  |----->| Select     |---->| Optional:  |----->| Alex reviews|---->| WIN CARD |
  | "Sunday  |     | winner:    |     | Upload     |      | Approves    |     | generated|
  |  ride"   |     | [Tom] won  |     | photo of   |      | outcome     |     | Share to |
  |          |     |            |     | finish line|      |             |     | WhatsApp!|
  ----------         --------------     ------------        -----------         ---------
  Emotion:          Emotion:           Emotion:            Emotion:            Emotion:
  Eager to          Triumphant         Proud, proving      Slightly anxious    CELEBRATION
  close this        "I told you!"      it happened         "Will Alex agree?"  "I won!"
```

**Steps:**

| Step | Action | Screen | Notes |
|------|--------|--------|-------|
| 1 | Open active bet | Bet detail (active state) | "Mark Complete" button visible |
| 2 | Tap "Mark Complete" | Winner selection | Select self or other participant as winner |
| 3 | Optionally upload evidence | Camera / gallery picker | Photo or video, max 50MB, on-device compression |
| 4a | If jury assigned: jury notified | Jury review screen (for jury) | Jury sees bet, winner claim, evidence. Approve/Reject. |
| 4b | If no jury: participants vote | Majority approval flow | Other participants confirm or dispute |
| 5 | Outcome approved | Bet resolved screen | Win/loss records updated, streak updated |
| 6 | Win Card generated | Shareable card screen | Pre-designed card with bet details, winner, evidence thumbnail |
| 7 | Share Win Card | Share sheet (WhatsApp, Instagram, etc.) | Deep link back to iBetcha embedded |

**Jury Flow Detail:**
- Jury receives push notification: "Tom claims he won the cycling bet vs Maria. Review?"
- Jury sees: bet description, stakes, who claimed victory, evidence (if uploaded)
- Jury taps Approve or Reject
- If rejected: bet returns to active state, participants can re-submit or dispute
- If no response in 7 days: auto-approve OR escalate to participant majority vote

**No-Jury (Majority Approval) Flow:**
- Creator declares winner
- All OTHER participants get notification: "Tom says he won. Agree?"
- Majority must approve (for 2-person bet: other person must agree)
- If 2-person bet and both claim they won: DISPUTED status

---

### Journey 6: Viewing a Friend's Profile and Betting Record

**Persona:** Maria Santos wants to check Tom's betting track record before accepting his next challenge.

**Emotional Arc:** Curious --> Impressed/Amused --> Competitive --> Motivated

```
  FRIENDS LIST       TOM'S PROFILE          HEAD-TO-HEAD         CHALLENGE
  ------------       -------------          ------------         ---------
  | Tap Tom  |----->| Tom Janssen   |----->| You vs Tom  |----->| [Challenge|
  |          |     | Win rate: 62% |      | 3-2 (you    |      |  Tom]     |
  |          |     | Streak: 2W    |      |  lead!)     |      |  button   |
  |          |     | Total: 24 bets|      | Last 5 bets |      |           |
  |          |     | Active: 3     |      |             |      |           |
  ------------       -------------          ------------         ---------
  Emotion:          Emotion:               Emotion:             Emotion:
  Curious           "He's good..."         "I'm ahead!"         "Let's go!"
```

**Profile Sections:**

| Section | Content | Visibility |
|---------|---------|------------|
| Header | Avatar, name, bio | Always visible |
| Stats Summary | Win rate %, total bets, current streak | Always visible |
| Head-to-Head | Record against the viewer specifically | Only when viewing a friend |
| Active Bets | List of currently active bets | Visible to friends |
| Recent Results | Last 5-10 resolved bets with outcomes | Visible to friends |
| Challenge Button | "Challenge [Name]" CTA | Prominent, opens Quick Bet with friend pre-selected |

**Key Design Decisions:**
- Head-to-head record is the HERO of the friend profile view -- this drives repeat betting
- Active bets show status but NOT the stakes (privacy)
- The Challenge button pre-fills Quick Bet with this friend selected
- Own profile shows full stats; friend profile shows limited stats

---

### Journey 7: Friend Invitation via WhatsApp Deep Link

**Persona:** Tom Janssen wants to invite his friend Ben Martens (not yet on iBetcha) to join and bet.

**Emotional Arc:** Generous --> Sharing --> Hopeful --> Connected

```
  FRIENDS LIST       INVITE SCREEN        WHATSAPP           BEN'S PHONE         BEN REGISTERS
  ------------       -------------        --------           -----------         -------------
  | [Invite  |----->| Generate   |----->| Share     |----->| Taps link |----->| Signs up   |
  |  Friend] |     | deep link  |      | "Join me  |      | Opens     |      | Auto-friend|
  | button   |     | with       |      |  on       |      | app store |      | with Tom   |
  |          |     | referral   |      |  iBetcha!"|      | or app    |      |            |
  ------------       -------------        --------           -----------         -------------
  Tom's              Tom shares           Tom sends          Ben installs        Both notified
  Emotion:           link                 via WhatsApp       & registers         "You're now
  "Ben should                                                                    friends!"
  be on this"
```

**Steps:**

| Step | Actor | Action | Notes |
|------|-------|--------|-------|
| 1 | Tom | Taps "Invite Friend" in friends list | Also available from empty states |
| 2 | Tom | App generates a unique deep link with referral code | Link format: ibetcha.app/invite/{code} |
| 3 | Tom | Shares via system share sheet (WhatsApp, SMS, etc.) | Pre-filled message: "Join me on iBetcha! [link]" |
| 4 | Ben | Receives WhatsApp message, taps link | Opens App Store/Play Store if not installed |
| 5 | Ben | Installs and opens iBetcha | Deep link preserved through install |
| 6 | Ben | Registers (OAuth or email) | Referral code auto-applied |
| 7 | Both | Auto-connected as friends | Both receive notification |

**Key Design Decisions:**
- Deep link must survive the install process (deferred deep linking)
- Pre-filled share message should be casual, not corporate
- Referral code is invisible to the user -- auto-applied
- If Ben already has the app, tapping the link opens it and sends a friend request

---

### Journey 8: Dispute Flow (No Jury, Majority Approval)

**Persona:** Maria Santos and Tom Janssen have a 2-person bet with no jury. Both claim they won.

**Emotional Arc:** Confident --> Frustrated --> Stalemate --> Resolution

```
  TOM CLAIMS WIN     MARIA DISAGREES      DISPUTED STATUS      RESOLUTION OPTIONS
  ---------------    ----------------     ----------------     ------------------
  | Tom: "I won"|-->| Maria gets   |---->| Bet marked   |---->| 1. One concedes |
  | [Mark       |   | notification |     | DISPUTED     |     | 2. Appoint jury |
  |  Complete]  |   | "Agree?" NO  |     | Visible to   |     | 3. Stays disputed|
  |             |   |              |     | both + friends|     |    (no winner)   |
  ---------------    ----------------     ----------------     ------------------
  Emotion:          Emotion:             Emotion:             Emotion:
  Triumphant        Defiant              Frustrated           Relieved
  "I won!"          "No way!"            "This is stuck"      (once resolved)
```

**Dispute Rules (from phase1-decisions.md):**

| Scenario | Participants | Rule |
|----------|-------------|------|
| 2-person, no jury | Both claim win | DISPUTED -- must agree or appoint jury |
| 3+ person, no jury | Majority decides | Majority approval wins |
| Any bet, with jury | Jury decides | Jury has final say (7-day timeout) |
| Jury timeout (7 days) | Falls to participants | Participant majority vote |

**Steps:**

| Step | Action | Result |
|------|--------|--------|
| 1 | Creator declares winner (themselves) | Other participant notified |
| 2 | Other participant disagrees (taps "Dispute") | Bet enters DISPUTED state |
| 3 | Both see dispute status on bet detail | Options: "Concede" or "Appoint Jury" |
| 4a | One participant concedes | Bet resolves, records updated |
| 4b | Both agree to appoint a jury | Select a mutual friend as jury, jury reviews |
| 4c | Neither budges | Bet stays DISPUTED indefinitely (visible on profiles) |

**Key Design Decisions:**
- DISPUTED bets appear on both profiles -- social pressure to resolve
- "Appoint Jury" is available as a resolution mechanism
- No automatic resolution for 2-person disputes -- this is intentional (prevents gaming)
- For 3+ person bets, majority rules immediately, no dispute state needed

---

## 2. BDD User Stories by Epic

### System Constraints (Cross-Cutting)

- All API responses must complete within 2 seconds under normal load
- Push notifications must be sent within 5 seconds of triggering event
- Evidence upload supports photo (JPEG, PNG) and video (MP4) up to 50MB
- On-device compression required before upload
- All user data must be deletable per GDPR requirements
- English language only for MVP
- Bet acceptance timeout: 48 hours
- Jury decision timeout: 7 days

---

### Epic 1: Auth and Onboarding

#### US-101: OAuth Registration

**Priority:** Must

##### Problem

Maria Santos is a 27-year-old runner who received a WhatsApp invite to iBetcha from her friend Tom. She finds it tedious to create yet another account with a new password for a social app she is trying out. She wants to sign up with a single tap using her existing Google or Apple account.

##### Who

- New user | Received invite or found app organically | Wants frictionless entry

##### Solution

One-tap OAuth registration with Google and Apple, plus traditional email/password as fallback.

##### Domain Examples

1. **Happy Path -- Google OAuth:** Maria taps "Continue with Google," selects her Google account (maria.santos@gmail.com), and is registered instantly. She is taken to profile setup.
2. **Happy Path -- Apple Sign-In:** Ben Martens taps "Continue with Apple," uses Face ID, and his account is created with a private relay email. He is taken to profile setup.
3. **Edge Case -- Existing Account:** Maria tries to register with Google but her email is already registered. She sees "Account exists -- log in instead?" with a link to sign in.
4. **Fallback -- Email/Password:** Alex Peeters prefers not to use OAuth. He enters alex.peeters@outlook.com, creates a password (min 8 chars, 1 number), and receives a verification email.

##### UAT Scenarios (BDD)

**Scenario: New user registers with Google OAuth**
Given Maria Santos has the iBetcha app installed and is on the registration screen
When Maria taps "Continue with Google" and selects her Google account maria.santos@gmail.com
Then Maria has a new iBetcha account and is taken to the profile setup screen

**Scenario: New user registers with Apple Sign-In**
Given Ben Martens has the iBetcha app installed and is on the registration screen
When Ben taps "Continue with Apple" and authenticates with Face ID
Then Ben has a new iBetcha account and is taken to the profile setup screen

**Scenario: Registration with an already-used email shows guidance**
Given Maria Santos already has an account linked to maria.santos@gmail.com
When Maria attempts to register again with the same Google account
Then Maria sees a message indicating an account already exists and is offered a sign-in option

**Scenario: New user registers with email and password**
Given Alex Peeters is on the registration screen
When Alex enters alex.peeters@outlook.com, creates password "Str0ngPass!", and taps "Create Account"
Then Alex receives a verification email and sees a "Verify your email" confirmation screen

**Scenario: Password does not meet security requirements**
Given Alex Peeters is on the registration screen with email/password form
When Alex enters the password "weak"
Then Alex sees a validation message explaining password requirements (minimum 8 characters, at least 1 number)

##### Acceptance Criteria

- [ ] Google OAuth registration creates account and navigates to profile setup
- [ ] Apple Sign-In registration creates account and navigates to profile setup
- [ ] Duplicate email shows existing account guidance (not a raw error)
- [ ] Email/password registration sends verification email
- [ ] Password validation enforces minimum 8 characters and at least 1 number
- [ ] All passwords stored with bcrypt or equivalent hashing
- [ ] OAuth tokens stored securely in device keychain

##### Outcome KPIs

- **Who:** New users arriving at registration screen
- **Does what:** Complete registration successfully
- **By how much:** 85% registration completion rate
- **Measured by:** Ratio of registration screen views to successful account creations
- **Baseline:** No current baseline (new app)

---

#### US-102: User Login

**Priority:** Must

##### Problem

Tom Janssen is a 30-year-old cyclist who already has an iBetcha account. He finds it frustrating when apps log him out and he has to remember credentials. He wants to stay logged in and re-authenticate quickly when needed.

##### Who

- Returning user | Has existing account | Wants fast re-entry

##### Solution

Persistent login session with OAuth or email/password sign-in, plus biometric unlock for returning sessions.

##### Domain Examples

1. **Happy Path -- OAuth Login:** Tom taps "Continue with Google," selects his account, and is taken directly to the home screen.
2. **Happy Path -- Returning Session:** Maria opens the app 3 days after last use. She is still logged in and sees her home screen immediately.
3. **Error -- Wrong Password:** Alex enters an incorrect password. He sees "Incorrect email or password" (not revealing which one is wrong).

##### UAT Scenarios (BDD)

**Scenario: Returning user logs in with Google OAuth**
Given Tom Janssen has an existing account linked to tom.janssen@gmail.com
When Tom taps "Continue with Google" and selects his account
Then Tom sees his home screen with his active bets and friends

**Scenario: User session persists across app restarts**
Given Maria Santos logged in 3 days ago and has not explicitly signed out
When Maria opens the iBetcha app
Then Maria sees her home screen without needing to re-authenticate

**Scenario: Login attempt with incorrect credentials shows secure error**
Given Alex Peeters has an account with email alex.peeters@outlook.com
When Alex enters his email with an incorrect password and taps "Sign In"
Then Alex sees "Incorrect email or password" without revealing which field is wrong

##### Acceptance Criteria

- [ ] OAuth login navigates to home screen for existing accounts
- [ ] Session persists across app restarts (token-based, refresh flow)
- [ ] Incorrect credentials show a generic error message (no field-specific disclosure)
- [ ] Account lockout after 5 consecutive failed attempts (15-minute cooldown)

##### Outcome KPIs

- **Who:** Returning users
- **Does what:** Access the app without re-authentication friction
- **By how much:** 95% of returning sessions are auto-authenticated
- **Measured by:** Ratio of app opens to login screen views
- **Baseline:** No current baseline

---

#### US-103: Onboarding Walkthrough

**Priority:** Must

##### Problem

Maria Santos just registered for iBetcha but has never used a social betting app before. She finds it confusing when apps dump her on an empty home screen without guidance. She wants to understand what iBetcha is about and what she should do first.

##### Who

- New user | Just completed registration | Needs orientation

##### Solution

A 3-screen onboarding carousel shown once after first registration, highlighting: (1) Create bets with friends, (2) Fair judging with jury system, (3) Build your reputation and brag. Skippable but not dismissible by accident.

##### Domain Examples

1. **Happy Path:** Maria registers, sees 3 onboarding screens, swipes through all three, taps "Get Started," and lands on home screen.
2. **Quick User:** Tom registers, sees onboarding, taps "Skip" on the first screen, and goes directly to home.
3. **Edge Case:** Ben completes onboarding, kills the app, and reopens. He does NOT see onboarding again.

##### UAT Scenarios (BDD)

**Scenario: New user completes onboarding walkthrough**
Given Maria Santos has just completed registration
When Maria views all 3 onboarding screens and taps "Get Started"
Then Maria sees the home screen with her first-bet CTA visible

**Scenario: New user skips onboarding**
Given Tom Janssen has just completed registration and sees the onboarding screen
When Tom taps "Skip"
Then Tom sees the home screen directly

**Scenario: Onboarding is shown only once**
Given Ben Martens has completed onboarding previously
When Ben opens the app again
Then Ben sees the home screen without any onboarding screens

##### Acceptance Criteria

- [ ] Onboarding shows exactly 3 screens after first registration
- [ ] User can swipe through screens or tap "Skip" at any point
- [ ] Onboarding is shown only once per account (flag stored)
- [ ] After onboarding, home screen shows a contextual CTA for first bet

##### Outcome KPIs

- **Who:** New users seeing onboarding
- **Does what:** Complete onboarding (view all 3 screens)
- **By how much:** 60% completion rate (vs. skip)
- **Measured by:** Onboarding completion events vs. skip events
- **Baseline:** No current baseline

---

#### US-104: Profile Setup

**Priority:** Must

##### Problem

Maria Santos just registered via Google OAuth and her profile is empty. She finds it impersonal when apps show a blank avatar to her friends. She wants to set up a recognizable profile so friends can find and identify her.

##### Who

- New user | Just completed registration/onboarding | Wants to be recognizable

##### Solution

Profile setup screen after onboarding with username (required), display name, profile picture (optional), and short bio (optional).

##### Domain Examples

1. **Happy Path:** Maria sets username "mariasantos", adds her photo, writes bio "Runner and coffee addict," and saves.
2. **Minimal Setup:** Tom sets username "tomj" and skips photo and bio.
3. **Username Conflict:** Maria tries "maria" but it is taken. She sees suggestions: "maria_santos", "maria.s", "maria27".

##### UAT Scenarios (BDD)

**Scenario: New user sets up a complete profile**
Given Maria Santos is on the profile setup screen after onboarding
When Maria enters username "mariasantos", uploads a profile photo, writes bio "Runner and coffee addict", and taps "Save"
Then Maria's profile is created and she sees the home screen

**Scenario: New user sets up a minimal profile**
Given Tom Janssen is on the profile setup screen
When Tom enters username "tomj" and taps "Save" without adding a photo or bio
Then Tom's profile is created with a default avatar and empty bio

**Scenario: Username already taken shows alternatives**
Given Maria Santos tries to set username "maria" which is already taken
When the system checks username availability
Then Maria sees "Username taken" with suggestions like "maria_santos", "maria.s", "maria27"

**Scenario: Username validation rejects invalid characters**
Given Alex Peeters is on the profile setup screen
When Alex enters username "alex!!!" containing special characters
Then Alex sees a validation message: "Username can only contain letters, numbers, underscores, and periods"

##### Acceptance Criteria

- [ ] Username is required, unique, and validated (letters, numbers, underscores, periods only)
- [ ] Display name is optional (defaults to username)
- [ ] Profile picture is optional (default avatar provided)
- [ ] Bio is optional, max 150 characters
- [ ] Username availability checked in real time
- [ ] Taken usernames show suggestions

##### Outcome KPIs

- **Who:** New users on profile setup
- **Does what:** Complete profile with at least a photo
- **By how much:** 50% include a profile photo
- **Measured by:** Profiles with non-default avatars / total profiles
- **Baseline:** No current baseline

---

### Epic 2: Friends and Social

#### US-201: Search and Add Friends by Username

**Priority:** Must

##### Problem

Sarah De Vries is a 25-year-old event organizer who knows her friend Alex's iBetcha username. She finds it cumbersome to scroll through long contact lists in other apps. She wants to search by username and send a friend request directly.

##### Who

- Active user | Knows friend's username | Wants to connect

##### Solution

Username search with auto-complete results and one-tap friend request.

##### Domain Examples

1. **Happy Path:** Sarah searches "alexp", sees "Alex Peeters (@alexpeeters)" in results, taps "Add Friend," request is sent.
2. **No Results:** Sarah searches "xyz123abc" and sees "No users found. Invite them to iBetcha?"
3. **Already Friends:** Sarah searches "tomj" and sees Tom with a "Friends" badge instead of an "Add" button.

##### UAT Scenarios (BDD)

**Scenario: User finds and sends friend request by username**
Given Sarah De Vries is on the friend search screen
When Sarah types "alexp" in the search field
Then Sarah sees "Alex Peeters (@alexpeeters)" in the results and can tap "Add Friend" to send a request

**Scenario: Search returns no results and suggests invitation**
Given Sarah De Vries is on the friend search screen
When Sarah types "xyz123abc" and no matching users exist
Then Sarah sees "No users found" with an option to invite someone to iBetcha

**Scenario: Search shows existing friendship status**
Given Sarah De Vries and Tom Janssen are already friends
When Sarah searches for "tomj"
Then Sarah sees Tom Janssen with a "Friends" badge instead of an "Add Friend" button

**Scenario: Friend request is received as a notification**
Given Sarah De Vries sent a friend request to Alex Peeters
When Alex opens the app
Then Alex sees a notification with the friend request and can accept or decline

##### Acceptance Criteria

- [ ] Username search returns results as user types (debounced, 300ms)
- [ ] Results show display name, username, and avatar
- [ ] "Add Friend" sends a friend request with push notification to recipient
- [ ] Existing friends show "Friends" badge, not "Add" button
- [ ] Pending requests show "Pending" badge
- [ ] No results state suggests inviting via link

##### Outcome KPIs

- **Who:** Users searching for friends
- **Does what:** Successfully send friend requests
- **By how much:** 70% of searches result in a friend request sent
- **Measured by:** Friend requests sent / search sessions
- **Baseline:** No current baseline

---

#### US-202: Accept or Decline Friend Request

**Priority:** Must

##### Problem

Alex Peeters received a friend request from Sarah but does not check the app frequently. He finds it annoying when social apps bury friend requests in menus. He wants to see and act on requests quickly.

##### Who

- User with pending request | May not check app frequently | Wants quick action

##### Solution

Friend requests visible via notification badge on friends tab, with accept/decline from notification or friends screen.

##### Domain Examples

1. **Happy Path:** Alex receives push notification "Sarah De Vries wants to be friends," taps Accept, they are now connected.
2. **Decline:** Alex receives a request from an unknown user "random_user" and taps Decline. No notification sent to the requester about the decline.
3. **In-App:** Alex opens friends tab, sees 2 pending requests, accepts one and declines the other.

##### UAT Scenarios (BDD)

**Scenario: User accepts friend request from push notification**
Given Alex Peeters received a friend request from Sarah De Vries
When Alex taps "Accept" on the push notification
Then Alex and Sarah are connected as friends and both see each other in their friends list

**Scenario: User declines friend request silently**
Given Alex Peeters received a friend request from an unknown user
When Alex taps "Decline" on the friend request
Then the request is removed and the requester is NOT notified of the decline

**Scenario: User manages multiple pending requests in-app**
Given Alex Peeters has 2 pending friend requests from Sarah and Ben
When Alex opens the friends tab and accepts Sarah's request but declines Ben's
Then Sarah appears in Alex's friends list and Ben's request is removed

##### Acceptance Criteria

- [ ] Friend requests arrive as push notifications with Accept/Decline actions
- [ ] Requests also visible in friends tab with badge count
- [ ] Accepting adds both users to each other's friends list
- [ ] Declining removes the request silently (no notification to requester)
- [ ] Friends list updates in real time after acceptance

##### Outcome KPIs

- **Who:** Users receiving friend requests
- **Does what:** Respond to friend requests (accept or decline)
- **By how much:** 80% response rate within 48 hours
- **Measured by:** Friend requests responded to / total requests received
- **Baseline:** No current baseline

---

#### US-203: Invite Friend via WhatsApp Deep Link

**Priority:** Must

##### Problem

Tom Janssen wants to bet with his friend Ben Martens, but Ben is not on iBetcha yet. Tom finds it awkward to verbally explain and spell out an app name. He wants to send a quick invite link via WhatsApp that makes joining effortless.

##### Who

- Active user | Friend not on platform | Wants viral spread

##### Solution

Generate a unique deep link with referral code, shareable via system share sheet. Link auto-adds both users as friends after the invitee registers.

##### Domain Examples

1. **Happy Path:** Tom taps "Invite Friend," shares link via WhatsApp to Ben. Ben taps link, installs, registers, and is auto-connected with Tom.
2. **Existing User Taps Link:** Maria (already on iBetcha) taps Tom's invite link. Instead of registration, she sees a friend request from Tom.
3. **Link Expires:** Tom shared a link 30 days ago. Ben taps it. The link still works (no expiration for MVP).

##### UAT Scenarios (BDD)

**Scenario: User generates and shares an invite link**
Given Tom Janssen is on the friends screen
When Tom taps "Invite Friend" and shares the generated link via WhatsApp to Ben Martens
Then Ben receives a WhatsApp message with a personalized iBetcha invite link

**Scenario: Invitee installs app via deep link and auto-connects**
Given Ben Martens tapped Tom's invite link and installed iBetcha
When Ben completes registration
Then Ben and Tom are automatically connected as friends

**Scenario: Existing user taps invite link and gets friend request**
Given Maria Santos already has an iBetcha account
When Maria taps Tom's invite link
Then Maria sees a friend request from Tom within the app

##### Acceptance Criteria

- [ ] Invite generates a unique deep link with embedded referral code
- [ ] Share sheet opens with pre-filled message (customizable)
- [ ] Deep link survives app install (deferred deep linking)
- [ ] New user is auto-connected to inviter after registration
- [ ] Existing user receives a friend request when tapping link
- [ ] Links do not expire for MVP

##### Outcome KPIs

- **Who:** Users sending invite links
- **Does what:** Convert invitees to registered users
- **By how much:** Above 20% conversion rate (invite sent to registration)
- **Measured by:** Registrations from invite links / total invite links opened
- **Baseline:** No current baseline (target from phase1-decisions.md)

---

#### US-204: Friends List Management

**Priority:** Must

##### Problem

Sarah De Vries has 15 friends on iBetcha and wants to find specific friends quickly to challenge them. She finds it tedious to scroll through long unorganized lists. She wants her friends list to be searchable and show who is active.

##### Who

- Active user | Multiple friends | Wants efficient friend management

##### Solution

Friends list with search, sorted by recent interaction, with options to remove friends.

##### Domain Examples

1. **Happy Path:** Sarah opens friends tab, sees 15 friends sorted by recent bet activity, searches "Tom" to find Tom quickly.
2. **Remove Friend:** Sarah removes "random_user" from her friends list. Confirmation dialog shown.
3. **Empty State:** New user Ben has 0 friends. He sees "No friends yet. Search or invite friends to start betting!"

##### UAT Scenarios (BDD)

**Scenario: User browses friends list sorted by recent activity**
Given Sarah De Vries has 15 friends on iBetcha
When Sarah opens the friends tab
Then Sarah sees her friends sorted by most recent bet interaction with each

**Scenario: User removes a friend with confirmation**
Given Sarah De Vries wants to remove "random_user" from her friends
When Sarah taps the remove option and confirms the action
Then "random_user" is removed from Sarah's friends list and Sarah is removed from theirs

**Scenario: Empty friends list shows guidance**
Given Ben Martens has no friends on iBetcha
When Ben opens the friends tab
Then Ben sees an empty state with options to search for friends or send an invite link

##### Acceptance Criteria

- [ ] Friends list sorted by most recent interaction (bet activity)
- [ ] Search/filter within friends list
- [ ] Remove friend with confirmation dialog
- [ ] Removing a friend is mutual (both lose the connection)
- [ ] Empty state with clear CTAs (search, invite)
- [ ] Each friend row shows avatar, name, and head-to-head record snippet

##### Outcome KPIs

- **Who:** Active users with friends
- **Does what:** Use friends list to initiate bets
- **By how much:** 40% of bet creations start from friends list
- **Measured by:** Bet creation source tracking
- **Baseline:** No current baseline

---

### Epic 3: Bet Creation

#### US-301: Quick Bet Creation

**Priority:** Must

##### Problem

Tom Janssen is sitting at a cafe with Maria and just made a bold claim about who will win the next cycling race. He finds it annoying when apps require filling out long forms for a simple wager. He wants to create a bet in under 15 seconds before the moment passes.

##### Who

- Active user | In a social moment | Wants speed above all

##### Solution

Minimal bet creation flow: select friend(s) + type bet + set stake = send. Three fields, under 15 seconds, accessible via FAB button.

##### Domain Examples

1. **Happy Path (2-person):** Tom taps "+", selects Maria, types "I'll beat you on Sunday's ride," stakes "Loser buys coffee," sends. Total: 12 seconds.
2. **Happy Path (multi-person):** Sarah taps "+", selects Tom, Maria, and Ben, types "Who runs the fastest 5K Saturday," stakes "Last place buys pizza for everyone," sends.
3. **Minimal Bet:** Tom taps "+", selects Maria, types "You can't chug that water faster than me," stakes "Bragging rights," sends. 8 seconds.

##### UAT Scenarios (BDD)

**Scenario: User creates a 2-person quick bet under 15 seconds**
Given Tom Janssen is on the home screen and wants to bet with Maria Santos
When Tom taps the create button, selects Maria, types "I'll beat you on Sunday's ride", sets stake "Loser buys coffee", and taps Send
Then the bet is created and Maria receives a push notification to accept or decline

**Scenario: User creates a multi-person quick bet**
Given Sarah De Vries wants to bet with Tom, Maria, and Ben
When Sarah taps the create button, selects all three friends, types "Who runs the fastest 5K Saturday", sets stake "Last place buys pizza for everyone", and taps Send
Then all three participants receive push notifications to accept or decline

**Scenario: Quick bet defaults to no jury and 48-hour acceptance window**
Given Tom Janssen creates a quick bet without setting a jury or deadline
When the bet is created
Then the bet has no jury assigned (outcome will use majority approval) and participants have 48 hours to accept

**Scenario: Quick bet creation with validation**
Given Tom Janssen is creating a quick bet
When Tom tries to send without selecting any friends
Then Tom sees a validation message "Select at least one friend to bet with"

**Scenario: Quick bet preserves form state on validation error**
Given Tom Janssen has typed a bet description and stake but forgot to select a friend
When validation fails on the missing friend selection
Then Tom's typed description and stake are preserved and not cleared

##### Acceptance Criteria

- [ ] Quick bet requires exactly 3 inputs: friend(s), description, stake
- [ ] Friend selection shows recent friends first with search
- [ ] Bet creation sends push notification to all selected participants
- [ ] Default: no jury (majority approval), 48h acceptance timeout
- [ ] Total flow completable in under 15 seconds
- [ ] Validation errors preserve form state
- [ ] FAB button is accessible from home screen at all times
- [ ] Haptic feedback on successful bet creation

##### Outcome KPIs

- **Who:** Active users creating bets
- **Does what:** Complete bet creation via quick flow
- **By how much:** 80% of bets created via quick flow (vs. full form)
- **Measured by:** Quick bet creations / total bet creations
- **Baseline:** No current baseline (target: under 15 seconds median creation time)

---

#### US-302: Full Bet Creation

**Priority:** Must

##### Problem

Sarah De Vries is organizing a structured 5K challenge for the weekend and wants to set specific terms -- a deadline, a designated jury, and a descriptive title. She finds it limiting when apps only offer a simple quick-entry mode. She wants access to all fields when the bet deserves more ceremony.

##### Who

- Active user | Structured bet | Wants full control over terms

##### Solution

Expandable form from Quick Bet that reveals additional fields: title, deadline, jury selection, evidence requirement toggle.

##### Domain Examples

1. **Happy Path:** Sarah expands from quick bet, adds title "Weekend 5K Challenge," sets deadline to Saturday 6PM, selects Alex as jury, sends.
2. **Jury Selection:** Sarah selects Alex Peeters as jury. Alex is her friend but NOT a participant. Alex will receive a notification that he has been designated as jury.
3. **Edge Case -- Jury is Participant:** Sarah tries to select Tom (a participant) as jury. System prevents this with message "Jury cannot be a participant."

##### UAT Scenarios (BDD)

**Scenario: User creates a full bet with all optional fields**
Given Sarah De Vries is creating a bet and wants to add more details
When Sarah taps "More options," adds title "Weekend 5K Challenge," sets deadline to Saturday 18:00, selects Alex Peeters as jury, and taps Send
Then the bet is created with all specified details and all participants plus jury receive notifications

**Scenario: Jury must be a non-participant**
Given Sarah De Vries is creating a bet with Tom, Maria, and Ben as participants
When Sarah tries to select Tom as jury
Then Sarah sees "Jury cannot be a participant in the bet" and Tom is not selectable as jury

**Scenario: Jury receives notification of their role**
Given Sarah De Vries creates a bet with Alex Peeters designated as jury
When the bet is created
Then Alex receives a push notification informing him he has been designated as jury for the bet

**Scenario: Deadline is set in the future only**
Given Sarah De Vries is setting a deadline for a bet
When Sarah tries to set a deadline in the past
Then Sarah sees "Deadline must be in the future"

##### Acceptance Criteria

- [ ] Full form expands from quick bet via "More options" toggle
- [ ] Additional fields: title (optional), deadline (date/time picker), jury (friend picker), evidence toggle
- [ ] Jury picker filters out selected participants
- [ ] Jury is notified of their designation
- [ ] Deadline validation prevents past dates
- [ ] Preview/review screen shows all fields before sending
- [ ] Full form does not reset quick bet fields when expanded

##### Outcome KPIs

- **Who:** Users who need structured bets
- **Does what:** Use the full form for bets with deadlines and juries
- **By how much:** 20% of bets use at least one "More options" field
- **Measured by:** Bets with optional fields / total bets
- **Baseline:** No current baseline

---

### Epic 4: Bet Lifecycle

#### US-401: Accept a Bet

**Priority:** Must

##### Problem

Maria Santos received a bet challenge from Tom about their weekend cycling ride. She finds it disruptive when apps force her to open them fully just to respond to a simple yes/no. She wants to accept the bet with minimum effort, ideally without leaving her current context.

##### Who

- Bet participant | Received bet invitation | Wants quick response

##### Solution

Accept via actionable push notification (without opening app) or from bet detail screen. Shows head-to-head record with challenger for competitive context.

##### Domain Examples

1. **Happy Path -- From Notification:** Maria receives push "Tom bet you: 'I'll beat you on Sunday's ride.' Stake: Coffee." She taps "Accept" on the notification without opening the app.
2. **Happy Path -- From App:** Maria opens the app, goes to pending bets, reviews the details, sees "You vs Tom: 3-2 (you lead!)", and taps Accept.
3. **Multi-Person:** Sarah created a bet with Tom, Maria, and Ben. Tom accepts, Maria accepts, Ben has not yet. Bet is not yet active.

##### UAT Scenarios (BDD)

**Scenario: User accepts bet from actionable push notification**
Given Maria Santos received a push notification about Tom's cycling bet
When Maria taps "Accept" on the push notification
Then the bet is accepted and Tom receives a notification that Maria accepted

**Scenario: User accepts bet from bet detail screen with head-to-head context**
Given Maria Santos opens a pending bet from Tom
When Maria sees the bet details including "You vs Tom: 3-2" and taps "Accept"
Then the bet is accepted and all participants are notified

**Scenario: Multi-person bet activates when all participants accept**
Given Sarah created a bet with Tom, Maria, and Ben
When Tom and Maria have accepted but Ben has not yet
Then the bet status shows "Waiting for Ben" and is not yet active

**Scenario: Multi-person bet becomes active when last participant accepts**
Given Tom and Maria have accepted Sarah's bet and Ben is the last remaining
When Ben accepts the bet
Then the bet status changes to "Active" and all participants receive a "Bet is ON!" notification

##### Acceptance Criteria

- [ ] Bet can be accepted from actionable push notification without opening app
- [ ] Bet can be accepted from bet detail screen in-app
- [ ] Bet detail shows head-to-head record with challenger
- [ ] Bet becomes active only when ALL participants have accepted
- [ ] Partial acceptance shows who has accepted and who is pending
- [ ] Creator and all accepted participants notified on each new acceptance
- [ ] "Bet is ON!" notification sent when last participant accepts

##### Outcome KPIs

- **Who:** Users receiving bet invitations
- **Does what:** Accept bet invitations
- **By how much:** 70% acceptance rate
- **Measured by:** Accepted bets / total bet invitations received
- **Baseline:** No current baseline

---

#### US-402: Decline a Bet

**Priority:** Must

##### Problem

Alex Peeters received a bet about running a 5K but he has a knee injury this week. He finds it stressful when social apps make declining feel rude or send shaming notifications. He wants to decline guilt-free with a simple action.

##### Who

- Bet participant | Cannot or will not participate | Wants guilt-free exit

##### Solution

Simple decline action with no shaming language. Creator is notified factually. Declining removes the bet from the decliner's view.

##### Domain Examples

1. **Happy Path:** Alex receives the 5K bet, taps "Decline," bet disappears from his view. Sarah (creator) is notified: "Alex declined your bet."
2. **From Notification:** Alex taps "Decline" directly on the push notification.
3. **Impact on Multi-Person Bet:** In a 4-person bet, Alex declines. The bet continues with 3 participants (creator can decide to proceed or cancel).

##### UAT Scenarios (BDD)

**Scenario: User declines a bet from bet detail screen**
Given Alex Peeters has a pending bet invitation for the "Weekend 5K Challenge"
When Alex taps "Decline" on the bet detail screen
Then the bet is removed from Alex's view and Sarah receives a neutral notification "Alex declined your bet"

**Scenario: User declines a bet from push notification**
Given Alex Peeters received a push notification about a bet
When Alex taps "Decline" on the notification
Then the bet is declined without opening the app

**Scenario: Declining a multi-person bet removes only the decliner**
Given Sarah created a bet with Tom, Maria, Ben, and Alex
When Alex declines the bet
Then Alex is removed from the participants list and the bet continues for the remaining participants

##### Acceptance Criteria

- [ ] Decline removes the bet from decliner's view
- [ ] Creator receives a neutral, non-shaming notification
- [ ] Decline works from push notification and in-app
- [ ] For multi-person bets, declining removes only the decliner
- [ ] Remaining participants can still accept; bet proceeds without decliner
- [ ] Declined bet does NOT appear in decliner's history

##### Outcome KPIs

- **Who:** Users receiving bet invitations
- **Does what:** Decline without negative social consequences
- **By how much:** Decline notifications use neutral language (100% compliance)
- **Measured by:** QA audit of notification text
- **Baseline:** N/A

---

#### US-403: Bet Acceptance Timeout (48 Hours)

**Priority:** Must

##### Problem

Tom Janssen created a bet 3 days ago but one participant never responded. He finds it frustrating that the bet hangs in limbo forever. He wants the system to handle unresponsive participants automatically so bets do not become zombies.

##### Who

- Bet creator | Participant(s) not responding | Wants automatic cleanup

##### Solution

48-hour acceptance timeout with a reminder notification at 24 hours. Expired bets are automatically cancelled and all parties notified.

##### Domain Examples

1. **Happy Path -- Timeout:** Tom sends a bet to Maria. 24 hours pass with no response. Maria gets a reminder: "Tom's bet is waiting for you! 24 hours left." After 48 hours, the bet expires.
2. **Partial Accept:** 3-person bet. Tom and Maria accepted, Ben did not respond. After 48 hours, the bet expires for all (since not all accepted).
3. **Last-Second Accept:** Ben receives the 24h reminder and accepts at hour 47. Bet becomes active.

##### UAT Scenarios (BDD)

**Scenario: Reminder sent at 24-hour mark**
Given Tom Janssen sent a bet to Maria Santos 24 hours ago with no response
When the 24-hour mark is reached
Then Maria receives a reminder push notification "Tom's bet is waiting for you! 24 hours left to respond"

**Scenario: Bet expires after 48 hours without full acceptance**
Given Tom Janssen sent a bet to Maria Santos 48 hours ago with no response
When the 48-hour timeout is reached
Then the bet is automatically cancelled and both Tom and Maria are notified "Bet expired: no response within 48 hours"

**Scenario: Partially accepted multi-person bet expires on timeout**
Given Sarah's bet with Tom, Maria, and Ben has been accepted by Tom and Maria but not Ben
When 48 hours pass since bet creation
Then the bet expires and Tom, Maria, Sarah, and Ben are all notified

**Scenario: Acceptance just before timeout prevents expiry**
Given Ben received a bet 47 hours ago and has not responded
When Ben accepts the bet at hour 47
Then the bet is accepted normally and does not expire

##### Acceptance Criteria

- [ ] Reminder notification sent at 24-hour mark to non-responding participants
- [ ] Bet automatically expires at 48 hours if not all participants have accepted
- [ ] Expiry notification sent to all parties (creator, accepted participants, non-responders)
- [ ] Expired bets are marked as "Expired" (not "Cancelled" -- different semantic)
- [ ] Acceptance at any point before timeout prevents expiry
- [ ] Expired bets do NOT count toward win/loss records

##### Outcome KPIs

- **Who:** Bet creators
- **Does what:** Have bets resolved (accepted or expired) within 48 hours
- **By how much:** 95% of bet invitations resolved within timeout window
- **Measured by:** (Accepted + Expired within 48h) / total invitations
- **Baseline:** No current baseline

---

#### US-404: Cancel a Bet (Pre-Acceptance Only)

**Priority:** Must

##### Problem

Sarah De Vries created a bet but realized she set the wrong stake. She finds it worrying that once she hits send, she cannot take it back. She wants to cancel a bet before everyone has accepted, without affecting anyone's record.

##### Who

- Bet creator | Made a mistake or changed mind | Wants to undo before commitment

##### Solution

Cancel button available only while the bet is in "Pending" state (not all participants accepted). Cancellation notifies all participants.

##### Domain Examples

1. **Happy Path:** Sarah created a bet, Tom accepted but Maria and Ben have not. Sarah cancels. Everyone is notified: "Sarah cancelled the bet."
2. **Cannot Cancel:** Sarah tries to cancel a bet where all participants have already accepted. Cancel button is not visible/available.
3. **Immediate Cancel:** Sarah creates a bet, immediately realizes the stake is wrong, cancels within seconds. No one has accepted yet.

##### UAT Scenarios (BDD)

**Scenario: Creator cancels a pending bet**
Given Sarah De Vries created a bet that has not been fully accepted (Tom accepted, Maria and Ben pending)
When Sarah taps "Cancel Bet" and confirms the cancellation
Then the bet is cancelled and all participants (Tom, Maria, Ben) receive a notification "Sarah cancelled the bet"

**Scenario: Cancel option is unavailable for fully accepted bets**
Given Sarah De Vries created a bet that all participants have accepted (status: Active)
When Sarah views the bet detail screen
Then the "Cancel Bet" option is not available

**Scenario: Creator cancels a bet before any responses**
Given Sarah De Vries just created a bet 30 seconds ago and no one has responded
When Sarah taps "Cancel Bet" and confirms
Then the bet is cancelled and all invited participants are notified

##### Acceptance Criteria

- [ ] Cancel button visible only when bet status is "Pending" (not fully accepted)
- [ ] Cancellation requires confirmation dialog
- [ ] All participants (accepted and pending) notified of cancellation
- [ ] Cancelled bets do NOT count toward win/loss records
- [ ] Cancelled bets appear in history as "Cancelled" (greyed out)

##### Outcome KPIs

- **Who:** Bet creators
- **Does what:** Cancel bets before they go active
- **By how much:** Less than 10% of bets are cancelled (low = good)
- **Measured by:** Cancelled bets / total bets created
- **Baseline:** No current baseline

---

#### US-405: Declare Bet Winner (Mark Complete)

**Priority:** Must

##### Problem

Tom Janssen won the cycling bet against Maria and wants to claim his victory. He finds it anticlimactic when accomplishments go unrecognized. He wants to declare the outcome and trigger the celebration/verification flow.

##### Who

- Bet participant | Bet outcome is known | Wants to resolve the bet

##### Solution

Any participant can mark a bet as complete by selecting the winner. This triggers the jury approval or participant majority approval flow.

##### Domain Examples

1. **Happy Path (with Jury):** Tom marks the cycling bet as complete, selects himself as winner, optionally uploads a photo. Alex (jury) is notified to approve.
2. **Happy Path (no Jury, 2-person):** Tom marks a bet as complete, selects himself as winner. Maria is asked to confirm or dispute.
3. **Happy Path (no Jury, 3+ person):** Sarah marks the 5K bet complete, selects Tom as winner. Maria and Ben are asked to confirm. Majority rules.
4. **Nominate Other Winner:** Maria marks the cycling bet complete and selects Tom as winner (conceding).

##### UAT Scenarios (BDD)

**Scenario: Participant declares themselves as winner on a bet with jury**
Given Tom Janssen and Maria Santos have an active bet with Alex Peeters as jury
When Tom taps "Mark Complete" and selects himself as winner
Then Alex receives a notification to review and approve the outcome

**Scenario: Participant declares themselves as winner on a 2-person bet without jury**
Given Tom Janssen and Maria Santos have an active bet with no jury
When Tom taps "Mark Complete" and selects himself as winner
Then Maria receives a notification asking her to confirm or dispute the outcome

**Scenario: Participant concedes by selecting the other person as winner**
Given Maria Santos and Tom Janssen have an active bet
When Maria taps "Mark Complete" and selects Tom as winner
Then the outcome is immediately resolved (no further approval needed) and records are updated

**Scenario: Any participant can initiate completion**
Given Sarah, Tom, Maria, and Ben have an active bet
When Maria taps "Mark Complete" (Maria is not the creator)
Then Maria can select the winner and the approval flow begins

##### Acceptance Criteria

- [ ] Any participant (not just creator) can mark bet as complete
- [ ] Winner selection shows all participants as options
- [ ] Selecting yourself as winner triggers approval flow (jury or participant vote)
- [ ] Selecting another participant as winner auto-resolves (concession)
- [ ] Evidence upload prompt shown after winner selection (optional)
- [ ] Appropriate approval flow triggered based on jury/no-jury configuration

##### Outcome KPIs

- **Who:** Participants in active bets
- **Does what:** Complete bets by declaring a winner
- **By how much:** 60% bet completion rate (active bets that reach resolution)
- **Measured by:** Resolved bets / total active bets
- **Baseline:** No current baseline (target from phase1-decisions.md)

---

#### US-406: Bet Resolution (Records and Stats Updated)

**Priority:** Must

##### Problem

Tom Janssen just had his cycling bet win approved. He finds it unsatisfying when achievements are buried in a list with no celebration. He wants his win to be recorded permanently, his stats updated, and a shareable moment created.

##### Who

- Bet winner (and loser) | Outcome approved | Wants recognition and record

##### Solution

Upon bet resolution: update win/loss records, update streaks, update head-to-head stats, generate Win Card, show celebration screen to winner.

##### Domain Examples

1. **Winner Experience:** Tom's bet is approved. He sees a celebration screen: "You Won! Your record vs Maria: 4-2. Current streak: 2 wins." Win Card generated.
2. **Loser Experience:** Maria sees "Better luck next time. Your record vs Tom: 2-4." No shaming, factual.
3. **Stats Update:** Tom's overall record moves from 12-8 to 13-8. Win rate: 62%.

##### UAT Scenarios (BDD)

**Scenario: Winner sees celebration and updated stats**
Given Tom's cycling bet against Maria was approved with Tom as winner
When the bet is resolved
Then Tom sees a celebration screen showing "You Won!", his updated record vs Maria (4-2), and his current winning streak

**Scenario: Loser sees factual outcome without shaming**
Given Maria lost the cycling bet against Tom
When the bet is resolved
Then Maria sees a neutral result screen showing the outcome and her updated record vs Tom (2-4)

**Scenario: Win/loss records are updated across all stats**
Given Tom won a bet against Maria, bringing his overall record to 13-8
When the bet is resolved
Then Tom's profile shows 13 wins, 8 losses, 62% win rate, and updated head-to-head against Maria

**Scenario: Win streak is tracked and displayed**
Given Tom has won his last 3 bets in a row
When his latest bet is resolved as a win
Then Tom's profile shows "Current streak: 3W" and he receives a notification "You're on a 3-bet winning streak!"

##### Acceptance Criteria

- [ ] Winner sees celebration screen with updated stats
- [ ] Loser sees factual, non-shaming outcome screen
- [ ] Win/loss record updated for both parties
- [ ] Head-to-head record updated
- [ ] Win/loss streak calculated and displayed
- [ ] Win Card auto-generated (see Epic 9)
- [ ] Rivalry notification sent if records are close ("You and Maria are now tied 4-4!")

##### Outcome KPIs

- **Who:** Users whose bets are resolved
- **Does what:** View their updated stats after resolution
- **By how much:** 70% of users view the resolution screen (not dismiss notification)
- **Measured by:** Resolution screen views / bet resolutions
- **Baseline:** No current baseline

---

### Epic 5: Jury System

#### US-501: Jury Reviews and Approves Outcome

**Priority:** Must

##### Problem

Alex Peeters was designated as jury for the cycling bet between Tom and Maria. He finds it unclear what is expected of him when apps assign roles without explanation. He wants a clear, simple interface to review the evidence and approve or reject the claimed outcome.

##### Who

- Designated jury member | Outcome submitted for review | Wants clarity and simplicity

##### Solution

Dedicated jury review screen showing bet details, claimed winner, evidence (if provided), and simple Approve/Reject buttons. Accessible from push notification.

##### Domain Examples

1. **Happy Path:** Alex gets notification "Tom claims he won the cycling bet vs Maria. Review?" Alex opens it, sees the bet details, sees Tom's photo of the finish line, taps "Approve." Bet resolved.
2. **Reject:** Alex reviews but the evidence is unclear. He taps "Reject." Bet returns to active state, participants notified.
3. **Quick Action:** Alex taps "Approve" directly on the push notification without opening the app.

##### UAT Scenarios (BDD)

**Scenario: Jury approves outcome from in-app review**
Given Alex Peeters is jury for Tom vs Maria's cycling bet and Tom claimed victory
When Alex opens the jury review screen, views the evidence photo, and taps "Approve"
Then the bet is resolved with Tom as winner, and both Tom and Maria are notified

**Scenario: Jury rejects the claimed outcome**
Given Alex Peeters is jury and reviews Tom's win claim
When Alex taps "Reject" on the jury review screen
Then the bet returns to active state and Tom and Maria are notified "Jury rejected the outcome. Bet is still active."

**Scenario: Jury approves from actionable push notification**
Given Alex Peeters received a push notification about judging Tom vs Maria's bet
When Alex taps "Approve" directly on the push notification
Then the bet is resolved without Alex needing to open the app

**Scenario: Jury sees all relevant information for decision**
Given Alex Peeters opens the jury review screen for Tom vs Maria's bet
When the review screen loads
Then Alex sees the bet description, stakes, who claimed victory, evidence (if uploaded), and Approve/Reject buttons

##### Acceptance Criteria

- [ ] Jury receives push notification with actionable Approve/Reject buttons
- [ ] In-app jury review screen shows: bet details, claimed winner, evidence
- [ ] Approve resolves the bet and notifies all parties
- [ ] Reject returns bet to active state and notifies participants
- [ ] Jury can only act once per outcome claim (no double-approving)
- [ ] Jury cannot be a participant in the bet

##### Outcome KPIs

- **Who:** Designated jury members
- **Does what:** Respond to jury review requests
- **By how much:** 70% response rate within 48 hours
- **Measured by:** Jury responses within 48h / total jury review requests
- **Baseline:** No current baseline (target from phase1-decisions.md)

---

#### US-502: Jury Timeout (7-Day Escalation)

**Priority:** Must

##### Problem

Tom Janssen won a bet 5 days ago but the jury (Alex) has not responded. He finds it infuriating when his victory hangs in limbo because someone else is unresponsive. He wants the system to escalate automatically so no bet is stuck forever.

##### Who

- Bet participant | Jury not responding | Wants automatic escalation

##### Solution

7-day jury timeout. Reminders at day 3 and day 6. After 7 days, escalate to participant majority vote.

##### Domain Examples

1. **Timeout with Escalation:** Alex does not respond for 7 days. System automatically falls back to participant majority vote. Tom voted for himself (implicit by declaring winner). Maria is asked to approve or dispute.
2. **Reminder Works:** Alex gets a day-3 reminder, reviews, and approves.
3. **Day 6 Warning:** Alex gets "Final reminder: 24 hours to review Tom vs Maria's bet before it escalates to participants."

##### UAT Scenarios (BDD)

**Scenario: Jury receives reminder at day 3**
Given Alex Peeters has not responded to a jury review request for 3 days
When the 3-day mark is reached
Then Alex receives a reminder push notification "Reminder: Tom vs Maria's bet is waiting for your verdict"

**Scenario: Jury receives final warning at day 6**
Given Alex Peeters has not responded to a jury review request for 6 days
When the 6-day mark is reached
Then Alex receives a final reminder "Last chance: 24 hours to review before the bet escalates to participant vote"

**Scenario: Jury timeout escalates to participant majority vote**
Given Alex Peeters has not responded for 7 days
When the 7-day timeout is reached
Then the jury role is bypassed and the bet outcome falls to participant majority approval, with all participants notified of the escalation

##### Acceptance Criteria

- [ ] Reminder sent to jury at day 3
- [ ] Final reminder sent at day 6 with urgency language
- [ ] After 7 days, jury role is bypassed
- [ ] Escalation falls to participant majority vote
- [ ] All parties notified of escalation reason
- [ ] Jury can still respond up until the exact timeout moment

##### Outcome KPIs

- **Who:** Bets with designated juries
- **Does what:** Reach resolution within the timeout period
- **By how much:** 90% of jury-assigned bets resolved within 7 days
- **Measured by:** Bets resolved within 7 days / total jury-assigned bets
- **Baseline:** No current baseline

---

### Epic 6: Evidence

#### US-601: Upload Evidence (Photo or Video)

**Priority:** Must

##### Problem

Tom Janssen just won the cycling bet and wants to prove it with a photo of the finish line. He finds it tedious when apps make uploading media a multi-step process. He wants to attach evidence quickly as part of the completion flow, but only when he chooses to.

##### Who

- Bet participant | Declaring outcome | Wants optional proof

##### Solution

Optional evidence attachment during the "Mark Complete" flow. Supports photo and video. On-device compression. Max 50MB per item.

##### Domain Examples

1. **Happy Path -- Photo:** Tom marks bet complete, taps "Add Evidence," selects a photo from his gallery. Photo compressed on-device, uploaded to S3.
2. **Happy Path -- Video:** Tom takes a 15-second video of crossing the finish line. Video compressed from 80MB to 35MB on device, uploaded.
3. **Skip Evidence:** Tom marks bet complete without adding any evidence. System accepts this -- evidence is optional.
4. **File Too Large:** Tom tries to upload a 4-minute uncompressed video (200MB). After compression it is still 70MB. System shows "File too large. Maximum 50MB after compression. Try a shorter clip."

##### UAT Scenarios (BDD)

**Scenario: User uploads a photo as evidence during bet completion**
Given Tom Janssen is marking the cycling bet as complete
When Tom taps "Add Evidence," selects a photo from his gallery, and the photo is compressed and uploaded
Then the evidence is attached to the bet outcome and visible to the jury and other participants

**Scenario: User uploads a video as evidence**
Given Tom Janssen wants to share video proof of his win
When Tom records or selects a 15-second video and it is compressed on-device to under 50MB
Then the video is uploaded and attached to the bet outcome

**Scenario: User skips evidence upload**
Given Maria Santos is marking a casual bet as complete (who picks the restaurant)
When Maria selects the winner without tapping "Add Evidence"
Then the bet completion proceeds normally without evidence

**Scenario: Evidence exceeds size limit after compression**
Given Tom Janssen tries to upload a 4-minute video
When on-device compression still results in a file larger than 50MB
Then Tom sees "File too large. Maximum 50MB after compression. Try a shorter clip."

##### Acceptance Criteria

- [ ] Evidence upload is OPTIONAL during bet completion
- [ ] Supports photo (JPEG, PNG) and video (MP4)
- [ ] On-device compression before upload
- [ ] Maximum 50MB per evidence item after compression
- [ ] Clear error message when file exceeds limit
- [ ] Evidence visible to jury and all participants on bet detail
- [ ] Upload progress indicator shown during upload
- [ ] Evidence stored securely (S3)

##### Outcome KPIs

- **Who:** Users completing bets
- **Does what:** Attach evidence to bet outcomes
- **By how much:** 30% of bet completions include evidence
- **Measured by:** Bet completions with evidence / total bet completions
- **Baseline:** No current baseline

---

### Epic 7: Notifications

#### US-701: Push Notifications for All Bet Lifecycle Events

**Priority:** Must

##### Problem

Maria Santos is not always in the app but needs to know when bets are created, accepted, or resolved. She finds it frustrating when she misses important updates because an app relies on her checking in manually. She wants timely push notifications for every important bet event.

##### Who

- All users | Bet activity happening | Wants to stay informed without checking app

##### Solution

Push notifications for all lifecycle events, with actionable buttons where applicable (Accept/Decline, Approve/Reject).

##### Domain Examples

1. **Bet Created:** Maria receives "Tom challenged you! 'I'll beat you on Sunday's ride.' Stake: Coffee. [Accept] [Decline]"
2. **Bet Accepted:** Tom receives "Maria accepted your bet! Game on."
3. **Bet Declined:** Tom receives "Maria declined your bet."
4. **Bet Completed:** Maria receives "Tom claims he won your cycling bet. Evidence attached."
5. **Bet Resolved:** Tom receives "You won! Your record vs Maria: 4-2."
6. **Bet Expired:** Tom receives "Your bet expired: Maria did not respond within 48 hours."
7. **Bet Cancelled:** Maria receives "Tom cancelled the cycling bet."
8. **Rivalry Milestone:** Tom receives "You and Maria are now tied at 4-4!"

##### UAT Scenarios (BDD)

**Scenario: Participant receives notification when challenged**
Given Tom Janssen creates a bet challenging Maria Santos
When the bet is created
Then Maria receives a push notification "Tom challenged you!" with Accept and Decline action buttons

**Scenario: Creator receives notification when bet is accepted**
Given Maria Santos accepted Tom's bet
When the acceptance is processed
Then Tom receives a push notification "Maria accepted your bet! Game on."

**Scenario: Actionable notification buttons work without opening app**
Given Maria Santos received a bet challenge notification with Accept and Decline buttons
When Maria taps "Accept" on the notification
Then the bet is accepted without Maria needing to open the app

**Scenario: Rivalry milestone notification fires on tied records**
Given Tom and Maria's head-to-head record becomes 4-4 after a bet resolution
When the record is updated
Then both Tom and Maria receive "You and Maria/Tom are now tied at 4-4!"

**Scenario: Bet expiry notification sent to all parties**
Given Tom's bet to Maria expired after 48 hours without response
When the timeout triggers
Then both Tom and Maria receive a notification about the expiry

##### Acceptance Criteria

- [ ] Notifications sent for: bet created, accepted, declined, completed, resolved, expired, cancelled
- [ ] Rivalry milestone notifications for tied records and streak milestones
- [ ] Actionable buttons on bet creation (Accept/Decline) and jury review (Approve/Reject)
- [ ] Notifications delivered within 5 seconds of event
- [ ] Notification tap deep-links to relevant bet detail screen
- [ ] Notification text is clear, concise, and uses participant names
- [ ] Users can receive notifications when app is in background

##### Outcome KPIs

- **Who:** All users receiving notifications
- **Does what:** Engage with notifications (tap or use action buttons)
- **By how much:** 50% notification engagement rate
- **Measured by:** Notification taps and action button uses / notifications delivered
- **Baseline:** No current baseline

---

### Epic 8: Profile and Reputation

#### US-801: View Own Profile with Betting Stats

**Priority:** Must

##### Problem

Tom Janssen has been using iBetcha for a month and wants to see his overall track record. He finds it satisfying when apps show his achievements and progress in one place. He wants a profile that reflects his betting reputation -- wins, losses, streaks, and who he bets with most.

##### Who

- Active user | Has betting history | Wants to see reputation

##### Solution

Profile screen showing: avatar, display name, bio, overall stats (wins, losses, win rate, current streak, total bets), recent bets, and top rivals (friends with most bets).

##### Domain Examples

1. **Active Profile:** Tom's profile shows: 13W-8L (62%), streak: 2W, 21 total bets. Top rival: Maria (4-2).
2. **New Profile:** Ben just joined, has 0 bets. Profile shows empty state with "Create your first bet to start building your reputation!"
3. **Stats After Loss:** After losing to Maria, Tom's profile updates to 13W-9L (59%), streak resets to 0.

##### UAT Scenarios (BDD)

**Scenario: Active user views their complete profile stats**
Given Tom Janssen has 13 wins, 8 losses, and a 2-bet winning streak
When Tom opens his profile
Then Tom sees his win/loss record (13W-8L), win rate (62%), current streak (2W), total bets (21), and his top rival Maria (4-2)

**Scenario: New user sees empty profile with guidance**
Given Ben Martens just joined and has no betting history
When Ben opens his profile
Then Ben sees his avatar and name with an empty state: "Create your first bet to start building your reputation!"

**Scenario: Profile stats update in real time after bet resolution**
Given Tom's bet against Maria was just resolved as a loss
When Tom opens his profile
Then Tom's record shows 13W-9L (59%) and his streak is reset

##### Acceptance Criteria

- [ ] Profile shows: avatar, display name, bio, join date
- [ ] Stats section: wins, losses, win rate %, current streak, total bets
- [ ] Top rivals section: friends with most mutual bets, showing head-to-head record
- [ ] Recent bets section: last 10 resolved bets with outcome
- [ ] Empty state for new users with CTA to create first bet
- [ ] Stats update immediately after bet resolution
- [ ] Profile is editable (name, avatar, bio)

##### Outcome KPIs

- **Who:** Active users
- **Does what:** View their own profile
- **By how much:** 2+ profile views per week per active user
- **Measured by:** Profile screen views / active users / week
- **Baseline:** No current baseline

---

#### US-802: View Friend's Profile and Head-to-Head Record

**Priority:** Must

##### Problem

Maria Santos wants to check Tom's betting track record before accepting his latest challenge. She finds it motivating when she can see competitive stats against a specific person. She wants to see Tom's overall record and specifically how they match up against each other.

##### Who

- Active user | Viewing friend's profile | Wants competitive context

##### Solution

Friend profile view showing their public stats, head-to-head record with viewer, active bets (without stakes), and a prominent "Challenge" button.

##### Domain Examples

1. **Happy Path:** Maria views Tom's profile. Sees "Tom Janssen -- 13W-8L (62%), streak: 2W." Below: "You vs Tom: 2-4. Challenge Tom?" She taps "Challenge" and Quick Bet opens with Tom pre-selected.
2. **No History:** Maria views Sarah's profile. They have no mutual bets. Shows "You vs Sarah: No bets yet. Be the first to challenge!"
3. **Active Bets Visible:** Tom has 3 active bets. Maria can see the descriptions but NOT the stakes (privacy).

##### UAT Scenarios (BDD)

**Scenario: User views friend's profile with head-to-head record**
Given Maria Santos and Tom Janssen have a 2-4 head-to-head record
When Maria views Tom's profile
Then Maria sees Tom's overall stats and their head-to-head record "You vs Tom: 2-4"

**Scenario: Challenge button opens Quick Bet with friend pre-selected**
Given Maria Santos is viewing Tom Janssen's profile
When Maria taps "Challenge Tom"
Then Quick Bet creation opens with Tom pre-selected as the friend

**Scenario: No mutual betting history shows encouragement**
Given Maria Santos and Sarah De Vries have no mutual bets
When Maria views Sarah's profile
Then Maria sees "You vs Sarah: No bets yet. Be the first to challenge!" with a Challenge button

**Scenario: Active bets visible without stakes**
Given Tom Janssen has 3 active bets
When Maria views Tom's profile
Then Maria can see the bet descriptions and participant names but NOT the stakes

##### Acceptance Criteria

- [ ] Friend profile shows overall stats (wins, losses, win rate, streak)
- [ ] Head-to-head record prominently displayed
- [ ] Active bets shown without stakes (privacy)
- [ ] "Challenge" button opens Quick Bet with friend pre-selected
- [ ] Empty head-to-head shows encouragement message
- [ ] Recent resolved bets visible (descriptions and outcomes)

##### Outcome KPIs

- **Who:** Users viewing friend profiles
- **Does what:** Initiate a challenge from the profile
- **By how much:** 25% of profile views lead to a bet creation
- **Measured by:** Bet creations from profile view / total profile views
- **Baseline:** No current baseline

---

#### US-803: Edit Profile

**Priority:** Must

##### Problem

Maria Santos uploaded a blurry profile photo when she first registered and wants to update it. She also wants to update her bio since she started cycling. She finds it frustrating when apps bury profile editing deep in settings menus.

##### Who

- Active user | Wants to update their public identity | Expects easy access

##### Solution

Edit profile accessible from own profile screen. Allows changing: display name, avatar, bio. Username changes not allowed for MVP (prevents confusion).

##### Domain Examples

1. **Happy Path:** Maria taps "Edit" on her profile, uploads a new photo, changes bio to "Runner, cyclist, and serial bet winner," saves.
2. **Display Name Change:** Tom changes display name from "Tom" to "Tom Janssen."
3. **Username Locked:** Maria tries to change her username but sees "Username cannot be changed" (MVP limitation).

##### UAT Scenarios (BDD)

**Scenario: User updates profile photo and bio**
Given Maria Santos is on her profile screen
When Maria taps "Edit", uploads a new photo, changes bio to "Runner, cyclist, and serial bet winner", and taps "Save"
Then Maria's profile is updated with the new photo and bio visible to all friends

**Scenario: Username is not editable**
Given Maria Santos is editing her profile
When Maria views the edit screen
Then the username field is displayed but not editable, with a note "Username cannot be changed"

##### Acceptance Criteria

- [ ] Edit accessible from own profile screen (edit icon/button)
- [ ] Editable fields: display name, avatar, bio
- [ ] Username is read-only on edit screen
- [ ] Photo upload with on-device cropping
- [ ] Bio max 150 characters with character counter
- [ ] Changes saved immediately and visible to friends

##### Outcome KPIs

- **Who:** Users who registered
- **Does what:** Customize their profile beyond defaults
- **By how much:** 40% of users edit profile at least once within first week
- **Measured by:** Profile edits within 7 days / new registrations
- **Baseline:** No current baseline

---

### Epic 9: Win Cards and Sharing

#### US-901: Auto-Generate Shareable Win Card

**Priority:** Must

##### Problem

Tom Janssen just won the cycling bet against Maria and wants to brag about it on WhatsApp. He finds it boring to just type "I won" in a group chat -- he wants something visual and satisfying. He wants a pre-made, good-looking card he can share instantly.

##### Who

- Bet winner | Bet just resolved | Wants to brag visually

##### Solution

Auto-generated Win Card after bet resolution showing: bet title/description, winner, loser(s), stakes, head-to-head record, evidence thumbnail (if available). Designed for screenshotting and sharing. Includes iBetcha branding and deep link.

##### Domain Examples

1. **Happy Path:** Tom wins the cycling bet. Win Card generated: "Tom beat Maria! 'I'll beat you on Sunday's ride.' Stake: Coffee. Record: Tom leads 4-2. [iBetcha logo]." Tom taps share, sends to WhatsApp group.
2. **With Evidence:** Win Card includes a thumbnail of Tom's finish line photo.
3. **Close Rivalry:** Win Card shows "Tom vs Maria: NOW TIED 4-4!" for extra drama.

##### UAT Scenarios (BDD)

**Scenario: Win Card is auto-generated after bet resolution**
Given Tom Janssen won the cycling bet against Maria Santos
When the bet is resolved
Then a Win Card is automatically generated showing bet description, winner (Tom), loser (Maria), stakes, and head-to-head record (4-2)

**Scenario: Win Card includes evidence thumbnail when available**
Given Tom uploaded a photo as evidence for his winning bet
When the Win Card is generated
Then the card includes a thumbnail of Tom's evidence photo

**Scenario: Winner shares Win Card via system share sheet**
Given Tom Janssen is viewing the Win Card for his resolved bet
When Tom taps "Share"
Then the system share sheet opens allowing Tom to share the card image to WhatsApp, Instagram, or other apps

**Scenario: Win Card includes iBetcha branding and deep link**
Given a Win Card has been generated for Tom's bet win
When the card is rendered
Then it includes the iBetcha logo and a deep link URL back to the bet details in the app

##### Acceptance Criteria

- [ ] Win Card auto-generated upon bet resolution for the winner
- [ ] Card shows: bet description, winner, loser(s), stakes, head-to-head record
- [ ] Evidence thumbnail included when evidence was uploaded
- [ ] iBetcha branding and deep link included on card
- [ ] Share button opens system share sheet
- [ ] Card is a static image (PNG/JPEG) optimized for WhatsApp and Instagram Stories
- [ ] Card generated within 3 seconds of resolution
- [ ] Loser also sees the card (but "Share" is less prominent)

##### Outcome KPIs

- **Who:** Bet winners
- **Does what:** Share Win Cards externally
- **By how much:** 30% of Win Cards are shared
- **Measured by:** Share button taps / Win Cards generated
- **Baseline:** No current baseline

---

### Epic 10: Account Management

#### US-1001: Block a User

**Priority:** Must

##### Problem

Sarah De Vries keeps receiving bet challenges from a person she no longer wants to interact with. She finds it uncomfortable to have to decline every bet individually and see this person in her friends list. She wants to block them completely.

##### Who

- User experiencing unwanted contact | Wants safety and control | Needs immediate effect

##### Solution

Block user option from their profile or from a bet detail. Blocking removes friendship, hides mutual content, and prevents all future contact.

##### Domain Examples

1. **Happy Path:** Sarah blocks "unwanted_user" from their profile. They are removed from Sarah's friends, all pending bets between them are cancelled, and neither can search or contact the other.
2. **From Bet:** Sarah receives a bet from someone she wants to block. She blocks them from the bet detail screen.
3. **Blocked User's Perspective:** The blocked user sees Sarah's profile as "User not found" if they search for her. No notification that they were blocked.

##### UAT Scenarios (BDD)

**Scenario: User blocks another user from their profile**
Given Sarah De Vries is viewing the profile of "unwanted_user"
When Sarah taps "Block User" and confirms
Then "unwanted_user" is removed from Sarah's friends, all pending bets between them are cancelled, and neither can find or contact the other

**Scenario: Blocked user cannot find the blocker**
Given Sarah De Vries blocked "unwanted_user"
When "unwanted_user" searches for Sarah's username
Then the search returns no results for Sarah

**Scenario: Blocking does not notify the blocked user**
Given Sarah De Vries blocks "unwanted_user"
When the block takes effect
Then "unwanted_user" does NOT receive any notification about being blocked

**Scenario: Active bets with blocked user are cancelled**
Given Sarah De Vries has a pending bet with "unwanted_user"
When Sarah blocks "unwanted_user"
Then the pending bet is automatically cancelled

##### Acceptance Criteria

- [ ] Block option available on user profile and bet detail screens
- [ ] Blocking requires confirmation dialog
- [ ] Blocking removes mutual friendship
- [ ] All pending bets between blocker and blocked are cancelled
- [ ] Blocked user cannot search for, view, or contact blocker
- [ ] No notification sent to blocked user about the block
- [ ] Block list accessible in Settings for unblocking

##### Outcome KPIs

- **Who:** Users experiencing unwanted contact
- **Does what:** Block problematic users
- **By how much:** Less than 2% of users use block (low = healthy community)
- **Measured by:** Block events / total active users
- **Baseline:** No current baseline

---

#### US-1002: Delete Account

**Priority:** Must

##### Problem

Ben Martens no longer uses iBetcha and wants his data removed. He finds it frustrating and concerning when apps make it impossible to delete an account. He wants a clear, permanent way to delete his account and all associated data, as required by GDPR and app store policies.

##### Who

- User leaving the platform | Concerned about data privacy | Expects clear deletion

##### Solution

Account deletion option in Settings with clear explanation of what is deleted. Requires re-authentication. 30-day grace period before permanent deletion.

##### Domain Examples

1. **Happy Path:** Ben goes to Settings > Delete Account, re-authenticates, confirms. His account enters a 30-day deletion queue. He can cancel within 30 days by logging back in.
2. **Active Bets:** Ben has 2 active bets. System warns "You have 2 active bets that will be cancelled if you delete your account."
3. **Permanent Deletion:** After 30 days, all of Ben's data is permanently deleted: profile, bets, evidence, stats.

##### UAT Scenarios (BDD)

**Scenario: User initiates account deletion with confirmation**
Given Ben Martens is in Settings
When Ben taps "Delete Account", re-authenticates, and confirms deletion
Then Ben's account enters a 30-day deletion queue and Ben is logged out

**Scenario: Active bets are warned before deletion**
Given Ben Martens has 2 active bets
When Ben attempts to delete his account
Then Ben sees a warning "You have 2 active bets that will be cancelled" before confirming

**Scenario: User cancels deletion within grace period**
Given Ben Martens initiated account deletion 10 days ago
When Ben logs back in within the 30-day grace period
Then Ben's account is reactivated and deletion is cancelled

**Scenario: Data is permanently deleted after grace period**
Given Ben Martens initiated account deletion 30 days ago and did not log back in
When the 30-day grace period expires
Then all of Ben's data (profile, bets, evidence, stats, friend connections) is permanently deleted

##### Acceptance Criteria

- [ ] Delete Account option in Settings
- [ ] Re-authentication required before deletion
- [ ] Warning shown if user has active bets
- [ ] 30-day grace period before permanent deletion
- [ ] User can cancel deletion by logging in within grace period
- [ ] After grace period: all user data permanently deleted (GDPR compliant)
- [ ] Deleted user appears as "[Deleted User]" in other users' bet histories
- [ ] User logged out immediately after initiating deletion

##### Outcome KPIs

- **Who:** Users choosing to leave
- **Does what:** Successfully delete their accounts
- **By how much:** 100% of deletion requests processed within 30 days
- **Measured by:** Deletion requests completed / deletion requests initiated
- **Baseline:** No current baseline (regulatory requirement)

---

#### US-1003: Privacy Policy and Terms of Service

**Priority:** Must

##### Problem

Maria Santos is privacy-conscious and wants to know how her data is used before she fully commits to the app. She finds it concerning when apps collect data without transparency. She wants accessible privacy and terms documentation as required by app store policies.

##### Who

- New or existing user | Privacy-conscious | Needs legal transparency

##### Solution

Privacy Policy and Terms of Service accessible from registration screen and Settings. Must be accepted before registration completes.

##### Domain Examples

1. **During Registration:** Maria sees "By creating an account, you agree to our [Privacy Policy] and [Terms of Service]." Links open readable documents.
2. **From Settings:** Tom goes to Settings > Privacy Policy to review data handling.
3. **Required Acceptance:** Alex tries to register without checking the ToS agreement. Registration is blocked.

##### UAT Scenarios (BDD)

**Scenario: Privacy Policy and ToS must be accepted during registration**
Given Maria Santos is on the registration screen
When Maria attempts to create an account
Then Maria must accept the Privacy Policy and Terms of Service before registration completes

**Scenario: Privacy Policy accessible from Settings**
Given Tom Janssen is a registered user
When Tom navigates to Settings
Then Tom can view the Privacy Policy and Terms of Service

**Scenario: Registration blocked without ToS acceptance**
Given Alex Peeters is on the registration screen and has not checked the ToS agreement
When Alex taps "Create Account"
Then registration is blocked with a message "Please accept the Privacy Policy and Terms of Service"

##### Acceptance Criteria

- [ ] Privacy Policy and ToS links on registration screen
- [ ] Acceptance checkbox required before account creation
- [ ] Both documents accessible from Settings at any time
- [ ] Documents written in clear, readable language
- [ ] Documents cover: data collection, storage, sharing, deletion, user rights

##### Outcome KPIs

- **Who:** Users at registration
- **Does what:** Accept ToS without abandoning
- **By how much:** Less than 5% drop-off at ToS step
- **Measured by:** ToS screen views vs. ToS acceptances
- **Baseline:** No current baseline

---

### Epic 11: Dispute Resolution

#### US-1101: Majority Approval for Bets Without Jury

**Priority:** Must

##### Problem

Sarah De Vries created a 4-person bet with no jury. The 5K challenge is over and she declared Tom as the winner. She finds it frustrating when unanimous agreement is required because one person can hold up the result. She wants a fair, majority-based resolution that prevents loser veto power.

##### Who

- Bet participants (3+) | No jury assigned | Need democratic resolution

##### Solution

When no jury is assigned and a bet has 3+ participants, outcome resolution requires majority approval from participants. The person who declared the result has an implicit vote for their declaration.

##### Domain Examples

1. **Happy Path (3+ people):** Sarah declares Tom won the 5K. There are 4 participants. Tom agrees (implicit -- he was declared winner). Maria agrees. Ben disagrees. Majority (3 of 4) = resolved. Tom wins.
2. **Majority Not Reached:** Sarah declares Tom won. Tom agrees. Maria disagrees. Ben disagrees. 2-2 split. Bet stays in voting until someone changes or a jury is appointed.
3. **Edge Case (exactly 3 people):** Sarah, Tom, Maria. Sarah declares Tom won. Tom agrees (implicit). Maria disagrees. 2-1 = majority. Tom wins.

##### UAT Scenarios (BDD)

**Scenario: Majority approves outcome in a 4-person bet**
Given Sarah's 5K bet has 4 participants (Sarah, Tom, Maria, Ben) with no jury, and Sarah declared Tom as winner
When Tom and Maria approve and Ben disputes (3 of 4 approve)
Then the outcome is resolved with Tom as winner and all participants are notified

**Scenario: Majority not reached results in ongoing vote**
Given Sarah declared Tom won a 4-person bet with no jury
When Tom approves but Maria and Ben both dispute (2-2 split)
Then the bet remains in "Awaiting Resolution" state with the option to appoint a jury

**Scenario: Declarer's selection counts as implicit approval**
Given Sarah declares Tom won a 3-person bet (Sarah, Tom, Maria) with no jury
When Tom is declared winner (implicit approval from Tom) and Sarah confirms
Then the outcome needs only 1 more approval from Maria for majority (2 of 3)

##### Acceptance Criteria

- [ ] Majority = more than 50% of participants
- [ ] Declarer's chosen winner gets their implicit approval
- [ ] Each participant can approve or dispute the outcome
- [ ] Majority reached = bet resolved
- [ ] No majority = bet stays in voting state
- [ ] Option to appoint jury if deadlocked
- [ ] All votes are visible to participants (transparency)

##### Outcome KPIs

- **Who:** Participants in no-jury bets
- **Does what:** Resolve bets via majority vote
- **By how much:** 80% of no-jury multi-person bets resolved within 48 hours
- **Measured by:** No-jury resolutions within 48h / total no-jury completion claims
- **Baseline:** No current baseline

---

#### US-1102: Two-Person Dispute (No Jury)

**Priority:** Must

##### Problem

Tom Janssen and Maria Santos have a 2-person bet with no jury. Both claim they won. Tom finds it infuriating when there is no mechanism to resolve a stalemate between two people. He wants a clear process to handle disputes without requiring external intervention initially.

##### Who

- 2 bet participants | Both claim victory | Need dispute resolution path

##### Solution

When both participants in a 2-person no-jury bet claim they won, the bet enters a DISPUTED state. Options: one person concedes, or both agree to appoint a jury.

##### Domain Examples

1. **Dispute Occurs:** Tom declares himself winner. Maria disputes. Bet enters DISPUTED state visible to both and on their profiles.
2. **Concession:** After 2 days of dispute, Maria concedes. Bet resolves with Tom as winner.
3. **Appoint Jury:** Tom and Maria both agree to appoint Alex as jury. Alex reviews and decides.
4. **Ongoing Dispute:** Neither concedes, neither appoints jury. Bet stays DISPUTED indefinitely. Visible on both profiles as unresolved.

##### UAT Scenarios (BDD)

**Scenario: Two-person bet enters disputed state when both claim victory**
Given Tom and Maria have a 2-person bet with no jury
When Tom declares himself winner and Maria disputes the outcome
Then the bet enters DISPUTED status visible to both participants

**Scenario: Dispute resolved by concession**
Given Tom and Maria have a DISPUTED bet
When Maria taps "Concede" on the disputed bet
Then the bet resolves with Tom as winner and both records are updated

**Scenario: Dispute resolved by appointing a jury**
Given Tom and Maria have a DISPUTED bet
When both agree to appoint Alex Peeters as jury
Then Alex receives a jury review request and the dispute follows the standard jury flow

**Scenario: Disputed bet visible on profiles**
Given Tom and Maria have a DISPUTED bet that has not been resolved
When any friend views Tom or Maria's profile
Then the disputed bet is visible with a "Disputed" badge

##### Acceptance Criteria

- [ ] 2-person no-jury bet with conflicting claims enters DISPUTED state
- [ ] DISPUTED bets show on both participants' profiles
- [ ] Options: Concede (resolves bet) or Appoint Jury (enters jury flow)
- [ ] Both participants must agree on the jury appointment
- [ ] Disputed bets remain indefinitely until resolved
- [ ] Disputed bets do NOT count in win/loss records until resolved

##### Outcome KPIs

- **Who:** Participants in 2-person disputed bets
- **Does what:** Resolve disputes (via concession or jury)
- **By how much:** 70% of disputed bets resolved within 7 days
- **Measured by:** Disputed bets resolved within 7 days / total disputed bets
- **Baseline:** No current baseline

---

### Epic 12: Empty States and Home Screen

#### US-1201: Home Screen with Active Bets

**Priority:** Must

##### Problem

Tom Janssen opens iBetcha and wants to see what is going on at a glance. He finds it overwhelming when apps show too much information on the main screen. He wants a clean home screen that shows his active bets and makes it easy to create new ones.

##### Who

- Active user | Returning to app | Wants quick overview

##### Solution

Home screen showing active bets list (sorted by most recent activity), pending actions (bets to accept, outcomes to approve), and the FAB for bet creation.

##### Domain Examples

1. **Active User:** Tom opens the app. Home screen shows: "2 pending actions" (bet to accept, outcome to approve) at top, then 3 active bets sorted by recent activity.
2. **No Pending Actions:** Maria opens the app. No pending actions. Shows 2 active bets.
3. **New User:** Ben opens the app. No bets, no friends. Shows empty state.

##### UAT Scenarios (BDD)

**Scenario: Active user sees pending actions and active bets**
Given Tom Janssen has 2 pending actions and 3 active bets
When Tom opens the home screen
Then Tom sees pending actions highlighted at the top, followed by his active bets sorted by most recent activity

**Scenario: User with no pending actions sees active bets only**
Given Maria Santos has no pending actions and 2 active bets
When Maria opens the home screen
Then Maria sees her active bets without a pending actions section

**Scenario: New user sees empty state with onboarding CTA**
Given Ben Martens has no bets and no friends
When Ben opens the home screen
Then Ben sees an empty state with "Add friends and create your first bet!" and CTAs for friend search and bet creation

##### Acceptance Criteria

- [ ] Pending actions shown prominently at top (bets to accept, outcomes to approve)
- [ ] Active bets list sorted by most recent activity
- [ ] Each bet row shows: description, participants, status, head-to-head snippet
- [ ] FAB for quick bet creation always visible
- [ ] Empty state for new users with guidance CTAs
- [ ] Pull-to-refresh on bet list
- [ ] Tab bar navigation: Home, Friends, Profile, Settings

##### Outcome KPIs

- **Who:** All active users
- **Does what:** Take action on pending items within one session
- **By how much:** 60% of pending actions resolved within same session
- **Measured by:** Pending actions resolved / pending actions shown
- **Baseline:** No current baseline

---

## 3. Screen Inventory

| # | Screen | Purpose | Epic |
|---|--------|---------|------|
| 1 | **Splash Screen** | App branding shown on launch | Auth |
| 2 | **Registration Screen** | OAuth buttons (Google, Apple) + email/password form + ToS links | Auth |
| 3 | **Login Screen** | OAuth buttons + email/password sign-in for returning users | Auth |
| 4 | **Email Verification Screen** | Confirmation screen after email/password registration | Auth |
| 5 | **Onboarding Carousel (3 screens)** | First-time walkthrough: Create, Judge, Brag | Onboarding |
| 6 | **Profile Setup Screen** | Username, display name, avatar, bio entry for new users | Onboarding |
| 7 | **Home Screen** | Active bets list, pending actions, FAB for bet creation | Core |
| 8 | **Home Screen -- Empty State** | Guidance for new users with no bets or friends | Core |
| 9 | **Quick Bet Creation** | Minimal bet creation: friend picker + description + stake | Bet Creation |
| 10 | **Full Bet Creation (expanded)** | Extended fields: title, deadline, jury, evidence toggle | Bet Creation |
| 11 | **Friend Picker (modal)** | Select one or more friends for bet or jury assignment | Bet Creation |
| 12 | **Bet Detail -- Pending** | Bet terms, Accept/Decline buttons, head-to-head record | Bet Lifecycle |
| 13 | **Bet Detail -- Active** | Active bet with Mark Complete button, participants, evidence | Bet Lifecycle |
| 14 | **Bet Detail -- Resolved** | Outcome, winner, evidence, Win Card, share button | Bet Lifecycle |
| 15 | **Bet Detail -- Disputed** | Dispute info, Concede and Appoint Jury options | Disputes |
| 16 | **Bet Detail -- Expired** | Expired bet info (timeout) | Bet Lifecycle |
| 17 | **Bet Detail -- Cancelled** | Cancelled bet info | Bet Lifecycle |
| 18 | **Mark Complete Flow** | Winner selection + optional evidence upload | Bet Lifecycle |
| 19 | **Evidence Upload** | Camera/gallery picker with compression progress indicator | Evidence |
| 20 | **Majority Vote Screen** | Vote status for no-jury bets (who approved, who pending) | Disputes |
| 21 | **Jury Review Screen** | Bet details, claimed winner, evidence, Approve/Reject buttons | Jury |
| 22 | **Win Card View** | Generated Win Card with share button | Win Cards |
| 23 | **Celebration Screen** | Winner celebration with updated stats and Win Card | Win Cards |
| 24 | **Result Screen (Loser)** | Neutral outcome display with updated stats | Bet Lifecycle |
| 25 | **Friends Tab** | Friends list with search, sorted by recent interaction | Friends |
| 26 | **Friends Tab -- Empty State** | No friends guidance with search and invite CTAs | Friends |
| 27 | **Friend Search** | Username search with auto-complete results | Friends |
| 28 | **Friend Requests** | Pending friend requests with Accept/Decline | Friends |
| 29 | **Invite Friend** | Generate and share deep link via system share sheet | Friends |
| 30 | **Own Profile** | Avatar, stats, top rivals, recent bets, edit button | Profile |
| 31 | **Friend Profile** | Friend's stats, head-to-head record, Challenge button | Profile |
| 32 | **Edit Profile** | Edit display name, avatar, bio | Profile |
| 33 | **Settings** | Account settings, notifications, privacy, block list, logout | Account |
| 34 | **Block List** | Manage blocked users (unblock) | Account |
| 35 | **Delete Account Flow** | Re-authentication + confirmation + active bets warning | Account |
| 36 | **Privacy Policy** | Full privacy policy document | Legal |
| 37 | **Terms of Service** | Full terms of service document | Legal |

**Total: 37 distinct screens/views**

---

## 4. Definition of Ready Checklist

### Epic 1: Auth and Onboarding

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-101 OAuth Registration | Yes | Yes (Maria, Ben, Alex) | 4 | 5 | Yes | Yes | Bcrypt, keychain | None | Yes | READY |
| US-102 User Login | Yes | Yes (Tom, Maria, Alex) | 3 | 3 | Yes | Yes | Token refresh | US-101 | Yes | READY |
| US-103 Onboarding | Yes | Yes (Maria, Tom, Ben) | 3 | 3 | Yes | Yes | None | US-101 | Yes | READY |
| US-104 Profile Setup | Yes | Yes (Maria, Tom) | 3 | 4 | Yes | Yes | Image upload | US-101 | Yes | READY |

**INVEST Check:**
- **Independent:** Each story can be built and tested independently (US-102-104 depend on US-101 for registration, but are otherwise independent of each other)
- **Negotiable:** Onboarding screen count and profile fields are negotiable
- **Valuable:** Each delivers user-facing value (registration, orientation, identity)
- **Estimable:** Standard OAuth, carousel, and form patterns -- well-understood
- **Small:** Each is 1-3 days of effort
- **Testable:** All have BDD scenarios with concrete assertions

---

### Epic 2: Friends and Social

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-201 Search Friends | Yes | Yes (Sarah) | 3 | 4 | Yes | Yes | Search debounce | US-101 | Yes | READY |
| US-202 Accept/Decline Request | Yes | Yes (Alex) | 3 | 3 | Yes | Yes | Push notifications | US-201 | Yes | READY |
| US-203 WhatsApp Invite | Yes | Yes (Tom, Ben, Maria) | 3 | 3 | Yes | Yes | Deferred deep linking | US-101 | Yes | READY |
| US-204 Friends List | Yes | Yes (Sarah, Ben) | 3 | 3 | Yes | Yes | None | US-201 | Yes | READY |

**INVEST Check:**
- **Independent:** Friend search, invites, and list management are independent paths
- **Negotiable:** Search algorithm, sort order, and invite message are negotiable
- **Valuable:** Social foundation -- the app is useless without friends
- **Estimable:** Standard patterns (search, lists, deep links)
- **Small:** 1-2 days each
- **Testable:** All BDD scenarios have concrete data and assertions

---

### Epic 3: Bet Creation

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-301 Quick Bet | Yes | Yes (Tom) | 3 | 5 | Yes | Yes | 15s target | US-204 | Yes | READY |
| US-302 Full Bet | Yes | Yes (Sarah) | 3 | 4 | Yes | Yes | Jury validation | US-301 | Yes | READY |

**INVEST Check:**
- **Independent:** Quick Bet is standalone; Full Bet extends it
- **Negotiable:** Which fields are in quick vs. full is negotiable
- **Valuable:** Core product function -- the #1 differentiator
- **Estimable:** Form UI with validation -- well-understood
- **Small:** Quick Bet 2-3 days, Full Bet 1-2 days (extends quick)
- **Testable:** 15-second timing target is measurable; BDD scenarios are specific

---

### Epic 4: Bet Lifecycle

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-401 Accept Bet | Yes | Yes (Maria) | 3 | 4 | Yes | Yes | Actionable push | US-301 | Yes | READY |
| US-402 Decline Bet | Yes | Yes (Alex) | 3 | 3 | Yes | Yes | Neutral messaging | US-301 | Yes | READY |
| US-403 Timeout 48h | Yes | Yes (Tom) | 3 | 4 | Yes | Yes | Scheduled jobs | US-301 | Yes | READY |
| US-404 Cancel Bet | Yes | Yes (Sarah) | 3 | 3 | Yes | Yes | State validation | US-301 | Yes | READY |
| US-405 Declare Winner | Yes | Yes (Tom) | 4 | 4 | Yes | Yes | Approval flows | US-401 | Yes | READY |
| US-406 Bet Resolution | Yes | Yes (Tom, Maria) | 3 | 4 | Yes | Yes | Stats aggregation | US-405 | Yes | READY |

**INVEST Check:**
- **Independent:** Accept, Decline, Timeout, Cancel are independent lifecycle transitions. Declare Winner and Resolution are sequential.
- **Negotiable:** Timeout durations, notification wording negotiable
- **Valuable:** Each represents a critical lifecycle state transition
- **Estimable:** State machine patterns -- well-understood
- **Small:** 1-2 days each
- **Testable:** All state transitions have concrete pre/post conditions

---

### Epic 5: Jury System

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-501 Jury Review | Yes | Yes (Alex) | 3 | 4 | Yes | Yes | Push actions | US-405 | Yes | READY |
| US-502 Jury Timeout | Yes | Yes (Tom, Alex) | 3 | 3 | Yes | Yes | Scheduled jobs | US-501 | Yes | READY |

**INVEST Check:**
- **Independent:** Jury review and timeout are independent (timeout is a fallback path)
- **Negotiable:** Timeout duration, reminder schedule negotiable
- **Valuable:** Fairness mechanism -- core differentiator
- **Estimable:** Notification + scheduled job patterns
- **Small:** 1-2 days each
- **Testable:** Clear timeout windows and escalation rules

---

### Epic 6: Evidence

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-601 Upload Evidence | Yes | Yes (Tom, Maria) | 4 | 4 | Yes | Yes | S3, compression | US-405 | Yes | READY |

**INVEST Check:**
- **Independent:** Evidence is optional and self-contained
- **Negotiable:** Compression quality, file size limits negotiable
- **Valuable:** Proof mechanism that supports fair judgment
- **Estimable:** File upload + compression is a known pattern
- **Small:** 2-3 days (compression + S3 integration)
- **Testable:** File size limits and format support are measurable

---

### Epic 7: Notifications

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-701 Push Notifications | Yes | Yes (Maria, Tom) | 8 | 5 | Yes | Yes | FCM/APNs | All epics | Yes | READY |

**INVEST Check:**
- **Independent:** Notification infrastructure is cross-cutting but can be built as a service
- **Negotiable:** Notification wording, timing, and grouping negotiable
- **Valuable:** Primary engagement mechanism
- **Estimable:** Push notification infrastructure is well-understood
- **Small:** 2-3 days for infrastructure + notification templates
- **Testable:** Delivery timing and content are measurable

---

### Epic 8: Profile and Reputation

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-801 Own Profile | Yes | Yes (Tom, Ben) | 3 | 3 | Yes | Yes | Stats queries | US-406 | Yes | READY |
| US-802 Friend Profile | Yes | Yes (Maria) | 3 | 4 | Yes | Yes | Privacy filtering | US-801 | Yes | READY |
| US-803 Edit Profile | Yes | Yes (Maria, Tom) | 3 | 2 | Yes | Yes | Image cropping | US-104 | Yes | READY |

**INVEST Check:**
- **Independent:** Own profile, friend profile, and edit are separate views
- **Negotiable:** Which stats to show, privacy levels negotiable
- **Valuable:** Core to the "Bragging Rights" direction -- reputation IS the product
- **Estimable:** Profile UI + stats aggregation queries
- **Small:** 1-2 days each
- **Testable:** Stats calculations and visibility rules are concrete

---

### Epic 9: Win Cards and Sharing

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-901 Win Card | Yes | Yes (Tom) | 3 | 4 | Yes | Yes | Image generation | US-406 | Yes | READY |

**INVEST Check:**
- **Independent:** Win Card generation is a self-contained feature triggered by resolution
- **Negotiable:** Card design, included information negotiable
- **Valuable:** Viral mechanic -- key to the Bragging Rights direction
- **Estimable:** Template image generation is a known pattern
- **Small:** 2-3 days
- **Testable:** Card content and share functionality are verifiable

---

### Epic 10: Account Management

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-1001 Block User | Yes | Yes (Sarah) | 3 | 4 | Yes | Yes | Cascading effects | US-204 | Yes | READY |
| US-1002 Delete Account | Yes | Yes (Ben) | 3 | 4 | Yes | Yes | GDPR, scheduled job | US-101 | Yes | READY |
| US-1003 Privacy/ToS | Yes | Yes (Maria, Tom, Alex) | 3 | 3 | Yes | Yes | Legal documents | None | Yes | READY |

**INVEST Check:**
- **Independent:** Block, delete, and legal docs are fully independent
- **Negotiable:** Grace period duration, block cascade behavior negotiable
- **Valuable:** Required for app store compliance and user safety
- **Estimable:** Standard patterns (block lists, soft delete, document display)
- **Small:** 1-2 days each
- **Testable:** All behaviors have clear assertions

---

### Epic 11: Dispute Resolution

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-1101 Majority Approval | Yes | Yes (Sarah, Tom) | 3 | 3 | Yes | Yes | Voting logic | US-405 | Yes | READY |
| US-1102 Two-Person Dispute | Yes | Yes (Tom, Maria) | 4 | 4 | Yes | Yes | Dispute state | US-405 | Yes | READY |

**INVEST Check:**
- **Independent:** Majority vote and 2-person dispute are separate flows
- **Negotiable:** Majority threshold, dispute visibility negotiable
- **Valuable:** Prevents broken incentives (loser veto)
- **Estimable:** Voting and state machine logic
- **Small:** 1-2 days each
- **Testable:** Vote thresholds and state transitions are mathematically verifiable

---

### Epic 12: Home Screen

| Story | Problem Clear | Persona Defined | 3+ Examples | BDD Scenarios | AC from BDD | Right-Sized | Tech Notes | Dependencies | KPIs | Status |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| US-1201 Home Screen | Yes | Yes (Tom, Maria, Ben) | 3 | 3 | Yes | Yes | Aggregation queries | All epics | Yes | READY |

**INVEST Check:**
- **Independent:** Home screen is the integration point but can be built incrementally
- **Negotiable:** Sort order, pending actions display negotiable
- **Valuable:** The app's front door -- first thing users see
- **Estimable:** List view with status badges -- standard pattern
- **Small:** 2-3 days
- **Testable:** Sort order, pending action display, empty states all testable

---

## Summary

### Story Count by Epic

| Epic | Stories | Priority (all Must for MVP) |
|------|---------|---------------------------|
| Auth and Onboarding | 4 | Must |
| Friends and Social | 4 | Must |
| Bet Creation | 2 | Must |
| Bet Lifecycle | 6 | Must |
| Jury System | 2 | Must |
| Evidence | 1 | Must |
| Notifications | 1 | Must |
| Profile and Reputation | 3 | Must |
| Win Cards and Sharing | 1 | Must |
| Account Management | 3 | Must |
| Dispute Resolution | 2 | Must |
| Home Screen | 1 | Must |
| **Total** | **30** | |

### Suggested Build Sequence (Walking Skeleton First)

**Phase A -- Walking Skeleton (Week 1-2):**
US-101 (Registration) > US-102 (Login) > US-104 (Profile Setup) > US-201 (Friend Search) > US-202 (Accept Friend) > US-301 (Quick Bet) > US-401 (Accept Bet) > US-405 (Declare Winner) > US-406 (Resolution) > US-1201 (Home Screen)

This delivers: register, add a friend, create a bet, accept it, resolve it, see results. The minimum end-to-end bet lifecycle.

**Phase B -- Core Enrichment (Week 3-4):**
US-103 (Onboarding) > US-302 (Full Bet) > US-402 (Decline) > US-403 (Timeout) > US-404 (Cancel) > US-601 (Evidence) > US-501 (Jury Review) > US-701 (Notifications)

This adds: the full bet creation path, all lifecycle states, evidence, jury, and push notifications.

**Phase C -- Social and Reputation (Week 5-6):**
US-203 (WhatsApp Invite) > US-204 (Friends List) > US-801 (Own Profile) > US-802 (Friend Profile) > US-803 (Edit Profile) > US-901 (Win Card) > US-1101 (Majority Vote) > US-1102 (Dispute)

This adds: viral invite loop, reputation profiles, Win Cards, and dispute resolution.

**Phase D -- Compliance and Polish (Week 7):**
US-1001 (Block) > US-1002 (Delete Account) > US-1003 (Privacy/ToS) > US-502 (Jury Timeout)

This adds: safety, compliance, and timeout edge cases.

### Key Metrics to Track from Day 1

| Metric | Target | Source |
|--------|--------|--------|
| Bet creation time (Quick Bet) | Under 15 seconds | phase1-decisions.md |
| Bet completion rate | Above 60% | phase1-decisions.md |
| Friend invitation conversion | Above 20% | phase1-decisions.md |
| Weekly active retention (D30) | Above 30% | phase1-decisions.md |
| Jury response rate (within 48h) | Above 70% | phase1-decisions.md |
| Win Card share rate | Above 30% | US-901 KPI |
| Registration completion rate | Above 85% | US-101 KPI |
| Quick Bet usage (vs full form) | Above 80% | US-301 KPI |
