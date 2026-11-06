package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.CalculationMethod
import com.example.ui.noorup.JuristicMethod
import com.example.ui.noorup.SolarPrayerEngine
import com.example.ui.noorup.WidgetUpdateScheduler
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The home-screen widget treats a prohibited window as inclusive of its last minute, so it only changes
 * on the NEXT minute. The wake-up must land there; waking on the printed end minute re-rendered the same
 * "prohibited" screen and left it stale for up to 15 minutes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetTransitionTimingTest {

  private val zone = TimeZone.getTimeZone("Asia/Dhaka")

  private fun prefs() =
    ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

  private fun at(minuteOfDay: Int, second: Int): Long =
    Calendar.getInstance(zone).apply {
      set(2026, Calendar.NOVEMBER, 6, minuteOfDay / 60, minuteOfDay % 60, second)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

  private fun timesToday() = SolarPrayerEngine.calculateTimes(
    latitude = 23.8759,
    longitude = 90.3795,
    calendar = Calendar.getInstance(zone).apply { timeInMillis = at(12 * 60, 0) },
    timezoneOffset = 6.0,
    method = CalculationMethod.KARACHI,
    juristic = JuristicMethod.HANAFI
  )

  private fun givenDhaka() {
    prefs().edit()
      .putString("selected_city_zone_id", "Asia/Dhaka")
      .putString("selected_city_lat_dbl", "23.8759")
      .putString("selected_city_lng_dbl", "90.3795")
      .putString("calculation_method", CalculationMethod.KARACHI.name)
      .putString("juristic_method", JuristicMethod.HANAFI.name)
      .commit()
  }

  @Test
  fun `wake-up lands the minute after the sunrise prohibited window ends`() {
    givenDhaka()
    val end = NoorUpWidgetProvider.parseTimeToMins(timesToday().makruhSunriseEnd)

    // The last prohibited minute: the screen is still correct, but the very next minute changes it.
    val next = WidgetUpdateScheduler.getNextTransitionMillis(
      ApplicationProvider.getApplicationContext(), nowMillis = at(end, 30)
    )
    assertEquals(at(end + 1, 0), next)
  }

  @Test
  fun `wake-up lands the minute after the sunset prohibited window ends`() {
    givenDhaka()
    val maghrib = NoorUpWidgetProvider.parseTimeToMins(timesToday().maghribStart)

    val next = WidgetUpdateScheduler.getNextTransitionMillis(
      ApplicationProvider.getApplicationContext(), nowMillis = at(maghrib, 30)
    )
    assertEquals(at(maghrib + 1, 0), next)
  }

  @Test
  fun `a city in another timezone is scheduled on its own wall clock`() {
    // Makkah (UTC+3) while the test device runs in another zone.
    prefs().edit()
      .putString("selected_city_zone_id", "Asia/Riyadh")
      .putString("selected_city_lat_dbl", "21.4225")
      .putString("selected_city_lng_dbl", "39.8262")
      .putString("calculation_method", CalculationMethod.KARACHI.name)
      .putString("juristic_method", JuristicMethod.HANAFI.name)
      .commit()
    val riyadh = TimeZone.getTimeZone("Asia/Riyadh")
    val times = SolarPrayerEngine.calculateTimes(
      latitude = 21.4225,
      longitude = 39.8262,
      calendar = Calendar.getInstance(riyadh).apply {
        set(2026, Calendar.NOVEMBER, 6, 12, 0, 0)
      },
      timezoneOffset = 3.0,
      method = CalculationMethod.KARACHI,
      juristic = JuristicMethod.HANAFI
    )
    val dhuhr = NoorUpWidgetProvider.parseTimeToMins(times.dhuhrStart)
    fun riyadhAt(minuteOfDay: Int, second: Int) = Calendar.getInstance(riyadh).apply {
      set(2026, Calendar.NOVEMBER, 6, minuteOfDay / 60, minuteOfDay % 60, second)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val next = WidgetUpdateScheduler.getNextTransitionMillis(
      ApplicationProvider.getApplicationContext(), nowMillis = riyadhAt(dhuhr, 0) - 30_000L
    )
    assertEquals(riyadhAt(dhuhr, 0), next)
  }
}
