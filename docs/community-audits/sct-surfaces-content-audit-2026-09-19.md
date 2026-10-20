# SecureChat — Surfaces, Content & AI-Readiness Audit

- **Series:** Collateral Web3 Open Audits (sixth engagement: StealthX clients)
- **Date:** 2026-09-19 (recon 2026-09-15, resumed and verified 2026-09-19)
- **Target:** NeaBouli/securechat @ `9cebb36843e7b3d31baccd966ecb09bffa64dfa2` + live `securechat.stealthx.tech` (byte-identical to repo, verified)
- **Scope:** public site (index/faq/privacy/ifr/payment-success/wiki×10), README/LOGBUCH/ECOSYSTEM/docs, cross-app invite wiring with stealthx.tech, SEO/AI anchors, license/security-policy layer
- **Method:** shared-surfaces recon agent + lead verification (live probes, anonymous GET only)
- **Register:** SCT-15 … SCT-30 (this report) — **0 Critical / 1 High / 6 Medium / 8 Low / 1 Info**
- **Cross-refs:** STX-29/31 (stealthx.tech invite page + GA) are the platform-side halves of SCT-15; STX-38 pricing chaos extends here (SCT-17); STX-30-class policy issue here (SCT-20)

---

## Executive summary

The public site is dramatically cleaner than stealthx.tech: **no GA4, no trackers, no third-party scripts on any page**; IFR/payment surfaces fail-closed with an actual test; sitemap/robots/llms.txt present and mostly honest. The remaining damage comes from two directions: (1) the **invite flow** — the app builds `stealthx.tech/invite/?app=securechat&link=…` URLs carrying sxId + full public key bundle + optional handle into a page that loads Google Analytics and navigates unvalidated deep links (the tracker-free app feeds the tracked platform page); and (2) **claim drift** — a flat-out false "no central server" in the FAQ, a wiki architecture page selling the decentralized target design as current fact, and 70 GPL-3.0 file headers contradicting the proprietary LICENSE.

## Severity table

| ID | Severity | Title |
|----|----------|-------|
| SCT-15 | High | App invite URLs carry sxId + key bundle + handle into the GA4-tracked, unvalidated `?link=` page on stealthx.tech |
| SCT-16 | Medium | FAQ falsely claims "no central server" and Kaspa-tied identity (contradicts the same page's own JSON-LD) |
| SCT-17 | Medium | Site sells €9/€19 "LIMITED — 100 Licenses" with live VLABS links on an admittedly unsellable alpha (pricing chaos, third variant) |
| SCT-18 | Medium | Wiki `architecture.html` presents the decentralized relay design as current fact (no roadmap banner, indexed) |
| SCT-19 | Medium | FAQ/index/llms.txt describe Chameleon's disabled overlay as working (cross-product misrepresentation) |
| SCT-20 | Medium | SECURITY.md disclaims the shipped alpha ("future application", "unreleased", N/A) |
| SCT-21 | Medium | 70 Kotlin files carry GPL-3.0 SPDX headers vs the proprietary source-available LICENSE |
| SCT-22 | Low | FAQ loads Google Fonts (visitor IP leak; inconsistent with no-third-party posture) |
| SCT-23 | Low | ifrunit.tech image hotload on the homepage + `preconnect` to stealthx.tech |
| SCT-24 | Low | "Buy $IFR on Uniswap" CTA on a fail-closed product page |
| SCT-25 | Low | Wiki `ifr-unlock.html` describes the wallet flow in present tense while disabled |
| SCT-26 | Low | LOGBUCH drift incl. "Elite (>=6.000 IFR)" token-amount tier threshold the platform publicly denies |
| SCT-27 | Low | F-Droid remnants in docs despite removal under the source-available license |
| SCT-28 | Low | README drift: removed `stealthx-ifr/` module listed; stale github.io "live site" URL; `_config.yml` baseurl vs CNAME |
| SCT-29 | Low | llms.txt residuals: Chameleon integration in present tense; "same stack" but lazysodium 5.1.0 vs 5.2.0 |
| SCT-30 | Info | humans.txt "Identity: Kaspa BlockDAG" as fact; sitemap lastmod stale + lists noindex ifr.html; robots AI opt-in amplifies drift; closed-testing URLs in public BRIDGE |

---

## SCT-15 — High — Invite URLs feed the GA4-tracked, unvalidated platform page

**Evidence (lead-verified):** `data/.../identity/PublicKeyBundleQr.kt:36-40` builds `https://stealthx.tech/invite/?app=securechat&link=<urlencoded stealthx://add/sxId?x=…&e=…&s=…&c=…[&h=handle]>` — sxId, full public key bundle, and an optional user-chosen display handle in the URL. The target page (stealth repo, verified in the STX audit) loads GA4 (`website/invite.html:36-37`, property G-V2L60E8E7R) and navigates the `?link=` deep link without scheme/origin validation (STX-31). Neither the client site nor the wiki documents this flow (grep: no `invite` references on the site).

**Impact:** a SecureChat user tapping "share invite" sends their sxId + public keys + handle in a URL that gets page-tracked by Google Analytics — the exact STX-29 leak, initiated from the tracker-free client. The platform's cleanest app pushes its users onto its leakiest page.

**Recommendation:** host invites on a tracker-free page (or the client site); strip `&h=` from URLs; add the stealthx.tech-side whitelist fix (STX-31); document the flow.

## SCT-16 — Medium — FAQ falsely claims "no central server"

**Evidence (lead-verified):** `faq.html:319` — "Your identity is tied to Kaspa, a decentralized blockchain… **There is no central server.**" This contradicts the same page's own JSON-LD (`faq.html:30`: "uses a central signaling relay"), `index.html:7` meta description, `privacy.html:22,28` (relay + metadata disclosure), `privacy.html:32` (Kaspa anchoring "is not performed by the current release"), and `llms.txt:3`. Materially false architecture claim on a privacy product — and AI crawlers ingest both versions (robots.txt opts them in).

**Recommendation:** rewrite with the relay-plus-roadmap wording used everywhere else.

## SCT-17 — Medium — Scarcity pricing on an unsellable alpha

**Evidence (lead-verified):** `index.html:948-965` sells "Pro Lifetime **€9 (LIMITED — 100 Licenses), rises to max €14.99**" and "Elite **€19 → max €23.99**" with live links to `https://vlabs.gr/en/shop?focus=security` (`:959,973,982`), while the same page (`:985`) states "No product is available for paid activation until the VLABS checkout card shows it as available." Meanwhile both repos' `docs/PRICING.md` list Chameleon €9/€19→€14.99/€23.99 and SecureCall €15/€25→€50/€100, stealthx.tech shows €15/€25, and the SecureCall app UI says €49 (STX-38) — **three properties, three price stories**, and now a fourth surface with a third price set. Scarcity banners ("LIMITED", "Price rises with every sale") on a product whose own bridge says paid controls are closed.

**Recommendation:** single canonical price source (VLABS) linked, not restated; remove numeric prices from alpha pages or from the PRICING.md copies.

## SCT-18 — Medium — Wiki architecture page sells the decentralized design as current

**Evidence:** `wiki/architecture.html` — title/OG "SecureChat Architecture — 3-Layer **Decentralized** Design" (`:21`); body describes "Relay operators earn KAS micropayments", "Anyone can run a relay node" (`:108-110`), "2-hop onion routing hides both parties' IPs" (`:168`), "Messages are deleted immediately after delivery… permissionless network" (`:164-169`); dated "April 2026" (`:92`); **no roadmap disclaimer anywhere on the page** (unlike `kaspa-integration.html:93` and `user-manual.html`); indexed (`index,follow`) and in the sitemap.

**Impact:** search engines and AI ingest an architecture the same site's privacy policy says doesn't exist (central relay today).

**Recommendation:** add the kaspa-integration-style banner or retitle "Target architecture (roadmap)".

## SCT-19 — Medium — SCT surfaces describe Chameleon's disabled overlay as working

**Evidence:** `faq.html:497` ("runs quietly in the background and encrypts text in any app — automatically. It uses Android's AccessibilityService…"), `:519` ("Chameleon handles overlay encryption across all apps… decoy mode based on your location"); `index.html` features card ("Automatic overlay encryption, location-based rules, decoy profiles"); `llms.txt:40` ("Integrates with SecureChat as overlay encryption layer" — present tense). Chameleon's own site/FAQ/manual say overlay and messenger are **disabled** in the alpha (chameleon/faq.html:35, wiki/user-manual.html:163-175).

**Recommendation:** uniform "planned/disabled" wording on SCT surfaces.

## SCT-20 — Medium — SECURITY.md disclaims the shipped alpha

**Evidence:** `SECURITY.md:8` describes the app as "**future** encrypted messaging application (Phase 1, Q2-Q3 2026)"; `:41` supported-versions table: "SecureChat Android app | **unreleased** | N/A". Reality: public APK v0.1.5 on GitHub releases, Play closed test v0.1.11 (`app/build.gradle.kts:38-39`), download button on the homepage. The shipped alpha has, by its own policy, no supported security channel — and the advisories link points at a repo whose policy disclaims the product.

**Recommendation:** add the alpha as supported-with-limits.

## SCT-21 — Medium — GPL-3.0 headers (70 files) vs proprietary LICENSE

**Evidence (lead-verified):** 70 Kotlin files in this repo carry `SPDX-License-Identifier: GPL-3.0-or-later` (e.g. `security/.../KeystoreManager.kt:4`, `data/.../PublicKeyBundleQr.kt:4`) while the root LICENSE forbids copy/build/run/distribute without written permission; the repo's own agent docs admit the conflict is unresolved (`docs/agent-bridge/PROJECT_STATE.md:12`). The README correctly says "source-available, not open source" — but file-level GPL grants vs repo-level prohibition is a legally contradictory grant that auditors, contributors and F-Droid/store reviewers cannot resolve. (Chameleon: 129 files, CHA-18.)

**Recommendation:** legal decision, then mechanical header/LICENSE reconciliation.

## SCT-22 — Low — FAQ loads Google Fonts

`faq.html:27-29` loads fonts from `fonts.googleapis.com`/`fonts.gstatic.com` (Schibsted + Hanken Grotesk) — the only third-party load on the site; visitor IP leak to Google on a privacy product's FAQ, no consent banner; index.html references the same families without loading them (inconsistent rendering). Self-host or drop.

## SCT-23 — Low — ifrunit.tech image hotload

`index.html:901,917` hotload `https://ifrunit.tech/assets/ifr_icon_256.png` (IP/referer leak to a third party on every homepage visit) + `<link rel="preconnect" href="https://stealthx.tech">` in the head. Vendor the icon locally.

## SCT-24 — Low — Uniswap CTA on a fail-closed product

`index.html:916` links `app.uniswap.org/explore/tokens/ethereum/0x77e99917…`; the contract also appears in `ifr.html:25` and `wiki/ifr-unlock.html:46`. Token purchase solicitation on a page whose own products can't be bought — combined with scarcity pricing (SCT-17), the pattern consumer-protection reviewers flag. Label as informational or gate behind the launch flag.

## SCT-25 — Low — Wiki ifr-unlock present tense

`wiki/ifr-unlock.html:39-42` — "**Current state**: The sales page connects MetaMask, Phantom, Coinbase Wallet…" while `index.html:842` has the flow disabled. Switch to "When launched…" wording matching ifr.html.

## SCT-26 — Low — LOGBUCH drift incl. hidden IFR tier threshold

`LOGBUCH.md:37` — "SC-10… Tier: Elite (>=6.000 IFR)" — a token-amount tier threshold contradicting "no token-amount tier threshold" on both sites, both PRICING.md, both ECOSYSTEM.md. Mark LOGBUCH as a historical snapshot or correct it; remove threshold numbers.

## SCT-27 — Low — F-Droid remnants

`docs/TODO.md:22-25` references SecureCall's fdroiddata MR !36495 (marked historical here — good), but planning references elsewhere persist; README.md:91 correctly says "not eligible under the current source-available license". Purge or banner-mark the remainder.

## SCT-28 — Low — README drift

`README.md:47` lists module `stealthx-ifr/` ("IFR web discount, AppSignature") — the module is now `:stealthx-access` (settings.gradle.kts), no such dir exists; `README.md:113` "Live site: neabouli.github.io/securechat/" — stale, canonical is the CNAME domain; `_config.yml:3-4` still has `url: neabouli.github.io` + `baseurl: /securechat` (inconsistent with the custom domain).

## SCT-29 — Low — llms.txt residuals

`llms.txt:40` Chameleon "Integrates with SecureChat as overlay encryption layer" (present tense; overlay disabled); `:44` "same stack as SecureChat" — chameleon pins lazysodium **5.1.0** (`chameleon/gradle/libs.versions.toml:9`) vs 5.2.0 here. Otherwise the best file on the site (relay + metadata disclosed, Kaspa/Tor marked roadmap, versions match the build).

## SCT-30 — Info — Anchor hygiene

`humans.txt:17` claims "Identity: Kaspa BlockDAG" as tech fact (roadmap in reality); sitemap covers all 14 public pages but `lastmod 2026-08-28` is stale (live Last-Modified 2026-09-04) and lists `ifr.html` which is `noindex`; robots.txt explicitly allows GPTBot/Google-Extended/anthropic-ai (deliberate opt-in — so SCT-16/18/19 propagate into AI answers); closed-testing URLs + package name in the public BRIDGE (low risk, unnecessary exposure).

---

## Verified strengths

- **No GA4/trackers/third-party scripts on any client page** (live-verified, 6 pages + full repo grep) — the cleanest surfaces in the StealthX platform (contrast STX-29).
- IFR surfaces fail-closed with a real test (`js/ifr-checkout.test.cjs` asserts zero handlers and zero fetch calls when gated).
- `payment-success.html` inert: no scripts, explicit "a checkout-return URL alone does not activate a tier".
- llms.txt mostly honest and version-accurate (SCT 0.1.5 public / 0.1.11 Play match `build.gradle.kts`).
- Wiki covers relay metadata honestly (`relay-architecture.html`); `kaspa-integration.html` carries the roadmap banner the architecture page lacks (SCT-18's fix pattern exists in-repo).
- LICENSE texts identical across the two client repos except product name; both READMEs correctly say "source-available, not open source".
