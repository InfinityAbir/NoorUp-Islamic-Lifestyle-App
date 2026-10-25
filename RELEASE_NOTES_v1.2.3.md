# NoorUp v1.2.3 (versionCode 10) — Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

## EN — What's new in 1.2.3

Learn Quran reading with an optional Bengali pronunciation line for verses. Personalize Digital Tasbih: add, edit, and save your own zikr and targets, with progress kept after reopening the app. Completion now has a gentle celebration and haptic feedback on supported devices. Dua search is more reliable across Bengali, English, Arabic, categories, and common keywords.

## BN — ১.২.৩-এ নতুন কী

কুরআনের আয়াতের সাথে ঐচ্ছিক বাংলা উচ্চারণ যোগ হয়েছে। এখন নিজের জিকির ও লক্ষ্য যোগ, সম্পাদনা ও সংরক্ষণ করতে পারবেন; অ্যাপ বন্ধ করলেও অগ্রগতি থাকবে। লক্ষ্য পূরণে সমর্থিত ডিভাইসে সুন্দর উদযাপন ও হ্যাপটিক ফিডব্যাক পাবেন। বাংলা, ইংরেজি, আরবি, ক্যাটাগরি ও সাধারণ কীওয়ার্ডে দোয়া সার্চ আরও নির্ভরযোগ্য হয়েছে।

## Included changes

- Optional, per-verse Bengali pronunciation from the bundled transliteration dataset.
- Persistent custom Tasbih/Zikr creation, editing, deletion, targets, reset, and completion state.
- Completion feedback that respects device haptic settings and works safely when vibration is unavailable.
- Dua filtering with stable dataset-derived categories, whitespace/case normalization, Bengali and Arabic-aware matching, and tag search.

## Pre-upload checklist

- [ ] Build and sign `:app:bundleRelease` using the release upload key.
- [ ] Install on a real phone and verify Quran pronunciation, custom Tasbih persistence, and completion vibration.
- [ ] Verify Dua results for Bengali, English, Arabic, and category-plus-search.
- [ ] Test update-over-existing-1.2.2 data before rollout.
- [ ] Upload version code 10, begin a staged rollout, then tag `v1.2.3`.
