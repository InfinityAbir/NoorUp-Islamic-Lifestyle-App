# NoorUp v1.2.1 (versionCode 8) — Release Notes

Paste the EN block into Play Console (English) and the BN block (বাংলা). Keep each under 500 characters.

## EN — What's new in 1.2.1

Bug-fix update: the home widget's prayer countdown and prohibited times now stay fully visible in Bangla and English — on small screens and larger fonts too. Prayer times and all features unchanged. Please update!

## BN — ১.২.১-এ নতুন কী

বাগ-ফিক্স আপডেট: হোম উইজেটের কাউন্টডাউন ও নিষিদ্ধ সময় এখন বাংলা ও ইংরেজিতে সম্পূর্ণ দেখা যাবে — ছোট স্ক্রিন ও বড় ফন্টেও। নামাজের সময় ও সব ফিচার অপরিবর্তিত। আপডেট করুন!

## What's in this build (versionCode 8)

- Active-prayer card is content-driven: the countdown (e.g. "Ends in 4h 54m") stays fully visible in English and Bangla, never overlapped by the garden progress section.
- Prohibited-times bar wraps naturally (sunrise • zawal • sunset all shown); compact on wide widgets, grows only as needed on narrow screens or larger fonts.
- Widget default size raised to 4×4 cells (`minHeight` 230dp) to fit the full content; resizable as before.
- No changes to prayer-time calculations, notifications, or any other feature.

## Release checklist (do not ship without these)

- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify` shows versionCode 8)
- [ ] Installed the signed AAB/APK on 2 real phones: fresh install + update-over-old-data
- [ ] Home widget added fresh: countdown fully visible in English AND Bangla
- [ ] Home widget at largest system font: no clipped/overlapped text, all 3 prohibited intervals shown
- [ ] Toggled language with widget on screen; times and layout update correctly
- [ ] Staged rollout (start ~20%), then commit + tag `v1.2.1`
