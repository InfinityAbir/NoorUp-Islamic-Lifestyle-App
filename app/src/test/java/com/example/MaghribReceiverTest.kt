package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.PrayerAlarmReceiver
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
        return shadowOf(nm).allNotifications.map {
            (it.extras.getCharSequence("android.title")?.toString().orEmpty()) to
                (it.extras.getCharSequence("android.text")?.toString().orEmpty())
        }
    }

    @Test
    fun `maghrib alarm during ramadan posts the iftar notification`() {
        // Offset 114 puts "today" (30 Oct 2026) inside Ramadan, a non-odd night.
        val posted = fireMaghrib(114)
        assertEquals(posted.toString(), 1, posted.size)
        assertTrue(posted[0].first.contains("Iftar"))
        assertTrue(posted[0].second.contains("2357"))
        assertFalse(posted[0].second.contains("Laylatul Qadr"))
    }

    @Test
    fun `maghrib alarm on an odd night also mentions laylatul qadr`() {
        val posted = fireMaghrib(127)
        assertEquals(posted.toString(), 1, posted.size)
        assertTrue(posted[0].first.contains("Iftar"))
        assertTrue(posted[0].second.contains("Laylatul Qadr"))
    }

    @Test
    fun `maghrib alarm outside ramadan keeps the normal prayer text`() {
        val posted = fireMaghrib(0)
        assertEquals(posted.toString(), 1, posted.size)
        assertEquals("Maghrib Prayer", posted[0].first)
    }

    @Test
    fun `ramadan mode off keeps the normal prayer text even inside ramadan`() {
        val posted = fireMaghrib(114, ramadanMode = "OFF")
        assertEquals("Maghrib Prayer", posted.single().first)
    }
}
