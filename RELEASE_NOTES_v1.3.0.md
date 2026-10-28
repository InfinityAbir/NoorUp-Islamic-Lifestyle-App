# NoorUp v1.3.0 (versionCode 15) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 14 (1.2.7), so this build uses 15.

## EN — What's new in 1.3.0

The Islamic Assistant is now AI-powered. Online, it answers in Bengali or English based on the Quran and authentic Sunnah, with references. Offline or when the AI is busy, built-in answers still work, with a clear message explaining why. Your question is sent to an AI service to write the reply. AI can make mistakes, so confirm important rulings with a scholar.

## BN — ১.৩.০-এ নতুন কী

ইসলামিক সহকারী এখন এআই-চালিত। অনলাইনে কুরআন ও সহীহ সুন্নাহর আলোকে, রেফারেন্সসহ, বাংলা বা ইংরেজিতে উত্তর পাবেন। অফলাইনে বা এআই ব্যস্ত থাকলে ইনবিল্ট উত্তর কাজ করবে এবং কারণ স্পষ্ট বার্তায় জানানো হবে। উত্তর তৈরির জন্য প্রশ্ন একটি এআই সেবায় পাঠানো হয়। এআই ভুল করতে পারে, তাই গুরুত্বপূর্ণ মাসআলা আলেমের কাছে যাচাই করুন।

## Included changes

- Islamic Assistant answers come from Google Gemini through the NoorUp `ai-proxy`
  Cloudflare Worker (`ai-proxy/`). The APK contains no API key; the Worker owns the
  system prompt, rate limits per device and IP, and caps input size.
- Answers must rest on the Quran and authentic hadith, cite the surah/ayah or hadith
  collection, never invent references, and note when schools differ (Hanafi default).
  The app's vetted offline answer for a topic is sent along as reference notes.
- Offline, rate-limited or no `aiProxyUrl`: the built-in rule-based engine answers
  instead, with a short notice when the AI was attempted.
- Clear, plain messages (English and Bengali, never status codes) for no internet, slow
  connection, asking too fast, free quota used up, AI busy, and question too long.
- New `INTERNET` permission (used only for the assistant).
- Assistant screen copy rewritten: no longer says "100% offline"; adds a privacy line.

## Play Console checklist

- [ ] Data safety form: user-typed questions are sent to a third-party AI service
      (Google Gemini via the app's Worker) to generate answers; not linked to identity
      (random per-install id used only for rate limiting).
- [ ] Update store description and screenshot 07 (re-export `07-ai` from
      `store-assets/playstore/corrected/07-ai.html`): remove "100% offline" for the assistant.
- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify`
      shows versionCode 15 / versionName 1.3.0).
- [ ] Smoke test on a phone: ask online (AI reply with references), then in airplane
      mode (built-in reply plus notice).
