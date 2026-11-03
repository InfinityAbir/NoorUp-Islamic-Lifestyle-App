# NoorUp v1.5.0 (versionCode 17) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 16 (1.4.0), so this build uses 17.

## EN — What's new in 1.5.0

Easier to read and easier to find things. The Quran has bigger, centered Arabic with a separate Bismillah header, A+/A- text size, Juz browsing, and Sepia or Night reading themes. Home now has a search bar and quick-access grid, and Tools is now Explore, with every feature described and grouped. New first-run tour. Pick a city by search, with more cities worldwide.

## BN — ১.৫.০-এ নতুন কী

পড়া ও খোঁজা আরও সহজ। কুরআনে বড় ও মাঝ-সারির আরবি, আলাদা বিসমিল্লাহ, A+/A- লেখার আকার, পারা ব্রাউজ এবং সেপিয়া বা নাইট রিডিং থিম। হোমে সার্চ বার ও দ্রুত অ্যাক্সেস গ্রিড, আর সরঞ্জাম এখন এক্সপ্লোর, যেখানে সব ফিচার বর্ণনাসহ সাজানো। নতুন ব্যবহারকারীর জন্য পরিচিতি। সার্চ করে শহর বাছুন, বিশ্বের আরও শহরসহ।

## Included changes

- **Quran reading**: Arabic is 30sp and centered, with the Bismillah as its own header above ayah 1 instead
  of being fused into the first ayah (the bundled text had it joined for 111 surahs; the translation and
  pronunciation data never included it, so 2:1 now reads just "الم"). A+/A- size control, Surah | Para |
  Bookmarks switch with the 30 standard Juz start points, and Sepia / Night reading themes that style only
  the open surah (status bar and backdrop included). Size, theme and Bengali pronunciation live in one
  "Reading settings" dialog behind the **Aa** button. Compact reader header; the audio panel is collapsed
  until opened; skeleton placeholders while a surah loads.
- **Home**: search bar, 4 x 2 quick-access grid (Quran, Duas, Hadith, Ask AI, Tasbih, Qibla, Zakat, More),
  one-tap Continue reading card. The prayer-alarm card moved under the prayer hero so the next prayer is the
  first thing seen.
- **Explore** (was Tools): grouped, described cards (Worship, Tasbih & Dhikr, Read & learn, Utilities &
  progress). Inside a tool there is a back link and its title; Digital Tasbih and Family Dhikr share a switcher.
  The top search icon is hidden where a full search field is already on screen.
- **First run**: 4-step tour with a language choice for new installs; a one-time "What's new" sheet for
  updates; one-time hints (reader controls, home widget after two days of use).
- **Location picker**: GPS first, manual city list collapsed behind a searchable row. Makkah and Madinah no
  longer appear under Bangladesh. Added Dubai, Karachi, Delhi, Istanbul, Kuala Lumpur, Singapore, Toronto, Sydney.
- Polish: tab crossfade, emerald selected-chip colour across the app, consistent empty states.
- Tests: `QuranStandardsTest` (Juz table, Bismillah split). `MaghribReceiverTest` no longer depends on the
  date it runs on (it computes the Hijri day offset instead of hard-coding one for 30 Oct 2026).

## Known issues

- Tajweed colouring is not included (no tajweed-annotated text in the bundle).

## Play Console checklist

- [ ] Update screenshots: Home (search + grid), Explore, Quran reader (new Arabic size, Sepia theme).
- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify` shows versionCode 17 /
      versionName 1.5.0).
- [ ] Smoke test the signed build on a phone: fresh install shows the tour; open Al-Baqarah, check 2:1 shows
      "الم" under a Bismillah header; change text size and theme under **Aa**; pick a city manually.
