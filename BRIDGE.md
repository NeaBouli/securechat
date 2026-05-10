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
