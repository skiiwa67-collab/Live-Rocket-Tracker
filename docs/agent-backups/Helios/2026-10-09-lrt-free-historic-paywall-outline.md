# Plan (research/outline only): free LRT with Historic behind a paywall — Alarm Pro pattern

Date: 2026-10-09 (CT). Author: Helios (Grok Bot executor).
Cross-links: GitHub https://github.com/skiiwa67-collab/Live-Rocket-Tracker/issues/81 · Notion https://app.notion.com/p/3f4a94933faa812f9db9d0c68dc52a55
Status: RESEARCH / OUTLINE ONLY. No app code changed, nothing touched in Play Console, nothing published or submitted. Chris alone publishes.
Sources read: Live-Rocket-Tracker `stamp-85` @ 4be9291 (versionCode 157 / 1.0.147), Alarm-Pro `main` @ 870b657, Notion pages cited below, Google Play Help / Android Developers docs cited below.

## 1. Alarm Pro pattern (as found)

**Model:** free app + ONE non-consumable Google Play in-app product. Not Stripe, not a subscription, and not a paid-to-free conversion (nothing found showing Alarm Pro was ever a paid app; the Notion product lock says "Free app unchanged until they buy Pro").

- Product id: `alarm_pro_unlock`, `BillingClient.ProductType.INAPP` — `Alarm-Pro/app/src/main/java/com/claudewidgets/alarmweather/ProBillingManager.kt` (PRODUCT_ID constant, `queryProduct()`).
- Library: `com.android.billingclient:billing:9.0.0` (`app/build.gradle.kts:46`), pending purchases enabled for one-time products. R8 keeps for billing in `app/proguard-rules.pro` (stamp 45, 2f453a6).
- Price: $1.99 one-time ("One IAP, $1.99" — Notion "Alarm Pro Play ship" https://app.notion.com/p/3cda94933faa8148a7f9f5916f785ae7; layout default "Unlock Pro - $1.99", replaced at runtime by Play's `formattedPrice`).
- Entitlement check: Play `queryPurchasesAsync(INAPP)` is source of truth; result cached in SharedPreferences `pro_access_cache/alarm_pro_unlocked` via `ProAccess.kt`. All feature code calls `ProAccess.isProUnlocked(context)` (widget provider, wallpaper tick service, alarm receiver, scheduler, settings).
- Restore/offline: cold-start + `onResume()` refresh (`WidgetConfigureActivity.kt:208-210`); empty results retried 3x with backoff (Razr returns empty first); transport failures never clear the cache (offline keeps last verified state). Purchases are acknowledged. Restore toast messages.
- Locked UI: Settings layout "FREE settings first; PRO cluster at bottom" (`activity_configure.xml`), Pro cards with gold "(Pro)" labels, Pro controls disabled (`updateProUiState`, `WidgetConfigureActivity.kt:187-205`), an "Unlock Pro for named presets" nudge button, toasts "Unlock Pro to save named presets."
- Free vs locked: free = alarm countdown widget, zip weather; Pro = animated widget backgrounds, font families, spoken alarm weather, named presets.
- Billing failures are non-fatal (`setUpProBilling()` try/catch).
- Stripe: Notion says explicitly "Play IAP is Google Play Billing, not Stripe. Stripe can wait." (Alarm Pro Play ship page; Tool Deficiencies https://app.notion.com/p/3cda94933faa811e82c5fe1ddce286b9 notes Stripe installed-not-signed-in). Stripe is NOT part of the pattern and would not be allowed for in-app digital unlocks on Play anyway (unverified here; Play's payments policy not re-fetched).
- Not found: license-tester list, Play Console promo usage, any Soft-PASS note specifically on the billing flow, any debug-unlock code in current `main` (Notion 1 Sep mentions "Debug Pro unlocked" on an early sideload; no such code in 870b657).

## 2. What "Historic" is in LRT code (stamp-85 @ 4be9291)

Paths relative to `app/src/main/java/com/ccos/retro/`.

- **Boundary:** `data/LaunchModels.kt:244` `WATCH_AFTER_NET_SEC = 60 min`; `isReplayable()` `:229-233` = demo OR (not active watch AND ≥60 min past NET). `PICKER_LOOKBACK_SEC = 48h` `:247` (recent past still in CURRENT picker; 9b2f855 "a finished flight inside 48h says Past and opens replay").
- **Entry point (UI):** HISTORIC tab button `ui/MainActivity.kt:136-141` (`telemetryListMode = "historical"`, AUTO off); layout `res/layout/activity_main.xml:365-381` (HISTORIC label/button), `:446-455` SEARCH HISTORIC box, `:479-490` historic data box. Chip styling `MainActivity.kt:509-515`, labels `:889`, `:923`.
- **Launch select / replay entry:** `MainActivity.kt:185-199` (historic tap enters replay) → `module/RocketTelemetryModule.kt:971 selectLaunch()` → `data/LaunchDataProvider.kt:341 rememberHistoricSelection()`.
- **Historic search:** `MainActivity.kt:322-374` (`historicBlob`, `matchesHistoric`, `fireHistoricSearchDebounced`, `isHistoricSearchUiActive`, `syncHistoricQueryFromBox`); `LaunchDataProvider.kt:189-219 setHistoricSearchInterest / searchScopedHistoricPool / requestHistoricSearch` (LL2 `launch/previous` ~1 yr, lines 248-249).
- **Historic pool / past data:** `LaunchDataProvider.kt:27-29` LL2 previous URLs, `:93 getPast()`, `:134 historicPool()` (real past newest-first ~20, demos fallback), `:86 demoCatalog`.
- **Replay / roll logic:** `RocketTelemetryModule.kt:379 isHistoricOrDemoPin`, `:389 hasSearchScopedHistoric`, `:396 leaveHistoricSearchForLiveAuto`, `:469 maybeAdvanceHistoricRoll` (HISTORIC + chip rolling), plus listMode branches at `:137,151,214,240,532,642,669,708-827`.
- **Time scrub:** CDT jump chips (`wallpaper/RetroCommandWallpaperService.kt:705-712`, "time scrub for demos / historical") → `RocketTelemetryModule.kt:174 jumpTo()`; `event/FlightEvents.kt:735 replayEndSec()`.
- **Event skip ±:** `RocketTelemetryModule.kt:186 skipEvent()` from wallpaper rocker (`RetroCommandWallpaperService.kt:732`, draw `:4686`) and Command Center prev/next (`ui/CommandCenterActivity.kt:116-123`). NOTE: skipEvent works on any tracked launch (comment "For testing"), not only historic — gating decision needed.
- **Mission tapes:** `data/MissionTapeStore.kt` (CDN tapes from repo `lrt-mission-tapes`, "published/observed historic event strings"), used for historic and live.
- **NOT Historic:** "hold-scrub" (`RocketTelemetryModule.kt:961 shouldPauseLiveForHoldScrub`, tip146) is LIVE launch Hold/Scrub pausing, not past-launch scrubbing.

## 3. Current LRT pricing model (as found)

- No billing code in LRT (`rg billing|BillingClient` in stamp-85: none; manifest has INTERNET, ACCESS_NETWORK_STATE, VIBRATE only).
- Notion 2026-09-24 "play-console-pricing-explore" (https://app.notion.com/p/3e5a94933faa81e19845c862a1790d66): "Keep LRT paid. Target price is about $1.99"; Console showed $9.99 (open mismatch); tip154 / 1.0.144 Production Active; "only about three installs". Current Console price NOT verified today.
- Earlier plan (1 Sep, Notion "LRT Play listing / money package" https://app.notion.com/p/3cea94933faa81a19fd6c2b2d7532944 and "Eve ↔ Elon team dump" https://app.notion.com/p/3cda94933faa813199e5c5ac2eb3957d): free install + own IAP; Pro = historic launches, YouTube PiP/VID, LCK, rumble/cues, Command Center; "Do not lock PAD Earth or the plates." That plan was never implemented ("No paywall in vc18") and was superseded by "keep paid" on 9-24. So this request reverses the 9-24 decision back toward the 1 Sep model, narrowed to Historic only.

## 4. Proposal — free vs locked (PROPOSAL, Chris decides)

FREE: live wallpaper HUD + eight plates; CURRENT tab, AUTO, upcoming list; live tracking through NET+60 min (and the 48 h "recent past" pick if Chris wants); LCK on live flights; PAD/Google Earth; catalog art; live event tape; Command Center on a live flight; single minimized YouTube pane on a live flight.
LOCKED ("LRT Historic", one-time unlock): HISTORIC tab + SEARCH HISTORIC; selecting/replaying any `isReplayable()` launch; HISTORIC rolling chip mode; CDT jump chips and event skip ± while on a replayable launch; demo catalog optional (could stay free for preview).
Open question for Chris: older 1 Sep plan also locked VID/LCK/Command Center/rumble — this outline keeps those free because the request says Historic only.

## 5. Work items + rough sizes (ESTIMATE)

1. Play Console (Chris only): create one-time product e.g. `lrt_historic_unlock` (id unverified/proposed), set price (~$1.99 per prior target — Chris decides), activate. S (Chris).
2. Add billing 9.x dependency + port `ProBillingManager`/`ProAccess` as `HistoricBilling`/`HistoricAccess`, R8 keeps. S.
3. Entitlement gate at entry points: HISTORIC button (`MainActivity.kt:136`), spinner select of replayable launch (`:194`), search box, `selectLaunch()` defense in module, roll/jump/skip on replayable launches, wallpaper-side guard (wallpaper reads prefs, so reset `telemetryListMode` to "current" if not entitled). M.
4. Locked-state UI: lock badge on HISTORIC chip, paywall sheet with price from Play, "Restore purchases" in Settings, Unlock button in settings like Alarm Pro. M (Da Vinci look).
5. Restore + offline grace: queryPurchases on start/onResume, retry empty, never clear cache on transport failure (copy Alarm Pro). S.
6. Promo-code redemption handling (In-app Promotions): purchases updated listener + onResume query handles codes redeemed in Play Store. S.
7. Remote kill/override (optional): e.g. flag in the existing CDN repo to unlock Historic for all if billing breaks. S–M; unverified need.
8. Grandfather existing paid buyers (see risks). S–M depending on approach.
9. Listing/Data safety/content: listing says "Contains in-app purchases" (auto from Play), description edits (free vs Historic), screenshots; Data safety likely unchanged (Play Billing handles payment; unverified). S.
10. Testing: license testers + internal/closed track; promo redemption scenarios per Google doc. M.
11. Flip app price Paid → Free in Console (Chris only, irreversible). XS but one-way.
Total rough: M–L (≈ 2–4 build stamps), ESTIMATE.

## 6. Risks / blockers

- **Paid → Free is permanent.** Google Play Help "Set up your app's prices" (https://support.google.com/googleplay/android-developer/answer/6334373): "You can change your app from paid to free. Once your app has been offered for free, the app can't be changed to paid. If you want to charge for the app, you need to create a new app with a new package name." A temporary $0 sale is the reversible alternative (https://support.google.com/googleplay/android-developer/answer/7271135; sales only for paid apps, 1–14 days, 30 days between sales).
- **Grandfathering existing buyers:** Play gives no built-in "bought the paid app" entitlement after the flip (my understanding, unverified). Options (unverified, pick one): (a) ship a paid-era update that writes a local "legacy owner" flag before flipping to free (lost on reinstall/new phone); (b) with ~3 installs (9-24 note), hand existing buyers a promo code for the Historic product; (c) Play Integrity licensing verdict — likely useless after the app is free. Recommend (a)+(b).
- Ordering: billing build must be live BEFORE flipping to free, or Historic is free for everyone in between.
- Separate product from Alarm Pro: Notion rule "Do not mix listings, IAP, branding, or testers."
- Pre-existing open bugs (#77/#78 New Shepard art, #79 event tape) are unrelated but visible to testers.
- Play Billing blocked on non-Play-signed builds except for license testers (Android docs) — sideload testing needs license tester accounts.

## 7. How it fits the promo-code tester plan

- Today (paid app): Play promo codes can give the whole paid app free. After flip: the app is free to install, so testers only need a code for the Historic one-time product.
- Google: "you can give users a free paid app, one-time product, or subscription with a promo code"; one-time product codes require integrating In-app Promotions; up to 500 non-subscription codes per quarter per app (any mix of paid-app and in-app codes); one-time-use codes downloaded as CSV; redeemable in Play Store or in-app; promotions last up to one year; codes can't be used for inactive products. Path: Monetize with Play > Promo codes > Create promo code. (https://support.google.com/googleplay/android-developer/answer/6321495)
- Alternatives: license testers (test payment methods, no real charge; can sideload debug builds with matching package) and closed testing tracks (https://developer.android.com/google/play/billing/test). Google says to test promo redemption before install, while app is foregrounded, and in multi-window.
- Sequencing: if Chris wants testers NOW (before any billing build), use paid-app promo codes on the current paid app. Codes created for the paid app won't carry over to an in-app product (a promotion's type can't be changed after creation — same doc).

## 8. Not found

- No Alarm Pro license-tester or promo-code records; no Alarm Pro billing Soft-PASS note.
- No evidence Alarm Pro was converted from paid to free (it appears free + IAP from the start).
- Current LRT Console price/paid status not checked today (no Console access by rule).
- No existing LRT issue for paywall/free/billing (searched before filing).
