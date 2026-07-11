# Rollenverteilung CC + Codex

## Vollstaendige Codex-Ownership — verbindlich ab 2026-07-11

- Gio hat Codex als alleinigen Main Developer fuer das oeffentliche SecureChat-Repository eingesetzt.
- Codex uebernimmt Produktcode, Android, Messenger/Transport, Kryptografie-Integration, IFR, Tests, Releases-Vorbereitung sowie Stripe, Entitlements, Refunds und Etimologio/myDATA.
- Andere Devs arbeiten nur nach explizitem Bridge-Handover oder als unabhaengige Reviewer; keine parallele Implementierung.
- Secrets, AFM, Kunden-, Stripe-, Provider- und AADE-Daten bleiben ausserhalb dieses oeffentlichen Repositories in privaten Runtime-Secrets bzw. der privaten VLABS-Steuerzentrale.
- Live-Zahlung, Deployment und produktive Rechnungsausgabe bleiben Gio-/Accountant-/Provider-gated.

### Verbindliche Aufgabenaufteilung

**Codex uebernimmt:**
- VLABS/Stripe Checkout, Webhook, Preise/Produkt-IDs und Privat/Firma/AFM-Erfassung.
- Server-signiertes SecureChat-Entitlement, idempotente Auslieferung, Refund/Revoke und Payment-E2E-Tests.
- Integration des verifizierten Fiat-Entitlements in den bestehenden `TierGate`, ohne unsicheren lokalen Bypass.
- Invoice-/Etimologio-Drafts, Provider-Adapter, Payment-Rechtscopy und Bridge-Status.

**Andere Devs/CC uebernehmen nur nach Handover:**
- Von Codex konkret zugewiesene Teilaufgaben oder unabhaengige Reviews.
- Security-/Regression-Review der `TierGate`-Entitlement-Grenze nach Handover.
- Keine parallele Produkt-, Krypto-, Stripe-/Etimologio- oder Fiat-Entitlement-Implementierung.

**Gio/Accountant/Provider:** Runtime-Secrets und Launch/Deploy durch Gio; Steuer-Mapping und produktive Rechnungsausgabe durch Accountant/Provider.

Siehe COOPERATION_RULES.md und CLAUDE_CODE_README.md. Diese Datei kann projektspezifisch erweitert werden, sobald an diesem Projekt gearbeitet wird.

## Claude Code (CC) — Reviewer / Support nach Handover

- Implementiert nur von Codex explizit uebergebene Dateien/Teilaufgaben.
- Fuehrt unabhaengige Reviews und uebergebene Tests aus.
- Dokumentiert Review-/Fixberichte in CC_RESPONSE.md.
- Markiert eigene Fixes nicht als final verifiziert, wenn Codex-Recheck vorgesehen ist.

## Codex — Main Developer / Owner

- Implementiert und priorisiert das gesamte Repository, Tests und Releases-Vorbereitung.
- Auditiert und re-verifiziert.
- Dokumentiert Findings in CODEX_FINDINGS.md.
- Aktualisiert die Bridge nach relevanten Aufgaben.
