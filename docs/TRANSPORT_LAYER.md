# SecureChat Transport Layer — Phase 1/2/3

## Overview
The SecureChat transport layer is modular and phased.
Each phase adds stronger anonymity while maintaining
backward compatibility.

## Phase 1 — LocalTransport (v0.1.x — now)
- No network required
- Messages exchanged via QR code or NFC tap
- Suitable for high-threat environments (airgapped)
- Suitable for bootstrap before relay network exists
- **Available now**

## Phase 2 — TorRelayTransport (Q3 2026)
- Kaspa-incentivized relay nodes as Tor Hidden Services (.onion)
- Node registry on Kaspa BlockDAG via OP_RETURN transactions
- 2-hop onion routing (Pro tier)
- KAS micropayments to relay operators
- Requires: tor-android library integration, Kaspa SDK
- **Status: planned**

## Phase 3 — OnionRelayTransport (Q4 2026)
- 3-hop internal onion routing (Elite tier)
- Cover traffic (dummy packets) against Global Passive Adversary
- Pluggable Transports (obfs4/Snowflake) for censored regions
- Sybil protection via KAS-Deposit on relay nodes
- **Status: design phase**

## Transport Selection (MessageRouter)
Preference order (highest anonymity first):
1. ONION_RELAY (Phase 3) — if available
2. TOR_RELAY (Phase 2) — if available
3. LOCAL (Phase 1) — fallback always available

## User Experience
- Phase 1 users see: "Share via QR" workflow
- Phase 2 users see: automatic background delivery via Tor
- Phase 3 users see: identical to Phase 2, higher anonymity guarantee

## Backward Compatibility
A Phase 3 client can still receive messages from a Phase 1 client
via LOCAL (QR/NFC). No forced upgrade path.
