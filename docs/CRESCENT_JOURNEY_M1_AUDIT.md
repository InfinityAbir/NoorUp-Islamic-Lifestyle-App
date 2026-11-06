# Crescent Journey — M1 Architecture Audit & Implementation Plan

Status: **M1 complete, awaiting approval.** No production code was changed.
Audited at: v1.5.2 (versionCode 19), branch `main`, commit `0b40fb7`.

---

## 1. Architecture findings

| # | Topic | Finding |
|---|-------|---------|
| 1 | Stack | Single-module Android app, Kotlin + Jetpack Compose (Material3), `minSdk 26`, `targetSdk 37`. Package `com.example`. Gson, WorkManager, Media3, Play Services Auth. **No Room, no SQLite, no Navigation-Compose, no Lottie, no DI framework.** |
| 2 | Structure | `NoorUpViewModel` (3,336 lines) holds nearly all state; `NoorUpScreens.kt` (7,249 lines) holds nearly all screens. Newer features live in sub-packages (`family/`, `ramadan/`, `quran/`, `backup/`, `hadith/`, `audio/`) with pure-Kotlin logic classes (`RamadanLogic`, `FamilyDhikrLogic`, `BackupCodec`) and a screen file each. This is the convention to follow. |
| 3 | Persistence | **One `SharedPreferences` file: `noorup_prefs`.** All prayer, Qada, settings and backup data live there as flat keys. There is no database version to migrate. |
| 4 | Navigation | Bottom bar, six tabs by Int index (`0 Home, 1 Quran, 2 Duas, 3 Explore, 4 Assistant, 5 Stats`) in `MainActivity` + `Crossfade`, persisted in `selected_tab`. Nested screens inside a tab use local state + `NestedBackHandler` / `BackNavigator` (v1.5.2). Crescent Journey will be a nested full-screen route inside the Stats tab — no new bottom tab. |
| 5 | Stats screen | `AnalyticsScreen` (`NoorUpScreens.kt:6686`). A `LazyColumn`: `TopControlsHeader` (location/search/language/theme), a summary `GlassCard`, an optional "Missed Prayers Detected" banner, two sub-tab buttons (`subTab` 0 = Consistency Timeline, 1 = Qada Ledger), then Recent Days list or Qada rows. Tapping a day opens `NoorGardenCard` in a dialog to edit that date. |
| 6 | Theme / language | `is_dark_mode`, `is_english` prefs; `NoorUpTheme`; `GoldAccent = 0xFFFBBF24`. All UI strings are inline `if (isEnglish) "…" else "…"` (not `strings.xml`, which has 5 entries). Digits via `toBanglaDigits()`. New UI must follow the inline bilingual convention. |
| 7 | Reduced motion | `TasbihFeedback.isReducedMotion(context)` reads `ANIMATOR_DURATION_SCALE`. Reuse it; add an in-app override. |
| 8 | Notifications | `PrayerNotificationScheduler` (AlarmManager `setAlarmClock`, exact-alarm permission check with inexact fallback), `PrayerAlarmReceiver`, `PrayerNotificationHelper` (channels incl. `garden_reminders_channel`), `BootReceiver` → `PrayerRescheduleWorker`, `WidgetUpdateReceiver` (TIME_SET / TIMEZONE_CHANGED / DATE_CHANGED). Request codes: prayers 5000–5004, garden 7001, hadith 7002, test 8888. A cancel-all list at `PrayerNotificationScheduler.kt:438` must include any new code. |
| 9 | Backup | `BackupCodec` whitelists key prefixes. Current prefixes include `prayer_habit_`, `prayer_record_`, `prayer_transferred_qada_`, `user_has_logged_`, `qada_`. **Any new key not matching the whitelist is silently dropped from backups.** |
| 10 | Widgets | `NoorUpWidgetProvider.kt:443` reads `prayer_record_<date>_completed` directly. `PrayerOrbitDashboardWidget.kt:746` also calls `toggleHabitPrayer`. That int key must stay maintained. |
| 11 | Tests | JUnit4 + Robolectric (`sdk=34`), driven through `NoorUpViewModel(app)` against a cleared `noorup_prefs`. `QadaTrackingTest` covers the Qada ledger. A stale `test_output.txt` at repo root shows a failed run; a fresh baseline is running (see §6). |

---

## 2. Existing prayer-tracking system (single source of truth)

**Data (all in `noorup_prefs`):**

| Key | Type | Meaning |
|-----|------|---------|
| `prayer_habit_<yyyy-MM-dd>_<ফজর\|যোহর\|আসর\|মাগরিব\|এশা>` | Boolean | Prayer checked. **Only two states: true / false-or-absent.** |
| `prayer_record_<date>_completed` | Int | Derived count 0–5, rewritten on each toggle. |
| `user_has_logged_<date>` | Boolean | Day was touched by the user. |
| `prayer_tracking_start_date` | String | First date ever toggled. |
| `current_prayer_streak`, `prayer_habit_last_active_date` | Int / String | Cached values. |
| `prayer_transferred_qada_<date>` | Boolean | Day was pushed to Qada. |

**Logic (`NoorUpViewModel`):**
- `toggleHabitPrayer` (l.2590): flips boolean, rewrites count, sets logged + start date, recomputes streak/history/Qada/pending, refreshes widgets. Synchronous, no transaction.
- `computeConsistencyHistory` (l.2381): last 31 days, **only days with `user_has_logged_*`** (today shown if logged or ≥1 completed).
- **Overall Prayer Health** (`AnalyticsScreen` l.6699): `Σ completed / Σ 5` over those logged days (variable names say "7Days"; the window is actually up to 31 days). Unlogged days are excluded.
- `computeCurrentStreak` (l.2428): consecutive days with 5/5, up to 365. Today not yet 5/5 does not break it if yesterday was 5/5. Correct as intended.
- Date keys use device-default `Calendar` / `SimpleDateFormat(Locale.US)`. Keys are stable strings, so a timezone change never rewrites history; it can only shift which key is "today".
- `ensureTodayRollover` handles midnight while the process lives.
- Legacy wipe `cleanLegacyMockDataIfPresent` is a one-shot (`migration_zero_state_clean_v4`) and has already run for every upgrading user. Do not add new wipes.

**Gaps vs the spec:**
1. **No "confirmed missed" state.** Unchecked is indistinguishable from "never recorded".
2. **No completion timestamps** and no "last modified" per prayer.
3. **No per-day history of edits** (corrections).
4. **No "exception" prayer state** exists; Travel/Qasr mode (`is_traveling`) changes rak'ah count only, not status. So "applicable exceptions" = none today.

## 3. Existing Qada system

- Pending counts: `qada_count_<Fajr|Dhuhr|Asr|Maghrib|Isha|Witr>`, fulfilled: `qada_made_up_<prayer>`, `qada_made_up_total`, manual adjustment `qada_manual_offset_<prayer>`, auto `qada_auto_count_<prayer>`.
- Ledger UI: `subTab == 1` in `AnalyticsScreen`; `fulfillQada` / `recordMissedQada` → `updateQada`. Covered by `QadaTrackingTest`.

**Inconsistencies found (documented, NOT changed — per spec §8):**
- **I-1.** `syncAutomaticQadaLedger` (l.2504) counts **every unchecked prayer on every day from the tracking start date up to yesterday** as pending Qada, including days the user never opened the app. This conflicts with the product rule "never classify unrecorded as missed / never auto-create Qada from unrecorded". It also adds Witr whenever Isha is unchecked.
- **I-2.** `transferMissedPrayersToQada` ("Add to Qada" button) increments `qada_count_*`, but the next `syncAutomaticQadaLedger` recomputes `qada_count_* = auto + manualOffset`, so the transfer is overwritten or redundant. The "Missed Prayers Detected" banner therefore does not reliably do what it says.
- **I-3.** The timeline row icon shows a red ✗ for any day below 5/5 (incl. today at 2/5 in the screenshot). This reads as judgment; the redesign softens it without changing the data.

Proposal: leave I-1/I-2 untouched in this feature (Qada stays independent) and list them as a follow-up decision in release notes. The Journey engine will not read Qada at all, and Qada will not read Journey states.

---

## 4. Reuse / modify / create

**Reuse as-is:** `noorup_prefs` + the existing prayer keys, `toggleHabitPrayer`, `computeCurrentStreak`, Overall Health formula, Qada ledger + counts, `TopControlsHeader`, `GlassCard`, `NestedBackHandler`, `TasbihFeedback.isReducedMotion`, `PrayerNotificationScheduler`/`Helper`/`BootReceiver`/`PrayerRescheduleWorker`, `BackupCodec`, `GoldAccent`, `toBanglaDigits`.

**Modify (small, surgical):**
- `AnalyticsScreen` — new section order, hero, enriched Recent Days rows, Friday Reflection entry; Qada ledger and timeline logic untouched.
- `NoorUpViewModel.toggleHabitPrayer` — additionally write per-prayer timestamp + state key (additive); expose a `journeyRevision` tick so the Journey recomputes.
- `PrayerNotificationScheduler` — add Friday Reflection alarm (code 7003) and include it in the cancel-all list; `PrayerNotificationHelper` — one new channel; `PrayerAlarmReceiver` — handle the new alarm; `PrayerRescheduleWorker` — re-arm it.
- `BackupCodec` — add `prayer_state_`, `prayer_ts_`, `journey_`, `reflection_` prefixes.
- `AndroidManifest` — nothing new required (receivers/permissions already declared).

**Create** (new package `com.example.ui.noorup.journey`, pure-Kotlin logic separate from Compose, no new dependencies):
- `JourneyModels.kt` — `PrayerStatus` (UPCOMING, PENDING, COMPLETED, CONFIRMED_MISSED, UNRECORDED), `DayRecord`, `Stage`, `Milestone`, `ReflectionEntry`.
- `PrayerRecordSource.kt` — interface + `PrefsPrayerRecordSource` (read-through over the existing keys; the only reader of raw keys for Journey).
- `JourneyEngine.kt` — daily %, rolling 7/30/90, weekly/monthly/yearly aggregation, stage score, milestones. Pure, deterministic, takes `today` + `ZoneId`.
- `CrescentCanvas.kt` — Compose Canvas crescent/stars/clouds (vector, no assets).
- `CrescentJourneyScreen.kt`, `JourneyAnalyticsScreens.kt`, `FridayReflectionScreen.kt`, `ReflectionStore.kt`, `JourneyPreferences.kt`, `FridayReminderScheduler.kt`.
- Tests under `app/src/test/java/com/example/` (`JourneyEngineTest`, `JourneyMigrationTest`, `ReflectionStoreTest`, `FridayReminderTest`, …).

## 5. Database / migration

- **Production database version: none (SharedPreferences only).** No schema, no Room migration, nothing to version-bump. Documented as the "no schema change" case in spec §13.
- All new data is **additive prefs keys**; no existing key is renamed, rewritten, or deleted.
- New keys: `prayer_state_<date>_<prayerEn>` (`"missed"` only when the user explicitly confirms), `prayer_ts_<date>_<prayerEn>` (Long, set on completion), `journey_*` (prefs, milestones, highest stage, calc version), `reflection_<fridayDate>` (JSON string per entry via existing Gson), `reflection_intention_*`.
- **Legacy mapping, deterministic, no historical rewrite:**
  - `prayer_habit_… = true` → COMPLETED (timestamp unknown, shown as "time not recorded").
  - `= false` on a day with no `user_has_logged_*` → UNRECORDED.
  - `= false` on a logged day → **UNRECORDED** (not "missed") unless a `prayer_state_… = "missed"` exists. This is the conservative reading of the product rules; it only affects the new Journey UI, never existing metrics.
- Upgrade test: seed a v1.5.2-shaped `noorup_prefs`, run the engine, assert every existing key is unchanged and counts/streak/health equal the existing VM output.

## 6. Baseline test status

Fresh baseline `./gradlew testDebugUnitTest` launched in the background; results will be appended to the M1 sign-off. The root-level `test_output.txt` is a stale failed run of unknown cause and is **not** trusted.

---

## 7. Design decisions that need your sign-off

**D1 — Confirmed missed vs unrecorded.** The app has no way to say "I missed this". Proposal: on the existing day-edit dialog add a three-way control per prayer (Completed / Missed / Not recorded). *Missed* writes `prayer_state_…="missed"`; it does **not** touch Qada (Qada stays manual/independent). Legacy unchecked prayers → Unrecorded.

**D2 — Consistency denominator.** Rolling consistency = `completed ÷ (5 × tracked days in window)`, where a *tracked day* has ≥1 record (same meaning as today's Overall Health). Unrecorded prayers inside a tracked day lower the % but are labelled "unrecorded", never "missed". Fully untracked days are excluded and shown as "not tracked". Without this, a user who never taps "missed" would always read 100%.

**D3 — Stage formula (deterministic, versioned `calc_v1`).**
`score = ⅔·r30 + ⅓·r90 + trendAdj` (**CHANGED IN M2 from the approved text:** you approved weights of 0.60 (30-day) and 0.30 (90-day). They sum to 0.9, so a perfect record scored 90 and every score was understated by 10%, caught by `JourneyEngineTest`. The shipped weights are ⅔ and ⅓: same 2:1 ratio, summing to 1, so a perfect record scores 100. Stage thresholds, caps, hysteresis and the ±3 trend limit are unchanged.), where `r30`/`r90` are rolling consistency (D2), `trendAdj = clamp((r30 − r30_previous30)·0.10, −3, +3)` (improvement bonus/damping).
Eligibility: need ≥7 tracked days for Stage 2+, ≥14 for Stage 3+, ≥30 for Stage 5+, ≥60 for Stage 6; otherwise stage is capped (short history can't jump to Eternal Light).
Thresholds as specified: 0–19 / 20–39 / 40–59 / 60–74 / 75–89 / 90–100.
Recovery: displayed stage = from current score with hysteresis (drops only when score is ≥3 points below the threshold). **Highest stage reached** and all milestones are stored forever and never revoked.

**D4 — Milestones.** "Tracking day" = a day with ≥1 record. First complete day = first 5/5. First complete week = first run of 7 consecutive tracked days (rewards participation, not perfection). 30/90/180/365 = total tracked days. Unlock date = date the condition first became true; stored once.

**D5 — Friday Reflection window.** Previous seven completed local days, Friday→Thursday (Friday's own record excluded, as specified), generated on/after Friday.

**D6 — Evening review / pending check-in.** The existing `garden_reminders_channel` evening reminder already acts as the evening review. Proposal: reuse it (add an "only if prayers remain unrecorded" check) rather than adding a second evening notification; add only the Friday Reflection and weekly-intention reminders as new alarms.

---

## 8. Milestone plan (each verified before the next)

| M | Scope | Key risks |
|---|-------|-----------|
| M2 | Stats screen redesign: hero (static crescent from real data), summary, nav buttons, enriched Recent Days, Friday entry (disabled-free: opens the real screen once M6 exists, so M2 ships the entry **after** M6 or as a real "Weekly summary" sheet) | Don't break timeline/Qada; keep `LazyColumn` performance |
| M3 | Five stars bound to records, daily illumination, tri-state edit control (D1), `prayer_ts_` writes | Widget count key must keep updating |
| M4 | Six stages, constellations, milestones (D3/D4) | Determinism, hysteresis tests |
| M5 | Daily/weekly/monthly/yearly analytics + per-prayer filters | Query cost → aggregate once per `revision`, cache in memory only |
| M6 | Friday Reflection module + history | Edit/delete, duplicate prevention by Friday-date key |
| M7 | Notifications, preferences, animation/reduced motion, i18n | Alarm re-arm on boot/timezone; backup whitelist |
| M8 | Test suite, perf, a11y pass | Robolectric heap limit already tuned |
| M9 | Release notes, known limitations, Play checklist, version bump | — |

**Scoping note.** M2's "Friday Reflection entry point" and "navigation to full Crescent Journey" must not be placeholder buttons (rule 8). Proposed sequencing to honour that: M2 builds the hero + redesigned Stats with the Journey/Friday entry rows hidden until their destinations exist (M3/M6), then each milestone turns its entry on. The redesigned Stats screen is fully real at every step.

## 9. Constraints honoured from the spec
No new bottom tab · no new dependencies (Canvas instead of Lottie) · no duplicate tracker/Qada/repository · no hardcoded prayer data · Qada untouched · timeline untouched · backup whitelist extended · only additive prefs keys · progress recorded here after each milestone.

---

# M2 — Stats screen redesign (complete, awaiting approval for M3)

## Files created
- `app/src/main/java/com/example/ui/noorup/journey/JourneyModels.kt` — `Prayer`, `PrayerStatus`, `PrayerRecordSource`, `DayRecord`, `Rolling`, `Stage`, `StageResult`, `JourneySummary`.
- `.../journey/JourneyEngine.kt` — pure, deterministic calc v1: daily %, rolling windows, history caps, stage score with hysteresis.
- `.../journey/PrefsPrayerRecordSource.kt` — read-only adapter over the existing `noorup_prefs` prayer keys.
- `.../journey/CrescentHero.kt` — Canvas hero (crescent, glow, fixed-seed sky, five stars, twinkle off when animations are removed), `PrayerStar`, `PrayerDayIndicators`, `statusLabel`.
- `app/src/test/java/com/example/JourneyEngineTest.kt`, `JourneyPrefsSourceTest.kt`.
- `docs/crescent_journey/m2_stats_hero.png`, `m2_recent_days.png` (emulator screenshots, seeded sample data).

## Files modified
- `NoorUpScreens.kt` — `AnalyticsScreen` only: hero inserted under the title; summary card, missed-prayers banner, Timeline/Qada buttons and day-edit dialog untouched; Recent Days rows enriched (five indicators, legend, row accessibility description, neutral moon icon instead of the red ✗).
- `docs/CRESCENT_JOURNEY_M1_AUDIT.md` — D3 weight correction and this section.

## Database / data changes
None. No keys written, renamed or deleted. M2 only reads. (`reading the journey changes no stored key` is a test.) Backup whitelist needs no change yet because nothing new is stored.

## Behaviour notes
- Hero shows today's records: completed = gold star, not yet completed = neutral "pending" star (UPCOMING by prayer time arrives in M3).
- Rolling 30-day consistency covers fully elapsed days only (today excluded) and ignores untracked days. Overall Prayer Health keeps its existing formula and still includes today, so the two figures can legitimately differ.
- Stage uses calc v1 with history caps: with fewer than 7 tracked days the stage stays First Light however good the score (seen on the emulator: 75% consistency over 4 tracked days shows Stage 1).
- The hero and the Journey/Friday entry points are not clickable: nothing placeholder. Friday Reflection entry stays hidden until M6; navigation to the full Journey arrives in M3.

## Tests executed (real results)
| Run | Result |
|-----|--------|
| `JourneyEngineTest` | 22 / 22 pass (after fixing the D3 weights; the first run had 3 failures that exposed the 0.60/0.30 weight bug) |
| `JourneyPrefsSourceTest` | 6 / 6 pass |
| `QadaTrackingTest` | 7 / 7 pass |
| Full `testDebugUnitTest` (earlier run, before the D3 fix, on a tree that also contained another session's tasbih work) | 249 tests, 3 failed (the 3 JourneyEngineTest cases above), 1 skipped. The skipped test was not investigated. A full-suite re-run after the fix has **not** been done. |
| `assembleDebug` | BUILD SUCCESSFUL |

## Verified on device
Pixel 8 API 36 emulator, debug build installed. Empty state and seeded state (5 days of records, one explicit "missed") both rendered. Hero numbers matched hand calculation (30-day 75% = 15 of 20; Overall Health 68% = 17 of 25). Bangla layout of the hero was not checked on a device. Dark theme only; light theme not checked.

## Known issues / follow-ups
1. The existing "Missed Prayers Detected" banner and Qada auto-ledger still treat unchecked prayers as missed (audit items I-1, I-2). Left unchanged on purpose.
2. Nothing in the app can write `prayer_state_*` yet, so "confirmed missed" cannot appear for real users until M3 adds the three-way control.
3. The Recent Days legend does not mention today's open "pending" ring.
4. The hero is tall (about 40% of the screen on a 1080x2400 device); acceptable, can be tightened.
5. Full-suite test re-run after the D3 fix still to do before release.
