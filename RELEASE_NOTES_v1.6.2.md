# NoorUp v1.6.2 (versionCode 22) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 21 (1.6.1), so this build uses 22.

## EN — What's new in 1.6.2

Your Tasbih goal is now remembered for each zikr. If you set a custom goal for Astaghfirullah, switching to another zikr and back, using the widget's ‹ › buttons, or resetting no longer puts the original number back. Each zikr keeps its own goal until you change it.

## BN — ১.৬.২-এ নতুন কী

তসবীহে এখন প্রতিটি যিকিরের লক্ষ্য আলাদাভাবে সংরক্ষিত থাকে। আপনি আস্তাগফিরুল্লাহর জন্য নিজের লক্ষ্য ঠিক করলে অন্য যিকিরে গিয়ে ফিরে এলে, উইজেটের ‹ › বাটন চাপলে বা রিসেট করলে আগের ডিফল্ট সংখ্যা আর ফিরে আসবে না। আপনি নিজে না বদলানো পর্যন্ত লক্ষ্য একই থাকবে।

## Included changes

### Fixes
- **Tasbih goal reset.** The goal you set on a built-in zikr was stored as one shared value for whichever zikr was active, so picking another zikr, using the widget's ‹ › buttons, or resetting from a later stage replaced it with that zikr's preset. Goals are now saved per zikr (`tasbih_goal_<id>#<stage>`) and used whenever that zikr is selected, reset, or switched to from the widget.

### Good to know
- No data migration is needed. Existing counts, history, custom zikr and family goals are unchanged. Built-in zikr start with their preset goal until you change it.
- Custom zikr and Family Dhikr goals already persisted correctly and are unchanged.
- A test was added for switching zikr and back.
