# NoorUp Pre-Release Verification (v1.2.0, versionCode 6)

Date: 2026-10-14. Scope: read-only inspection + test execution. **No application code, resources, tests, versions, permissions, or assets were modified for this report.** Quran fix is proposed only — awaiting approval.

## 0. UPDATE 2026-10-14 (later same day): release blockers FIXED, tests green

Approved scope from "Fix Release Blockers Safely" was implemented. No commit/push/publish performed.

**Files changed (only these; all other dirty/new files are pre-existing user work, untouched):**
- `app/src/main/java/com/example/ui/noorup/quran/QuranRepository.kt` — removed `buildCompleteList()` fabrication + `getSurahVerses()` legacy entry (had zero callers); `loadFullSurah()` now returns ONLY validated bundled content or empty; added `isAuthenticVerseList()` gate (exact size, sequential 1..N, non-blank AR/BN/EN).
- `app/src/main/java/com/example/ui/noorup/NoorUpViewModel.kt` — removed `buildCompleteVerseList()`; `_surahs` seeds metadata with EMPTY verse lists; `openSurah()` publishes empty + loading, fills only on validated success, else sets new `quranLoadError`; added `retrySurahLoad()`; `closeSurah()` clears error; `enterFocus()` starts with null verse + new `isFocusLoading`, validates before publishing, sets error on failure; added `retryFocusLoad()`; `focusNextVerse`/`focusPrevVerse` trigger a load when the target verse is not yet loaded (prevents stuck spinner).
- `app/src/main/java/com/example/ui/noorup/NoorUpScreens.kt` — surah detail: new error + Retry card (`retrySurahLoad()`) shown only when load failed with empty list; verse cards render solely from the validated list (empty list renders nothing). Focus view: null verse shows spinner while loading, error + Retry (`retryFocusLoad()`) on failure. Hadith book cards: new "আংশিক নমুনা / Partial sample" badge for partial books.
- `app/src/main/java/com/example/ui/noorup/hadith/HadithModels.kt` — new `Book.isPartialSample` (default false; existing positional construction unaffected).
- `app/src/main/java/com/example/ui/noorup/hadith/HadithRepository.kt` — muwatta: "৩টি হাদিস (আংশিক নমুনা)" / "২টি অধ্যায় (নমুনা)", flagged partial; riyadussalihin: "৩টি হাদিস (আংশিক নমুনা)" / "৩টি অধ্যায় (নমুনা)", flagged partial. Inflated ১,৮৫৮/১,৮৯৬ and ৬১/৩৭১ claims removed. No Hadith text touched; no datasets added.
- `app/src/main/AndroidManifest.xml` — removed `INTERNET` + `ACCESS_NETWORK_STATE` (app-declared). Location/notification/boot/alarm/vibrate untouched.
- Tests: NEW `QuranLoadingRegressionTest` (10 tests, §8 scenarios 1–8), NEW `HadithPartialSampleTest` (5 tests); UPDATED `QuranOfflineCompletenessTest.viewmodel…` to the new honest contract (starts unloaded → complete after open). Asset-level tests in that file unchanged.

**Quran behavior before → after:** before, opening a surah instantly showed synthetic verses under the spinner, Focus showed them immediately, failures kept them, and the master/search/bookmark lists were pre-filled with fabrications. After, every surface shows empty + loading spinner until the validated bundle lands; failures show an explicit error + Retry with no verses; bookmarks/mem flags still persist via prefs and re-apply on load; race guards, caching, offline asset path unchanged. `getSurahVerses()` removal is safe (zero callers repo-wide).

**Hadith representation:** muwatta + riyadussalihin remain listed (8-book UI intact) with partial-sample badges and true available counts; all other books unchanged. Displayed counts now trace to parsed asset contents, guarded by tests.

**Permissions verdict:** INTERNET fully gone (absent from merged manifest + merger report). ACCESS_NETWORK_STATE was removed from our manifest but persists in the merged manifest, INJECTED by `androidx.work:work-runtime:2.10.0` (merger report lines 368–371) alongside WAKE_LOCK/FOREGROUND_SERVICE — required by the offline-first rescheduling architecture; no `tools:node="remove"` applied (out of scope/risk). Do not claim "never accesses the internet"; accurate claim: "no INTERNET permission, no network code paths, no network SDKs; WorkManager-injected state-check permission only."

**Test commands + results (same JDK setup as §3):**
- `.\gradlew.bat :app:testDebugUnitTest --console=plain --rerun-tasks` → BUILD SUCCESSFUL: 53 tests, 0 failures, 0 errors, 1 pre-existing skip (`ExampleRobolectricTest`: 12/1-skip; `ExampleUnitTest` 9; `HadithPartialSampleTest` 5 new pass; `QuranLoadingRegressionTest` 10 new pass; `QuranOfflineCompletenessTest` 4 pass incl. updated contract test; `ResponsiveBreakpointsTest` 5; `RiskyLogicRegressionTest` 8).
- Intermediate run note: first full run after the fix failed exactly one test — the old `viewmodel exposes complete verse lists` assertion that encoded placeholder behavior; updated to the honest contract, then green. One new-test iteration fixed a test-only bug (`openFd` on compressed JSON assets → stream read).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL (`app-debug.apk`, 49.5 MB, size driven by bundled offline assets).

**Remaining manual checks (still required, unchanged from §9):** D2 masjid-timetable prayer comparison; D3 Qibla field check; D4 translation provenance + reference proofread; D5 2-device install/alarm/reboot/widget/GPS matrix incl. Quran open/switch/fail/retry/Focus pass; D6 live Play specs + Data Safety + rating + tester criteria; D7 screenshot readability; D8 `brag.mp4` frame audit. Store copy corrections (§7) still frozen pending asset redesign — not published.

---

## 1. Executive summary + readiness verdict

**Verdict: NOT release-ready. Two P0 correctness blockers must be fixed first.**

- **P0-1 (confirmed): Quran placeholder verses can render as genuine text.** `openSurah()` publishes synthetic placeholder verses into the visible verse list before the bundled asset loads, the detail screen renders that list underneath the loading spinner, Focus mode reads placeholders immediately, and load failures leave placeholders on screen. Full trace in §4.
- **P0-2 (confirmed): Hadith book cards advertise totals the bundles do not contain.** `muwatta.json` and `riyadussalihin.json` each contain **3** hadiths while their cards claim ১,৮৫৮ and ১,৮৯৬ respectively (`HadithRepository.kt:107-134`, verified by parsing both files this session).
- **Otherwise green:** full local unit suite passes 38/38 (1 pre-existing skip), Quran assets are present and complete on disk (114 files, 6236 verses), prayer/Qibla math is unit-pinned (with field verification still required), and the app is offline by implementation (with one permission-cleanup item pending approval).
- **Do not publish** the current store screenshots/drafts (Dua 24+, "সম্পূর্ণ অফলাইন", "সঠিক", "সহীহ" framing, offline-AI wording) — corrected table in §8.

## 2. Baseline Git + build status

- Branch: `main`, ahead of `origin/main` by 1. Working tree is **dirty with extensive pre-existing user changes** (46 files in `git diff --stat`, plus untracked `docs/`, `store-assets/`, `ResponsiveUi.kt`, `QuranOfflineCompletenessTest.kt`, `ResponsiveBreakpointsTest.kt`; staged deletions of `FamilyCircleSync.kt`, `FamilyLiveLink.kt`, `FamilyZikrTest.kt`, consistent with the v1.2.0 "remove Family Zikr" note). Nothing was discarded, reset, or reformatted for this task.
- App identity: `applicationId com.aistudio.noorup.qzmxlw`, `versionName 1.2.0`, `versionCode 6`, `minSdk 26`, `targetSdk/compileSdk 36`, AGP 8.5.1 (AGP warns it was tested only up to compileSdk 34 — warning only, build succeeds).
- Environment note: `JAVA_HOME` is not set in a fresh shell; tests were run with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. No dependencies, Gradle config, or architecture were changed.
- No pre-existing build errors: all executed builds succeeded.

## 3. Tests run + results (working-tree code, commands exact)

JDK setup: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`

1. Targeted: `.\gradlew.bat :app:testDebugUnitTest --tests "com.example.QuranOfflineCompletenessTest" --tests "com.example.RiskyLogicRegressionTest" --tests "com.example.ResponsiveBreakpointsTest" --rerun-tasks` → **BUILD SUCCESSFUL** (41s). Results from `app/build/test-results/testDebugUnitTest/*.xml`:
   - `QuranOfflineCompletenessTest`: 4/4 pass — 114 asset files present; every file gap-free 1..totalVerses with non-blank Arabic+Bangla+English; grand total 6236; metadata totals match assets. **This verifies the bundled JSON, not the on-screen rendering (see P0-1).**
   - `RiskyLogicRegressionTest`: 8/8 pass — Dhaka Jan times within ±25 min of published values; no NaN at polar latitudes; Dhaka Qibla ≈277.6° ±3° with distance 4800–5500 km; Bengali/Arabic digit parsing; Hijri leap/clamp/grid checks.
   - `ResponsiveBreakpointsTest`: 5/5 pass — carousel card width clamps to viewport fractions with readable minimum.
2. Full suite: `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, 38 tests, 0 failures, 0 errors, 1 skipped:
   - `ExampleRobolectricTest` 12 (1 skipped), `ExampleUnitTest` 9, plus the 17 above. The single skip is a pre-existing `<skipped/>` entry in `ExampleRobolectricTest` (not caused by this task; exact test name not chased since nothing was modified).
3. Not run (correctly out of scope for local unit task): `androidTest` instrumented tests (`ExampleInstrumentedTest`) require a device/emulator; release signing/`bundleRelease` was not built (keystore-gated by design); no manual device checks were performed.

Test gaps: no automated test covers that placeholders never reach the UI (§8 regression plan proposes them); prayer/Qibla tests are tolerance-pinned math, not field accuracy; notification reboot/exact-alarm behavior is covered only by Robolectric shadows + manual checklist in `RELEASE_NOTES_v1.2.0.md`.

## 4. Quran placeholder issue — CONFIRMED (root cause + trace)

**Rule: synthetic placeholder content must never render as genuine Quranic text. The current code violates this.**

Root cause (two seeding sites, one premature publish):
- `NoorUpViewModel.kt:558-584` `buildCompleteVerseList()` fills every verse number missing from the small embedded sample with a fabricated `Verse` (`arabicText "آيَةٌ كَرِيمَةٌ مِنْ سُورَةِ …"`, `banglaTranslation "…অফলাইন সম্পূর্ণ পাঠ লোড হচ্ছে"`, correct-looking `number = i`). `_surahs` is initialized with these lists (`:586-590`), so 113 of 114 surahs contain mostly synthetic verses at startup.
- `QuranRepository.kt:332-350` `buildCompleteList()` has the same fallback pattern for the non-asset path.
- `openSurah()` (`NoorUpViewModel.kt:606-638`): line 608 **immediately publishes the seeded (placeholder) list** into `_selectedSurahVerses`, then loads the asset on IO. On failure/exception/empty result the placeholders stay (lines 618/632-633 only log). `closeSurah()` clears correctly (`:640-644`).
- Focus mode: `enterFocus()` (`:1261-1297`) sets `_focusVerse` from the seeded list synchronously (line 1268) before the IO load finishes, so the firstpainted focus verse can be synthetic; only later refreshed if the load wins the race guards.
- UI: `NoorUpScreens.kt:1644-1703` shows a loading spinner **and simultaneously renders `items(selectedVerses)`** underneath it, with a header "Showing: N" at full count (`:1616`). There is no error/empty state for failed loads — the list is never withheld while unauthenticated content is present. Surah search/bookmark/master views read the same seeded `_surahs`, so placeholders are reachable without opening a surah.

Flow-by-flow answers: (1) selection calls `openSurah`, placeholders appear instantly; (2) initial state is placeholder-seeded; (3) loading spinner shows above placeholder verses instead of replacing them; (4) asset retrieval via `loadFullSurah` works (test-proven) and replaces placeholders only on success for the opened surah; (5) error/empty paths keep placeholders with no user-visible failure; (6) verse cards render `arabicText`/translation indistinguishably from real text; (7) switching surahs replaces the list (no stale old surah — race guards at :617/:626 + job cancel work) but the new surah's placeholders show until its load lands; (8) reopening reloads from cache/asset and converges to real text; offline repeat works because assets are bundled (no network involved).

**Proposed minimal fix (NOT implemented — approval requested):**
- Represent "not yet loaded" explicitly: keep seeded metadata (names, counts) but store **empty verse lists** for unopened surahs instead of synthesizing verses; delete the placeholder-fill branches in `buildCompleteVerseList` and `QuranRepository.buildCompleteList` (or gate them behind a clearly-labeled debug flag, default off).
- In `openSurah`/`enterFocus`: publish `emptyList()` + `isQuranLoading=true` first; publish real verses only after `loadFullSurah` returns non-empty and passes a lightweight sanity check (size == totalVerses, numbers 1..N, blank-text rejection). On failure/empty: keep the list empty, expose an error state (`quranError: StateFlow<String?>`), and render a retry card — never fall back to fabricated text.
- In `NoorUpScreens.kt` detail + Focus views: render verse cards only when the loaded list is non-empty and number-complete; show spinner while loading, error+retry on failure. Keep bookmark/mem flags re-application, race guards, caching, and offline asset path unchanged. No new database, network, or architecture.
- Affected files: `NoorUpViewModel.kt` (~:557-644, :1246-1297), `quran/QuranRepository.kt` (:331-356), `NoorUpScreens.kt` (~:1393-1706 incl. focus view ~:1068-1210), plus new/updated tests (§8). Also correct the in-app canned Quran answer (`NoorUpViewModel.kt:933`) only if its "6,236 ayahs fully offline" wording outruns the verified rendering path.

Regression plan: see §8 (8 required scenarios mapped to concrete tests in existing frameworks: Robolectric + existing unit conventions; no new test libraries).

## 5. Quran completeness + provenance

- Present and test-verified on disk: 114/114 `surah_*.json` + `_manifest.json` (`{"surahs":114,"totalVerses":6236}`); `QuranOfflineCompletenessTest` 4/4 pass (counts, ordering, trilingual non-blank, 6236 total). Spot-check `surah_002.json` (363 KB) shows real translations; smallest `surah_112.json` (924 B) is size-consistent with 4 short verses.
- **Not proven by file counts alone:** on-screen correctness (blocked by P0-1), translation accuracy/edition, and recitation/text licensing. Provenance search over `app/src/main` found **no translator, edition, or license attribution** — the only "Uthmani" mention is the app's own canned assistant answer (`NoorUpViewModel.kt:933`), which is itself an unverified claim. **Report translation source, edition, and licensing as UNVERIFIED. Do not name a translator or script standard in the listing until documented.**
- Safe-failure behavior today is inadequate (placeholders instead of failure states) — addressed by the proposed fix.

## 6. Release-critical reviews (no modifications made)

- **Prayer times:** `SolarPrayerEngine` custom solar math with 8 calculation methods (default KARACHI labeled "Islamic Foundation / Karachi 18°/18°" — a label, not an endorsement) + Hanafi/Shafi Asr; 8 BD city presets + optional GPS. Tests prove: Dhaka January within **±25 min** of published values, no-NaN at extremes, digit handling. That is a gross-regression guard, **not certification**. Unverified: year-round accuracy, high-latitude behavior beyond NaN-safety, GPS-vs-preset deltas, masjid-timetable agreement (manual D2 required).
- **Qibla:** bearing math + rotation-vector fusion with accel/mag fallback + `GeomagneticField` declination correction (`QiblaCompassView.kt`), unit-pinned to ±3° for Dhaka. A unit test is not compass accuracy: sensor quality, calibration, cases, and metal interference dominate real-world error. Listing must say "guidance" with calibrate-away-from-metal + cross-check advice (manual D3 required).
- **Built-in answers:** local keyword-rule engine with canned BN/EN responses (`NoorUpViewModel.kt:900-952`); zero network/AI SDK imports found; `build.gradle.kts` has no Firebase/Gemini/LLM. Must be described as "offline built-in answers (rule-based learning aid)", never Gemini/LLM/fatwa. Note: the canned Quran/Dua answers hard-code counts ("6,236", "18") — keep them in sync with verified data.
- **Duas:** **18** items in `NoorUpRepository.kt` (recounted this session), each with Arabic + BN phonetic + EN transliteration + BN/EN meaning + reference; copy + system-share confirmed. "24+" in README/v1 docs is wrong.
- **Hadith:** 8 book configs; 6 large assets vs **muwatta (3 hadiths vs ১,৮৫৮ claimed)** and **riyadussalihin (3 vs ১,৮৯৬ claimed)** — P0-2. Search/filter/pagination/bookmarks/share exist, but any screen showing metadata totals for those two books misleads. Do not claim "complete 6-book with sharh" (roadmap still lists it as future).
- **Privacy/permissions:** manifest declares `INTERNET`, `ACCESS_NETWORK_STATE` (no code path uses them — grep over `app/src/main/java` finds no HTTP/client/Firebase/WebView/ads usage), plus justified `FINE/COARSE_LOCATION` (permission-gated, Geocoder off-main-thread), `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`. Location persists in local `SharedPreferences`; backup/extraction rule files are empty (local-only posture, Console declaration still manual). **Do not claim "never accesses the internet"** until the two unused permissions are resolved (recommendation only — removal needs approval + retest per §9).
- **Responsive/notifications:** `ResponsiveBreakpointsTest` 5/5 + `ResponsiveUi.kt` guards exist, but small-screen/wide/tablet rendering and widget/alarm/reboot behavior are not proven by unit tests — manual matrix in §9 (D5) still required.

## 7. Store listing corrections (design freeze — nothing edited or published)

| Asset or claim | Current issue | Evidence | Recommended correction |
|---|---|---|---|
| Screenshot 01 pill "সঠিক নামাজের সময়" | Absolute accuracy claim | Only ±25 min regression pin (`RiskyLogicRegressionTest`); no certification | Pill → neutral "নামাজের সময়সূচি"; headlines without "certified/accurate"; add "compare with masjid timetable" |
| Screenshot 02 sub "…সম্পূর্ণ অফলাইন" + Plan "114 Surahs with Arabic + BN + EN" | Overstates verified rendering; translation source undisclosed | Assets test-proven on disk but P0-1 placeholders can render; no provenance in repo | Hedged line "১১৪ সূরার তালিকা • অফলাইন পাঠ • বাংলা-ইংরেজি অর্থ" + drop "সম্পূর্ণ" until P0-1 fixed, test re-run, source disclosed |
| Screenshot 03 sub "২৪+ বিভাগ" | Wrong count, confuses items with categories | 18 `DuaItem` (`NoorUpRepository.kt`); AI text says 18 | "১৮টি মাসনূন দোয়া • উচ্চারণ ও রেফারেন্সসহ" |
| Screenshot 04 "সহীহ হাদিস বাংলায়" as blanket | Implies whole library uniformly graded/complete; 2 books have 3 items | `HadithRepository.kt:107-134` vs parsed 3+3; roadmap lists completion as future | "হাদিস বাংলায় পড়ুন / অধ্যায়, বিষয় ও বুকমার্কসহ"; show grading only where the visible book carries it |
| Screenshot 05 Qibla "কম্পাস" hero | Implies precision; field accuracy unverified | Fusion + declination implemented, ±3° math pin only | Single hero (Tasbih) + "কিবলা নির্দেশনা (guidance — calibrate + cross-check)" |
| Screenshot 07 AI "১০০% অফলাইন • কুরআন-সুন্নাহ ভিত্তিক" | "AI" suggests LLM; basis overstates review | Rule engine (`NoorUpViewModel.kt:900-952`), no scholar-review evidence | "অফলাইনে ইসলামিক উত্তর • শেখার সহায়তা" + "consult scholars for rulings" |
| Descriptions "Accurate…", "no ads", "offline" | Absolutes exceed evidence while unused network permissions exist | Manifest INTERNET/ACCESS_NETWORK_STATE unused; no ads SDK confirmed | "Works offline, no ads SDK" + prayer/masjid disclaimer; no "never touches internet" until C1 resolved |
| Feature graphic / `brag.mp4` / readability | Landscape graphic must not be a crop; video frames must match v1.2.0; subs must survive phone preview size | `store-assets/*`, `Video/brag.mp4` unreviewed frame-by-frame | Separate 1024×500 composition; per-frame v1.2.0 audit + captions; delete subs that fail preview-size test rather than shrinking |

## 8. Regression tests for the proposed Quran fix (existing frameworks only)

Conventions: local Robolectric unit tests (`QuranOfflineCompletenessTest` pattern) + `NoorUpViewModel` under Robolectric with `ApplicationProvider`; no new libraries.

1. Unopened surah exposes no synthetic content: instantiate VM, assert every unopened surah's verse list is empty (or metadata-only) and contains none of the known placeholder strings ("آيَةٌ كَرِيمَةٌ", "অফলাইন সম্পূর্ণ পাঠ লোড হচ্ছে").
2. Loading state while unavailable: call `openSurah(n)`, assert `isQuranLoading==true` and `selectedSurahVerses` stays empty until the asset load completes (use dispatcher control or asset-gated assertion, not timing flakes).
3. Success renders bundled verses: after load, `selectedSurahVerses` equals `loadFullSurah` output size/content for several surahs (short + Baqarah-length), numbers exactly 1..N, no placeholder strings.
4. Failure shows no fabrication: point the loader at a missing/corrupt asset (temp test asset or mocked Context), assert list stays empty, loading clears, and an error state is exposed (new `quranError` flow) — no placeholder verses.
5. No stale surah: open A, then B before A's IO finishes; assert the visible list never shows A's verses under B's header (selected-number guards), final state is B's authentic verses.
6. Reopen works: open → close → reopen same surah; authentic verses return (cache path), bookmarks/mem flags preserved from prefs.
7. Offline intact: same as (3) with no network (it already is — assert no HTTP clients touched; Robolectric has no network); repeat-open works consecutively.
8. Completeness still green: existing `QuranOfflineCompletenessTest` 4/4 unchanged and passing post-fix.

## 9. Prioritized checklist

**P0 — blockers (correctness / misleading content):**
- P0-1: Fix placeholder-verse rendering (§4 proposal; needs approval → implement → §8 tests → manual open/switch/fail/reopen pass).
- P0-2: Fix Hadith inflated totals — either bundle complete muwatta/riyadussalihin, remove those two books from the library UI, or display actual loaded counts with "sample/partial" labeling (needs content/product decision; needs approval).
- P0-3: Freeze corrected listing copy/assets per §7 table; do not publish current screenshots/drafts.

**P1 — important verification + UX:**
- P1-1: Re-run full `:app:testDebugUnitTest` after any fix (gate on 0 failures).
- P1-2: Decide unused `INTERNET`/`ACCESS_NETWORK_STATE` (remove vs justify — needs approval; retest install/widget/alarms/Geocoder if removed).
- P1-3: Disclose Quran translation source/edition/license or keep hedged wording; scholar/BN proofread of Dua/Hadith references.
- P1-4: Manual device matrix: 2 phones (small + large), fresh install + update-over-data, exact-alarm delivery, reboot reschedule, widget pin/update, GPS vs preset prayer delta, Qibla vs known bearing, Quran open/switch/Focus pass.
- P1-5: Fix README staleness (Dua 18, Gemini→offline rules, Qibla roadmap line, Quran framing) so docs stop contradicting code.
- P1-6: Sync canned assistant answers (`NoorUpViewModel.kt:926-933`) with verified counts; keep "consult scholars" disclaimer.

**P2 — future:** Ramadan mode, cloud sync/backup, Quran audio, expanded Hadith sharh, referral/invite — do not promise in listing until shipped.

## 10. Verified / suspected / manual-verification

- **Verified fact:** 38/38 unit tests pass (1 pre-existing skip); 114 Quran assets + 6236 total on disk with trilingual spot-check; P0-1 placeholder flow traced to exact lines; P0-2 3-vs-1858/1896 counts parsed; 18 Duas recounted; rule-based offline answers; sensor-fusion Qibla code exists; INTERNET/ACCESS_NETWORK_STATE declared but unused by any code path; dirty-tree baseline recorded.
- **Suspected (needs confirmation):** Hadith book cards display metadata totals (code shows totals on cards; exact on-screen rendering should be eyeballed); one Robolectric skip is a benign pre-existing ignore (name not chased).
- **Manual verification required:** D1 post-fix test re-run; D2 prayer times vs 2+ masjid timetables; D3 Qibla field check; D4 translation provenance + reference proofread; D5 device install/alarm/reboot/widget/GPS matrix; D6 live Play specs + Data Safety + rating + tester-count in Console; D7 screenshot phone-preview readability; D8 `brag.mp4` frame audit.

*Change control honored: read/inspect/test only. No source, resource, test, version, permission, dependency, asset, or Git state was altered for this report.*
