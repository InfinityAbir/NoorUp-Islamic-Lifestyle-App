# NoorUp v1.2.2 (versionCode 9) — Release Notes

Paste the EN block into Play Console (English) and the BN block (বাংলা). Keep each under 500 characters.

## EN — What's new in 1.2.2

The home widget is now fully dynamic: no wasted space below, two-column on wide screens and stacked on narrow ones. Countdown, prayer times, and prohibited times stay visible in Bangla and English, including larger fonts. Prayer times and all features unchanged. Please update!

## BN — ১.২.২-এ নতুন কী

হোম উইজেট এখন সম্পূর্ণ ডাইনামিক: নিচে ফাঁকা জায়গা নেই, চওড়া স্ক্রিনে দুই কলাম ও সরু স্ক্রিনে স্ট্যাকড লেআউট। বাংলা ও ইংরেজিতে, বড় ফন্টেও কাউন্টডাউন, নামাজের সময় ও নিষিদ্ধ সময় সম্পূর্ণ দেখা যাবে। নামাজের সময় ও সব ফিচার অপরিবর্তিত। আপডেট করুন!

## What's in this build (versionCode 9)

- Widget root is content-driven (`wrap_content`, no fixed container heights or `minHeight`): bottom edge follows the prohibited-times bar, no hardcoded height correction.
- Responsive cards: two-column when width allows, stacked narrow layout below 320dp via launcher width (`onAppWidgetOptionsChanged` re-selects layout on resize).
- Prayer rows and prohibited-times bar grow only when text wraps; compact when everything fits.
- No changes to prayer-time calculations, countdown logic, Hijri dates, notifications, or any other feature.

## Release checklist (do not ship without these)

- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify` shows versionCode 9)
- [ ] Installed the signed AAB/APK on 2 real phones: fresh install + update-over-old-data
- [ ] Home widget added fresh: no empty space below prohibited bar, countdown fully visible in English AND Bangla
- [ ] Home widget resized narrow: cards stack, all 5 prayers + all 3 prohibited intervals visible
- [ ] Home widget at largest system font: no clipped/overlapped text
- [ ] Toggled language with widget on screen; times and layout update correctly
- [ ] Staged rollout (start ~20%), then commit + tag `v1.2.2`
