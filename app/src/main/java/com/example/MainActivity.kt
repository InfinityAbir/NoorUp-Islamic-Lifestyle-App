package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.noorup.*
import com.example.ui.noorup.audio.QuranAudioBar
import com.example.ui.noorup.search.GlobalSearchScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App-owned edge-to-edge: avoids the deprecated bar-color setters and
        // short-edges cutout mode used by androidx.activity.enableEdgeToEdge().
        enableNoorUpEdgeToEdge()

        // Initialize high-importance Notification Channels for prayer times and daily updates
        PrayerNotificationHelper.createNotificationChannels(this)
        // Widget + alarm scheduling does Solar math + WorkManager init — keep it
        // off the main thread to avoid startup ANR (especially on emulator).
        val appCtx = applicationContext
        Thread {
            try {
                NoorUpWidgetProvider.updateAllWidgets(appCtx)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpActivity", "updateAllWidgets failed", e)
            }
            try {
                WidgetUpdateScheduler.scheduleNextWidgetUpdate(appCtx)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpActivity", "scheduleNextWidgetUpdate failed", e)
            }
        }.start()

        setContent {
            val viewModel: NoorUpViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isEnglish by viewModel.isEnglish.collectAsState()
            val selectedTab by viewModel.selectedTab.collectAsState()
            val searchOpen by viewModel.searchOpen.collectAsState()
            val context = LocalContext.current

            // Android 13+ notification permission is owned by the Home-screen
            // prayer-alarm onboarding: one rationale, one system dialog, then
            // exact-alarm access. A second global banner here would prompt twice.

            // Sync notification schedule on first launch
            LaunchedEffect(Unit) {
                viewModel.schedulePrayerNotifications(context)
                // Notification tap deep link (e.g. daily hadith): open the
                // requested tab and show the requested content once.
                handleNotificationDeepLink(viewModel, this@MainActivity.intent)
            }

            NoorUpTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text(if (isEnglish) "Home" else "হোম", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Quran") },
                                label = { Text(if (isEnglish) "Quran" else "কুরআন", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Duas") },
                                label = { Text(if (isEnglish) "Duas" else "দুআ", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts & Tools") },
                                label = { Text(if (isEnglish) "Tools" else "সরঞ্জাম", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { viewModel.selectTab(4) },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Assistant") },
                                label = { Text(if (isEnglish) "Assistant" else "সহকারী", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 5,
                                onClick = { viewModel.selectTab(5) },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                                label = { Text(if (isEnglish) "Stats" else "পরিসংখ্যান", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(top = 4.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> HomeScreen(viewModel)
                                1 -> QuranScreen(viewModel)
                                2 -> DuasScreen(viewModel)
                                3 -> ToolsScreen(viewModel)
                                4 -> AiAssistantScreen(viewModel)
                                5 -> AnalyticsScreen(viewModel)
                            }
                            QuranAudioBar(
                                controller = viewModel.audio,
                                isEnglish = isEnglish,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                            )
                            if (searchOpen) GlobalSearchScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationDeepLink(viewModel: NoorUpViewModel, intent: android.content.Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "noorup" && data.host == "dhikr") {
            intent?.data = null // one-shot
            viewModel.handleFamilyLink(data)
            return
        }
        val widgetSurah = intent?.getIntExtra(DailyAyatWidgetProvider.EXTRA_OPEN_VERSE_SURAH, -1) ?: -1
        val widgetDua = intent?.getStringExtra(DailyAyatWidgetProvider.EXTRA_OPEN_DUA_ID)
        if (widgetSurah > 0) {
            // Daily Verse widget: jump to the exact ayah, once.
            val ayah = intent!!.getIntExtra(DailyAyatWidgetProvider.EXTRA_OPEN_VERSE_AYAH, 1)
            intent.removeExtra(DailyAyatWidgetProvider.EXTRA_OPEN_VERSE_SURAH)
            viewModel.openQuranFromSearch(widgetSurah, ayah)
        } else if (widgetDua != null) {
            intent!!.removeExtra(DailyAyatWidgetProvider.EXTRA_OPEN_DUA_ID)
            viewModel.openDuaById(widgetDua)
        } else if (intent?.getBooleanExtra(PrayerNotificationHelper.EXTRA_OPEN_DAILY_HADITH, false) == true) {
            val id = intent.getIntExtra(PrayerNotificationHelper.EXTRA_DAILY_HADITH_ID, -1)
            viewModel.handleDailyHadithDeepLink(id)
        } else if (intent?.getBooleanExtra(PrayerNotificationHelper.EXTRA_OPEN_HOME, false) == true) {
            viewModel.handleHomeNotificationDeepLink()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App was already alive when the notification was tapped.
        try {
            val vm = androidx.lifecycle.ViewModelProvider(this)[NoorUpViewModel::class.java]
            handleNotificationDeepLink(vm, intent)
        } catch (e: Exception) {
            android.util.Log.w("NoorUpActivity", "deep link handling failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        val appCtx = applicationContext
        // The Daily Verse widget can bookmark while the app is in the background.
        try {
            androidx.lifecycle.ViewModelProvider(this)[NoorUpViewModel::class.java].refreshBookmarksFromStorage()
        } catch (_: Exception) { }
        Thread {
            try {
                NoorUpWidgetProvider.updateAllWidgets(appCtx)
            } catch (e: Exception) {
                android.util.Log.e("NoorUpActivity", "onResume updateAllWidgets failed", e)
            }
            // Re-check notification state on return from Settings: if the user
            // re-enabled notifications while away, ensure alarms are armed.
            try {
                val prefs = appCtx.getSharedPreferences("noorup_prefs", MODE_PRIVATE)
                if (prefs.getBoolean("prayer_notifications_enabled", true)) {
                    PrayerNotificationScheduler.scheduleAllPrayerAlerts(appCtx)
                }
            } catch (_: Exception) { }
        }.start()
    }
}

