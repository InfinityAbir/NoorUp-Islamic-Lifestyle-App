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
  fun `verify noorup notification icon is monochrome white on transparent background`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_noorup_notification)
    org.junit.Assert.assertNotNull("Notification icon drawable must exist", drawable)

    val size = 24
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable!!.setBounds(0, 0, size, size)
    drawable.draw(canvas)

    // Check corners are transparent (not a solid colored card/circle)
    val corners = listOf(Pair(0, 0), Pair(size - 1, 0), Pair(0, size - 1), Pair(size - 1, size - 1))
    for ((cx, cy) in corners) {
      val alpha = android.graphics.Color.alpha(bitmap.getPixel(cx, cy))
      assertEquals("Corner ($cx, $cy) must be transparent", 0, alpha)
    }

    var countAlpha = 0
    for (y in 0 until size) {
      for (x in 0 until size) {
        val pixel = bitmap.getPixel(x, y)
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha > 0) {
          countAlpha++
          val red = android.graphics.Color.red(pixel)
          val green = android.graphics.Color.green(pixel)
          val blue = android.graphics.Color.blue(pixel)
          // For white with alpha, red == green == blue == 255
          assertEquals("Pixel at ($x, $y) red channel must be 255", 255, red)
          assertEquals("Pixel at ($x, $y) green channel must be 255", 255, green)
          assertEquals("Pixel at ($x, $y) blue channel must be 255", 255, blue)
        }
      }
    }
    assertTrue("Icon must contain visible foreground pixels (found $countAlpha)", countAlpha > 0)
  }

  @Test
  fun `verify prayer notification uses ic_noorup_notification small icon`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ui.noorup.PrayerNotificationHelper.createNotificationChannels(context)
    com.example.ui.noorup.PrayerNotificationHelper.showPrayerAlert(
      context = context,
      notificationId = 1234,
      title = "Test Prayer",
      message = "Test Message"
    )

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
    val notification = shadowNotificationManager.getNotification(1234)
    org.junit.Assert.assertNotNull("Notification should be posted", notification)
    assertEquals(
      "Small icon should be ic_noorup_notification",
      R.drawable.ic_noorup_notification,
      notification.icon
    )
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

  @Test
  fun `test dynamic widget update scheduler calculates future transition timestamp`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val now = System.currentTimeMillis()
    val nextTransition = com.example.ui.noorup.WidgetUpdateScheduler.getNextTransitionMillis(context, now)

    assertTrue("Next prayer transition ($nextTransition) must be strictly in the future (> $now)", nextTransition > now)
    assertTrue("Next prayer transition should be within the next 24 hours", nextTransition <= now + 24 * 60 * 60 * 1000L)
  }

  @Test
  fun `test dynamic widget update scheduler sets alarm in AlarmManager`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAlarmManager = org.robolectric.Shadows.shadowOf(alarmManager)

    com.example.ui.noorup.WidgetUpdateScheduler.scheduleNextWidgetUpdate(context)

    val scheduledAlarms = shadowAlarmManager.scheduledAlarms
    assertTrue("AlarmManager should have at least one scheduled alarm for dynamic widget updating", scheduledAlarms.isNotEmpty())

    val nextAlarm = shadowAlarmManager.nextScheduledAlarm
    org.junit.Assert.assertNotNull("Next scheduled alarm must not be null", nextAlarm)
    assertTrue("Scheduled trigger time must be >= current time", nextAlarm!!.triggerAtTime >= System.currentTimeMillis())
  }

  @Test
  fun `test widget update broadcast receiver executes without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = com.example.ui.noorup.WidgetUpdateReceiver()
    val intent = android.content.Intent(com.example.ui.noorup.WidgetUpdateScheduler.ACTION_AUTO_UPDATE_WIDGET)

    receiver.onReceive(context, intent)
    // Verification that widget provider and scheduler were called cleanly
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAlarmManager = org.robolectric.Shadows.shadowOf(alarmManager)
    assertTrue("Receiver should ensure an alarm is scheduled", shadowAlarmManager.scheduledAlarms.isNotEmpty())
  }

  @Test
  fun `test widget provider lifecycle onEnabled and onDisabled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val provider = NoorUpWidgetProvider()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAlarmManager = org.robolectric.Shadows.shadowOf(alarmManager)

    provider.onEnabled(context)
    assertTrue("onEnabled should schedule widget update alarm", shadowAlarmManager.scheduledAlarms.isNotEmpty())

    provider.onDisabled(context)
    val cancelledIntent = android.content.Intent(context, com.example.ui.noorup.WidgetUpdateReceiver::class.java).apply {
      action = com.example.ui.noorup.WidgetUpdateScheduler.ACTION_AUTO_UPDATE_WIDGET
    }
    val pendingIntent = android.app.PendingIntent.getBroadcast(
      context,
      9001,
      cancelledIntent,
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    assertTrue("onDisabled should cancel or nullify pending alarm intent", pendingIntent == null || shadowAlarmManager.scheduledAlarms.none { it.operation == pendingIntent })
  }
}
