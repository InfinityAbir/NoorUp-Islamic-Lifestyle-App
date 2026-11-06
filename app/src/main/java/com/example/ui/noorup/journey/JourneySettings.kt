package com.example.ui.noorup.journey

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.PrayerNotificationHelper

@Composable
private fun SettingSwitchRow(title: String, subtitle: String?, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) MutedText(subtitle, size = 11)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun safeStart(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // A device without that settings screen simply keeps the in-app note.
    }
}

/** General Journey preferences + the reminder settings. Language and theme stay on the main header toggles. */
@Composable
fun JourneySettingsCard(isEnglish: Boolean, settings: JourneyPrefs, onChanged: () -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val read = version // re-read the live prefs after every change
    GlassCard {
        SectionLabel(if (isEnglish) "Animation" else "অ্যানিমেশন")
        SettingSwitchRow(
            if (isEnglish) "Celestial animations" else "আকাশের অ্যানিমেশন",
            if (isEnglish) "Twinkling stars and glowing prayer stars." else "ঝিকমিক তারা ও জ্বলজ্বলে নামাজের তারা।",
            settings.animationsEnabled
        ) { settings.animationsEnabled = it; version++; onChanged() }
        SettingSwitchRow(
            if (isEnglish) "Reduced motion" else "কম নড়াচড়া",
            if (isEnglish) "Keeps everything still. Android's \"remove animations\" setting is also respected." else "সবকিছু স্থির রাখে। অ্যান্ড্রয়েডের \"অ্যানিমেশন বন্ধ\" সেটিংও মানা হয়।",
            settings.reducedMotion
        ) { settings.reducedMotion = it; version++; onChanged() }
        Spacer(Modifier.height(8.dp))
        MutedText(
            if (isEnglish) "Language and light/dark theme use the toggles at the top of the Stats screen, so there is only one place to change them."
            else "ভাষা ও লাইট/ডার্ক থিম পরিসংখ্যান পর্দার উপরের টগল দিয়ে বদলান, যাতে পরিবর্তনের জায়গা একটিই থাকে।",
            size = 11
        )
    }
    Spacer(Modifier.height(14.dp))
    GlassCard {
        SectionLabel(if (isEnglish) "Evening review" else "সন্ধ্যার পর্যালোচনা")
        SettingSwitchRow(
            if (isEnglish) "Only remind me if a prayer is still open" else "কোনো ওয়াক্ত বাকি থাকলেই অনুস্মারক",
            if (isEnglish) "The existing evening Noor Garden reminder stays quiet once all five prayers are recorded as completed."
            else "পাঁচ ওয়াক্তই আদায় রেকর্ড হয়ে গেলে নূর বাগানের সন্ধ্যার অনুস্মারক চুপ থাকে।",
            settings.eveningOnlyIfPending
        ) { settings.eveningOnlyIfPending = it; version++; onChanged() }
    }
    Spacer(Modifier.height(14.dp))
    ReminderSettings(isEnglish, settings) { version++; onChanged() }
}

/** Friday Reflection + weekly intention reminders. Opt-in, gentle, and honest about system limits. */
@Composable
fun ReminderSettings(isEnglish: Boolean, settings: JourneyPrefs, onChanged: () -> Unit) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val read = version
    var pending by remember { mutableStateOf<String?>(null) }

    fun changed() {
        version++
        onChanged()
        try {
            PrayerNotificationHelper.createNotificationChannels(context)
            FridayReminderScheduler.scheduleAll(context)
        } catch (_: Exception) { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when (pending) {
                "friday" -> settings.fridayReminderEnabled = true
                "intention" -> settings.intentionReminderEnabled = true
            }
            changed()
        }
        pending = null
    }

    fun requestEnable(kind: String) {
        if (Build.VERSION.SDK_INT >= 33 && !PrayerNotificationHelper.areNotificationsEnabled(context)) {
            pending = kind
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (kind == "friday") settings.fridayReminderEnabled = true else settings.intentionReminderEnabled = true
            changed()
        }
    }

    val notificationsOn = PrayerNotificationHelper.areNotificationsEnabled(context)
    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        runCatching { (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() }.getOrDefault(true)
    val master = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE).getBoolean("prayer_notifications_enabled", true)
    val minutes = settings.fridayMinutes
    val timeLabel = String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60).let {
        if (isEnglish) it else it.map { c -> if (c in '0'..'9') '০' + (c - '0') else c }.joinToString("")
    }

    GlassCard {
        SectionLabel(if (isEnglish) "Weekly reminders" else "সাপ্তাহিক অনুস্মারক")
        Spacer(Modifier.height(4.dp))
        SettingSwitchRow(
            if (isEnglish) "Friday Reflection reminder" else "শুক্রবারের প্রতিফলন অনুস্মারক",
            if (isEnglish) "One quiet notification on Fridays. Skipped if you have already saved this week's reflection." else "শুক্রবারে একটি শান্ত নোটিফিকেশন। এই সপ্তাহের প্রতিফলন সংরক্ষিত থাকলে বাদ যায়।",
            settings.fridayReminderEnabled
        ) { on ->
            if (on) requestEnable("friday") else { settings.fridayReminderEnabled = false; changed() }
        }
        if (settings.fridayReminderEnabled || settings.intentionReminderEnabled) {
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isEnglish) "Reminder time" else "অনুস্মারকের সময়", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    MutedText(if (isEnglish) "Local time, follows your timezone." else "স্থানীয় সময়, আপনার টাইমজোন অনুসরণ করে।", size = 11)
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> settings.fridayMinutes = h * 60 + m; changed() },
                            minutes / 60, minutes % 60,
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(timeLabel) }
            }
        }
        if (settings.fridayReminderEnabled) {
            SettingSwitchRow(
                if (isEnglish) "Skip if reflection is done" else "প্রতিফলন সম্পন্ন হলে বাদ দিন",
                null,
                settings.skipIfReflectionDone
            ) { settings.skipIfReflectionDone = it; changed() }
        }
        SettingSwitchRow(
            if (isEnglish) "Weekly intention reminder" else "সাপ্তাহিক নিয়তের অনুস্মারক",
            if (isEnglish) "On Mondays, shows the intention you set. Only sent if you have one." else "সোমবারে আপনার ঠিক করা নিয়ত দেখায়। নিয়ত থাকলেই পাঠানো হয়।",
            settings.intentionReminderEnabled
        ) { on ->
            if (on) requestEnable("intention") else { settings.intentionReminderEnabled = false; changed() }
        }

        if (!master) {
            Spacer(Modifier.height(6.dp))
            MutedText(
                if (isEnglish) "Prayer notifications are turned off in NoorUp, so these reminders are paused too."
                else "নূরআপে নামাজের নোটিফিকেশন বন্ধ আছে, তাই এই অনুস্মারকগুলোও বন্ধ থাকবে।",
                size = 11
            )
        }
        if (!notificationsOn) {
            Spacer(Modifier.height(6.dp))
            MutedText(
                if (isEnglish) "Notifications are switched off for NoorUp in Android settings, so nothing can be shown."
                else "অ্যান্ড্রয়েড সেটিংসে নূরআপের নোটিফিকেশন বন্ধ, তাই কিছু দেখানো যাবে না।",
                size = 11
            )
            OutlinedButton(
                onClick = {
                    safeStart(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(if (isEnglish) "Open notification settings" else "নোটিফিকেশন সেটিংস খুলুন") }
        }
        if (!canExact) {
            Spacer(Modifier.height(6.dp))
            MutedText(
                if (isEnglish) "Exact alarms are off, so a reminder can arrive a few minutes late. It will still arrive."
                else "এক্সাক্ট অ্যালার্ম বন্ধ, তাই অনুস্মারক কয়েক মিনিট দেরিতে আসতে পারে। তবু আসবে।",
                size = 11
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OutlinedButton(
                    onClick = { safeStart(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEnglish) "Allow exact alarms" else "এক্সাক্ট অ্যালার্মের অনুমতি") }
            }
        }
        Spacer(Modifier.height(6.dp))
        MutedText(
            if (isEnglish) "If reminders arrive late on your phone, set NoorUp to \"Unrestricted\" in the battery settings."
            else "ফোনে অনুস্মারক দেরিতে এলে ব্যাটারি সেটিংসে নূরআপকে \"আনরিস্ট্রিক্টেড\" করুন।",
            size = 11
        )
        OutlinedButton(
            onClick = { safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text(if (isEnglish) "Open battery settings" else "ব্যাটারি সেটিংস খুলুন") }
    }
}
