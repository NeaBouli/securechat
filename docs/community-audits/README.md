# Community Audits — Collateral Web3 Open Audits

External audit of SecureChat, commissioned by the repository owner and published
with his explicit authorization (public series like IFR/Ekklesia/Stealth/
Prometheus/TrueRepublic). **Report-only**: no audited code changed; live checks
anonymous GET only; no secrets read. Companion audit of the sibling app:
NeaBouli/chameleon (same series).

- **Audit date:** 2026-09-19 (recon 2026-09-15, resumed/verified 2026-09-19)
- **Baseline:** `main` @ `9cebb36843e7b3d31baccd966ecb09bffa64dfa2`
- **Register:** SCT-01 … SCT-30 — **0 Critical / 4 High / 11 Medium / 14 Low / 1 Informational**
- **Prior baseline:** internal readiness audit 2026-07-12 (BRIDGE.md) — re-checked in the full-scope report (all major claims HOLD; 222-test suite not re-executed, read-only mandate)
- **Finding tracker:** umbrella issue (see issue list) with one checkbox per finding

## Reports

| # | Report | Register | Severity (C/H/M/L/I) | SHA-256 |
|---|--------|----------|----------------------|---------|
| 1 | [Full-scope security & crypto](sct-full-scope-audit-2026-09-19.md) | SCT-01…14 | 0/3/5/6/0 | `8c407a35fd3604bdcc7246601419822d20c4bc6fd062b04159fd97a597c9c96a` |
| 2 | [Surfaces, content & AI-readiness](sct-surfaces-content-audit-2026-09-19.md) | SCT-15…30 | 0/1/6/8/1 | `6d9e20a92f27c986d7d1c7de8296b86813c3e5cb0a34eff0cfb45c4bfedf202d` |

## Headline findings

- **SCT-01 (High):** the shipped message protocol is **not** a Double Ratchet —
  a symmetric chain over a *static* per-contact DH key, no post-compromise
  security; the real `DoubleRatchet.kt` is dead code while "Signal-grade …
  break-in recovery" is claimed on homepage/README/llms.txt/FAQ/wiki
  (`data/.../ChatSessionRepository.kt:30-65,109,146`).
- **SCT-02 (High):** sxId↔Ed25519 binding not enforced on contact import —
  impersonation via self-signed bundles; the chameleon repo received this exact
  fix on 2026-08-02, the backport here is missing
  (`data/.../ContactRepository.kt:82-100`).
- **SCT-03 (High):** unauthenticated relay IDENTIFY — anyone can claim any sxId
  (client-side instance of the SecureCall STX-01 class)
  (`data/.../ContactExchangeManager.kt:185-189`).
- **SCT-15 (High):** app-generated invite URLs carry sxId + full key bundle +
  handle into the GA4-tracked, unvalidated `?link=` page on stealthx.tech
  (`data/.../PublicKeyBundleQr.kt:36-40`; platform side = STX-29/31).

## Verified strengths (selection)

Strict entitlement verifier (Ed25519 + device-bound aud/sub + fail-closed HMAC
tier cache); SQLCipher + Keystore + EncryptedSharedPreferences; manifest
hygiene (no backup, no cleartext, no WebView, FLAG_SECURE); **TLS pins verified
live against the current chain 2026-09-19 (all 3 SPKI hashes match)**; IFR/paid
surfaces hard-gated with a real JS test; **zero trackers on the entire site**;
Tor/Onion transports fail-closed as documented.
