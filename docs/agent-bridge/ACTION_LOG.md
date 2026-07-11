# Action Log

## 2026-07-11 — Codex SecureChat Fiat-Entitlement-Verifier

- Bestehender Activation-Code-Flow akzeptiert fuer Fiat-Tiers nicht mehr das unbewiesene Serverfeld `tier`, sondern verlangt ein Ed25519-signiertes Entitlement.
- Verifier bindet Token an Issuer, Audience `securechat`, lokale StealthX-Client-ID, SecureChat-Produkt, PRO/ELITE-Tier, Ausgabe/Ablauf und gehashte Order-Referenz. Manipulierte, kopierte, abgelaufene oder fremde Tokens failen geschlossen.
- Public Key kommt als nicht geheimes `STEALTHX_ENTITLEMENT_PUBLIC_KEY_BASE64` Build-Setting; ohne Key bleibt Fiat-Aktivierung geschlossen. Private Signing Keys liegen nie in App/Git.
- Verifiziertes Ablaufdatum wird bis in den bestehenden HMAC-geschuetzten Repository-/TierGate-Cache uebernommen; es entsteht kein zweiter Tier-Cache.
- Serververtrag liegt auf StealthX Branch `codex-payment-hardening-20260711`, Commit `0b4fa1b`; Activation-Response echoet keinen Code.
- Verifikation: Crypto- und Data-Unit-Tests PASS; Crypto/Data/Presentation Compile PASS; Gradle BUILD SUCCESSFUL. Kein Key, Checkout, Payment, Deploy oder externer Request.

## 2026-07-11 — Codex Payment-Ownership dokumentiert

- Rollen, Status und offene Payment-/Etimologio-Gates fuer SecureChat eingetragen.
- Keine Secrets oder Steuerdaten in die Public Bridge geschrieben.
- Keine Zahlung, Rechnung, Provider-/AADE-Anfrage oder Deployment ausgefuehrt.
- Aufgabenmatrix ergaenzt: Codex implementiert Payment/Entitlement/Etimologio; Core-Dev bleibt bei Produkt/Krypto und reviewt die Integrationsgrenze.
- Gio-Folgeentscheidung eingetragen: Codex uebernimmt das gesamte Public Repo; andere Devs nur nach Handover/als Reviewer.

## 2026-05-08
