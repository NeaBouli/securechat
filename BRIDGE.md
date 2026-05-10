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
