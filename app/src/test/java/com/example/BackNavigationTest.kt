package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.TabHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android Back: tab history rules and the ViewModel's single Back entry point. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackNavigationTest {

    private fun app(): Application = ApplicationProvider.getApplicationContext()

    private fun freshVm(): NoorUpViewModel {
        app().getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        return NoorUpViewModel(app())
    }

    @Test
    fun `back retraces tabs then exits at Home`() {
        val h = TabHistory()
        h.onSelect(0, 1)
        h.onSelect(1, 2)
        assertEquals(1, h.pop(2))
        assertEquals(0, h.pop(1))
        assertNull(h.pop(0))
    }

    @Test
    fun `reselecting a visited tab pops to it instead of looping`() {
        val h = TabHistory()
        h.onSelect(0, 1)
        h.onSelect(1, 2)
        h.onSelect(2, 1) // Quran again: history is now [Home]
        assertEquals(0, h.pop(1))
        assertNull(h.pop(0))

        val loop = TabHistory()
        loop.onSelect(0, 3)
        loop.onSelect(3, 0) // back on Home via the bar
        assertTrue(loop.isEmpty)
        assertNull(loop.pop(0))
    }

    @Test
    fun `non-Home start with empty history returns to Home before exiting`() {
        val h = TabHistory()
        assertEquals(0, h.pop(3))
        assertNull(h.pop(0))
    }

    @Test
    fun `view model back goes to previous tab and only exits at Home root`() {
        val vm = freshVm()
        assertFalse(vm.canHandleSystemBack())
        assertFalse(vm.handleSystemBack())

        vm.selectTab(2)
        vm.selectTab(3)
        assertTrue(vm.canHandleSystemBack())
        assertTrue(vm.handleSystemBack())
        assertEquals(2, vm.selectedTab.value)
        assertTrue(vm.handleSystemBack())
        assertEquals(0, vm.selectedTab.value)
        assertFalse(vm.handleSystemBack())
    }

    @Test
    fun `restored non-Home tab goes Home on first back`() {
        app().getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE).edit().putInt("selected_tab", 4).commit()
        val vm = NoorUpViewModel(app())
        assertEquals(4, vm.selectedTab.value)
        assertTrue(vm.handleSystemBack())
        assertEquals(0, vm.selectedTab.value)
        assertFalse(vm.handleSystemBack())
    }

    @Test
    fun `nested handlers win over tab history and higher priority runs first`() {
        val vm = freshVm()
        vm.selectTab(1)
        val calls = mutableListOf<String>()
        val unregisterScreen = vm.backNavigator.register(10) { calls += "screen" }
        val unregisterOverlay = vm.backNavigator.register(100) { calls += "overlay" }

        assertTrue(vm.handleSystemBack())
        assertEquals(listOf("overlay"), calls)
        assertEquals(1, vm.selectedTab.value) // tab untouched while nested handlers exist

        unregisterOverlay()
        assertTrue(vm.handleSystemBack())
        assertEquals(listOf("overlay", "screen"), calls)

        unregisterScreen()
        assertTrue(vm.handleSystemBack()) // now falls through to tab history
        assertEquals(0, vm.selectedTab.value)
    }
}
