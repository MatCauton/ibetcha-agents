# iBetcha

Social betting and bragging-rights platform. Challenge friends to informal bets, settle outcomes with a jury or group vote, and collect Win Cards when you win.

## Project structure

```
iBetcha-agents/
├── backend/        Java 25 + Spring Boot 3.5 REST API
└── mobile/         React Native + Expo mobile app (iOS & Android)
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 10+ |
| Expo CLI | bundled via `npx` |
| PostgreSQL | 16+ |
| Android Studio / Xcode | for running the emulator/simulator |

---

## Backend

### 1. Create the database

```sql
CREATE USER ibetcha WITH PASSWORD 'ibetcha_dev';
CREATE DATABASE ibetcha OWNER ibetcha;
```

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The server starts on **http://localhost:8080**. Liquibase migrations run automatically on startup and create all tables.

### 3. Verify it's running

```
GET http://localhost:8080/actuator/health
```

Expected response: `{ "status": "UP" }`

### 4. API docs (Swagger UI)

```
http://localhost:8080/swagger-ui/index.html
```

All endpoints require a `Bearer` token except `/api/v1/auth/**`.

### Configuration

The defaults in `application.yml` work out of the box for local development. Override any value with environment variables:

| Property | Env var | Default | Notes |
|----------|---------|---------|-------|
| DB URL | — | `localhost:5432/ibetcha` | Change in `application.yml` |
| DB user | — | `ibetcha` | |
| DB password | — | `ibetcha_dev` | |
| S3 bucket | `IBETCHA_S3_EVIDENCE_BUCKET` | `ibetcha-evidence-dev` | Evidence upload feature |
| AWS region | — | `eu-west-1` | Set in `application.yml` |

**JWT key pair**: an ES256 key pair is generated fresh on every startup (MVP behaviour). All existing tokens are invalidated when the backend restarts. Before production, load a stable key pair from AWS Secrets Manager.

### Running tests

```bash
cd backend
mvn test
```

Tests use Testcontainers — Docker must be running. A real PostgreSQL container is started automatically; no manual setup needed.

---

## Mobile app

### 1. Install dependencies

```bash
cd mobile
npm install
```

### 2. Configure OAuth (optional for local dev)

OAuth sign-in (Google / Apple) requires client credentials. For local testing you can skip this and use email/password registration instead.

To enable OAuth, edit `mobile/app.json`:

```json
{
  "expo": {
    "plugins": [
      "expo-router",
      "expo-secure-store",
      "@react-native-google-signin/google-signin",
      "expo-apple-authentication"
    ],
    "extra": {
      "googleWebClientId": "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    },
    "ios": {
      "usesAppleSignIn": true
    }
  }
}
```

Get `googleWebClientId` from [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials → OAuth 2.0 Web Client ID.

Apple Sign-In only works on a real iOS device or Xcode simulator with a paid Apple Developer account.

### 3. Start the app

```bash
cd mobile
npx expo start
```

Then press:
- `a` — open Android emulator
- `i` — open iOS simulator
- Scan the QR code with Expo Go on a physical device

### Backend connection

The app connects to the backend automatically:

| Platform | Backend URL |
|----------|-------------|
| Android emulator | `http://10.0.2.2:8080/api/v1` |
| iOS simulator / physical device | `http://localhost:8080/api/v1` |

Make sure the backend is running before opening the app.

---

## Using the app

### 1. Create an account

Open the app → tap **Register** → enter email, username, display name, and password.

Alternatively, tap **Sign in with Google** or **Sign in with Apple** (requires OAuth setup above).

### 2. Add friends

Go to the **Friends** tab → search for a username → send a friend request. The other user accepts it from their Friends tab.

### 3. Create a bet

Tap the **+** button on the home screen. Fill in:

- **Description** — what the bet is about (required)
- **Stake** — what the loser owes (required, informal — e.g. "buys coffee")
- **Participants** — select from your friends (at least one)

Tap **More options** to set:
- **Title** — short label
- **Deadline** — date the bet must be completed by
- **Jury** — a trusted friend who resolves disputes (not a participant)
- **Evidence required** — toggle if proof must be uploaded

The bet is created in **PENDING_ACCEPTANCE** state. All participants must accept before it becomes active.

### 4. Accept or decline a bet

Open the bet from your home screen → tap **Accept** or **Decline**. Once all participants accept, the bet moves to **ACTIVE**.

### 5. Complete a bet

When the outcome is known, any participant opens the bet and taps **Mark Complete**:

- Select the winner from the participant list
- If you select yourself as the winner, participants vote to approve (**PENDING_APPROVAL**)
- If you select someone else and concede, the bet resolves immediately (**RESOLVED**)
- If a jury was assigned, the jury member gets a notification to approve or reject the outcome (**PENDING_JURY_VERDICT**)

### 6. Approve or dispute an outcome

When a bet is in **PENDING_APPROVAL**, each participant can:
- **Approve** — vote in favour of the declared winner
- **Dispute** — reject the outcome (sends to jury if one was assigned, otherwise moves to DISPUTED)

Majority approval resolves the bet. In a 2-person bet, if both parties disagree, the bet moves to **DISPUTED**.

### 7. Jury verdict

If you are the jury on a bet in **PENDING_JURY_VERDICT**:
- Open the bet — a blue banner indicates your jury role
- Tap **Approve** → select the winner → confirm
- Tap **Reject** → the bet returns to ACTIVE and participants can re-declare

The jury has 7 days to submit a verdict. After that, the bet automatically escalates to participant majority vote.

### 8. View a Win Card

After a bet resolves, tap **View Win Card** to see the bragging-rights card. It shows the winner, loser, stake, and your all-time head-to-head record. Tap **Share** to send it to friends.

### 9. Upload evidence

While a bet is ACTIVE or PENDING_APPROVAL, tap **Add Evidence** to attach a photo or video. The file uploads directly to S3 — the backend only receives metadata after the upload completes. Evidence is visible to all participants.

---

## Bet lifecycle

```
PENDING_ACCEPTANCE  →  ACTIVE  →  PENDING_APPROVAL  →  RESOLVED
                    ↓           ↓                    ↑
                 EXPIRED    PENDING_JURY_VERDICT  ────┘
                 CANCELLED       ↓
                             DISPUTED
```

| State | Meaning |
|-------|---------|
| PENDING_ACCEPTANCE | Waiting for all participants to accept (48h timeout) |
| ACTIVE | All accepted — bet is in progress |
| PENDING_APPROVAL | Outcome declared — participants are voting |
| PENDING_JURY_VERDICT | Jury is reviewing the outcome (7d timeout) |
| RESOLVED | Winner confirmed |
| DISPUTED | 2-person deadlock — manual resolution required |
| CANCELLED | Creator cancelled before anyone accepted |
| EXPIRED | Acceptance deadline passed |

---

## Development notes

- **Modular monolith** — bounded contexts are enforced by ArchUnit; no cross-context direct field access
- **Authentication** — JWT (ES256), 15-minute access tokens, 30-day refresh tokens with family-based replay protection
- **Passwords** — BCrypt strength 12
- **Evidence storage** — client uploads directly to S3 with presigned PUT URLs; backend stores only the S3 key
- **Push notifications** — FCM device tokens are registered but actual sending is stubbed (logs only) — wired in a later walking skeleton
- **Scheduled jobs** — run every 5 minutes (acceptance timeout) and 10 minutes (jury timeout); no external scheduler needed
- **API versioning** — all endpoints are under `/api/v1`
