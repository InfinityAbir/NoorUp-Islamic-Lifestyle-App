# NoorUp v1.2.7 (versionCode 14) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Version 13 (1.2.6) was uploaded to Play but never attached to a release, so it
is inactive. Play does not allow deleting bundles or reusing a version code;
this build uses 14 instead.

## EN — What's new in 1.2.7

NoorUp now fits Android 15 edge-to-edge screens properly: content stays clear of the status and navigation bars, and system icons stay readable in light and dark themes. The prayer timetable has been rebuilt so Sunrise, Zawal and Sunset cards no longer overlap or clip on small phones or with large text; badges stay on one line and time ranges remain fully readable.

## BN — ১.২.৭-এ নতুন কী

Android ১৫-এ কনটেন্ট স্ট্যাটাস ও নেভিগেশন বারের নিচে হারায় না; হালকা/গাঢ় থিমে সিস্টেম আইকন স্পষ্ট থাকে। নামাজের সময়সূচি নতুন করে সাজানো হয়েছে—ছোট ফোনে বা বড় লেখায় সূর্যোদয়, যাওয়াল ও সূর্যাস্ত কার্ড আর ওভারল্যাপ বা কাটা পড়ে না; ব্যাজ এক লাইনে থাকে এবং সময় পুরোপুরি পড়া যায়।

## Included changes

### Android 15 edge-to-edge
- Replaced `androidx.activity.enableEdgeToEdge()` with an app-owned helper
  (`ui/theme/EdgeToEdge.kt`). Activity 1.13.0's helper called the deprecated
  `Window.setStatusBarColor()` / `setNavigationBarColor()` on every API level
  and set `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` on API 28+; those calls
  were the Play Console "Deprecated APIs" reports.
- API 26–34 still draw behind the bars via
  `WindowCompat.setDecorFitsSystemWindows(window, false)`. API 35+ is
  edge-to-edge by platform default, so no window call is made there.
- API 30–34 use the non-deprecated
  `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`; `safeDrawing` insets keep controls
  clear of cutouts.
- `isNavigationBarContrastEnforced = true` keeps three-button navigation
  legible over the bottom bar; gesture navigation stays transparent.
- Status-bar and navigation-bar icon appearance are both driven by the Compose
  theme, so icons have correct contrast in light and dark themes.

### Prayer timetable layout
- Timeline cards use `BoxWithConstraints` with weighted content instead of a
  rigid row, so cards grow vertically instead of clipping.
- Prayer name and badge share a `FlowRow`; badges are single-line and never
  wrap character-by-character, even for long Bangla labels.
- Prohibited-time reasons wrap in full; Arabic text uses RTL direction.
- Time ranges stack below the content on narrow widths and stay side-by-side
  on wider phones.
- The three prohibited-time chips (Sunrise, Zawal, Sunset) stack vertically on
  narrow widgets or large font scales.
- Noor Garden row and blossom labels made weight-based and line-limited.
- Thresholds: timetable stacks below 320dp content width or font scale ≥ 1.3;
  chips stack below 340dp strip width or font scale ≥ 1.3.

### Tests
- New `PrayerTimelineResponsiveTest` covers 320/360/390/412dp, large font
  scales, and invalid widths.

### Packaging
- Excluded the unused `libandroidx.graphics.path.so` that Compose ui-graphics
  pulls in through `androidx.graphics:graphics-path`. The library is only
  reached via `Path.iterator()`, `PathHitTester` and `Path.toSvg()`, none of
  which this app uses, and its prebuilt binary ships stripped with no debug
  symbols. Removing it keeps native code out of the bundle, so Play no longer
  asks for a symbol file.

## Pre-upload checklist

- [ ] Build and sign `:app:bundleRelease` with the release upload key; confirm
      the bundle reports versionCode 14 / versionName 1.2.7.
- [ ] Confirm the bundle contains no `lib/*/*.so` entries (no native code, so
      no debug-symbol warning at upload).
- [ ] Install on a real phone: fresh install and update-over-1.2.5.
- [ ] Android 15+: verify Home content is not hidden behind the status bar or
      navigation bar with both gesture and three-button navigation.
- [ ] Switch light/dark theme and confirm status-bar and navigation-bar icons
      stay readable.
- [ ] Check the prayer timetable at 320/360/390/412dp, with font scale ≥ 1.3,
      and in Bangla and English: Sunrise, Zawal and Sunset cards must not
      overlap or clip; badges must stay on one line.
- [ ] Scroll to the bottom of Home and confirm the last card clears the bottom
      navigation bar.
- [ ] Upload version code 14 to **Production**, review the EN/BN notes, start a
      staged rollout, then tag `v1.2.7`.
- [ ] After Play reprocesses the bundle, confirm the **Deprecated APIs** report
      no longer lists the edge-to-edge warnings.
