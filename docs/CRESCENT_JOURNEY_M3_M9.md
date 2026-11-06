# Crescent Journey — M3 to M9 build report

Continues `CRESCENT_JOURNEY_M1_AUDIT.md` (audit, decisions D1–D6, M2). Built on v1.5.2 + the unreleased tasbih widget work that shares this working tree.
**Nothing is committed and the app version is not bumped** (that is a release decision; see §M9).

## Result at a glance

| Milestone | Built | Verified how |
|-----------|-------|--------------|
| M3 Dynamic crescent | Yes | Unit tests + emulator |
| M4 Celestial progression | Yes | Unit tests + emulator |
| M5 Analytics and history | Yes | Unit tests + emulator |
| M6 Friday Reflection | Yes | Unit tests + emulator |
| M7 Notifications and personalisation | Yes | Unit tests + emulator (permission flow, alarm scheduled); **notification firing not observed on a device** |
| M8 Testing | Yes | Full suite: see §M8 |
| M9 Release prep | Docs and checklist written; version not bumped | — |

## M3 — Dynamic crescent integration
- **Full-screen route** `CrescentJourneyScreen`, opened by tapping the Stats hero (nested route in the Stats tab, no new bottom tab). Back closes the current section first, then the route. State lives in `NoorUpViewModel.journeyRoute`, so notifications can deep-link into it.
- **Five stars** bound to the tracker. Statuses: upcoming (outline, by real prayer time for the selected city), pending, completed (gold, glow, fades in; breathes softly when motion is on), marked missed (dimmed), not recorded (dashed outline). Tap a star to inspect it.
- **Three-way editor** (D1): Completed / Missed / Not recorded, on the Today screen, in Daily history and in the existing day-edit dialog. Missed is only ever set by the user and never creates Qada.
- **One write path.** `toggleHabitPrayer` now delegates to a new `setHabitPrayer(date, prayer, value)`; the editor uses the same path, so streak, Qada sync, widgets and the `prayer_record_*` count stay consistent.
- Daily illumination = completed / 5; animates; updates the instant a status changes.

## M4 — Celestial progression
- Six stage environments drawn with Canvas only (no assets, no new dependency): sky colours, stars, clouds, horizon glow, constellation lines, aurora, harmony rings, rays, increasing with stage.
- Stage calculation `calc_v1` (see D3 in the audit; **weights are ⅔ / ⅓, not the approved 0.60 / 0.30**, because those summed to 0.9). Highest stage reached and shown stage are stored; milestones are stored once and never revoked.
- Six constellations (Dawn Star, Seven Stars, Crescent Arc, Lantern, Minaret Crown, Eternal Circle) with a gallery, dates, history and a discovery dialog. If several unlock at once, only the newest is celebrated and the rest are marked seen.

## M5 — Analytics and history
- **Daily:** last 30 days, expandable, with per-prayer status, completion time ("time not recorded" for older records), last-changed time, and editing.
- **Weekly:** seven completed days, bars, totals (completed / marked missed / not recorded / full days / tracked / untracked), prayer-by-prayer, comparison with the previous week, last six weeks.
- **Monthly:** calendar heatmap, totals, per-prayer consistency, comparison with the previous month.
- **Yearly:** twelve-month bars, tracking history, steadiest month, milestones in the year, long-term progression.
- Per-prayer filter on weekly / monthly / yearly. Untracked days are shown as untracked, never as missed.

## M6 — Friday Reflection
- Entry card on Stats (Section F): availability, previous reflection, last week's summary, History.
- Screen "Your Weekly Journey": summary for the seven completed days Friday to Thursday (the Friday itself is never counted), comparison with the previous week, three optional questions, one editable/deletable intention, privately stored.
- One entry per Friday (upsert by date, creation time kept). History with edit and delete (confirmation). Intentions are never marked achieved or failed.

## M7 — Notifications and personalisation
- **Friday reminder** and **weekly intention reminder** (Mondays): opt-in, own gentle channel, plain exact-while-idle alarms (no alarm-clock status-bar icon) with an automatic inexact fallback when exact alarms are off. Re-armed by the existing boot / app-update / timezone-change / 6-hourly paths via `scheduleDailyTasks`. One post per Friday; skipped when this week's reflection is saved (setting); paused when prayer notifications are off.
- **Evening review:** the existing evening Noor Garden reminder now stays quiet once all five prayers are completed (setting, on by default). No second evening notification was added.
- Notification permission flow (Android 13+), honest status and shortcuts for notification settings, exact alarms and battery.
- Settings: animations, reduced motion (also respects Android "remove animations"), reminders. Language and theme stay on the existing header toggles (no duplicate settings).
- English and Bangla throughout, including Bangla digits and Bangla month and weekday names.
- Backup whitelist extended: `prayer_state_`, `prayer_ts_`, `prayer_mod_`, `journey_`, `reflection_`, `friday_`, `intention_`. Transient `last_*_posted` keys are not backed up. R8 keep rule added for `ReflectionEntry`.

## Files created
`app/src/main/java/com/example/ui/noorup/journey/`: `JourneyModels.kt`, `JourneyEngine.kt`, `PrefsPrayerRecordSource.kt`, `JourneyStore.kt`, `FridayReminderScheduler.kt`, `CelestialArt.kt`, `CrescentHero.kt`, `CrescentJourneyScreen.kt`, `JourneyAnalyticsUi.kt`, `JourneyUi.kt`, `JourneySettings.kt`, `FridayReflectionScreen.kt`.
Tests (`app/src/test/java/com/example/`): `JourneyEngineTest`, `JourneyAnalyticsEngineTest`, `JourneyPrefsSourceTest`, `JourneyRecordEditTest`, `JourneyStoreTest`, `JourneyFormatTest`.
Docs: this file, `docs/crescent_journey/*.png`.

## Files modified (Journey-related edits only)
`NoorUpScreens.kt` (Stats screen only), `NoorUpViewModel.kt` (`setHabitPrayer`, `setPrayerRecord`, completion/last-change metadata, `journeyRevision`, `journeyRoute`, today timing and prayer window), `PrayerNotificationHelper.kt` (journey channel, route extra), `PrayerNotificationScheduler.kt` (cancel/re-arm hooks), `PrayerAlarmReceiver.kt` (Friday/intention branches, evening skip), `MainActivity.kt` (journey deep-link branch), `BackupCodec.kt`, `proguard-rules.pro`.
(`MainActivity.kt`, `NoorUpViewModel.kt` and `NoorUpScreens.kt` also contain the other session's tasbih edits.)

## Database and data changes
- **No database exists** (SharedPreferences only), so no migration and no schema version.
- **Additive keys only:** `prayer_state_<date>_<Prayer>`, `prayer_ts_…`, `prayer_mod_…`, `journey_*`, `reflection_<friday>`, `friday_*`, `intention_*`, plus the transient `last_friday_reflection_posted` / `last_intention_reminder_posted`.
- **No existing key is renamed, rewritten or deleted.** Legacy records map as: checked = completed; unchecked on a past day = not recorded (never missed). Old records simply have no completion time.
- Upgrade safety is covered by `JourneyRecordEditTest` ("a pre-journey installation upgrades without changing any stored value") and `JourneyStoreTest` ("persist is idempotent and writes only journey keys").

## M8 — Testing
**Final result:** the full unit suite (`./gradlew testDebugUnitTest --rerun-tasks`) passed **3 times in a row**: **330 tests, 0 failures, 0 errors, 1 skipped**. The one skip is `ExampleRobolectricTest > launch MainActivity`, which carries a pre-existing `@Ignore` ("Hangs offline on activity setup"). Of the 330, **92 are Crescent Journey tests** (`JourneyEngineTest` 22, `JourneyAnalyticsEngineTest` 20, `JourneyPrefsSourceTest` 6, `JourneyRecordEditTest` 11, `JourneyStoreTest` 27, `JourneyFormatTest` 6); `QadaTrackingTest` (7) is unchanged and passes.

Along the way: the first run found a real formula bug (score weights summing to 0.9, fixed), a test that compared unrelated prefs keys, and two tests that raced the Application's startup alarm thread (both fixed in the tests, not the product). One run also had a single failure in the pre-existing `PrayerAlarmOnboardingTest` that did not reproduce (see Known issues, item 12).

Coverage by area:
- **Prayer tracking:** completing each prayer, completed ↔ missed ↔ not recorded, past-day edits leave today alone, future dates ignored, timestamps, revision bumps, Qada untouched (`JourneyRecordEditTest`).
- **Crescent / stages:** daily %, timed upcoming/pending, thresholds, history caps, hysteresis, trend clamps, recovery, determinism (`JourneyEngineTest`, `JourneyAnalyticsEngineTest`).
- **Analytics:** week / month / year windows, untracked vs unrecorded vs missed, per-prayer tallies, incomplete history.
- **Friday Reflection:** window (Friday to Thursday, Friday excluded), previous week, create / edit / duplicate prevention / delete / history / intention clear / corrupt data (`JourneyStoreTest`).
- **Notifications:** next-trigger maths incl. timezone change and DST, arm / idempotent / cancel, inexact fallback, master switch, once-per-Friday, skip when done, intention window (`JourneyStoreTest`).
- **Existing functionality:** `QadaTrackingTest` and the whole pre-existing suite.
- **Not automated:** instrumented/UI tests (the project has none), device reboot, real notification delivery, TalkBack, low-end device profiling.

## M9 — Release preparation
- **Version:** not bumped. The working tree also holds the tasbih widget work, so versionCode / versionName and the release notes title should be set once, together.
- **Draft release notes:** `RELEASE_NOTES_Crescent_Journey_DRAFT.md`.
- **Play checklist:**
  - [ ] Decide the version and bump `versionCode` / `versionName`.
  - [ ] Decide commit and branch strategy together with the tasbih work; none of this is committed.
  - [ ] Native Bangla review of the six stage names and strings.
  - [ ] Build the signed release (R8 on) and check Journey, Friday history (Gson + R8 keep rule) and the Stats screen on a release build.
  - [ ] Upgrade test from the published 1.5.2 build on a device with real data (installed over it, not a fresh install).
  - [ ] Real-device check of the Friday reminder (exact alarms on and off), reboot and timezone change.
  - [ ] Data Safety form: reflections are stored on the device and included in the user's own passphrase-encrypted backup; nothing is sent to a server.
  - [ ] Update the store listing screenshots (`docs/crescent_journey/`).

## Known issues and limitations (honest list)
1. **Existing Qada behaviour left as is** (audit I-1, I-2): the "Missed Prayers Detected" banner and the Qada auto-ledger still treat unrecorded prayers as missed. Journey never reads or writes Qada.
2. **Notification firing not seen on a device.** On the emulator exact alarms are off; the Friday alarm was seen scheduled for 20:00 local via `dumpsys alarm`, and posting logic is unit-tested, but I did not watch a notification arrive.
3. **Reboot and timezone-change re-arming** rely on the existing receivers/worker; unit-tested for the maths and re-arming, not exercised on a device.
4. **No performance profiling on a low-end device.** By design: Canvas only, no assets, infinite animations only while motion is allowed and a star is completed, one snapshot per data change plus a once-a-minute tick, history scans bounded (800 days). Not measured.
5. **Accessibility:** touch targets are at least 48dp, every star/day/chart has a content description, text uses sp and was checked at 1.6x font scale in Bangla on the emulator. **TalkBack was not run.**
6. **Light theme:** page backgrounds follow the app theme; the "sky" surfaces stay night-blue in both themes on purpose.
7. **Bangla wording** of the stage names is mine and needs a native review (e.g. "আসমানি সুর" is natural but means "heavenly melody"; "আসমানি ঐকতান" is closer to "harmony").
8. **Reflection text is plain SharedPreferences** like all other app data (no new encryption); it is included in the existing encrypted backup. `allowBackup` stays false.
9. The Stats hero is tall (about 40% of the screen).
10. Milestones are awarded retroactively from existing history the first time the Journey is opened.
11. The ViewModel computes `today` from the device date; a city in another timezone that has crossed midnight uses device-date records (upcoming/pending timing is skipped in that case).
12. Pre-existing flaky test: `PrayerAlarmOnboardingTest > individual prayer opt-outs…` failed once in one full run and passed on three isolated runs and two further full runs; it appears to race the Application's background alarm scheduling and is unrelated to this work.
