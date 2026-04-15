# SecureChat — Entwicklungslogbuch

## Projekt-Info
- **Repo:** github.com/NeaBouli/securechat
- **App:** com.stealthx.securechat
- **Plattform:** Android 8.0+ (API 26+)
- **Sprache:** Kotlin + Jetpack Compose
- **Crypto:** lazysodium-android (XChaCha20, X25519, Double Ratchet)
- **Identity:** StealthXIdentity.kt (sx_ Format)
- **IFR:** Gleicher Stack wie Chameleon

## Architektur-Regeln (BINDEND)
1. Kein Crypto ausserhalb :stealthx-crypto
2. TierGate = einzige Tier-Checks
3. Feature Branches — nie direkt auf main
4. allowBackup="false"
5. Kein Debug-Log in Crypto/Security

## Schritte

| Step | Beschreibung | Status |
|------|--------------|--------|
| SC-00 | Projekt-Setup + Gradle Multi-Module | DONE |
| SC-01 | Crypto Layer (shared mit Chameleon) | DONE |
| SC-02 | Identity Layer (StealthXIdentity) | DONE |
| SC-03 | Local Messaging (Room + SQLCipher) | DONE |
| SC-04 | Key Exchange (QR + NFC) | ⏳ |
| SC-05 | IFR Token Integration | DONE |
| SC-06 | Presentation Layer (Compose) | ⏳ |
| SC-07 | Relay Transport (Phase 2) | ⏳ |
| SC-08 | Security Hardening | ⏳ |
| SC-09 | Release Prep | ⏳ |

## SC-00 (2026-04-16) — Projekt-Setup
- 12 Gradle Module aufgesetzt
- Shared Code von Chameleon kopiert (stealthx-crypto, stealthx-ifr, security, shared, data, domain)
- BUILD SUCCESSFUL — APK 42 MB
- Chameleon-kompatibel: gleicher Crypto-Stack

---

# SecureChat Landing — LOGBUCH

## 2026-04-15 — Diagnose securechat.stealthx.tech

### Checks durchgeführt

| # | Check | Ergebnis |
|---|-------|----------|
| 1 | DNS CNAME (`dig`) | ✅ `securechat.stealthx.tech` → `neabouli.github.io.` |
| 2 | DNS A Record (Google 8.8.8.8) | ✅ Resolves zu GitHub Pages IPs (185.199.108-111.153) |
| 3 | DNS lokal (`nslookup 127.0.2.2`) | ⏳ NXDOMAIN — lokaler Resolver noch nicht propagiert |
| 4 | GitHub Pages Status | ✅ `built`, cname gesetzt, source: `main /` |
| 5 | CNAME Datei | ✅ `securechat.stealthx.tech` |
| 6 | Repo Struktur | ✅ index.html, privacy.html, wiki/, sitemap.xml, robots.txt, llms.txt |
| 7 | Pages Build | ✅ Commit `6b3e780`, built 09:20 UTC, keine Fehler |
| 8 | HTTP via IP | ✅ `200 OK` — Seite wird ausgeliefert |
| 9 | HTTPS via IP | ✅ `200 OK` — SSL funktioniert |
| 10 | HTTPS Enforcement | ⏳ Zertifikat noch nicht in GitHub registriert — wird automatisch aktiviert |

### Befund

- **Seite ist LIVE und erreichbar** über beide Protokolle (HTTP + HTTPS)
- DNS CNAME korrekt gesetzt bei Cloudflare/Provider
- Lokale DNS-Propagation dauert noch (bis zu 30 Min normal)
- GitHub SSL-Zertifikat wird provisioniert — `https_enforced` kann danach per API aktiviert werden:
  ```bash
  gh api repos/NeaBouli/securechat/pages -X PUT \
    --input - <<< '{"cname":"securechat.stealthx.tech","https_enforced":true,"source":{"branch":"main","path":"/"}}'
  ```

### Status

| Komponente | Status |
|-----------|--------|
| `neabouli.github.io/securechat/` | ✅ Redirect → Custom Domain |
| `http://securechat.stealthx.tech` | ✅ 200 OK |
| `https://securechat.stealthx.tech` | ✅ 200 OK |
| HTTPS Enforcement | ⏳ Pending (Zertifikat-Provisionierung) |
