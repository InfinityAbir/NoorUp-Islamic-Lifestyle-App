# NoorUp v1.5.1 (versionCode 18) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 17 (1.5.0), so this build uses 18.

## EN — What's new in 1.5.1

Mark your favorite Surahs with a heart and find them in the new Favorites tab. A quick index on the side jumps through all 114 Surahs and 30 Paras. The home-screen widget now has larger prayer times and shows the three prohibited times on a single line. Removed the duplicate search icon on the Duas page.

## BN — ১.৫.১-এ নতুন কী

প্রিয় সূরায় হার্ট দিয়ে রাখুন এবং নতুন প্রিয় ট্যাবে দ্রুত খুঁজে নিন। পাশের দ্রুত সূচি দিয়ে ১১৪টি সূরা ও ৩০টি পারায় সরাসরি যান। হোম-স্ক্রিন উইজেটে নামাজের সময় আরও বড় এবং তিনটি নিষিদ্ধ সময় এক লাইনে। দুআ পেজে দুইবার থাকা সার্চ আইকন সরানো হয়েছে।

## Included changes

- **Quran favorites**: a heart on every Surah row and a **Favorites** chip beside Surah | Para | Bookmarks.
  Hearts toggle from either list and stay in sync. Only Surah numbers are stored (`fav_surah_<n>`), separate
  from Ayat bookmarks and reading progress, and they are included in Backup & Restore.
- **Quick index**: a compact side rail in the Surah (1–114) and Para (1–30) lists; tap or drag to jump, with a
  bubble showing the current entry.
- **Widget**: prayer names and times are larger (12–12.5sp); the three prohibited times sit on one auto-shrinking
  line next to a short "নিষিদ্ধ / Prohibited" label.
- **Duas**: the top search icon is hidden because the page already has its own search bar.
- **Tests**: `QuranFavoritesTest` added (favorites persistence, independence from bookmarks, index maths);
  the obsolete prohibited-text-size test was removed.
