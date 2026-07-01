# Play Integrity Plan - SecureChat

Date: 2026-07-01
Status: Planning complete, implementation not started
Package scope: `securechat.app` public Play app. Test-only tier packages (`securechat.app.free`, `.pro`, `.elite`) are for local/internal validation and should not drive public Play policy.

## Goal

Use Play Integrity as a backend risk signal for account, access, and abuse-sensitive operations. Do not block app startup, local message viewing, or emergency/broadcast UX from a missing or weak verdict in the first release.

## Source Baseline

- Google Play Integrity API overview: verifies that server requests come from the genuine app on a genuine, certified Android device.
- Standard requests: suitable for app-to-server interactions and supported on Android 5.0+.
- Classic requests: useful for high-value actions that need explicit nonce binding and replay/tamper protection.
- Optional verdicts: app access risk and Play Protect can be enabled in Play Console after linking a Cloud project.
- Current Android dependency from Google docs: `com.google.android.play:integrity:1.6.0`.
- Official references checked on 2026-07-01:
  - https://developer.android.com/google/play/integrity/overview
  - https://developer.android.com/google/play/integrity/setup
  - https://developer.android.com/google/play/integrity/standard
  - https://developer.android.com/google/play/integrity/classic
  - https://developer.android.com/google/play/integrity/verdicts

## Phase 1 - Signal Only

Protected actions:
- Activation-code redeem.
- Access-tier refresh.
- Relay registration or high-rate reconnect patterns.
- Future broadcast-management actions that affect other users.

Client behavior:
- Request an integrity token only for protected server calls.
- Do not request on every app launch.
- If Play services, network, or token generation fails, continue the action where product-safe and mark the backend event as `integrityUnavailable=true`.
- Do not store raw verdicts or raw tokens locally.

Backend behavior:
- Generate a short-lived nonce or request hash for the protected action.
- Decode/verify the token server-side through Google Play.
- Store summarized verdict fields with timestamp, package name, action name, and coarse risk.
- Never log raw tokens.
- Treat weak or missing verdicts as telemetry until a separate enforcement decision is made.

## Phase 2 - Policy Gates

Candidate soft gates:
- Flag non-recognized app verdicts for activation-code redeem.
- Flag devices below expected integrity during repeated access-tier refresh attempts.
- Use recent device activity to detect token/request abuse.
- Use app access risk as a warning/step-up signal for admin or broadcast features only.

Hard denial should require separate Gio sign-off and observed telemetry. Avoid using Play Integrity to block read-only message access or local encrypted data operations.

## Backend Contract Draft

`POST /integrity/challenge`

Request:
```json
{
  "action": "activation_redeem",
  "clientId": "securechat-device-id",
  "packageName": "securechat.app"
}
```

Response:
```json
{
  "challengeId": "uuid",
  "nonce": "base64url-16-to-500-chars",
  "expiresAt": "ISO-8601"
}
```

`POST /integrity/verify`

Request:
```json
{
  "challengeId": "uuid",
  "integrityToken": "signed-token",
  "actionPayloadHash": "sha256-base64url"
}
```

Response:
```json
{
  "decision": "allow",
  "risk": "low",
  "signals": {
    "app": "recognized",
    "device": "meets_device_integrity",
    "playProtect": "not_evaluated",
    "appAccessRisk": "not_evaluated"
  }
}
```

## Implementation Tasks

- Link the SecureChat Play app to a Google Cloud project in Play Console.
- Enable optional Play Protect and App Access Risk verdicts only after deciding whether the added latency is acceptable.
- Add the Play Integrity dependency to `app/build.gradle.kts`.
- Add a small client wrapper for challenge, token request, and verify calls.
- Add backend challenge storage with single-use nonce expiry.
- Add backend token verification and summarized telemetry.
- Add tests for nonce replay, wrong package, expired challenge, missing token, and Google API failure.
- Monitor results before any enforcement.

## Rollback

Keep all Phase 1 behavior behind backend config:
- `PLAY_INTEGRITY_ENABLED=false` disables challenge generation and verification.
- `PLAY_INTEGRITY_ENFORCEMENT=log` is the initial and default mode.
- `PLAY_INTEGRITY_ENFORCEMENT=deny` must not be used until telemetry is reviewed.
