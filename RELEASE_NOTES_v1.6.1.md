# NoorUp v1.6.1 (versionCode 21) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 20 (1.6.0), so this build uses 21.

## EN — What's new in 1.6.1

Continue reading now follows the Ayat you last read. Browsing a previous day in Prayer Times shows and saves that day correctly. Home widgets refresh more reliably after prohibited times end and when you unlock your phone. In Tasbih you can now reorder your zikr, and your own custom zikr are saved reliably. Bookmarks and tasbih counts stay in sync with the Daily Verse and Tasbih widgets.

## BN — ১.৬.১-এ নতুন কী

"পড়া চালিয়ে যান" এখন সর্বশেষ পড়া আয়াত থেকে শুরু হয়। নামাজের সময়ে আগের দিন দেখলে সেই দিনের তথ্যই দেখায় ও সংরক্ষণ হয়। নিষিদ্ধ সময় শেষ হওয়ার পর এবং ফোন আনলক করলে হোম উইজেট আরও নির্ভরযোগ্যভাবে আপডেট হয়। তসবীহে যিকিরের ক্রম বদলানো যায়, আর নিজের যোগ করা যিকির এখন নিশ্চিতভাবে সংরক্ষিত থাকে। বুকমার্ক ও তসবীহ গণনা ডেইলি ভার্স ও তসবীহ উইজেটের সাথে সিঙ্ক থাকে।

## Included changes

### Fixes
- **Continue reading** (Quran tab, Home, Favorites) no longer sticks at Ayat 1. The reader now records the Ayat being read.
- **Prayer day navigation.** Going to a previous day reloads that day's prayer checks. Before, it showed today's ticks and could write edits to the wrong day.
- **Widget refresh timing.** The widget scheduler wakes the minute after a prohibited window ends (windows include their last minute), uses the selected city's timezone and the same coordinates as the widget, and refreshes on screen-on and unlock. Journey "Missed" / "Not recorded" edits now refresh the widgets.
- **Widget streak** is computed live and uses the device date for today's count.
- **Widget sync.** In-app bookmarks refresh the Daily Verse widget, widget bookmark changes re-sync an open reader, and in-app tasbih counts refresh the Tasbih widget.
- **Custom zikr disappearing in release builds.** Custom zikr are now saved with fixed JSON keys, so R8/ProGuard renaming can no longer make saved zikr unreadable.

### Tasbih
- **Reorder zikr.** Up/down arrows in Manage Zikr reorder both the built-in and your own zikr. The Tasbih widget's ‹ › buttons follow the same order.
- **"Tasbih of Fatimah" built-in removed.** If it was your active zikr, the app falls back to the first built-in zikr. You can recreate it as a custom zikr.

### Good to know
- No data migration is needed. Existing custom zikr, counts, history, Qada, streak and Journey records are unchanged.
- Tests were added for each fix.

### Store listing
- Play Store screenshots were rebuilt from real app screens (12 images, `store-assets/playstore/real/`). The old hand-drawn mockups were removed.
