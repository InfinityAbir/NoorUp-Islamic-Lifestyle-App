# NoorUp v1.6.4 (versionCode 24) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 23 (1.6.3), so this build uses 24.

## EN — What's new in 1.6.4

NoorUp can now be installed on more devices, including desktop-class and Android XR devices and tablets that Play previously hid it from. The app itself is unchanged.

## BN — ১.৬.৪-এ নতুন কী

এখন আরও বেশি ডিভাইসে নূরআপ ইনস্টল করা যাবে, যেমন ডেস্কটপ-ধরনের ও অ্যান্ড্রয়েড এক্সআর ডিভাইস এবং কিছু ট্যাবলেট, যেগুলোতে আগে প্লে স্টোর অ্যাপটি দেখাত না। অ্যাপের ভেতরে কোনো পরিবর্তন নেই।

## Included changes

### Fixes
- **Availability.** The ML Kit code-scanner library locks one of its helper screens to portrait, which made Play treat a portrait screen as a required hardware feature. The manifest now declares `android.hardware.screen.portrait` as optional (`required="false"`), which clears the Play Console "Required hardware feature declarations are restricting your app's availability" warning.

### Good to know
- No app behaviour, data or UI changed. Existing users are not affected.
- Build the AAB from this version (`versionCode` 24); the 23 build is already used.
