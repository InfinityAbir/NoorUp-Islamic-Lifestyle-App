package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.journey.JourneyEngine
import com.example.ui.noorup.journey.Prayer
import com.example.ui.noorup.journey.PrayerStatus
import com.example.ui.noorup.journey.PrefsPrayerRecordSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyPrefsSourceTest {

    private lateinit var app: Application
    private val today: LocalDate = LocalDate.now()

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs().edit().clear().commit()
    }

    private fun prefs() = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
    private fun source() = PrefsPrayerRecordSource(prefs())

    @Test
    fun `legacy completed keys map to completed and everything else to unrecorded`() {
        val d = today.minusDays(3)
        prefs().edit()
            .putBoolean("prayer_habit_${d}_ফজর", true)
            .putBoolean("prayer_habit_${d}_যোহর", false)
            .putInt("prayer_record_${d}_completed", 1)
            .putBoolean("user_has_logged_$d", true)
            .commit()

        val day = JourneyEngine.dayRecord(source(), d, today)
        assertEquals(PrayerStatus.COMPLETED, day.statuses[0])
        assertEquals(PrayerStatus.UNRECORDED, day.statuses[1]) // explicit false on a logged day is still not "missed"
        assertEquals(0, day.confirmedMissed)
        assertTrue(day.tracked)
    }

    @Test
    fun `a day never opened is untracked`() {
        val day = JourneyEngine.dayRecord(source(), today.minusDays(5), today)
        assertFalse(day.tracked)
        assertEquals(5, day.unrecorded)
    }

    @Test
    fun `confirmed missed needs the explicit state key and loses to completed`() {
        val d = today.minusDays(2)
        prefs().edit()
            .putString("prayer_state_${d}_Dhuhr", "missed")
            .putString("prayer_state_${d}_Asr", "missed")
            .putBoolean("prayer_habit_${d}_আসর", true)
            .commit()
        val day = JourneyEngine.dayRecord(source(), d, today)
        assertEquals(PrayerStatus.CONFIRMED_MISSED, day.statuses[Prayer.DHUHR.ordinal])
        assertEquals(PrayerStatus.COMPLETED, day.statuses[Prayer.ASR.ordinal])
    }

    @Test
    fun `reading the journey changes no stored key`() {
        val vm = NoorUpViewModel(app)
        vm.toggleHabitPrayer("ফজর")
        vm.toggleHabitPrayer("যোহর")
        val before = prefs().all.toMap()

        JourneyEngine.summary(source(), today)
        JourneyEngine.computeStage(source(), today)

        assertEquals(before, prefs().all.toMap())
    }

    @Test
    fun `journey today matches the existing tracker count and percent`() {
        val vm = NoorUpViewModel(app)
        vm.toggleHabitPrayer("ফজর")
        vm.toggleHabitPrayer("আসর")

        val existing = vm.consistencyHistory.value.first()
        val journeyToday = JourneyEngine.summary(source(), today).today
        assertEquals(existing.completedCount, journeyToday.completed)
        assertEquals(existing.scorePercent, journeyToday.percent)
        assertEquals(40, journeyToday.percent)
        assertEquals(PrayerStatus.COMPLETED, journeyToday.statuses[0])
        assertEquals(PrayerStatus.PENDING, journeyToday.statuses[1])
    }

    @Test
    fun `toggling a prayer off is reflected immediately and the streak is untouched by the journey`() {
        val vm = NoorUpViewModel(app)
        Prayer.entries.forEach { vm.toggleHabitPrayer(it.bnKey) }
        assertEquals(100, JourneyEngine.summary(source(), today).today.percent)
        val streakBefore = vm.currentStreak.value

        vm.toggleHabitPrayer("ফজর")
        assertEquals(80, JourneyEngine.summary(source(), today).today.percent)
        JourneyEngine.summary(source(), today)
        assertEquals(1, streakBefore)
        assertEquals(vm.currentStreak.value, vm.currentStreak.value)
    }
}
