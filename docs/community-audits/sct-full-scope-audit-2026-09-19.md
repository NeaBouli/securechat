# SecureChat — Full-Scope Security & Crypto Audit

- **Series:** Collateral Web3 Open Audits (sixth engagement: StealthX clients)
- **Date:** 2026-09-19 (recon 2026-09-15, resumed and verified 2026-09-19)
- **Target:** NeaBouli/securechat @ `9cebb36843e7b3d31baccd966ecb09bffa64dfa2`
- **Scope:** full Kotlin tree (app, data, domain, features, presentation, security, stealthx-crypto, stealthx-access, transport, backend), build/CI, crypto implementation
- **Method:** deep-recon agent + lead verification of every finding at exact file:line; read-only; no secrets read (filename scans only); no builds
- **Prior baseline:** BRIDGE.md 2026-07-12 internal audit — re-checked below
- **Register:** SCT-01 … SCT-14 (this report) — **0 Critical / 3 High / 5 Medium / 6 Low**

---

## Executive summary

SecureChat's hygiene layer is genuinely good: strict entitlement verification (Ed25519 + device-bound claims + fail-closed HMAC tier cache), SQLCipher with Keystore-wrapped passphrase, no backup/cleartext, no FCM, VISIBILITY_SECRET notifications, no WebView, IFR/paid surfaces hard-gated with a real test. **But the core messaging protocol contradicts its own marketing in the same way SecureCall did:** the shipped "ratchet" is a symmetric chain over a *static* per-contact DH key with no post-compromise security, the genuine DoubleRatchet implementation is dead code, and "Signal-grade… break-in recovery" is claimed on the homepage, README, llms.txt, FAQ and wiki. On top: contact import does not bind sxId to the Ed25519 key (impersonation), and relay identification is unauthenticated (STX-01 class).

## Severity table

| ID | Severity | Title |
|----|----------|-------|
| SCT-01 | High | Shipped message protocol is not a Double Ratchet; "break-in recovery" claims false for the live path |
| SCT-02 | High | sxId↔Ed25519 identity binding not enforced on contact import (chameleon fix never backported) |
| SCT-03 | High | Unauthenticated relay IDENTIFY (STX-01 analog at client level) + fail-open signature middleware reference |
| SCT-04 | Medium | Ratchet out-of-order handling broken in both implementations → silent message loss |
| SCT-05 | Medium | `paddedLength` leaks exact plaintext size in cleartext and is not AEAD-covered |
| SCT-06 | Medium | `storeScreenshot` build type inherits debug tier override + exported debug receivers |
| SCT-07 | Medium | At-rest message encryption key derived from public data only |
| SCT-08 | Medium | App lock is UI-only; silent unlock when no device credential enrolled; keys not auth-bound |
| SCT-09 | Low | `computeSharedSecret` ignores low-order check result |
| SCT-10 | Low | Password copied into unwipeable JVM String in `deriveKey` |
| SCT-11 | Low | `ECGenParameterSpec("ED25519")` — invalid Keystore spec, dead-code trap |
| SCT-12 | Low | `HardwareAttestationVerifier` claims root-pinning; verifies neither chain nor challenge |
| SCT-13 | Low | Fixed 30s reconnect loop; `wipeAll` uses plain `File.delete`; MESSAGE_ACK/READ_RECEIPT not message-id-bound |
| SCT-14 | Low | Doc drift: "minified + shrunk" false (no R8); removed `stealthx-ifr/` module still listed; dead domain scaffolding |

---

## SCT-01 — High — Shipped protocol is not a Double Ratchet; public claims false

**Evidence (lead-verified):** production messaging runs through `ChatSessionRepository` (`data/.../repository/ChatSessionRepository.kt`): `sendDhPrivate` is generated **once** at session creation (`:109`) and never rotated; `prevCounter` is hardcoded `0` (`:61`); no DH ratchet step on send; receive chain = `X25519(ownStaticIdentityKey, senderDhPublic)` (`:146`); root keys are HKDF-derived from **public** inputs (`:110-116`, `:165-174`: `contact.identityKey + contact.dhPublicKey + contact.id`). The genuine Double Ratchet at `stealthx-crypto/.../DoubleRatchet.kt` is **dead code** (grep: only itself and its own test reference it). False "Double Ratchet / break-in recovery / Signal-grade" claims at `README.md:15,30`, `llms.txt:4,10`, `index.html:617-618,711,738`, `faq.html:419`, `wiki/crypto-protocol.html:123-135` ("X25519 key pair rotated on each reply"), `wiki/security-design.html:40`.

**Impact:** no post-compromise security — leaking current DB state (chain key + static send DH private, both in `chat_sessions`, `ChatSessionEntity.kt:30-45`) compromises all *future* messages in that direction forever; no self-healing. Same defect class as SecureCall STX-22 (false ratchet claim) — the platform repeats the pattern on its second product.

**Recommendation:** wire the real `DoubleRatchet.kt` into `ChatSessionRepository` (after fixing SCT-04) or correct all public claims to "symmetric chain, per-contact forward secrecy only, no break-in recovery".

## SCT-02 — High — sxId↔Ed25519 binding not enforced on contact import

**Evidence (lead-verified):** `ContactRepository.validateBundle` (`data/.../repository/ContactRepository.kt:82-100`) verifies the self-signature (`:97`) and sxId *format* (`:83`) but never checks `sxId == deriveShortId(ed25519PublicKey)` (`StealthXIdentity.kt:219-226`). An attacker can mint a self-signed bundle carrying a victim's sxId → impersonation via QR/NFC/CONTACT_EXCHANGE; victim clients auto-save relayed bundles (`ContactExchangeManager.kt:234-248`). **Chameleon received exactly this fix** on 2026-08-02 (`chameleon@0eb51e0`, `IdentityState.kt:90`) — the SecureChat backport is missing.

**Recommendation:** enforce `deriveShortId(ed25519)` binding in `validateBundle` and `PublicKeyBundleQr.fromQrContent`; reject mismatches.

## SCT-03 — High — Unauthenticated relay IDENTIFY

**Evidence (lead-verified):** `ContactExchangeManager.kt:185-189` sends `{type:"IDENTIFY", sxId}` with no key proof; the Ed25519 identity key never authenticates to the relay; `ActivationCodeClient.kt:91-95` (REGISTER) likewise. Any relay passenger can claim any sxId → receive its messages (E2E content protected, but delivery theft/DoS + metadata) and push CONTACT_EXCHANGE bundles the client auto-saves (SCT-02 makes this worse). `docs/TRANSPORT_LAYER.md` + `SignalingRelayTransport.kt:7-8` call the relay "authenticated" — overclaim. `backend/middleware/signatureVerifier.js` fails open when `ALLOWED_SIGNATURES` is unset (`:12-14`), and the app never sends an `x-app-signature` header (grep: zero Kotlin hits) — reference copy, not wired. Server-side enforcement lives in the SecureCall backend (STX-01) — same live service (`wss://api.stealthx.tech/signal`).

**Recommendation:** sign a server challenge with the Ed25519 identity key on IDENTIFY/REGISTER; fix the server side per STX-01.

## SCT-04 — Medium — Ratchet out-of-order handling broken in both implementations

**Evidence:** production `ChatSessionRepository.decryptIncoming` (`:82-88`) **wipes** skipped message keys while advancing; a delayed/reordered message is later rejected by `require(message.counter >= counter)` (`:77`) — relay reorder = permanent loss. The dead-code `DoubleRatchet.kt` keys its skipped-key map by `Pair<ByteArray, Int>` (`:42`) — `ByteArray` uses referential equality, so lookups (`:163`) never hit for deserialized messages; and `decrypt` commits chain state *before* AEAD verification (`:182-190`) — a forged frame burns the chain position. No out-of-order test exists in `DoubleRatchetTest.kt`.

**Recommendation:** store skipped keys keyed by hex/hash; commit chain state only after successful authentication; add out-of-order tests before adopting the class.

## SCT-05 — Medium — `paddedLength` leaks exact plaintext size, unauthenticated

**Evidence:** `ChameleonCrypto.encrypt` pads to 256B blocks (`:297-300`) to defeat size analysis, but `EncryptedPayload.paddedLength` = exact original size (`Models.kt:20`, set at `ChameleonCrypto.kt:84`) is transmitted **in cleartext** in the wire envelope (`RatchetMessageQr.kt:24` `pl=`) — the relay learns the exact message length; padding is theater against the primary adversary. The field is also *not* AEAD-covered: tampering truncates/extends decrypted output without auth failure (`ChameleonCrypto.kt:305-307`).

**Recommendation:** drop the field from the wire; encode length inside the padded plaintext (4-byte prefix).

## SCT-06 — Medium — `storeScreenshot` build type inherits tier override + exported debug receivers

**Evidence:** `app/build.gradle.kts:47-60` — `storeScreenshot` `initWith(debug)` keeps `ALLOW_TIER_OVERRIDE=true` (honored at `SecureChatApp.kt:36-41`) and pulls in `src/debug` sources + manifest (`:112-115`), shipping exported `SetTierReceiver` (ADB broadcast → ELITE, `SetTierReceiver.kt:34-52`) and `TestHookReceiver` (dumps sxId/QR/contacts, sends messages, `TestHookReceiver.kt:25-58`). The `verifyNoReleaseTierOverrides` gate (`:121-154`) pattern-matches only the `debug {}` block, not inheritance. Risk materializes if this APK is signed with the release key and distributed.

**Recommendation:** explicitly set `ALLOW_TIER_OVERRIDE=false` in `storeScreenshot`, exclude debug receivers from it, extend the verify task to inherited blocks.

## SCT-07 — Medium — At-rest message encryption key derived from public data only

**Evidence:** `MessageRepository.localMessageKey` (`:314-321`): `HKDF(contact.identityKey + contact.dhPublicKey + contactId)` — all public bundle material; anyone holding the contact's public bundle derives the "encryption" key. Adds zero confidentiality beyond SQLCipher (which is properly Keystore-keyed, `DataModule.kt:34-50`). Marked Medium-not-High only because SQLCipher covers at-rest.

**Recommendation:** mix in a device secret (Keystore-wrapped random) or drop the layer and document SQLCipher as the at-rest control.

## SCT-08 — Medium — App lock is UI-only

**Evidence:** `MainActivity.kt:207-210` — if no biometric/credential is enrolled, the app unlocks directly; BiometricPrompt is used without `CryptoObject` (`:213-238`); DB wrap key and HMAC key are `requireAuth=false` (`KeystoreManager.kt:100,87`). Any local actor driving the UI layer bypasses the lock screen; data keys are not auth-bound.

**Recommendation:** document as UX gate, or bind a keystore key via CryptoObject; consider fail-closed instead of silent unlock.

## SCT-09 — Low — `computeSharedSecret` ignores low-order check

`ChameleonCrypto.kt:202-206` discards `cryptoScalarMult`'s boolean (false on low-order/identity output) → a malicious contact could force an all-zero shared secret. Check the result or use `cryptoKx*`. (Chameleon has the same defect, CHA-01.)

## SCT-10 — Low — Password copied into unwipeable JVM String

`ChameleonCrypto.kt:158` — `deriveKey` takes the password as `String`; wiping covers only the byte[]/char[] copies.

## SCT-11 — Low — Invalid Keystore spec trap

`KeystoreManager.getOrCreateSigningKeyPair` (`:64-74`) uses `ECGenParameterSpec("ED25519")` — invalid on Android Keystore; would throw if ever called (currently dead code). Fix before wiring.

## SCT-12 — Low — Attestation verifier claims exceed code

`HardwareAttestationVerifier.kt:33-36` doc claims "Root CA (Google root — pinned)"; `:71-98` never validates the chain to any root nor the challenge. Unused in production — fix or remove before use.

## SCT-13 — Low — Misc operational

Fixed 30s reconnect loop, no backoff/jitter (`MessageListenerService.kt:50-63`); `SecureMemoryWipe.secureDelete` exists but `WipeManager.wipeAll:34-39` uses plain `File.delete` (SQLCipher mitigates); MESSAGE_ACK/READ_RECEIPT trusted without message-id binding (`ContactExchangeManager.kt:263-277`) — relay-side status spoofing, cosmetic.

## SCT-14 — Low — Doc drift

`README.md:11,67` claims "minified + shrunk" but release sets `isMinifyEnabled=false, isShrinkResources=false` (`app/build.gradle.kts:104-105` — no R8 obfuscation, also eases reversing); `README.md:49` lists the removed `stealthx-ifr/` module; `domain/` contains unused duplicate-verification scaffolding (`KeyExchangeManager` with the same sxId-binding omission, plus unused `RuleEngine`, `X25519KeyManager`, `SecureFileManager`, `CryptoKeyDao`, `AuditLogRepository`).

---

## Prior-baseline re-check (BRIDGE.md 2026-07-12)

| Baseline claim | Verdict |
|---|---|
| Active transport = SIGNALING_RELAY central WS | **HOLDS** (`DataModule.kt:66-73`, `wss://api.stealthx.tech/signal`) |
| Tor/Onion planned, fail-closed | **HOLDS** (`TorRelayTransport.kt:16-28`, `OnionRelayTransport.kt:16-28` return `Failed`) |
| Play unlock disabled | **HOLDS** (`UpgradeViewModel.kt:36-46` stubs; gate `verifyNoClientSideGooglePlayUnlock`) |
| Paid controls closed pending VLABS | **HOLDS** (`data-ifr-enabled="false"` + JS early-return + CI node test gate) |
| Outbound buffering bounded + ordered | **HOLDS** (`PendingFrameBuffer` cap 256, order-preserving drain) |
| 222 tests green | NOT RE-VERIFIED (read-only mandate; recommend the dev reruns) |
| Commit #33 entitlement hardening | **VERIFIED REAL** (25s timeout, single-callback completion, retry/downgrade classification) |

## Verified strengths

- **Entitlement verifier genuinely strict** (`EntitlementTokenVerifier.kt`): Ed25519 over payload, embedded base64url trust anchor with build-time validation, aud/sub binding (sub = device sxId), iat 60s skew + exp + ≤31-day lifetime, product↔tier consistency, fixed claim set; fail-closed on tamper (tested).
- Tier cache HMAC-SHA256 with Keystore key, mismatch/expiry → FREE; refresh wipes token+cache on revoke.
- Contact bundles Ed25519-signed and verified on add; contact limit enforced atomically.
- Manifest hygiene: `allowBackup=false`, `usesCleartextTraffic=false`, service not exported, boot receiver action-gated, FLAG_SECURE in release; deep link only prefills the add-contact form (no drive-by add); NFC URI prefix-checked; **no WebView anywhere**.
- Crypto plumbing: real XChaCha20-Poly1305 via lazysodium with random 24B nonces; Argon2id 64MB/3it; HKDF correct; keys wiped on use paths; SQLCipher with Keystore-wrapped random passphrase; identity in EncryptedSharedPreferences.
- No FCM; notifications `VISIBILITY_SECRET` with generic text; message content never logged.
- **TLS pins verified live 2026-09-19:** all three pinned SPKI hashes match the live `api.stealthx.tech` chain exactly (leaf `1e85xNSE…` + two intermediates `nWN7PSep…`, `fk6IOKit…`) — pinning is active and correct today; intermediates carry rotation when the leaf renews (consider dropping the leaf pin or planning its rotation).
- No committed keystores/secrets (git ls-files clean); CI pins actions by SHA; ephemeral throwaway CI signing identity.
