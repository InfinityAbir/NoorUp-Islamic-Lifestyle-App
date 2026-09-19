# NoorUp — Islamic Lifestyle App for Bengali-Speaking Muslims

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Firebase-Gemini_AI-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase AI" />
</p>

<p align="center"><b>A modern, lightweight, and clutter-free Islamic companion — Bengali-first, built for everyday practice.</b></p>

<p align="center">Prayer times • Quran • Hadith • Duas • Qibla • Zakat • Tasbih • AI guidance • Habit analytics</p>

---

## Table of Contents

- [About the Project](#about-the-project)
- [Why I Built NoorUp](#why-i-built-noorup)
- [Who Will Use It](#who-will-use-it)
- [Why They Will Use It](#why-they-will-use-it)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Permissions](#permissions)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Author & Contact](#author--contact)

---

## About the Project

**NoorUp** is a native Android Islamic lifestyle app designed specifically for **Bengali-speaking users**.

Most existing Islamic apps are either English/Arabic-only, overloaded with ads, inaccurate for Bangladesh prayer times, or split across 5–6 different apps. NoorUp brings everything together in one clean, fast, offline-friendly place:

> Accurate prayer times, Quran with Bengali translation, authentic Hadith, categorized Masnoon Duas, Qibla, Hijri events, Zakat calculator, Digital Tasbih, Qada tracking, prayer notifications + home-screen widget, and a Bengali-capable AI Islamic assistant.

Built with **Kotlin + Jetpack Compose (Material 3)**, with a custom **Solar Prayer Engine**, **WorkManager-based notifications**, and **Firebase AI (Gemini)** for guidance.

Package: `com.aistudio.noorup.azxy99` • Min SDK 24 • Target SDK 36

---

## Why I Built NoorUp

I built NoorUp to solve problems I personally faced and saw around me in Bangladesh:

1. **Language barrier:** Many elders and new learners struggle with English-only Islamic apps. They need clear Bangla — with Arabic preserved and English as an option, not a requirement.
2. **Fragmentation:** Prayer times in one app, Quran in another, Duas in a PDF, Zakat math on a calculator, Hadith on random websites. I wanted **one trusted home** for daily deen.
3. **Trust and accuracy:** Generic global prayer-time APIs are often off for Dhaka/Chattogram/Sylhet, and Makruh times (sunrise, zawal, sunset) are rarely explained. NoorUp uses a transparent solar calculation engine with city/GPS calibration and explicit Makruh warnings.
4. **Distraction-free design:** Many apps are heavy, ad-filled, and cluttered. I wanted a **lightweight, calm, glassmorphic UI** with dark/light mode that respects focus and elders' readability.
5. **Consistency, not just information:** Knowing prayer times is not enough. Tracking prayers, making up Qada, building streaks, and family Dhikr are what change habits — so I built analytics in from day one.

This is also my exploration of production-grade Android: Compose navigation, Room, WorkManager + AlarmManager, widgets, bilingual typography (Bangla/Arabic/English), and Gemini integration with safe local fallback.

---

## Who Will Use It

- **Bengali-speaking practicing Muslims** in Bangladesh and West Bengal who want daily prayer, Quran, and Dua guidance in Bangla.
- **Elders and new learners** who prefer large, simple Bangla UI with phonetics and references, without complex settings.
- **Students and professionals** who want Sehri/Iftar times, prayer alerts, Qibla, and a home-screen widget for at-a-glance tracking.
- **Families** doing collective Dhikr (Family Zikr counter, Noor Garden motivation system).
- **Travelers** who need Qasr (shortened prayer) guidance and GPS-based time recalculation.
- **Anyone calculating Zakat** in Vori/Taka who wants a simple Nisab-based calculator in Bangla.

The app is fully bilingual (বাংলা / English toggle) so it also works for English-preferring users.

---

## Why They Will Use It

- **Bengali-first, not translated as an afterthought:** Every screen, prayer name, Dua meaning, and Hadith explanation is available in Bangla with Bengali digits, alongside Arabic and English.
- **Accurate and transparent prayer times:** Custom solar engine + city presets + live GPS, with start/end times, Makruh windows explained, Sehri-end / Iftar-start bar, and Tahajjud window.
- **Everything offline where it matters:** Quran (114 Surahs), Duas with references, and core prayer logic work without constant internet. AI features gracefully fall back offline.
- **Habit system:** Prayer check-ins, streaks, consistency timeline, missed-day → Qada transfer, and Qada ledger help users actually stay consistent.
- **Reliable reminders:** High-priority prayer notifications, Sehri/Maghrib alerts, boot-resilient rescheduling, and a 4x2 home-screen widget.
- **Authentic sources:** Duas and Hadith show references (e.g., Sahih Bukhari 6306, Sahih Muslim 2708, Quran Surah references) instead of unsourced text.
- **Calm and fast:** Native Compose UI, dark mode, no web-view bloat, no ads in codebase.

---

## Key Features

### 1. Home — Prayer Orbit Dashboard
- Modern orbit + timeline hero widget for 5 daily prayers + Sunrise + Tahajjud
- Current / passed states, countdown, start–end dual timestamps
- Sehri Ends & Iftar Starts quick status bar
- Date + solar calibration card (calendar picker, next/previous day, calculation & juristic methods)
- Travel Qasr mode card, Family Zikr card, Noor Garden streak card

### 2. Quran
- All 114 Surahs with Bangla / English / Arabic names, verse counts, Makki/Madani tags
- Full Arabic text + Bangla + English translation (Al-Fatihah & Al-Ikhlas fully curated)
- Search Surah, bookmark verses, mark as memorized

### 3. Duas (Masnoon)
- 24+ categorized Duas: Morning/Evening, Protection, Forgiveness, Anxiety/Debt, Rizq, Parents, Family, Knowledge, Travel, Mosque/Worship
- Arabic + Bangla phonetic + English transliteration + Bangla/English meaning + authentic reference
- One-tap copy

### 4. Hadith Library
- Canonical book structure (Bukhari-style datasets), chapters, topics, narrator, grading (Sahih / Muttafaqun Alayh)
- Bangla + English translation, explanation/lessons, references
- Robust parser: flexible keys, int/string tolerance, Mojibake / double-encoded UTF-8 repair, HTML-entity cleaning for flawless Bangla/Arabic typography
- Search, topic/chapter filter, pagination, bookmarks

### 5. Tools
- **Prayer Alerts Management:** per-prayer toggles, Sehri/Maghrib alerts, test notification + exact-alarm check
- **Digital Tasbih:** Dhikr presets, custom goal, tap counter, reset
- **Solar Engine Inspector:** dawn twilight, sunrise, zawal, sunset horizon breakdown
- **Qibla Direction:** Kaaba guidance card
- **Hijri Calendar & Events:** upcoming Islamic events with days-left countdown
- **Zakat & Sadaqah Calculator:** gold (vori), silver, cash, business assets, investments, liabilities → 2.5% net Zakat with Nisab logic
- **Home-Screen Widget:** 4x2 glassmorphic prayer widget with 1-tap pin (`NoorUpWidgetProvider`)

### 6. AI Companion (Gemini)
- Islamic Q&A assistant powered by Firebase AI (Gemini), bilingual
- Quick prompts: Assalamu Alaikum, Fajr & Prayer Rules, Tahajjud Virtue, Masnoon Duas, Zakat help
- Local Islamic fallback responses when API key/offline is unavailable

### 7. Stats — Consistency & Qada Analytics
- Daily completion %, streak counter, 7-day consistency timeline
- Missed-day detection → transfer to Qada ledger
- Pending vs. made-up Qada tracking per prayer

### Cross-cutting
- Bangla/English toggle everywhere, Bengali digits, dark/light Material 3 theme
- GPS location + city presets, offline-first Room caching, WorkManager rescheduling, BootReceiver

---

## Tech Stack

**Language & UI:**
- Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose, Coil (optional)

**Android Jetpack:**
- Lifecycle + ViewModel, Room 2.7.0 (KSP), WorkManager 2.10.0, DataStore (ready), Activity Compose

**Networking & Data:**
- Retrofit 2.12.0, OkHttp + Logging Interceptor, Moshi + Codegen (KSP), Gson 2.11.0

**Firebase:**
- Firebase BOM 34.17.0, Firebase AI (Gemini), App Check (Recaptcha + Debug)

**System:**
- AlarmManager exact alarms, Notification Channels, AppWidget (Glance-style custom provider), Geocoder + Location

**Build & Quality:**
- AGP 9.1.1, Secrets Gradle Plugin 2.0.1 (`.env`), KSP, Robolectric, Roborazzi screenshot tests, Espresso + Compose UI tests

See `gradle/libs.versions.toml` and `app/build.gradle.kts` for full versions.

---

## Architecture Overview

Single-module Compose app with MVVM:

- `MainActivity.kt` — edge-to-edge Scaffold, 6-tab bottom nav (Home, Quran, Duas, Tools, AI, Stats), notification permission banner
- `ui/noorup/NoorUpViewModel.kt` — single source of truth (language, theme, city/GPS, prayers, Quran/Hadith state, Tasbih, Zakat, habits/Qada, AI chat, notifications)
- `ui/noorup/NoorUpRepository.kt` — static curated datasets (prayer times, 114 Surahs, Duas, Hijri events)
- `ui/noorup/NoorUpScreens.kt` (~3900 lines) — all feature screens + GlassCard design system
- `ui/noorup/SolarPrayerEngine.kt` — solar math for Fajr/Sunrise/Dhuhr/Asr/Maghrib/Isha + Makruh windows
- `ui/noorup/hadith/` — `HadithModels.kt`, `HadithRepository.kt`, normalizer + deserializer
- Workers/Receivers: `PrayerNotificationWorker`, `PrayerRescheduleWorker`, `NoorGardenReminderWorker`, `PrayerAlarmReceiver`, `BootReceiver`, `PrayerNotificationScheduler`, `PrayerNotificationHelper`
- Widget: `NoorUpWidgetProvider.kt` + `PrayerOrbitDashboardWidget.kt`

---

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended)
- JDK 11+, Android SDK 36
- A Gemini API key (only for AI Companion — app runs without it in local-fallback mode)

### Run Locally

1. Clone the repo:
   ```bash
   git clone https://github.com/InfinityAbir/NoorUp-Islamic-Lifestyle-App.git
   ```
2. Open the folder in Android Studio and let it sync Gradle.
3. Create a `.env` file in the project root (see `.env.example`):
   ```env
   GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
   ```
4. Run on an emulator or physical device (Min SDK 24):
   - `Run > Run 'app'`

> Note: The template `signingConfig = signingConfigs.getByName("debugConfig")` line is for local debug builds. For your own Play release, configure your own `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` env vars (see `app/build.gradle.kts`) and [reset your upload key](https://support.google.com/googleplay/android-developer/answer/9842756) if needed.

---

## Configuration

- **Language:** Top bar pill toggles বাংলা / English instantly.
- **Theme:** Dark / light toggle, persisted.
- **Location:** City preset or `Use Current GPS Location` (requests `ACCESS_FINE_LOCATION`) for precise Sehri/Iftar/prayer times.
- **Calculation:** Change calculation method + Hanafi/Shafi Asr juristic method from the Date/Solar card.
- **Notifications:** Enable per-prayer alerts from Tools → Prayer Alerts. Android 13+ will prompt for `POST_NOTIFICATIONS`. Exact alarms require `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.
- **Widget:** Tools → Home Widget → `Pin Widget to Home Screen` (or long-press Home → Widgets → NoorUp).

---

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Prayer / Sehri / Iftar high-priority alerts |
| `ACCESS_FINE_LOCATION` / `COARSE` | GPS-based prayer-time calibration (optional) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Exact prayer-time alarms |
| `RECEIVE_BOOT_COMPLETED` | Reschedule notifications after reboot |
| `VIBRATE`, `WAKE_LOCK` | Alert reliability |

No ads SDKs. No unnecessary data collection in codebase.

---

## Project Structure

```
NoorUp-Islamic-Lifestyle-App/
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/example/
│       ├── MainActivity.kt
│       ├── NoorUpApplication.kt
│       ├── NoorUpWidgetProvider.kt
│       └── ui/
│           ├── theme/ (Color.kt, Theme.kt, Type.kt)
│           └── noorup/
│               ├── NoorUpScreens.kt
│               ├── NoorUpViewModel.kt
│               ├── NoorUpModels.kt
│               ├── NoorUpRepository.kt
│               ├── SolarPrayerEngine.kt
│               ├── Prayer* (Scheduler, Helper, Workers, Receiver)
│               └── hadith/ (HadithModels.kt, HadithRepository.kt)
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── .env.example
└── README.md
```

---

## Roadmap

- [ ] Full Quran (all verses) via verified offline bundle + audio
- [ ] Complete 6-book Hadith offline datasets with Bengali sharh
- [ ] Accurate Qibla compass with sensor fusion
- [ ] Prayer-time API fallback + auto city detection
- [ ] Ramadan mode (Sehri/Iftar countdown, Taraweeh tracker)
- [ ] Cloud backup / sync (Firestore + Auth, optional)
- [ ] Play Store release with signed bundle + screenshots

Contributions and issue reports are welcome.

---

## Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m "Add your feature"`
4. Push and open a Pull Request

Please keep Bangla typography intact (UTF-8), add references for any new Dua/Hadith, and include a screenshot for UI changes.

---

## License

This project is currently **All Rights Reserved** — source available for learning and review.

If you want to reuse code or contribute a licensed fork, please contact the author. A formal open-source license (e.g., MIT) can be added on request.

---

## Author & Contact

**Abir Hasan (InfinityAbir)** — ASP.NET Core / Android developer, building clean, secure, real-world apps.

- GitHub: https://github.com/InfinityAbir
- Portfolio: https://infinityabir.github.io/abir-hasan-portfolio/
- LinkedIn: https://www.linkedin.com/in/infinityabirhasan/
- Email: abirha3896@gmail.com

> If NoorUp helps your daily practice, please ⭐ star the repo and share feedback via Issues.
