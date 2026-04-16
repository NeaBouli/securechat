# SecureChat

**End-to-end encrypted messaging for Android. Zero metadata.**

Part of the [StealthX Platform](https://stealthx.tech).

## Status
v0.1.0-alpha — Android app in development. Website live.

## Features
- XChaCha20-Poly1305 end-to-end encryption
- X25519 key exchange + Double Ratchet forward secrecy
- Unified sx_ID across SecureCall, SecureChat, Chameleon
- QR code + NFC contact exchange
- Safety numbers for manual verification
- No phone number, no account, no metadata
- GPL-3.0 open source

## Build
```bash
git clone https://github.com/NeaBouli/securechat
cd securechat
./gradlew assembleDebug
```

## Website
See https://securechat.stealthx.tech or /docs/ folder.

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
