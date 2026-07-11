# TODO

## Payment / Etimologio — Owner Codex

**Zuweisung:** Alle Payment- und Nicht-Payment-Produktaufgaben dieses Public Repos sind Codex-Aufgaben. Andere Devs arbeiten nur nach Bridge-Handover oder als Reviewer.

- [x] SecureChat Pro/Elite als VLABS-Shopwaren mit Produktseiten und Rechtscopy vorbereiten.
- [x] Privat-/Firma-/AFM-Datenerfassung und internen Invoice-/Etimologio-Draft-Vertrag auf VLABS-Seite vorbereiten.
- [x] Signierten, idempotenten SecureChat-Entitlement-Consumer implementieren und testen.
- [x] Vollrefund-/Dispute-Revoke serverseitig und signierte Lease-Ablaufgrenze clientseitig implementieren; Partial Refund bleibt Operator-Review ohne automatisches Revoke.
- [ ] Automatische Lease-Erneuerung fuer nicht widerrufene Entitlements vor Ablauf implementieren; bis dahin kein Verkauf freischalten.
- [ ] Private Runtime-Secrets setzen und Stripe-Testmode E2E durchfuehren.
- [ ] Runtime-Ed25519-Public-Key in Release-Build setzen; Private Key bleibt ausschliesslich auf dem Signaling-Server.
- [ ] Accountant Mapping sowie Gio Launch-/Deployment-Freigabe; erst danach `Coming Soon` entfernen.
- [ ] Reviewer nach Handover: Security-/Regression-Review des Entitlement-Verifiers und der `TierGate`-Integration.
