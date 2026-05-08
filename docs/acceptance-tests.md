# iBetcha -- Acceptance Tests

**Date:** 2026-05-08
**Source documents:** product-owner-output.md, phase1-decisions.md, ddd-architecture.md, solution-architecture.md
**Status:** Ready for development handoff

These acceptance tests are the definition of done for each user story. They are written in the ubiquitous language of the iBetcha domain. State names are canonical DDD names: `PENDING_ACCEPTANCE`, `ACTIVE`, `PENDING_JURY_VERDICT`, `PENDING_APPROVAL`, `RESOLVED`, `DISPUTED`, `EXPIRED`, `CANCELLED`.

All tests marked `[E2E]` exercise the full stack through the REST API driving port (mobile client -> API server -> PostgreSQL). Tests marked `[Integration]` exercise one or two components with real I/O. Walking skeleton scenarios are marked `[WS]`.

---

## Table of Contents

1. [Feature: Auth and Onboarding](#feature-auth-and-onboarding)
2. [Feature: Friends and Social](#feature-friends-and-social)
3. [Feature: Bet Creation](#feature-bet-creation)
4. [Feature: Bet Acceptance and Decline](#feature-bet-acceptance-and-decline)
5. [Feature: Bet Expiry and Cancellation](#feature-bet-expiry-and-cancellation)
6. [Feature: Bet Completion and Outcome Declaration](#feature-bet-completion-and-outcome-declaration)
7. [Feature: Jury System](#feature-jury-system)
8. [Feature: Dispute Resolution](#feature-dispute-resolution)
9. [Feature: Evidence](#feature-evidence)
10. [Feature: Reputation and Profile](#feature-reputation-and-profile)
11. [Feature: Win Cards and Sharing](#feature-win-cards-and-sharing)
12. [Feature: Notifications](#feature-notifications)
13. [Feature: Account Management](#feature-account-management)
14. [Feature: Home Screen](#feature-home-screen)

---

## Feature: Auth and Onboarding

**Epic:** US-101 (OAuth Registration), US-102 (Login), US-103 (Onboarding), US-104 (Profile Setup)
**Driving port:** `POST /api/v1/auth/register`, `POST /api/v1/auth/oauth/google`, `POST /api/v1/auth/oauth/apple`, `POST /api/v1/auth/login`

```gherkin
Feature: Auth and Onboarding
  iBetcha users register, authenticate, and set up their profile
  before they can create or participate in bets.

  Background:
    Given the iBetcha API is available
    And no user with email "maria.santos@gmail.com" exists
    And no user with email "alex.peeters@outlook.com" exists
    And no user with email "ben.martens@icloud.com" exists


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-AUTH-01]
  Scenario: New user registers via Google OAuth, completes onboarding, and is ready to bet
    Given Maria Santos is on the registration screen
    When Maria authenticates with a valid Google ID token for "maria.santos@gmail.com"
    Then a new iBetcha account is created for Maria
    And Maria is taken to the profile setup screen
    When Maria enters username "mariasantos", uploads a profile photo, and writes bio "Runner and coffee addict"
    And Maria taps "Save"
    Then Maria's profile is created and she is taken to the home screen
    And the home screen shows a first-bet call-to-action for Maria


  # ── Registration ─────────────────────────────────────────────────────────────

  @E2E [AUTH-01]
  Scenario: New user registers with Google OAuth
    Given Maria Santos is on the registration screen
    When Maria authenticates with a valid Google ID token for "maria.santos@gmail.com"
    Then a new iBetcha account is created for Maria
    And Maria is navigated to the profile setup screen
    And no password is stored for Maria's account

  @E2E [AUTH-02]
  Scenario: New user registers with Apple Sign-In
    Given Ben Martens is on the registration screen
    When Ben authenticates with a valid Apple identity token and authorization code
    And Ben's Apple account uses a private relay email "ben.private@privaterelay.appleid.com"
    Then a new iBetcha account is created for Ben using the relay email
    And Ben is navigated to the profile setup screen

  @E2E [AUTH-03]
  Scenario: New user registers with email and password
    Given Alex Peeters is on the registration screen
    When Alex submits email "alex.peeters@outlook.com", password "Str0ngPass!", and accepts the Terms of Service
    Then Alex receives a verification email at "alex.peeters@outlook.com"
    And Alex sees a "Verify your email" confirmation screen
    And Alex's account is not yet active until email is verified

  @E2E [AUTH-04]
  Scenario: Email registration requires Terms of Service acceptance
    Given Alex Peeters is on the registration screen
    When Alex submits email "alex.peeters@outlook.com" and password "Str0ngPass!" without accepting Terms of Service
    Then registration is blocked with the message "Please accept the Privacy Policy and Terms of Service"

  @E2E [AUTH-05]
  Scenario: Duplicate email shows account-exists guidance
    Given Maria Santos already has an iBetcha account linked to "maria.santos@gmail.com"
    When Maria attempts to register again with the same Google account
    Then Maria sees "An account already exists for this email" with a sign-in option
    And no duplicate account is created

  @E2E [AUTH-06]
  Scenario: Weak password is rejected with clear requirements
    Given Alex Peeters is on the email registration screen
    When Alex enters the password "weak"
    Then Alex sees a validation message "Password must be at least 8 characters and contain at least 1 number"
    And the form is not submitted

  @E2E [AUTH-07]
  Scenario: Email verification token completes registration and issues tokens
    Given Alex Peeters submitted registration with email "alex.peeters@outlook.com"
    When Alex submits the email verification token received in the verification email
    Then Alex's account is activated
    And Alex receives an access token and a refresh token
    And Alex is navigated to the profile setup screen


  # ── Login ────────────────────────────────────────────────────────────────────

  @E2E [AUTH-08]
  Scenario: Returning user logs in with Google OAuth
    Given Tom Janssen has an existing account linked to "tom.janssen@gmail.com"
    When Tom authenticates with a valid Google ID token for "tom.janssen@gmail.com"
    Then Tom receives a valid access token and refresh token
    And Tom sees the home screen with his active bets and friends

  @E2E [AUTH-09]
  Scenario: Incorrect credentials show a generic error without field disclosure
    Given Alex Peeters has an account with email "alex.peeters@outlook.com"
    When Alex submits his email with the incorrect password "WrongPass1"
    Then Alex sees "Incorrect email or password"
    And the error message does not reveal whether the email or password was wrong

  @E2E [AUTH-10]
  Scenario: Account is locked after 5 consecutive failed login attempts
    Given Alex Peeters has an account with email "alex.peeters@outlook.com"
    When Alex fails to log in 5 consecutive times with incorrect passwords
    Then Alex's account is locked with a 15-minute cooldown
    And Alex sees a message indicating the cooldown period

  @E2E [AUTH-11]
  Scenario: Session persists across app restarts
    Given Maria Santos successfully logged in 3 days ago and has not signed out
    When Maria opens the iBetcha app
    Then Maria sees her home screen without needing to re-authenticate

  @E2E [AUTH-12]
  Scenario: Expired refresh token forces re-authentication
    Given Maria Santos has a refresh token that has expired
    When the app attempts to refresh Maria's access token
    Then the refresh attempt is rejected
    And Maria is prompted to log in again

  @E2E [AUTH-13]
  Scenario: Logout invalidates the refresh token
    Given Tom Janssen is logged in with a valid refresh token
    When Tom submits a logout request with his refresh token
    Then the refresh token is revoked
    And subsequent refresh attempts with the same token are rejected


  # ── Onboarding Walkthrough ───────────────────────────────────────────────────

  @E2E [AUTH-14]
  Scenario: New user completes the 3-screen onboarding walkthrough
    Given Maria Santos has just registered and is on the onboarding screen
    When Maria views all 3 onboarding screens and taps "Get Started"
    Then Maria sees the home screen with a first-bet call-to-action visible

  @E2E [AUTH-15]
  Scenario: New user skips onboarding
    Given Tom Janssen has just registered and sees the first onboarding screen
    When Tom taps "Skip"
    Then Tom sees the home screen directly without viewing the remaining onboarding screens

  @E2E [AUTH-16]
  Scenario: Onboarding is shown only once per account
    Given Ben Martens completed onboarding during a previous session
    When Ben opens the iBetcha app in a new session
    Then Ben sees the home screen without any onboarding screens


  # ── Profile Setup ────────────────────────────────────────────────────────────

  @E2E [AUTH-17]
  Scenario: New user sets up a complete profile
    Given Maria Santos is on the profile setup screen after registration
    When Maria enters username "mariasantos", uploads a profile photo, writes bio "Runner and coffee addict", and taps "Save"
    Then Maria's profile is created with username "mariasantos", the uploaded photo, and bio "Runner and coffee addict"
    And Maria is taken to the home screen

  @E2E [AUTH-18]
  Scenario: New user sets up a minimal profile with username only
    Given Tom Janssen is on the profile setup screen
    When Tom enters username "tomj" and taps "Save" without adding a photo or bio
    Then Tom's profile is created with username "tomj", a default avatar, and an empty bio
    And Tom is taken to the home screen

  @E2E [AUTH-19]
  Scenario: Taken username shows alternative suggestions
    Given the username "maria" is already registered by another user
    And Maria Santos is on the profile setup screen
    When Maria enters username "maria" and the system checks availability
    Then Maria sees "Username already taken" with suggested alternatives such as "maria_santos", "maria.s", or "maria27"

  @E2E [AUTH-20]
  Scenario: Username with invalid characters is rejected
    Given Alex Peeters is on the profile setup screen
    When Alex enters username "alex!!!" containing special characters
    Then Alex sees "Username can only contain letters, numbers, underscores, and periods"
    And the profile is not saved

  @E2E [AUTH-21]
  Scenario: Bio exceeding 150 characters is rejected
    Given Maria Santos is editing her profile
    When Maria enters a bio that is 151 characters long
    Then Maria sees "Bio cannot exceed 150 characters"
    And the profile is not saved


  # ── Deep Link Onboarding ─────────────────────────────────────────────────────

  @E2E [AUTH-22]
  Scenario: New user registers via WhatsApp invite deep link and is auto-connected to inviter
    Given Tom Janssen generated invite link "https://ibetcha.app/invite/abc123"
    And Ben Martens received this link and installed iBetcha
    When Ben completes registration using referral code "abc123"
    Then Ben and Tom are automatically connected as friends
    And both Ben and Tom receive a notification "You and Tom/Ben are now friends!"
    And Ben's home screen shows Tom in his friends list
```

---

## Feature: Friends and Social

**Epic:** US-201 (Search and Add), US-202 (Accept/Decline), US-203 (Invite Link), US-204 (Friends List)
**Driving port:** `GET /api/v1/friends/search`, `POST /api/v1/friends/request`, `POST /api/v1/friends/request/{id}/accept`, `POST /api/v1/friends/invite`, `POST /api/v1/users/{id}/block`

```gherkin
Feature: Friends and Social
  Users build their social graph by finding friends, sending requests,
  accepting invitations, and managing connections.

  Background:
    Given the following users exist:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
      | sarahdev    | Sarah De Vries |
      | benm        | Ben Martens    |


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-SOCIAL-01]
  Scenario: User searches for a friend, sends a request, and the friend accepts
    Given Sarah De Vries is on the friend search screen
    When Sarah searches for "alexp" and taps "Add Friend" on Alex Peeters' result
    Then a friend request is sent to Alex
    And Alex receives a push notification "Sarah De Vries wants to be friends"
    When Alex taps "Accept" on the notification
    Then Sarah and Alex are connected as friends
    And both see each other in their friends lists


  # ── Username Search ──────────────────────────────────────────────────────────

  @E2E [SOC-01]
  Scenario: User finds another user by partial username
    Given Sarah De Vries is on the friend search screen
    When Sarah types "alexp" in the search field
    Then the results include "Alex Peeters (@alexpeeters)"
    And Sarah can tap "Add Friend" to send a request to Alex

  @E2E [SOC-02]
  Scenario: Search result shows existing friendship status
    Given Sarah De Vries and Tom Janssen are already friends
    When Sarah searches for "tomj"
    Then Tom Janssen appears in the results with a "Friends" badge
    And there is no "Add Friend" button for Tom

  @E2E [SOC-03]
  Scenario: Search result shows pending request status
    Given Sarah De Vries sent a friend request to Alex Peeters that is not yet accepted
    When Sarah searches for "alexpeeters"
    Then Alex Peeters appears in the results with a "Pending" badge

  @E2E [SOC-04]
  Scenario: Search returns no results and suggests invitation
    Given no user exists with username containing "xyz123abc"
    When Sarah De Vries searches for "xyz123abc"
    Then Sarah sees "No users found" with an option to invite someone to iBetcha

  @E2E [SOC-05]
  Scenario: Blocked user does not appear in search results
    Given Sarah De Vries has blocked "unwanted_user"
    When Sarah searches for "unwanted_user"
    Then the search returns no results for "unwanted_user"

  @E2E [SOC-06]
  Scenario: Blocker does not appear in blocked user's search results
    Given Sarah De Vries has blocked "unwanted_user"
    When "unwanted_user" searches for "sarahdev"
    Then the search returns no results for Sarah


  # ── Friend Requests ──────────────────────────────────────────────────────────

  @E2E [SOC-07]
  Scenario: User accepts a friend request from a push notification
    Given Alex Peeters received a friend request from Sarah De Vries
    When Alex taps "Accept" on the push notification
    Then Alex and Sarah are connected as friends
    And both see each other in their friends lists
    And Sarah is not notified that Alex saw the request notification (only that he accepted)

  @E2E [SOC-08]
  Scenario: User accepts a friend request in-app
    Given Alex Peeters has 2 pending friend requests from Sarah and Ben
    When Alex opens the friends tab and accepts Sarah's request
    Then Sarah appears in Alex's friends list
    And Ben's request remains pending

  @E2E [SOC-09]
  Scenario: User declines a friend request silently
    Given Alex Peeters received a friend request from an unknown user
    When Alex taps "Decline" on the friend request
    Then the request is removed from Alex's view
    And the requester does NOT receive a notification about the decline

  @E2E [SOC-10]
  Scenario: Declining removes the request without trace
    Given Alex Peeters declined a request from "unknown_user"
    When Alex checks his friend requests
    Then "unknown_user"'s request is no longer in the list


  # ── WhatsApp Invite Link ─────────────────────────────────────────────────────

  @E2E [SOC-11]
  Scenario: User generates an invite link and shares it
    Given Tom Janssen is on the friends screen
    When Tom taps "Invite Friend"
    Then a unique invite link is generated in the format "https://ibetcha.app/invite/{code}"
    And the system share sheet opens with a pre-filled message containing the link

  @E2E [SOC-12]
  Scenario: New user installs via invite link and is auto-connected to inviter
    Given Tom Janssen generated invite link with code "abc123"
    And Ben Martens tapped the link, installed iBetcha, and completed registration
    When Ben's registration is processed with referral code "abc123"
    Then Ben and Tom are automatically connected as friends
    And Tom receives "Ben Martens joined iBetcha! You're now friends."
    And Ben receives "You and Tom Janssen are now friends!"

  @E2E [SOC-13]
  Scenario: Existing user taps invite link and receives a friend request
    Given Maria Santos already has an iBetcha account
    And Tom Janssen generated invite link with code "xyz789"
    When Maria taps Tom's invite link while logged in
    Then Maria sees a friend request from Tom within the app
    And Maria is not taken to the registration screen

  @E2E [SOC-14]
  Scenario: Invite link does not expire
    Given Tom Janssen generated invite link with code "old999" 30 days ago
    When Ben Martens taps the link and registers
    Then the referral code "old999" is valid and auto-connects Ben with Tom


  # ── Friends List Management ──────────────────────────────────────────────────

  @E2E [SOC-15]
  Scenario: Friends list is sorted by most recent bet interaction
    Given Sarah De Vries has friends Tom Janssen (last bet 1 hour ago) and Alex Peeters (last bet 5 days ago)
    When Sarah opens the friends tab
    Then Tom Janssen appears above Alex Peeters in the list

  @E2E [SOC-16]
  Scenario: Friends list shows head-to-head snippet per friend
    Given Maria Santos has 3 wins and 2 losses against Tom Janssen
    When Maria opens the friends tab
    Then Tom Janssen's row shows the head-to-head record "3-2" (Maria leads)

  @E2E [SOC-17]
  Scenario: User removes a friend with confirmation
    Given Sarah De Vries and "random_user" are friends
    When Sarah taps the remove option on "random_user" and confirms the action
    Then "random_user" is removed from Sarah's friends list
    And Sarah is removed from "random_user"'s friends list (mutual removal)

  @E2E [SOC-18]
  Scenario: Empty friends list shows guidance for new users
    Given Ben Martens has no friends on iBetcha
    When Ben opens the friends tab
    Then Ben sees an empty state with clear options to search for friends or send an invite link


  # ── Block a User ─────────────────────────────────────────────────────────────

  @E2E [SOC-19]
  Scenario: Blocking removes friendship and cancels pending bets
    Given Sarah De Vries and "unwanted_user" are friends
    And Sarah has a bet in PENDING_ACCEPTANCE state with "unwanted_user"
    When Sarah taps "Block User" on "unwanted_user"'s profile and confirms
    Then "unwanted_user" is removed from Sarah's friends list
    And the PENDING_ACCEPTANCE bet between them is automatically CANCELLED
    And neither can find or contact the other via search

  @E2E [SOC-20]
  Scenario: Blocked user is not notified of the block
    Given Sarah De Vries blocks "unwanted_user"
    When the block takes effect
    Then "unwanted_user" does NOT receive any notification about being blocked

  @E2E [SOC-21]
  Scenario: Active bets between blocker and blocked are cancelled on block
    Given Sarah De Vries has a bet in PENDING_ACCEPTANCE with "unwanted_user"
    When Sarah blocks "unwanted_user"
    Then the PENDING_ACCEPTANCE bet is automatically moved to CANCELLED state
    And the cancellation is logged in bet history

  @E2E [SOC-22]
  Scenario: User cannot send a friend request to a blocked user
    Given Sarah De Vries has blocked "unwanted_user"
    When "unwanted_user" attempts to send a friend request to Sarah
    Then the request is silently blocked and Sarah does not receive it
```

---

## Feature: Bet Creation

**Epic:** US-301 (Quick Bet), US-302 (Full Bet)
**Driving port:** `POST /api/v1/bets`

```gherkin
Feature: Bet Creation
  Users create bets with friends using either the Quick Bet flow (3 fields)
  or the Full Bet flow (all optional fields). Both produce the same Bet aggregate.

  Background:
    Given the following users exist and are friends with each other:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
      | sarahdev    | Sarah De Vries |
      | benm        | Ben Martens    |
    And the API is authenticated as "tomj" unless stated otherwise


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-BET-01]
  Scenario: Creator creates a quick bet and invitee receives a notification
    Given Tom Janssen is on the home screen
    When Tom taps the create button, selects Maria Santos, types "I'll beat you on Sunday's ride", sets stake "Loser buys coffee", and taps Send
    Then a bet is created in PENDING_ACCEPTANCE state
    And the bet has description "I'll beat you on Sunday's ride" and stake "Loser buys coffee"
    And Maria Santos receives a push notification "Tom challenged you!" with Accept and Decline action buttons
    And the bet has no jury assigned
    And the bet acceptance window expires 48 hours from now


  # ── Quick Bet Creation ───────────────────────────────────────────────────────

  @E2E [BET-01]
  Scenario: Quick bet requires exactly 3 fields and creates bet in PENDING_ACCEPTANCE
    Given Tom Janssen submits a quick bet to Maria Santos with description "I'll beat you on Sunday's ride" and stake "Loser buys coffee"
    Then the bet is created in PENDING_ACCEPTANCE state
    And the bet has no jury assigned (outcome will use majority approval)
    And the acceptance window is set to 48 hours from creation time

  @E2E [BET-02]
  Scenario: Quick bet to multiple friends notifies all participants
    Given Sarah De Vries creates a quick bet selecting Tom Janssen, Maria Santos, and Ben Martens as participants
    And the description is "Who runs the fastest 5K Saturday" with stake "Last place buys pizza for everyone"
    When the bet is submitted
    Then all three of Tom, Maria, and Ben receive push notifications to accept or decline
    And the bet is in PENDING_ACCEPTANCE state

  @E2E [BET-03]
  Scenario: Quick bet creation fails when no friends are selected
    Given Tom Janssen is creating a quick bet
    When Tom submits without selecting any friends
    Then Tom sees "Select at least one friend to bet with"
    And the bet is not created

  @E2E [BET-04]
  Scenario: Validation error preserves form state
    Given Tom Janssen has typed description "I'll beat you on Sunday's ride" and stake "Loser buys coffee"
    When Tom submits without selecting any friends and validation fails
    Then Tom's typed description "I'll beat you on Sunday's ride" and stake "Loser buys coffee" are preserved
    And the form is not reset

  @E2E [BET-05]
  Scenario: Quick bet description field is required
    Given Tom Janssen selected Maria Santos as the friend
    When Tom submits without entering a description
    Then Tom sees a validation message that description is required
    And the bet is not created

  @E2E [BET-06]
  Scenario: Quick bet stake field is required
    Given Tom Janssen selected Maria Santos and typed description "I'll beat you on Sunday's ride"
    When Tom submits without entering a stake
    Then Tom sees a validation message that stake is required
    And the bet is not created

  @E2E [BET-07]
  Scenario: User cannot create a bet with a non-friend
    Given Tom Janssen is not friends with "stranger_user"
    When Tom attempts to create a bet with "stranger_user" as participant
    Then the bet creation is rejected with "You can only bet with friends"

  @E2E [BET-08]
  Scenario: Bet requires at least 2 participants total (creator + at least 1 invitee)
    Given Tom Janssen selects no invitees (only himself)
    When Tom submits the bet
    Then the bet is rejected with "A bet must have at least one other participant"


  # ── Full Bet Creation ────────────────────────────────────────────────────────

  @E2E [BET-09]
  Scenario: Full bet with all optional fields creates bet with complete details
    Given Sarah De Vries is creating a bet and taps "More options"
    When Sarah selects Tom Janssen, Maria Santos, and Ben Martens as participants
    And adds title "Weekend 5K Challenge"
    And sets description "Who runs the fastest 5K on Saturday"
    And sets stake "Pizza for everyone"
    And sets deadline to this Saturday at 18:00
    And designates Alex Peeters as jury
    And taps Send
    Then a bet is created in PENDING_ACCEPTANCE state with all specified fields
    And Tom, Maria, and Ben each receive a push notification to accept or decline
    And Alex receives a notification "You have been designated as jury for Sarah's bet"

  @E2E [BET-10]
  Scenario: Jury cannot be a participant in the same bet
    Given Sarah De Vries is creating a bet with Tom Janssen and Maria Santos as participants
    When Sarah tries to designate Tom Janssen as jury
    Then Sarah sees "Jury cannot be a participant in the bet"
    And Tom Janssen is not selectable as jury

  @E2E [BET-11]
  Scenario: Bet deadline must be in the future
    Given Sarah De Vries is setting a deadline for a bet
    When Sarah sets the deadline to a date in the past
    Then Sarah sees "Deadline must be in the future"
    And the bet is not submitted

  @E2E [BET-12]
  Scenario: Full form expands from quick bet without resetting existing fields
    Given Tom Janssen has entered description "I'll beat you" and stake "Coffee" in the quick bet form
    When Tom taps "More options" to expand the full form
    Then the description "I'll beat you" and stake "Coffee" remain populated in the expanded form

  @E2E [BET-13]
  Scenario: Full bet review screen shows all fields before sending
    Given Sarah De Vries has filled in the full bet form with all fields
    When Sarah navigates to the review screen
    Then the review screen shows: participants, description, stake, title, deadline, jury, and evidence toggle

  @E2E [BET-14]
  Scenario: Quick bet and full bet both produce the same Bet aggregate type
    Given Tom creates a quick bet with description "Quick challenge" and stake "Coffee"
    And Sarah creates a full bet with title "Full Challenge", description "Structured bet", stake "Dinner", deadline, and jury
    When both bets are created
    Then both bets are in PENDING_ACCEPTANCE state
    And both bets share the same lifecycle and state machine behavior
```

---

## Feature: Bet Acceptance and Decline

**Epic:** US-401 (Accept), US-402 (Decline)
**Driving port:** `POST /api/v1/bets/{betId}/accept`, `POST /api/v1/bets/{betId}/decline`

```gherkin
Feature: Bet Acceptance and Decline
  Participants respond to bet invitations via push notification actions or in-app.
  A bet becomes ACTIVE only when all remaining participants have accepted.

  Background:
    Given the following users exist and are friends with each other:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
      | sarahdev    | Sarah De Vries |
      | benm        | Ben Martens    |
    And Tom Janssen created a bet "I'll beat you on Sunday's ride" with stake "Loser buys coffee" against Maria Santos
    And the bet is in PENDING_ACCEPTANCE state with id "bet-sunday-ride"


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-ACCEPT-01]
  Scenario: Participant accepts a bet via push notification and bet becomes ACTIVE
    Given Maria Santos received a push notification about "bet-sunday-ride"
    When Maria taps "Accept" on the push notification without opening the app
    Then "bet-sunday-ride" moves to ACTIVE state
    And Tom receives a push notification "Maria accepted your bet! Game on."
    And both Tom and Maria receive "Bet is ON!" notification


  # ── Accept via Notification ──────────────────────────────────────────────────

  @E2E [ACC-01]
  Scenario: User accepts bet from actionable push notification without opening app
    Given Maria Santos received a push notification about "bet-sunday-ride"
    When Maria taps "Accept" on the notification
    Then the bet is accepted for Maria
    And Tom receives a notification that Maria accepted

  @E2E [ACC-02]
  Scenario: User accepts bet from bet detail screen with head-to-head context
    Given Maria Santos opens "bet-sunday-ride" from the app's pending bets list
    When the bet detail screen loads
    Then Maria sees the head-to-head record "You vs Tom: 3-2" (Maria leads)
    When Maria taps "Accept"
    Then the bet is accepted and all participants are notified


  # ── Multi-Person Acceptance ──────────────────────────────────────────────────

  @E2E [ACC-03]
  Scenario: Multi-person bet remains PENDING_ACCEPTANCE until all participants accept
    Given Sarah De Vries created "Weekend 5K Challenge" with Tom, Maria, and Ben in PENDING_ACCEPTANCE
    When Tom Janssen accepts and Maria Santos accepts but Ben Martens has not responded
    Then the bet status remains PENDING_ACCEPTANCE
    And the bet detail shows "Waiting for Ben Martens"

  @E2E [ACC-04]
  Scenario: Multi-person bet becomes ACTIVE when last participant accepts
    Given "Weekend 5K Challenge" is in PENDING_ACCEPTANCE with only Ben Martens outstanding
    When Ben Martens accepts the bet
    Then the bet transitions to ACTIVE state
    And all participants (Sarah, Tom, Maria, Ben) receive a "Bet is ON!" notification

  @E2E [ACC-05]
  Scenario: Accepting a bet that has already expired is rejected
    Given "bet-sunday-ride" has been in PENDING_ACCEPTANCE for more than 48 hours and has EXPIRED
    When Maria Santos attempts to accept the bet
    Then the accept request is rejected with "This bet has expired"


  # ── Decline ──────────────────────────────────────────────────────────────────

  @E2E [ACC-06]
  Scenario: User declines bet from bet detail screen
    Given Alex Peeters has a pending invitation for "Weekend 5K Challenge"
    When Alex taps "Decline" on the bet detail screen
    Then the bet is removed from Alex's view
    And Sarah (creator) receives a neutral notification "Alex Peeters declined your bet"
    And the notification language is factual and not shaming

  @E2E [ACC-07]
  Scenario: User declines bet from push notification without opening app
    Given Alex Peeters received a push notification about a bet
    When Alex taps "Decline" on the notification
    Then the bet is declined without Alex opening the app

  @E2E [ACC-08]
  Scenario: Declining a multi-person bet removes only the decliner
    Given Sarah created a 4-person bet with Tom, Maria, Ben, and Alex in PENDING_ACCEPTANCE
    When Alex Peeters declines
    Then Alex is removed from the participants list
    And the bet continues in PENDING_ACCEPTANCE for the remaining participants (Tom, Maria, Ben)

  @E2E [ACC-09]
  Scenario: Declined bet does not appear in the decliner's history
    Given Alex Peeters declined "Weekend 5K Challenge"
    When Alex views his bet history
    Then "Weekend 5K Challenge" does not appear in Alex's history

  @E2E [ACC-10]
  Scenario: Bet expires when decline reduces participants below 2
    Given Tom Janssen created a 2-person bet with only Maria Santos as invitee
    When Maria Santos declines the bet
    Then the bet transitions to EXPIRED state (INSUFFICIENT_PARTICIPANTS)
    And Tom receives a notification "Maria declined. Your bet has expired (not enough participants)"
```

---

## Feature: Bet Expiry and Cancellation

**Epic:** US-403 (Timeout), US-404 (Cancel)
**Driving port:** `DELETE /api/v1/bets/{betId}` (cancel), System scheduler (expiry)

```gherkin
Feature: Bet Expiry and Cancellation
  Bets in PENDING_ACCEPTANCE expire automatically after 48 hours.
  Only the creator can cancel a bet, and only before it becomes ACTIVE.

  Background:
    Given the following users exist and are friends:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | sarahdev    | Sarah De Vries |
      | benm        | Ben Martens    |


  # ── Acceptance Timeout ───────────────────────────────────────────────────────

  @E2E [EXP-01]
  Scenario: Reminder notification is sent at the 24-hour mark
    Given Tom Janssen sent a bet to Maria Santos 24 hours ago with no response
    When the 24-hour mark is reached
    Then Maria receives a push notification "Tom's bet is waiting for you! 24 hours left to respond"

  @E2E [EXP-02]
  Scenario: Bet expires automatically after 48 hours without full acceptance
    Given Tom Janssen sent a bet to Maria Santos and 48 hours have elapsed with no response from Maria
    When the system scheduler runs the expiry job
    Then the bet transitions from PENDING_ACCEPTANCE to EXPIRED
    And both Tom and Maria receive a notification "Bet expired: no response within 48 hours"

  @E2E [EXP-03]
  Scenario: Partially accepted multi-person bet expires on 48-hour timeout
    Given Sarah created a 3-person bet with Tom and Ben in PENDING_ACCEPTANCE
    And Tom accepted but Ben has not responded
    When 48 hours pass since bet creation
    Then the bet transitions to EXPIRED
    And Sarah, Tom, and Ben are all notified of the expiry

  @E2E [EXP-04]
  Scenario: Acceptance just before the 48-hour timeout prevents expiry
    Given Ben Martens received a bet 47 hours and 59 minutes ago
    When Ben accepts the bet at hour 47
    Then the bet is accepted normally and transitions to ACTIVE or PENDING_ACCEPTANCE (others may still be pending)
    And the bet does not expire

  @E2E [EXP-05]
  Scenario: Expired bet does not count toward win/loss records
    Given Tom Janssen's bet to Maria Santos expired after 48 hours
    When both Tom and Maria view their profiles
    Then neither Tom's nor Maria's win/loss record is affected by the expired bet

  @E2E [EXP-06]
  Scenario: Expired bet is marked as EXPIRED, not CANCELLED
    Given Tom Janssen's bet expired after 48 hours
    When Tom views the bet in his history
    Then the bet shows status "Expired" (not "Cancelled")


  # ── Creator Cancels Bet ──────────────────────────────────────────────────────

  @E2E [CAN-01]
  Scenario: Creator cancels a bet that is still in PENDING_ACCEPTANCE
    Given Sarah De Vries created "Weekend 5K Challenge" with Tom, Maria, and Ben in PENDING_ACCEPTANCE
    And Tom accepted but Maria and Ben are still pending
    When Sarah taps "Cancel Bet" and confirms the cancellation dialog
    Then the bet transitions from PENDING_ACCEPTANCE to CANCELLED
    And all invited participants (Tom, Maria, Ben) receive "Sarah De Vries cancelled the bet"

  @E2E [CAN-02]
  Scenario: Creator cancels a bet immediately after creation with no responses yet
    Given Sarah De Vries just created a bet 30 seconds ago with no acceptances
    When Sarah taps "Cancel Bet" and confirms
    Then the bet is CANCELLED and all invited participants are notified

  @E2E [CAN-03]
  Scenario: Cancel option is not available for bets in ACTIVE state
    Given Sarah De Vries created a bet that all participants have accepted (ACTIVE state)
    When Sarah views the bet detail screen
    Then the "Cancel Bet" option is not visible or available

  @E2E [CAN-04]
  Scenario: Cancelled bet does not count toward win/loss records
    Given Sarah De Vries cancelled "Weekend 5K Challenge" before it went ACTIVE
    When all participants view their profiles
    Then no win/loss records are affected by the cancelled bet

  @E2E [CAN-05]
  Scenario: Only the creator can cancel a bet
    Given Tom Janssen is a participant (not creator) in Sarah's bet in PENDING_ACCEPTANCE
    When Tom attempts to cancel the bet
    Then the cancellation is rejected with "Only the bet creator can cancel"

  @E2E [CAN-06]
  Scenario: Cancellation requires a confirmation dialog
    Given Sarah De Vries is on the bet detail screen for a PENDING_ACCEPTANCE bet
    When Sarah taps "Cancel Bet"
    Then a confirmation dialog appears asking Sarah to confirm the cancellation
    And the bet is not cancelled until Sarah confirms
```

---

## Feature: Bet Completion and Outcome Declaration

**Epic:** US-405 (Mark Complete), US-406 (Resolution and Stats)
**Driving port:** `POST /api/v1/bets/{betId}/complete`, `POST /api/v1/bets/{betId}/vote`

```gherkin
Feature: Bet Completion and Outcome Declaration
  Any participant in an ACTIVE bet can mark it complete and declare a winner.
  This triggers the appropriate approval flow: jury verdict or participant majority vote.
  Concession (selecting someone else as winner) resolves immediately without approval.

  Background:
    Given the following users exist and are friends:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
      | sarahdev    | Sarah De Vries |
      | benm        | Ben Martens    |


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-COMPLETE-01]
  Scenario: Tom declares victory on a 2-person no-jury bet, Maria approves, bet resolves
    Given Tom Janssen and Maria Santos have an ACTIVE bet "I'll beat you on Sunday's ride" with no jury
    When Tom taps "Mark Complete" and selects himself as winner
    Then the bet transitions to PENDING_APPROVAL
    And Maria receives "Tom Janssen claims he won. Do you agree?"
    When Maria approves the outcome
    Then the bet transitions to RESOLVED with Tom as winner
    And Tom's win record increases by 1
    And Maria's loss record increases by 1
    And Tom receives a celebration notification with updated head-to-head record


  # ── Mark Complete: Participant Claims Victory ─────────────────────────────────

  @E2E [COMP-01]
  Scenario: Any participant (not just creator) can initiate bet completion
    Given Sarah De Vries, Tom Janssen, Maria Santos, and Ben Martens have an ACTIVE bet with no jury
    When Maria Santos (not the creator) taps "Mark Complete" and selects Tom as winner
    Then the bet transitions to PENDING_APPROVAL
    And all other participants are notified to vote on the outcome

  @E2E [COMP-02]
  Scenario: Marking complete on a bet with jury moves to PENDING_JURY_VERDICT
    Given Tom Janssen and Maria Santos have an ACTIVE bet with Alex Peeters as jury
    When Tom taps "Mark Complete" and selects himself as winner
    Then the bet transitions to PENDING_JURY_VERDICT
    And Alex Peeters receives a notification to review and approve the outcome

  @E2E [COMP-03]
  Scenario: Marking complete on a no-jury bet moves to PENDING_APPROVAL
    Given Tom Janssen and Maria Santos have an ACTIVE bet with no jury
    When Tom taps "Mark Complete" and selects himself as winner
    Then the bet transitions to PENDING_APPROVAL
    And Maria Santos receives "Tom Janssen claims he won. Do you agree?"

  @E2E [COMP-04]
  Scenario: Selecting another participant as winner auto-resolves as a concession
    Given Maria Santos and Tom Janssen have an ACTIVE bet
    When Maria taps "Mark Complete" and selects Tom Janssen as winner (not herself)
    Then the bet immediately transitions to RESOLVED with Tom as winner
    And no jury or approval flow is triggered
    And Tom's win record and Maria's loss record are updated immediately

  @E2E [COMP-05]
  Scenario: Cannot mark complete on a bet that is not ACTIVE
    Given a bet is in PENDING_ACCEPTANCE state
    When Tom Janssen attempts to mark the bet complete
    Then the request is rejected with "Bet is not active"

  @E2E [COMP-06]
  Scenario: Cannot mark complete if not a participant in the bet
    Given Tom Janssen and Maria Santos have an ACTIVE bet
    When Alex Peeters (not a participant) attempts to mark the bet complete
    Then the request is rejected with "You are not a participant in this bet"

  @E2E [COMP-07]
  Scenario: Winner selection shows all participants as options
    Given Sarah, Tom, Maria, and Ben have an ACTIVE bet
    When Sarah opens the "Mark Complete" flow
    Then the winner selection shows Sarah, Tom, Maria, and Ben as options

  @E2E [COMP-08]
  Scenario: Cannot submit a new outcome claim when one is already PENDING_APPROVAL
    Given Tom Janssen already submitted an outcome claim that is in PENDING_APPROVAL
    When Tom attempts to submit another outcome claim for the same bet
    Then the request is rejected with "An outcome is already awaiting approval"


  # ── No-Jury Majority Vote (3+ Person) ────────────────────────────────────────

  @E2E [COMP-09]
  Scenario: Majority approval resolves a 4-person no-jury bet
    Given Sarah's 5K bet has 4 participants (Sarah, Tom, Maria, Ben) with no jury
    And Sarah declared Tom as winner (bet in PENDING_APPROVAL)
    And Tom has an implicit approval as declared winner
    When Maria Santos approves the outcome (2 of 4 have approved; need 3 for majority)
    And Ben Martens approves (3 of 4 approved = majority reached)
    Then the bet transitions to RESOLVED with Tom as winner
    And all 4 participants are notified of the resolution

  @E2E [COMP-10]
  Scenario: Majority not reached leaves bet in PENDING_APPROVAL
    Given Sarah's 5K bet with 4 participants is in PENDING_APPROVAL with Tom declared winner
    When Maria Santos disputes and Ben Martens disputes (Tom approved implicitly, Sarah approved: 2-2 split)
    Then the bet remains in PENDING_APPROVAL
    And participants can still change their votes or appoint a jury

  @E2E [COMP-11]
  Scenario: Declarer's selection counts as implicit approval
    Given Sarah, Tom, and Maria have a 3-person bet in PENDING_APPROVAL
    And Sarah declared Tom as winner (implicit: Tom approved, Sarah approved = 2 votes)
    When Maria Santos approves (3rd vote = majority in 3-person bet)
    Then the bet transitions to RESOLVED with Tom as winner

  @E2E [COMP-12]
  Scenario: 3-person bet resolves on 2-of-3 majority
    Given Sarah, Tom, and Maria have a 3-person bet in PENDING_APPROVAL
    And Sarah declared Tom as winner (Tom implicit vote + Sarah's implicit = 2 votes already)
    Then the bet immediately transitions to RESOLVED with majority already reached (2 of 3)
    And Maria receives a notification about the outcome


  # ── Bet Resolution and Stats Update ──────────────────────────────────────────

  @E2E [COMP-13]
  Scenario: Winner sees celebration screen with updated stats after resolution
    Given Tom Janssen's bet against Maria Santos was approved with Tom as winner
    When the bet transitions to RESOLVED
    Then Tom sees a celebration screen showing "You Won!"
    And Tom's updated record versus Maria (e.g. 4 wins, 2 losses)
    And Tom's current winning streak

  @E2E [COMP-14]
  Scenario: Loser sees factual outcome screen without shaming language
    Given Maria Santos lost the cycling bet against Tom Janssen
    When the bet transitions to RESOLVED
    Then Maria sees a neutral result screen with the factual outcome
    And the message does not use any shaming language
    And Maria sees her updated record versus Tom

  @E2E [COMP-15]
  Scenario: Win/loss records update for all participants on resolution
    Given Tom Janssen's overall record is 12 wins and 8 losses before resolution
    When Tom wins a bet against Maria Santos and the bet transitions to RESOLVED
    Then Tom's record shows 13 wins and 8 losses (62% win rate)
    And Maria's loss count increases by 1

  @E2E [COMP-16]
  Scenario: Head-to-head record updates for both participants on resolution
    Given Tom Janssen leads Maria Santos 3-2 in their head-to-head record
    When Tom wins another bet against Maria
    Then Tom's head-to-head shows 4-2 against Maria
    And Maria's head-to-head shows 2-4 against Tom

  @E2E [COMP-17]
  Scenario: Win streak is tracked and displayed on resolution
    Given Tom Janssen has won his last 2 consecutive resolved bets
    When Tom wins a 3rd consecutive bet
    Then Tom's profile shows "Current streak: 3W"
    And Tom receives a notification "You're on a 3-bet winning streak!"

  @E2E [COMP-18]
  Scenario: Win streak resets on a loss
    Given Tom Janssen has a current streak of 3 wins
    When Tom loses a bet that transitions to RESOLVED
    Then Tom's streak resets to 0 (or starts a new loss streak of 1L)

  @E2E [COMP-19]
  Scenario: Rivalry milestone notification fires when head-to-head records tie
    Given Tom Janssen leads Maria Santos 4-3 in head-to-head record
    When Maria wins a bet against Tom bringing the record to 4-4
    Then both Tom and Maria receive "You and Maria/Tom are now tied at 4-4!"
```

---

## Feature: Jury System

**Epic:** US-501 (Jury Approval), US-502 (Jury Timeout)
**Driving port:** `POST /api/v1/bets/{betId}/jury/approve`, `POST /api/v1/bets/{betId}/jury/reject`

```gherkin
Feature: Jury System
  A designated jury member (non-participant) has final say on bet outcomes.
  If the jury does not respond within 7 days, the outcome escalates
  to participant majority vote.

  Background:
    Given the following users exist and are friends:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
      | sarahdev    | Sarah De Vries |
    And Tom Janssen and Maria Santos have an ACTIVE bet "Sunday's cycling race" with Alex Peeters as jury
    And Tom submitted an outcome claim declaring himself winner
    And the bet is now in PENDING_JURY_VERDICT state


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-JURY-01]
  Scenario: Jury receives claim notification, reviews evidence, approves, bet resolves
    Given Alex Peeters received a push notification "Tom claims he won the cycling bet. Review?"
    When Alex opens the jury review screen and views the bet details and evidence
    Then Alex sees the bet description, declared winner (Tom), stake, and any uploaded evidence
    When Alex taps "Approve"
    Then the bet transitions from PENDING_JURY_VERDICT to RESOLVED
    And both Tom and Maria are notified of the resolution


  # ── Jury Approval ────────────────────────────────────────────────────────────

  @E2E [JURY-01]
  Scenario: Jury approves outcome from in-app review screen
    Given Alex Peeters opens the jury review screen for Tom vs Maria's bet
    When Alex views bet description "Sunday's cycling race", declared winner Tom, and evidence photo
    And Alex taps "Approve"
    Then the bet transitions from PENDING_JURY_VERDICT to RESOLVED with Tom as winner
    And both Tom and Maria receive resolution notifications

  @E2E [JURY-02]
  Scenario: Jury review screen shows all relevant information for the decision
    Given Alex Peeters opens the jury review screen
    When the screen loads
    Then Alex sees:
      | Field          | Content                       |
      | Bet description | Sunday's cycling race        |
      | Declared winner | Tom Janssen                  |
      | Stake          | Loser buys coffee             |
      | Evidence       | Uploaded photo (if available) |
      | Action buttons | Approve and Reject            |

  @E2E [JURY-03]
  Scenario: Jury approves from actionable push notification without opening app
    Given Alex Peeters received a push notification about judging Tom vs Maria's bet
    When Alex taps "Approve" directly on the notification
    Then the bet transitions to RESOLVED without Alex needing to open the app

  @E2E [JURY-04]
  Scenario: Jury rejects the claimed outcome and bet returns to ACTIVE
    Given Alex Peeters is reviewing Tom's win claim for "Sunday's cycling race"
    When Alex taps "Reject" on the jury review screen
    Then the bet transitions from PENDING_JURY_VERDICT back to ACTIVE state
    And Tom and Maria receive "Alex Peeters rejected the outcome. Bet is still active."
    And Tom or Maria can submit a new outcome claim

  @E2E [JURY-05]
  Scenario: Jury can only act once per outcome claim
    Given Alex Peeters already approved the outcome for "Sunday's cycling race"
    When Alex attempts to approve again
    Then the second approval is rejected with "You have already reviewed this outcome"

  @E2E [JURY-06]
  Scenario: Non-designated user cannot act as jury
    Given Alex Peeters is the designated jury for "Sunday's cycling race"
    When Tom Janssen (a participant) attempts to call the jury approve endpoint
    Then the request is rejected with "You are not the designated jury for this bet"

  @E2E [JURY-07]
  Scenario: Jury cannot vote after the bet has already been resolved
    Given "Sunday's cycling race" was already resolved via participant majority vote
    When Alex Peeters attempts to approve the jury outcome
    Then the request is rejected with "This bet has already been resolved"


  # ── Jury Timeout and Escalation ───────────────────────────────────────────────

  @E2E [JURY-08]
  Scenario: Jury receives reminder notification at day 3
    Given Alex Peeters has not responded to the jury review for 3 days
    When the 3-day mark is reached
    Then Alex receives "Reminder: Tom vs Maria's bet is waiting for your verdict"

  @E2E [JURY-09]
  Scenario: Jury receives final warning notification at day 6
    Given Alex Peeters has not responded to the jury review for 6 days
    When the 6-day mark is reached
    Then Alex receives "Last chance: 24 hours to review before the bet escalates to participant vote"

  @E2E [JURY-10]
  Scenario: Jury timeout after 7 days escalates bet to participant majority vote
    Given Alex Peeters has not responded to the jury review for 7 days
    When the 7-day timeout job fires
    Then the bet transitions from PENDING_JURY_VERDICT to PENDING_APPROVAL
    And all participants (Tom and Maria) receive "Jury did not respond. Outcome is now decided by participant vote."
    And the original outcome claim (Tom as winner) is preserved for participant voting

  @E2E [JURY-11]
  Scenario: Jury cannot approve after the timeout has triggered escalation
    Given Alex Peeters' 7-day jury window has elapsed and the bet has moved to PENDING_APPROVAL
    When Alex attempts to approve the outcome
    Then the request is rejected with "Jury review period has ended"
```

---

## Feature: Dispute Resolution

**Epic:** US-1101 (Majority Approval), US-1102 (Two-Person Dispute)
**Driving port:** `POST /api/v1/bets/{betId}/vote`, `POST /api/v1/bets/{betId}/concede`, `POST /api/v1/bets/{betId}/dispute-jury`

```gherkin
Feature: Dispute Resolution
  When a 2-person no-jury bet has conflicting outcome claims, the bet enters
  DISPUTED state. Resolution options: one participant concedes,
  or both agree to appoint a jury.

  Background:
    Given the following users exist and are friends:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | alexpeeters | Alex Peeters   |
    And Tom Janssen and Maria Santos have a 2-person ACTIVE bet "Sunday's ride" with no jury


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-DISPUTE-01]
  Scenario: Both claim victory, bet enters DISPUTED state, one concedes to resolve
    Given Tom declared himself winner (bet transitions to PENDING_APPROVAL)
    When Maria disputes Tom's claim
    Then the bet transitions to DISPUTED state
    And both Tom and Maria see "Bet disputed. Concede or appoint a jury." on the bet detail screen
    When Maria taps "Concede" on the disputed bet
    Then the bet transitions to RESOLVED with Tom as winner
    And both Tom and Maria's records are updated


  # ── Entering Dispute ──────────────────────────────────────────────────────────

  @E2E [DIS-01]
  Scenario: 2-person no-jury bet enters DISPUTED when both claim victory
    Given Tom declared himself winner of "Sunday's ride" (bet moves to PENDING_APPROVAL)
    When Maria Santos votes to DISPUTE Tom's claim
    Then the bet transitions from PENDING_APPROVAL to DISPUTED
    And both Tom and Maria receive "Bet disputed. Concede or appoint a jury."

  @E2E [DIS-02]
  Scenario: Disputed bet is visible on both participants' profiles
    Given "Sunday's ride" is in DISPUTED state between Tom and Maria
    When a friend views Tom's profile
    Then "Sunday's ride" is visible with a "Disputed" badge

  @E2E [DIS-03]
  Scenario: DISPUTED bet does not count toward win/loss records
    Given "Sunday's ride" is in DISPUTED state
    When Tom and Maria view their profiles
    Then neither Tom's nor Maria's win/loss record is affected by the disputed bet


  # ── Resolution: Concession ───────────────────────────────────────────────────

  @E2E [DIS-04]
  Scenario: Dispute resolved when one participant concedes
    Given "Sunday's ride" is in DISPUTED state
    When Maria Santos taps "Concede" on the disputed bet detail screen
    Then the bet transitions from DISPUTED to RESOLVED with Tom as winner
    And Tom's win record and Maria's loss record are updated
    And both receive resolution notifications

  @E2E [DIS-05]
  Scenario: Either participant can concede to resolve a dispute
    Given "Sunday's ride" is in DISPUTED state
    When Tom Janssen (who originally claimed victory) taps "Concede"
    Then the bet transitions to RESOLVED with Maria as winner


  # ── Resolution: Appoint Jury ──────────────────────────────────────────────────

  @E2E [DIS-06]
  Scenario: Dispute resolved by appointing a jury after both agree
    Given "Sunday's ride" is in DISPUTED state
    When Tom proposes Alex Peeters as dispute jury
    And Maria confirms Alex Peeters as dispute jury
    Then the bet transitions from DISPUTED to PENDING_JURY_VERDICT
    And Alex receives a notification to act as jury for the disputed bet

  @E2E [DIS-07]
  Scenario: Appointing a dispute jury requires both participants to agree
    Given "Sunday's ride" is in DISPUTED state
    When Tom proposes Alex Peeters as dispute jury
    But Maria has not yet confirmed the selection
    Then the bet remains in DISPUTED state
    And Alex is not yet notified

  @E2E [DIS-08]
  Scenario: Proposed dispute jury must not be a participant in the bet
    Given "Sunday's ride" is in DISPUTED state between Tom and Maria
    When Tom proposes Maria Santos as dispute jury (Maria is a participant)
    Then the proposal is rejected with "Jury cannot be a participant in the bet"

  @E2E [DIS-09]
  Scenario: 3-person bet with split vote stays in PENDING_APPROVAL (no DISPUTED state)
    Given Sarah, Tom, and Maria have a 3-person bet in PENDING_APPROVAL with Tom declared winner
    When Sarah approves and Maria disputes (2 approve, 1 dispute = majority for approval)
    Then the bet resolves to RESOLVED (majority reached)
    And the DISPUTED state is not used for 3+ person bets

  @E2E [DIS-10]
  Scenario: Ongoing dispute stays DISPUTED indefinitely until resolved
    Given "Sunday's ride" has been in DISPUTED state for 30 days
    When no action has been taken by either participant
    Then the bet remains in DISPUTED state
    And it continues to show on both participants' profiles
```

---

## Feature: Evidence

**Epic:** US-601 (Evidence Upload)
**Driving port:** `POST /api/v1/evidence/upload-url`, `POST /api/v1/evidence/confirm`

```gherkin
Feature: Evidence
  Evidence (photo or video) is optionally attached during the Mark Complete flow.
  It is uploaded directly to S3 via a presigned URL.
  Evidence is visible to the jury and all participants.

  Background:
    Given Tom Janssen and Maria Santos have an ACTIVE bet "Sunday's cycling race" with no jury
    And Tom is in the process of marking the bet complete


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-EVIDENCE-01]
  Scenario: Tom uploads a photo as evidence and it is visible to participants on the bet detail
    Given Tom Janssen is in the Mark Complete flow
    When Tom taps "Add Evidence", selects a JPEG photo from his gallery under 50MB
    And the photo is compressed on-device and uploaded to S3 via a presigned URL
    And Tom confirms the upload via the confirm endpoint
    Then the evidence is attached to the outcome claim
    And Maria Santos can see the evidence photo when viewing the bet detail


  # ── Photo Upload ─────────────────────────────────────────────────────────────

  @E2E [EVI-01]
  Scenario: User uploads a JPEG photo as evidence
    Given Tom Janssen is marking the cycling bet complete
    When Tom requests an evidence upload URL for a JPEG file
    And Tom uploads the compressed JPEG directly to S3 via the presigned URL
    And Tom confirms the upload to the backend
    Then the evidence is attached to the bet outcome
    And the evidence type is recorded as JPEG

  @E2E [EVI-02]
  Scenario: User uploads a PNG photo as evidence
    Given Tom Janssen is marking the cycling bet complete
    When Tom uploads a compressed PNG image via presigned URL and confirms
    Then the PNG evidence is attached to the bet outcome

  @E2E [EVI-03]
  Scenario: Presigned upload URL is valid for 15 minutes
    Given Tom Janssen requested an evidence upload URL
    When Tom attempts to upload a file using the presigned URL after 16 minutes
    Then the upload is rejected by S3 as the URL has expired

  @E2E [EVI-04]
  Scenario: Backend rejects evidence confirmation when S3 object does not exist
    Given Tom Janssen calls the confirm endpoint with an S3 key that was never uploaded
    When the backend verifies the object in S3
    Then the confirmation is rejected with "Evidence file not found in storage"


  # ── Video Upload ─────────────────────────────────────────────────────────────

  @E2E [EVI-05]
  Scenario: User uploads a video as evidence after on-device compression
    Given Tom Janssen wants to share video proof of his win
    When Tom records a 15-second video that is compressed on-device to 30MB
    And Tom uploads the video via presigned URL and confirms
    Then the video evidence is attached to the bet outcome
    And all participants can view the video evidence on the bet detail screen


  # ── Skip Evidence ────────────────────────────────────────────────────────────

  @E2E [EVI-06]
  Scenario: User skips evidence upload
    Given Maria Santos is marking a casual bet complete (who picks the restaurant)
    When Maria selects the winner without tapping "Add Evidence"
    Then the bet completion proceeds normally without evidence
    And the outcome claim is submitted with no evidence attached

  @E2E [EVI-07]
  Scenario: Bet without evidence proceeds through the full approval flow
    Given Tom Janssen submitted an outcome claim with no evidence on a jury bet
    When Alex Peeters opens the jury review screen
    Then Alex sees the bet details and claimed winner without an evidence section
    And Alex can still approve or reject the outcome


  # ── File Size Validation ─────────────────────────────────────────────────────

  @E2E [EVI-08]
  Scenario: Evidence exceeding 50MB after compression is rejected at presigned URL request
    Given Tom Janssen is requesting an upload URL for a 70MB compressed video
    When Tom submits the upload URL request with file size 73400320 bytes (70MB)
    Then the request is rejected with "File too large. Maximum 50MB after compression. Try a shorter clip."
    And no presigned URL is issued

  @E2E [EVI-09]
  Scenario: Evidence at exactly 50MB is accepted
    Given Tom Janssen is requesting an upload URL for a file that is exactly 50MB (52428800 bytes)
    When Tom submits the upload URL request
    Then the presigned URL is issued and the upload proceeds normally


  # ── Evidence Access ──────────────────────────────────────────────────────────

  @E2E [EVI-10]
  Scenario: Evidence is visible to jury in the jury review screen
    Given Tom Janssen uploaded a photo as evidence on a jury bet
    When Alex Peeters (jury) opens the jury review screen
    Then Alex can see the evidence photo

  @E2E [EVI-11]
  Scenario: Evidence is visible to all participants on bet detail
    Given Tom Janssen uploaded a photo as evidence during completion
    When Maria Santos views the bet detail screen
    Then Maria can see the evidence photo attached to the outcome claim

  @E2E [EVI-12]
  Scenario: Evidence cannot be attached after the bet is resolved
    Given "Sunday's cycling race" is already in RESOLVED state
    When Tom Janssen attempts to request an evidence upload URL for this bet
    Then the request is rejected with "Evidence can only be uploaded during bet completion"
```

---

## Feature: Reputation and Profile

**Epic:** US-801 (Own Profile), US-802 (Friend Profile)
**Driving port:** `GET /api/v1/profile/{userId}`, `GET /api/v1/profile/me`

```gherkin
Feature: Reputation and Profile
  User profiles show betting stats, streaks, and head-to-head records.
  Friend profiles include a head-to-head breakdown and a Challenge button.
  Stats are updated immediately after each bet resolution.

  Background:
    Given the following users exist and are friends:
      | username    | displayName    | wins | losses |
      | tomj        | Tom Janssen    | 13   | 8      |
      | mariasantos | Maria Santos   | 8    | 13     |
      | benm        | Ben Martens    | 0    | 0      |
    And Tom Janssen and Maria Santos have a head-to-head record of 4 wins for Tom and 2 wins for Maria


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-PROFILE-01]
  Scenario: User views own profile showing all stats, then views a friend's profile with head-to-head
    Given Tom Janssen opens his own profile
    Then Tom sees 13 wins, 8 losses, 62% win rate, current streak, and 21 total bets
    When Tom taps on Maria Santos in his friends list and views her profile
    Then Tom sees Maria's overall stats
    And the head-to-head record "You vs Maria: 4-2 (Tom leads)" is prominently displayed
    And a "Challenge Maria" button is visible


  # ── Own Profile ──────────────────────────────────────────────────────────────

  @E2E [REP-01]
  Scenario: Active user views their complete profile stats
    Given Tom Janssen has 13 wins, 8 losses, and a current 2-bet winning streak
    When Tom opens his own profile
    Then Tom sees:
      | Stat         | Value |
      | Wins         | 13    |
      | Losses       | 8     |
      | Win rate     | 62%   |
      | Total bets   | 21    |
      | Current streak | 2W  |

  @E2E [REP-02]
  Scenario: New user sees empty profile with guidance CTA
    Given Ben Martens just joined and has no betting history
    When Ben opens his profile
    Then Ben sees his avatar and name
    And an empty state message "Create your first bet to start building your reputation!"

  @E2E [REP-03]
  Scenario: Profile stats update immediately after bet resolution
    Given Tom Janssen's profile shows 13 wins and 8 losses (62%)
    When Tom loses a bet that transitions to RESOLVED
    Then Tom's profile immediately shows 13 wins and 9 losses (59%)
    And Tom's winning streak resets

  @E2E [REP-04]
  Scenario: Win streak displayed correctly on profile
    Given Tom Janssen has won 3 consecutive resolved bets
    When Tom opens his profile
    Then the streak section shows "3W" (3 consecutive wins)

  @E2E [REP-05]
  Scenario: Expired and cancelled bets do not count toward win/loss record
    Given Tom Janssen has 3 expired bets and 2 cancelled bets
    When Tom views his profile stats
    Then the expired and cancelled bets are excluded from his win/loss count
    And Tom's win rate is calculated only from RESOLVED bets


  # ── Friend Profile and Head-to-Head ──────────────────────────────────────────

  @E2E [REP-06]
  Scenario: User views friend's profile with head-to-head record
    Given Tom Janssen and Maria Santos have a head-to-head record of 4-2 (Tom leads)
    When Maria views Tom's profile
    Then Maria sees Tom's overall stats (13W-8L)
    And the head-to-head section shows "You vs Tom: 2-4" (from Maria's perspective)

  @E2E [REP-07]
  Scenario: Challenge button on friend profile opens Quick Bet with friend pre-selected
    Given Maria Santos is viewing Tom Janssen's profile
    When Maria taps "Challenge Tom"
    Then Quick Bet creation opens with Tom Janssen pre-selected as the friend

  @E2E [REP-08]
  Scenario: No head-to-head history shows encouragement message
    Given Maria Santos and Sarah De Vries have no mutual resolved bets
    When Maria views Sarah's profile
    Then the head-to-head section shows "You vs Sarah: No bets yet. Be the first to challenge!"
    And a Challenge button is prominently displayed

  @E2E [REP-09]
  Scenario: Friend's active bets are visible without showing stakes
    Given Tom Janssen has 3 active bets
    When Maria Santos views Tom's profile
    Then Maria sees the bet descriptions and participants for Tom's 3 active bets
    And the stakes for each bet are NOT shown (privacy)

  @E2E [REP-10]
  Scenario: Head-to-head record is symmetric between both users
    Given Tom leads Maria 4-2 in head-to-head
    When Tom views Maria's profile
    Then Tom sees "You vs Maria: 4-2" (4 wins for Tom)
    When Maria views Tom's profile
    Then Maria sees "You vs Tom: 2-4" (2 wins for Maria)


  # ── Edit Profile ─────────────────────────────────────────────────────────────

  @E2E [REP-11]
  Scenario: User updates profile photo and bio
    Given Maria Santos is on her profile screen
    When Maria taps "Edit", uploads a new profile photo, changes bio to "Runner, cyclist, and serial bet winner", and taps "Save"
    Then Maria's profile is updated with the new photo and bio
    And the updated profile is visible to all her friends

  @E2E [REP-12]
  Scenario: Username is read-only on the edit profile screen
    Given Maria Santos is on the edit profile screen
    When Maria views the edit form
    Then the username field "mariasantos" is visible but not editable
    And a note "Username cannot be changed" is displayed

  @E2E [REP-13]
  Scenario: Profile display name can be changed
    Given Tom Janssen's display name is "Tom"
    When Tom changes display name to "Tom Janssen" and saves
    Then Tom's display name is updated to "Tom Janssen" on his profile and in friends lists
```

---

## Feature: Win Cards and Sharing

**Epic:** US-901 (Win Card Generation)
**Driving port:** `GET /api/v1/bets/{betId}/win-card`

```gherkin
Feature: Win Cards and Sharing
  A Win Card is auto-generated after every bet resolution.
  It contains the bet summary, winner, loser(s), head-to-head record,
  optional evidence thumbnail, and iBetcha branding with a deep link.

  Background:
    Given Tom Janssen won the bet "Sunday's cycling race" against Maria Santos
    And the bet transitioned to RESOLVED with Tom as winner
    And the resolved bet id is "bet-sunday-ride"


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-WINCARD-01]
  Scenario: Win Card is generated on resolution and winner can share it
    Given "bet-sunday-ride" just transitioned to RESOLVED
    When Tom Janssen opens the resolution screen
    Then a Win Card has been automatically generated for "bet-sunday-ride"
    And the card shows bet description "Sunday's cycling race", winner Tom, loser Maria, stake, and head-to-head record
    When Tom taps "Share"
    Then the system share sheet opens allowing Tom to share the card to WhatsApp or other apps


  # ── Auto-Generation ───────────────────────────────────────────────────────────

  @E2E [WIN-01]
  Scenario: Win Card is auto-generated within 3 seconds of bet resolution
    Given "bet-sunday-ride" just transitioned to RESOLVED
    When Tom requests the Win Card for "bet-sunday-ride"
    Then the Win Card is available within 3 seconds of resolution
    And the card is a PNG image optimized for sharing

  @E2E [WIN-02]
  Scenario: Win Card contains all required fields
    Given Tom won "bet-sunday-ride" against Maria with stake "Loser buys coffee" and head-to-head 4-2
    When the Win Card is generated
    Then the card includes:
      | Field              | Content                           |
      | Bet description    | Sunday's cycling race             |
      | Winner             | Tom Janssen                       |
      | Loser              | Maria Santos                      |
      | Stake              | Loser buys coffee                 |
      | Head-to-head       | Tom leads Maria 4-2               |
      | iBetcha branding   | iBetcha logo                      |
      | Deep link URL      | https://ibetcha.app/win/bet-sunday-ride |

  @E2E [WIN-03]
  Scenario: Win Card includes evidence thumbnail when evidence was uploaded
    Given Tom Janssen uploaded a finish line photo as evidence for "bet-sunday-ride"
    When the Win Card is generated
    Then the card includes a thumbnail of Tom's evidence photo

  @E2E [WIN-04]
  Scenario: Win Card is generated for the winner but also viewable by the loser
    Given "bet-sunday-ride" is RESOLVED with Tom as winner
    When Maria Santos opens the resolution screen
    Then Maria can see the Win Card for the bet
    And the "Share" button is present but less prominent for Maria (she is the loser)


  # ── Sharing ──────────────────────────────────────────────────────────────────

  @E2E [WIN-05]
  Scenario: Winner shares Win Card via system share sheet
    Given Tom Janssen is viewing the Win Card for "bet-sunday-ride"
    When Tom taps "Share"
    Then the system share sheet opens with the Win Card PNG as the shared content

  @E2E [WIN-06]
  Scenario: Win Card deep link opens bet detail in app for recipients
    Given the Win Card deep link is "https://ibetcha.app/win/bet-sunday-ride"
    When a recipient taps the deep link on a device with iBetcha installed
    Then the app opens to the bet detail screen for "bet-sunday-ride"

  @E2E [WIN-07]
  Scenario: Win Card is formatted for both Instagram Story and WhatsApp preview
    Given the Win Card for "bet-sunday-ride" is generated
    When the card is retrieved
    Then the card is available in 1080x1920 resolution (Instagram Story ratio)
    And a 1200x630 crop is available for WhatsApp/Open Graph link previews
```

---

## Feature: Notifications

**Epic:** US-701 (Lifecycle Notifications)
**Driving port:** Push notification delivery (FCM/APNs), triggered by domain events

```gherkin
Feature: Notifications
  Push notifications are sent within 5 seconds for all bet lifecycle events.
  Bet creation and jury review notifications include actionable buttons.
  Notification taps deep-link to the relevant bet detail screen.

  Background:
    Given the following users have registered device tokens:
      | username    | displayName    | platform |
      | tomj        | Tom Janssen    | iOS      |
      | mariasantos | Maria Santos   | Android  |
      | alexpeeters | Alex Peeters   | iOS      |


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-NOTIF-01]
  Scenario: Bet created notification is delivered with Accept/Decline actions and tapping opens bet detail
    Given Tom Janssen creates a bet challenging Maria Santos
    When the bet is created
    Then Maria Santos receives a push notification "Tom challenged you!" within 5 seconds
    And the notification includes "Accept" and "Decline" action buttons
    When Maria taps the notification body (not a button)
    Then the app opens to the bet detail screen for the new bet


  # ── Bet Lifecycle Notifications ───────────────────────────────────────────────

  @E2E [NOT-01]
  Scenario: Participant receives notification when challenged with Accept/Decline actions
    Given Tom Janssen creates a bet challenging Maria Santos with stake "Loser buys coffee"
    When the bet is created
    Then Maria Santos receives a push notification containing "Tom challenged you!" and the stake
    And the notification includes actionable "Accept" and "Decline" buttons

  @E2E [NOT-02]
  Scenario: Creator receives notification when bet is accepted
    Given Maria Santos accepted Tom Janssen's bet
    When the acceptance is processed
    Then Tom receives "Maria Santos accepted your bet! Game on."

  @E2E [NOT-03]
  Scenario: Creator receives neutral notification when bet is declined
    Given Maria Santos declined Tom Janssen's bet
    When the decline is processed
    Then Tom receives "Maria Santos declined your bet" with neutral, non-shaming language

  @E2E [NOT-04]
  Scenario: All participants receive Bet is ON notification when last participant accepts
    Given Tom and Maria are the last two participants to accept Sarah's 3-person bet
    When both Tom and Maria accept (bet becomes ACTIVE)
    Then all participants (Sarah, Tom, Maria) receive "Bet is ON!" notification

  @E2E [NOT-05]
  Scenario: All participants receive notification when bet expires
    Given Tom Janssen's bet to Maria Santos expired after 48 hours
    When the expiry is processed
    Then both Tom and Maria receive a notification "Bet expired: no response within 48 hours"

  @E2E [NOT-06]
  Scenario: All participants receive notification when bet is cancelled by creator
    Given Sarah De Vries cancelled "Weekend 5K Challenge"
    When the cancellation is processed
    Then all invited participants (Tom, Maria, Ben) receive "Sarah De Vries cancelled the bet"

  @E2E [NOT-07]
  Scenario: Participants receive notification when outcome is claimed for approval
    Given Tom Janssen claimed victory in a 2-person no-jury bet with Maria
    When the outcome claim is submitted
    Then Maria receives "Tom Janssen claims he won. Do you agree?"

  @E2E [NOT-08]
  Scenario: All participants receive notification when bet is resolved
    Given Tom Janssen's cycling bet against Maria is RESOLVED with Tom as winner
    When the resolution is processed
    Then Tom receives a victory notification showing his updated record
    And Maria receives a factual outcome notification showing the result

  @E2E [NOT-09]
  Scenario: Jury receives actionable notification with Approve/Reject buttons
    Given Tom Janssen claimed victory in a bet with Alex Peeters as jury
    When the outcome claim triggers the jury flow
    Then Alex Peeters receives a notification "Tom claims he won. Review?" within 5 seconds
    And the notification includes actionable "Approve" and "Reject" buttons

  @E2E [NOT-10]
  Scenario: Actionable notification Accept button works without opening the app
    Given Maria Santos received a bet challenge notification with Accept and Decline buttons
    When Maria taps "Accept" on the notification
    Then the bet is accepted without Maria opening the app

  @E2E [NOT-11]
  Scenario: Rival milestone notification fires when head-to-head records tie
    Given Tom Janssen leads Maria Santos 4-3 in head-to-head record
    When Maria wins a bet against Tom bringing the record to 4-4
    Then both Tom and Maria receive "You and Maria/Tom are now tied at 4-4!"

  @E2E [NOT-12]
  Scenario: Notification tap deep-links to relevant bet detail screen
    Given Maria Santos received a bet challenge notification for "bet-sunday-ride"
    When Maria taps the notification body
    Then the app opens to the bet detail screen for "bet-sunday-ride"

  @E2E [NOT-13]
  Scenario: Users can receive notifications when app is in background
    Given Maria Santos has the iBetcha app in the background
    When Tom Janssen creates a bet challenging Maria
    Then Maria's device receives the push notification while the app is backgrounded

  @E2E [NOT-14]
  Scenario: 24-hour bet acceptance reminder is sent
    Given Tom Janssen sent a bet to Maria Santos and exactly 24 hours have elapsed with no response
    When the 24-hour reminder job fires
    Then Maria receives "Tom's bet is waiting for you! 24 hours left to respond"

  @E2E [NOT-15]
  Scenario: Jury receives day-3 reminder notification
    Given Alex Peeters has not responded to a jury review for 3 days
    When the day-3 reminder job fires
    Then Alex receives "Reminder: Tom vs Maria's bet is waiting for your verdict"

  @E2E [NOT-16]
  Scenario: Jury receives day-6 final warning notification
    Given Alex Peeters has not responded to a jury review for 6 days
    When the day-6 reminder job fires
    Then Alex receives "Last chance: 24 hours to review before the bet escalates to participant vote"

  @E2E [NOT-17]
  Scenario: Notification delivery target is within 5 seconds
    Given a bet lifecycle event occurs (e.g., bet created)
    When the event is processed and the notification is dispatched
    Then the push notification is delivered to the recipient device within 5 seconds
```

---

## Feature: Account Management

**Epic:** US-1001 (Block), US-1002 (Delete Account), US-1003 (Privacy and Terms)

```gherkin
Feature: Account Management
  Users can block other users, delete their accounts (30-day grace period),
  and access Privacy Policy and Terms of Service.

  Background:
    Given the following users exist:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | benm        | Ben Martens    |


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-ACCT-01]
  Scenario: User requests account deletion, is warned of active bets, and is logged out
    Given Ben Martens has 2 active bets
    When Ben navigates to Settings and taps "Delete Account"
    Then Ben sees "You have 2 active bets that will be cancelled"
    When Ben re-authenticates and confirms deletion
    Then Ben's account enters a 30-day deletion queue (DELETION_PENDING status)
    And Ben is logged out immediately


  # ── Block a User ─────────────────────────────────────────────────────────────

  @E2E [ACCT-01]
  Scenario: Blocking a user removes friendship, cancels pending bets, and hides both users from each other
    Given Maria Santos and "unwanted_user" are friends
    And Maria has a bet in PENDING_ACCEPTANCE with "unwanted_user"
    When Maria taps "Block User" on "unwanted_user"'s profile and confirms
    Then "unwanted_user" is removed from Maria's friends list
    And the PENDING_ACCEPTANCE bet between them is CANCELLED
    And neither user can find the other via search

  @E2E [ACCT-02]
  Scenario: Blocked user is not notified of the block
    Given Maria Santos is about to block "unwanted_user"
    When Maria confirms the block
    Then "unwanted_user" does NOT receive any push notification about being blocked

  @E2E [ACCT-03]
  Scenario: Blocker cannot be found by blocked user in search
    Given Maria Santos blocked "unwanted_user"
    When "unwanted_user" searches for "mariasantos"
    Then the search returns no results for Maria

  @E2E [ACCT-04]
  Scenario: Block option requires a confirmation dialog
    Given Maria Santos is on "unwanted_user"'s profile
    When Maria taps "Block User"
    Then a confirmation dialog appears before the block takes effect


  # ── Account Deletion ──────────────────────────────────────────────────────────

  @E2E [ACCT-05]
  Scenario: User initiates account deletion with re-authentication and is logged out
    Given Ben Martens is in the Settings screen
    When Ben taps "Delete Account", re-authenticates successfully, and confirms
    Then Ben's account transitions to DELETION_PENDING status with a 30-day grace window
    And Ben is logged out immediately after confirming

  @E2E [ACCT-06]
  Scenario: Deletion warning shows count of active bets
    Given Ben Martens has 2 bets in ACTIVE state
    When Ben initiates account deletion
    Then Ben sees a warning "You have 2 active bets that will be cancelled" before the confirmation step

  @E2E [ACCT-07]
  Scenario: User cancels deletion by logging back in within the grace period
    Given Ben Martens initiated account deletion 10 days ago (DELETION_PENDING)
    When Ben logs back in within the 30-day grace period
    Then Ben's account is reactivated (status ACTIVE)
    And the pending deletion is cancelled
    And Ben sees his home screen as usual

  @E2E [ACCT-08]
  Scenario: Account and data are permanently deleted after 30-day grace period
    Given Ben Martens initiated account deletion 30 days ago and did not log back in
    When the system scheduler runs the account purge job
    Then all of Ben's data is permanently deleted: profile, bets, evidence, stats, friend connections
    And Ben's username "benm" becomes available for registration by others

  @E2E [ACCT-09]
  Scenario: Deleted user appears as "[Deleted User]" in others' bet histories
    Given Ben Martens had a RESOLVED bet with Tom Janssen before deleting his account
    When Tom views his bet history after Ben's account is deleted
    Then the bet shows "[Deleted User]" in place of Ben's name

  @E2E [ACCT-10]
  Scenario: Account deletion re-authentication is required
    Given Ben Martens is on the Delete Account screen
    When Ben proceeds without re-authenticating
    Then the deletion cannot be confirmed until re-authentication completes


  # ── Privacy Policy and Terms of Service ──────────────────────────────────────

  @E2E [ACCT-11]
  Scenario: Terms of Service must be accepted during registration
    Given Alex Peeters is on the email registration screen
    When Alex submits the registration form without checking the ToS acceptance checkbox
    Then registration is blocked with "Please accept the Privacy Policy and Terms of Service"

  @E2E [ACCT-12]
  Scenario: Google OAuth registration requires ToS acceptance
    Given Maria Santos is on the Google OAuth registration flow
    When the server receives a registration request without acceptedTerms: true
    Then registration is rejected with 422 and "Terms of Service must be accepted"

  @E2E [ACCT-13]
  Scenario: Privacy Policy and Terms of Service are accessible from Settings
    Given Tom Janssen is a registered user in the Settings screen
    When Tom navigates to Privacy Policy and then Terms of Service
    Then both documents are accessible and readable without leaving the app
```

---

## Feature: Home Screen

**Epic:** US-1201 (Home Screen)
**Driving port:** `GET /api/v1/bets?filter=active`, `GET /api/v1/bets?filter=pending`

```gherkin
Feature: Home Screen
  The home screen is the central hub. It surfaces pending actions (bets to accept,
  outcomes to approve) and active bets sorted by most recent activity.
  New users see an empty state with guidance.

  Background:
    Given the following users exist:
      | username    | displayName    |
      | tomj        | Tom Janssen    |
      | mariasantos | Maria Santos   |
      | benm        | Ben Martens    |


  # ── Walking Skeleton ─────────────────────────────────────────────────────────

  @walking_skeleton @E2E [WS-HOME-01]
  Scenario: Active user opens the app and sees pending actions and active bets
    Given Tom Janssen has 1 bet to accept, 1 outcome to approve, and 3 ACTIVE bets
    When Tom opens the home screen
    Then the top of the screen shows 2 pending actions (bet to accept and outcome to approve)
    And below the pending actions Tom sees his 3 ACTIVE bets sorted by most recent activity
    And the create bet button (FAB) is visible


  # ── Pending Actions ───────────────────────────────────────────────────────────

  @E2E [HOME-01]
  Scenario: Pending actions are highlighted at the top of the home screen
    Given Tom Janssen has a bet in PENDING_ACCEPTANCE to respond to and an outcome in PENDING_APPROVAL to vote on
    When Tom opens the home screen
    Then both pending actions are shown at the top of the screen with clear call-to-action labels

  @E2E [HOME-02]
  Scenario: Home screen with no pending actions shows only active bets
    Given Maria Santos has no pending actions and 2 ACTIVE bets
    When Maria opens the home screen
    Then Maria sees her 2 ACTIVE bets without a pending actions section at the top


  # ── Active Bets List ──────────────────────────────────────────────────────────

  @E2E [HOME-03]
  Scenario: Active bets are sorted by most recent activity
    Given Tom Janssen has ACTIVE bet A (last activity 1 hour ago) and ACTIVE bet B (last activity 3 days ago)
    When Tom opens the home screen
    Then ACTIVE bet A appears above ACTIVE bet B in the list

  @E2E [HOME-04]
  Scenario: Each bet row shows description, participants, status, and head-to-head snippet
    Given Tom Janssen has an ACTIVE bet "Sunday's cycling race" with Maria Santos
    And Tom leads Maria 4-2 in head-to-head
    When Tom views the home screen bet list
    Then the bet row shows:
      | Field            | Content                      |
      | Description      | Sunday's cycling race        |
      | Participants     | You vs Maria Santos          |
      | Status           | Active                       |
      | Head-to-head     | 4-2 (Tom leads)              |


  # ── Empty States ─────────────────────────────────────────────────────────────

  @E2E [HOME-05]
  Scenario: New user with no bets sees empty state with guidance CTAs
    Given Ben Martens has no bets and no friends
    When Ben opens the home screen
    Then Ben sees an empty state with the message "Add friends and create your first bet!"
    And there are clear call-to-action buttons for friend search and bet creation

  @E2E [HOME-06]
  Scenario: New user arriving via deep link sees friend suggestion in empty state
    Given Ben Martens registered via Tom's invite link and Tom is his only friend
    When Ben opens the home screen with 0 bets
    Then Ben sees a CTA "Challenge Tom to your first bet!"


  # ── FAB and Navigation ────────────────────────────────────────────────────────

  @E2E [HOME-07]
  Scenario: Create bet FAB is always visible on the home screen
    Given Maria Santos is on the home screen
    When Maria scrolls through the bet list
    Then the create bet button (FAB) remains visible at all times

  @E2E [HOME-08]
  Scenario: Pull-to-refresh updates the bet list
    Given Tom Janssen is on the home screen
    When Tom pulls down to refresh
    Then the bet list is updated with the latest data from the server
```

---

## Appendix A: Domain State Reference

All tests use the following canonical DDD state names for the `Bet` aggregate:

| State | Description |
|-------|-------------|
| `PENDING_ACCEPTANCE` | Bet created; waiting for all invitees to accept or decline |
| `ACTIVE` | All remaining participants accepted; bet is live |
| `PENDING_JURY_VERDICT` | Outcome claimed; waiting for designated jury to approve or reject |
| `PENDING_APPROVAL` | Outcome claimed; waiting for participant majority vote (no jury, or jury timed out) |
| `RESOLVED` | Outcome approved; winner and loser determined. Final state. |
| `DISPUTED` | 2-person no-jury bet with conflicting outcome claims. Deadlock. |
| `EXPIRED` | 48h timeout elapsed before full acceptance, or declines reduced participants below 2. Terminal. |
| `CANCELLED` | Creator cancelled before full acceptance. Terminal. |

---

## Appendix B: Persona Reference

| Name | Username | Role in Stories |
|------|----------|----------------|
| Maria Santos | mariasantos | New user via deep link, frequent bet participant |
| Tom Janssen | tomj | Competitive bet creator, frequent challenger |
| Sarah De Vries | sarahdev | Organizer of structured group bets |
| Alex Peeters | alexpeeters | Jury member, cautious participant |
| Ben Martens | benm | New user invited via WhatsApp link |

---

## Appendix C: Test Coverage Summary

| Feature | Total Scenarios | Walking Skeleton | Error / Edge Cases | Error Ratio |
|---------|-----------------|------------------|--------------------|-------------|
| Auth and Onboarding | 22 | 1 | 11 | 50% |
| Friends and Social | 22 | 1 | 10 | 45% |
| Bet Creation | 14 | 1 | 8 | 57% |
| Bet Acceptance and Decline | 10 | 1 | 5 | 50% |
| Bet Expiry and Cancellation | 11 | 0 | 8 | 73% |
| Bet Completion and Outcome | 19 | 1 | 8 | 42% |
| Jury System | 11 | 1 | 6 | 55% |
| Dispute Resolution | 10 | 1 | 6 | 60% |
| Evidence | 12 | 1 | 5 | 42% |
| Reputation and Profile | 13 | 1 | 4 | 31% |
| Win Cards and Sharing | 7 | 1 | 2 | 29% |
| Notifications | 17 | 1 | 5 | 29% |
| Account Management | 13 | 1 | 7 | 54% |
| Home Screen | 8 | 1 | 3 | 38% |
| **TOTAL** | **189** | **13** | **88** | **47%** |

Overall error/edge path ratio: **47%** (target: 40% minimum — satisfied).

---

## Appendix D: Implementation Sequence (One at a Time)

The following order is recommended for the DELIVER wave. Each walking skeleton scenario is the first to implement within its feature. All other scenarios in each feature are initially marked `@ignore`/`@skip` and enabled one at a time.

**Phase 1 — Walking Skeletons (prove the stack connects end-to-end):**
1. `[WS-AUTH-01]` New user registers via Google OAuth and completes profile setup
2. `[WS-SOCIAL-01]` User searches for friend, sends request, friend accepts
3. `[WS-BET-01]` Creator creates a quick bet and invitee receives notification
4. `[WS-ACCEPT-01]` Participant accepts via push notification, bet becomes ACTIVE
5. `[WS-COMPLETE-01]` Tom declares victory, Maria approves, bet resolves
6. `[WS-JURY-01]` Jury receives notification, reviews, approves, bet resolves
7. `[WS-DISPUTE-01]` Both claim victory, enters DISPUTED, one concedes
8. `[WS-EVIDENCE-01]` Photo uploaded as evidence, visible to participants
9. `[WS-PROFILE-01]` User views own profile, then friend profile with head-to-head
10. `[WS-WINCARD-01]` Win Card auto-generated and winner shares it
11. `[WS-NOTIF-01]` Bet created notification delivered with action buttons
12. `[WS-ACCT-01]` User requests deletion, warned of active bets, logged out
13. `[WS-HOME-01]` Active user sees pending actions and active bets on home screen

**Phase 2 — Happy paths per feature (enable in feature order)**

**Phase 3 — Error paths and edge cases (enable by error ratio priority)**
