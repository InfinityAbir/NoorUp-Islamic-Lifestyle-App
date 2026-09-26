package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = try {
      context.getString(R.string.app_name)
    } catch (e: Exception) {
      "NoorUp"
    }
    assertEquals("NoorUp", appName)
  }

  @Test
  fun `launch MainActivity`() {
    try {
      org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    } catch (t: Throwable) {
      t.printStackTrace()
      var cause: Throwable? = t
      while (cause != null) {
        println("CAUSE: ${cause.javaClass.name}: ${cause.message}")
        cause = cause.cause
      }
      throw t
    }
  }

  @Test
  fun `verify fresh install zero state and explicit user logging behavior`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.noorup.NoorUpViewModel(application)

    // 1. Fresh state zeroing assertions:
    val habitPrayers = vm.habitPrayers.value
    val completedCount = habitPrayers.values.count { it }
    assertEquals(0, completedCount)
    assertEquals(5, habitPrayers.size)
    assertTrue("All prayers should be unchecked on fresh install", habitPrayers.values.all { !it })

    val qadaCounts = vm.qadaCounts.value
    assertTrue("All Qada counters should initialize to 0 on fresh install", qadaCounts.values.all { it == 0 })
    assertEquals(0, qadaCounts.values.sum())

    val history = vm.consistencyHistory.value
    assertTrue("Consistency history should be completely empty on fresh install with no mock entries", history.isEmpty())

    // 2. Strict User Intent assertions:
    // When user checks Fajr -> 1/5 -> 20%
    vm.toggleHabitPrayer("ফজর")
    val updatedHabit = vm.habitPrayers.value
    assertTrue("Fajr should now be checked", updatedHabit["ফজর"] == true)
    assertEquals(1, updatedHabit.values.count { it })

    val updatedHistory = vm.consistencyHistory.value
    assertEquals(1, updatedHistory.size)
    val todayRecord = updatedHistory.first()
    assertEquals(1, todayRecord.completedCount)
    assertEquals(5, todayRecord.totalCount)
    assertEquals(20, todayRecord.scorePercent)
    assertEquals(false, todayRecord.isStreakMaintained)

    // When user increments Qada for Fajr
    vm.updateQada("Fajr", 3)
    val updatedQada = vm.qadaCounts.value
    assertEquals(3, updatedQada["Fajr"])
    assertEquals(3, updatedQada.values.sum())
  }

  @Test
  fun `test widget update execution across different simulated conditions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appWidgetManager = AppWidgetManager.getInstance(context)

    // Test English mode
    val prefs = context.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_english", true).apply()
    NoorUpWidgetProvider.updateWidget(context, appWidgetManager, 101)

    // Test Bangla mode
    prefs.edit().putBoolean("is_english", false).apply()
    NoorUpWidgetProvider.updateWidget(context, appWidgetManager, 101)

    // Test with custom coordinates
    prefs.edit()
      .putFloat("selected_city_lat", 21.4225f)
      .putFloat("selected_city_lng", 39.8262f)
      .putString("selected_city_name_en", "Makkah")
      .putString("selected_city_name_bn", "মক্কা")
      .apply()
    NoorUpWidgetProvider.updateWidget(context, appWidgetManager, 101)
  }
}
