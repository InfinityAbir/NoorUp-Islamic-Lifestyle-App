# NoorUp v1.4.0 (versionCode 16) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 15 (1.3.0), so this build uses 16.

## EN — What's new in 1.4.0

Listen to the Quran: Arabic recitation by three reciters, with Bangla or English translation audio, background playback, repeat and surah loop, and offline download. New global search across Quran, Hadith and Duas. Ramadan mode with Sehri/Iftar countdown and fasting, Taraweeh and Zakat al-Fitr tools. Daily Verse & Dua widget. Family Dhikr goals shared by link or QR. Encrypted backup to Google Drive or a file.

## BN — ১.৪.০-এ নতুন কী

কুরআন শুনুন: তিনজন ক্বারীর তিলাওয়াত, বাংলা বা ইংরেজি অনুবাদের অডিও, ব্যাকগ্রাউন্ডে চলা, রিপিট ও সূরা লুপ এবং অফলাইন ডাউনলোড। কুরআন, হাদিস ও দুআয় নতুন সার্চ। রমজান মোডে সেহরি/ইফতার কাউন্টডাউন, রোজা, তারাবীহ ও ফিতরা। আজকের আয়াত ও দুআ উইজেট। লিংক বা QR-এ পারিবারিক যিকর। গুগল ড্রাইভ বা ফাইলে এনক্রিপ্টেড ব্যাকআপ।

## Included changes

- **Quran audio** (Media3 ExoPlayer + MediaSession service): per-ayah Arabic from EveryAyah (Alafasy,
  Husary, Abdul Basit); translation track is the human Sahih International recording for English and the
  phone's own Bengali text-to-speech voice for Bangla (no public Bangla recording exists). Modes: Arabic +
  translation, Arabic only, translation only. Speeds 0.75x–1.5x, repeat-ayah and surah loop, seek bar, lock-screen
  and notification controls, highlighted and auto-scrolled verse, Wi-Fi download for offline play.
- **Global search**: Quran (Arabic, Bangla, English, transliteration, `2:255` references), all eight hadith
  collections (indexed once on the phone), duas and the offline assistant; tap a result to open it.
- **Ramadan mode** (Tools > Ramadan, and a Home card in Hijri month 9 or when switched on): Sehri/Iftar
  countdown, fasting tracker with streak and Qada log, Taraweeh counter, Zakat al-Fitr calculator (BDT, editable
  rates), Laylatul Qadr checklist and duas. The Maghrib alert becomes the Iftar alert and mentions odd nights.
- **Daily Verse & Dua widget** with refresh, save and open; updates daily.
- **Family Dhikr**: goals shared by `noorup://dhikr` link or QR, editable, custom dhikr, family prayer check-in
  and streak. No server: progress travels in links.
- **Backup & Restore**: AES-256-GCM encrypted (passphrase never stored) to the Google Drive app-data folder
  or to a file. Setup: `docs/GOOGLE_DRIVE_BACKUP.md`.
- Prayer widget: larger Prohibited-times text that wraps instead of clipping, scaled to widget width.
- Fix: jumping to an ayah (search, widget, bookmarks, Continue reading) landed one ayah early.
- Unit-test JVM heap raised so the full suite runs.

## Play Console checklist

- [ ] **Do the Drive backup setup first**, or the Google Drive button will show "not set up" for users: create
      the Android OAuth client for package `com.aistudio.noorup.qzmxlw` with the **Play App Signing** SHA-1 and
      your upload-key SHA-1, and add the `drive.appdata` scope (see `docs/GOOGLE_DRIVE_BACKUP.md`).
- [ ] Foreground-service declaration: the app now declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
      (Quran audio plays with the screen off). Answer the "Foreground service permissions" form with the
      media-playback use.
- [ ] Data safety form: audio is streamed from everyayah.com; the optional Drive backup is encrypted on the
      device and stored in the user's own Drive; the QR scanner uses Google Play services' scanner (no camera
      permission requested by NoorUp).
- [ ] New permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `ACCESS_NETWORK_STATE`.
- [ ] Update the store description/screenshots for audio, Ramadan, search, widget, Family Dhikr and backup.
- [ ] `./gradlew :app:bundleRelease` built and signed (check: `jarsigner -verify` shows versionCode 16 /
      versionName 1.4.0).
- [ ] Smoke test the signed build on a phone: play a surah with the screen off, search `112:1`, open the
      Ramadan tab, add the widget, create and edit a family goal, and make a file backup then restore it.
