# NoorUp Play Store screenshots (real app UI)

12 images, 1080x1920, same layout as before (pill, Bengali headline, subtitle, phone frame).
Phone content is a real screenshot from the app (v1.6.0 debug build on Pixel 8 emulator, 1080x2400, dark theme,
status bar cropped). Nothing in the phone is hand-drawn.

Upload order: 01-home ... 12-widgets (`*-1080x1920.png`).

- `screens/` real captures (cropped) used inside the frames.
- `NN-name.html` editable slide masters (Google Fonts need internet when rendering).
- Slide 12 shows the two real home-screen widgets (Prayer Timeline, Tasbih) cropped from an emulator home screen.

Render: Edge headless, `--window-size=1080,1920 --force-device-scale-factor=1 --virtual-time-budget=15000`.
