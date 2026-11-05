# NoorUp v1.5.2 (versionCode 19) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 18 (1.5.1), so this build uses 19.

## EN — What's new in 1.5.2

The Android Back button now works everywhere. It closes an open Surah, Explore tool or search before leaving a screen, then returns to the previous tab, and only exits from Home. Tabs remember your scroll position and where you were. The side number index in the Surah and Para lists has been removed.

## BN — ১.৫.২-এ নতুন কী

অ্যান্ড্রয়েডের ব্যাক বাটন এখন পুরো অ্যাপে ঠিকভাবে কাজ করে। খোলা সূরা, এক্সপ্লোরের টুল বা সার্চ আগে বন্ধ হয়, তারপর আগের ট্যাবে ফেরে, আর শুধু হোম থেকে অ্যাপ বন্ধ হয়। ট্যাব বদলালেও স্ক্রল অবস্থান ও আপনি যেখানে ছিলেন তা মনে থাকে। সূরা ও পারা তালিকার পাশের সংখ্যার সূচি সরানো হয়েছে।

## Included changes

- **System Back navigation**: one app-level `BackHandler` (`MainActivity`) driven by `NoorUpViewModel.handleSystemBack()`.
  Order: dialogs, search overlay, in-screen destination, previous tab, then exit at Home. The handler is disabled at
  the Home root so the system's normal exit and predictive back-to-home animation apply.
  - In-screen destinations register through `NestedBackHandler` (`BackNavigation.kt`): Quran Focus mode, bookmark
    list, open Surah, Para/Favorite filter; Explore tool back to the hub (same as the "All features" row).
  - `TabHistory` keeps bottom-navigation history; revisiting a tab pops to it, so there are no duplicate
    destinations or loops. A restored non-Home tab goes to Home on the first Back.
- **State preservation**: each tab's content is wrapped in a `SaveableStateProvider`, and the Quran bookmark
  filter, Explore tool, Dua segment and Stats sub-tab now use `rememberSaveable`, so scroll positions and sub-screen
  choices survive tab switches and Back.
- **Surah/Para quick index removed**: the side rail with 1 / 20 / 40... markers is no longer shown (the
  `QuickIndex` helper and its tests are kept).
- **Tests**: `BackNavigationTest` added (tab history rules, ViewModel Back entry point, nested handler priority).
