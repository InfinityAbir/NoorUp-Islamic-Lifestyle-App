# Google Drive backup: one-time setup

Backup and restore (Tools > Backup) write one encrypted file, `noorup_backup.enc`, to the hidden
`appDataFolder` of the user's own Google Drive. There is no NoorUp server. The code is complete, but Google
only lets the app sign in once an OAuth client exists for the exact package name and signing key.

Until that is configured the button shows "Google Drive sign-in isn't set up for this build yet", and the
**Save to file / Restore from file** option on the same screen works without Google.

## Steps (Google Cloud console)

1. Create (or reuse) a project and enable the **Google Drive API**.
2. OAuth consent screen: add the scope `https://www.googleapis.com/auth/drive.appdata`
   (non-sensitive, so no Google verification review is needed) and publish the app.
3. Credentials > Create OAuth client ID > **Android**, once per signing key:
   - Package name: `com.aistudio.noorup.qzmxlw`
   - SHA-1 of the **release/upload key**, and also of the Play App Signing key (Play Console > App integrity).
   - Add the debug key SHA-1 as well if you want to test debug builds.
4. Nothing goes into the app: no client secret or API key is bundled.

## Privacy notes for the Play Console data-safety form

- The backup is encrypted on the device with AES-256-GCM; the key comes from a passphrase the user types
  (PBKDF2-HMAC-SHA256, 210,000 iterations). The passphrase is never stored or sent, so a forgotten passphrase
  cannot be recovered.
- Contents: bookmarks, Hifz progress, prayer-habit and Qada history, Tasbih targets and counts, Ramadan and
  family trackers, and settings. Device ids and notification bookkeeping are excluded.

## Family Dhikr links

Goals and progress travel inside `noorup://dhikr?...` links and QR codes; nothing is uploaded. Chat apps often
do not make a custom-scheme link tappable, so every shared message tells people to copy it and use
Tools > Family Dhikr > Paste (or Scan). If you later want one-tap `https://` links, host a small redirect page
and add an Android App Link for it.
