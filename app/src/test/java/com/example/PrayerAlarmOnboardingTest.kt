package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.CalculationMethod
import com.example.ui.noorup.JuristicMethod
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.PrayerAlarmOnboarding
import com.example.ui.noorup.PrayerAlarmSetupStatus
import com.example.ui.noorup.PrayerNotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Home-screen prayer-alarm onboarding: status resolution, permission-driven
 * retries, and the rule that setup only counts once alarms are actually armed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerAlarmOnboardingTest {

    private lateinit var app: Application
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun status(
        complete: Boolean = false,
        dismissed: Boolean = false,
        granted: Boolean = true,
        exact: Boolean = true,
        denied: Boolean = false
    ) = PrayerAlarmOnboarding.resolveStatus(
        onboardingComplete = complete,
        promoDismissed = dismissed,
        notificationsGranted = granted,
        exactAlarmAccess = exact,
        notificationDenied = denied
    )

    // --- Status resolution (first launch / returning / revoked) ---

    @Test
    fun `fresh install shows the onboarding banner`() {
        assertEquals(PrayerAlarmSetupStatus.NOT_STARTED, status(granted = false))
    }

    @Test
    fun `tapping later keeps a compact retry row instead of the banner`() {
        assertEquals(PrayerAlarmSetupStatus.PROMO_DISMISSED, status(dismissed = true))
    }

    @Test
    fun `completed setup with permissions hides the onboarding ui`() {
        assertEquals(PrayerAlarmSetupStatus.ACTIVE, status(complete = true))
    }

    @Test
    fun `revoked notification permission after setup surfaces a retry`() {
        assertEquals(
            PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED,
            status(complete = true, granted = false)
        )
    }

    @Test
    fun `missing exact alarm access keeps setup blocked`() {
        assertEquals(
            PrayerAlarmSetupStatus.EXACT_ALARM_BLOCKED,
            status(complete = true, exact = false)
        )
    }

    @Test
    fun `denied during onboarding is not reported as a fresh start`() {
        assertEquals(
            PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED,
            status(granted = false, denied = true)
        )
        // ...and a granted user who dismissed is not treated as denied.
        assertEquals(
            PrayerAlarmSetupStatus.PROMO_DISMISSED,
            status(dismissed = true, granted = true, denied = true)
        )
    }

    // --- Permission helpers ---

    @Test
    fun `notification helper follows the runtime grant on android 13`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(PrayerAlarmOnboarding.notificationsGranted(app))
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(PrayerAlarmOnboarding.notificationsGranted(app))
    }

    @Test
    @Config(sdk = [29])
    fun `notification helper is implicitly granted before android 13`() {
        assertTrue(PrayerAlarmOnboarding.notificationsGranted(app))
    }

    @Test
    @Config(sdk = [26])
    fun `exact alarm access is not required before android 12`() {
        // minSdk 26: AlarmClock alarms need no special access on these versions.
        assertTrue(PrayerAlarmOnboarding.exactAlarmAccessGranted(app))
    }

    // --- Scheduling verification ---

    private fun schedule() = PrayerNotificationScheduler.scheduleAllPrayerAlerts(
        context = app,
        latitude = 23.8103,
        longitude = 90.4125,
        method = CalculationMethod.KARACHI,
        juristic = JuristicMethod.HANAFI,
        isEnglish = false,
        zoneId = "Asia/Dhaka"
    )

    @Test
    fun `scheduling records a verifiable armed schedule`() {
        prefs.edit().putBoolean("prayer_notifications_enabled", true).commit()
        schedule()

        assertEquals(5, PrayerNotificationScheduler.scheduledPrayerAlarmCount(app))
        assertTrue(
            PrayerNotificationScheduler.nextPrayerAlarmTriggerMillis(app) >
                System.currentTimeMillis()
        )
        assertTrue(PrayerNotificationScheduler.hasVerifiedScheduledAlarms(app))
    }

    @Test
    fun `individual prayer opt-outs are reflected in the verification`() {
        prefs.edit()
            .putBoolean("prayer_notifications_enabled", true)
            .putBoolean("prayer_enabled_Fajr", false)
            .putBoolean("prayer_enabled_Asr", false)
            .commit()
        schedule()

        assertEquals(3, PrayerNotificationScheduler.scheduledPrayerAlarmCount(app))
        assertTrue(PrayerNotificationScheduler.hasVerifiedScheduledAlarms(app))
    }

    @Test
    fun `disabled notifications clear the verification record`() {
        prefs.edit().putBoolean("prayer_notifications_enabled", false).commit()
        schedule()

        assertEquals(0, PrayerNotificationScheduler.scheduledPrayerAlarmCount(app))
        assertEquals(0L, PrayerNotificationScheduler.nextPrayerAlarmTriggerMillis(app))
        assertFalse(PrayerNotificationScheduler.hasVerifiedScheduledAlarms(app))
    }

    // --- Onboarding state via the ViewModel ---

    @Test
    fun `dismissing the promo persists without completing setup`() {
        val vm = NoorUpViewModel(app)
        vm.dismissPrayerAlarmPromo()

        assertTrue(vm.isPrayerAlarmPromoDismissed.value)
        assertFalse(vm.isPrayerAlarmOnboardingComplete.value)
        assertTrue(prefs.getBoolean(PrayerAlarmOnboarding.KEY_PROMO_DISMISSED, false))
        assertFalse(prefs.getBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, false))

        val recreated = NoorUpViewModel(app)
        assertTrue(recreated.isPrayerAlarmPromoDismissed.value)
        assertFalse(recreated.isPrayerAlarmOnboardingComplete.value)
    }

    @Test
    fun `returning user with completed setup stays hidden`() {
        prefs.edit().putBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, true).commit()
        val vm = NoorUpViewModel(app)

        assertTrue(vm.isPrayerAlarmOnboardingComplete.value)
        assertEquals(
            PrayerAlarmSetupStatus.ACTIVE,
            status(complete = vm.isPrayerAlarmOnboardingComplete.value)
        )
    }

    @Test
    fun `startup scheduling never completes onboarding`() {
        val vm = NoorUpViewModel(app)
        vm.schedulePrayerNotifications(app)

        awaitTrue { PrayerNotificationScheduler.scheduledPrayerAlarmCount(app) == 5 }
        assertFalse(vm.isPrayerAlarmOnboardingComplete.value)
        assertFalse(prefs.getBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, false))
    }

    @Test
    @Config(sdk = [29])
    fun `enabling from home completes onboarding only after alarms are armed`() {
        val vm = NoorUpViewModel(app)
        var verified: Boolean? = null
        vm.enablePrayerAlarms(app) { verified = it }

        awaitTrue { verified != null }
        assertEquals(true, verified)
        assertTrue(vm.isPrayerAlarmOnboardingComplete.value)
        assertTrue(prefs.getBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, false))
        assertEquals(5, PrayerNotificationScheduler.scheduledPrayerAlarmCount(app))
        assertTrue(PrayerNotificationScheduler.hasVerifiedScheduledAlarms(app))
    }

    @Test
    fun `denied notification permission leaves onboarding incomplete and retryable`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val vm = NoorUpViewModel(app)
        var verified: Boolean? = null
        vm.enablePrayerAlarms(app) { verified = it }

        assertEquals(false, verified)
        assertFalse(vm.isPrayerAlarmOnboardingComplete.value)
        assertTrue(vm.isPrayerAlarmNotificationDenied.value)
        assertEquals(
            PrayerAlarmSetupStatus.NOTIFICATIONS_DENIED,
            status(granted = false, denied = vm.isPrayerAlarmNotificationDenied.value)
        )

        // Retry after granting in Settings must be able to finish setup.
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        vm.clearPrayerAlarmNotificationDenied()
        vm.enablePrayerAlarms(app) { verified = it }
        awaitTrue { verified == true }
        assertTrue(vm.isPrayerAlarmOnboardingComplete.value)
    }

    @Test
    fun `missing exact alarm access blocks completion`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val vm = NoorUpViewModel(app)
        var verified: Boolean? = null
        vm.enablePrayerAlarms(app) { verified = it }

        assertEquals(false, verified)
        assertFalse(vm.isPrayerAlarmOnboardingComplete.value)
        // Setup is still unfinished, so the banner (and its exact-alarm dialog)
        // remains the retry path; the blocked status is for post-setup revokes.
        assertEquals(
            PrayerAlarmSetupStatus.NOT_STARTED,
            status(granted = true, exact = false)
        )
        assertEquals(
            PrayerAlarmSetupStatus.EXACT_ALARM_BLOCKED,
            status(complete = true, granted = true, exact = false)
        )
    }

    // --- helpers ---

    private fun awaitTrue(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(20)
        }
        fail("condition not met within ${timeoutMs}ms")
    }
}
