# NoorUp v1.2.5 (versionCode 12) — Production Release Notes

Paste the matching language block into Google Play Console. Both descriptions
are below the 500-character Play release-note limit.

## EN — What's new in 1.2.5

Prayer alarms are now easier to set up: a new Home banner guides you through notification permission and exact-alarm access, then confirms your alarms are scheduled. Tapping the daily hadith notification opens that hadith directly, while prayer alerts return to Home. This release also improves alarm reliability, prevents duplicate alerts, and respects your notification settings.

## BN — ১.২.৫-এ নতুন কী

নামাজের অ্যালার্ম চালু করা এখন সহজ: হোমের নতুন ব্যানার নোটিফিকেশন ও সঠিক অ্যালার্মের অনুমতি দিয়ে অ্যালার্ম নির্ধারণ নিশ্চিত করে। দৈনিক হাদিসের নোটিফিকেশনে ট্যাপ করলে সেই হাদিস খুলবে; নামাজের নোটিফিকেশন হোমে ফিরাবে। অ্যালার্মের নির্ভরযোগ্যতা বেড়েছে, ডুপ্লিকেট নোটিফিকেশন বন্ধ এবং আপনার সেটিংস সম্মান করা হয়।

## Included changes

### Prayer-alarm onboarding (Home)
- New Home card for first-time users: **Enable Alarm** opens a short rationale,
  then requests Android 13+ notification permission and Android 12+ exact-alarm
  access in the correct order.
- Scheduling is verified against the existing `PrayerNotificationScheduler`
  before onboarding is marked complete, so the app never claims alarms are
  active when none are armed.
- **Later** collapses the banner to a compact row instead of nagging.
- If notifications are denied or exact-alarm access is revoked, a status card
  explains the problem and routes to the correct system settings screen; the
  flow re-checks permissions on every `ON_RESUME`.
- Removed the old global notification-permission banner from `MainActivity` so
  users see one rationale and one system prompt, not two.

### Notification deep links
- Daily-hadith notification taps now open the exact hadith in the app.
- Prayer and Noor Garden notification taps return to the Home dashboard.
- Works both on cold start and when the activity is already alive
  (`onNewIntent`).

### Reliability fixes
- Re-arms prayer and daily alarms when Android broadcasts an exact-alarm
  permission change.
- Prayer alarms now reschedule off the main thread via `goAsync()`, avoiding
  receiver timeouts/ANRs.
- Removed random notification IDs for malformed alarm intents; a missing ID is
  skipped instead of overwriting a real prayer notification.
- Duplicate prayer alerts within a short window are dropped, and posting a new
  wakt clears the previous prayer alert.
- Notification posts are skipped when alerts are disabled or
  `POST_NOTIFICATIONS` is denied; channel creation is idempotent.
- Noor Garden's 10 PM reminder now treats a post-midnight Doze delay as the
  previous evening's slot, so the next evening is not skipped.

### Small UI polish
- Bottom navigation label changed from "AI" to "Assistant" / "সহকারী" to match
  the in-app name.
- Navigation labels now ellipsize instead of clipping on narrow screens.

## Pre-upload checklist

- [ ] Build and sign `:app:bundleRelease` with the release upload key; confirm
      the bundle reports versionCode 12 / versionName 1.2.5.
- [ ] Install on a real phone: fresh install and update-over-1.2.4.
- [ ] Android 13+: Home → Enable Alarm → rationale → allow notifications →
      allow exact alarms → confirm onboarding disappears and alarms are armed.
- [ ] Deny notifications once, then use the status card to reach Settings and
      enable them; confirm the flow resumes and schedules.
- [ ] Revoke "Alarms & reminders" in system settings, return to the app, and
      confirm the exact-alarm status card appears.
- [ ] Tap **Later**, reopen the app, and confirm the compact retry row remains.
- [ ] Trigger a test prayer notification and a daily-hadith notification; tap
      each and verify Home / exact hadith opens.
- [ ] Disable prayer alerts, wait for a scheduled wakt, and confirm no
      notification is posted; re-enable and confirm scheduling resumes.
- [ ] Test the 10 PM Noor Garden reminder (or use the test trigger) and verify
      it is not duplicated.
- [ ] Upload version code 12 to **Production**, review the EN/BN notes, start a
      staged rollout, then tag `v1.2.5`.
