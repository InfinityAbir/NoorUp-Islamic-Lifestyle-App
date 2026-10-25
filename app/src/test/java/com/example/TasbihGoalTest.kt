package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Custom tasbih goals: any number 1..MAX, persisted, count preserved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TasbihGoalTest {

  private fun vm(): NoorUpViewModel =
    NoorUpViewModel(ApplicationProvider.getApplicationContext<Application>())

  @Test
  fun `default goal is 33`() {
    assertEquals(33, vm().tasbihGoal.value)
  }

  @Test
  fun `custom goal applies without resetting the count`() {
    val viewModel = vm()
    repeat(7) { viewModel.incrementTasbih() }
    viewModel.setTasbihGoal(100)
    assertEquals(100, viewModel.tasbihGoal.value)
    assertEquals("Count must survive a goal change", 7, viewModel.tasbihCount.value)
  }

  @Test
  fun `goal survives a fresh ViewModel via prefs`() {
    vm().setTasbihGoal(500)
    assertEquals(500, vm().tasbihGoal.value)
  }

  @Test
  fun `goal is clamped to 1 to MAX`() {
    val viewModel = vm()
    viewModel.setTasbihGoal(0)
    assertEquals(1, viewModel.tasbihGoal.value)
    viewModel.setTasbihGoal(-40)
    assertEquals(1, viewModel.tasbihGoal.value)
    viewModel.setTasbihGoal(Int.MAX_VALUE)
    assertEquals(NoorUpViewModel.MAX_TASBIH_GOAL, viewModel.tasbihGoal.value)
  }

  @Test
  fun `selecting a dhikr preset still resets count with its goal`() {
    val viewModel = vm()
    repeat(5) { viewModel.incrementTasbih() }
    viewModel.setDhikr("Alhamdulillah", 33)
    assertEquals(33, viewModel.tasbihGoal.value)
    assertEquals(0, viewModel.tasbihCount.value)
  }
}
