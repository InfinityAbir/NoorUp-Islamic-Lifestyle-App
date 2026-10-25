# NoorUp v1.2.0 (versionCode 7) — Release Notes draft

Paste the EN block into Play Console (English) and the BN block (বাংলা). Keep each under 500 characters.

## EN — What's new in 1.2.0

Prayer alerts you can trust: per-prayer toggles, honest status, no duplicates — only the latest stays in your shade.

Quran shows only verified offline verses (error + retry instead of guesses). Two small Hadith books are labeled partial samples. Tapping the daily Hadith opens it in the app. Tasbih goals are customizable (33/100/500/1000 or any number). The AI tab is now Islamic Assistant with more offline topics. Family Zikr removed; your data is untouched — please update!

## BN — ১.২.০-এ নতুন কী

ভরসাযোগ্য নামাজ অ্যালার্ট: প্রতি ওয়াক্তে আলাদা টগল, সৎ স্ট্যাটাস, ডুপ্লিকেট বন্ধ — শেডে শুধু সর্বশেষ ওয়াক্ত থাকবে।

কুরআনে এখন শুধু যাচাইকৃত অফলাইন আয়াত (অনুমান নয়, ব্যর্থতায় পুনরায় চেষ্টা)। দুটি ছোট হাদিস গ্রন্থে আংশিক নমুনা লেবেল। দৈনিক হাদিস নোটিফিকেশনে চাপ দিলে সেই হাদিস খুলবে। তসবীহ লক্ষ্য নিজের মতো (৩৩/১০০/৫০০/১০০০ বা যেকোনো সংখ্যা)। এআই ট্যাব এখন সৎ নামে ইসলামিক সহকারী, অনেক বেশি অফলাইন বিষয়সহ। ফ্যামিলি যিকির সরানো হয়েছে; আপনার তথ্য অপরিবর্তিত — আপডেট করুন!

## Release checklist (do not ship without these)

- [ ] Upload keystore created; `NOORUP_KEYSTORE_*` in local.properties (never committed)
- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify`)
- [ ] Installed the signed AAB/APK on 2 real phones: fresh install + update-over-old-data
- [ ] Prayer alert fires once; granting exact alarms hides the warning
- [ ] Tools has 7 tabs, Hadith/Quran screens load under R8
- [ ] Store screenshots contain no Family Zikr; data safety reflects fully-offline app
- [ ] Staged rollout (start ~20%), then commit + tag `v1.2.0`
