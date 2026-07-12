# Project State

## 2026-07-12 — Full Software Readiness Audit

- Android client, website, transport implementation, public claims and all paid-access entry points were reviewed together.
- Current online delivery is a central signaling relay, now represented by `SIGNALING_RELAY`; Tor/Onion and decentralized discovery remain roadmap and their placeholders fail closed.
- Contact-exchange buffering is bounded to 256 frames and failed drains preserve Double-Ratchet ordering.
- Unverified Google Play client callbacks cannot persist PRO/ELITE access. Purchase and restore controls remain launch-gated, with a repository check preventing reintroduction of direct local Play unlocks.
- Legacy website checkout calls are disabled; availability is routed through VLABS. Public privacy/product/F-Droid/LLM metadata no longer claims zero metadata, no server, or completed decentralization.
- Release `v0.1.5-alpha-securechat` APK was independently checked: valid v2 signature, package `securechat.app`, versionCode 6, versionName `0.1.5-alpha`; live TLS leaf and backup pins match the client configuration.
- Remaining external gates are runtime entitlement key configuration, signed cross-repo activation/revoke E2E, optional server-side Play/RTDN implementation, physical two-device tests, and controlled launch approval.
- Legal distribution gate: the source-available root license conflicts with many `GPL-3.0-or-later` source headers. F-Droid/store distribution must remain blocked until Gio/legal selects and applies one consistent licensing policy.
- The stale SecureChat F-Droid metadata was removed. Queue shutdown now shares the send monitor, and focused tests cover capacity, failed-drain ordering, clear, concurrency and stop synchronization.

## 2026-07-11 — Signierter Fiat-Entitlement-Consumer implementiert

- SecureChat akzeptiert bezahlte PRO/ELITE-Aktivierung nur nach Ed25519-Verifikation eines geraete-, produkt- und zeitgebundenen Server-Tokens.
- Verifizierte Fiat-Tiers laufen weiterhin ausschliesslich durch `AccessTierRepository` und `TierGate`; der signierte Ablauf begrenzt Offline-Zugriff nach Refund/Dispute.
- Server-Revoke verhindert Neuausstellung nach Vollrefund/Dispute. Teilrefund bleibt bewusst Review statt automatischer Rechteaenderung.
- Noch nicht sell ready: sichere automatische Lease-Erneuerung, Runtime-Keypair/Public-Key-Build, Stripe-Testmode E2E, Accountant/Provider und Gio-Launchfreigabe fehlen.

## 2026-07-11 — SecureChat Payment-/Etimologio-Integration

- Repository Owner: Codex uebernimmt das gesamte oeffentliche SecureChat-Repository, nicht nur Payment.
- Andere Devs arbeiten nur nach Codex-Handover oder als Reviewer; private Payment-/Steuerdaten bleiben lokal/Runtime-only.
- SecureChat Pro und Elite sind als Waren im lokalen VLABS-Shop mit kanonischer Produktseite, serverkontrolliertem Preis und Privat-/Firmenauswahl inklusive AFM/VAT vorbereitet.
- SecureChat-Produktseiten enthalten lokal aktualisierte Preis-, Lizenz-, Digitalleistungs-, Widerrufs- und Rechtehinweise und verlinken die VLABS-Softwarebedingungen.
- Noch nicht release-ready: SecureChat braucht einen serverseitigen, signierten und Stripe-session-idempotenten Entitlement-Consumer sowie einen Revoke-Pfad fuer Vollrefunds.
- Bis zu diesem Consumer, Stripe-Test-E2E, Accountant Mapping und Gio-Freigabe bleibt der VLABS-Verkauf `Coming Soon`; kein Etimologio-Provider ist produktiv aktiv.
- Keine Secrets, Zahlung, Rechnung, Provider-/AADE-Anfrage oder Deployment bei dieser Bridge-Aktualisierung.
