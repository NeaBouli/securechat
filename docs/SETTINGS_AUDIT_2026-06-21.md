# SecureChat Settings Audit - 2026-06-21

Scope: full Settings and Access/Upgrade wiring pass for the Android app.

## Findings fixed

- External website links in Settings and Upgrade now use guarded `ACTION_VIEW` handling and show a toast instead of crashing when no browser handler exists.
- Activation success copy now says `Unlocked` instead of `Unaccess`.
- Added a real `Background Message Listener` setting.
- The new setting is backed by encrypted preferences and immediately starts/stops `MessageListenerService`.
- `SecureChatApp` only auto-starts the foreground listener when the setting is enabled.
- `MessageListenerService` checks the setting on create/start and during reconnect, stops foreground mode when disabled, and closes the WebSocket via `ContactExchangeManager.stopListening()`.
- `ContactExchangeManager` now exposes `stopListening()` and clears pending frames/identified state.
- Version bumped to `versionCode=5`, `versionName=0.1.4-alpha`.

## Verified settings

- Biometric Unlock persists through `AppPreferences.biometricEnabled` and is consumed by `MainActivity`.
- STEALTH-DELETE persists through `AppPreferences.stealthDeleteEnabled` and is consumed by `ConversationsViewModel`.
- Duress PIN persists through encrypted preferences and is checked by `MainActivity`.
- Contact limit and Emergency Broadcast remain tier-gated by `TierGate`.
- Buy Lifetime Access and User Manual links return HTTP 200.
- Activation Code calls the activation backend and persists Pro/Elite through `AccessTierRepository`.
- IFR/wallet code guard passes for app source.

## Builds and installs

- `./gradlew --no-daemon --max-workers=1 verifyNoAppIfrWalletCode`
- `./gradlew --no-daemon --max-workers=1 app:assembleRelease app:bundleRelease`
- `./gradlew --no-daemon --max-workers=1 app:assembleInternalRelease`

Installed Internal APK:

- S7 `ce10160adc00152604`: `securechat.app` versionCode `5`, versionName `0.1.4-alpha`
- Tab S4 `ce12182c68644439037e`: `securechat.app` versionCode `5`, versionName `0.1.4-alpha`
- S10 was not connected.

Desktop artifacts:

- `/Users/gio/Desktop/SecureChat-LATEST.aab` SHA256 `e5928f415b49623559d6a2ab0bd6c0f908bb84a614140135b3dc46f10cadda9f`
- `/Users/gio/Desktop/SecureChat-Release-LATEST.apk` SHA256 `9d0c3dc7a134474b7ba7e52110b65236286b45d3c053f90fb8b35b6a557323e0`
- `/Users/gio/Desktop/SecureChat-Internal-LATEST.apk` SHA256 `d74c42d1bfea5259f149a73e4c714930160298c84117ce934b769f22b558a962`
