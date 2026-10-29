package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.ramadan.RamadanLogic
import com.example.ui.noorup.ramadan.RamadanNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.GregorianCalendar

/** The Maghrib alarm doubles as the Iftar alert during Ramadan and adds a Laylatul Qadr line on odd nights. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RamadanAlertTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Walks the real calendar around Ramadan 1448 (Feb-Mar 2027) and returns the first date whose Hijri day matches. */
    private fun dateWithRamadanDay(day: Int): Calendar {
        val cal = GregorianCalendar(2027, Calendar.JANUARY, 25)
        repeat(70) {
            val h = RamadanLogic.hijriNow(context, cal)
            if (h.month == RamadanLogic.RAMADAN_MONTH && h.day == day) return cal.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        throw AssertionError("no Ramadan day $day found")
    }

    @Test
    fun `iftar alert in english and bangla during ramadan`() {
        val cal = dateWithRamadanDay(10)
        val en = RamadanNotifications.maghribOverride(context, true, cal)!!
        assertTrue(en.first.contains("Iftar"))
        assertTrue(en.second.contains("2357")) // Abu Dawud reference for the dua
        val bn = RamadanNotifications.maghribOverride(context, false, cal)!!
        assertTrue(bn.first.contains("ইফতার"))
    }

    @Test
    fun `odd night adds the laylatul qadr reminder and even night does not`() {
        // After Maghrib on day 26 it is already the 27th night.
        val odd = RamadanNotifications.maghribOverride(context, true, dateWithRamadanDay(26))!!
        assertTrue(odd.second.contains("Laylatul Qadr"))
        assertTrue(odd.second.contains("27"))
        val bnOdd = RamadanNotifications.maghribOverride(context, false, dateWithRamadanDay(26))!!
        assertTrue(bnOdd.second.contains("২৭"))

        val even = RamadanNotifications.maghribOverride(context, true, dateWithRamadanDay(25))!!
        assertFalse(even.second.contains("Laylatul Qadr"))
    }

    @Test
    fun `no ramadan wording outside the month or when mode is off`() {
        assertNull(RamadanNotifications.maghribOverride(context, true, GregorianCalendar(2026, Calendar.OCTOBER, 30)))
        context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE).edit().putString("ramadan_mode", "OFF").commit()
        assertNull(RamadanNotifications.maghribOverride(context, true, dateWithRamadanDay(26)))
        assertNotNull(dateWithRamadanDay(26))
        assertEquals(26, RamadanLogic.hijriNow(context, dateWithRamadanDay(26)).day)
    }
}
