package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QadaTrackingTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun vm(): NoorUpViewModel = NoorUpViewModel(app)

    @Test
    fun `initial qada state is zero on fresh install`() {
        val viewModel = vm()
        val counts = viewModel.qadaCounts.value
        assertEquals("Fajr initial count should be 0", 0, counts["Fajr"])
        assertEquals("Isha initial count should be 0", 0, counts["Isha"])
        assertEquals("Witr initial count should be 0", 0, counts["Witr"])
        assertEquals("Total pending initial count should be 0", 0, counts.values.sum())
        assertEquals("Total fulfilled initial count should be 0", 0, viewModel.totalMadeUpQada.value)
    }

    @Test
    fun `minus button records additional missed prayer and increases pending count`() {
        val viewModel = vm()
        viewModel.recordMissedQada("Fajr")
        assertEquals(1, viewModel.qadaCounts.value["Fajr"])
        assertEquals(1, viewModel.qadaCounts.value.values.sum())
        assertEquals(0, viewModel.totalMadeUpQada.value)

        viewModel.recordMissedQada("Fajr")
        assertEquals(2, viewModel.qadaCounts.value["Fajr"])
        assertEquals(2, viewModel.qadaCounts.value.values.sum())
    }

    @Test
    fun `plus button fulfills prayer, decreases pending count, and increases fulfilled count`() {
        val viewModel = vm()
        // Record 2 missed Fajr prayers
        viewModel.recordMissedQada("Fajr")
        viewModel.recordMissedQada("Fajr")
        assertEquals(2, viewModel.qadaCounts.value["Fajr"])
        assertEquals(0, viewModel.totalMadeUpQada.value)

        // Fulfill 1 Fajr prayer (plus button)
        viewModel.fulfillQada("Fajr")
        assertEquals(1, viewModel.qadaCounts.value["Fajr"])
        assertEquals(1, viewModel.totalMadeUpQada.value)

        // Fulfill 2nd Fajr prayer (plus button)
        viewModel.fulfillQada("Fajr")
        assertEquals(0, viewModel.qadaCounts.value["Fajr"])
        assertEquals(2, viewModel.totalMadeUpQada.value)
    }

    @Test
    fun `plus button at zero pending does not create negative count or false fulfillment`() {
        val viewModel = vm()
        assertEquals(0, viewModel.qadaCounts.value["Fajr"] ?: 0)
        assertEquals(0, viewModel.totalMadeUpQada.value)

        // Pressing plus when pending is 0
        viewModel.fulfillQada("Fajr")
        assertEquals(0, viewModel.qadaCounts.value["Fajr"])
        assertEquals(0, viewModel.totalMadeUpQada.value)
    }

    @Test
    fun `reversing fulfillment with minus button adjusts fulfilled and pending counts`() {
        val viewModel = vm()
        viewModel.recordMissedQada("Dhuhr") // pending = 1
        viewModel.fulfillQada("Dhuhr")       // pending = 0, fulfilled = 1

        assertEquals(0, viewModel.qadaCounts.value["Dhuhr"])
        assertEquals(1, viewModel.totalMadeUpQada.value)

        // User presses minus button to reverse the fulfillment
        viewModel.recordMissedQada("Dhuhr")  // pending = 1, fulfilled = 0
        assertEquals(1, viewModel.qadaCounts.value["Dhuhr"])
        assertEquals(0, viewModel.totalMadeUpQada.value)
    }

    @Test
    fun `isha and witr remain completely independent`() {
        val viewModel = vm()
        viewModel.recordMissedQada("Isha")
        viewModel.recordMissedQada("Witr")

        assertEquals(1, viewModel.qadaCounts.value["Isha"])
        assertEquals(1, viewModel.qadaCounts.value["Witr"])
        assertEquals(0, viewModel.totalMadeUpQada.value)

        // Fulfill Isha only
        viewModel.fulfillQada("Isha")

        // Isha count is 0, Witr count remains 1
        assertEquals("Isha pending should be 0", 0, viewModel.qadaCounts.value["Isha"])
        assertEquals("Witr pending should remain 1", 1, viewModel.qadaCounts.value["Witr"])
        assertEquals("Fulfilled count should be 1", 1, viewModel.totalMadeUpQada.value)

        // Fulfill Witr only
        viewModel.fulfillQada("Witr")
        assertEquals("Witr pending should be 0", 0, viewModel.qadaCounts.value["Witr"])
        assertEquals("Fulfilled count should be 2", 2, viewModel.totalMadeUpQada.value)
    }

    @Test
    fun `qada counts and fulfilled total survive app restart via persistence`() {
        val viewModel1 = vm()
        viewModel1.recordMissedQada("Asr")
        viewModel1.recordMissedQada("Asr")
        viewModel1.fulfillQada("Asr")
        assertEquals(1, viewModel1.qadaCounts.value["Asr"])
        assertEquals(1, viewModel1.totalMadeUpQada.value)

        // Create new ViewModel instance to simulate app restart
        val viewModel2 = vm()
        assertEquals(1, viewModel2.qadaCounts.value["Asr"])
        assertEquals(1, viewModel2.totalMadeUpQada.value)
    }
}
