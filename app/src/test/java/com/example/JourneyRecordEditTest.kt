package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.journey.JourneyEngine
import com.example.ui.noorup.journey.JourneyRepository
import com.example.ui.noorup.journey.Prayer
import com.example.ui.noorup.journey.PrayerStatus
import com.example.ui.noorup.journey.PrefsPrayerRecordSource
import com.example.ui.noorup.journey.RecordChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Tri-state prayer edits (Completed / Missed / Not recorded) through the existing tracker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyRecordEditTest {

    private lateinit var app: Application
    private val today: LocalDate = LocalDate.now()
    private val yesterday: LocalDate = today.minusDays(1)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs().edit().clear().commit()
    }

    private fun prefs() = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
    private fun source() = PrefsPrayerRecordSource(prefs())
    private fun vm() = NoorUpViewModel(app)

    @Test
    fun `completing a prayer writes the flag, count, timestamp and logged day`() {
        val vm = vm()
        vm.setPrayerRecord(today.toString(), Prayer.FAJR, RecordChoice.COMPLETED)
        assertTrue(source().isCompleted(today, Prayer.FAJR))
        assertEquals(1, prefs().getInt("prayer_record_${today}_completed", -1))
        assertTrue(prefs().getBoolean("user_has_logged_$today", false))
        assertNotNull(source().completedAt(today, Prayer.FAJR))
        assertNotNull(source().modifiedAt(today, Prayer.FAJR))
        assertEquals(1, vm.consistencyHistory.value.first().completedCount)
    }

    @Test
    fun `the existing toggle also records and clears the completion time`() {
        val vm = vm()
        vm.toggleHabitPrayer("ফজর")
        assertNotNull(source().completedAt(today, Prayer.FAJR))
        vm.toggleHabitPrayer("ফজর")
        assertNull(source().completedAt(today, Prayer.FAJR))
        assertFalse(source().isCompleted(today, Prayer.FAJR))
    }

    @Test
    fun `completed to missed to not recorded to completed`() {
        val vm = vm()
        val d = yesterday.toString()
        vm.setPrayerRecord(d, Prayer.DHUHR, RecordChoice.COMPLETED)
        vm.setPrayerRecord(d, Prayer.DHUHR, RecordChoice.MISSED)
        assertFalse(source().isCompleted(yesterday, Prayer.DHUHR))
        assertTrue(source().isConfirmedMissed(yesterday, Prayer.DHUHR))
        assertNull("completion time is cleared", source().completedAt(yesterday, Prayer.DHUHR))
        assertEquals(0, prefs().getInt("prayer_record_${yesterday}_completed", -1))

        vm.setPrayerRecord(d, Prayer.DHUHR, RecordChoice.UNRECORDED)
        assertFalse(source().isConfirmedMissed(yesterday, Prayer.DHUHR))
        assertFalse(source().isCompleted(yesterday, Prayer.DHUHR))
        assertEquals(PrayerStatus.UNRECORDED, JourneyEngine.dayRecord(source(), yesterday, today).statuses[Prayer.DHUHR.ordinal])

        vm.setPrayerRecord(d, Prayer.DHUHR, RecordChoice.MISSED)
        vm.setPrayerRecord(d, Prayer.DHUHR, RecordChoice.COMPLETED)
        assertTrue(source().isCompleted(yesterday, Prayer.DHUHR))
        assertFalse("completed clears the missed mark", source().isConfirmedMissed(yesterday, Prayer.DHUHR))
        assertNull(prefs().getString("prayer_state_${yesterday}_Dhuhr", null))
    }

    @Test
    fun `a day with only a missed mark still shows in the timeline and counts as tracked`() {
        val vm = vm()
        vm.setPrayerRecord(yesterday.toString(), Prayer.ASR, RecordChoice.MISSED)
        assertTrue(vm.consistencyHistory.value.any { it.dateKey == yesterday.toString() })
        assertTrue(JourneyEngine.dayRecord(source(), yesterday, today).tracked)
    }

    @Test
    fun `marking missed never touches Qada or the fulfilled total`() {
        val vm = vm()
        vm.setPrayerRecord(yesterday.toString(), Prayer.FAJR, RecordChoice.MISSED)
        assertEquals(0, vm.totalMadeUpQada.value)
        assertEquals(0, prefs().getInt("qada_made_up_total", 0))
        assertFalse(prefs().contains("prayer_transferred_qada_$yesterday"))
    }

    @Test
    fun `editing a past day leaves today's selected day alone`() {
        val vm = vm()
        vm.setPrayerRecord(yesterday.toString(), Prayer.FAJR, RecordChoice.COMPLETED)
        assertTrue(source().isCompleted(yesterday, Prayer.FAJR))
        assertTrue("today's checks are untouched", vm.habitPrayers.value.values.none { it })
        assertFalse(source().isCompleted(today, Prayer.FAJR))
    }

    @Test
    fun `five prayers yesterday give a streak of one and the journey agrees`() {
        val vm = vm()
        Prayer.entries.forEach { vm.setPrayerRecord(yesterday.toString(), it, RecordChoice.COMPLETED) }
        assertEquals(1, vm.currentStreak.value)
        assertEquals(100, JourneyEngine.dayRecord(source(), yesterday, today).percent)
        val old = vm.consistencyHistory.value.first { it.dateKey == yesterday.toString() }
        assertEquals(5, old.completedCount)
        assertEquals(100, old.scorePercent)
    }

    @Test
    fun `future dates are ignored`() {
        val vm = vm()
        // The ViewModel writes unrelated keys on background threads at startup, so compare only prayer records.
        fun records() = prefs().all.filterKeys {
            it.startsWith("prayer_habit_") || it.startsWith("prayer_record_") || it.startsWith("prayer_state_") ||
                it.startsWith("prayer_ts_") || it.startsWith("prayer_mod_") || it.startsWith("user_has_logged_") ||
                it == "prayer_tracking_start_date"
        }
        val before = records()
        vm.setPrayerRecord(today.plusDays(1).toString(), Prayer.FAJR, RecordChoice.COMPLETED)
        vm.setPrayerRecord("not-a-date", Prayer.FAJR, RecordChoice.COMPLETED)
        assertEquals(before, records())
    }

    @Test
    fun `every change bumps the journey revision so screens refresh`() {
        val vm = vm()
        val r0 = vm.journeyRevision.value
        vm.setPrayerRecord(today.toString(), Prayer.ISHA, RecordChoice.COMPLETED)
        val r1 = vm.journeyRevision.value
        vm.setPrayerRecord(yesterday.toString(), Prayer.ISHA, RecordChoice.MISSED)
        val r2 = vm.journeyRevision.value
        assertTrue(r1 > r0)
        assertTrue(r2 > r1)
    }

    @Test
    fun `routes open and close without a new tab`() {
        val vm = vm()
        vm.openJourneyRoute("friday")
        assertEquals("friday", vm.journeyRoute.value)
        assertEquals(5, vm.selectedTab.value)
        vm.setJourneyRoute(null)
        assertNull(vm.journeyRoute.value)
    }

    @Test
    fun `a pre-journey installation upgrades without changing any stored value`() {
        // Shape of a v1.5.2 user: three tracked days, a missed Qada, a streak.
        val d1 = today.minusDays(3)
        val d2 = today.minusDays(2)
        val e = prefs().edit()
        listOf(d1 to listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা"), d2 to listOf("ফজর", "যোহর")).forEach { (d, done) ->
            Prayer.entries.forEach { e.putBoolean("prayer_habit_${d}_${it.bnKey}", it.bnKey in done) }
            e.putInt("prayer_record_${d}_completed", done.size).putBoolean("user_has_logged_$d", true)
        }
        e.putString("prayer_tracking_start_date", d1.toString())
            .putInt("qada_count_Fajr", 2).putInt("qada_made_up_total", 4)
            .putBoolean("is_english", true).putBoolean("migration_zero_state_clean_v4", true)
            .commit()
        val legacy = prefs().all.toMap()

        val vm = vm()
        val repo = JourneyRepository(prefs())
        val data = repo.snapshot(today)
        repo.persist(data)

        // The journey reads the same records the old screens read.
        val dayOne = vm.consistencyHistory.value.first { it.dateKey == d1.toString() }
        assertEquals(dayOne.completedCount, JourneyEngine.dayRecord(repo.source, d1, today).completed)
        assertEquals(5, dayOne.completedCount)
        assertEquals(PrayerStatus.UNRECORDED, JourneyEngine.dayRecord(repo.source, d2, today).statuses[Prayer.ASR.ordinal])
        assertEquals(4, vm.totalMadeUpQada.value)

        // Nothing the older version stored was changed by reading or persisting the journey.
        val journeyOnly = prefs().all.filterKeys { it.startsWith("journey_") }
        assertTrue(journeyOnly.isNotEmpty())
        legacy.forEach { (k, v) ->
            if (k.startsWith("qada_") || k.startsWith("current_prayer")) return@forEach // existing sync may refresh these itself
            assertEquals("legacy key $k changed", v, prefs().all[k])
        }
    }
}
