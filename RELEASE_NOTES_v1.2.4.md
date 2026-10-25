# NoorUp v1.2.4 (versionCode 11) — Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

## EN — What's new in 1.2.4

Digital Tasbih is now simpler to use. Tap anywhere inside the counter circle to count — a separate goal shortcut on the circle is no longer needed, since the Goal button sits right below. Bengali wording across the Tasbih screen has also been refined for clearer, more natural meaning, including the round-complete message and the New round button.

## BN — ১.২.৪-এ নতুন কী

ডিজিটাল তসবীহ এখন আরও সহজ। কাউন্টার বৃত্তের যেকোনো জায়গায় ট্যাপ করলেই গণনা হবে; লক্ষ্য নির্ধারণের জন্য নিচের 'লক্ষ্য' বোতামটি রয়েছে। তসবীহ স্ক্রিনের বাংলা লেখাও আরও পরিষ্কার ও অর্থপূর্ণ করা হয়েছে—যেমন রাউন্ড সম্পন্ন হওয়ার বার্তা এবং 'নতুন রাউন্ড' বোতাম।

## Included changes

- Removed the inline edit/goal shortcut from inside the Tasbih counter circle. The entire circle is now a single tap target that only increments the count.
- Goal editing remains available from the dedicated Goal button below the counter; no Tasbih functionality was removed.
- Replaced the word "দৌর" with the clearer "রাউন্ড" and reworded the Tasbih copy:
  - Completion banner: "তসবীহ পূর্ণ হয়েছে — আবার শুরু করতে 'নতুন রাউন্ড' চাপুন।"
  - Completed-round button: "নতুন রাউন্ড"
  - "My Zikr" hint now reads more naturally.
  - Stage editor hint now explains multi-stage rounds without the stray English word "Celebration".

## Pre-upload checklist

- [ ] Build and sign `:app:bundleRelease` using the release upload key.
- [ ] Install on a real phone and verify: tapping anywhere in the circle increments the count and never opens the goal dialog.
- [ ] Verify the Goal button still opens the goal dialog and saves correctly.
- [ ] Complete a round in Bangla and English: banner, New round button, and completion dialog all render without clipping.
- [ ] Test update-over-existing-data before rollout.
- [ ] Upload version code 11, begin a staged rollout, then tag `v1.2.4`.
