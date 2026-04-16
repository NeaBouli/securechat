# SecureChat

**End-to-end encrypted messaging for Android. Zero metadata.**

Part of the [StealthX Platform](https://stealthx.tech).

## Status

**v0.1.0-alpha — Internal Alpha — not for public use**

BUILD SUCCESSFUL — Debug 42 MB / Release 21 MB (minified + shrunk)

## Features
- XChaCha20-Poly1305 end-to-end encryption
- X25519 key exchange + Double Ratchet forward secrecy
- Argon2id password KDF + Ed25519 identity signing
- Unified `sx_` ID across SecureCall, SecureChat, Chameleon
- QR code + NFC contact exchange (zxing-embedded)
- Safety numbers for manual verification (6x4 digit groups)
- **Emergency Broadcast** (Elite tier, Phase 2)
- No phone number, no account, no metadata
- GPL-3.0 open source

## Crypto Stack

| Component | Algorithm | Parameters |
|-----------|-----------|------------|
| Symmetric Encryption | XChaCha20-Poly1305 | 256-bit AEAD, 192-bit nonce |
| Key Exchange | X25519 ECDH | Curve25519 |
| Forward Secrecy | Double Ratchet | HKDF-SHA256 |
| Password KDF | Argon2id | 64 MB memory, 3 iterations |
| Identity Signing | Ed25519 | Curve25519 signatures |
| Key Storage | Android Keystore | StrongBox / TEE |

Single library: `lazysodium-android 5.1.0`. No AES-GCM. No BouncyCastle. No custom crypto.

## Unified StealthX ID
Format: `sx_[9 Base58 chars]` — e.g. `sx_a7Kx9mPq2`
Deterministic from Ed25519 public key. Same ID works in SecureCall, SecureChat, and Chameleon.
See [UNIFIED_ID_SYSTEM.md](docs/UNIFIED_ID_SYSTEM.md).

## Module Structure (13 Gradle modules)

```
app/                  Entry point, Hilt DI graph
stealthx-crypto/      THE ONLY crypto module (XChaCha20, DR, Argon2id)
stealthx-ifr/         IFR Token, WalletConnect, AppSignature
security/             Android Keystore, SecureMemoryWipe
shared/               Pure JVM data models (PublicKeyBundle, StealthXContactId)
data/                 Room + SQLCipher, StealthXIdentity, Entities
domain/               EncryptionEngine, TierGate, KeyExchangeManager, MessageRouter
presentation/         Jetpack Compose UI, Navigation, Theme
transport/            RelayTransport interface + Local/Tor/Onion implementations
features/messenger/   Chat UI
features/contacts/    QR + NFC contact exchange
features/settings/    Settings
features/broadcast/   Emergency Broadcast (Elite, Phase 2)
```

**Dependency rule:** `:domain` never imports `:data`. Crypto only in `:stealthx-crypto`. Tier checks only in `TierGate`.

## Build
```bash
git clone https://github.com/NeaBouli/securechat
cd securechat
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (minified + shrunk)
```

Requirements: JDK 17, Android SDK 35, Android 8.0+ (API 26+) target devices.

## IFR Token Tiers

| Tier | IFR Lock | Lifetime Price | Features |
|------|----------|---------------|----------|
| Free | 0 IFR | — | Core messaging, 10 contacts |
| Pro | >= 2,000 IFR | EUR 9 | Unlimited contacts, groups, Kaspa identity |
| Elite | >= 6,000 IFR | EUR 19 | 3-hop onion, decoy profile, Emergency Broadcast |
| Suite | >= 8,000 IFR | EUR 54 | All StealthX products, lifetime |

Same IFR lock works across SecureCall + SecureChat + Chameleon.
See [PRICING.md](docs/PRICING.md).

## Documentation
- [LOGBUCH.md](LOGBUCH.md) — Development log (SC-00 to SC-10)
- [ECOSYSTEM.md](ECOSYSTEM.md) — StealthX platform overview
- [docs/UNIFIED_ID_SYSTEM.md](docs/UNIFIED_ID_SYSTEM.md)
- [docs/RELAY_ARCHITECTURE.md](docs/RELAY_ARCHITECTURE.md)
- [docs/TRANSPORT_LAYER.md](docs/TRANSPORT_LAYER.md)
- [docs/PRICING.md](docs/PRICING.md)
- [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md)
- [docs/TODO.md](docs/TODO.md)
- [SECURITY.md](SECURITY.md) — Vulnerability disclosure policy
- [CONTRIBUTING.md](CONTRIBUTING.md)

## License
GPL-3.0-or-later. Source-available. Audit every line.

## Website
See https://securechat.stealthx.tech

---

# 🔐 SecureChat — GitHub Pages Site

Landing page and Wiki for **SecureChat** — a StealthX Platform product.

**Live site:** https://neabouli.github.io/securechat/

## Structure

```
index.html          Main landing page
wiki/
  index.html        Wiki hub
  architecture.html System architecture
  security-design.html Crypto spec + threat model
  roadmap.html      Development phases
  ifr-unlock.html   IFR Token guide
privacy.html        Privacy policy
```

## Design

Mirrors stealthx.tech design — dark tactical aesthetic, green logo accent, same CSS variables.

## Related

- [SecureCall](https://stealthx.tech) — encrypted voice calls
- [Chameleon](https://github.com/NeaBouli/chameleon) — privacy OS
- [IFR Token](https://ifrunit.tech) — unified lifetime access
