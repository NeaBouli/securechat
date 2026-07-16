# TODO

## Payment / Etimologio — Owner Codex

**Zuweisung:** Alle Payment- und Nicht-Payment-Produktaufgaben dieses Public Repos sind Codex-Aufgaben. Andere Devs arbeiten nur nach Bridge-Handover oder als Reviewer.

- [x] SecureChat Pro/Elite als VLABS-Shopwaren mit Produktseiten und Rechtscopy vorbereiten.
- [x] Privat-/Firma-/AFM-Datenerfassung und internen Invoice-/Etimologio-Draft-Vertrag auf VLABS-Seite vorbereiten.
- [x] Signierten, idempotenten SecureChat-Entitlement-Consumer implementieren und testen.
- [x] Vollrefund-/Dispute-Revoke serverseitig und signierte Lease-Ablaufgrenze clientseitig implementieren; Partial Refund bleibt Operator-Review ohne automatisches Revoke.
- [x] Automatische Lease-Erneuerung fuer nicht widerrufene Entitlements vor Ablauf implementieren.
- [x] Direkte lokale Freischaltung durch Google-Play-Clientcallbacks entfernen und per Build-Guard absichern.
- [x] Zentralen Signaling-Relay korrekt modellieren, geplante Tor/Onion-Pfade fail-closed setzen und oeffentliche Metadaten-/Server-Claims korrigieren.
- [x] Kontakt-Frame-Queue begrenzen und Reihenfolge bei fehlgeschlagenem Drain erhalten.
- [x] Legacy-Webcheckout deaktivieren; Kaufverfuegbarkeit bleibt bis zum E2E-Gate bei VLABS.
- [ ] Private Runtime-Secrets setzen und Stripe-Testmode E2E durchfuehren.
- [ ] Runtime-Ed25519-Public-Key in Release-Build setzen; Private Key bleibt ausschliesslich auf dem Signaling-Server.
- [ ] Signierte Aktivierung, automatische Erneuerung und Vollrefund-/Dispute-Revoke cross-repository im Testmodus pruefen.
- [x] Kanonischen Node-Signer-Token als echte Cross-Repo-Kompatibilitaetsregression im Kotlin-Verifier pruefen.
- [x] Produkt und Tier im Kotlin-Verifier auf die kanonischen SecureChat-Paare fest binden.
- [ ] Android-Client auf zwei physischen Geraeten pruefen: Erstkontakt, Versand, Reconnect, Hintergrundzustellung, Prozessneustart und Offline-/Relay-Ausfall.
- [ ] Google Play nur aktivieren, wenn serverseitige Purchase-Token-Verifikation und RTDN-Revoke E2E bereitstehen; andernfalls dauerhaft deaktiviert lassen.
- [ ] Lizenzentscheidung durch Gio/Legal: Root-`LICENSE` ist source-available und verbietet Build/Distribution, zahlreiche Quelldateien deklarieren dagegen `GPL-3.0-or-later`; vor Store-/F-Droid-Vertrieb konsistent aufloesen.
- [ ] Accountant Mapping sowie Gio Launch-/Deployment-Freigabe; erst danach `Coming Soon` entfernen.
- [ ] Reviewer nach Handover: Security-/Regression-Review des Entitlement-Verifiers und der `TierGate`-Integration.
