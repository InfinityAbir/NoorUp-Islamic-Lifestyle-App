# NoorUp v1.6.3 (versionCode 23) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

Play already has versionCode 22 (1.6.2), so this build uses 23.

## EN — What's new in 1.6.3

- Fixed: after you updated yesterday's Noor Garden from Stats, Home showed yesterday's prayers as today's. Home now keeps showing today.
- The home widget now resizes with you: text and spacing scale to fit any size. Prohibited times now sit between the prayer times, in order, and the one running now is highlighted.
- Your Tasbih goal is remembered for each zikr and no longer resets when you switch zikr or use the widget.

## BN — ১.৬.৩-এ নতুন কী

- ঠিক করা হয়েছে: পরিসংখ্যান থেকে গতকালের নূর বাগান আপডেট করার পর হোমে গতকালের ওয়াক্ত আজকের হিসেবে দেখাত। এখন হোমে সবসময় আজকের নূর বাগান দেখায়।
- হোম উইজেট এখন আকার বদলালে লেখা ও ফাঁকা জায়গা নিজে থেকে মানিয়ে নেয়। নিষিদ্ধ সময়গুলো এখন নামাজের সময়ের মাঝে ক্রমানুসারে দেখায়, আর চলমান নিষিদ্ধ সময় আলাদা করে চিহ্নিত থাকে।
- তসবীহে প্রতিটি যিকিরের লক্ষ্য আলাদাভাবে সংরক্ষিত থাকে; যিকির বদলালে বা উইজেট ব্যবহার করলে আর রিসেট হয় না।

## Included changes

### Fixes
- **Noor Garden showed the wrong day on Home.** Tapping a past day in the Stats timeline selected that date app-wide, and closing the editor never restored it. Home therefore kept rendering the edited day under "Today's Noor Garden" until the app was restarted. The previous selection is now restored on Done or dismiss. Stored data was never affected.
- **Tasbih goal reset.** The goal you set on a built-in zikr was stored as one shared value for whichever zikr was active, so picking another zikr, using the widget's ‹ › buttons, or resetting from a later stage replaced it with that zikr's preset. Goals are now saved per zikr (`tasbih_goal_<id>#<stage>`) and used whenever that zikr is selected, reset, or switched to from the widget.

### Improvements
- **Home widget scales with its size.** The widget fills its launcher cell and scales all text and paddings by its width and height, so resizing it resizes the content. Extra height is shared evenly between the prayer rows.
- **Prohibited times inside the prayer list.** Sunrise, Zawal and Sunset windows now appear between the prayers in time order, at the same text size, instead of in a cramped one-line footer. The window running now is shown in red with a flame.
- **Asr row dash.** The dash in the Asr row was green; it now matches the other rows.
- **Narrow widget header.** The Hijri date has its own line so it is no longer cut off.

### Good to know
- No data migration is needed. Counts, history, prayer records, custom zikr and family goals are unchanged. Built-in zikr start with their preset goal until you change it.
- Margins, the progress bar and the divider in the widget keep a fixed size.
- Tests were added for switching zikr and back, and for the widget scale rules.
