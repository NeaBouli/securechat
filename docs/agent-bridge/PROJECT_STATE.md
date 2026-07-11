# Project State

## 2026-07-11 — SecureChat Payment-/Etimologio-Integration

- Repository Owner: Codex uebernimmt das gesamte oeffentliche SecureChat-Repository, nicht nur Payment.
- Andere Devs arbeiten nur nach Codex-Handover oder als Reviewer; private Payment-/Steuerdaten bleiben lokal/Runtime-only.
- SecureChat Pro und Elite sind als Waren im lokalen VLABS-Shop mit kanonischer Produktseite, serverkontrolliertem Preis und Privat-/Firmenauswahl inklusive AFM/VAT vorbereitet.
- SecureChat-Produktseiten enthalten lokal aktualisierte Preis-, Lizenz-, Digitalleistungs-, Widerrufs- und Rechtehinweise und verlinken die VLABS-Softwarebedingungen.
- Noch nicht release-ready: SecureChat braucht einen serverseitigen, signierten und Stripe-session-idempotenten Entitlement-Consumer sowie einen Revoke-Pfad fuer Vollrefunds.
- Bis zu diesem Consumer, Stripe-Test-E2E, Accountant Mapping und Gio-Freigabe bleibt der VLABS-Verkauf `Coming Soon`; kein Etimologio-Provider ist produktiv aktiv.
- Keine Secrets, Zahlung, Rechnung, Provider-/AADE-Anfrage oder Deployment bei dieser Bridge-Aktualisierung.
