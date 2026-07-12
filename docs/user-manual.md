# SecureChat — User Manual

**Version 0.1.5-alpha · StealthX Platform**

---

## What Is SecureChat?

SecureChat is an end-to-end encrypted messenger where no message ever touches a server in readable form. Messages are encrypted on your device before they leave it and can only be decrypted by the intended recipient. There are no phone numbers, no email addresses, no cloud accounts, and no registration. Your identity is a cryptographic key pair generated locally and stored only on your device.

---

## How It Works

Every SecureChat user has a **StealthX ID** — a short, unique identifier derived from their Ed25519 public key. When you add a contact, you exchange public keys directly (via QR code or NFC) rather than through a server. This means no third party can intercept or fake the key exchange.

Once keys are exchanged, SecureChat uses a **Double Ratchet** protocol with XChaCha20-Poly1305 encryption for every message. Each message is encrypted with a different key. If one key is ever compromised, past and future messages remain secure. Messages are stored locally on your device in an encrypted database (SQLCipher, AES-256).

For current online delivery, both users must be online at the same time. A central StealthX signaling relay forwards the opaque encrypted payload and processes sender/recipient identifiers plus connection metadata; Tor, decentralized relays and multi-hop onion routing are roadmap features. QR export/import remains available for offline exchange.

---

## Tier Overview

SecureChat does not verify wallets inside the Android app. Browser-based IFR discounts are planned and currently disabled; paid activation remains launch-gated in VLABS.

| Feature | Free | Pro (≥ 2,000 IFR) | Elite (≥ 6,000 IFR) |
|---|---|---|---|
| E2E encrypted messaging | Yes | Yes | Yes |
| QR key exchange | Yes | Yes | Yes |
| Max contacts | 10 | Unlimited | Unlimited |
| Group messaging | Roadmap | Roadmap | Roadmap |
| Encrypted file transfer | Roadmap | Roadmap | Roadmap |
| Kaspa identity anchor | Roadmap | Roadmap | Roadmap |
| Chameleon integration | Roadmap | Roadmap | Roadmap |
| 3-hop onion routing | Roadmap | Roadmap | Roadmap |
| Decoy chat profiles | Roadmap | Roadmap | Roadmap |
| Advanced threat detection | Roadmap | Roadmap | Roadmap |
| Emergency broadcast | No | No | Yes |

---

## First-Time Setup

### Step 1 — Identity Creation

On first launch, SecureChat silently generates your cryptographic identity: an Ed25519 signing key and an X25519 encryption key. This happens in the background before you see any UI. Your keys are stored locally only — they are never sent to any server.

### Step 2 — Biometric Unlock

SecureChat uses biometric authentication (fingerprint, face, or PIN) to protect access to the app. On launch, a biometric prompt appears. Authenticate to proceed. This happens every time you open the app.

If biometric authentication is unavailable on your device, the app opens directly and displays an error. In this case, consider enabling a device screen lock.

### Step 3 — Add Your First Contact

The conversations screen will be empty. Tap the **+** button (bottom right) to open the Add Contact screen. You cannot send messages without first adding a contact and exchanging keys.

---

## Main Navigation

**Conversations** — The home screen. Shows all your active conversations sorted by last message time. Unread messages appear with a red badge. Tap a conversation to open it. When empty, the screen shows a placeholder message.

The toolbar has three controls:
- **ID button** (left) — opens your My ID screen
- **Lock icon** (center) — your 5-tap emergency wipe trigger (see Emergency Wipe below)
- **Settings** (right) — opens Settings

---

## Adding a Contact

Tap **+** on the Conversations screen to open the Add Contact screen.

**If you are at the contact limit (Free tier):**
A red card appears showing "Contact limit reached (10/10)". You must upgrade to Pro for unlimited contacts. The input field is disabled.

**Three ways to add a contact:**

**1. Scan QR Code** (fastest)
Tap **Scan QR Code**. The camera opens. Point it at your contact's QR code (shown on their My ID screen). The app validates the signature automatically and adds the contact.

**2. NFC Tap**
Coming in a future update.

**3. Paste QR Content**
If your contact cannot show you their QR code directly, they can share the text content of their QR code (via any messenger, email, or even printed). Paste it into the input field. The field expects the format `stealthx://add/sx_...?...`. Tap **Add Contact** when the button becomes active.

On success: "Contact added" appears and you return to Conversations. The new contact is now listed and ready for messaging.

---

## Your ID (My ID Screen)

Tap the **ID button** in the toolbar to see your identity.

**What is displayed:**
- Your StealthX ID (format: `sx_` followed by 9 characters, e.g., `sx_a7Kx9mPq2`)
- A QR code encoding your full public key bundle
- Your custom handle, if set

**Sharing your ID:**
Show the QR code to your contact and have them scan it. Or tap **Share** to send the text version of your ID via any app.

**Important:** Your StealthX ID works for both SecureChat and SecureCall. Share it once and your contact can reach you on both apps.

---

## Sending and Receiving Messages

Open a conversation by tapping it on the home screen.

**Sending a message:**
Type in the text field at the bottom and tap **Send**. Each message is encrypted individually before leaving your device. Message status indicators:
- *Pending* — queued for delivery
- *Sent* — delivered to recipient's device
- *Failed* — delivery failed (recipient may be offline)

**QR Message Export (air-gapped sending):**
Tap the **QR** button in the bottom bar to export your last outgoing message as a QR code. Show this QR to your contact. They scan it in their Chat screen using the QR scanner button in the top bar. This method works completely offline with no internet connection on either side.

**Importing a message:**
Tap the file icon in the top bar to open the import field. Paste the text content of a message received outside the app. This is the text equivalent of the QR export.

**Safety Number verification:**
Tap the **Shield** icon in the top bar to see the Safety Number for this conversation. A Safety Number is a series of digit groups derived from your shared secret. Contact your conversation partner through a separate channel and compare these numbers out loud. If they match, your keys are genuine and no man-in-the-middle attack has occurred.

---

## Settings — Complete Reference

Open Settings from the toolbar (gear icon on the right).

---

### Tier Status Card

At the top of Settings, a card shows:
- Your current tier (FREE / PRO / ELITE) with color coding
- Signed activation tier and entitlement expiry, when configured
- Local cache validity

---

### Security

**Biometric Unlock**
When enabled, the app requires biometric authentication (fingerprint, face, or PIN) every time it is opened or resumed from the background. Disable only if you have another device-level lock in place. Default: **On**.

**Stealth Delete (5-tap)**
When enabled, tapping the lock icon in the Conversations toolbar five times triggers an immediate, irreversible wipe of all app data. Default: **On**.

What the wipe deletes:
- The entire encrypted message database (including WAL and SHM journal files)
- All encrypted preferences and activation settings
- Your StealthX identity (keys)
- All app cache and code cache directories

After the wipe the app closes. There is no recovery.

---

### Free Features

**E2E Encrypted Messaging**
End-to-end encrypted messaging using XChaCha20-Poly1305 and Double Ratchet. Available to all tiers.

**QR Key Exchange**
Device-to-device public key exchange via QR code or text. Available to all tiers. No server involved.

**Contact Limit**
Shows your current contact usage. Free tier: 10 contacts maximum. Pro and Elite: unlimited. Displayed as "9/10 contacts used (Free tier)" or "Unlimited".

---

### Pro Roadmap Features

Each of the following shows a lock icon and **Unlock** button if your tier is below Pro.

**Group Messaging**
Planned encrypted group chats. Not implemented in the current release.

**Encrypted File Transfer**
Planned end-to-end encrypted file sharing. Not implemented in the current release.

**Kaspa Identity Anchor**
Planned public-key anchoring. The current release does not write an identity to Kaspa.

**Chameleon Integration**
Planned cross-product integration. It is not active in the current release.

---

### Elite Features And Roadmap

Each of the following shows a lock icon and **Unlock** button if your tier is below Elite.

**Onion Routing (3-hop)**
Roadmap only. Current network delivery uses the central signaling relay and must not be treated as Tor or multi-hop traffic.

**Decoy Chat Profiles**
Roadmap only. The current duress flow wipes local data before showing a decoy screen; it does not create fake conversation histories.

**Advanced Threat Detection**
Roadmap only. The current release does not provide behavioral surveillance detection.

**Emergency Broadcast**
Send an encrypted alert message to all your contacts simultaneously with one tap. Available from Elite tier. Tap to open the Broadcast screen.

---

### IFR Holder Discount

IFR holder discounts are launch-gated and not active in the current sales flow. Any future browser verification will remain outside the Android app and will be documented before activation.

- >= 2,000 IFR -> Pro discount
- >= 6,000 IFR -> Elite discount
- WalletConnect is not used inside the Android app.

---

### About

- Version: 0.1.0-alpha
- Platform: SecureChat — StealthX Platform

---

## Permissions Reference

| Permission | Purpose |
|---|---|
| Camera | QR code scanning for contact key exchange |
| Biometric | App unlock authentication |
| NFC | Contact exchange via NFC (future) |
| Notifications | Future: incoming message alerts |
| Internet | IFR token verification via Ethereum RPC |

SecureChat requests no contacts, phone state, location, or call log permissions. It does not access your address book, SMS, or phone.

---

## Emergency Wipe

On the Conversations screen, the **lock icon** in the toolbar center is the wipe trigger. Tap it five times rapidly (within a few seconds). There is no confirmation dialog and no delay. All data is destroyed immediately and the app closes.

This is designed for high-urgency situations where you need to destroy the app's contents faster than you can type a PIN or navigate to a settings menu.

---

## Security Architecture — Brief Overview

**Message encryption:** XChaCha20-Poly1305 with a 192-bit random nonce per message. A 16-byte authentication tag prevents any tampering. Messages are padded to 256-byte blocks to prevent size-based analysis.

**Key exchange:** X25519 Elliptic Curve Diffie-Hellman. Generates a shared secret without transmitting private keys.

**Forward secrecy:** Double Ratchet. Each message uses a different key derived from the ratchet state. Old message keys are deleted after use. A compromise of today's key reveals nothing about past or future messages.

**Identity signing:** Ed25519. Every contact's public key bundle is signed by their private signing key. SecureChat verifies this signature when you add a contact, preventing impersonation.

**Storage:** SQLCipher AES-256 encrypted database. Encrypted SharedPreferences (AES-256-GCM) for app settings.

**IFR cache integrity:** HMAC-SHA256 over the cached tier data. The HMAC key lives in Android Keystore (hardware-backed TEE or StrongBox). Any tampering with the cache is detected and the tier reverts to Free.

---

## Troubleshooting

**My contact is not receiving messages**
Both users must be online at the same time for message delivery. If your contact is offline, use the QR message export to deliver the message without an internet connection.

**I scanned the QR but the contact was not added**
Ensure you are scanning the contact's QR from their My ID screen, not a screenshot (FLAG_SECURE may block screenshots). Ask them to share the text content of their ID instead.

**Biometric fails and the app closes**
The app closes when biometric authentication fails to protect your data. Check your device's biometric enrollment in Android Settings.

**My tier shows Free after verifying IFR**
Verify the wallet on securechat.stealthx.tech/#ifr. If you need IFR, use the Uniswap link there, then start the discounted Stripe checkout.

**I wiped by accident**
The wipe is irreversible by design. There is no backup and no recovery. Your contacts will need to re-add you using your new identity after you reinstall.
