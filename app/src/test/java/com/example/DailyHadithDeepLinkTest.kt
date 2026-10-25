package com.example

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.PrayerNotificationHelper
import com.example.ui.noorup.hadith.DailyHadithProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Notification tap must open the app on the Hadith segment and show that
 * exact daily hadith — never a blank screen or a different hadith.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyHadithDeepLinkTest {

  private fun vm(): NoorUpViewModel =
    NoorUpViewModel(ApplicationProvider.getApplicationContext<Application>())

  @Test
  fun `tap extras round-trip through the intent`() {
    val context: android.content.Context = ApplicationProvider.getApplicationContext()
    val intent = PrayerNotificationHelper.putDailyHadithExtras(
      Intent(context, MainActivity::class.java), 4
    )
    assertTrue(intent.getBooleanExtra(PrayerNotificationHelper.EXTRA_OPEN_DAILY_HADITH, false))
    assertEquals(4, intent.getIntExtra(PrayerNotificationHelper.EXTRA_DAILY_HADITH_ID, -1))
  }

  @Test
  fun `deep link opens Dua tab on Hadith segment with that hadith`() {
    val viewModel = vm()
    viewModel.handleDailyHadithDeepLink(3)
    assertEquals(2, viewModel.selectedTab.value)
    assertEquals(1, viewModel.duaSegment.value)
    val shown = viewModel.dailyHadithDialog.value
    assertNotNull(shown)
    assertEquals(3, shown!!.id)
    assertTrue(shown.arabicText.isNotBlank())
  }

  @Test
  fun `prayer notification opens Home regardless of the previously selected tab`() {
    val viewModel = vm()
    viewModel.selectTab(5)
    viewModel.handleHomeNotificationDeepLink()
    assertEquals(0, viewModel.selectedTab.value)
  }

  @Test
  fun `unknown id falls back to todays hadith instead of nothing`() {
    val viewModel = vm()
    viewModel.handleDailyHadithDeepLink(9999)
    assertEquals(2, viewModel.selectedTab.value)
    val shown = viewModel.dailyHadithDialog.value
    assertNotNull(shown)
    assertEquals(DailyHadithProvider.getTodayHadith().id, shown!!.id)
  }

  @Test
  fun `dismiss clears the dialog`() {
    val viewModel = vm()
    viewModel.handleDailyHadithDeepLink(1)
    assertNotNull(viewModel.dailyHadithDialog.value)
    viewModel.dismissDailyHadithDialog()
    assertNull(viewModel.dailyHadithDialog.value)
  }

  @Test
  fun `segment state stays within bounds`() {
    val viewModel = vm()
    viewModel.setDuaSegment(5)
    assertEquals(1, viewModel.duaSegment.value)
    viewModel.setDuaSegment(-3)
    assertEquals(0, viewModel.duaSegment.value)
  }
}
