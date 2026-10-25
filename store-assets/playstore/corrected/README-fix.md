# NoorUp Play Store phone-mockup fix — corrected exports for review

Source: `store-assets/playstore/*.html` (editable masters, 1080x1920).
Output here: `store-assets/playstore/corrected/` — originals untouched.

## Visual standard (same for all 8)

- Canvas: 1080x1920 preserved.
- Phone outer: 640x1320, ratio 0.4848 (modern smartphone; was 860x1210, ratio 0.7107 tablet-like).
- Frame: 12px solid #0f1a14 (was 18px), radius 60px (was 88px) — slimmer, modest, consistent.
- Centered horizontally; vertical: phoneTop 439-441 (6 files) or 510 (01, 07 with 2-line H1); bottom whitespace 159-161 or 90 — no clipping, shadow intact.
- Top spacing only: `.pill margin-top` 150 -> 100 (6 files), 60 (01), 72 (07); `.phone margin-top` 56 -> 44 (6 files), 39 (01), 40 (07).
- Nothing else changed: Bengali H1/sub, colors, fonts, backgrounds, badges, app UI text/sizes, nav — identical (see diff: only 2 CSS lines per file).

## Files

Editable (future exports use these):
- 01-prayer-times.html, 02-quran.html, 03-dua.html, 04-hadith.html
- 05-qibla-tools.html, 06-zakat.html, 07-ai.html, 08-tracking.html

Rendered PNGs (1080x1920, Play-compatible):
- 01-prayer-times-1080x1920.png, 02-quran-1080x1920.png, 03-dua-1080x1920.png, 04-hadith-1080x1920.png
- 05-qibla-tools-1080x1920.png, 06-zakat-1080x1920.png, 07-ai-1080x1920.png, 08-tracking-1080x1920.png

Previews:
- compare-03-dua-before-after.png (requested)
- compare-04-hadith-before-after.png (requested)

Render: Playwright Chromium, `.shot` element screenshot at scale 1, fonts waited (`document.fonts.ready`), 1080x1920 exact.

## QA

- All PNGs 1080x1920.
- All phones 640x1320 ratio 0.4848.
- `.screen` scrollHeight == clientHeight (no inner clipping) for all 8.
- No stretch: inner font/padding CSS untouched; text only reflows (e.g. chips wrap), never scaled non-uniformly.
- Edges/corners/nav checked visually; bottom nav visible in all; no transparency.
- Originals preserved (staged A files untouched); corrected/ is untracked; no app code touched; nothing committed/pushed/uploaded.
