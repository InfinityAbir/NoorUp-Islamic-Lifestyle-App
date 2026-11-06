package com.example

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.PrayerNotificationHelper
import com.example.ui.noorup.backup.BackupCodec
import com.example.ui.noorup.journey.FridayReminderScheduler
import com.example.ui.noorup.journey.JourneyPrefs
import com.example.ui.noorup.journey.JourneyRepository
import com.example.ui.noorup.journey.MilestoneId
import com.example.ui.noorup.journey.ReflectionEntry
import com.example.ui.noorup.journey.ReflectionStore
import com.example.ui.noorup.journey.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyStoreTest {

    private lateinit var app: Application
    private val friday = LocalDate.of(2026, 11, 6)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs().edit().clear().commit()
        PrayerNotificationHelper.createNotificationChannels(app, force = true)
    }

    private fun prefs() = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
    private fun store() = ReflectionStore(prefs())

    private fun entry(date: LocalDate = friday, q1: Int? = 0, intention: String = "") = ReflectionEntry(
        fridayDate = date.toString(),
        periodStart = date.minusDays(7).toString(),
        periodEnd = date.minusDays(1).toString(),
        q1 = q1, q2 = 2, q3 = "note", intention = intention,
        snapCompleted = 20, snapTrackedDays = 7, snapPercent = 57
    )

    // ------------------------------------------------------------ reflection store

    @Test
    fun `reflection save then get round trips every field`() {
        val saved = store().save(entry(intention = "Pray Fajr on time"), 1_000L)
        val loaded = store().get(friday)!!
        assertEquals(saved, loaded)
        assertEquals(0, loaded.q1)
        assertEquals(2, loaded.q2)
        assertEquals("note", loaded.q3)
        assertEquals("Pray Fajr on time", loaded.intention)
        assertEquals(1_000L, loaded.createdAt)
        assertEquals(57, loaded.snapPercent)
    }

    @Test
    fun `all questions are optional`() {
        store().save(ReflectionEntry(fridayDate = friday.toString()), 5L)
        val e = store().get(friday)!!
        assertNull(e.q1)
        assertNull(e.q2)
        assertEquals("", e.q3)
        assertEquals("", e.intention)
    }

    @Test
    fun `saving the same friday twice edits instead of duplicating and keeps the creation time`() {
        store().save(entry(q1 = 0), 1_000L)
        store().save(entry(q1 = 3), 9_000L)
        assertEquals(1, store().all().size)
        val e = store().get(friday)!!
        assertEquals(3, e.q1)
        assertEquals(1_000L, e.createdAt)
        assertEquals(9_000L, e.updatedAt)
    }

    @Test
    fun `reflections list newest friday first`() {
        listOf(friday.minusDays(14), friday, friday.minusDays(7)).forEach { store().save(entry(it), 1L) }
        assertEquals(
            listOf(friday, friday.minusDays(7), friday.minusDays(14)).map { it.toString() },
            store().all().map { it.fridayDate }
        )
    }

    @Test
    fun `delete removes only that reflection`() {
        store().save(entry(friday), 1L)
        store().save(entry(friday.minusDays(7)), 1L)
        store().delete(friday)
        assertNull(store().get(friday))
        assertFalse(store().hasEntry(friday))
        assertNotNull(store().get(friday.minusDays(7)))
    }

    @Test
    fun `deleting the intention keeps the rest of the reflection`() {
        store().save(entry(intention = "Avoid delaying Salah"), 1L)
        store().clearIntention(friday, 2L)
        val e = store().get(friday)!!
        assertEquals("", e.intention)
        assertEquals("note", e.q3)
        assertEquals(2L, e.updatedAt)
    }

    @Test
    fun `long text is trimmed and capped`() {
        val saved = store().save(entry().copy(q3 = "  " + "x".repeat(2000) + "  ", intention = "y".repeat(500)), 1L)
        assertEquals(ReflectionStore.MAX_TEXT, saved.q3.length)
        assertEquals(ReflectionStore.MAX_INTENTION, saved.intention.length)
    }

    @Test
    fun `a corrupt stored reflection is ignored instead of crashing`() {
        prefs().edit().putString("reflection_2026-11-06", "{not json").commit()
        assertNull(store().get(friday))
        assertTrue(store().all().isEmpty())
    }

    @Test
    fun `active intention covers the week from friday and is never marked achieved`() {
        store().save(entry(intention = "Stay consistent"), 1L)
        assertEquals("Stay consistent", store().activeIntention(friday)!!.intention)
        assertEquals("Stay consistent", store().activeIntention(friday.plusDays(6))!!.intention)
        assertNull(store().activeIntention(friday.plusDays(7)))
        assertNull(store().activeIntention(friday.minusDays(1)))
    }

    // ------------------------------------------------------------ journey repository

    @Test
    fun `persist is idempotent and writes only journey keys`() {
        val today = LocalDate.of(2026, 11, 6)
        (1..40).forEach { d ->
            val date = today.minusDays(d.toLong())
            prefs().edit().putBoolean("user_has_logged_$date", true)
                .putBoolean("prayer_habit_${date}_ফজর", true).commit()
        }
        prefs().edit().putString("prayer_tracking_start_date", today.minusDays(40).toString()).commit()
        val before = prefs().all.toMap()
        val repo = JourneyRepository(prefs())
        val data = repo.snapshot(today)
        repo.persist(data)
        val afterFirst = prefs().all.toMap()
        repo.persist(repo.snapshot(today))
        assertEquals(afterFirst, prefs().all.toMap())

        val added = afterFirst.keys - before.keys
        assertTrue("only journey_ keys may be added: $added", added.all { it.startsWith("journey_") })
        before.forEach { (k, v) -> assertEquals("existing key $k must not change", v, afterFirst[k]) }
        assertNotNull(prefs().getString("journey_milestone_DAYS_30", null))
    }

    @Test
    fun `milestones and highest stage are never taken away`() {
        val today = LocalDate.of(2026, 11, 6)
        (1..35).forEach { d ->
            val date = today.minusDays(d.toLong())
            Prayer5(date)
        }
        val repo = JourneyRepository(prefs())
        repo.persist(repo.snapshot(today))
        val first = repo.snapshot(today)
        assertTrue(first.milestones.first { it.id == MilestoneId.DAYS_30 }.unlockedOn != null)
        val stageBefore = first.summary.stage.stage

        // The user later wipes their records: nothing already earned disappears.
        prefs().all.keys.filter { it.startsWith("prayer_habit_") || it.startsWith("user_has_logged_") }
            .forEach { prefs().edit().remove(it).commit() }
        val after = repo.snapshot(today)
        assertTrue(after.milestones.first { it.id == MilestoneId.DAYS_30 }.unlockedOn != null)
        assertTrue(after.maxStage.number >= stageBefore.number)
        assertEquals(Stage.FIRST_LIGHT, after.summary.stage.stage) // today's stage may fall; the record of it does not
    }

    private fun Prayer5(date: LocalDate) {
        val e = prefs().edit().putBoolean("user_has_logged_$date", true)
        com.example.ui.noorup.journey.Prayer.entries.forEach { e.putBoolean("prayer_habit_${date}_${it.bnKey}", true) }
        e.commit()
    }

    @Test
    fun `milestone unlock date is stored once and the seen flag starts false`() {
        val today = LocalDate.of(2026, 11, 6)
        Prayer5(today.minusDays(1))
        val repo = JourneyRepository(prefs())
        repo.persist(repo.snapshot(today))
        val m = repo.snapshot(today).milestones.first { it.id == MilestoneId.FIRST_DAY }
        assertEquals(today.minusDays(1), m.unlockedOn)
        assertFalse(m.seen)
        repo.markSeen(MilestoneId.FIRST_DAY)
        assertTrue(repo.snapshot(today).milestones.first { it.id == MilestoneId.FIRST_DAY }.seen)

        // Another 5/5 day later does not move the stored date.
        Prayer5(today.minusDays(3))
        assertEquals(today.minusDays(1), repo.snapshot(today).milestones.first { it.id == MilestoneId.FIRST_DAY }.unlockedOn)
    }

    // ------------------------------------------------------------ preferences

    @Test
    fun `journey preference defaults are conservative`() {
        val p = JourneyPrefs(prefs())
        assertTrue(p.animationsEnabled)
        assertFalse(p.reducedMotion)
        assertFalse(p.fridayReminderEnabled)
        assertFalse(p.intentionReminderEnabled)
        assertTrue(p.skipIfReflectionDone)
        assertTrue(p.eveningOnlyIfPending)
        assertEquals(20 * 60, p.fridayMinutes)
    }

    @Test
    fun `motion is allowed only when every setting agrees`() {
        val p = JourneyPrefs(prefs())
        assertTrue(p.motionAllowed(systemReduced = false))
        assertFalse(p.motionAllowed(systemReduced = true))
        p.reducedMotion = true
        assertFalse(p.motionAllowed(false))
        p.reducedMotion = false
        p.animationsEnabled = false
        assertFalse(p.motionAllowed(false))
    }

    @Test
    fun `reminder minutes are kept inside a day`() {
        val p = JourneyPrefs(prefs())
        p.fridayMinutes = 99_999
        assertEquals(1439, p.fridayMinutes)
        p.fridayMinutes = -5
        assertEquals(0, p.fridayMinutes)
    }

    @Test
    fun `evening review stays quiet only when all five are completed and the setting is on`() {
        val date = "2026-11-05"
        assertFalse(JourneyPrefs.shouldSkipEvening(prefs(), date))
        com.example.ui.noorup.journey.Prayer.entries.take(4).forEach { prefs().edit().putBoolean("prayer_habit_${date}_${it.bnKey}", true).commit() }
        assertFalse(JourneyPrefs.shouldSkipEvening(prefs(), date))
        prefs().edit().putBoolean("prayer_habit_${date}_এশা", true).commit()
        assertTrue(JourneyPrefs.shouldSkipEvening(prefs(), date))
        JourneyPrefs(prefs()).eveningOnlyIfPending = false
        assertFalse(JourneyPrefs.shouldSkipEvening(prefs(), date))
    }

    // ------------------------------------------------------------ backup

    @Test
    fun `new journey keys are part of the backup and transient keys are not`() {
        listOf(
            "prayer_state_2026-11-06_Fajr", "prayer_ts_2026-11-06_Fajr", "prayer_mod_2026-11-06_Fajr",
            "journey_stage_max", "journey_milestone_DAYS_30", "reflection_2026-11-06",
            "friday_reminder_enabled", "friday_reminder_minutes", "intention_reminder_enabled"
        ).forEach { assertTrue("$it should be backed up", BackupCodec.isBackedUp(it)) }
        listOf("last_friday_reflection_posted", "last_intention_reminder_posted").forEach {
            assertFalse("$it is device-specific", BackupCodec.isBackedUp(it))
        }
    }

    // ------------------------------------------------------------ scheduling

    private fun zone() = ZoneId.systemDefault()

    @Test
    fun `next trigger is the next friday at the chosen time and strictly in the future`() {
        val z = ZoneId.of("Asia/Dhaka")
        fun ms(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0) = ZonedDateTime.of(y, m, d, h, min, s, 0, z).toInstant().toEpochMilli()
        val twenty = 20 * 60
        assertEquals(ms(2026, 11, 6, 20, 0), FridayReminderScheduler.nextTrigger(ms(2026, 11, 4, 12, 0), z, DayOfWeek.FRIDAY, twenty))
        assertEquals(ms(2026, 11, 6, 20, 0), FridayReminderScheduler.nextTrigger(ms(2026, 11, 6, 19, 59, 59), z, DayOfWeek.FRIDAY, twenty))
        assertEquals(ms(2026, 11, 13, 20, 0), FridayReminderScheduler.nextTrigger(ms(2026, 11, 6, 20, 0), z, DayOfWeek.FRIDAY, twenty))
        assertEquals(ms(2026, 11, 13, 20, 0), FridayReminderScheduler.nextTrigger(ms(2026, 11, 6, 21, 0), z, DayOfWeek.FRIDAY, twenty))
        assertEquals(ms(2026, 11, 9, 20, 0), FridayReminderScheduler.nextTrigger(ms(2026, 11, 7, 8, 0), z, DayOfWeek.MONDAY, twenty))
    }

    @Test
    fun `a timezone change moves the trigger to the new local time`() {
        val now = ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val dhaka = FridayReminderScheduler.nextTrigger(now, ZoneId.of("Asia/Dhaka"), DayOfWeek.FRIDAY, 20 * 60)
        val london = FridayReminderScheduler.nextTrigger(now, ZoneId.of("Europe/London"), DayOfWeek.FRIDAY, 20 * 60)
        assertEquals(6 * 3_600_000L, london - dhaka) // 20:00 in London is 6 hours after 20:00 in Dhaka
    }

    @Test
    fun `daylight saving shifts do not move the local reminder time`() {
        val ny = ZoneId.of("America/New_York")
        val beforeShift = ZonedDateTime.of(2026, 3, 6, 20, 30, 0, 0, ny).toInstant().toEpochMilli()
        val trigger = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(FridayReminderScheduler.nextTrigger(beforeShift, ny, DayOfWeek.FRIDAY, 20 * 60)), ny)
        assertEquals(LocalDate.of(2026, 3, 13), trigger.toLocalDate())
        assertEquals(20, trigger.hour)
        assertEquals(0, trigger.minute)
    }

    private fun alarmManager(): AlarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * NoorUpApplication re-arms prayer, daily and Friday alarms on a background thread at startup.
     * Let it finish before a test changes settings, so it cannot re-arm something the test just cancelled.
     */
    private fun settleStartup() {
        Thread.sleep(500)
    }

    /**
     * The application arms prayer/garden/hadith alarms itself (on a background thread), so only
     * the Journey alarms (request codes 7003 Friday, 7004 intention) are counted here.
     */
    private fun journeyAlarms(code: Int? = null) = shadowOf(alarmManager()).scheduledAlarms.filter {
        val c = shadowOf(it.operation).requestCode
        if (code == null) c == FridayReminderScheduler.REQ_FRIDAY || c == FridayReminderScheduler.REQ_INTENTION else c == code
    }

    @Test
    fun `scheduling arms one friday alarm, is idempotent, and disabling cancels it`() {
        settleStartup()
        val settings = JourneyPrefs(prefs())
        settings.fridayReminderEnabled = true
        val now = ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, zone()).toInstant().toEpochMilli()

        FridayReminderScheduler.scheduleAll(app, now)
        FridayReminderScheduler.scheduleAll(app, now)
        val friday = journeyAlarms(FridayReminderScheduler.REQ_FRIDAY)
        assertEquals(1, friday.size)
        assertEquals(ZonedDateTime.of(2026, 11, 6, 20, 0, 0, 0, zone()).toInstant().toEpochMilli(), friday[0].triggerAtTime)
        assertEquals("intention reminder stays off", 0, journeyAlarms(FridayReminderScheduler.REQ_INTENTION).size)

        settings.fridayReminderEnabled = false
        FridayReminderScheduler.scheduleAll(app, now)
        assertEquals(0, journeyAlarms().size)
    }

    @Test
    fun `scheduling still arms an alarm whichever kind the device allows`() {
        settleStartup()
        JourneyPrefs(prefs()).fridayReminderEnabled = true
        FridayReminderScheduler.scheduleAll(app, System.currentTimeMillis())
        assertEquals(1, journeyAlarms().size)
    }

    @Test
    fun `turning prayer notifications off pauses the journey reminders`() {
        settleStartup()
        JourneyPrefs(prefs()).fridayReminderEnabled = true
        JourneyPrefs(prefs()).intentionReminderEnabled = true
        FridayReminderScheduler.scheduleAll(app)
        assertEquals(2, journeyAlarms().size)
        prefs().edit().putBoolean("prayer_notifications_enabled", false).commit()
        FridayReminderScheduler.scheduleAll(app)
        assertEquals(0, journeyAlarms().size)
    }

    // ------------------------------------------------------------ posting decisions

    private fun notifications() = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).allNotifications

    @Test
    fun `friday reminder posts once per friday`() {
        JourneyPrefs(prefs()).fridayReminderEnabled = true
        assertTrue(FridayReminderScheduler.shouldPostFriday(prefs(), friday))
        FridayReminderScheduler.onFridayFire(app, friday)
        assertEquals(1, notifications().size)
        assertFalse("a duplicate delivery must not post again", FridayReminderScheduler.shouldPostFriday(prefs(), friday))
        assertFalse(FridayReminderScheduler.shouldPostFriday(prefs(), friday.plusDays(1)))
        assertTrue("next week is a new Friday", FridayReminderScheduler.shouldPostFriday(prefs(), friday.plusDays(7)))
    }

    @Test
    fun `friday reminder is skipped when the reflection is already saved`() {
        JourneyPrefs(prefs()).fridayReminderEnabled = true
        store().save(entry(), 1L)
        assertFalse(FridayReminderScheduler.shouldPostFriday(prefs(), friday))
        FridayReminderScheduler.onFridayFire(app, friday)
        assertEquals(0, notifications().size)
        JourneyPrefs(prefs()).skipIfReflectionDone = false
        assertTrue(FridayReminderScheduler.shouldPostFriday(prefs(), friday))
    }

    @Test
    fun `friday reminder respects the opt in and the master switch`() {
        assertFalse(FridayReminderScheduler.shouldPostFriday(prefs(), friday))
        JourneyPrefs(prefs()).fridayReminderEnabled = true
        prefs().edit().putBoolean("prayer_notifications_enabled", false).commit()
        assertFalse(FridayReminderScheduler.shouldPostFriday(prefs(), friday))
    }

    @Test
    fun `intention reminder needs an active intention and posts once a day`() {
        JourneyPrefs(prefs()).intentionReminderEnabled = true
        val monday = friday.plusDays(3)
        assertNull(FridayReminderScheduler.shouldPostIntention(prefs(), monday))
        store().save(entry(intention = "Pray on time"), 1L)
        assertEquals("Pray on time", FridayReminderScheduler.shouldPostIntention(prefs(), monday)!!.intention)
        FridayReminderScheduler.onIntentionFire(app, monday)
        assertEquals(1, notifications().size)
        assertNull(FridayReminderScheduler.shouldPostIntention(prefs(), monday))
        assertNull("outside the week of the intention", FridayReminderScheduler.shouldPostIntention(prefs(), friday.plusDays(10)))
    }
}
