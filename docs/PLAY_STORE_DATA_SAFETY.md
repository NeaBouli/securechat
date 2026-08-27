# SecureChat - Play Store Data Safety Form

This document reflects the `securechat.app` closed-alpha candidate at version
`0.1.11-alpha` (version code 15). Recheck it whenever relay, entitlement,
diagnostics, analytics, advertising, payment, or SDK behavior changes.

## Data Collection

| Question | Answer |
|----------|--------|
| Does the app collect or share user data? | Yes - limited data is transmitted off-device for app functionality |
| Is collected data encrypted in transit? | Yes |
| Can users request deletion? | Yes - through the privacy-policy contact channels |

## Data Types To Declare

| Play data type | Collected | Shared | Required | Purpose |
|----------------|-----------|--------|----------|---------|
| Personal info - User IDs | Yes | No* | Required for relay routing | App functionality; account management; security and fraud prevention |
| Personal info - Other info (activation code and entitlement token) | Yes | No | Optional; only after paid activation | App functionality; account management; security and fraud prevention |
| Personal info - Name (optional display handle) | Yes | No* | Optional | Contact exchange and app functionality |
| Messages | Yes | No* | Required when the user sends a message | App functionality |

`*` User-directed contact bundles and encrypted messages go to the recipient selected
by the user. Under Google Play's user-initiated transfer exception this is not
declared as third-party sharing, but it is still collected because it leaves the
device through the StealthX relay.

## Data Not Collected

- Android contacts or address book
- Precise or approximate location
- Photos, videos, audio, or user files
- Payment-card, bank-account, wallet, or purchase-history data
- Advertising ID, advertising data, analytics, crash logs, or diagnostics

## Processing Details

- A persistent pseudonymous `sx_...` identifier is sent to the StealthX relay for
  routing and presence.
- Signed public-key bundles can include routing identifiers, public keys, a timestamp,
  and an optional display handle. Private keys never leave the device.
- Messages contain sender/recipient routing identifiers and an end-to-end encrypted
  payload. The relay receives ciphertext, not message plaintext.
- Paid activation sends a user-entered activation code and pseudonymous device-bound
  identifier. Entitlement refresh later sends the signed entitlement token.
- Network endpoints use HTTPS or WSS. Message content is additionally protected by
  application-layer end-to-end encryption.

## Storage And Deletion

- Local structured data uses encrypted storage and sensitive preferences use Android
  protected storage. Cloud backup and device transfer are disabled.
- Users can delete local contacts/messages and can remove all local data by clearing
  app storage or uninstalling SecureChat.
- The app has no conventional user account. Server-side deletion requests use the
  contact channels in the privacy policy.
- Purchase and entitlement records may be retained for security, fraud prevention,
  refunds, tax, and legal obligations.

## SDK Inventory Relevant To Data Safety

- OkHttp for StealthX HTTPS/WSS network transport
- No Firebase, advertising, analytics, crash-reporting, Stripe, wallet, RevenueCat,
  or Google Play Billing SDK is included in the Android app

## Privacy Policy

URL: https://securechat.stealthx.tech/privacy.html

Contact: kaspartisan@proton.me
