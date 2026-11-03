package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.HijriDateCalculator
import com.example.ui.noorup.PrayerAlarmReceiver
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the real PrayerAlarmReceiver exactly as AlarmManager would (notification id 2003 = Maghrib) and reads
 * back what was posted, so the Ramadan wording is proven end to end, not just in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MaghribReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun fireMaghrib(hijriDayOffset: Int, ramadanMode: String = "AUTO"): List<Pair<String, String>> {
        context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("prayer_notifications_enabled", true)
            .putBoolean("is_english", true)
            .putInt("hijri_day_offset", hijriDayOffset)
            .putString("ramadan_mode", ramadanMode)
            .commit()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(nm).allNotifications // touch to make sure the shadow is initialised
        nm.cancelAll()
        PrayerAlarmReceiver().onReceive(
            context,
            Intent()
                .putExtra("prayer_name", "Maghrib Prayer")
                .putExtra("message", "It is time for Maghrib.")
                .putExtra("channel_id", "prayer_reminders_channel")
                .putExtra("notification_id", 2003)
        )
        // The receiver reschedules alarms on a background thread (goAsync). Let it finish so it cannot leak
        // into the next test's Robolectric environment.
        waitForReceiverThreads()
        return shadowOf(nm).allNotifications.map {
            (it.extras.getCharSequence("android.title")?.toString().orEmpty()) to
                (it.extras.getCharSequence("android.text")?.toString().orEmpty())
        }
    }

    private fun waitForReceiverThreads() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val busy = Thread.getAllStackTraces().keys.any { t ->
                t.isAlive && t.name.startsWith("Thread-") &&
                    t.stackTrace.any { it.className.contains("PrayerAlarmReceiver") || it.className.contains("PrayerNotificationScheduler") }
            }
            if (!busy) break
            Thread.sleep(50)
        }
        Thread.sleep(200)
    }

    /**
     * The Hijri day offset that makes today's Hijri date [month]/[day] for the default test location (Dhaka),
     * so these tests do not depend on the date they run on.
     */
    private fun offsetFor(month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        return (-400..400).first { offset ->
            val hijri = HijriDateCalculator.calculateHijriDate(today, offset, isBangladesh = true)
            hijri.month == month && hijri.day == day
        }
    }

    @Test
    fun `maghrib alarm during ramadan posts the iftar notification`() {
        // Ramadan day 13: after Maghrib it is already the 14th night, which is not an odd last-ten night.
        val posted = fireMaghrib(offsetFor(month = 9, day = 13))
        assertEquals(posted.toString(), 1, posted.size)
        assertTrue(posted[0].first.contains("Iftar"))
        assertTrue(posted[0].second.contains("2357"))
        assertFalse(posted[0].second.contains("Laylatul Qadr"))
    }

    @Test
    fun `maghrib alarm on an odd night also mentions laylatul qadr`() {
        // Ramadan day 26: after Maghrib it is the 27th night.
        val posted = fireMaghrib(offsetFor(month = 9, day = 26))
        assertEquals(posted.toString(), 1, posted.size)
        assertTrue(posted[0].first.contains("Iftar"))
        assertTrue(posted[0].second.contains("Laylatul Qadr"))
    }

    @Test
    fun `maghrib alarm outside ramadan keeps the normal prayer text`() {
        // Safar (month 2) is never Ramadan, whatever the real date is.
        val posted = fireMaghrib(offsetFor(month = 2, day = 10))
        assertEquals(posted.toString(), 1, posted.size)
        assertEquals("Maghrib Prayer", posted[0].first)
    }

    @Test
    fun `ramadan mode off keeps the normal prayer text even inside ramadan`() {
        val posted = fireMaghrib(offsetFor(month = 9, day = 13), ramadanMode = "OFF")
        assertEquals("Maghrib Prayer", posted.single().first)
    }
}
