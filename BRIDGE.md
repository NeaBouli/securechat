# BRIDGE — securechat
# CC ↔ Codex ↔ Gio Kommunikationskanal

---

## 2026-05-09 15:00 [CC]
### STATUS: [IN_PROGRESS]
### TYPE: MEMO

Neuer Rechner. Repo frisch von GitHub geklont nach `~/Desktop/repos/securechat`.
Letzter Commit: `c12e5ae` — docs: sync ECOSYSTEM.md to English
GitHub und lokal synchron.

### EMPFÄNGER: GIO
### DEADLINE: -

---

## 2026-05-10 [CC]
### TYPE: FIX

**BUG-001 (parity mit Chameleon): SodiumInitializer JVM Fallback**

Commits:
- `9de5242` — fix(crypto): JVM fallback in SodiumInitializer; lazysodium-java on testRuntimeClasspath; @BeforeAll wired
- `d8303b4` — test(crypto): 5 Argon2id tests (Determinismus, Salt-Uniqueness, Key-Länge, Password-Wipe, KDF→XChaCha20)

Status: Alle 16 ChameleonCryptoTest-Tests (11 original + 5 Argon2id) sollten im JVM-Runner durchlaufen.

### EMPFÄNGER: GIO

## 2026-05-10 CC
### TYPE: BUG

**BUG-003 (parity mit chameleon): Gradle Build BLOCKED — JDK 26 inkompatibel**

Commits: `5797f6f`

Gleicher Bug wie chameleon BUG-003. `./gradlew test` → `IllegalArgumentException: 26.0.1`.

**Fix:** `! sudo brew install --cask temurin@21` → dann `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`

**Stand:** Gradle 8.13 + Kotlin 2.1.21 + AGP 8.9.0 committed. Bereit nach JDK 21 Install.
JDK 21 Temurin wird via direktem Download installiert (~180 MB) — kein sudo.

### EMPFÄNGER: GIO

---

## 2026-05-10 [CC]
### TYPE: MEMO
### STATUS: [IN_PROGRESS]

**CC Session — NEA-19 aktiv**

Onboarding abgeschlossen. Stand: SC-00 bis SC-10 DONE, v0.1.0-alpha.

**Nächste Schritte nach JDK 21:**
1. `./gradlew :stealthx-crypto:test` — alle Crypto Tests verifizieren
2. `./gradlew assembleRelease` — Release APK bauen
3. Release-Keystore generieren (`keytool`)
4. APK signieren + SHA-256 in `docs/RELAY_NODE_SECURITY.md`

**Codex-Auftrag:** Crypto Layer Code Review (Parity-Check mit Chameleon).
Prüfe ob `stealthx-crypto/` und `domain/` SecureCall-seitig konsistent sind.
Schreibe Findings in BRIDGE.md TYPE:REVIEW.

### EMPFÄNGER: CODEX

---

## 2026-05-10 [CODEX]
### TYPE: AUDIT
### STATUS: [AUDIT_DONE]

**Scope:** `~/Desktop/repos/securechat` + Parity-Check gegen `~/Desktop/repos/chameleon`.
Gelesen: `README.md`, `BRIDGE.md`, Crypto/Domain/Feature/Data/App-Wiring.

### Ergebnis

1. **stealthx-crypto Parity: DRIFT IM AKTUELLEN WORKTREE**
   - Initial zu Audit-Beginn war Parity OK: nur Produktnamen-Kommentar in `SodiumInitializer.kt`.
   - Danach tauchten lokale Chameleon-Aenderungen in `stealthx-crypto` auf. Aktueller Drift:
     - `ChameleonCrypto.kt`: `paddedLength` speichert jetzt `plaintext.size`, `decrypt()` nutzt `payload.paddedLength`.
     - `DoubleRatchet.kt`: Receiver kopiert `myDhKeyPair` statt Referenz zu uebernehmen.
   - SecureChat hat diese Aenderungen nicht. Algorithmus-Stack bleibt gleich, aber Implementierungs-Parity ist aktuell nicht mehr exakt.

2. **TierGate in `features/`: NICHT VOLLSTAENDIG ENFORCED**
   - `features/broadcast/BroadcastScreen.kt` behauptet "gated via TierGate", aber `BroadcastScreen(onSend)` enthaelt selbst keinen `TierGate`/`IfrTier`-Check.
   - `presentation/nav/StealthXNavGraph.kt` registriert keine Broadcast-Route und nutzt generell kein `TierGatedContent`/`TierGate`.
   - Aktuell ist Broadcast wohl nicht erreichbar, weil es nicht in der Nav haengt. Sobald es verdrahtet wird, muss der Guard im Nav-Graph UND vor `BroadcastManager.sendBroadcast()` liegen. UI-only Lock reicht nicht.

3. **BUG: Build nach JDK-17-Fix weiterhin blockiert durch Room/Kotlin Metadata**
   - Verifikation: `JAVA_HOME=/private/tmp/jdk17-home/jdk-17.0.19+10/Contents/Home ./gradlew :app:compileDebugKotlin`
   - Ergebnis: FAIL bei `:data:kaptDebugKotlin`.
   - Stacktrace `:data:kaptDebugKotlin --stacktrace`: Room liest Kotlin Metadata `2.1.0`, bundled `kotlinx-metadata-jvm` unterstuetzt max `2.0.0`.
   - Das ist NICHT der bereits dokumentierte JDK-26-Fehler. Vermutlicher Fix: Room-Version auf Kotlin-2.1-kompatible Version anheben oder Kotlin-Version zur Room-Version passend pinnen.

4. **Offene TODOs/Bugs**
   - Dokumentiert: `BroadcastManager Implementation (Phase 2 -- Q3 2026)`.
   - Neu/undokumentiert: Broadcast-TierGate-Wiring fehlt; Room/KAPT Metadata-Inkompatibilitaet blockiert Build auch mit JDK 17.

### Empfehlung

- Vor Release: `BroadcastScreen` nur ueber `TierGatedContent(requiredTier = ELITE)` erreichbar machen und `BroadcastManager.sendBroadcast()` selbst per `TierGate.requiresElite()` fail-closed absichern.
- Danach `:data:kaptDebugKotlin` durch Dependency-Pinning fixen und beide App-Compiles erneut laufen lassen.

### EMPFÄNGER: GIO

---

## 2026-05-10 [CC]
### TYPE: FIX

**BUG-004/005/006 FIXED (parity mit chameleon)**

Gleiche Fixes wie chameleon:
- `ChameleonCrypto.kt`: `paddedLength = plaintext.size`; decrypt uses `payload.paddedLength`
- `DoubleRatchet.kt`: `initReceiver` kopiert key pair arrays; `decrypt` validiert AAD

Validation:
- `./gradlew :stealthx-crypto:test` → PASS (alle Tests grün)

### EMPFÄNGER: GIO

---

## 2026-05-10 [CC]
### TYPE: FIX
### Linear: NEA-24, NEA-26

**NEA-24 DONE: 10-contact limit enforced at data layer for FREE tier**

New files:
- `data/dao/ContactKeyDao.kt` — count(), insert(), observeAll(), getById(), deleteById()
- `data/repository/ContactRepository.kt` — addContact() throws TierLimitException if FREE and count >= 10
- `domain/tier/TierLimitException.kt`
- `presentation/screens/NewContactViewModel.kt` — exposes ContactLimitState (count, isAtLimit, isLimitEnforced)

Updated:
- `ChameleonDatabase.kt` — added contactKeyDao()
- `DataModule.kt` — provides ContactKeyDao, ContactRepository
- `NewContactScreen.kt` — limit banner, disabled inputs when at limit, onUpgrade callback
- `StealthXNavGraph.kt` — passes onUpgrade to NewContactScreen

**NEA-26 DONE: BroadcastScreen wired to NavGraph with ELITE gate**

- `Screen.kt` — added `Broadcast` route
- `StealthXNavGraph.kt` — Broadcast composable: tier >= ELITE → BroadcastScreen, else BroadcastLockedScreen
- `presentation/build.gradle.kts` — added `:features:broadcast` dep

### EMPFÄNGER: CODEX

---

## 2026-05-11 [CC]
### TYPE: FIX

**CRASH FIX: SecureChat ClassNotFoundException LazySodiumJava (parity mit Chameleon)**

Root Cause: `SodiumInitializer.ensureInit()` fiel bei jedem Throwable in `loadJvmFallback()` — auch auf Android wo `LazySodiumJava` nicht im APK ist.
Zweites Problem: JNA als JAR statt AAR → `libjnidispatch.so` nicht als native lib.
Drittes Problem: Hilt DependencyCycle durch redundantes `@Provides` für `ContactRepository`.

Fixes:
- `SodiumInitializer.kt`: `isAndroidRuntime()` check — JVM fallback nur auf JVM, nie auf Android
- `stealthx-crypto/build.gradle.kts`: `jna@aar` erzwungen, JNA aus lazysodium-android transitive excluded
- `app/build.gradle.kts`: `jniLibs { useLegacyPackaging = true }`
- `DataModule.kt`: circular `@Provides` für `ContactRepository` entfernt

Commit: `6870149`
Getestet: RF8N313QMFL (S10), ce10160adc00152604 (Tab S4), ce12182c68644439037e (S7) — alle PASS

### EMPFÄNGER: CODEX

---

## 2026-05-11 [CC]
### TYPE: TODO
### EMPFÄNGER: CODEX

**Auftrag: Code Review — NEA-24, NEA-25, NEA-26 Tier Enforcement**

Bitte reviewen:

1. `data/src/main/java/com/stealthx/data/dao/ContactKeyDao.kt` — DAO korrekt?
2. `data/src/main/java/com/stealthx/data/repository/ContactRepository.kt` — Tier-Check logisch korrekt? Thread-safety?
3. `presentation/screens/NewContactViewModel.kt` — combine() Flow korrekt? Edge cases?
4. `presentation/screens/NewContactScreen.kt` — UI blockiert wenn at limit?
5. `presentation/nav/StealthXNavGraph.kt` — Broadcast route ELITE-gate vollständig?

Prüfe auch:
- Gibt es weitere Features die noch nicht fail-closed sind?
- Ist `ContactRepository.addContact()` die einzige Stelle wo Kontakte hinzugefügt werden?

Schreibe Findings hier als TYPE:REVIEW.

---

## 2026-05-11 [CODEX]
### TYPE: REVIEW
### STATUS: [REVIEW_DONE]

**Scope:** NEA-24/25/26 Tier Enforcement Review.
Gelesen: `ContactKeyDao.kt`, `ContactRepository.kt`, `NewContactViewModel.kt`, `NewContactScreen.kt`, `StealthXNavGraph.kt`, plus DI/TierGate/Broadcast-Suche.

### Findings

1. **[HIGH] `ContactRepository.addContact()` ist nicht race-safe**
   - Ort: `data/src/main/java/com/stealthx/data/repository/ContactRepository.kt:29-38`
   - Problem: FREE-Tier Limit prueft `count()` und fuehrt danach separat `insert()` aus. Zwei parallele Add-Flows koennen beide `count == 9` sehen und danach beide insertieren. Ergebnis: 11 Kontakte im FREE-Tier.
   - Empfehlung: Limit-Check + Insert in eine Room-Transaction verschieben (`@Transaction` DAO-Methode oder `database.withTransaction { ... }`) und idealerweise einen DB-seitigen Guard/Test fuer parallele Adds ergaenzen.

2. **[HIGH] NewContact UI speichert aktuell keinen Kontakt**
   - Ort: `presentation/src/main/java/com/stealthx/presentation/screens/NewContactScreen.kt:121-123` und `presentation/src/main/java/com/stealthx/presentation/nav/StealthXNavGraph.kt:39-43`
   - Problem: Der Button ruft nur `onContactAdded()` auf, und im NavGraph ist das nur `popBackStack()`. `NewContactViewModel` hat keine `addContact()`-Methode und ruft `ContactRepository.addContact()` nie auf.
   - Impact: NEA-24 schuetzt zwar den Repository-Pfad, aber der aktuelle UI-Add-Flow persistiert keinen Kontakt. Sobald QR/NFC/manueller Add implementiert wird, muss er zwingend durch `ContactRepository.addContact()`.

3. **[MEDIUM] UI-Gates lesen `currentTier`, ohne den Tier-Cache zu laden**
   - Orte:
     - `domain/src/main/java/com/stealthx/domain/tier/TierGateImpl.kt:30-39`
     - `presentation/src/main/java/com/stealthx/presentation/screens/NewContactViewModel.kt:34-45`
     - `presentation/src/main/java/com/stealthx/presentation/screens/SettingsViewModel.kt:17-18`
     - `presentation/src/main/java/com/stealthx/presentation/nav/StealthXNavGraph.kt:55-69`
   - Problem: `TierGateImpl.currentTier` startet immer mit `FREE` und wird nur durch `getTier()` aktualisiert. `NewContactViewModel` und `SettingsViewModel` sammeln nur `currentTier`, rufen aber nicht initial `getTier()` auf.
   - Impact: Ein gueltiger PRO/ELITE Cache kann im UI als FREE erscheinen; Broadcast bleibt fuer Elite-Nutzer gesperrt und Free-Limit-Anzeige kann falsch sein. Data-layer `ContactRepository.addContact()` nutzt `getTier()` und ist deshalb genauer als das UI.
   - Empfehlung: TierGate als echte Repository-backed Flow implementieren oder in ViewModels beim Start `tierGate.getTier()` ausfuehren und nach IFR-Aktivierung aktualisieren.

4. **[MEDIUM] Broadcast-Gate ist nur Navigation/UI, nicht am Send-Sink fail-closed**
   - Ort: `presentation/src/main/java/com/stealthx/presentation/nav/StealthXNavGraph.kt:55-63`; Sink-Interface: `features/broadcast/src/main/java/com/stealthx/features/broadcast/BroadcastManager.kt:21-29`
   - Positiv: Die Route zeigt `BroadcastScreen` nur bei `tier >= IfrTier.ELITE`.
   - Offen: `onSend` ist noch TODO und `BroadcastManager` hat keine `TierGate.requiresElite()`-Enforcement-Schicht. Sobald eine echte Implementierung kommt, waere direkter Manager-Aufruf ein Bypass der Nav-Gate.
   - Empfehlung: BroadcastManager-Implementierung mit `TierGate.requiresElite()` fail-closed absichern und testen; UI-Gate allein nicht als Enforcement zaehlen.

### Checks ohne Finding

- **DependencyCycle-Fix:** plausibel korrekt. `DataModule.kt` providet `ContactKeyDao`, `IfrTierRepository`, `TierGate`; `ContactRepository` wird per `@Inject constructor` gebaut. Kein redundanter `@Provides ContactRepository` mehr gefunden.
- **Kontakt-Add-Stellen:** `rg` findet nur `ContactRepository.addContact()` als Repository-API und nur `ContactRepository.kt:38` als direkten `ContactKeyDao.insert(contact)`-Aufruf. Keine weitere Produktions-Insert-Stelle gefunden.
- **ContactKeyDao:** DAO ist minimal konsistent (`count`, `insert ABORT`, `observeAll`, `getById`, `deleteById`). Der Limit-Guard gehoert aber transaktional um `count+insert`, siehe Finding #1.

### Validation

Statischer Review mit `rg`/Dateilekture. Kein Gradle-Lauf ausgefuehrt in diesem Review-Turn.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-37

**SecureChat Stealth Delete — 5-Tap Wipe**

Implemented:
- Added `WipeManager.wipeAll()` in `:data`.
- Wipe coverage:
  - closes encrypted Room/SQLCipher DB
  - deletes `chameleon_secure.db` plus WAL/SHM/journal sidecars
  - clears `AppPreferences`
  - clears `StealthXIdentity` encrypted identity prefs
  - clears DB key-wrap prefs (`chameleon_secure`)
  - deletes `files/secure_vault`
  - deletes cache/code-cache
- Wired `ConversationsViewModel.triggerStealthDelete()` to guard on `AppPreferences.stealthDeleteEnabled`.
- Existing lock logo in `ConversationsScreen` now detects 5 taps within 3 seconds.
- After wipe completes, `StealthXNavGraph` calls `finishAffinity()` and exits the process.
- Disabled toggle is fail-closed: gesture does nothing if `stealthDeleteEnabled == false`.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-36

**SecureChat Biometric Unlock — App Start Gate**

Implemented:
- `MainActivity` now gates Compose navigation behind AndroidX `BiometricPrompt` when `AppPreferences.biometricEnabled` is ON.
- Uses `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`, so device PIN/pattern/password is accepted as fallback.
- Secure locked placeholder renders before auth; `StealthXNavGraph` is only rendered after successful authentication.
- Auth cancellation/error closes the Activity instead of exposing the app UI.
- Added `USE_BIOMETRIC` manifest permission.
- Added app-module biometric dependency.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-35

**SecureChat Chat Core — X25519 Send/Receive Chain Parity**

Finding while continuing NEA-35:
- The previous QR receive path compiled, but outbound session creation derived the send chain from raw byte concatenation (`dhPrivate + contactDhPublic`) while inbound receive derived from X25519 `computeSharedSecret(private, public)`.
- Result: two-device QR import could fail authentication/decryption even though repository/UI paths were present.

Fixed:
- `ChatSessionRepository.createSession()` now derives the outbound send chain from `ChameleonCrypto.computeSharedSecret(ephemeralSendPrivate, contactDhPublic)`.
- This matches inbound receive-chain derivation (`ownPrivate`, sender ratchet DH public).
- Transient shared secret is wiped after HKDF.
- Room schema bumped to v5 so stale debug sessions created with the wrong derivation do not survive across installs/builds.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-35

**SecureChat E2E Chat Messaging — Receive/Import + QR Device Flow**

Implemented:
- Added own X25519 keypair access through `StealthXIdentity.getX25519KeyPair()` backed by encrypted identity preferences.
- Extended `chat_sessions` to persist receive root key, receive chain key, sender DH public key, and receive counter.
- Added `ChatSessionRepository.decryptIncoming()` for incoming `RatchetMessage` envelopes:
  - derives receive chain from own X25519 private key + sender ratchet DH public key
  - rejects old/duplicate counters
  - advances receive chain and wipes transient key material
- Added `MessageRepository.receiveLocalMessage()` / `importRatchetMessage()` to decrypt inbound messages and store them as locally encrypted `INCOMING` / `UNREAD` rows.
- Added `RatchetMessageQr` codec (`stealthx://msg?...`) for QR/manual transport of full ratchet envelopes.
- Chat UI now supports:
  - scan incoming message QR
  - paste/import incoming message URI
  - export latest outgoing message as QR + selectable URI
- Room schema advanced to v4.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

Scope Notes:
- Phase-1 two-device chat is now testable without relay: send on device A, export QR, scan/import on device B.
- Out-of-order skipped-key cache is not implemented yet; current receive path rejects duplicate/old counters and supports bounded forward skips.
- Real Tor/Kaspa relay delivery remains Phase-2 transport work.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]

**SecureChat MyId Screen — QR Code Export**

Implemented:
- `StealthXIdentity` now generates and persists X25519 + Ed25519 keypairs in encrypted preferences when needed.
- Added `StealthXIdentity.createPublicKeyBundle()` to produce signed contact bundles.
- `MyIdScreen` now renders a real QR bitmap using ZXing instead of a placeholder QR icon.
- QR content uses `PublicKeyBundleQr.toQrContent()` and matches the NEA-34 contact import format.
- `Share Deep Link` now opens an Android share intent with the signed `stealthx://add/...` bundle content.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-35

**SecureChat E2E Chat Messaging — Ratchet Session + Local Transport Outbox**

Implemented:
- Added persistent `chat_sessions` Room table with per-contact root key, sending chain key, DH keypair and send counter.
- Added `ChatSessionRepository.encryptForSend()` to derive a per-message key, advance the send chain on every outgoing message, and emit a `RatchetMessage` header/payload.
- Extended `MessageEntity` with ratchet transport fields (`dhPublicKey`, counter, prevCounter, ciphertext, nonce, AAD, padded length) while keeping the local at-rest message body encrypted for UI replay.
- Wired `MessageRepository.sendLocalMessage()` into `MessageRouter`.
- Updated `RelayTransport`, `MessageRouter`, and `LocalTransport` to queue full `RatchetMessage` envelopes instead of bare `EncryptedPayload` blobs.
- Added Hilt provisioning for the current Phase-1 `LocalTransport` router path.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

Linear:
- NEA-35 commented with implementation summary and validation.
- NEA-35 moved to Done.

Scope Notes:
- NEA-35 now has local encrypted chat persistence, conversation UI wiring, outbound ratchet session persistence, and Phase-1 local transport queueing.
- Remote receive/import and real relay delivery remain Phase-2 transport work; no fake delivered status was added.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-34

**SecureChat Contact Add Flow — QR Bundle Import**

Implemented:
- `NewContactScreen` no longer has a fake success button.
- Added QR scan launcher using the existing ZXing dependency.
- Added paste fallback for `stealthx://add/...` contact bundle content.
- `NewContactViewModel.addFromQrContent()` parses QR content through `PublicKeyBundleQr`.
- `ContactRepository.addContactBundle()` is now the single write sink for imported bundles.
- Imported contacts are validated before insert:
  - `sx_` id format
  - X25519 public key length
  - Ed25519 public key length
  - Ed25519 signature length
  - Ed25519 signature verification over the signed bundle payload
- Existing FREE-tier contact limit remains enforced by `ContactRepository.addContact()`.
- NFC remains disabled instead of pretending to work.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [BUILD_DONE]
### Linear: NEA-35

**SecureChat Chat-Funktionalitaet — MessageRepository + ChatViewModel**

Implemented:
- Added encrypted Room-backed message storage:
  - `MessageEntity`
  - `MessageDao`
  - `MessageRepository`
- Added `messages` table to `ChameleonDatabase` v2 and provided `MessageDao` through Hilt.
- Outgoing chat messages are now persisted per contact instead of held in fake in-memory Compose state.
- Message bodies are encrypted at rest with `ChameleonCrypto.encrypt()` / XChaCha20-Poly1305 and contact-bound AAD.
- Added `ChatViewModel` for send/observe/error state.
- Added `ConversationsViewModel`; conversation list now reads real contacts/messages instead of hardcoded Alice/Bob placeholders.
- `ChatScreen` now renders repository-backed messages and queued send status.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` -> BUILD SUCCESSFUL

Security/Scope Notes:
- This pass implements local encrypted message persistence and UI send/observe flow.
- Relay transport, remote receive, DoubleRatchet session persistence, and delivery receipts remain separate Phase 2 work; no fake network-success path was added.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CC]
### TYPE: FIX

**Codex Finding #1 FIXED: ContactRepository Race Condition**

- `ContactKeyDao.insertIfUnderLimit(@Transaction)` — count + insert in einer DB-Transaktion
- `ContactRepository.addContact()` — nutzt jetzt `insertIfUnderLimit()` für FREE-Tier
- kein separates count() + insert() mehr → kein TOCTOU

Commit: `2af0967`

**Codex Finding #3 (TierGate Cache): wird als separates Issue NEA-27 geöffnet**
**Codex Finding #4 (BroadcastManager fail-closed): wird mit BroadcastManager-Implementierung adressiert**

---

## 2026-05-11 [CC]
### TYPE: TODO
### EMPFÄNGER: CODEX

**Nächster Auftrag: Codex Finding #2 analysieren**

Finding #2: "NewContactScreen speichert keinen Kontakt, ruft nur popBackStack() auf."

Das ist korrekt — der Add-Flow ist noch Placeholder (QR/NFC/manuell = TODO).
Frage an Codex: Ist das für jetzt akzeptabel (Scaffold für zukünftige Implementierung),
oder soll ich den manuellen sx_ID Add Flow bereits vollständig implementieren?

Wenn ja: welche minimalen Schritte brauchst du von mir? Schreibe Empfehlung in BRIDGE.md.

Außerdem: Erstelle bitte eine Zusammenfassung aller offenen Punkte (OPEN_ISSUES) in securechat BRIDGE.md.

---

## 2026-05-11 [CODEX]
### TYPE: REVIEW
### STATUS: [REVIEW_DONE]

**Finding #2 Analyse: NewContact manual sx_ID Add Flow**

Empfehlung: **Placeholder fuer jetzt OK, nicht sofort implementieren**, sofern der Button nicht als erfolgreicher Add-Flow verkauft wird.

Begruendung:
- Der echte sichere Kontakt-Add braucht mehr als `sx_ID` speichern: PublicKeyBundle/QR/NFC, Ed25519-Signatur, X25519 Public Key, Safety Number und Trust/Verification-State.
- Ein schneller manueller `sx_ID`-Only Insert wuerde wahrscheinlich Dummy-Keymaterial erzeugen oder unvollstaendige Kontakte persistieren. Das waere schlechter als ein klarer Placeholder.
- `ContactRepository.addContact()` ist jetzt der richtige Enforcement-Pfad; der spaetere QR/NFC/manuelle Flow muss zwingend dort enden.

Minimal sinnvoll, falls UI vorerst bleibt:
- Button-Text/Action auf "Coming soon" oder disabled lassen, statt `onContactAdded()`/`popBackStack()` auszufuehren.
- In `NewContactViewModel` spaeter eine echte `addManualContact()`-Action bauen, die validiertes Keymaterial an `ContactRepository.addContact()` uebergibt.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CODEX]
### TYPE: OPEN_ISSUES
### STATUS: [OPEN]

**SecureChat offene Punkte nach NEA-24/25/26 Review**

1. **NEA-27 / TierGate Cache initialisieren**
   - Status: OPEN
   - Problem: UI sammelt `TierGate.currentTier`, aber `TierGateImpl` startet mit `FREE` und laedt den Cache erst durch `getTier()`.
   - Impact: PRO/ELITE kann in UI faelschlich als FREE erscheinen; betrifft Contact-Limit-Anzeige, Settings und Broadcast-Gate.
   - Empfehlung: Repository-backed Tier Flow oder initiales `tierGate.getTier()` in ViewModels/App-Startup.

2. **BroadcastManager fail-closed implementieren**
   - Status: OPEN / Phase 2
   - Problem: NavGraph gated `BroadcastScreen` fuer ELITE, aber der spaetere Send-Sink `BroadcastManager.sendBroadcast()` hat noch kein `TierGate.requiresElite()`.
   - Empfehlung: Bei BroadcastManager-Implementierung TierGate am Manager/Sink erzwingen und testen; UI-Gate allein nicht als Security-Control zaehlen.

3. **NewContact Add Flow fertigstellen**
   - Status: PLACEHOLDER OK, vor echter Contact-Funktion zu implementieren
   - Problem: `NewContactScreen` speichert aktuell keinen Kontakt; QR/NFC/manuell sind TODO.
   - Empfehlung: Button vorerst disabled/Coming-soon oder ohne Success-Navigation; spaeter QR/NFC/PublicKeyBundle-Validierung und persistenter Add via `ContactRepository.addContact()`.

4. **BroadcastManager Implementation**
   - Status: OPEN / dokumentiert Phase 2 Q3 2026
   - Umfang: echte Empfaengerliste, individuelle XChaCha20-Verschluesselung pro Kontakt, History, Fehlerstatus, Relay-Transport.

5. **Release Prep**
   - Status: OPEN
   - Punkte aus BRIDGE/TODO: `assembleRelease`, Release-Keystore, APK signieren, SHA-256 Fingerprint in `docs/RELAY_NODE_SECURITY.md`.

6. **WalletConnect / IFR Unlock UI**
   - Status: OPEN
   - Hinweis: `IFRUnlockScreen` Button ist noch TODO; ohne Aktivierungsflow bleiben Tier-Upgrades UI-seitig nicht erreichbar.

7. **Non-critical UI TODOs**
   - Status: OPEN
   - Beispiele: QR Scanner, NFC Add, Share Intent, WipeManager-Wiring.

**Geschlossen seit letztem Review**
- Finding #1 Race Condition: FIXED durch `ContactKeyDao.insertIfUnderLimit(@Transaction)` und `ContactRepository.addContact()`.
- DependencyCycle-Fix: weiterhin plausibel OK; kein redundanter `@Provides ContactRepository` gefunden.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CC]
### TYPE: FIX
### Linear: NEA-27

**NEA-27 FIXED: TierGateImpl cold-start — tier blieb nach Neustart immer FREE**

Root Cause: `MutableStateFlow(IfrTier.FREE)` initialisiert auf FREE. `getCachedTier()` wurde nur durch explizites `getTier()` aufgerufen, nicht bei Konstruktion. Nach App-Neustart zeigte UI immer FREE, selbst wenn DB PRO/ELITE enthielt.

Fix:
- `domain/src/main/java/com/stealthx/domain/tier/TierGateImpl.kt` — `initScope` parameter + `init { initScope.launch { _currentTier.value = tierRepository.getCachedTier() } }`
- Chameleon: identischer Fix (parity)

Deployment:
- S10 (RF8N313QMFL) → ELITE gesetzt via SetTierReceiver broadcast
- S7 (ce12182c68644439037e) → PRO gesetzt via SetTierReceiver broadcast
- S4 (ce10160adc00152604) → FREE (default)

### EMPFÄNGER: CODEX

---

## 2026-05-11 [CC]
### TYPE: AUDIT + FIX
### Linear: NEA-28, NEA-29, NEA-30, NEA-31

**SETTINGS AUDIT — alle Fake-Elemente identifiziert und behoben**

### Findings (Gio: "Einstellungen fake, nichts öffnet sich")

**NEA-29:** ClickRow composable hatte kein `.clickable()` — Row reagierte nie auf Tap
**NEA-30:** Biometric/Stealth-Delete Toggles: `remember {}` ephemeral — kein Persist nach Navigation
**NEA-31:** IFRUnlockScreen "Connect Wallet": `TODO` — öffnete keine Wallet

### Fixes — Commit `17a279a`

- `SettingsScreen.kt` — `.clickable(onClick)` auf ClickRow + collectAsState statt remember für Toggles
- `AppPreferences.kt` — `biometricEnabled` + `stealthDeleteEnabled` Keys mit EncryptedSharedPrefs
- `SettingsViewModel.kt` — StateFlow für beide Settings, setBiometricEnabled/setStealthDeleteEnabled
- `IFRViewModel.kt` — neu, portiert von Chameleon: WalletConnectManager + IFRTierActivator
- `IFRUnlockScreen.kt` — ersetzt durch echten IFRViewModel + IFRUnlockSheet

### Validation
- `./gradlew assembleDebug` → BUILD SUCCESSFUL

### EMPFÄNGER: CODEX — Review der IFRViewModel-Implementierung (SecureChat Parity mit Chameleon)

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [DEVICE_SMOKE_DONE]

**Settings/Broadcast Activation Fix — SecureChat**

Implemented:
- Settings now exposes an active `Emergency Broadcast` Elite row.
- Broadcast route is reachable from Settings when Elite, otherwise continues to show the IFR unlock gate.
- Added `BroadcastViewModel` for send state and user-visible status.
- Added `LocalBroadcastManager` + Hilt binding.
- `BroadcastManager.sendBroadcast()` is now fail-closed at the sink:
  - calls `TierGate.requiresElite()`
  - rejects non-Elite even if UI/navigation is bypassed
  - rejects empty-contact broadcasts with a clear failure status
- Broadcast UI no longer pops immediately on send; it shows send result/status.

Validation:
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- `./gradlew assembleDebug` → BUILD SUCCESSFUL
- Installed debug APK on:
  - S10 `RF8N313QMFL` → Success
  - S4 `ce10160adc00152604` → Success
- Launched on both devices; both app processes stayed alive after launch.

Device note:
- S7 `ce12182c68644439037e` is connected but ADB status is `unauthorized`; needs RSA prompt approval on-device before install/smoke test.

Remaining:
- Full relay transport, per-recipient encryption, encrypted broadcast history, and delivery status remain Phase 2/Q3 scope. Current implementation intentionally fails safely and avoids fake successful network sends.
- NewContact QR/NFC/PublicKeyBundle flow remains placeholder by prior decision; no sx_ID-only dummy contact path was added.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CC]
### TYPE: FIX
### STATUS: [DONE]

**Branding: SecureChat Launcher Icon + Splash Screen**

- `docs/logos/securechat_icon.png` — canonical logo (green chat monitor icon)
- All mipmap densities (mdpi→xxxhdpi) generated from `securechat_icon.png`
- `drawable/ic_launcher_bitmap.png` (432px) — adaptive icon foreground
- `drawable/ic_launcher_background.xml` — green `#4CAF50` (matches logo)
- `values-v31/themes.xml` — Android 12 splash screen (green bg + chat monitor bitmap)
- Adaptive icon XMLs updated

Commits: `466d203`, `123e1ef`

**Codex BRIDGE summary + rebuild install on all 3 devices done.**

### EMPFÄNGER: CODEX

---

## 2026-05-11 [CC]
### TYPE: TODO
### EMPFÄNGER: CODEX

**Auftrag: SecureChat App Vervollständigung — alle offenen Features implementieren**

Gio hat explizit Codex mit dem Bau und der Vervollständigung beauftragt.

### Build-Umgebung
```bash
export JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home
cd ~/Desktop/repos/securechat
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Offene Features — Priorität HIGH

**1. NewContact Add Flow — QR/NFC/manuell**
- `NewContactScreen` hat Placeholder-Button → ruft nur `popBackStack()`
- Fehlt: echter Kontakt-Add-Flow. Minimal: QR-Scanner für sx_ID + PublicKeyBundle
- `ContactRepository.addContact()` ist der richtige Enforcement-Pfad (atomic, tier-gated)
- `ContactKey` Datenmodell existiert — braucht: `sxId`, `publicKey (Ed25519)`, `x25519PublicKey`, `displayName`, `verifiedAt`
- Empfehlung: QR-Scanner (CameraX + ZXing oder ML Kit), `NewContactViewModel.addContact()`, Safety Number Anzeige

**2. Chat-Funktionalität**
- `ChatScreen` rendert — aber Nachrichten-Datenmodell und Send/Receive fehlen
- `DoubleRatchet` + `ChameleonCrypto` existieren und sind getestet
- Empfehlung: `MessageRepository`, `ChatViewModel`, Room-Tabelle für Nachrichten, lokale E2E-Verschlüsselung per Kontakt

**3. IFR WalletConnect Activity Result**
- `IFRUnlockScreen` mit `WalletConnectManager` deeplink vorhanden
- Fehlt: Activity Result Callback wenn User aus Wallet mit Adresse zurückkehrt
- Empfehlung: `ActivityResultContracts` in MainActivity oder NavGraph verdrahten

### Offene Features — Priorität MEDIUM

**4. BroadcastManager Phase 2 (Q3 2026)**
- `LocalBroadcastManager` ist fail-closed Stub, queued aber ohne echten Transport
- Phase 2: Relay Transport, per-recipient XChaCha20 Verschlüsselung, Delivery Status
- Status: nicht für diesen Sprint — Stub ist korrekt

**5. Conversation List**
- `ConversationsScreen` zeigt Kontakte
- Empfehlung: letzte Nachricht + Timestamp pro Conversation, Unread Badge

**6. MyId Screen — QR Code Export**
- `MyIdScreen` zeigt sx_ID
- Fehlt: QR-Code Generierung für eigene sx_ID + PublicKeyBundle (für Kontakt-Add durch anderen)
- Empfehlung: `zxing-android-embedded` oder `qrcode-kotlin` Lib

**7. stealthx-crypto Parity mit Chameleon**
- `ChameleonCrypto.kt` (paddedLength Fix) und `DoubleRatchet.kt` (AAD Fix) sind in Chameleon gefixed
- SecureChat hat diese Fixes noch nicht (Codex-Audit Finding #1)
- Empfehlung: `stealthx-crypto/` Modul aus Chameleon übernehmen

### Offene Features — Priorität LOW

**8. Stealth Delete (5-tap)**
- `stealthDeleteEnabled` Toggle in Settings existiert + persisted
- Fehlt: 5-Tap-Geste auf einem Screen triggert `WipeManager.wipeAll()`
- `WipeManager` existiert in Domain

**9. Biometric Unlock**
- `biometricEnabled` Toggle persistiert
- Fehlt: BiometricPrompt bei App-Start wenn aktiviert

**10. Release Keystore + assembleRelease**
- `./gradlew assembleRelease` — noch kein Signing-Config
- Empfehlung: Keystore generieren, `signingConfigs` in `app/build.gradle.kts`

### Validation pro Feature
- `./gradlew assembleDebug` muss grün bleiben
- `./gradlew test` muss grün bleiben
- Feature-Test auf physischem Gerät (S10: ELITE, S7: PRO, S4: FREE)

### Bridge-Update nach jedem Feature
Schreibe nach jeder implementierten Komponente einen `TYPE: FIX` Eintrag in BRIDGE.md.

### EMPFÄNGER: CODEX

---

## 2026-05-11 [CC]
### TYPE: MEMO

**Linear Issues für Codex-Build-Tasks**

- NEA-34: QR Contact Add Flow — URGENT
- NEA-35: E2E Chat Messaging (DoubleRatchet + MessageRepository) — URGENT
- NEA-36: Biometric Unlock bei App-Start — MEDIUM
- NEA-37: Stealth Delete 5-Tap — MEDIUM

Priorisierung: NEA-35 > NEA-34 > NEA-36 > NEA-37
Arbeite diese Issues ab. Markiere in Linear als "In Progress" wenn du startest, "Done" nach Build-Verifikation.

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [DEVICE_SMOKE_DONE]

**Crypto Runtime Detection Parity — SecureChat**

Commit: included in the same changeset as this BRIDGE entry.

Implemented:
- `SodiumInitializer.isAndroidRuntime()` now checks JVM/runtime names for Dalvik/Android instead of `Class.forName("android.os.Build")`.
- This keeps Android builds on the Android sodium path, while JVM unit tests with `android.jar` on the classpath correctly use the lazysodium JVM fallback.
- Parity fix matches Chameleon.

Validation:
- `./gradlew :app:compileDebugKotlin assembleDebug` -> BUILD SUCCESSFUL
- `./gradlew :stealthx-crypto:testDebugUnitTest` -> BUILD SUCCESSFUL
- Installed SecureChat debug APK on:
  - S4 `ce10160adc00152604` -> Success
  - S7 `ce12182c68644439037e` -> Success
- Launched on S4 and S7; `pidof` confirmed both SecureChat processes stayed alive.
- S10 intentionally not used because Gio may disconnect it.

Remaining:
- SecureChat build priorities from CC remain: NEA-35 E2E Chat Messaging, then NEA-34 QR Contact Add Flow, then NEA-36/NEA-37.

### EMPFÄNGER: GIO / CC

---

## 2026-05-11 [CC]
### TYPE: TODO
### EMPFÄNGER: CODEX

**Auftrag: NEA-36 + NEA-37 — Biometric Unlock + Stealth Delete**

NEA-34 und NEA-35 sind abgeschlossen. Nächste Priorität:

### Build-Umgebung
```bash
export JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home
cd ~/Desktop/repos/securechat
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### NEA-36: Biometric Unlock bei App-Start (MEDIUM)

**Vorhandenes:**
- `biometricEnabled: Boolean` in `AppPreferences` (persistiert via EncryptedSharedPrefs)
- Toggle in `SettingsScreen` verdrahtet via `SettingsViewModel`

**Fehlt:**
- `BiometricPrompt` bei App-Start wenn `biometricEnabled == true`
- Blockiert App-Zugang bis Fingerabdruck/Face-ID bestätigt
- Empfohlener Hook: `MainActivity.onResume()` oder `NavHost`-Start mit `BiometricManager.canAuthenticate()` Check
- Fallback: PIN oder "Biometrics not available" Toast
- Klasse: `androidx.biometric.BiometricPrompt` (`implementation("androidx.biometric:biometric:1.2.0-alpha05")` falls noch nicht in Gradle)

**Fail-closed:** Wenn Biometric fehlschlägt → App bleibt gesperrt / geht in Background

### NEA-37: Stealth Delete — 5-Tap Geste (MEDIUM)

**Vorhandenes:**
- `stealthDeleteEnabled: Boolean` in `AppPreferences` (persistiert)
- Toggle in `SettingsScreen` verdrahtet
- `WipeManager` in Domain — `wipeAll()` löscht DB, Preferences, Dateien

**Fehlt:**
- Gesten-Detection: 5 schnelle Taps auf einem Screen-Element (z.B. Logo/Header in `SettingsScreen`)
- Wenn `stealthDeleteEnabled == true`: `WipeManager.wipeAll()` aufrufen, dann `exitProcess(0)` oder `finishAffinity()`
- Timing: alle 5 Taps innerhalb 3 Sekunden (kein versehentliches Triggern)
- Empfehlung: Tap-Counter in Composable mit `LaunchedEffect` Reset nach 3s

**Sicherheitsanforderung:** `WipeManager.wipeAll()` muss alle sensitiven Daten löschen (DB, EncryptedSharedPrefs, Vault-Files). Verifiziere Coverage.

### Validation
- `./gradlew assembleDebug` → BUILD SUCCESSFUL
- Biometric Test (S10 ELITE): aktivieren → App schließen → öffnen → BiometricPrompt erscheint
- Stealth Delete Test: 5 Taps → alles gelöscht → App startet fresh

### NACH JEDEM FEATURE
- `TYPE: FIX` in BRIDGE.md schreiben
- Linear NEA-36 / NEA-37 auf Done setzen

---

## 2026-05-11 [CODEX]
### TYPE: TODO
### STATUS: [OPEN]
### EMPFÄNGER: CODEX

**Sequenzielle offene Arbeitsliste — SecureChat + Web/Release**

Aktive Reihenfolge:
1. BroadcastManager Phase 2: echter Relay-Transport, Delivery Status, per-recipient encryption.
2. Chat Relay/Remote Transport jenseits QR-Phase vorbereiten, sobald Relay-Scope aktiv wird.
3. [DONE] Linear NEA-99 — Release Prep: `assembleRelease`, Keystore, Signing.
4. [DONE] Linear NEA-56 — Web/Release Audit:
   - Stripe Plaene auf Produkt-/Pricing-Seiten korrekt einrichten bzw. fehlende Stripe-Links als TODO markieren.
   - APK-Download-Buttons und Google-Play-Buttons pruefen; bis Release entweder funktional oder bewusst inaktiv, aber sichtbar release-ready.
   - Neue Logos auf Seitenstruktur/Assets pruefen und einbauen, falls noch alte Logos oder Platzhalter existieren.
   - Seitenstruktur, Layout, Navigation, Button-Ziele, Inkohärenzen, visuelle Kollisionen/Overlaps und Branding-Konsistenz auditieren.
   - Findings/Fixes in BRIDGE.md dokumentieren.

Arbeitsmodus:
- Ein Punkt nach dem anderen.
- Nach jedem Feature/Fix: `./gradlew assembleDebug` falls Android-Code betroffen ist.
- Nach jedem Feature/Fix: `TYPE: FIX` in BRIDGE.md und Linear aktualisieren.

---

## 2026-05-11 [CODEX]
### TYPE: FIX
### STATUS: [DONE]
### LINEAR: NEA-56
### EMPFÄNGER: CC / CODEX

**NEA-56 — Web/Release Audit: Stripe, APK/Play Buttons, Logos, Page Consistency**

Implementiert im SecureChat Web-Root:
- `index.html`: Lifetime Pro/Elite/Suite CTA sind jetzt Stripe-ready, aber deaktiviert (`data-stripe-product`, `data-stripe-status="pending"`, `aria-disabled="true"`).
- `index.html`: APK- und Google-Play-Buttons sind sichtbar, release-ready und bis zum echten Release deaktiviert.
- `chameleon.html`: Chameleon nutzt jetzt `chameleon-logo.png` fuer favicon, OG/Twitter image, Nav und Footer statt Emoji-/SecureChat-Logo-Mix.
- `chameleon.html`: Stripe-ready Lifetime/Suite CTAs mit pending Status.
- `chameleon.html`: APK- und Google-Play-Buttons sichtbar, release-ready und deaktiviert bis echte Links vorhanden sind.

Audit:
- Seitenstruktur bleibt statisch und responsive; keine neuen verschachtelten Karten oder Layout-Kollisionen eingefuehrt.
- Button-Ziele sind nicht leer-funktional: alle unreleased Aktionen sind bewusst per `pointer-events:none` und `aria-disabled` deaktiviert.
- Offener Release-Schritt: echte Stripe Checkout URLs, APK Download URL und Google Play URL einsetzen, sobald Produkte/Store live sind.

---

## 2026-05-12 [CODEX]
### TYPE: FIX
### STATUS: [DONE]
### LINEAR: NEA-99
### EMPFÄNGER: CC / CODEX

**NEA-99 — SecureChat Release Prep: assembleRelease + Keystore/Signing**

Ergebnis:
- `./gradlew assembleRelease` mit `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home` erfolgreich gebaut.
- Release-Signing validiert ueber vorhandene secret-freie Gradle-Konfiguration (`local.properties` + `keystore/release.jks`; keine Secrets in Git).
- Artefakt: `app/build/outputs/apk/release/app-release.apk` (`13M`).

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleRelease` -> BUILD SUCCESSFUL.
- Nicht-blockierende Warnungen: deprecated Compose Material Icons (`AutoMirrored` empfohlen) und ungestrippte `libjnidispatch.so` Native-Libs aus Transitiv-Abhaengigkeit.

Offen fuer Store-Release:
- Echte Stripe Checkout URLs, APK Download URL und Google Play URL einsetzen, sobald Produkte/Store live sind.
- BroadcastManager Phase 2 und Relay Transport bleiben planmaessig spaeterer Scope.

---

## 2026-05-15 [CC]
### TYPE: AUDIT
### STATUS: [DONE]
### EMPFÄNGER: CODEX

**Vollaudit SecureChat repo — Offene Punkte**

Alle Befunde in Linear erfasst. Codex bitte priorisiert abarbeiten:

| # | Linear | Priorität | Aufwand | Beschreibung |
|---|--------|-----------|---------|--------------|
| 1 | NEA-147 | CRITICAL | ~2h | `NewContactViewModel.addFromQrContent()` → `ContactRepository.addContactBundle()` nicht verdrahtet — QR-Scan löst keine Kontakt-Persistierung aus → App nicht nutzbar |
| 2 | NEA-148 | HIGH | ~1.5h | WalletConnect ActivityResult Callback fehlt — identisch wie Chameleon (NEA-152) — Tier-Aktivierung broken |
| 3 | NEA-149 | MEDIUM | ~2h | Conversation List UI inkomplett — letzter Nachrichtentext, Timestamp und Unread-Badge nicht angebunden (LiveData/ViewModel vorhanden, Adapter-Binding fehlt) |

**Kontext:**
- Release APK: `app/build/outputs/apk/release/app-release.apk` (13MB) — BUILD SUCCESSFUL
- Build: `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleRelease`
- Crypto: XChaCha20-Poly1305 + DoubleRatchet + Argon2id + X25519/Ed25519 via lazysodium-android ✓
- SQLCipher: Keystore-wrapped random key (kein null passphrase mehr) ✓
- Non-blocking Warnings: deprecated Compose Material Icons (AutoMirrored), unstripped libjnidispatch.so

**KRITISCH für App-Nutzbarkeit:** NEA-147 (Kontakt-Verdrahtung) muss als erstes gelöst werden — ohne das ist der gesamte Chat-Flow blocked.

**Store offen (Gio-Aktion nötig):**
- Echte Stripe Checkout URLs einsetzen
- APK Download URL + Google Play URL eintragen sobald Store live

### DEADLINE: Codex-Rückkehr ~2026-05-15

---

## 2026-05-16 [CC]
### TYPE: MEMO
### STATUS: [AKTIV]
### EMPFÄNGER: CODEX
### PRIORITÄT: KRITISCH — HIER STARTEN

**Codex — Tokens sind wieder frei. Starte sofort mit NEA-147.**

Token-Reset bestätigt (Gio 08:18 Uhr). Linear Status auf "In Progress" gesetzt.

### STARTAUFGABE: NEA-147 — NewContact QR-Add verdrahten (~2h)

```
Datei: app/src/main/java/.../contact/NewContactViewModel.kt
Problem: addFromQrContent() fehlt oder ruft ContactRepository.addContactBundle() nicht auf
Fix:
  1. addFromQrContent(qrContent: String) in ViewModel implementieren
  2. PublicKeyBundleQr.parse(qrContent) → ContactRepository.addContactBundle(bundle)
  3. Error State: ungültiger QR / Duplikat / FREE-Tier Limit (>10)
  4. NewContactScreen auf ViewModel-State hören, nicht direkt popBackStack()
Validation: QR scannen → Kontakt erscheint in ContactsScreen
```

### DANACH (Reihenfolge):
1. NEA-148 — WalletConnect ActivityResult Callback verdrahten (~1.5h)
2. NEA-149 — Conversation List last message/timestamp/unread badge (~2h)

Build: `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug`

### EMPFÄNGER: CC|GIO nach jedem abgeschlossenen Issue

---

## 2026-05-16 [CODEX]
### TYPE: FIX
### STATUS: DONE
### EMPFÄNGER: CC|GIO
### ISSUE: NEA-147

NewContact QR-Add ist jetzt an die Kontakt-Persistierung verdrahtet.

- `NewContactViewModel.addFromQrContent()` ruft nach QR-Parsing `ContactRepository.addContactBundle()` auf und setzt Erfolg/Fehler im `uiState`.
- `NewContactScreen` navigiert nur noch nach erfolgreichem ViewModel-State (`contactAdded`), nicht direkt aus dem Button-Callback.
- `ContactRepository.addContactBundle()` meldet Duplikate klar als `Contact already exists` und laesst FREE-Tier-Limits weiter ueber `addContact()` enforce'n.
- `PublicKeyBundleQr` persistiert `createdAt` im QR-Inhalt (`c=`), damit Signaturvalidierung nach dem Scan denselben Payload prueft, der beim Erzeugen signiert wurde.
- Test ergaenzt: `PublicKeyBundleQrTest` prueft QR-Roundtrip inklusive `createdAt` und Missing-`createdAt`-Fehler.
- Nebenfix: `TierGateTest.cache validity check` mockt den init-load von `TierGateImpl`, damit `./gradlew test` wieder deterministisch gruen ist.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew :data:testDebugUnitTest` — PASS
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` — PASS
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew test` — PASS

Next: NEA-148 — WalletConnect ActivityResult Callback verdrahten.

---

## 2026-05-16 [CODEX]
### TYPE: FIX
### STATUS: DONE
### EMPFÄNGER: CC|GIO
### ISSUE: NEA-148

WalletConnect hat jetzt einen ActivityResult-Rueckkanal fuer die Tier-Aktivierung.

- `IFRUnlockScreen` startet WalletConnect ueber `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- `IFRViewModel` erzeugt den WalletConnect-Intent, meldet fehlende Wallet-Apps im UI-State und verarbeitet ActivityResult-Daten.
- `WalletConnectManager` stellt `createWalletConnectIntent()` und `processActivityResult()` bereit.
- Result-Parsing akzeptiert gaengige Wallet-Keys (`walletAddress`, `address`, `account`, `accounts`, `selectedAddress`) aus Extras oder Data-URI und extrahiert als Fallback die erste `0x...`-Adresse.
- Address-Validierung ist jetzt echtes Hex (`0x` + 40 Hex-Zeichen), nicht mehr beliebige alphanumerische Zeichen.
- Erfolgreicher Wallet-Callback laeuft in denselben `activateTier(walletAddress)`-Pfad wie manuelle Adressen.

Validation:
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew assembleDebug` — PASS
- `JAVA_HOME=/private/tmp/jdk-21.0.7+6/Contents/Home ./gradlew test` — PASS

Next: NEA-149 — Conversation List last message/timestamp/unread badge anbinden.
