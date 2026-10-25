# NOORUP Store Listing Audit

> Source of truth: codebase inspection on 2026-10-14 (v1.2.0, versionCode 6).
> Reference PDFs: `Store_Listing_Checklist_Final.pdf` (15-point design checklist) and `ReelBlink-100-Plus-Downloads-Guide.pdf` (one developer's experience, not a guarantee).
> Labels used below: **[PDF]** = directly supported by a reference PDF. **[Analysis]** = my own inference from the NoorUp codebase — verify before publishing.

## 1. What NoorUp actually does (corrected 2026-10-14 — evidence-graded)

Evidence grades: **C** = confirmed by implementation + data inspected this session. **T** = covered by an automated test (must still be run). **M** = requires manual verification. **U** = unverified — do not claim.

Package `com.aistudio.noorup.qzmxlw`, Min SDK 26, Target SDK 36. Kotlin + Compose Material 3.

- Home prayer dashboard **(C)**: 5 prayers + Sunrise + Tahajjud, current/passed state, countdown, start–end times, Sehri-ends / Iftar-starts bar, date picker + solar calibration card (8 calculation methods + Hanafi/Shafi Asr), Travel Qasr mode card, Noor Garden streak card. Refs: `SolarPrayerEngine.kt`, `NoorUpModels.kt:157-177`, `NoorUpViewModel.kt:66-72,179-186`.
- Quran **(C data + T test, with corrections)**: 114 `surah_*.json` asset files present on disk plus `_manifest.json` (`{"surahs":114,"totalVerses":6236}`). Spot-check `surah_002.json` (363 KB) shows 286 verses with real Arabic + Bangla + English — NOT placeholders. Smallest file `surah_112.json` (924 B) is consistent with 4 short verses. **Correction to v1 of this doc:** the "only Fatihah & Ikhlas fully curated" line described the in-code fallback list (`QuranRepository.all114Surahs` embedded sample verses), not the bundled assets. Full text loads on demand via `QuranRepository.loadFullSurah()` + `NoorUpViewModel.openSurah()` (`NoorUpViewModel.kt:606-638`). Caveats: (a) `QuranOfflineCompletenessTest` (114 files, gap-free 1..total, 3-language non-blank, 6236 total) exists but was **not executed this session — M**; run `./gradlew test` before claiming "complete". (b) Translation source/edition (which Bangla/English translator, Uthmani text source) is **U** — no attribution found in repo; do not name a translator. (c) UX risk **(C)**: before a surah is opened, `_surahs` is seeded by `buildCompleteVerseList()` (`NoorUpViewModel.kt:558-584`) which injects synthetic placeholder verses ("آيَةٌ كَرِيمَةٌ... / অফলাইন সম্পূর্ণ পাঠ লোড হচ্ছে"). Users browsing/searching unopened surahs can see placeholder text presented as verses — listing must not imply otherwise, and code should stop rendering placeholders as real verses (deferred code change, needs approval).
- Duas **(C — count corrected)**: **18** `DuaItem` entries in `NoorUpRepository.kt` (grep count = 18), each with Arabic + Bangla phonetic + English transliteration + BN/EN meaning + reference. **Correction:** v1 of these docs and the README said "24+" — wrong. In-app AI text also says 18 (`NoorUpViewModel.kt:926-928`), which is the correct number. Copy + system-share verified (`NoorUpScreens.kt:2866-2954`).
- Hadith **(C partial + U completeness)**: 8 asset files; 6 large (bukhari 31 MB, muslim 23 MB, abudawud 15 MB, tirmidhi 15 MB, nasai 15 MB, ibnmajah 10 MB) + 2 tiny stubs (`muwatta.json` 9.5 KB, `riyadussalihin.json` 9.7 KB — contents **U**). Book/chapter/topic/narrator/grading, BN+EN translation, search + filter + pagination + bookmarks, system-share (`NoorUpScreens.kt:2685-2711`) confirmed. Do NOT claim "complete 6-book with Bengali sharh" — roadmap still lists it as future, and stub files are unverified.
- Tools **(C)**: per-prayer alert toggles + Sehri/Maghrib alerts + test notification + exact-alarm status; Digital Tasbih (presets, custom goal, counter); Solar Engine Inspector; Qibla compass view; Hijri calendar + events countdown; Zakat calculator with "8 Quranic Heads / কুরআনের ৮ খাত" tab, Nisab basis, Vori units, 2.5% math (`ZakatCalculatorView.kt:40-45,91-93,150-217`); 4x2 widget with 1-tap pin.
- Qibla **(C implemented, accuracy M — correction)**: v1 of this doc said "sensor-fusion NOT done". That was wrong vs code: `QiblaCompassView.kt:54-100` implements rotation-vector sensor fusion with accelerometer/magnetometer fallback and `GeomagneticField` declination correction, and `RiskyLogicRegressionTest.qibla_dhakaBearing_matchesPublished` pins Dhaka ≈277.6° ±3°. **Corrected stance:** bearing math is implemented and unit-pinned, but on-device compass accuracy is **M** (sensor quality, calibration, metal interference) — listing must say "guidance" and advise calibrating + cross-checking, never "precise to the degree". README roadmap line calling sensor fusion future work is stale — flag for README fix.
- Prayer-time accuracy **(T pin + M field check — do not over-claim)**: `SolarPrayerEngine` custom solar math; `RiskyLogicRegressionTest.dhakaTimes_matchPublishedApproximately` allows **±25 min** vs published Dhaka times — a gross-regression guard, not a certification. Correct listing language: "solar calculation with method/juristic options and city/GPS calibration; Makruh windows shown; compare with your local masjid timetable". Never "certified accurate" or "Islamic Foundation approved" — the KARACHI method label ("Islamic Foundation / Karachi 18°/18°", `NoorUpModels.kt:165`) is a naming choice, not an endorsement **(U)**.
- Islamic Assistant **(C)**: v1.2.0 is a local keyword-rule engine (`NoorUpViewModel.kt:900-952`) — no network calls, no keys, airplane-mode safe. It is NOT an LLM: fixed canned answers (e.g. Quran answer claims "6,236 ayahs... fully offline" — update only after Quran test passes). The in-app name was changed from "AI Companion" to "Islamic Assistant" to match. Listing must say "offline built-in answers for learning" and "consult scholars for fiqh rulings", never "AI fatwa" or "Gemini answers" (old README Gemini text is stale).
- Stats **(C local-only)**: completion %, streak, 7-day timeline, missed-day → Qada ledger, pending vs made-up per prayer (`AnalyticsScreen`, habit models). No Firebase Analytics/Crashlytics/RemoteConfig/network analytics found by grep — local analytics only.
- Cross-cutting **(C)**: BN/EN toggle, Bengali digits (pinned by `widgetParse_handlesBanglaAndArabicDigits`), dark/light M3, 8 BD city presets + optional GPS, `AlarmManager + WorkManager + BootReceiver` rescheduling. Location: permission-gated (`NoorUpScreens.kt:179`), `Geocoder` off-main-thread (`:4601-4607`), lat/lng persisted in local `SharedPreferences` only.

## 2. Offline / no-ads / privacy verification (corrected)

- **Offline (C code, M permission cleanup):** no `HttpURLConnection` / `URL(` / Retrofit / OkHttp / Firebase / Gemini / WebView / ads imports found by grep over `app/src/main/java`; `app/build.gradle.kts` has only AndroidX + WorkManager + Gson. Quran/Hadith/Dua/AI all resolve to bundled assets + local `SharedPreferences`. **Inconsistency found:** `AndroidManifest.xml` still declares `INTERNET` + `ACCESS_NETWORK_STATE` though no code path uses the network. Until removed or justified, listing copy must say "works offline; no account; assistant answers on-device" — not "never touches the internet". Code change (remove unused permissions, needs approval + retest) is filed in §6.
- **No-ads (C):** no ads SDK dependency, no `AdView`/`MobileAds` references. Safe to state "no ads SDK in v1.2.0" with `app/build.gradle.kts` + grep as evidence.
- **Location (C implementation, M Console declaration):** optional, permission-gated GPS + 8 BD presets; coordinates stored in local prefs for prayer/Qibla math only. No exfiltration path exists in code, but the Data Safety form ("location collected?") is still a **manual Console declaration** — declare "precise location, on-device use, not shared" or remove GPS if you want a "no location collected" label.
- **Data Safety manual-confirm list (all M, cannot be auto-verified):** data-collection/no-collection answers, exact-alarm + notification runtime rationale text, content rating questionnaire, target-API/deadline banner, closed-test opt-in count/days → production eligibility, account-deletion field (no accounts — declare accordingly), backup/transfer rules (`backup_rules.xml` / `data_extraction_rules.xml` are currently empty — confirm that means local-only backup, not cloud sync).

## 3. Confirmed vs inferred (do not mix)

**Confirmed by implementation + tests/data inspected:** 18 Duas; 114 Quran asset files + 6236-verse manifest on disk with real trilingual spot-check (surah 002); Hadith 6 large + 2 stub files with share/search/bookmarks; Qibla sensor-fusion + declination code with ±3° unit pin; 8 calculation methods + 2 juristic methods; Zakat 8-heads tab + Nisab/Vori/2.5% logic; offline rule-based AI; local-only stats; empty cloud-backup rules; no network/ads SDK imports.
**Inferred in v1 (now corrected, do not publish):** "24+ Duas" (wrong — 18); "only 2 surahs curated so no full-Quran claim" (wrong framing — assets are full, fallback list is partial); "no sensor fusion" (wrong — implemented); "all permissions justified" (wrong — INTERNET unused); "accurate prayer times" as absolute (only ±25 min regression pin + field check pending); Quran translator attribution (none found — U); muwatta/riyadussalihin completeness (stub-size — U).

## 4. Existing store assets (verified)

- `store-assets/noorup-icon-512.png`, `store-assets/noorup-feature-1024x500.png`.
- `store-assets/playstore/01–08 *-1080x1920.png` + editable `.html` masters (1080×1920 portrait).
- Sequence today: 01 prayer-times → 02 quran → 03 dua → 04 hadith → 05 qibla-tools → 06 zakat → 07 ai → 08 tracking. All Bengali-first headlines (see `NOORUP_PROMOTIONAL_CONTENT_PLAN.md` for verbatim copy).
- Branding: app icon = emerald crescent (`#10B981`) + gold sparkle (`#FBBF24`) on deep emerald (`#064E3B`), adaptive 108dp vector (`ic_launcher_foreground/background.xml`). App theme: EmeraldGlow `#10B981`, EmeraldDark `#064E3B`, Gold `#FBBF24`, Dark bg `#090D16`, Light bg `#F8FAFC`. Store HTML uses `Hind Siliguri` + `Noto Serif Bengali`; in-app `Type.kt` is default system font — acceptable but note the mismatch for Bengali headline weight.
- `Video/brag.mp4` exists (unreviewed here); `RELEASE_NOTES_v1.2.0.md` draft EN/BN under 500 chars exists.

## 5. Checklist evaluation (15-point PDF → NoorUp)

1. **Start with a situation [PDF]**: Current 01 opens with benefit ("ওয়াক্তমতো নামাজের সময়"), not a felt problem. Opportunity: add hook line or reorder so swipe 1 names the pain (missed wakt / scattered apps) before the solution.
2. **Problem → solution [PDF]**: No explicit before/after pair today; 8 shots are 8 parallel features. Consider making 01 = pain + promise, 02 = orbit dashboard as the payoff.
3. **Planned sequence [PDF]**: Roles are mostly clear (hook → core → supporting → close), but the prayer-times differentiator (method options + Makruh + Sehri/Iftar, evidence-graded above — not "certified accurate") competes with Quran/Dua for position 1–2. Tracking (08) is a strong closer but currently last without an explicit invitation frame.
4. **One message per shot [PDF]**: Good — each HTML has one pill + one H1 + one sub. Risk: 05 bundles Qibla+Tasbih+Hijri in one frame. Split or pick one hero per frame.
5. **Simple natural language [PDF]**: Bengali copy is short and spoken. Keep English listing equally plain; avoid "solar engine / Nisab logic / sensor fusion" in headlines — move to supporting text.
6. **Mobile-size text [PDF]**: Layout uses 96px H1 / 37px sub / 30px pill at 1080px wide — likely readable, but subs (e.g. screenshot 02's "১১৪ সূরা • আরবি + বাংলা + ইংরেজি • সম্পূর্ণ অফলাইন") must be checked at real Play preview size AND reworded per §1 (full-text-pending-test + source undisclosed). Rule: if sub is unreadable, delete it rather than shrinking further.
7. **Expressions/actions support message [PDF]**: Phone mockups carry the meaning; no people/characters to judge. Ensure status shown (e.g. "যোহর • চলছে") matches the headline claim.
8. **Match the real app [PDF]** — highest risk: (a) do not show Family Zikr (removed in v1.2.0); (b) screenshot 02's "সম্পূর্ণ অফলাইন" + Plan draft's "114 Surahs with Arabic text, Bangla + English meaning" may only ship after `QuranOfflineCompletenessTest` passes AND translation source is disclosed — until then use the hedged Quran line in the content plan; (c) Qibla frame must say "guidance" (sensor fusion implemented, field accuracy unverified — never "precise"); (d) Dua count is 18, not 24+ — fix screenshot 03 sub; (e) Assistant frame must say offline built-in answers, not Gemini/LLM/fatwa (and the frame title must drop "AI" to match the renamed in-app "সহকারী").
9. **Show important features completely [PDF]**: Phone frames crop bottom nav; ensure the claimed choice (e.g. calculation method, Qada ledger, Zakat inputs) is visibly uncut in at least one shot.
10. **Clean composition [PDF]**: HTML masters use soft radial bg + heavy phone border + shadow — generally clean. Watch: small dot indicators and warning pill text at preview size; decorative glow must not lower contrast.
11. **Complex → simple benefit [PDF]**: "Solar Engine Inspector", "Nisab", "Qasr" need benefit headlines ("Never pray in Makruh time by mistake") with technical terms only in sub-labels.
12. **Design each format separately [PDF]**: Portrait masters exist. Feature graphic (`1024x500` landscape) must be a separate composition, not a cropped screenshot. Same for icon and any video cover.
13. **Masters vs uploads separate [PDF]**: Good pattern already — keep `.html` editable → high-res master → Play-size export with clear filenames. Never upload the editable directly.
14. **Preserve approved work [PDF]**: Do not redesign all 8 shots at once. Fix headline/sub/accuracy per shot and re-verify layout.
15. **Review as one system [PDF]**: Check all 8 together for order, typography (Hind Siliguri everywhere?), consistent pill/H1/sub style, cropping, accurate representation, export dimensions/formats.

## 6. Prioritized recommendations (corrected)

### P0 — Immediate (before public launch, mostly Play Console manual work)

1. Freeze truthful copy per §1 evidence grades: Dua 18 (not 24+); Quran hedged line until test + source disclosure; Qibla "guidance"; prayer times without "certified accurate"; AI as rule-based offline answers. Remove Gemini/online-AI, Quran-audio, Family Zikr claims. *Play Console edit. No code change.*
2. Rewrite title (≤30 chars) + short desc (≤80 chars) + full desc (≤4000 chars); Bengali-first with plain English twin. Use corrected drafts in content plan. *Play Console edit + BN proofread (external).*
3. Fix screenshot story: 01 hook, 02 orbit payoff, 03–05 Quran/Dua/Hadith (with corrected 02/03 subs), 06–07 Zakat/tools + offline assistant, 08 tracking + invite. Split or re-scope 05. *Design export, no code change. Do not replace approved assets without review.*
4. Mobile-readability + consistency pass at real preview size. *Designer check on a phone, no code change.*
5. Verify upload specs + Data Safety + closed-test → production path live in Console (web lookup unavailable). *Console access (external).*

### P1 — Soon (small, testable, needs approval)

6. Run `./gradlew test` (gates: `QuranOfflineCompletenessTest` 114/6236/3-language, `RiskyLogicRegressionTest` Dhaka ±25 min + Qibla ±3°, `ResponsiveBreakpointsTest`); confirm exact-alarm + boot-reschedule + widget on 2 real devices per `RELEASE_NOTES_v1.2.0.md`. Do not regress `ResponsiveUi.kt` clipping guards.
7. Content-share polish (Dua/Hadith system-share exists): append app name + Play link only after public release; no referral logic.
8. Privacy-safe feedback channel (listing contact email + system-share feedback) for prayer-time error reports; no analytics SDK.

### P2 — Optional future (only if meaningful value, needs approval + new work)

9. Referral system: **not recommended now** (no accounts/backend; spam/policy risk). Revisit only as plain system-share + Play link with UTM, never incentivized ratings.
10. Roadmap items (Ramadan mode, cloud sync, Quran audio, expanded Hadith sharh): do not promise until shipped and tested. Note: Qibla sensor fusion is already implemented — README roadmap needs updating, not new development.

## 7. Final prioritized change list (nothing implemented — awaiting approval)

**A. Play Console edits (manual, no code):** A1 corrected title/short/full (Dua 18, hedged Quran, Qibla guidance, no accuracy absolute, rule-based AI). A2 Data Safety (location on-device use, no collection/sharing, no accounts, empty cloud-backup rules). A3 content rating + target-API/deadline + closed-test → production checklist. A4 contact email for tester feedback.
**B. Promotional asset edits (design only, present before replacing):** B0 screenshot 01 pill `সঠিক নামাজের সময়` → neutral `নামাজের সময়সূচি` (drop accuracy absolute). B1 screenshot 02 sub → hedged Quran line (e.g. "১১৪ সূরার তালিকা • অফলাইন পাঠ • বাংলা-ইংরেজি অর্থ" + drop "সম্পূর্ণ" until test+source). B2 screenshot 03 sub → "১৮টি মাসনূন দোয়া • উচ্চারণ ও রেফারেন্সসহ". B3 screenshot 05 → single hero (Tasbih) + Qibla/Hijri strip with "guidance" wording. B4 feature-graphic landscape recomposition (not a crop). B5 `brag.mp4` frame-by-frame v1.2.0 audit before use as promo video.
**C. Code changes (approval + tests required, NOT implemented):** C1 remove or justify unused `INTERNET`/`ACCESS_NETWORK_STATE` (`AndroidManifest.xml`) — evidence: zero network imports; retest install + widget + alarms. C2 stop rendering synthetic placeholder verses as real content (`NoorUpViewModel.buildCompleteVerseList` + `QuranRepository.buildCompleteList`) — show loading/cover state until `loadFullSurah` resolves. C3 fix README staleness (Dua 24+→18, Gemini offline correction, Qibla roadmap line, Quran coverage framing). C4 (optional later) Play-link UTM on Dua/Hadith share text post-launch.
**D. Manual verification (human/Console/device):** D1 `./gradlew test` full pass (especially Quran completeness). D2 prayer-time field check vs 2+ local masjid timetables (record method/city/date deltas; never publish "certified"). D3 Qibla field check vs known bearing (record device/calibration). D4 Quran translation source identification + scholar/BN proofread of Dua/Hadith references. D5 2-device fresh-install + update-over-data + alarm/widget/Geocoder-offline test. D6 live Play spec + Data Safety + rating + tester-count confirmation in Console.

## 8. What needs external/manual action

- Google Play Console access + Data Safety + rating + target-API/deadline + tester criteria (manual).
- BN proofread, translation-source disclosure, voiceover/music licensing, performer/location consent (external).
- No API keys needed (offline). No new permissions — recommend removing unused ones (C1).
- Never: spam, fake/incentivized reviews, misleading ads, keyword stuffing, debug-signed uploads. Signed `bundleRelease` AAB only; staged rollout ~20% first.
