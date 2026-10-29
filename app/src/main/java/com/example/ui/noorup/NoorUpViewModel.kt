package com.example.ui.noorup

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.NoorUpWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NoorUpViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

    /** Quran recitation player (Media3 session in [com.example.ui.noorup.audio.QuranPlaybackService]). */
    val audio = com.example.ui.noorup.audio.QuranAudioController(application, viewModelScope)

    /** Encrypted Google Drive (appDataFolder) backup and restore. */
    val backup = com.example.ui.noorup.backup.DriveBackupManager(application)

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    // --- Tools sub-tab order (user can drag tabs; saved) ---
    private val _toolsOrder = MutableStateFlow(ToolTabOrder.decode(prefs.getString("tools_tab_order", null)))
    val toolsOrder: StateFlow<List<Int>> = _toolsOrder.asStateFlow()

    fun setToolsOrder(order: List<Int>) {
        val clean = ToolTabOrder.decode(ToolTabOrder.encode(order))
        _toolsOrder.value = clean
        prefs.edit().putString("tools_tab_order", ToolTabOrder.encode(clean)).apply()
    }

    fun resetToolsOrder() {
        _toolsOrder.value = ToolTabOrder.DEFAULT
        prefs.edit().remove("tools_tab_order").apply()
    }

    // --- Universal search (top-bar overlay) + jump targets ---
    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    val searchableDuas: List<DuaItem> get() = NoorUpRepository.duas

    /** Set by a search result; the Duas screen applies it to its filter once, then clears it. */
    private val _pendingDuaQuery = MutableStateFlow<String?>(null)
    val pendingDuaQuery: StateFlow<String?> = _pendingDuaQuery.asStateFlow()

    fun consumePendingDuaQuery() { _pendingDuaQuery.value = null }

    // --- Family Dhikr & shared goals (serverless: links carry the goal and progress) ---
    private val familyStore = com.example.ui.noorup.family.FamilyStore(prefs)
    private val _familyGoals = MutableStateFlow(familyStore.goals())
    val familyGoals: StateFlow<List<com.example.ui.noorup.family.FamilyGoal>> = _familyGoals.asStateFlow()
    private val _familyMyName = MutableStateFlow(familyStore.myName())
    val familyMyName: StateFlow<String> = _familyMyName.asStateFlow()
    private val _familyCheckIns = MutableStateFlow(familyStore.checkIns())
    val familyCheckIns: StateFlow<Map<String, Map<String, Int>>> = _familyCheckIns.asStateFlow()
    private val _pendingFamilyJoin = MutableStateFlow<com.example.ui.noorup.family.FamilyLinkPayload?>(null)
    val pendingFamilyJoin: StateFlow<com.example.ui.noorup.family.FamilyLinkPayload?> = _pendingFamilyJoin.asStateFlow()
    private val _familyMessage = MutableStateFlow<String?>(null)
    val familyMessage: StateFlow<String?> = _familyMessage.asStateFlow()

    fun clearFamilyMessage() { _familyMessage.value = null }
    fun reportFamilyMessage(text: String) { _familyMessage.value = text }
    fun familyTodayKey(): String = getTodayDateKey()

    fun setFamilyMyName(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val old = _familyMyName.value
        familyStore.setMyName(clean)
        _familyMyName.value = clean
        if (old.isNotBlank() && old != clean) {
            // Carry this person's existing counts and check-ins over to the new name.
            saveFamilyGoals(_familyGoals.value.map { g ->
                val c = g.contributions[old] ?: return@map g
                g.copy(contributions = g.contributions - old + (clean to maxOf(c, g.contributions[clean] ?: 0)))
            })
            val ci = _familyCheckIns.value.mapValues { (_, m) ->
                val c = m[old] ?: return@mapValues m
                m - old + (clean to c)
            }
            _familyCheckIns.value = ci; familyStore.saveCheckIns(ci)
        }
        familyRecordMyCheckIn()
    }

    private fun saveFamilyGoals(goals: List<com.example.ui.noorup.family.FamilyGoal>) {
        _familyGoals.value = goals
        familyStore.saveGoals(goals)
    }

    fun createFamilyGoal(title: String, target: Int) {
        val me = _familyMyName.value
        if (me.isBlank() || target <= 0) return
        val goal = com.example.ui.noorup.family.FamilyGoal(
            id = com.example.ui.noorup.family.FamilyDhikrLogic.newId(), title = title,
            target = target.coerceAtMost(100_000_000), createdBy = me
        )
        saveFamilyGoals(_familyGoals.value + goal)
    }

    fun addFamilyCount(goalId: String, delta: Int) {
        val me = _familyMyName.value
        if (me.isBlank() || delta <= 0) return
        saveFamilyGoals(_familyGoals.value.map { g ->
            if (g.id != goalId) g
            else g.copy(contributions = g.contributions + (me to ((g.contributions[me] ?: 0) + delta).coerceAtMost(100_000_000)))
        })
    }

    /** Rename a goal or change its target. Family members receive the change with the next link you share. */
    fun editFamilyGoal(goalId: String, title: String, target: Int) {
        val clean = title.trim().take(60)
        if (clean.isBlank() || target <= 0) return
        saveFamilyGoals(_familyGoals.value.map { g ->
            if (g.id != goalId) g else g.copy(title = clean, target = target.coerceAtMost(100_000_000), rev = g.rev + 1)
        })
    }

    fun deleteFamilyGoal(goalId: String) = saveFamilyGoals(_familyGoals.value.filterNot { it.id == goalId })

    fun handleFamilyText(text: String) {
        val payload = com.example.ui.noorup.family.FamilyLink.parseText(text)
        if (payload == null) {
            _familyMessage.value = if (_isEnglish.value) "That link isn't a NoorUp family link." else "এটি নূরআপের পারিবারিক লিংক নয়।"
            return
        }
        applyFamilyPayload(payload)
    }

    /** Deep link (noorup://dhikr?...) delivered to MainActivity. */
    fun handleFamilyLink(uri: android.net.Uri?) {
        selectTab(3)
        val payload = com.example.ui.noorup.family.FamilyLink.parse(uri)
        if (payload == null) {
            _familyMessage.value = if (_isEnglish.value) "That link isn't a NoorUp family link." else "এটি নূরআপের পারিবারিক লিংক নয়।"
            return
        }
        applyFamilyPayload(payload)
    }

    private fun applyFamilyPayload(p: com.example.ui.noorup.family.FamilyLinkPayload) {
        val known = _familyGoals.value.any { it.id == p.goalId }
        if (p.member == null && !known) {
            _pendingFamilyJoin.value = p // plain invite: ask first (the dialog lives in the Family tab)
            _requestedToolTab.value = 8
            return
        }
        if (p.member != null) {
            saveFamilyGoals(com.example.ui.noorup.family.FamilyDhikrLogic.merge(_familyGoals.value, p))
            if (p.prayers != null && p.dateKey != null) recordCheckIn(p.dateKey, p.member, p.prayers)
            _familyMessage.value = if (_isEnglish.value)
                "${p.member}'s progress was added to \"${p.title}\"." else "${p.member}-এর প্রগ্রেস \"${p.title}\"-এ যোগ হয়েছে।"
        } else {
            _familyMessage.value = if (_isEnglish.value) "You already have \"${p.title}\"." else "\"${p.title}\" আপনার কাছে আগে থেকেই আছে।"
        }
        // Tools tab 8 is Family Dhikr (see ToolsScreen).
        _requestedToolTab.value = 8
    }

    fun acceptFamilyJoin() {
        val p = _pendingFamilyJoin.value ?: return
        saveFamilyGoals(com.example.ui.noorup.family.FamilyDhikrLogic.merge(_familyGoals.value, p))
        _pendingFamilyJoin.value = null
        _requestedToolTab.value = 8
    }

    fun dismissFamilyJoin() { _pendingFamilyJoin.value = null }

    /** One-shot request for ToolsScreen to switch to a specific sub-tab (e.g. Family Dhikr from a deep link). */
    private val _requestedToolTab = MutableStateFlow<Int?>(null)
    val requestedToolTab: StateFlow<Int?> = _requestedToolTab.asStateFlow()
    fun consumeRequestedToolTab() { _requestedToolTab.value = null }

    private fun recordCheckIn(dateKey: String, member: String, prayers: Int) {
        val next = _familyCheckIns.value.toMutableMap()
        next[dateKey] = (next[dateKey] ?: emptyMap()) + (member to maxOf(prayers, next[dateKey]?.get(member) ?: 0))
        _familyCheckIns.value = next
        familyStore.saveCheckIns(next)
    }

    /** Records my own prayer count (from Noor Garden) for today. */
    fun familyRecordMyCheckIn() {
        val me = _familyMyName.value
        if (me.isBlank()) return
        val count = _habitPrayers.value.values.count { it }
        val today = getTodayDateKey()
        val next = _familyCheckIns.value.toMutableMap()
        // My own count may go down if I untick a prayer, so overwrite rather than max().
        next[today] = (next[today] ?: emptyMap()) + (me to count)
        _familyCheckIns.value = next
        familyStore.saveCheckIns(next)
    }

    fun familyStreak(): Int = com.example.ui.noorup.family.FamilyDhikrLogic.familyStreak(
        _familyCheckIns.value, getTodayDateKey()
    ) { key ->
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply { time = fmt.parse(key) ?: Date() }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        fmt.format(cal.time)
    }

    fun familyProgressShareText(goal: com.example.ui.noorup.family.FamilyGoal, isEng: Boolean): String {
        val me = _familyMyName.value
        val mine = goal.contributions[me] ?: 0
        val prayers = _habitPrayers.value.values.count { it }
        val link = com.example.ui.noorup.family.FamilyLink.build(goal, me, mine, prayers, getTodayDateKey())
        return if (isEng) "$me's NoorUp progress on \"${goal.title}\": $mine counted today toward ${goal.target}. Open to add it to the family total:\n$link${familyShareHint(true)}"
        else "$me-এর নূরআপ প্রগ্রেস \"${goal.title}\": মোট ${mine.toBanglaDigits()} গোনা হয়েছে, লক্ষ্য ${goal.target.toBanglaDigits()}। পরিবারের মোটে যোগ করতে খুলুন:\n$link${familyShareHint(false)}"
    }

    /** Chat apps often don't make noorup:// links tappable, so every share explains the paste route. */
    private fun familyShareHint(isEng: Boolean): String =
        if (isEng) "\n\nCan't tap the link? Copy this whole message, then open NoorUp > Tools > Family Dhikr > Paste."
        else "\n\nলিংকে ট্যাপ না হলে পুরো বার্তাটি কপি করে নূরআপ > সরঞ্জাম > পারিবারিক যিকর > পেস্ট করুন।"

    fun familyInviteShareText(goal: com.example.ui.noorup.family.FamilyGoal, isEng: Boolean): String {
        val me = _familyMyName.value
        val link = com.example.ui.noorup.family.FamilyLink.build(goal)
        return if (isEng) "$me invites you to a family dhikr goal on NoorUp: \"${goal.title}\" — target ${goal.target}.\n$link${familyShareHint(true)}"
        else "$me আপনাকে নূরআপে পারিবারিক যিকর লক্ষ্যে আমন্ত্রণ জানিয়েছে: \"${goal.title}\" — লক্ষ্য ${goal.target.toBanglaDigits()}।\n$link${familyShareHint(false)}"
    }

    fun familyCheckInShareText(goal: com.example.ui.noorup.family.FamilyGoal, isEng: Boolean): String {
        val me = _familyMyName.value
        val prayers = _habitPrayers.value.values.count { it }
        val link = com.example.ui.noorup.family.FamilyLink.build(goal, me, goal.contributions[me] ?: 0, prayers, getTodayDateKey())
        return if (isEng) "$me prayed $prayers/5 today. Keep our family streak going!\n$link${familyShareHint(true)}"
        else "$me আজ ${prayers.toBanglaDigits()}/৫ ওয়াক্ত পড়েছে। আমাদের পারিবারিক ধারাবাহিকতা ধরে রাখি!\n$link${familyShareHint(false)}"
    }

    // --- Dua bookmarks (also written by the Daily Verse widget) ---
    private fun loadBookmarkedDuas(): Set<String> =
        prefs.all.filter { (k, v) -> k.startsWith("dua_bm_") && v == true }.keys.map { it.removePrefix("dua_bm_") }.toSet()

    private val _bookmarkedDuas = MutableStateFlow(loadBookmarkedDuas())
    val bookmarkedDuas: StateFlow<Set<String>> = _bookmarkedDuas.asStateFlow()

    fun toggleDuaBookmark(id: String) {
        val now = id !in _bookmarkedDuas.value
        prefs.edit().putBoolean("dua_bm_$id", now).apply()
        _bookmarkedDuas.value = if (now) _bookmarkedDuas.value + id else _bookmarkedDuas.value - id
    }

    /** Re-read bookmarks that another component (the widget) may have changed. */
    fun refreshBookmarksFromStorage() {
        _bookmarkedDuas.value = loadBookmarkedDuas()
        refreshBookmarkedVerses()
    }

    fun openDuaById(id: String) {
        NoorUpRepository.duas.firstOrNull { it.id == id }?.let { openDuaFromSearch(it, _isEnglish.value) }
    }

    fun openGlobalSearch() { _searchOpen.value = true }
    fun closeGlobalSearch() { _searchOpen.value = false }

    fun openQuranFromSearch(surah: Int, ayah: Int) {
        closeGlobalSearch()
        selectTab(1)
        openSurahAt(surah, ayah)
    }

    fun openDuaFromSearch(dua: DuaItem, isEng: Boolean) {
        closeGlobalSearch()
        _pendingDuaQuery.value = if (isEng) dua.titleEnglish else dua.titleBangla
        setDuaSegment(0)
        selectTab(2)
    }

    fun openHadithLibraryFromSearch(bookId: String, number: Int) {
        closeGlobalSearch()
        setDuaSegment(1)
        selectTab(2)
        selectHadithBook(bookId)
        setHadithSearchQuery(number.toString())
    }

    fun openAssistantFromSearch(query: String, isEng: Boolean) {
        closeGlobalSearch()
        selectTab(4)
        sendAiMessage(query, isEng)
    }

    /** Built-in assistant answer for [query], or null when the offline engine has no specific topic for it. */
    fun offlineKnowledgeAnswer(query: String, isEng: Boolean): String? =
        generateLocalIslamicResponse(query, isEng).takeIf { it != genericLocalReply(isEng) }

    private val _selectedTab = MutableStateFlow(prefs.getInt("selected_tab", 0).coerceIn(0, 5))
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab.coerceIn(0, 5)
        prefs.edit().putInt("selected_tab", tab.coerceIn(0, 5)).apply()
    }

    // --- Dua-screen segment (0: Duas, 1: Hadith Library) + daily-hadith deep link ---
    private val _duaSegment = MutableStateFlow(0)
    val duaSegment: StateFlow<Int> = _duaSegment.asStateFlow()

    fun setDuaSegment(segment: Int) {
        _duaSegment.value = segment.coerceIn(0, 1)
    }

    private val _dailyHadithDialog =
        MutableStateFlow<com.example.ui.noorup.hadith.DailyFeaturedHadith?>(null)
    val dailyHadithDialog: StateFlow<com.example.ui.noorup.hadith.DailyFeaturedHadith?> =
        _dailyHadithDialog.asStateFlow()

    fun openDailyHadithDialog(id: Int) {
        val all = com.example.ui.noorup.hadith.DailyHadithProvider.getAllHadiths()
        _dailyHadithDialog.value =
            all.find { it.id == id } ?: com.example.ui.noorup.hadith.DailyHadithProvider.getTodayHadith()
    }

    fun dismissDailyHadithDialog() {
        _dailyHadithDialog.value = null
    }

    /** Notification tap: open the Dua tab on the Hadith segment and show that hadith. */
    fun handleDailyHadithDeepLink(hadithId: Int) {
        selectTab(2)
        setDuaSegment(1)
        openDailyHadithDialog(hadithId)
    }

    /** Prayer/Garden notification taps always return the user to the Home dashboard. */
    fun handleHomeNotificationDeepLink() {
        selectTab(0)
    }

    // --- Language State (Bangla / English) ---
    private val _isEnglish = MutableStateFlow(prefs.getBoolean("is_english", false))
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    // --- Quran Bengali pronunciation preference (shared by reader + Focus mode) ---
    private val _quranShowTransliteration =
        MutableStateFlow(prefs.getBoolean("quran_show_transliteration", true))
    val quranShowTransliteration: StateFlow<Boolean> = _quranShowTransliteration.asStateFlow()

    fun toggleQuranTransliteration() {
        _quranShowTransliteration.update { !it }
        prefs.edit()
            .putBoolean("quran_show_transliteration", _quranShowTransliteration.value)
            .apply()
    }

    fun toggleLanguage() {
        _isEnglish.update { !it }
        prefs.edit().putBoolean("is_english", _isEnglish.value).apply()
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.Default) {
            try {
                NoorUpWidgetProvider.updateAllWidgets(appContext)
                com.example.DailyAyatWidgetProvider.updateAll(appContext)
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "toggleLanguage widget update failed", e)
            }
        }
        // Re-arm prayer alarms so already-scheduled notifications fire in the new
        // language instead of the stale baked-in one (self-heals at next fire anyway).
        schedulePrayerNotifications(appContext)
        refreshConsistencyHistory()
    }

    // --- Theme State (Dark / Light) ---
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.update { !it }
        prefs.edit().putBoolean("is_dark_mode", _isDarkMode.value).apply()
    }

    private val initialMethod = CalculationMethod.entries.find { it.name == prefs.getString("calculation_method", CalculationMethod.KARACHI.name) } ?: CalculationMethod.KARACHI
    private val _calculationMethod = MutableStateFlow(initialMethod)
    val calculationMethod: StateFlow<CalculationMethod> = _calculationMethod.asStateFlow()

    private val initialJuristic = JuristicMethod.entries.find { it.name == prefs.getString("juristic_method", JuristicMethod.HANAFI.name) } ?: JuristicMethod.HANAFI
    private val _juristicMethod = MutableStateFlow(initialJuristic)
    val juristicMethod: StateFlow<JuristicMethod> = _juristicMethod.asStateFlow()

    private val initialLat = prefs.getString("selected_city_lat_dbl", null)?.toDoubleOrNull()
        ?: prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
    private val initialLng = prefs.getString("selected_city_lng_dbl", null)?.toDoubleOrNull()
        ?: prefs.getFloat("selected_city_lng", 90.3795f).toDouble()
    private val persistedZone = prefs.getString("selected_city_zone_id", "") ?: ""
    private val initialTzOffset = try {
        val tz = if (persistedZone.isBlank()) java.util.TimeZone.getDefault()
        else java.util.TimeZone.getTimeZone(persistedZone)
        val now = System.currentTimeMillis()
        tz.getOffset(now) / 3600000.0
    } catch (_: Exception) {
        Calendar.getInstance().let { it.timeZone.getOffset(it.timeInMillis) / 3600000.0 }
    }

    private val _calculatedPrayerTimes = MutableStateFlow(
        SolarPrayerEngine.calculateTimes(
            latitude = initialLat,
            longitude = initialLng,
            calendar = Calendar.getInstance(),
            timezoneOffset = initialTzOffset,
            method = initialMethod,
            juristic = initialJuristic
        )
    )
    val calculatedPrayerTimes: StateFlow<CalculatedPrayerTimes> = _calculatedPrayerTimes.asStateFlow()

    // --- Location / GPS State ---
    private val _selectedCityLocation = MutableStateFlow(
        CityLocation(
            nameBn = prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা",
            nameEn = prefs.getString("selected_city_name_en", "Uttara, Dhaka") ?: "Uttara, Dhaka",
            latitude = initialLat,
            longitude = initialLng,
            zoneId = prefs.getString("selected_city_zone_id", "") ?: "",
            fajrStart = _calculatedPrayerTimes.value.fajrStart,
            fajrEnd = _calculatedPrayerTimes.value.fajrEnd,
            makruhSunriseStart = _calculatedPrayerTimes.value.makruhSunriseStart,
            makruhSunriseEnd = _calculatedPrayerTimes.value.makruhSunriseEnd,
            dhuhrStart = _calculatedPrayerTimes.value.dhuhrStart,
            dhuhrEnd = _calculatedPrayerTimes.value.dhuhrEnd,
            makruhZawalStart = _calculatedPrayerTimes.value.makruhZawalStart,
            makruhZawalEnd = _calculatedPrayerTimes.value.makruhZawalEnd,
            asrStart = _calculatedPrayerTimes.value.asrStart,
            asrEnd = _calculatedPrayerTimes.value.asrEnd,
            makruhSunsetStart = _calculatedPrayerTimes.value.makruhSunsetStart,
            makruhSunsetEnd = _calculatedPrayerTimes.value.makruhSunsetEnd,
            maghribStart = _calculatedPrayerTimes.value.maghribStart,
            maghribEnd = _calculatedPrayerTimes.value.maghribEnd,
            ishaStart = _calculatedPrayerTimes.value.ishaStart,
            ishaEnd = _calculatedPrayerTimes.value.ishaEnd
        )
    )
    val selectedCityLocation: StateFlow<CityLocation> = _selectedCityLocation.asStateFlow()

    /** UTC offset in hours for [zoneId] at [atMillis], honoring DST. Blank = device zone. */
    private fun tzOffsetFor(zoneId: String, atMillis: Long = System.currentTimeMillis()): Double {
        return try {
            val tz = if (zoneId.isBlank()) java.util.TimeZone.getDefault()
            else java.util.TimeZone.getTimeZone(zoneId)
            tz.getOffset(atMillis) / 3600000.0
        } catch (_: Exception) {
            java.util.TimeZone.getDefault().getOffset(atMillis) / 3600000.0
        }
    }

    private fun buildPresetCity(nameBn: String, nameEn: String, lat: Double, lng: Double, zoneId: String): CityLocation {
        val calc = SolarPrayerEngine.calculateTimes(
            latitude = lat,
            longitude = lng,
            calendar = Calendar.getInstance(),
            timezoneOffset = tzOffsetFor(zoneId),
            method = _calculationMethod.value,
            juristic = _juristicMethod.value
        )
        return CityLocation(
            nameBn = nameBn,
            nameEn = nameEn,
            latitude = lat,
            longitude = lng,
            zoneId = zoneId,
            fajrStart = calc.fajrStart,
            fajrEnd = calc.fajrEnd,
            makruhSunriseStart = calc.makruhSunriseStart,
            makruhSunriseEnd = calc.makruhSunriseEnd,
            dhuhrStart = calc.dhuhrStart,
            dhuhrEnd = calc.dhuhrEnd,
            makruhZawalStart = calc.makruhZawalStart,
            makruhZawalEnd = calc.makruhZawalEnd,
            asrStart = calc.asrStart,
            asrEnd = calc.asrEnd,
            makruhSunsetStart = calc.makruhSunsetStart,
            makruhSunsetEnd = calc.makruhSunsetEnd,
            maghribStart = calc.maghribStart,
            maghribEnd = calc.maghribEnd,
            ishaStart = calc.ishaStart,
            ishaEnd = calc.ishaEnd
        )
    }

    // Back-compat getter: was a stale once-computed val (method change never
    // recomputed it). Now delegates to the refreshed flow.
    val availableCities: List<CityLocation>
        get() = _availableCities.value.ifEmpty {
            // Pre-refresh fallback so first composition never sees an empty picker.
            listOf(
                buildPresetCity("ঢাকা", "Dhaka", 23.8103, 90.4125, "Asia/Dhaka"),
                buildPresetCity("চট্টগ্রাম", "Chattogram", 22.3569, 91.7832, "Asia/Dhaka"),
                buildPresetCity("সিলেট", "Sylhet", 24.8949, 91.8687, "Asia/Dhaka"),
                buildPresetCity("রাজশাহী", "Rajshahi", 24.3745, 88.6042, "Asia/Dhaka"),
                buildPresetCity("খুলনা", "Khulna", 22.8456, 89.5403, "Asia/Dhaka"),
                buildPresetCity("বরিশাল", "Barishal", 22.7010, 90.3535, "Asia/Dhaka"),
                buildPresetCity("রংপুর", "Rangpur", 25.7439, 89.2752, "Asia/Dhaka"),
                buildPresetCity("ময়মনসিংহ", "Mymensingh", 24.7471, 90.4203, "Asia/Dhaka"),
                buildPresetCity("মক্কা মুকাররমা", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"),
                buildPresetCity("মদিনা মুনাওয়ারা", "Madinah", 24.4672, 39.6111, "Asia/Riyadh"),
                buildPresetCity("লন্ডন", "London", 51.5074, -0.1278, "Europe/London"),
                buildPresetCity("নিউ ইয়র্ক", "New York", 40.7128, -74.0060, "America/New_York")
            )
        }

    // --- Dynamic Solar & Astronomical Prayer Engine State ---
    private val _selectedCalendarDate = MutableStateFlow<Calendar>(Calendar.getInstance())
    val selectedCalendarDate: StateFlow<Calendar> = _selectedCalendarDate.asStateFlow()

    // --- Hijri & Gregorian Astronomical Calendar Engine ---
    private val _hijriDayOffset = MutableStateFlow(prefs.getInt("hijri_day_offset", 0))
    val hijriDayOffset: StateFlow<Int> = _hijriDayOffset.asStateFlow()

    val isBangladeshLocation: StateFlow<Boolean> = _selectedCityLocation.map { city ->
        HijriDateCalculator.isBangladeshLocation(city.latitude, city.longitude, "${city.nameBn} ${city.nameEn}")
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HijriDateCalculator.isBangladeshLocation(initialLat, initialLng, prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা")
    )

    fun setHijriDayOffset(offset: Int) {
        _hijriDayOffset.value = offset
        prefs.edit().putInt("hijri_day_offset", offset).apply()
        recalculateSolarTimes()
    }

    val currentHijriDate: StateFlow<HijriDateResult> = combine(
        _selectedCalendarDate,
        _hijriDayOffset,
        _selectedCityLocation
    ) { date, offset, city ->
        val isBD = HijriDateCalculator.isBangladeshLocation(city.latitude, city.longitude, "${city.nameBn} ${city.nameEn}")
        HijriDateCalculator.calculateHijriDate(date, offset, isBD)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HijriDateCalculator.calculateHijriDate(
            Calendar.getInstance(),
            prefs.getInt("hijri_day_offset", 0),
            HijriDateCalculator.isBangladeshLocation(initialLat, initialLng, prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা")
        )
    )

    val currentGregorianDate: StateFlow<GregorianDateResult> = _selectedCalendarDate.map { date ->
        HijriDateCalculator.calculateGregorianDate(date)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HijriDateCalculator.calculateGregorianDate(Calendar.getInstance())
    )

    // --- Ramadan Mode ---
    val ramadanStore = com.example.ui.noorup.ramadan.RamadanStore(prefs)
    private val _ramadanMode = MutableStateFlow(ramadanStore.modePref())
    val ramadanMode: StateFlow<com.example.ui.noorup.ramadan.RamadanModePref> = _ramadanMode.asStateFlow()

    /** Bumped whenever a Ramadan tracker is written, so tracker UIs re-read the store. */
    private val _ramadanTick = MutableStateFlow(0)
    val ramadanTick: StateFlow<Int> = _ramadanTick.asStateFlow()
    fun ramadanChanged() { _ramadanTick.update { it + 1 } }

    fun setRamadanMode(mode: com.example.ui.noorup.ramadan.RamadanModePref) {
        ramadanStore.setModePref(mode)
        _ramadanMode.value = mode
    }

    /** Auto: on for the whole of Hijri month 9. On/Off override it (On also previews outside Ramadan). */
    val ramadanActive: StateFlow<Boolean> = combine(_ramadanMode, currentHijriDate) { mode, hijri ->
        when (mode) {
            com.example.ui.noorup.ramadan.RamadanModePref.ON -> true
            com.example.ui.noorup.ramadan.RamadanModePref.OFF -> false
            com.example.ui.noorup.ramadan.RamadanModePref.AUTO ->
                com.example.ui.noorup.ramadan.RamadanLogic.isRamadan(hijri.month)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentMoonPhase: StateFlow<MoonPhaseInfo> = combine(
        _selectedCalendarDate,
        _hijriDayOffset,
        _selectedCityLocation
    ) { date, offset, city ->
        val isBD = HijriDateCalculator.isBangladeshLocation(city.latitude, city.longitude, "${city.nameBn} ${city.nameEn}")
        HijriDateCalculator.getMoonPhase(date, offset, isBD)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HijriDateCalculator.getMoonPhase(
            Calendar.getInstance(),
            prefs.getInt("hijri_day_offset", 0),
            HijriDateCalculator.isBangladeshLocation(initialLat, initialLng, prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা")
        )
    )

    // Calendar browsing state for Hijri Month Grid
    private val _browsingHijriMonth = MutableStateFlow(
        HijriDateCalculator.calculateHijriDate(
            Calendar.getInstance(),
            prefs.getInt("hijri_day_offset", 0),
            HijriDateCalculator.isBangladeshLocation(initialLat, initialLng, prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা")
        ).month
    )
    val browsingHijriMonth: StateFlow<Int> = _browsingHijriMonth.asStateFlow()

    private val _browsingHijriYear = MutableStateFlow(
        HijriDateCalculator.calculateHijriDate(
            Calendar.getInstance(),
            prefs.getInt("hijri_day_offset", 0),
            HijriDateCalculator.isBangladeshLocation(initialLat, initialLng, prefs.getString("selected_city_name_bn", "উত্তরা, ঢাকা") ?: "উত্তরা, ঢাকা")
        ).year
    )
    val browsingHijriYear: StateFlow<Int> = _browsingHijriYear.asStateFlow()

    val hijriMonthGrid: StateFlow<List<HijriMonthDay>> = combine(
        _browsingHijriYear,
        _browsingHijriMonth,
        _hijriDayOffset,
        _selectedCityLocation
    ) { year, month, offset, city ->
        val isBD = HijriDateCalculator.isBangladeshLocation(city.latitude, city.longitude, "${city.nameBn} ${city.nameEn}")
        HijriDateCalculator.getHijriMonthGrid(year, month, offset, isBD)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicIslamicMilestones: StateFlow<List<DynamicIslamicMilestone>> = currentHijriDate.map { hijri ->
        HijriDateCalculator.getDynamicIslamicMilestones(hijri)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextBrowsingHijriMonth() {
        if (_browsingHijriMonth.value == 12) {
            _browsingHijriMonth.value = 1
            _browsingHijriYear.value += 1
        } else {
            _browsingHijriMonth.value += 1
        }
    }

    fun prevBrowsingHijriMonth() {
        if (_browsingHijriMonth.value == 1) {
            _browsingHijriMonth.value = 12
            _browsingHijriYear.value -= 1
        } else {
            _browsingHijriMonth.value -= 1
        }
    }

    fun resetBrowsingHijriToCurrent() {
        val cur = currentHijriDate.value
        _browsingHijriMonth.value = cur.month
        _browsingHijriYear.value = cur.year
    }

    private fun recalculateSolarTimes() {
        val loc = _selectedCityLocation.value
        val date = _selectedCalendarDate.value
        val method = _calculationMethod.value
        val juristic = _juristicMethod.value
        // Use the selected city's timezone (with DST) so London/New York times are
        // correct; live GPS positions (blank zoneId) use the device timezone.
        val tzOffset = tzOffsetFor(loc.zoneId, date.timeInMillis)

        val calc = SolarPrayerEngine.calculateTimes(
            latitude = loc.latitude,
            longitude = loc.longitude,
            calendar = date,
            timezoneOffset = tzOffset,
            method = method,
            juristic = juristic
        )
        _calculatedPrayerTimes.value = calc

        _selectedCityLocation.value = loc.copy(
            fajrStart = calc.fajrStart,
            fajrEnd = calc.fajrEnd,
            makruhSunriseStart = calc.makruhSunriseStart,
            makruhSunriseEnd = calc.makruhSunriseEnd,
            dhuhrStart = calc.dhuhrStart,
            dhuhrEnd = calc.dhuhrEnd,
            makruhZawalStart = calc.makruhZawalStart,
            makruhZawalEnd = calc.makruhZawalEnd,
            asrStart = calc.asrStart,
            asrEnd = calc.asrEnd,
            makruhSunsetStart = calc.makruhSunsetStart,
            makruhSunsetEnd = calc.makruhSunsetEnd,
            maghribStart = calc.maghribStart,
            maghribEnd = calc.maghribEnd,
            ishaStart = calc.ishaStart,
            ishaEnd = calc.ishaEnd
        )

        // Heavy IPC (5× setAlarmClock + widget RemoteViews + WorkManager DB) off main.
        val app = getApplication<Application>()
        val isEng = _isEnglish.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (prefs.getBoolean("prayer_notifications_enabled", true)) {
                    PrayerNotificationScheduler.scheduleAllPrayerAlerts(
                        context = app,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        method = method,
                        juristic = juristic,
                        isEnglish = isEng,
                        zoneId = loc.zoneId
                    )
                }
            } catch (_: Exception) { }
            try {
                NoorUpWidgetProvider.updateAllWidgets(app)
            } catch (_: Exception) { }
            try { refreshAvailableCities() } catch (_: Exception) { }
        }
    }

    private val _availableCities = MutableStateFlow<List<CityLocation>>(emptyList())
    val availableCitiesFlow: StateFlow<List<CityLocation>> = _availableCities.asStateFlow()

    private fun refreshAvailableCities() {
        val method = _calculationMethod.value
        val juristic = _juristicMethod.value
        _availableCities.value = listOf(
            buildPresetCity("ঢাকা", "Dhaka", 23.8103, 90.4125, "Asia/Dhaka"),
            buildPresetCity("চট্টগ্রাম", "Chattogram", 22.3569, 91.7832, "Asia/Dhaka"),
            buildPresetCity("সিলেট", "Sylhet", 24.8949, 91.8687, "Asia/Dhaka"),
            buildPresetCity("রাজশাহী", "Rajshahi", 24.3745, 88.6042, "Asia/Dhaka"),
            buildPresetCity("খুলনা", "Khulna", 22.8456, 89.5403, "Asia/Dhaka"),
            buildPresetCity("বরিশাল", "Barishal", 22.7010, 90.3535, "Asia/Dhaka"),
            buildPresetCity("রংপুর", "Rangpur", 25.7439, 89.2752, "Asia/Dhaka"),
            buildPresetCity("ময়মনসিংহ", "Mymensingh", 24.7471, 90.4203, "Asia/Dhaka"),
            buildPresetCity("মক্কা মুকাররমা", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"),
            buildPresetCity("মদিনা মুনাওয়ারা", "Madinah", 24.4672, 39.6111, "Asia/Riyadh"),
            buildPresetCity("লন্ডন", "London", 51.5074, -0.1278, "Europe/London"),
            buildPresetCity("নিউ ইয়র্ক", "New York", 40.7128, -74.0060, "America/New_York")
        )
    }

    fun setCalendarDate(calendar: Calendar) {
        _selectedCalendarDate.value = calendar.clone() as Calendar
        loadHabitsForSelectedDate()
        recalculateSolarTimes()
    }

    fun setDate(year: Int, monthZeroIndexed: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _selectedCalendarDate.value = cal
        loadHabitsForSelectedDate()
        recalculateSolarTimes()
    }

    fun goToNextDay() {
        val cal = (_selectedCalendarDate.value.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        _selectedCalendarDate.value = cal
        loadHabitsForSelectedDate()
        recalculateSolarTimes()
    }

    fun goToPreviousDay() {
        val cal = (_selectedCalendarDate.value.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _selectedCalendarDate.value = cal
        recalculateSolarTimes()
    }

    fun resetToToday() {
        _selectedCalendarDate.value = Calendar.getInstance()
        loadHabitsForSelectedDate()
        recalculateSolarTimes()
    }

    fun setCalculationMethod(method: CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit().putString("calculation_method", method.name).apply()
        recalculateSolarTimes()
    }

    fun setJuristicMethod(juristic: JuristicMethod) {
        _juristicMethod.value = juristic
        prefs.edit().putString("juristic_method", juristic.name).apply()
        recalculateSolarTimes()
    }

    fun setCityLocation(city: CityLocation) {
        _selectedCityLocation.value = city
        prefs.edit()
            .putString("selected_city_name_bn", city.nameBn)
            .putString("selected_city_name_en", city.nameEn)
            .putFloat("selected_city_lat", city.latitude.toFloat())
            .putFloat("selected_city_lng", city.longitude.toFloat())
            .putString("selected_city_lat_dbl", city.latitude.toString())
            .putString("selected_city_lng_dbl", city.longitude.toString())
            .putString("selected_city_zone_id", city.zoneId)
            .apply()
        recalculateSolarTimes()
    }

    fun updateGpsLocation(latitude: Double, longitude: Double, areaName: String, areaNameBn: String) {
        val cityLocation = CityLocation(
            nameBn = areaNameBn,
            nameEn = areaName,
            latitude = latitude,
            longitude = longitude,
            fajrStart = "04:48 AM",
            fajrEnd = "05:58 AM",
            makruhSunriseStart = "05:59 AM",
            makruhSunriseEnd = "06:15 AM",
            dhuhrStart = "12:05 PM",
            dhuhrEnd = "04:21 PM",
            makruhZawalStart = "11:50 AM",
            makruhZawalEnd = "12:04 PM",
            asrStart = "04:22 PM",
            asrEnd = "05:59 PM",
            makruhSunsetStart = "06:00 PM",
            makruhSunsetEnd = "06:17 PM",
            maghribStart = "06:18 PM",
            maghribEnd = "07:32 PM",
            ishaStart = "07:33 PM",
            ishaEnd = "04:47 AM"
        )
        _selectedCityLocation.value = cityLocation
        prefs.edit()
            .putString("selected_city_name_bn", areaNameBn)
            .putString("selected_city_name_en", areaName)
            .putFloat("selected_city_lat", latitude.toFloat())
            .putFloat("selected_city_lng", longitude.toFloat())
            .putString("selected_city_lat_dbl", latitude.toString())
            .putString("selected_city_lng_dbl", longitude.toString())
            .putString("selected_city_zone_id", "")
            .apply()
        recalculateSolarTimes()
    }

    private fun toEnglishDigitsStrict(s: String): String {
        val map = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9',
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return s.map { map[it] ?: it }.joinToString("")
    }

    fun parseTimeToMins(timeStr: String): Int {
        try {
            val clean = toEnglishDigitsStrict(timeStr).trim().uppercase()
            val isPm = clean.contains("PM")
            val timePortion = clean.replace("AM", "").replace("PM", "").trim()
            val timeParts = timePortion.split(":")
            var hours = timeParts[0].trim().toInt()
            val mins = timeParts[1].trim().takeWhile { it.isDigit() }.toInt()
            if (isPm && hours < 12) hours += 12
            if (!isPm && hours == 12) hours = 0
            return hours * 60 + mins
        } catch (e: Exception) {
            return 0
        }
    }

    // --- Tasbih State (dynamic definitions, persisted mid-round) ---
    private data class InitialTasbihState(
        val active: TasbihDhikr,
        val stageIndex: Int,
        val count: Int,
        val goal: Int,
        val completed: Boolean
    )

    private fun resolveInitialTasbih(custom: List<TasbihDhikr>): InitialTasbihState {
        val savedActiveId = prefs.getString("tasbih_active_id", null)
        val savedActive = savedActiveId?.let { TasbihRepository.findById(it, custom) }
        val legacyText = prefs.getString("tasbih_dhikr", null)
        val active = savedActive
            ?: legacyText?.let { text ->
                TasbihRepository.findBuiltInByLegacyText(text)
                    ?: TasbihRepository.legacyDefinition(
                        text,
                        prefs.getInt("tasbih_goal", TasbihRepository.builtIns.first().stages.first().target)
                    )
            }
            ?: TasbihRepository.builtIns.first()

        val stageIndex = prefs.getInt("tasbih_stage", 0)
            .coerceIn(0, active.stages.lastIndex)
        val savedGoal = prefs.getInt(
            "tasbih_goal",
            active.stages[stageIndex].target
        ).coerceIn(1, MAX_TASBIH_GOAL)
        val savedCount = prefs.getInt("tasbih_count", 0)
            .coerceIn(0, savedGoal)
        val completed = prefs.getBoolean("tasbih_completed", false) ||
            (savedCount >= savedGoal && stageIndex == active.stages.lastIndex)
        return InitialTasbihState(active, stageIndex, savedCount, savedGoal, completed)
    }

    private val initialCustomDhikrs = TasbihRepository.loadCustom(prefs)
    private val initialTasbih = resolveInitialTasbih(initialCustomDhikrs)

    private val _customDhikrs = MutableStateFlow(initialCustomDhikrs)
    val customDhikrs: StateFlow<List<TasbihDhikr>> = _customDhikrs.asStateFlow()

    private val _activeDhikr = MutableStateFlow(initialTasbih.active)
    val activeDhikr: StateFlow<TasbihDhikr> = _activeDhikr.asStateFlow()

    private val _tasbihStageIndex = MutableStateFlow(initialTasbih.stageIndex)
    val tasbihStageIndex: StateFlow<Int> = _tasbihStageIndex.asStateFlow()

    private val _tasbihCount = MutableStateFlow(initialTasbih.count)
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _tasbihGoal = MutableStateFlow(initialTasbih.goal)
    val tasbihGoal: StateFlow<Int> = _tasbihGoal.asStateFlow()

    private val _tasbihCompleted = MutableStateFlow(initialTasbih.completed)
    val tasbihCompleted: StateFlow<Boolean> = _tasbihCompleted.asStateFlow()

    private val _currentDhikr = MutableStateFlow(initialTasbih.active.fullTitle(false))
    val currentDhikr: StateFlow<String> = _currentDhikr.asStateFlow()

    // No replay: restoring a completed round must not re-fire celebration or
    // vibration. The UI separately renders the persisted completed state.
    private val _completionEvents = MutableSharedFlow<TasbihCompletionEvent>(
        extraBufferCapacity = 4
    )
    val completionEvents: SharedFlow<TasbihCompletionEvent> = _completionEvents.asSharedFlow()

    private fun persistTasbihState() {
        prefs.edit()
            .putInt("tasbih_count", _tasbihCount.value)
            .putInt("tasbih_goal", _tasbihGoal.value)
            .putString("tasbih_dhikr", _currentDhikr.value)
            .putString("tasbih_active_id", _activeDhikr.value.id)
            .putInt("tasbih_stage", _tasbihStageIndex.value)
            .putBoolean("tasbih_completed", _tasbihCompleted.value)
            .apply()
    }

    /** Exactly one increment per call. Extra taps after completion are ignored. */
    fun incrementTasbih() {
        if (_tasbihCompleted.value) return
        val active = _activeDhikr.value
        val stageIndex = _tasbihStageIndex.value.coerceIn(0, active.stages.lastIndex)
        val goal = _tasbihGoal.value.coerceIn(1, MAX_TASBIH_GOAL)

        if (_tasbihCount.value >= goal) {
            if (stageIndex == active.stages.lastIndex) {
                completeTasbihRound(active)
            } else {
                advanceTasbihStage(active, stageIndex)
            }
            return
        }

        val next = _tasbihCount.value + 1
        if (next < goal) {
            _tasbihCount.value = next
            persistTasbihState()
            return
        }

        if (stageIndex < active.stages.lastIndex) {
            advanceTasbihStage(active, stageIndex)
        } else {
            _tasbihCount.value = next
            completeTasbihRound(active)
        }
    }

    private fun advanceTasbihStage(active: TasbihDhikr, stageIndex: Int) {
        val nextIndex = (stageIndex + 1).coerceAtMost(active.stages.lastIndex)
        _tasbihStageIndex.value = nextIndex
        _tasbihCount.value = 0
        _tasbihGoal.value = active.stages[nextIndex].target.coerceIn(1, MAX_TASBIH_GOAL)
        _tasbihCompleted.value = false
        persistTasbihState()
    }

    private fun completeTasbihRound(active: TasbihDhikr) {
        if (_tasbihCompleted.value) return
        _tasbihCompleted.value = true
        _tasbihCount.value = _tasbihGoal.value
        persistTasbihState()
        TasbihFeedback.vibrateCompletion(getApplication())
        _completionEvents.tryEmit(
            TasbihCompletionEvent(
                titleBangla = active.titleBangla,
                titleEnglish = active.titleEnglish,
                totalTarget = if (active.stages.size == 1) _tasbihGoal.value else active.totalTarget,
                stageCount = active.stages.size
            )
        )
    }

    fun resetTasbih() {
        val active = _activeDhikr.value
        if (_tasbihStageIndex.value != 0) {
            _tasbihGoal.value = active.stages.first().target.coerceIn(1, MAX_TASBIH_GOAL)
        }
        _tasbihStageIndex.value = 0
        _tasbihCount.value = 0
        _tasbihCompleted.value = false
        persistTasbihState()
    }

    /**
     * Legacy API kept for the existing test suite. Selects a built-in/custom
     * definition when possible; unknown legacy strings become a transient
     * single-stage definition so no user data is lost.
     */
    fun setDhikr(dhikr: String, goal: Int) {
        val sane = goal.coerceIn(1, MAX_TASBIH_GOAL)
        val found = TasbihRepository.findBuiltInByLegacyText(dhikr)
            ?: TasbihRepository.findById(dhikr, _customDhikrs.value)
            ?: TasbihRepository.legacyDefinition(dhikr, sane)
        _currentDhikr.value = dhikr
        _activeDhikr.value = found
        _tasbihStageIndex.value = 0
        _tasbihCount.value = 0
        _tasbihGoal.value = sane
        _tasbihCompleted.value = false
        persistTasbihState()
    }

    /** User-editable round goal (1..MAX_TASBIH_GOAL), persisted. Keeps the current count. */
    fun setTasbihGoal(goal: Int) {
        val sane = goal.coerceIn(1, MAX_TASBIH_GOAL)
        _tasbihGoal.value = sane

        val active = _activeDhikr.value
        if (!active.isBuiltIn && active.id != "legacy_active") {
            // Custom definitions own their targets; keep the edit persisted.
            val updated = active.copy(
                stages = active.stages.mapIndexed { index, stage ->
                    if (index == _tasbihStageIndex.value) stage.copy(target = sane) else stage
                }
            )
            _activeDhikr.value = updated
            _currentDhikr.value = updated.fullTitle(false)
            val newCustom = _customDhikrs.value.map { if (it.id == updated.id) updated else it }
            _customDhikrs.value = newCustom
            TasbihRepository.saveCustom(prefs, newCustom)
        } else if (active.id == "legacy_active") {
            _activeDhikr.value = active.copy(
                stages = listOf(active.stages.first().copy(target = sane))
            )
        }

        _tasbihCompleted.value = _tasbihCount.value >= sane
        persistTasbihState()
    }

    /** Selects one of the built-in or user-created definitions. */
    fun selectDhikr(id: String): Boolean {
        val found = TasbihRepository.findById(id, _customDhikrs.value) ?: return false
        _activeDhikr.value = found
        _currentDhikr.value = found.fullTitle(false)
        _tasbihStageIndex.value = 0
        _tasbihCount.value = 0
        _tasbihGoal.value = found.stages.first().target.coerceIn(1, MAX_TASBIH_GOAL)
        _tasbihCompleted.value = false
        persistTasbihState()
        return true
    }

    /** Creates a custom definition, persists it, and selects it. */
    fun addCustomDhikr(
        titleBangla: String,
        titleEnglish: String,
        arabicText: String,
        stages: List<TasbihStage>
    ): String? {
        if (TasbihRepository.validationError(titleBangla, titleEnglish, stages) != null) {
            return null
        }
        val id = TasbihRepository.newCustomId()
        val candidate = TasbihDhikr(
            id = id,
            titleBangla = titleBangla,
            titleEnglish = titleEnglish,
            arabicText = arabicText,
            stages = stages,
            isBuiltIn = false
        )
        val safe = TasbihRepository.sanitizeCustom(listOf(candidate)).firstOrNull()
            ?: return null
        val newCustom = _customDhikrs.value + safe
        _customDhikrs.value = newCustom
        TasbihRepository.saveCustom(prefs, newCustom)
        selectDhikr(id)
        return id
    }

    /** Built-ins and unknown ids are rejected; editing never mutates a built-in. */
    fun updateCustomDhikr(
        id: String,
        titleBangla: String,
        titleEnglish: String,
        arabicText: String,
        stages: List<TasbihStage>
    ): Boolean {
        if (TasbihRepository.isBuiltInId(id)) return false
        val existing = _customDhikrs.value.firstOrNull { it.id == id } ?: return false
        if (existing.isBuiltIn) return false
        if (TasbihRepository.validationError(titleBangla, titleEnglish, stages) != null) {
            return false
        }
        val candidate = TasbihDhikr(
            id = id,
            titleBangla = titleBangla,
            titleEnglish = titleEnglish,
            arabicText = arabicText,
            stages = stages,
            isBuiltIn = false
        )
        val safe = TasbihRepository.sanitizeCustom(listOf(candidate)).firstOrNull()
            ?: return false
        val newCustom = _customDhikrs.value.map { if (it.id == id) safe else it }
        _customDhikrs.value = newCustom
        TasbihRepository.saveCustom(prefs, newCustom)

        if (_activeDhikr.value.id == id) {
            _activeDhikr.value = safe
            _currentDhikr.value = safe.fullTitle(false)
            _tasbihStageIndex.value = 0
            _tasbihCount.value = 0
            _tasbihGoal.value = safe.stages.first().target.coerceIn(1, MAX_TASBIH_GOAL)
            _tasbihCompleted.value = false
            persistTasbihState()
        }
        return true
    }

    /** Only user-created definitions can be deleted. */
    fun deleteCustomDhikr(id: String): Boolean {
        if (TasbihRepository.isBuiltInId(id)) return false
        val existing = _customDhikrs.value.firstOrNull { it.id == id } ?: return false
        if (existing.isBuiltIn) return false
        val newCustom = _customDhikrs.value.filterNot { it.id == id }
        _customDhikrs.value = newCustom
        TasbihRepository.saveCustom(prefs, newCustom)
        if (_activeDhikr.value.id == id) {
            selectDhikr(TasbihRepository.builtIns.first().id)
        }
        return true
    }

    /** Convenience for customising a protected built-in without mutating it. */
    fun duplicateBuiltInDhikr(id: String): String? {
        val source = TasbihRepository.builtIns.firstOrNull { it.id == id } ?: return null
        val copy = source.copy(
            id = TasbihRepository.newCustomId(),
            titleBangla = "${source.titleBangla} (কপি)",
            titleEnglish = "${source.titleEnglish} (Copy)",
            isBuiltIn = false
        )
        val safe = TasbihRepository.sanitizeCustom(listOf(copy)).firstOrNull()
            ?: return null
        val newCustom = _customDhikrs.value + safe
        _customDhikrs.value = newCustom
        TasbihRepository.saveCustom(prefs, newCustom)
        selectDhikr(safe.id)
        return safe.id
    }

    // --- Quran / Hifz State (114 Surah metadata; verses load on demand) ---
    //
    // Release-blocker fix (v1.2.0): unopened surahs keep EMPTY verse lists.
    // Synthetic placeholder verses must never render as genuine Quranic text,
    // so "not yet loaded" is represented explicitly until the bundled asset
    // loads and passes validation in QuranRepository.isAuthenticVerseList().
    // Surah names, numbers, verse counts, and revelation metadata stay available.
    private val _surahs = MutableStateFlow(
        com.example.ui.noorup.quran.QuranRepository.all114Surahs.map { surah ->
            surah.copy(verses = emptyList())
        }
    )
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    // Full offline surah verses (from assets/quran/surah_###.json), loaded on demand.
    private val _selectedSurahNumber = MutableStateFlow<Int?>(null)
    val selectedSurahNumber: StateFlow<Int?> = _selectedSurahNumber.asStateFlow()

    private val _selectedSurahVerses = MutableStateFlow<List<Verse>>(emptyList())
    val selectedSurahVerses: StateFlow<List<Verse>> = _selectedSurahVerses.asStateFlow()

    private val _isQuranLoading = MutableStateFlow(false)
    val isQuranLoading: StateFlow<Boolean> = _isQuranLoading.asStateFlow()

    // True when the most recent surah load failed validation or threw, with
    // the verse list kept empty. The UI renders an error + retry action.
    // Cleared on close, on opening another surah, and on successful load.
    private val _quranLoadError = MutableStateFlow(false)
    val quranLoadError: StateFlow<Boolean> = _quranLoadError.asStateFlow()

    // Focus-mode load state: the focus verse starts as null (spinner) until
    // the authentic text lands. Shares the quranLoadError flag for failures.
    private val _isFocusLoading = MutableStateFlow(false)
    val isFocusLoading: StateFlow<Boolean> = _isFocusLoading.asStateFlow()

    private var openSurahJob: kotlinx.coroutines.Job? = null
    private var focusLoadJob: kotlinx.coroutines.Job? = null
    private var bookmarkRefreshJob: kotlinx.coroutines.Job? = null

    // Unlike a freshly opened Surah, the bookmark screen must work before any
    // Surah has been loaded into memory after an app update/restart.
    private val _bookmarkedVerses = MutableStateFlow<List<Pair<Surah, Verse>>>(emptyList())
    val bookmarkedVerses: StateFlow<List<Pair<Surah, Verse>>> = _bookmarkedVerses.asStateFlow()

    // Last reader position, so the Quran tab can offer "Continue reading".
    // Independent from Focus and from saved Ayat bookmarks.
    private val _readerResume = MutableStateFlow(loadReaderResume())
    val readerResume: StateFlow<Pair<Int, Int>?> = _readerResume.asStateFlow()

    // One-shot request to scroll the reader to an Ayat once its verses load.
    // Explicit navigation (bookmark / Continue reading / Go to Ayat) uses this;
    // the UI consumes it after scrolling so it can never leak into a later open.
    private val _pendingReaderAyat = MutableStateFlow<Int?>(null)
    val pendingReaderAyat: StateFlow<Int?> = _pendingReaderAyat.asStateFlow()

    private fun loadReaderResume(): Pair<Int, Int>? {
        val surah = prefs.getInt("reader_last_surah", 0)
        val verse = prefs.getInt("reader_last_verse", 0)
        val total = com.example.ui.noorup.quran.QuranRepository.getSurahMeta(surah)?.totalVerses ?: 0
        if (surah !in 1..114 || total <= 0) return null
        return surah to verse.coerceIn(1, total)
    }

    private fun persistReaderPosition(surahNumber: Int, verseNumber: Int) {
        val total = com.example.ui.noorup.quran.QuranRepository.getSurahMeta(surahNumber)?.totalVerses ?: 0
        if (total <= 0) return
        val verse = verseNumber.coerceIn(1, total)
        _readerResume.value = surahNumber to verse
        prefs.edit()
            .putInt("reader_last_surah", surahNumber)
            .putInt("reader_last_verse", verse)
            .apply()
    }

    fun consumeReaderAyatJump() {
        _pendingReaderAyat.value = null
    }

    fun refreshBookmarkedVerses() {
        bookmarkRefreshJob?.cancel()
        bookmarkRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            val bookmarkedBySurah = prefs.all
                .asSequence()
                .filter { (key, value) -> value == true && Regex("^bm_(\\d{1,3})_(\\d{1,3})$").matches(key) }
                .mapNotNull { (key, _) ->
                    val match = Regex("^bm_(\\d{1,3})_(\\d{1,3})$").matchEntire(key) ?: return@mapNotNull null
                    val surah = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val verse = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    if (surah in 1..114 && verse > 0) surah to verse else null
                }
                .groupBy({ it.first }, { it.second })

            val restored = bookmarkedBySurah.toSortedMap().flatMap { (surahNumber, verseNumbers) ->
                val meta = com.example.ui.noorup.quran.QuranRepository.getSurahMeta(surahNumber)
                    ?: return@flatMap emptyList()
                com.example.ui.noorup.quran.QuranRepository
                    .loadFullSurah(getApplication<Application>().applicationContext, surahNumber)
                    .filter { it.number in verseNumbers }
                    .map { meta to it.copy(isBookmarked = true) }
            }
            _bookmarkedVerses.value = restored
        }
    }

    fun openSurah(surahNumber: Int) = openSurahInternal(surahNumber, jumpToAyat = null)

    /**
     * Open a Surah and scroll the reader to [verseNumber] once its bundled
     * verses load. Used by Continue reading, bookmarked Ayat, and reader jumps.
     */
    fun openSurahAt(surahNumber: Int, verseNumber: Int) =
        openSurahInternal(surahNumber, jumpToAyat = verseNumber)

    private fun openSurahInternal(surahNumber: Int, jumpToAyat: Int?) {
        val totalAyat =
            com.example.ui.noorup.quran.QuranRepository.getSurahMeta(surahNumber)?.totalVerses ?: 0
        val targetAyat = jumpToAyat?.coerceIn(1, maxOf(1, totalAyat))
        _pendingReaderAyat.value = targetAyat
        persistReaderPosition(surahNumber, targetAyat ?: 1)
        _selectedSurahNumber.value = surahNumber
        // Never publish unverified content: start empty, fill only on success.
        _selectedSurahVerses.value = emptyList()
        _quranLoadError.value = false
        _isQuranLoading.value = true
        openSurahJob?.cancel()
        openSurahJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val requested = surahNumber
                val ctx = getApplication<Application>().applicationContext
                val full = com.example.ui.noorup.quran.QuranRepository.loadFullSurah(ctx, requested)
                // Race guard: user opened another surah while we were on IO.
                if (_selectedSurahNumber.value != requested) return@launch
                val total = com.example.ui.noorup.quran.QuranRepository.getSurahMeta(requested)?.totalVerses ?: 0
                if (full.isNotEmpty() && com.example.ui.noorup.quran.QuranRepository.isAuthenticVerseList(full, total)) {
                    // Re-apply persisted bookmarks + memorized flags (never erase).
                    val enriched = full.map { v ->
                        v.copy(
                            isBookmarked = prefs.getBoolean("bm_${requested}_${v.number}", v.isBookmarked),
                            isMemorized = prefs.getBoolean("mem_${requested}_${v.number}", v.isMemorized)
                        )
                    }
                    if (_selectedSurahNumber.value != requested) return@launch
                    _selectedSurahVerses.value = enriched
                    // Keep master list in sync so bookmarks/search see authentic text
                    val cur = _surahs.value
                    _surahs.value = cur.map { s -> if (s.number == requested) s.copy(verses = enriched) else s }
                    _quranLoadError.value = false
                } else {
                    // Missing, partial, or invalid bundle: stay empty, show error.
                    _quranLoadError.value = true
                }
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "openSurah failed: $surahNumber", e)
                if (_selectedSurahNumber.value == surahNumber) _quranLoadError.value = true
            } finally {
                if (_selectedSurahNumber.value == surahNumber) _isQuranLoading.value = false
            }
        }
    }

    /** Retry the currently selected surah after a load failure. */
    fun retrySurahLoad() {
        _selectedSurahNumber.value?.let { surah ->
            val resumeAyat = _readerResume.value?.takeIf { it.first == surah }?.second
            openSurahInternal(surah, jumpToAyat = resumeAyat)
        }
    }

    fun closeSurah() {
        _selectedSurahNumber.value = null
        _selectedSurahVerses.value = emptyList()
        _pendingReaderAyat.value = null
        _isQuranLoading.value = false
        _quranLoadError.value = false
    }

    fun toggleBookmark(surahNumber: Int, verseNumber: Int) {
        val newBm: Boolean = !(_surahs.value.find { it.number == surahNumber }
            ?.verses?.find { it.number == verseNumber }?.isBookmarked ?: false)
        prefs.edit().putBoolean("bm_${surahNumber}_${verseNumber}", newBm).apply()
        _surahs.update { list ->
            list.map { surah ->
                if (surah.number == surahNumber) {
                    surah.copy(verses = surah.verses.map { verse ->
                        if (verse.number == verseNumber) verse.copy(isBookmarked = newBm) else verse
                    })
                } else surah
            }
        }
        if (_selectedSurahNumber.value == surahNumber) {
            _selectedSurahVerses.update { verses ->
                verses.map { v -> if (v.number == verseNumber) v.copy(isBookmarked = newBm) else v }
            }
        }
        // Keep focus verse bookmark in sync
        _focusVerse.update { fv ->
            if (_focusSurahNumber.value == surahNumber && fv?.number == verseNumber) {
                fv.copy(isBookmarked = newBm)
            } else fv
        }
        refreshBookmarkedVerses()
    }

    fun toggleMemorized(surahNumber: Int, verseNumber: Int) {
        val cur = _surahs.value.find { it.number == surahNumber }
            ?.verses?.find { it.number == verseNumber }?.isMemorized ?: false
        val next = !cur
        prefs.edit().putBoolean("mem_${surahNumber}_${verseNumber}", next).apply()
        val list = _surahs.value.map { surah ->
            if (surah.number == surahNumber) {
                surah.copy(verses = surah.verses.map { verse ->
                    if (verse.number == verseNumber) verse.copy(isMemorized = next) else verse
                })
            } else surah
        }
        _surahs.value = list
        if (_selectedSurahNumber.value == surahNumber) {
            _selectedSurahVerses.value = _selectedSurahVerses.value.map { v ->
                if (v.number == verseNumber) v.copy(isMemorized = next) else v
            }
        }
        val fv = _focusVerse.value
        if (_focusSurahNumber.value == surahNumber && fv?.number == verseNumber) {
            _focusVerse.value = fv.copy(isMemorized = next)
        }
    }

    // --- Hadith & Dua State ---
    val hadithRepository = com.example.ui.noorup.hadith.HadithRepository(application)

    private val _hadithBooks = MutableStateFlow<List<com.example.ui.noorup.hadith.Book>>(hadithRepository.defaultCanonicalBooks)
    val hadithBooks: StateFlow<List<com.example.ui.noorup.hadith.Book>> = _hadithBooks.asStateFlow()

    private val _allLoadedHadiths = MutableStateFlow<List<com.example.ui.noorup.hadith.Hadith>>(emptyList())
    val allLoadedHadiths: StateFlow<List<com.example.ui.noorup.hadith.Hadith>> = _allLoadedHadiths.asStateFlow()

    private val _selectedHadithBookId = MutableStateFlow("all")
    val selectedHadithBookId: StateFlow<String> = _selectedHadithBookId.asStateFlow()

    // Stable ID; labels are resolved only by the UI for the chosen language.
    private val _selectedHadithTopic = MutableStateFlow("all")
    val selectedHadithTopic: StateFlow<String> = _selectedHadithTopic.asStateFlow()

    private val _selectedHadithChapterId = MutableStateFlow(0)
    val selectedHadithChapterId: StateFlow<Int> = _selectedHadithChapterId.asStateFlow()

    private val _hadithSearchQuery = MutableStateFlow("")
    val hadithSearchQuery: StateFlow<String> = _hadithSearchQuery.asStateFlow()

    private val _hadithPage = MutableStateFlow(0)
    val hadithPage: StateFlow<Int> = _hadithPage.asStateFlow()

    private val _isHadithLoading = MutableStateFlow(true)
    val isHadithLoading: StateFlow<Boolean> = _isHadithLoading.asStateFlow()

    private val _hadithLoadError = MutableStateFlow<String?>(null)
    val hadithLoadError: StateFlow<String?> = _hadithLoadError.asStateFlow()

    private val _isLoadingMoreHadiths = MutableStateFlow(false)
    val isLoadingMoreHadiths: StateFlow<Boolean> = _isLoadingMoreHadiths.asStateFlow()

    // Explicitly refreshed filter state. The former nested combine/stateIn
    // pipeline could retain its initial list while the visible topic chip had
    // already changed. Each refresh cancels the previous computation, so an
    // older large search can never overwrite a newer selection.
    private val _filteredHadiths = MutableStateFlow<List<com.example.ui.noorup.hadith.Hadith>>(emptyList())
    val filteredHadiths: StateFlow<List<com.example.ui.noorup.hadith.Hadith>> = _filteredHadiths.asStateFlow()
    private val _displayedHadiths = MutableStateFlow<List<com.example.ui.noorup.hadith.Hadith>>(emptyList())
    val displayedHadiths: StateFlow<List<com.example.ui.noorup.hadith.Hadith>> = _displayedHadiths.asStateFlow()
    private val _hasMoreHadiths = MutableStateFlow(false)
    val hasMoreHadiths: StateFlow<Boolean> = _hasMoreHadiths.asStateFlow()
    private var hadithFilterJob: Job? = null

    private fun refreshHadithResults() {
        val all = _allLoadedHadiths.value
        val query = _hadithSearchQuery.value
        val bookId = _selectedHadithBookId.value
        val topic = _selectedHadithTopic.value
        val chapterId = _selectedHadithChapterId.value
        hadithFilterJob?.cancel()
        hadithFilterJob = viewModelScope.launch(Dispatchers.Default) {
            val filtered = hadithRepository.filterHadiths(all, query, bookId, topic, chapterId)
            val displayed = hadithRepository.paginate(filtered, page = 0, pageSize = 25)
            _filteredHadiths.value = filtered
            _displayedHadiths.value = displayed
            _hasMoreHadiths.value = displayed.size < filtered.size
        }
    }

    val featuredDailyHadith: StateFlow<com.example.ui.noorup.hadith.Hadith?> = _allLoadedHadiths.map { list ->
        if (list.isEmpty()) null
        else {
            // Rotate daily instead of always firstOrNull(): dayOfYear-indexed.
            val cal = java.util.Calendar.getInstance()
            val idx = (cal.get(java.util.Calendar.DAY_OF_YEAR) + cal.get(java.util.Calendar.YEAR)) % list.size
            list[kotlin.math.abs(idx) % list.size]
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectHadithBook(bookId: String) {
        _selectedHadithBookId.value = bookId
        // Individual source files do not share a verified topic taxonomy.
        // Never leave a cross-library topic restriction silently applied to a
        // single-book view.
        if (bookId != "all") _selectedHadithTopic.value = "all"
        _selectedHadithChapterId.value = 0
        _hadithPage.value = 0
        refreshHadithResults()
        viewModelScope.launch(Dispatchers.IO) {
            ensureBookLoaded(bookId)
        }
    }

    private suspend fun ensureBookLoaded(bookId: String) {
        try {
            _hadithLoadError.value = null
            if (bookId == "all") {
                // "All" is a corpus scope, not a cache snapshot. Load every
                // configured book before filtering so search/topic results are
                // never limited to whichever card was opened first.
                _isHadithLoading.value = true
                val all = mutableListOf<com.example.ui.noorup.hadith.Hadith>()
                for (book in hadithRepository.defaultCanonicalBooks) {
                    all += hadithRepository.getHadithsForBook(book.id)
                }
                val uniqueAll = all.distinctBy { it.id }
                _allLoadedHadiths.value = uniqueAll
                refreshHadithResults()
            } else if (!hadithRepository.isBookLoaded(bookId)) {
                _isHadithLoading.value = true
                val bookHadiths = hadithRepository.getHadithsForBook(bookId)
                _allLoadedHadiths.update { current ->
                    (current + bookHadiths).distinctBy { it.id }
                }
                refreshHadithResults()
            }
        } catch (e: Exception) {
            Log.e("NoorUpViewModel", "Error ensuring book loaded: $bookId", e)
            _hadithLoadError.value = e.message ?: "Unable to load Hadith data"
        } finally {
            _isHadithLoading.value = false
        }
    }

    fun selectHadithTopic(topic: String) {
        _selectedHadithTopic.value = hadithRepository.canonicalTopicId(topic)
        _hadithPage.value = 0
        refreshHadithResults()
    }

    fun selectHadithChapter(chapterId: Int) {
        _selectedHadithChapterId.value = chapterId
        _hadithPage.value = 0
        refreshHadithResults()
    }

    fun setHadithSearchQuery(query: String) {
        _hadithSearchQuery.value = query
        _hadithPage.value = 0
        refreshHadithResults()
    }

    fun loadNextHadithPage() {
        if (_isLoadingMoreHadiths.value || !hasMoreHadiths.value) return
        _isLoadingMoreHadiths.value = true
        _hadithPage.update { it + 1 }
        val displayed = hadithRepository.paginate(_filteredHadiths.value, _hadithPage.value, pageSize = 25)
        _displayedHadiths.value = displayed
        _hasMoreHadiths.value = displayed.size < _filteredHadiths.value.size
        _isLoadingMoreHadiths.value = false
    }

    fun loadHadithData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isHadithLoading.value = true
            try {
                val books = hadithRepository.getCanonicalBooks()
                _hadithBooks.value = books
                // Opening the Duas tab must remain instant. The full Hadith
                // corpus is deliberately deferred until the Hadith segment is
                // selected; parsing ~100 MB of JSON while composing this
                // screen caused an ANR on real phones.
                _allLoadedHadiths.value = hadithRepository.getHadithsForBook("bukhari")
                refreshHadithResults()
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "Error loading Hadith datasets from assets", e)
                _hadithLoadError.value = e.message ?: "Unable to load Hadith data"
            } finally {
                _isHadithLoading.value = false
            }
        }
    }

    private val _bookmarkedHadiths = MutableStateFlow<Set<Int>>(
        prefs.getStringSet("bookmarked_hadiths", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    )
    val bookmarkedHadiths: StateFlow<Set<Int>> = _bookmarkedHadiths.asStateFlow()

    fun toggleHadithBookmark(hadithId: Int) {
        // Never do prefs I/O inside update{}: the lambda can re-run on CAS retry
        // and would double-write. Compute first, then publish + persist once.
        val cur = _bookmarkedHadiths.value
        val updated = if (cur.contains(hadithId)) cur - hadithId else cur + hadithId
        _bookmarkedHadiths.value = updated
        prefs.edit().putStringSet("bookmarked_hadiths", updated.map { it.toString() }.toSet()).apply()
    }

    // --- Islamic Assistant Chat State ---
    // Answers come from Gemini through the NoorUp AI proxy. When the proxy is not built in,
    // unreachable, or rate limited, the built-in rule-based engine answers instead.
    internal var aiChat: AiChatClient = ProxyAiChatClient(BuildConfig.AI_PROXY_URL) { aiDeviceId() }

    /** Random per-install id the proxy uses for rate limiting. Carries no personal data. */
    private fun aiDeviceId(): String =
        prefs.getString("ai_device_id", null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString("ai_device_id", it).apply()
        }

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("আসসালামু আলাইকুম! আমি নূরআপ ইসলামিক সহকারী। ইসলামিক আদব, মাসআলা, দুআ কিংবা ইতিহাস সম্পর্কে যেকোনো প্রশ্ন করতে পারেন। / Assalamu Alaikum! I am your NoorUp Islamic assistant. Feel free to ask any question.", false, "এখন")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun sendAiMessage(prompt: String, isEng: Boolean) {
        if (prompt.isBlank()) return
        // Context for the model: recent turns before this question, without the canned greeting.
        val history = _chatMessages.value.drop(1).takeLast(AI_HISTORY_MESSAGES)
        val userMsg = ChatMessage(prompt, true, "এইমাত্র")
        _chatMessages.update { it + userMsg }
        _isAiThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalReply = answerQuestion(prompt, history, isEng)
                _chatMessages.update { it + ChatMessage(finalReply, false, "এইমাত্র") }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    private suspend fun answerQuestion(prompt: String, history: List<ChatMessage>, isEng: Boolean): String {
        val offline = generateLocalIslamicResponse(prompt, isEng)
        if (!aiChat.isConfigured) return offline
        // The offline answers carry Quran/Hadith references, so they ground the AI answer for known topics.
        val isKnownTopic = offline != genericLocalReply(isEng)
        return aiChat.ask(prompt, history, isEng, if (isKnownTopic) offline else "").getOrElse { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            val failure = (error as? AiChatException)?.failure ?: AiFailure.OTHER
            val note = aiUnavailableNote(failure, isEng)
            // A topic the offline engine knows keeps its answer; otherwise only the notice is useful.
            if (isKnownTopic) "$offline\n\n$note" else note
        }
    }

    /** Plain-language notice for each failure. Never includes status codes or technical text. */
    private fun aiUnavailableNote(failure: AiFailure, isEng: Boolean): String = when (failure) {
        AiFailure.NETWORK ->
            if (isEng) "No internet connection, so AI answers are unavailable. Connect and ask again for a fuller answer."
            else "ইন্টারনেট সংযোগ নেই, তাই এআই উত্তর পাওয়া যাচ্ছে না। সংযোগ চালু করে আবার জিজ্ঞাসা করলে বিস্তারিত উত্তর পাবেন।"
        AiFailure.TIMEOUT ->
            if (isEng) "Your connection is slow or unstable, so the AI answer did not arrive. Please try again."
            else "আপনার ইন্টারনেট ধীর বা অস্থির, তাই এআই উত্তর আসেনি। আবার চেষ্টা করুন।"
        AiFailure.TOO_FAST ->
            if (isEng) "You are asking too quickly. Please wait a minute and try again."
            else "আপনি খুব দ্রুত প্রশ্ন করছেন। এক মিনিট অপেক্ষা করে আবার চেষ্টা করুন।"
        AiFailure.LIMIT ->
            if (isEng) "The free AI limit has been reached for now. Please try again a little later."
            else "এখনকার জন্য ফ্রি এআই সীমা শেষ হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
        AiFailure.BUSY ->
            if (isEng) "The AI assistant is very busy right now. Please try again in a minute."
            else "এআই সহকারী এখন অনেক ব্যস্ত। এক মিনিট পর আবার চেষ্টা করুন।"
        AiFailure.TOO_LONG ->
            if (isEng) "Your question is too long. Please shorten it and ask again."
            else "আপনার প্রশ্নটি অনেক লম্বা। সংক্ষেপে লিখে আবার জিজ্ঞাসা করুন।"
        AiFailure.OTHER ->
            if (isEng) "The AI assistant is temporarily unavailable. Please try again shortly."
            else "এআই সহকারী সাময়িকভাবে অনুপলব্ধ। একটু পরে আবার চেষ্টা করুন।"
    }

    private fun genericLocalReply(isEng: Boolean): String =
        if (isEng) {
            "Thank you for your question. As your Islamic Companion, I recommend consulting authentic Quranic verses and Sahih Hadith for precise fiqh rulings. Feel free to ask about Salah times, daily Masnoon Duas, fasting rules, or Zakat calculation."
        } else {
            "আপনার জিজ্ঞাসার জন্য ধন্যবাদ। ইসলামি বিধান, নামাজের সঠিক সময়সূচী, রোজা, যাকাত, কুরআন তেলাওয়াত কিংবা দৈনন্দিন মাসনূন দোয়ার যেকোনো বিষয়ে আপনাকে সহযোগিতা করতে আমি প্রস্তুত। আরো সুনির্দিষ্ট প্রশ্ন করতে পারেন।"
        }

    private fun generateLocalIslamicResponse(prompt: String, isEng: Boolean): String {
        val lower = prompt.trim().lowercase()
        return when {
            // Salam first: "assalamu alaikum" contains no "hi", but greetings like
            // "hi, salam" must resolve to the salam branch, not the generic one.
            lower.contains("salam") || lower.contains("সালাম") || lower.contains("assalam") -> {
                if (isEng) {
                    "Wa Alaikum Assalam wa Rahmatullahi wa Barakatuh! May Allah's peace, mercy, and blessings be upon you. How can I help you today regarding your prayers, Quran, or Islamic knowledge?"
                } else {
                    "ওয়ালাইকুমুস সালাম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু! আপনার উপরও আল্লাহর রহমত, শান্তি ও বরকত বর্ষিত হোক। আজকে আপনাকে নামাজ, কুরআন বা অন্য কোনো বিষয়ে কীভাবে সাহায্য করতে পারি?"
                }
            }
            lower.contains("hello") || lower.contains("hey") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "hi" || it == "hy" } -> {
                if (isEng) {
                    "Assalamu Alaikum wa Rahmatullahi wa Barakatuh! Welcome to the NoorUp Islamic Assistant. I am here to help you with Islamic queries, prayer timings, Quran & Hadith citations, Masnoon Duas, and daily spirituality. How can I assist you today?"
                } else {
                    "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু! নূরআপ ইসলামিক সহকারিতে আপনাকে স্বাগতম। নামাজ, কুরআন-হাদিস, মাসনূন দুআ, রোজা, যাকাত কিংবা ইসলামিক যেকোনো বিষয়ে আপনার যেকোনো প্রশ্ন করতে পারেন।"
                }
            }
            // --- Purification (before general prayer: "oju namaz" must hit wudu) ---
            lower.contains("wudu") || lower.contains("ওযু") || lower.contains("অজু") || lower.contains("ablution") -> {
                if (isEng) {
                    "Wudu (ablution) is required before Salah. Wash in order: intention in the heart, Bismillah, hands to wrists, mouth, nose, face, arms to elbows, wipe (masah) the head and ears, then feet to ankles. It is broken by passing wind, deep sleep, or using the toilet. The Prophet (ﷺ) said cleanliness is half of faith (Sahih Muslim)."
                } else {
                    "নামাজের আগে ওযু ফরজ। ক্রম: নিয়ত, বিসমিল্লাহ, কব্জি পর্যন্ত হাত, কুলি, নাকে পানি, মুখমণ্ডল, কনুই পর্যন্ত হাত, মাথা-কান মাসেহ, টাখনু পর্যন্ত পা। বায়ু নির্গমন, গভীর ঘুম বা প্রস্রাব-পায়খানায় ওযু ভেঙে যায়। রাসূল (সা.) বলেছেন পবিত্রতা ঈমানের অর্ধেক (সহীহ মুসলিম)।"
                }
            }
            lower.contains("ghusl") || lower.contains("গোসল") || lower.contains("নাপাক") -> {
                if (isEng) {
                    "Ghusl (full bath) becomes obligatory after marital relations, wet dreams with discharge, and when menstruation or post-natal bleeding ends. Method: intention, Bismillah, wash hands and private parts, perform wudu, then pour water over the whole body ensuring no spot stays dry, including rinsing the mouth and nose. For doubtful cases, ask your local imam."
                } else {
                    "স্বপ্নদোষে বীর্যপাত, সহবাস এবং হায়েজ-নেফাস শেষ হলে গোসল ফরজ হয়। পদ্ধতি: নিয়ত, বিসমিল্লাহ, হাত ও লজ্জাস্থান ধোয়া, ওযু করা, তারপর কুলি ও নাকে পানিসহ সারা শরীরে পানি পৌঁছে দেওয়া যেন কোথাও শুকনো না থাকে। সন্দেহজনক ক্ষেত্রে স্থানীয় ইমামের পরামর্শ নিন।"
                }
            }
            lower.contains("tayammum") || lower.contains("তায়াম্মুম") || lower.contains("তায়ামুম") -> {
                if (isEng) {
                    "Tayammum (dry ablution) is allowed when water is unavailable or its use would harm the sick: strike clean earth or dust with both palms once, wipe the face, then wipe both hands up to the wrists. One tayammum suffices for one prayer time; it breaks with whatever breaks wudu or when water becomes available."
                } else {
                    "পানি না পেলে বা অসুস্থতার কারণে পানি ক্ষতিকর হলে তায়াম্মুম জায়েজ: পবিত্র মাটি বা ধুলায় দুই হাত একবার মেরে মুখমণ্ডল মাসেহ, তারপর কব্জি পর্যন্ত দুই হাত মাসেহ। এক তায়াম্মুমে এক ওয়াক্তের নামাজ; ওযু ভঙ্গের কারণে বা পানি পাওয়া গেলে তা ভেঙে যায়।"
                }
            }
            // --- Prayer specifics (before the general prayer branch) ---
            lower.contains("jumuah") || lower.contains("জুমা") || lower.contains("জুম্মা") || lower.contains("জুমআ") || lower.contains("friday") || lower.contains("শুক্রবার") -> {
                if (isEng) {
                    "Jumuah (Friday) prayer replaces Dhuhr: take ghusl, wear clean clothes, apply perfume, go early, listen to the khutbah silently, and pray 2 rak'ats behind the imam. Reciting Surah Al-Kahf and sending abundant salawat on Friday carry great virtue."
                } else {
                    "জুমার নামাজ যোহরের স্থলাভিষিক্ত: গোসল, পরিষ্কার পোশাক, সুগন্ধি, তাড়াতাড়ি যাওয়া, নীরবে খুতবা শোনা এবং ইমামের পেছনে ২ রাকাত ফরজ। শুক্রবারে সূরা কাহফ তিলাওয়াত ও বেশি বেশি দরুদ পাঠের বিরাট ফজিলত রয়েছে।"
                }
            }
            lower.contains("eid") || lower.contains("ঈদ") -> {
                if (isEng) {
                    "Eid prayer is 2 rak'ats prayed in congregation after sunrise (no adhan or iqamah), followed by the khutbah. On Eid al-Fitr pay Fitrah before the prayer; on Eid al-Adha those who can afford it offer Qurbani. Taking different routes to and from the Eidgah is Sunnah."
                } else {
                    "ঈদের নামাজ জামাতে ২ রাকাত, সূর্যোদয়ের পর (আজান-ইকামত ছাড়া), এরপর খুতবা। ঈদুল ফিতরে নামাজের আগেই ফিতরা দিন; ঈদুল আজহায় সামর্থ্যবানরা কুরবানি করবেন। ঈদগাহে এক পথে যাওয়া অন্য পথে ফেরা সুন্নত।"
                }
            }
            lower.contains("taraweeh") || lower.contains("তারাবি") || lower.contains("তারাবীহ") || lower.contains("tarawih") -> {
                if (isEng) {
                    "Taraweeh is the special night prayer of Ramadan, commonly 20 rak'ats in Hanafi practice in Bangladesh, prayed in sets of 2 after Isha. Whoever stands Ramadan nights in faith seeking reward has past sins forgiven (Bukhari & Muslim). Seek Laylatul Qadr in the odd nights of the last 10 days."
                } else {
                    "তারাবি রমজানের বিশেষ রাতের নামাজ; বাংলাদেশে হানাফি আমলে সাধারণত ২০ রাকাত, এশার পর ২ রাকাত করে। ঈমানের সাথে সওয়াবের আশায় রমজানের রাতে কিয়ামকারীর অতীত গুনাহ মাফ হয় (বুখারী ও মুসলিম)। শেষ ১০ দিনের বেজোড় রাতে লাইলাতুল কদর তালাশ করুন।"
                }
            }
            lower.contains("witr") || lower.contains("বিতর") || lower.contains("বেতের") || lower.contains("qunut") || lower.contains("কুনুত") -> {
                if (isEng) {
                    "Witr is the odd-numbered closing prayer of the night — 1 or 3 rak'ats prayed after Isha (and after Taraweeh in Ramadan). The Qunut supplication is recited in Witr; in Hanafi fiqh it is necessary (wajib), so learn its wording from the Dua tab or your imam."
                } else {
                    "বিতর রাতের শেষ বেজোড় নামাজ — এশার পর (রমজানে তারাবির পর) ১ বা ৩ রাকাত। বিতরে দোয়া কুনুত পড়া হয়; হানাফি ফিকহে এটি ওয়াজিব, তাই দোয়াটি দুআ ট্যাব বা ইমামের কাছ থেকে শিখে নিন।"
                }
            }
            lower.contains("janaza") || lower.contains("জানাজা") || lower.contains("জানাযা") || lower.contains("funeral") -> {
                if (isEng) {
                    "Janaza (funeral) prayer has 4 takbirs with no bowing or prostration: after the 1st recite Sana, after the 2nd send salawat (Darood Ibrahim), after the 3rd make dua for the deceased and all Muslims, after the 4th say salam to both sides. Joining it earns great reward (hadith)."
                } else {
                    "জানাজার নামাজে ৪ তাকবির, রুকু-সিজদা নেই: ১ম তাকবিরের পর সানা, ২য়-এর পর দরুদে ইবরাহীম, ৩য়-এর পর মৃত ও সকল মুসলিমের জন্য দোয়া, ৪র্থ-এর পর দুদিকে সালাম। এতে অংশ নেওয়ায় বিরাট সওয়াব রয়েছে (হাদিস)।"
                }
            }
            lower.contains("qasr") || lower.contains("কসর") || lower.contains("কছর") ||
                ((lower.contains("সফর") || lower.contains("travel")) && (lower.contains("নামাজ") || lower.contains("namaz") || lower.contains("prayer") || lower.contains("salah") || lower.contains("shorten"))) -> {
                if (isEng) {
                    "Qasr means shortening 4-rakat fard prayers to 2 while traveling. In Hanafi fiqh it applies from a journey of about 77 km (48 miles) until you intend to stay 15+ days — then pray full. Sunnah and nafl are prayed as usual when possible. Rulings differ between schools, so confirm with your imam."
                } else {
                    "কসর অর্থ সফরে ৪ রাকাত ফরজ ২ রাকাত পড়া। হানাফি ফিকহে প্রায় ৭৭ কিমি (৪৮ মাইল) সফর থেকে ১৫+ দিন থাকার নিয়ত না করা পর্যন্ত কসর; থাকার নিয়ত করলে পূর্ণ নামাজ। সুন্নত-নফল সম্ভব হলে যথারীতি। মাজহাবভেদে মতভেদ থাকায় ইমামের সাথে মিলিয়ে নিন।"
                }
            }
            lower.contains("qada") || lower.contains("কাজা") || lower.contains("কাযা") || lower.contains("qaza") ||
                (lower.contains("missed") && (lower.contains("prayer") || lower.contains("namaz") || lower.contains("salah"))) -> {
                if (isEng) {
                    "Missed (Qada) fard prayers remain a debt — make them up as soon as possible in proper form, starting with the earliest missed. Track them in NoorUp's Stats tab (Qada ledger). No fixed extra prayer cancels missed ones; accompany the makeup with sincere tawbah."
                } else {
                    "ছুটে যাওয়া (কাজা) ফরজ নামাজ ঋণ হিসেবে থাকে — যথাশীঘ্র সঠিক নিয়মে আদায় করুন, আগেরটা আগে। নূরআপের পরিসংখ্যান ট্যাবে কাজা হিসাব রাখতে পারেন। কাজার বিকল্প কোনো নির্দিষ্ট নামাজ নেই; আদায়ের সাথে আন্তরিক তওবা করুন।"
                }
            }
            (lower.contains("sunnah") || lower.contains("সুন্নত") || lower.contains("সুন্নাত") || lower.contains("nafl") || lower.contains("নফল") || lower.contains("নফল")) &&
                !lower.contains("fast") && !lower.contains("রোজা") && !lower.contains("রমজান") && !lower.contains("ramadan") -> {
                if (isEng) {
                    "Daily Sunnah Rawatib are 12 rak'ats: 2 before Fajr, 4 before + 2 after Dhuhr, 2 after Maghrib, 2 after Isha — whoever guards them gets a house in Paradise (hadith). Other beloved nafl: Tahajjud, Ishraq/Chasht after sunrise, and Tahiyyatul Masjid on entering a mosque."
                } else {
                    "দৈনিক সুন্নতে রাতেবা ১২ রাকাত: ফজরের আগে ২, যোহরের আগে ৪ ও পরে ২, মাগরিবের পরে ২, এশার পরে ২ — এগুলো রক্ষাকারীর জন্য জান্নাতে ঘর (হাদিস)। অন্যান্য প্রিয় নফল: তাহাজ্জুদ, সূর্যোদয়ের পর ইশরাক/চাশত এবং মসজিদে ঢুকে তাহিয়্যাতুল মসজিদ।"
                }
            }
            lower.contains("tahajjud") || lower.contains("তাহাজ্জুদ") || lower.contains("night prayer") -> {
                if (isEng) {
                    "Tahajjud (Qiyam al-Layl) is an immensely rewarding voluntary night prayer performed after waking from sleep during the last third of the night before Fajr. It usually consists of 2 to 8 rak'ats followed by Witr."
                } else {
                    "তাহাজ্জুদ নামাজ রাতের শেষ তৃতীয়াংশে ঘুমের পর আদায় করা অত্যন্ত বরকতপূর্ণ নফল ইবাদত। সাধারণত ২ রাকাত করে ৪ বা ৮ রাকাত এবং শেষে বিতর পড়া উত্তম। এই সময়ে বান্দার দোয়া সরাসরি কবুল হয়।"
                }
            }
            lower.contains("azan") || lower.contains("আজান") || lower.contains("আযান") || lower.contains("adhan") || lower.contains("iqamah") || lower.contains("ইকামত") || lower.contains("ইকামাহ") -> {
                if (isEng) {
                    "When you hear the Adhan, repeat its words silently (at Hayya alas-Salah and Hayya alal-Falah reply: La hawla wa la quwwata illa billah), then send salawat and recite the prescribed dua after Adhan; whoever does so earns the Prophet's (ﷺ) intercession (hadith). Iqamah adds Qad qamatis-Salah."
                } else {
                    "আজান শুনে নীরবে একই বাক্য পুনরাবৃত্তি করুন (হাইয়া আলাস-সালাহ ও হাইয়া আলাল-ফালাহ-তে 'লা হাওলা ওয়া লা কুওয়াতা ইল্লা বিল্লাহ' বলুন), তারপর দরুদ ও আজানের নির্ধারিত দোয়া পড়ুন; এতে রাসূল (সা.)-এর শাফায়াত লাভ হয় (হাদিস)। ইকামতে 'কাদ কামাতিস-সালাহ' যোগ হয়।"
                }
            }
            lower.contains("sahw") || lower.contains("সাহু") || lower.contains("সহু") -> {
                if (isEng) {
                    "Sajdah Sahw (prostration of forgetfulness) mends mistakes like missing a wajib act: at the end of prayer (after Tashahhud in Hanafi practice) salute once to the right, perform 2 prostrations, then complete Tashahhud, Darood, dua and salam to both sides. Rulings differ by school — ask your imam when unsure."
                } else {
                    "ভুলে ওয়াজিব ছুটে গেলে সাহু সিজদা: নামাজের শেষে (হানাফি আমলে তাশাহহুদের পর) ডানদিকে এক সালাম ফিরিয়ে ২টি সিজদা, তারপর তাশাহহুদ, দরুদ, দোয়া পড়ে দুদিকে সালাম। মাজহাবভেদে নিয়মে পার্থক্য আছে — সন্দেহে ইমামকে জিজ্ঞেস করুন।"
                }
            }
            lower.contains("istikhara") || lower.contains("ইস্তিখারা") || lower.contains("ইস্তেখারা") -> {
                if (isEng) {
                    "Istikhara seeks Allah's guidance for a decision: pray 2 rak'at nafl outside fard times, then recite the Istikhara dua naming your matter, trusting Allah with the outcome. No dream is required — proceed on the path Allah opens while consulting knowledgeable people."
                } else {
                    "কোনো সিদ্ধান্তে আল্লাহর দিকনির্দেশনা চাওয়াই ইস্তিখারা: ফরজ সময়ের বাইরে ২ রাকাত নফল পড়ে ইস্তিখারার দোয়ায় বিষয়টি উল্লেখ করে ফলাফল আল্লাহর উপর ছেড়ে দিন। স্বপ্ন দেখা জরুরি নয় — বিজ্ঞজনের পরামর্শসহ আল্লাহ যে পথ খুলে দেন সেদিকে অগ্রসর হোন।"
                }
            }
            lower.contains("prayer") || lower.contains("namaz") || lower.contains("নামাজ") || lower.contains("ওয়াক্ত") || lower.contains("ওয়াক্ত") -> {
                if (isEng) {
                    "Prayer (Salah) is the second pillar of Islam and a direct connection between the believer and Allah. The 5 daily obligatory prayers are Fajr, Dhuhr, Asr, Maghrib, and Isha. Remember to avoid performing prayers during the 3 Makruh times: Sunrise, Midday Zenith (Zawal), and Pre-Sunset."
                } else {
                    "নামাজ ইসলামের দ্বিতীয় স্তম্ভ এবং মুমিনের শ্রেষ্ঠ ইবাদত। দৈনিক পাঁচ ওয়াক্ত ফরজ নামাজ: ফজর, যোহর, আসর, মাগরিব ও এশা। নামাজের ৩টি নিষিদ্ধ (মাকরুহ) সময় এড়িয়ে চলুন: সূর্যোদয়, ঠিক দুপুর (দ্বিপ্রহর/জাওয়াল) এবং সূর্যাস্তের পূর্বের সময়।"
                }
            }
            // --- Fasting specifics (before the general fasting branch) ---
            lower.contains("sehri") || lower.contains("সেহরি") || lower.contains("সেহরী") || lower.contains("sahri") || lower.contains("suhoor") || lower.contains("iftar") || lower.contains("ইফতার") -> {
                if (isEng) {
                    "Finish Sehri before true dawn (the Fajr time in the app's prayer times) — delaying it toward the end of its time is Sunnah. Open Iftar immediately at sunset, preferably with dates and water. Sehri itself carries blessing (barakah), so try not to skip it."
                } else {
                    "সত্যিকারের ভোর (অ্যাপের ফজরের সময়) হওয়ার আগেই সেহরি শেষ করুন — শেষ সময় পর্যন্ত বিলম্ব করা সুন্নত। সূর্যাস্তের সাথে সাথে খেজুর-পানি দিয়ে ইফতার করুন। সেহরিতে বরকত আছে, তাই বাদ না দেওয়ার চেষ্টা করুন।"
                }
            }
            lower.contains("fidya") || lower.contains("fidyah") || lower.contains("ফিদিয়া") || lower.contains("kaffara") || lower.contains("kaffarah") || lower.contains("কাফফারা") ||
                (lower.contains("missed") && (lower.contains("fast") || lower.contains("রোজা"))) -> {
                if (isEng) {
                    "Fast missed with a valid excuse (travel, illness) is made up later (Qada). Fidya — feeding a poor person per missed day — applies to those permanently unable to fast (chronic illness, very old age). Deliberately breaking a Ramadan fast requires Kaffara. Confirm your own case with a scholar."
                } else {
                    "বৈধ ওজরে (সফর, অসুস্থতা) ছোটা রোজা পরে কাজা করুন। স্থায়ীভাবে অক্ষমদের (দীর্ঘমেয়াদি অসুস্থ, অতি বৃদ্ধ) জন্য ফিদিয়া — প্রতি রোজার বদলে একজন মিসকিনকে খাওয়ানো। ইচ্ছাকৃত রমজানের রোজা ভাঙলে কাফফারা ওয়াজিব হয়। নিজের অবস্থা আলেমের কাছে যাচাই করুন।"
                }
            }
            lower.contains("monday") || lower.contains("thursday") || lower.contains("সোম") || lower.contains("বৃহস্পতি") || lower.contains("আইয়ামে বীজ") || lower.contains("white days") || lower.contains("shawwal") || lower.contains("শাওয়াল") || lower.contains("আরাফা") || lower.contains("arafah") -> {
                if (isEng) {
                    "Beloved voluntary fasts: Mondays and Thursdays, the White Days (13th–15th of each lunar month), the 9th–10th of Muharram, the Day of Arafah (for non-pilgrims), and six days of Shawwal after Ramadan. Begin any nafl fast with Sehri intention before dawn."
                } else {
                    "প্রিয় নফল রোজা: সোম ও বৃহস্পতিবার, আইয়ামে বীজ (প্রতি চন্দ্রমাসের ১৩–১৫), মহররমের ৯–১০, আরাফার দিন (হাজি ছাড়া) এবং রমজানের পর শাওয়ালের ৬ রোজা। ভোরের আগে সেহরির নিয়তে শুরু করুন।"
                }
            }
            lower.contains("break") && (lower.contains("fast") || lower.contains("রোজা")) -> {
                if (isEng) {
                    "Deliberate eating, drinking, smoking, or marital relations during the day break the fast, as do menstruation and post-natal bleeding. Forgetful eating does not break it — complete the fast. Unintentional vomiting, tasting without swallowing, eye drops and miswak do not break it; treatment injections are debated, so ask a scholar."
                } else {
                    "ইচ্ছাকৃত খাওয়া-পান, ধূমপান বা দিনে সহবাসে রোজা ভাঙে; হায়েজ-নেফাসেও ভাঙে। ভুলে খেলে রোজা ভাঙে না — পূর্ণ করুন। অনিচ্ছাকৃত বমি, না গিলে স্বাদ নেওয়া, চোখের ড্রপ ও মিসওয়াকে ভাঙে না; চিকিৎসার ইনজেকশন নিয়ে মতভেদ আছে, আলেমকে জিজ্ঞেস করুন।"
                }
            }
            lower.contains("laylatul") || lower.contains("লাইলাতুল") || lower.contains("শবে কদর") || lower.contains("qadr night") || lower.contains("decree night") -> {
                if (isEng) {
                    "Laylatul Qadr (Night of Decree) is better than a thousand months (Quran 97:3) — seek it in the odd nights of Ramadan's last 10 days. Pray: Allahumma innaka 'afuwwun tuhibbul-'afwa fa'fu 'anni (O Allah, You love to pardon, so pardon me). Increase prayer, Quran, dua and charity on these nights."
                } else {
                    "লাইলাতুল কদর হাজার মাসের চেয়ে শ্রেষ্ঠ (কুরআন ৯৭:৩) — রমজানের শেষ ১০ দিনের বেজোড় রাতে তালাশ করুন। পড়ুন: আল্লাহুম্মা ইন্নাকা আফুউউন তুহিব্বুল আফওয়া ফা'ফু আন্নি। এই রাতগুলোতে নামাজ, কুরআন, দোয়া ও দান বাড়িয়ে দিন।"
                }
            }
            lower.contains("itikaf") || lower.contains("ইতিকাফ") || lower.contains("i'tikaf") -> {
                if (isEng) {
                    "Itikaf is secluding oneself in the mosque for worship, Sunnah in Ramadan's last 10 days seeking Laylatul Qadr: stay in the mosque with prayer, Quran and dhikr, leaving only for necessities. Many scholars say women observe it at their home prayer place — ask your imam for details."
                } else {
                    "ইতিকাফ মসজিদে নির্জনে ইবাদত; রমজানের শেষ ১০ দিনে লাইলাতুল কদরের আশায় সুন্নত: প্রয়োজন ছাড়া বের না হয়ে নামাজ, কুরআন ও যিকিরে থাকুন। অনেক আলেমের মতে নারীরা ঘরের নামাজের জায়গায় করবেন — বিস্তারিত ইমামের কাছে জেনে নিন।"
                }
            }
            lower.contains("আশুরা") || lower.contains("ashura") || lower.contains("মহররম") || lower.contains("muharram") || lower.contains("কারবালা") || lower.contains("karbala") -> {
                if (isEng) {
                    "The 10th of Muharram (Ashura), with the 9th, is a Sunnah fast expiating the previous year's sins (Sahih Muslim) — in gratitude for Allah saving Musa (AS). Give charity, and remember Karbala's sacrifice of Husayn (RA) with dignity, avoiding innovations."
                } else {
                    "মহররমের ১০ তারিখ (আশুরা) ৯ তারিখসহ সুন্নত রোজা — গত বছরের গুনাহের কাফফারা (সহীহ মুসলিম) — মূসা (আ.)-কে আল্লাহর রক্ষার কৃতজ্ঞতায়। দান করুন এবং হুসাইন (রা.)-এর কারবালার ত্যাগ মর্যাদায় স্মরণ করুন, বিদআত এড়িয়ে।"
                }
            }
            lower.contains("জিলহজ") || lower.contains("যিলহজ") || lower.contains("dhul") || lower.contains("zulhijjah") -> {
                if (isEng) {
                    "The first 10 days of Dhul-Hijjah are the best days for good deeds (hadith) — fast (especially Arafah day for non-pilgrims), recite takbeer, tahleel and tahmeed abundantly, and sacrifice Qurbani if able."
                } else {
                    "জিলহজের প্রথম ১০ দিন নেক আমলের শ্রেষ্ঠ দিন (হাদিস) — রোজা রাখুন (বিশেষত হাজি ছাড়া আরাফার দিন), বেশি বেশি তাকবীর-তাহলীল-তাহমীদ পড়ুন এবং সামর্থ্য থাকলে কুরবানি দিন।"
                }
            }
            lower.contains("roza") || lower.contains("fasting") || lower.contains("রোজা") || lower.contains("সিয়াম") || lower.contains("ramadan") || lower.contains("রমজান") -> {
                if (isEng) {
                    "Fasting (Sawm) during Ramadan is the fourth pillar of Islam. Sahri should be finished before Fajr dawn, and Iftar should be opened immediately after sunset with dates or water, reciting: 'Dhahaba adh-Dhama'u wabtallat al-'urooqu wa thabata al-ajru in sha Allah'."
                } else {
                    "রমজানের রোজা ইসলামের অন্যতম ফরজ বিধান। ফজরের আজানের পূর্বেই সাহরি শেষ করা এবং সূর্যাস্তের সাথে সাথে খেজুর ও পানি দিয়ে ইফতার করা সুন্নত। ইফতারের দোয়া: 'যাহাবাজ জামাউ ওয়াবতাল্লাতিল উরূকু ওয়া সাবাতাল আজরু ইনশাআল্লাহ'।"
                }
            }
            lower.contains("fitr") || lower.contains("ফিতর") || lower.contains("ফিৎরা") || lower.contains("fitrah") || lower.contains("ফিতরা") -> {
                if (isEng) {
                    "Zakat al-Fitr (Fitrah) purifies the fasting person and feeds the poor on Eid: due from every Muslim (the head of household pays for dependents) before the Eid prayer. The amount is one sa' (about 2.5–3 kg) of staple food or its cash value — confirm this year's official rate with your local masjid."
                } else {
                    "সদকাতুল ফিতর রোজাদারকে পরিশুদ্ধ করে ও ঈদের দিন গরিবকে খাওয়ায়: ঈদের নামাজের আগেই প্রত্যেক মুসলিমের পক্ষ থেকে (পরিবারের পক্ষে কর্তা) আদায় করতে হয়। পরিমাণ এক সা' (প্রায় ২.৫–৩ কেজি) প্রধান খাদ্য বা তার মূল্য — এ বছরের নির্ধারিত হার মসজিদ থেকে জেনে নিন।"
                }
            }
            lower.contains("zakat") || lower.contains("যাকাত") || lower.contains("যাকাত") -> {
                if (isEng) {
                    "Zakat (2.5%) is obligatory on Muslims possessing wealth above the Nisab threshold (approx. 7.5 tola gold or 52.5 tola silver equivalent) held for one full lunar year. You can use our built-in Zakat Calculator in the Tools section!"
                } else {
                    "যাকাত ইসলামের অন্যতম মৌলিক স্তম্ভ। নিসাব পরিমাণ সম্পদ (সাড়ে ৭ ভরি স্বর্ণ বা সাড়ে ৫২ ভরি রূপার সমপরিমাণ মূল্য) এক বছর স্থায়ী থাকলে উদ্বৃত্ত সম্পদের ২.৫% যাকাত প্রদান করা ফরজ। আমাদের 'সরঞ্জাম' ট্যাবে সহজে যাকাত হিসেব করতে পারেন।"
                }
            }
            // --- Hajj (before dua: "hajj dua" should still reach hajj) ---
            lower.contains("hajj") || lower.contains("হজ") || lower.contains("হজ্জ") -> {
                if (isEng) {
                    "Hajj, the fifth pillar, is obligatory once for every able Muslim (health, means and a safe route). Rites span 8th–13th Dhul-Hijjah: Ihram from Miqat, Tawaf, Sa'i, Mina, Arafah (the essence of Hajj), Muzdalifah, Rami, Qurbani, Halq/Taqsir, and farewell Tawaf. Study a full guide or travel with a learned group."
                } else {
                    "হজ পঞ্চম স্তম্ভ; সামর্থ্যবান (স্বাস্থ্য, অর্থ, নিরাপদ পথ) প্রত্যেক মুসলিমের উপর জীবনে একবার ফরজ। ৮–১৩ জিলহজ: মীকাত থেকে ইহরাম, তাওয়াফ, সাঈ, মিনা, আরাফা (হজের মূল), মুজদালিফা, রামি, কুরবানি, হলক/তাকসির ও বিদায়ী তাওয়াফ। পূর্ণ গাইড পড়ে বা অভিজ্ঞ দলের সাথে যান।"
                }
            }
            lower.contains("umrah") || lower.contains("ওমরাহ") || lower.contains("উমরাহ") || lower.contains("umra") || lower.contains("তাওয়াফ") || lower.contains("tawaf") || lower.contains("সাঈ") || lower.contains("sa'i") || lower.contains("saee") -> {
                if (isEng) {
                    "Umrah (lesser pilgrimage) can be done anytime: enter Ihram with intention and Talbiyah, perform Tawaf (7 rounds), pray 2 rak'ats at Maqam Ibrahim, do Sa'i between Safa and Marwah (7 trips), then shave or trim (Halq/Taqsir). Unlike Hajj it has no fixed days and omits the Arafah and Mina stays."
                } else {
                    "ওমরাহ যেকোনো সময় করা যায়: নিয়ত ও তালবিয়াসহ ইহরাম, তাওয়াফ (৭ চক্কর), মাকামে ইবরাহীমে ২ রাকাত, সাফা-মারওয়ায় সাঈ (৭ বার), তারপর মাথা মুণ্ডানো বা চুল ছাঁটা। হজের মতো নির্দিষ্ট দিন, আরাফা-মিনায় অবস্থান এতে নেই।"
                }
            }
            // --- Dua specifics (before the general dua branch) ---
            lower.contains("পিতা") || lower.contains("মাতা") || lower.contains("পিতামাতা") || lower.contains("parents") || lower.contains("mother") || lower.contains("father") || lower.contains("আম্মু") || lower.contains("আব্বু") || lower.contains("মায়ের") || lower.contains("বাবার") -> {
                if (isEng) {
                    "Honoring parents (Birr al-Walidayn) ranks among the greatest deeds after Salah: speak gently, serve them, and never say 'uff' to them (Quran 17:23). Pray for them: Rabbi-rhamhuma kama rabbayani sagheera — My Lord, have mercy upon them as they raised me when small (Quran 17:24)."
                } else {
                    "পিতামাতার সেবা (বিররুল ওয়ালিদাইন) নামাজের পর শ্রেষ্ঠ আমলগুলোর অন্যতম: নম্র কথা, সেবা, তাদের প্রতি 'উফ' পর্যন্ত নয় (কুরআন ১৭:২৩)। তাদের জন্য দোয়া করুন: রব্বিরহামহুমা কামা রব্বায়ানি সাগিরা — হে আমার রব, শৈশবে তারা যেমন আমাকে লালন করেছেন তেমনি তাদের প্রতি রহম করুন (কুরআন ১৭:২৪)।"
                }
            }
            lower.contains("sayyidul") || lower.contains("সাইয়েদুল") || lower.contains("সাইয়্যিদুল") || lower.contains("সায়্যিদুল") || lower.contains("sayyid") -> {
                if (isEng) {
                    "Sayyidul Istighfar (the chief of forgiveness-seeking) — full text in NoorUp's Dua tab. Whoever recites it with conviction in the morning or evening and dies that day or night enters Paradise (Sahih al-Bukhari 6306)."
                } else {
                    "সাইয়্যিদুল ইস্তিগফার (ক্ষমা প্রার্থনার সর্দার) — পূর্ণ পাঠ নূরআপের দুআ ট্যাবে। যে দৃঢ় বিশ্বাসে সকাল বা সন্ধ্যায় পড়ে সেদিন বা রাতে মারা গেলে জান্নাতে প্রবেশ করে (সহীহ বুখারী ৬৩০৬)।"
                }
            }
            lower.contains("সকাল-সন্ধ্যা") || lower.contains("সকাল সন্ধ্যা") || lower.contains("adhkar") || lower.contains("আজকার") || lower.contains("আযকার") ||
                ((lower.contains("সকাল") || lower.contains("সন্ধ্যা")) && (lower.contains("আমল") || lower.contains("দোয়া") || lower.contains("দোয়া") || lower.contains("যিকির") || lower.contains("যিকর") || lower.contains("dua"))) -> {
                if (isEng) {
                    "Morning-evening remembrances shield the day: Ayatul Kursi, Surah Ikhlas/Falaq/Nas (3x each), and abundant istighfar and salawat. Recite after Fajr and after Asr. Find them with transliteration in NoorUp's Dua tab."
                } else {
                    "সকাল-সন্ধ্যার যিকির দিনকে সুরক্ষিত রাখে: আয়াতুল কুরসি, সূরা ইখলাস/ফালাক/নাস (৩ বার করে), বেশি ইস্তিগফার ও দরুদ। ফজর ও আসরের পর পড়ুন। উচ্চারণসহ নূরআপের দুআ ট্যাবে পাবেন।"
                }
            }
            lower.contains("ঘুম") || lower.contains("sleep") || lower.contains("bedtime") || lower.contains("বিছানা") -> {
                if (isEng) {
                    "Before sleep: perform wudu, dust the bed, recite Ayatul Kursi and the last two verses of Baqarah, the three Quls wiping over the body, Surah Al-Mulk when possible, and sleep on the right side saying Bismika Allahumma amutu wa ahya."
                } else {
                    "ঘুমের আগে: ওযু, বিছানা ঝাড়া, আয়াতুল কুরসি ও বাকারার শেষ ২ আয়াত, তিন কুল পড়ে শরীরে মাসেহ, সম্ভব হলে সূরা মুলক এবং ডান কাতে 'বিসমিকা আল্লাহুম্মা আমুতু ওয়া আহইয়া' বলে ঘুমান।"
                }
            }
            (lower.contains("খাবার") || lower.contains("খাওয়া") || lower.contains("খাওয়ার") || lower.contains("খেতে") || lower.contains("food") || lower.contains("eat")) && !lower.contains("halal") && !lower.contains("হালাল") && !lower.contains("haram") && !lower.contains("হারাম") -> {
                if (isEng) {
                    "Before eating say Bismillah; if forgotten at the start, say Bismillahi awwalahu wa akhirahu. After eating praise Allah (Alhamdulillah). Eat with the right hand, together as a family — blessings gather over shared food."
                } else {
                    "খাওয়ার আগে বিসমিল্লাহ; শুরুতে ভুলে গেলে 'বিসমিল্লাহি আউওয়ালাহু ওয়া আখিরাহু' বলুন। শেষে আলহামদুলিল্লাহ বলুন। ডান হাতে, পরিবারের সাথে মিলে খান — একত্রে খাওয়ায় বরকত হয়।"
                }
            }
            (lower.contains("সফর") || lower.contains("travel")) && (lower.contains("দোয়া") || lower.contains("দোয়া") || lower.contains("dua")) -> {
                if (isEng) {
                    "When setting out, recite the travel dua: Subhanalladhi sakhkhara lana hadha... (Quran 43:13-14) — glory to Him who subjected this transport to us. The traveler's dua is answered, so remember Allah much on the way."
                } else {
                    "সফরে বেরিয়ে পড়ুন: সুবহানাল্লাযি সাখখারা লানা হাযা... (কুরআন ৪৩:১৩-১৪) — পবিত্র সেই সত্তা যিনি এই বাহন আমাদের অনুগত করেছেন। মুসাফিরের দোয়া কবুল হয়, তাই পথে বেশি যিকির করুন।"
                }
            }
            lower.contains("মসজিদ") || lower.contains("mosque") || lower.contains("masjid") -> {
                if (isEng) {
                    "Enter the mosque with the right foot, seeking refuge with Allah, and pray 2 rak'ats Tahiyyatul Masjid before sitting. Leave with the left foot, asking Allah's bounty. Keep the mosque calm, clean and phone-silent."
                } else {
                    "ডান পায়ে মসজিদে ঢুকে আল্লাহর আশ্রয় চান এবং বসার আগে ২ রাকাত তাহিয়্যাতুল মসজিদ পড়ুন। বাম পায়ে বেরিয়ে আল্লাহর অনুগ্রহ চান। মসজিদ শান্ত, পরিষ্কার ও ফোন নীরব রাখুন।"
                }
            }
            lower.contains("ঋণ") || lower.contains("debt") || lower.contains("কর্জ") || lower.contains("loan") || lower.contains("anxiety") || lower.contains("দুশ্চিন্তা") -> {
                if (isEng) {
                    "Overwhelmed by debt or anxiety? Recite the Prophet's (ﷺ) taught dua: Allahumma inni a'udhu bika minal-hammi wal-hazan... (full text in NoorUp's Dua tab). Combine dua with practical steps: list debts, cut expenses, seek halal income, and ask for rescheduling."
                } else {
                    "ঋণ বা দুশ্চিন্তায় ভারাক্রান্ত? রাসূল (সা.)-এর শেখানো দোয়া পড়ুন: আল্লাহুম্মা ইন্নি আউজু বিকা মিনাল হাম্মি ওয়াল হাযান... (পূর্ণ পাঠ নূরআপের দুআ ট্যাবে)। দোয়ার সাথে বাস্তব পদক্ষেপ: ঋণের তালিকা, খরচ কমানো, হালাল আয় এবং সময় চাওয়া।"
                }
            }
            lower.contains("তসবীহ") || lower.contains("তাসবীহ") || lower.contains("tasbih") || lower.contains("tasbeeh") || lower.contains("যিকির") || lower.contains("যিকর") || lower.contains("dhikr") || lower.contains("সুবহানাল্লাহ") || lower.contains("subhanallah") -> {
                if (isEng) {
                    "After each fard prayer say SubhanAllah, Alhamdulillah and Allahu Akbar 33 times each, completing 100 with La ilaha illallahu wahdahu... (hadith). Use NoorUp's Digital Tasbih in the Tools tab with any custom goal — the best dhikr is the consistent one."
                } else {
                    "প্রতি ফরজ নামাজের পর সুবহানাল্লাহ, আলহামদুলিল্লাহ ও আল্লাহু আকবার ৩৩ বার করে, শেষে 'লা ইলাহা ইল্লাল্লাহু ওয়াহদাহু...' দিয়ে ১০০ পূর্ণ করুন (হাদিস)। নূরআপের সরঞ্জাম ট্যাবে ডিজিটাল তসবীহে যেকোনো লক্ষ্যে গুনুন — ধারাবাহিক যিকিরই শ্রেষ্ঠ।"
                }
            }
            lower.contains("দরুদ") || lower.contains("দরূদ") || lower.contains("darood") || lower.contains("durood") || lower.contains("salawat") || lower.contains("সালাওয়াত") -> {
                if (isEng) {
                    "Sending salawat (Darood Ibrahim in prayer; abundant salawat on Fridays) brings Allah's blessings tenfold per recitation and nearness to the Prophet (ﷺ) (Quran 33:56). Make it a daily habit — e.g. 100 times morning and evening."
                } else {
                    "দরুদ পাঠে (নামাজে দরুদে ইবরাহীম; শুক্রবারে বেশি বেশি) প্রতি বারে আল্লাহর দশগুণ রহমত ও রাসূল (সা.)-এর নৈকট্য (কুরআন ৩৩:৫৬)। দৈনিক অভ্যাস করুন — যেমন সকাল-সন্ধ্যায় ১০০ বার।"
                }
            }
            lower.contains("dua") || lower.contains("দোয়া") || lower.contains("দোয়া") || lower.contains("munajat") || lower.contains("মোনাজাত") -> {
                if (isEng) {
                    "The Prophet (ﷺ) said: 'Dua is the essence of worship' (Tirmidhi). You can explore 18 categorized Masnoon Duas (morning-evening, protection, travel, food, parents, rizq, mosque, Jannah and more) with authentic references and transliterations directly in the 'Dua' tab of NoorUp."
                } else {
                    "রাসূলুল্লাহ (সা.) বলেছেন: 'দোয়াই হলো ইবাদতের মূল' (তিরমিযী)। নূরআপের 'দুআ' ট্যাবে সকাল-সন্ধ্যা, সুরক্ষা, সফর, খাবার, পিতা-মাতা, রিযিক, মসজিদ, জান্নাতসহ ১৮টি মাসনূন দোয়া উচ্চারণ ও অর্থসহ সংকলিত আছে।"
                }
            }
            // --- Quran specifics (before the general quran branch) ---
            (lower.contains("সিজদা") || lower.contains("সিজদাহ") || lower.contains("sajda") || lower.contains("sajdah") || lower.contains("prostration")) &&
                (lower.contains("তিলাওয়াত") || lower.contains("তেলাওয়াত") || lower.contains("tilawat") || lower.contains("recit")) -> {
                if (isEng) {
                    "There are 14 verses of prostration (Sajdah Tilawat) in the Quran: on reciting or hearing one, perform a single prostration with takbeer — in or outside prayer as applicable. Ask your imam for the list of verses and congregational details."
                } else {
                    "কুরআনে তিলাওয়াতে সিজদার আয়াত ১৪টি: পড়লে বা শুনলে তাকবিরসহ একটি সিজদা করুন — নামাজে বা বাইরে প্রযোজ্য নিয়মে। আয়াতের তালিকা ও জামাতের বিধান ইমামের কাছে জেনে নিন।"
                }
            }
            lower.contains("tajweed") || lower.contains("তাজবীদ") || lower.contains("তাজউইদ") || lower.contains("তিলাওয়াত") || lower.contains("তেলাওয়াত") || lower.contains("tilawat") -> {
                if (isEng) {
                    "Tajweed is reciting the Quran correctly and beautifully, slowly with contemplation (tartil — Quran 73:4). Start with correct pronunciation of letters (makharij), short daily portions with a teacher or verified audio, and constant revision. NoorUp's Focus mode helps verse-by-verse contemplation."
                } else {
                    "তাজবীদ হলো শুদ্ধ ও সুন্দরভাবে ধীরে ধীরে চিন্তাসহ তিলাওয়াত (তারতীল — কুরআন ৭৩:৪)। মাখরাজ শুদ্ধ করা দিয়ে শুরু করুন, শিক্ষক বা নির্ভরযোগ্য অডিওসহ প্রতিদিন অল্প অল্প, নিয়মিত পুনরাবৃত্তিসহ। নূরআপের ফোকাস মোড আয়াতভিত্তিক চিন্তায় সাহায্য করে।"
                }
            }
            lower.contains("হিফজ") || lower.contains("hifz") || lower.contains("hifdh") ||
                ((lower.contains("মুখস্থ") || lower.contains("মুখস্ত") || lower.contains("memoriz")) && (lower.contains("কুরআন") || lower.contains("quran") || lower.contains("সূরা") || lower.contains("সুরা") || lower.contains("surah"))) -> {
                if (isEng) {
                    "Memorizing the Quran: take small portions, understand the meaning, recite to a teacher daily, revise old portions constantly (morning is best), and pray with what you memorized. Consistency beats speed — even one verse a day completes the journey. Track memorized verses in NoorUp's Quran tab."
                } else {
                    "কুরআন হিফজ: অল্প অল্প অংশ নিন, অর্থ বুঝুন, প্রতিদিন শিক্ষককে শোনান, পুরনো অংশ নিয়মিত পুনরাবৃত্তি করুন (ভোর উত্তম) এবং মুখস্থ অংশ দিয়ে নামাজ পড়ুন। গতির চেয়ে ধারাবাহিকতা বড় — দিনে এক আয়াত হলেও লক্ষ্যে পৌঁছাবেন। নূরআপের কুরআন ট্যাবে মুখস্থ চিহ্নিত রাখুন।"
                }
            }
            lower.contains("আয়াতুল কুরসি") || lower.contains("আয়াতুল কুরসী") || lower.contains("ayatul kursi") || lower.contains("ayat al-kursi") -> {
                if (isEng) {
                    "Ayatul Kursi (Quran 2:255) is the greatest verse of the Quran, describing Allah's eternal life and boundless authority. Reciting it after every fard prayer, before sleep, and morning-evening carries immense protection and virtue according to hadith."
                } else {
                    "আয়াতুল কুরসি (কুরআন ২:২৫৫) সর্বশ্রেষ্ঠ আয়াত — আল্লাহর চিরঞ্জীবত্ব ও অসীম কর্তৃত্বের বর্ণনা। প্রতি ফরজ নামাজের পর, ঘুমের আগে ও সকাল-সন্ধ্যায় পড়ায় হাদিস অনুযায়ী বিরাট সুরক্ষা ও ফজিলত রয়েছে।"
                }
            }
            lower.contains("কাহফ") || lower.contains("kahf") -> {
                if (isEng) {
                    "Reciting Surah Al-Kahf (18) on Friday brings light between the two Fridays (hadith). Its first 10 verses protect against the trial of Dajjal (Sahih Muslim). Read it with meaning — its stories teach faith under trial."
                } else {
                    "শুক্রবারে সূরা কাহফ (১৮) পড়লে দুই জুমার মাঝে নূর হয় (হাদিস)। এর প্রথম ১০ আয়াত দাজ্জালের ফিতনা থেকে রক্ষা করে (সহীহ মুসলিম)। অর্থসহ পড়ুন — এর গল্পগুলো পরীক্ষায় ঈমান শেখায়।"
                }
            }
            lower.contains("মুলক") || lower.contains("মূলক") || lower.contains("mulk") -> {
                if (isEng) {
                    "Surah Al-Mulk (67) intercedes for its reciter and protects from the punishment of the grave according to hadith — make it a nightly habit before sleep. Its 30 verses invite reflection on Allah's dominion over life, death and provision."
                } else {
                    "সূরা মুলক (৬৭) পাঠকারীর জন্য সুপারিশ করে ও কবরের আজাব থেকে রক্ষা করে হাদিস অনুযায়ী — ঘুমের আগে নিয়মিত পড়ুন। এর ৩০ আয়াত জীবন-মৃত্যু ও রিজিকে আল্লাহর কর্তৃত্ব নিয়ে চিন্তা করায়।"
                }
            }
            lower.contains("ইয়াসিন") || lower.contains("ইয়াসীন") || lower.contains("yasin") || lower.contains("ya-sin") -> {
                if (isEng) {
                    "Surah Ya-Sin (36) is called the heart of the Quran in hadith — recite it seeking Allah's pleasure and forgiveness. Read it with translation: it affirms resurrection, prophethood and gratitude through powerful parables."
                } else {
                    "সূরা ইয়াসিন (৩৬) হাদিসে কুরআনের হৃদয় নামে পরিচিত — আল্লাহর সন্তুষ্টি ও ক্ষমা চেয়ে পড়ুন। অনুবাদসহ পড়ুন: পুনরুত্থান, নবুয়ত ও কৃতজ্ঞতা শক্তিশালী দৃষ্টান্তে প্রমাণিত হয়েছে।"
                }
            }
            (lower.contains("বাকারা") || lower.contains("baqarah")) && (lower.contains("শেষ") || lower.contains("last")) -> {
                if (isEng) {
                    "The last two verses of Surah Al-Baqarah (2:285-286) suffice whoever recites them at night (Sahih al-Bukhari and Sahih Muslim). They affirm faith in Allah, His angels, Books and Messengers, and end with the beautiful supplication of Rabbana."
                } else {
                    "সূরা বাকারার শেষ ২ আয়াত (২:২৮৫-২৮৬) রাতে পড়লে তা-ই যথেষ্ট হয় (সহীহ বুখারী ও সহীহ মুসলিম)। এতে আল্লাহ, ফেরেশতা, কিতাব ও রাসূলদের প্রতি ঈমান এবং 'রাব্বানা'র মর্মস্পর্শী দোয়া রয়েছে।"
                }
            }
            lower.contains("ইখলাস") || lower.contains("ikhlas") || lower.contains("ফালাক") || lower.contains("falaq") || lower.contains("muawwid") || lower.contains("মুআউয়িয") || lower.contains("মুআওবিয") ||
                lower.contains("সূরা নাস") || lower.contains("সুরা নাস") || lower.contains("surah nas") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "nas" || it == "নাস" } -> {
                if (isEng) {
                    "Surah Al-Ikhlas (112) equals one-third of the Quran in reward (Sahih al-Bukhari). Recite Ikhlas, Al-Falaq and An-Nas three times morning and evening for protection — the Prophet (ﷺ) did so regularly."
                } else {
                    "সূরা ইখলাস (১১২) সওয়াবে কুরআনের এক-তৃতীয়াংশের সমান (সহীহ বুখারী)। সকাল-সন্ধ্যায় ইখলাস, ফালাক ও নাস তিনবার করে পড়ুন — রাসূল (সা.) নিয়মিত এমন করতেন।"
                }
            }
            lower.contains("quran") || lower.contains("কুরআন") || lower.contains("surah") || lower.contains("সূরা") || lower.contains("সুরা") -> {
                if (isEng) {
                    "The Holy Quran is the final revelation from Allah sent as a guide for humanity. NoorUp contains all 114 surahs with complete verses (6,236 ayahs) — Arabic Uthmani text with Bangla & English translations, fully offline. Open any surah to read every verse, or use Focus mode for verse-by-verse contemplation."
                } else {
                    "পবিত্র কুরআন মানবজাতির জন্য হেদায়েতের আলোকবর্তিকা। নূরআপে ১১৪টি সূরার সম্পূর্ণ আয়াত (৬,২৩৬টি) — আরবি উসমানি পাঠসহ বাংলা ও ইংরেজি অনুবাদ, সম্পূর্ণ অফলাইনে আছে। যেকোনো সূরা খুলে প্রতিটি আয়াত পড়ুন, অথবা ফোকাস মোডে এক আয়াত করে তাদাব্বুর করুন।"
                }
            }
            // --- Beliefs, Seerah, manners & daily life (before qibla) ---
            lower.contains("pillar") || lower.contains("স্তম্ভ") -> {
                if (isEng) {
                    "The five pillars of Islam: (1) Shahada — testifying none is worthy of worship but Allah and Muhammad is His Messenger; (2) Salah — five daily prayers; (3) Zakat — obligatory charity; (4) Sawm — fasting Ramadan; (5) Hajj — pilgrimage for the able. Build knowledge and practice upon them step by step."
                } else {
                    "ইসলামের পাঁচ স্তম্ভ: (১) শাহাদা; (২) সালাত — পাঁচ ওয়াক্ত নামাজ; (৩) যাকাত; (৪) সাওম — রমজানের রোজা; (৫) হজ। এগুলো ভিত্তি; ধাপে ধাপে জ্ঞান ও আমল গড়ুন।"
                }
            }
            lower.contains("ঈমান") || lower.contains("iman") || lower.contains("faith") || lower.contains("belief") -> {
                if (isEng) {
                    "The six articles of Iman: belief in Allah, His angels, His Books, His Messengers, the Last Day, and divine decree (Qadr). Faith grows with obedience and weakens with sin; renew it with knowledge, worship and good company."
                } else {
                    "ঈমানের ছয় রুকন: আল্লাহ, ফেরেশতা, কিতাবসমূহ, রাসূলগণ, শেষ দিবস ও তাকদীরের প্রতি বিশ্বাস। আনুগত্যে ঈমান বাড়ে, পাপে কমে; জ্ঞান, ইবাদত ও সৎসঙ্গে নবায়ন করুন।"
                }
            }
            lower.contains("কালিমা") || lower.contains("kalima") || lower.contains("kalimah") || lower.contains("shahada") || lower.contains("শাহাদা") -> {
                if (isEng) {
                    "The Kalima Tayyiba — La ilaha illallah Muhammadur Rasulullah (none worthy of worship but Allah; Muhammad is Allah's Messenger) — is the gateway to Islam and the weightiest dhikr. Understand its meaning, live by it, and teach it to children first."
                } else {
                    "কালিমা তাইয়্যিবা — লা ইলাহা ইল্লাল্লাহু মুহাম্মাদুর রাসূলুল্লাহ — ইসলামের প্রবেশদ্বার ও শ্রেষ্ঠ যিকির। অর্থ বুঝুন, অনুযায়ী জীবন গড়ুন এবং শিশুদের প্রথমে এটাই শেখান।"
                }
            }
            lower.contains("আসমাউল") || lower.contains("আল্লাহর নাম") ||
                lower.contains("names of allah") || ((lower.contains("99") || lower.contains("৯৯")) && (lower.contains("নাম") || lower.contains("name"))) -> {
                if (isEng) {
                    "Allah has 99 beautiful names (Asmaul Husna) — Ar-Rahman, Ar-Rahim, Al-Malik and more; whoever learns and lives by them enters Paradise, according to hadith. Call upon Allah by these names in dua and reflect on one name each day."
                } else {
                    "আল্লাহর ৯৯টি সুন্দর নাম (আসমাউল হুসনা); হাদিস অনুযায়ী যে এগুলো শিখে আমল করে সে জান্নাতে প্রবেশ করে। দোয়ায় এই নামে ডাকুন এবং প্রতিদিন একটি নাম নিয়ে চিন্তা করুন।"
                }
            }
            lower.contains("ফেরেশতা") || lower.contains("angel") || lower.contains("জিবরাঈল") || lower.contains("জিবরাইল") || lower.contains("মিকাঈল") || lower.contains("ইসরাফিল") -> {
                if (isEng) {
                    "Angels are noble servants of Allah created from light: Jibril brought revelation, Mika'il oversees provision and rain, Israfil will blow the Trumpet, and the angel of death takes souls at their term. Kiraman-Katibin record every deed — live as though watched, for you are."
                } else {
                    "ফেরেশতারা নূরের তৈরি সম্মানিত বান্দা: জিবরাঈল (আ.) ওহি আনতেন, মিকাঈল (আ.) রিজিক-বৃষ্টির দায়িত্বে, ইসরাফিল (আ.) শিঙায় ফুঁ দেবেন এবং মালাকুল মাউত নির্ধারিত সময়ে জান কবজ করেন। কিরামান-কাতিবীন প্রতিটি আমল লেখেন — দেখা হচ্ছে ভেবে জীবন গড়ুন।"
                }
            }
            lower.contains("আখিরাত") || lower.contains("আখেরাত") || lower.contains("afterlife") || lower.contains("কিয়ামত") || lower.contains("কেয়ামত") || lower.contains("হাশর") || lower.contains("judgment day") || lower.contains("জান্নাত") || lower.contains("জাহান্নাম") || lower.contains("jannah") || lower.contains("jahannam") || lower.contains("paradise") || lower.contains("hellfire") -> {
                if (isEng) {
                    "Belief in the Last Day: after death comes the grave, then resurrection, the Scales (Mizan), crossing the Sirat, and eternal Paradise or Hell. This belief gives patience in hardship and restraint from sin — prepare with prayer, charity and repentance."
                } else {
                    "শেষ দিবসের বিশ্বাস: মৃত্যুর পর কবর, পুনরুত্থান, মীযান, সিরাত পার হওয়া এবং চিরস্থায়ী জান্নাত বা জাহান্নাম। এই বিশ্বাস কষ্টে ধৈর্য ও পাপ থেকে বিরত রাখে — নামাজ, দান ও তওবা দিয়ে প্রস্তুতি নিন।"
                }
            }
            lower.contains("তাকদীর") || lower.contains("তকদির") || lower.contains("destiny") || lower.contains("divine decree") || lower.contains("taqdeer") -> {
                if (isEng) {
                    "Qadr (divine decree) is the sixth article of faith: nothing happens outside Allah's knowledge and will, yet humans choose and are accountable. So tie your camel — work hard with lawful means — then trust (tawakkul) Allah for the outcome."
                } else {
                    "তাকদীর ঈমানের ষষ্ঠ রুকন: আল্লাহর জ্ঞান-ইচ্ছার বাইরে কিছুই ঘটে না, তবু মানুষ পছন্দ করে ও দায়ী থাকে। তাই উট বেঁধে রাখুন — হালাল উপায়ে কঠোর পরিশ্রম করুন, ফলের জন্য আল্লাহর উপর তাওয়াক্কুল করুন।"
                }
            }
            lower.contains("শিরক") || lower.contains("shirk") || lower.contains("polytheism") -> {
                if (isEng) {
                    "Shirk — associating partners with Allah in worship, love, or trust — is the gravest sin; Allah forgives all sins for whoever repents, but unrepented shirk is never forgiven (Quran 4:48). Direct all worship, dua and ultimate reliance to Allah alone."
                } else {
                    "শিরক — ইবাদত, ভালোবাসা বা ভরসায় আল্লাহর সাথে শরিক করা — সবচেয়ে বড় পাপ; তওবাকারীর সব গুনাহ আল্লাহ ক্ষমা করেন, কিন্তু তওবা ছাড়া শিরক ক্ষমা হয় না (কুরআন ৪:৪৮)। সব ইবাদত, দোয়া ও চূড়ান্ত ভরসা কেবল আল্লাহর জন্য।"
                }
            }
            lower.contains("তওবা") || lower.contains("তাওবা") || lower.contains("tawba") || lower.contains("taubah") || lower.contains("repent") -> {
                if (isEng) {
                    "Tawbah is always open before death: stop the sin at once, feel remorse, resolve never to return, and restore others' rights if violated. Allah forgives ALL sins of the repentant — 'despair not of Allah's mercy' (Quran 39:53). Follow a bad deed quickly with a good one."
                } else {
                    "মৃত্যুর আগে তওবার দরজা সবসময় খোলা: সাথে সাথে পাপ বন্ধ, অনুতাপ, ফিরে না যাওয়ার সংকল্প এবং বান্দার হক থাকলে তা ফেরত। তওবাকারীর সব গুনাহ আল্লাহ ক্ষমা করেন — 'আল্লাহর রহমত থেকে নিরাশ হয়ো না' (কুরআন ৩৯:৫৩)। মন্দের পর দ্রুত ভালো আমল করুন।"
                }
            }
            lower.contains("নবী") || lower.contains("রাসূল") || lower.contains("prophet") || lower.contains("মুহাম্মদ") || lower.contains("muhammad") || lower.contains("সীরাত") || lower.contains("সিরাত") || lower.contains("seerah") || lower.contains("seerat") -> {
                if (isEng) {
                    "Prophet Muhammad (ﷺ), born in Makkah around 570 CE, received the first revelation at 40 (610 CE), invited to Islam for 23 years, migrated to Madinah (622 CE), and passed away in 11 AH (632 CE). His life — mercy, honesty, courage and worship — is the living Quran; study the Seerah regularly."
                } else {
                    "মুহাম্মদ (সা.) মক্কায় প্রায় ৫৭০ খ্রিস্টাব্দে জন্মগ্রহণ করেন, ৪০ বছর বয়সে (৬১০) প্রথম ওহি পান, ২৩ বছর দাওয়াত দেন, মদিনায় হিজরত করেন (৬২২) এবং ১১ হিজরিতে (৬৩২) ইন্তেকাল করেন। তাঁর জীবন — দয়া, সততা, সাহস ও ইবাদত — জীবন্ত কুরআন; নিয়মিত সীরাত পড়ুন।"
                }
            }
            lower.contains("খলিফা") || lower.contains("খোলাফা") || lower.contains("caliph") || lower.contains("আবু বকর") || lower.contains("উমর") || lower.contains("উসমান") || lower.contains("আলী") -> {
                if (isEng) {
                    "The Rightly Guided Caliphs in order: Abu Bakr as-Siddiq (11–13 AH), Umar al-Faruq (13–23 AH), Uthman Dhun-Nurayn (23–35 AH), and Ali al-Murtada (35–40 AH). Loving all four and learning from their justice and sacrifice is the way of Ahl as-Sunnah."
                } else {
                    "খোলাফায়ে রাশেদা ক্রমানুসারে: আবু বকর সিদ্দিক (১১–১৩ হি.), উমর ফারুক (১৩–২৩ হি.), উসমান জুন-নূরাইন (২৩–৩৫ হি.) ও আলী মুরতাজা (৩৫–৪০ হি.)। চারজনকে ভালোবাসা-সম্মান এবং তাঁদের ন্যায় ও ত্যাগ থেকে শিক্ষা আহলে সুন্নতের পথ।"
                }
            }
            lower.contains("হিজরত") || lower.contains("hijrah") || lower.contains("hijra") || lower.contains("মদিনা") || lower.contains("madinah") || lower.contains("medina") -> {
                if (isEng) {
                    "The Hijrah (622 CE) — the Prophet's (ﷺ) migration from Makkah to Madinah with Abu Bakr — marks year 1 of the Islamic calendar. In Madinah he built the mosque, united Muhajirun and Ansar as brothers, and established the first Muslim community."
                } else {
                    "হিজরত (৬২২ খ্রি.) — আবু বকর (রা.)-সহ মক্কা থেকে মদিনায় রাসূল (সা.)-এর গমন — ইসলামি বর্ষপঞ্জির প্রথম বছর। মদিনায় তিনি মসজিদ নির্মাণ করেন, মুহাজির-আনসারকে ভাই বানান এবং প্রথম মুসলিম সমাজ প্রতিষ্ঠা করেন।"
                }
            }
            lower.contains("বদর") || lower.contains("badr") || lower.contains("উহুদ") || lower.contains("uhud") || lower.contains("খন্দক") || lower.contains("khandaq") -> {
                if (isEng) {
                    "Early decisive events: Badr (2 AH) — 313 ill-equipped believers triumphed by Allah's help; Uhud (3 AH) taught obedience to the Prophet's (ﷺ) command; Khandaq (5 AH) showed patience and planning. The lesson: victory comes with faith, unity and obedience, not numbers."
                } else {
                    "প্রাথমিক নির্ণায়ক ঘটনা: বদর (২ হি.) — ৩১৩ জন অপ্রতুল মুমিন আল্লাহর সাহায্যে জয়ী; ওহুদ (৩ হি.) রাসূল (সা.)-এর নির্দেশ মানার শিক্ষা দিল; খন্দক (৫ হি.) ধৈর্য ও পরিকল্পনা শেখাল। শিক্ষা: সংখ্যায় নয়, ঈমান-ঐক্য-আনুগত্যে বিজয়।"
                }
            }
            lower.contains("মক্কা বিজয়") || lower.contains("মক্কাবিজয়") || lower.contains("conquest of makkah") || lower.contains("fath makkah") -> {
                if (isEng) {
                    "The Conquest of Makkah (8 AH/630 CE) was a bloodless triumph of mercy: the Prophet (ﷺ) forgave his former persecutors — 'no blame upon you today' — and cleansed the Kaaba of idols, declaring truth has come and falsehood has vanished."
                } else {
                    "মক্কা বিজয় (৮ হি./৬৩০ খ্রি.) ছিল দয়ার রক্তপাতহীন বিজয়: রাসূল (সা.) সাবেক নির্যাতকদের ক্ষমা করলেন — 'আজ তোমাদের উপর কোনো দোষ নেই' — এবং কাবা থেকে মূর্তি সরিয়ে ঘোষণা দিলেন সত্য এসেছে, মিথ্যা বিলুপ্ত হয়েছে।"
                }
            }
            lower.contains("প্রতিবেশী") || lower.contains("প্রতিবেশি") || lower.contains("neighbor") || lower.contains("neighbour") -> {
                if (isEng) {
                    "Jibril kept advising the Prophet (ﷺ) about neighbors until he thought they would inherit (Bukhari & Muslim). Share food, visit the sick, avoid noise and harm, and help in need — regardless of their faith. Good neighborliness is a branch of faith."
                } else {
                    "জিবরাঈল (আ.) প্রতিবেশী সম্পর্কে এত تاکید করতেন যে রাসূল (সা.) ভাবলেন তারা উত্তরাধিকার পাবে (বুখারী ও মুসলিম)। খাবার ভাগ করুন, অসুস্থকে দেখতে যান, শব্দ-কষ্ট দেওয়া থেকে বিরত থাকুন, প্রয়োজনে সাহায্য করুন — ধর্ম নির্বিশেষে। সদ্ব্যবহার ঈমানের শাখা।"
                }
            }
            lower.contains("সত্য") || lower.contains("সততা") || lower.contains("মিথ্যা") || lower.contains("honest") || lower.contains("truth") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "lie" || it == "lies" } -> {
                if (isEng) {
                    "Truthfulness (Sidq) leads to righteousness and Paradise; lying leads to wickedness and Hell (Bukhari & Muslim). Speak truth even against yourself, keep promises and trusts (amanah), and avoid exaggeration and false excuses."
                } else {
                    "সত্যবাদিতা (সিদক) পুণ্য ও জান্নাতের পথে নেয়; মিথ্যা পাপ ও জাহান্নামের পথে (বুখারী ও মুসলিম)। নিজের বিরুদ্ধে গেলেও সত্য বলুন, ওয়াদা-আমানত রক্ষা করুন, অতিরঞ্জন-মিথ্যা অজুহাত এড়িয়ে চলুন।"
                }
            }
            lower.contains("রাগ") || lower.contains("anger") || lower.contains("angry") -> {
                if (isEng) {
                    "Anger is from Shaytan: when angry, seek refuge (A'udhu billahi minash-shaytanir-rajim), stay silent, change posture (sit if standing, lie down if sitting), and perform wudu — for anger is fire, extinguished by water (hadith). Forgiving while able earns the highest ranks."
                } else {
                    "রাগ শয়তানের পক্ষ থেকে: রাগে 'আউজু বিল্লাহি মিনাশ শাইতানির রাজিম' পড়ুন, চুপ থাকুন, অবস্থা বদলান (দাঁড়িয়ে থাকলে বসুন, বসে থাকলে শুয়ে পড়ুন) এবং ওযু করুন — রাগ আগুন, পানিতে নেভে (হাদিস)। ক্ষমতা থাকতেও ক্ষমা সর্বোচ্চ মর্যাদা আনে।"
                }
            }
            lower.contains("গীবত") || lower.contains("গিবত") || lower.contains("backbit") || lower.contains("gossip") -> {
                if (isEng) {
                    "Backbiting (Gheebah) — mentioning your brother in his absence what he dislikes — is like eating his dead brother's flesh (Quran 49:12), even if true; if untrue it is slander. Guard the tongue: change the topic, defend the absent, and repent."
                } else {
                    "গীবত — ভাইয়ের অনুপস্থিতিতে তার অপছন্দের কথা বলা — মৃত ভাইয়ের গোশত খাওয়ার মতো (কুরআন ৪৯:১২), সত্য হলেও; মিথ্যা হলে তা অপবাদ। জিহ্বা সংযত রাখুন: প্রসঙ্গ বদলান, অনুপস্থিতের পক্ষ নিন এবং তওবা করুন।"
                }
            }
            lower.contains("সদকা") || lower.contains("সাদকা") || lower.contains("sadaqah") || lower.contains("charity") || lower.contains("donate") || lower.contains("দানশীল") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "দান" } -> {
                if (isEng) {
                    "Sadaqah never decreases wealth (Sahih Muslim): give secretly and openly — even a smile or kind word counts as charity. Regular small giving beats rare large amounts: sponsor an orphan, feed the hungry, or support a student's learning."
                } else {
                    "সদকায় সম্পদ কমে না (সহীহ মুসলিম): গোপনে-প্রকাশ্যে দিন, হাসিমুখ ও ভালো কথাও সদকা। মাঝে মাঝে বড় অঙ্কের চেয়ে নিয়মিত অল্প দান উত্তম — এতিমের দায়িত্ব, ক্ষুধার্তকে খাওয়ানো বা শিক্ষার্থীর পড়ায় সাহায্য করুন।"
                }
            }
            lower.contains("রিজিক") || lower.contains("রিযিক") || lower.contains("রুজি") || lower.contains("rizq") || lower.contains("উপার্জন") || lower.contains("হালাল রোজগার") || lower.contains("halal income") -> {
                if (isEng) {
                    "Seek halal rizq: the honest, trustworthy merchant will be with the martyrs (hadith). Work with excellence and honesty, avoid interest (riba), cheating and haram trades, pay workers promptly, and make dua for barakah — gratitude increases provision."
                } else {
                    "হালাল রিজিক তালাশ করুন: সৎ-আমানতদার ব্যবসায়ী শহীদদের সাথে থাকবে (হাদিস)। নৈপুণ্য-সততায় কাজ করুন, সুদ, প্রতারণা ও হারাম ব্যবসা এড়ান, শ্রমিকের মজুরি দ্রুত দিন এবং বরকতের দোয়া করুন — কৃতজ্ঞতায় রিজিক বাড়ে।"
                }
            }
            lower.contains("বিয়ে") || lower.contains("বিবাহ") || lower.contains("marriage") || lower.contains("nikah") || lower.contains("নিকাহ") || lower.contains("মোহর") || lower.contains("mahr") || lower.contains("wedding") -> {
                if (isEng) {
                    "Nikah (marriage) completes half one's deen (hadith): choose piety over looks and wealth, fix an agreed mahr honoring the bride, keep the ceremony simple and free of haram, and build the home on mercy and consultation (Quran 30:21)."
                } else {
                    "নিকাহ দ্বীনের অর্ধেক পূর্ণ করে (হাদিস): রূপ-সম্পদের চেয়ে দ্বীনদারি দেখুন, সম্মত মোহর নির্ধারণ করে কনেকে সম্মান দিন, অনুষ্ঠান সহজ ও হারামমুক্ত রাখুন এবং দয়া-পরামর্শে সংসার গড়ুন (কুরআন ৩০:২১)।"
                }
            }
            lower.contains("সন্তান") || lower.contains("children") || lower.contains("parenting") || lower.contains("বাচ্চা") || lower.contains("শিশু") -> {
                if (isEng) {
                    "Children are a trust (amanah): teach them Salah from age seven (hadith), model what you preach, discipline with mercy never in anger, make dua for them, and teach Quran, honesty and gratitude early. A righteous child's dua benefits parents after death."
                } else {
                    "সন্তান আমানত: সাত বছর থেকে নামাজ শেখান (হাদিস), যা বলেন নিজে করে দেখান, রাগে নয় দয়ার সাথে শাসন করুন, তাদের জন্য দোয়া করুন এবং ছোটবেলা থেকে কুরআন, সততা ও কৃতজ্ঞতা শেখান। নেক সন্তানের দোয়া মৃত্যুর পরও পিতামাতার কাজে আসে।"
                }
            }
            lower.contains("বদনজর") || lower.contains("evil eye") || lower.contains("ruqyah") || lower.contains("রুক") || lower.contains("যাদু") || lower.contains("magic") || lower.contains("জ্বিন") || lower.contains("jinn") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "জিন" || it == "jinn" } -> {
                if (isEng) {
                    "For evil eye, magic or distress, use authentic Ruqyah: Surah Al-Fatihah, Ayatul Kursi, and the three Quls morning-evening — the Prophet (ﷺ) approved ruqyah free of shirk (Sahih Muslim). Avoid amulets and fortune-tellers; trust Allah with dua, charity and morning-evening adhkar."
                } else {
                    "বদনজর, যাদু বা অস্থিরতায় প্রামাণ্য রুকইয়াহ: সূরা ফাতিহা, আয়াতুল কুরসি ও তিন কুল সকাল-সন্ধ্যায় — শিরকমুক্ত রুকইয়াহতে রাসূল (সা.) অনুমতি দিয়েছেন (সহীহ মুসলিম)। তাবিজ-গণক এড়িয়ে দোয়া, দান ও সকাল-সন্ধ্যার যিকিরে আল্লাহর উপর ভরসা রাখুন।"
                }
            }
            lower.contains("স্বপ্ন") || lower.contains("dream") -> {
                if (isEng) {
                    "Good dreams are glad tidings from Allah — thank Him and share with loved ones. Bad dreams are from Shaytan: spit lightly to the left three times, seek refuge, do not narrate them, and pray (hadith). No dream overrides Quran and Sunnah."
                } else {
                    "ভালো স্বপ্ন আল্লাহর সুসংবাদ — শুকরিয়া আদায় করে প্রিয়জনকে বলুন। খারাপ স্বপ্ন শয়তানের পক্ষ থেকে: বামে তিনবার হালকা থুথু, আউজু বিল্লাহ, কাউকে না বলা এবং নামাজ (হাদিস)। কোনো স্বপ্ন কুরআন-সুন্নাহর ঊর্ধ্বে নয়।"
                }
            }
            // --- Sacred months note: Ashura & Dhul-Hijjah live with the fasting
            // specifics above (their queries contain fasting words). ---
            (lower.contains("হারাম") || lower.contains("haram") || lower.contains("শূকর") || lower.contains("pork") || lower.contains("alcohol") || lower.contains("শরাব") || lower.contains("liquor") || lower.contains("মদ্যপান") ||
                lower.split(Regex("[\\s,.!?;]+")).any { it == "মদ" }) ||
                (lower.contains("হালাল") && (lower.contains("খাবার") || lower.contains("food") || lower.contains("খাওয়া") || lower.contains("eat"))) -> {
                if (isEng) {
                    "Eat halal and pure (tayyib): carrion, blood, pork and intoxicants are forbidden (Quran 2:173, 5:90). Say Bismillah, eat with the right hand, avoid waste and doubtful matters. Alcohol destroys prayer, health and family — never trade, serve or sit where it flows."
                } else {
                    "হালাল-পবিত্র (তাইয়্যিব) খান: মৃত, রক্ত, শূকর ও নেশাদ্রব্য হারাম (কুরআন ২:১৭৩, ৫:৯০)। বিসমিল্লাহ বলে ডান হাতে খান, অপচয় ও সন্দেহজনক বিষয় এড়ান। মদ নামাজ, স্বাস্থ্য ও পরিবার ধ্বংস করে — এর ব্যবসা-পরিবেশন বা আসরে বসা থেকে দূরে থাকুন।"
                }
            }
            lower.contains("হিজাব") || lower.contains("hijab") || lower.contains("পর্দা") || lower.contains("porda") || lower.contains("modest dress") -> {
                if (isEng) {
                    "Modest dress (hijab/parda) guards dignity: Allah commands believing men and women to lower their gaze and guard modesty, and women to draw their coverings over themselves (Quran 24:30-31). Dress simply, avoid extravagance and indecency."
                } else {
                    "শালীন পোশাক (হিজাব/পর্দা) মর্যাদা রক্ষা করে: আল্লাহ মুমিন নারী-পুরুষকে দৃষ্টি নত ও লজ্জাস্থান হেফাজতের এবং নারীদের ওড়না টেনে নেওয়ার নির্দেশ দিয়েছেন (কুরআন ২৪:৩০-৩১)। সাদাসিধে পোশাক পরুন, অপচয়-অশ্লীলতা এড়ান।"
                }
            }
            lower.contains("qibla") || lower.contains("কিবলা") || lower.contains("compass") || lower.contains("kaaba") || lower.contains("কাবা") || lower.contains("mecca") || lower.contains("মক্কা") || lower.contains("makkah") -> {
                if (isEng) {
                    "The Qibla is the direction towards the Kaaba in Mecca (approx. 278° WNW from Bangladesh). Use our live Qibla Compass tool with real-time sensor calibration in the Tools menu."
                } else {
                    "কিবলা হলো মক্কার পবিত্র কাবা শরীফের অভিমুখ (বাংলাদেশ থেকে আনুমানিক ২৭৮° পশ্চিম-উত্তর-পশ্চিম)। সঠিক কিবলা জানতে 'সরঞ্জাম' ট্যাবের ডিজিটাল কিবলা কম্পাস ব্যবহার করুন।"
                }
            }
            else -> genericLocalReply(isEng)
        }
    }

    // --- 1. Interactive "Noor Garden" Habit Growth & Performance Calculation Engine ---
    companion object {
        val MANDATORY_PRAYERS = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা")
        val ALL_QADA_PRAYERS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Witr")
        const val MAX_TASBIH_GOAL = 100000
        private const val AI_HISTORY_MESSAGES = 8
        private const val MIGRATION_CLEAN_MOCK_V4 = "migration_zero_state_clean_v4"
    }

    private fun cleanLegacyMockDataIfPresent() {
        if (!prefs.getBoolean(MIGRATION_CLEAN_MOCK_V4, false)) {
            val editor = prefs.edit()
            editor.remove("prayer_habit_initialized_v2")
            editor.remove("prayer_habit_initialized")
            val allKeys = prefs.all.keys.toList()
            for (key in allKeys) {
                if (key.startsWith("prayer_habit_") || key.startsWith("prayer_record_") || key.startsWith("qada_count_") || key.startsWith("user_has_logged_") || key == "prayer_tracking_start_date") {
                    editor.remove(key)
                }
            }
            editor.putBoolean(MIGRATION_CLEAN_MOCK_V4, true)
            editor.apply()
        }
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getDateKeyForDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun getDayLabel(daysAgo: Int): String {
        val isEng = _isEnglish.value
        return when (daysAgo) {
            0 -> if (isEng) "Today / আজ" else "আজ / Today"
            1 -> if (isEng) "Yesterday / গতকাল" else "গতকাল / Yesterday"
            else -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                SimpleDateFormat("dd MMM", Locale.US).format(cal.time)
            }
        }
    }

    private fun dateKeyFor(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    private fun loadHabitPrayers(dateKey: String): Map<String, Boolean> {
        cleanLegacyMockDataIfPresent()
        // Strictly loads what user explicitly checked. Default: unchecked (0 completed, 0%).
        return MANDATORY_PRAYERS.associateWith { prayer ->
            prefs.getBoolean("prayer_habit_${dateKey}_$prayer", false)
        }
    }

    /** A continuous week makes gaps visible and lets users fill in any recent date. */
    private fun computeConsistencyHistory(habitMap: Map<String, Boolean> = _habitPrayers.value): List<DailyRecord> {
        val historyList = mutableListOf<DailyRecord>()
        val todayKey = getTodayDateKey()
        val todayCompleted = habitMap.values.count { it }
        val hasLoggedToday = prefs.getBoolean("user_has_logged_$todayKey", false) || todayCompleted > 0

        // 1. Today's Record: ONLY included if user has explicitly logged/interacted today
        if (hasLoggedToday) {
            val todayTotal = 5
            val todayPercent = (todayCompleted * 100) / todayTotal
            val todayStreak = (todayCompleted == todayTotal)
            historyList.add(
                DailyRecord(
                    dateKey = todayKey,
                    dayLabel = getDayLabel(0),
                    completedCount = todayCompleted,
                    totalCount = todayTotal,
                    scorePercent = todayPercent,
                    isStreakMaintained = todayStreak
                )
            )
        }

        // 2. Past days: ONLY include past dates where user explicitly logged prayer entries.
        // No fake mock baseline numbers! Empty history if no past user records exist.
        for (i in 1..30) {
            val pastKey = getDateKeyForDaysAgo(i)
            if (prefs.getBoolean("user_has_logged_$pastKey", false) && prefs.contains("prayer_record_${pastKey}_completed")) {
                val pastCompleted = prefs.getInt("prayer_record_${pastKey}_completed", 0)
                val pastPercent = (pastCompleted * 100) / 5
                val pastStreak = pastCompleted == 5
                historyList.add(
                    DailyRecord(
                        dateKey = pastKey,
                        dayLabel = getDayLabel(i),
                        completedCount = pastCompleted,
                        totalCount = 5,
                        scorePercent = pastPercent,
                        isStreakMaintained = pastStreak
                    )
                )
            }
        }

        return historyList
    }

    private fun computeCurrentStreak(): Int {
        val todayCompleted = loadHabitPrayers(getTodayDateKey()).values.count { it }
        var streak = 0
        val startIndex: Int
        if (todayCompleted == 5) {
            streak = 1
            startIndex = 1
        } else {
            // Keep streak alive during the current day if yesterday was completed
            val yesterdayKey = getDateKeyForDaysAgo(1)
            val yesterdayCompleted = prefs.getInt("prayer_record_${yesterdayKey}_completed", 0)
            if (yesterdayCompleted == 5) {
                streak = 1
                startIndex = 2
            } else {
                return 0
            }
        }

        for (i in startIndex..365) {
            val pastKey = getDateKeyForDaysAgo(i)
            val pastCompleted = prefs.getInt("prayer_record_${pastKey}_completed", 0)
            if (pastCompleted == 5) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun computePendingMissedDays(): List<MissedDayRecord> {
        val prayerMapping = listOf(
            "ফজর" to "Fajr",
            "যোহর" to "Dhuhr",
            "আসর" to "Asr",
            "মাগরিব" to "Maghrib",
            "এশা" to "Isha"
        )
        val list = mutableListOf<MissedDayRecord>()
        for (i in 1..7) {
            val pastKey = getDateKeyForDaysAgo(i)
            val alreadyTransferred = prefs.getBoolean("prayer_transferred_qada_$pastKey", false)
            if (alreadyTransferred) continue

            val hasLogged = prefs.getBoolean("user_has_logged_$pastKey", false)
            val missedBn = mutableListOf<String>()
            val missedEn = mutableListOf<String>()
            for ((bn, en) in prayerMapping) {
                val completed = prefs.getBoolean("prayer_habit_${pastKey}_$bn", false)
                if (!completed) {
                    missedBn.add(bn)
                    missedEn.add(en)
                }
            }
            if (missedBn.isNotEmpty() && hasLogged) {
                list.add(
                    MissedDayRecord(
                        dateKey = pastKey,
                        daysAgo = i,
                        dayLabel = getDayLabel(i),
                        missedPrayersBn = missedBn,
                        missedPrayersEn = missedEn,
                        hasLogged = hasLogged
                    )
                )
            }
        }
        return list
    }

    /**
     * The first day a user checks a prayer becomes the tracking start date. We only
     * count Qada after that point, so dates before they began logging are never
     * treated as missed prayers.
     */
    private fun syncAutomaticQadaLedger() {
        val startKey = prefs.getString("prayer_tracking_start_date", null) ?: return
        val todayKey = getTodayDateKey()
        val automaticCounts = ALL_QADA_PRAYERS.associateWith { 0 }.toMutableMap()
        if (startKey < todayKey) {
            val calendar = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startKey) ?: return
            }
            while (dateKeyFor(calendar) < todayKey) {
                val dateKey = dateKeyFor(calendar)
                MANDATORY_PRAYERS.forEachIndexed { index, prayer ->
                    if (!prefs.getBoolean("prayer_habit_${dateKey}_$prayer", false)) {
                        val qadaPrayer = ALL_QADA_PRAYERS[index]
                        automaticCounts[qadaPrayer] = (automaticCounts[qadaPrayer] ?: 0) + 1
                        if (prayer == "এশা") {
                            automaticCounts["Witr"] = (automaticCounts["Witr"] ?: 0) + 1
                        }
                    }
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Keep any user-entered Qada adjustment separate from the automatic count.
        // This stops a manually completed Qada from reappearing after a refresh.
        val displayedCounts = ALL_QADA_PRAYERS.associateWith { prayer ->
            val manualKey = "qada_manual_offset_$prayer"
            val manualOffset = if (prefs.contains(manualKey)) {
                prefs.getInt(manualKey, 0)
            } else {
                prefs.getInt("qada_count_$prayer", 0)
            }
            maxOf(0, (automaticCounts[prayer] ?: 0) + manualOffset)
        }
        _qadaCounts.value = displayedCounts
        prefs.edit().apply {
            ALL_QADA_PRAYERS.forEach { prayer ->
                val manualKey = "qada_manual_offset_$prayer"
                if (!prefs.contains(manualKey)) putInt(manualKey, prefs.getInt("qada_count_$prayer", 0))
                putInt("qada_auto_count_$prayer", automaticCounts[prayer] ?: 0)
                putInt("qada_count_$prayer", displayedCounts[prayer] ?: 0)
            }
            apply()
        }
    }

    private var habitDateKey: String = getTodayDateKey()

    private fun loadHabitsForSelectedDate() {
        habitDateKey = dateKeyFor(_selectedCalendarDate.value)
        _habitPrayers.value = loadHabitPrayers(habitDateKey)
    }

    /** Selects a timeline date so its prayer checks can be corrected in Noor Garden. */
    fun openGardenForDate(dateKey: String) {
        val calendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: Date()
        }
        setCalendarDate(calendar)
    }

    /** Midnight rollover: if process survived past 00:00, yesterday's checks must not render as today's. */
    fun ensureTodayRollover() {
        val today = getTodayDateKey()
        if (dateKeyFor(_selectedCalendarDate.value) == today && habitDateKey != today) {
            habitDateKey = today
            val fresh = loadHabitPrayers(today)
            _habitPrayers.value = fresh
            _consistencyHistory.value = computeConsistencyHistory()
            _currentStreak.value = computeCurrentStreak()
            _pendingMissedDays.value = computePendingMissedDays()
        }
    }

    private val _habitPrayers = MutableStateFlow(loadHabitPrayers(getTodayDateKey()))
    val habitPrayers: StateFlow<Map<String, Boolean>> = _habitPrayers.asStateFlow()

    private val _consistencyHistory = MutableStateFlow(computeConsistencyHistory())
    val consistencyHistory: StateFlow<List<DailyRecord>> = _consistencyHistory.asStateFlow()

    private val _currentStreak = MutableStateFlow(computeCurrentStreak())
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _pendingMissedDays = MutableStateFlow(computePendingMissedDays())
    val pendingMissedDays: StateFlow<List<MissedDayRecord>> = _pendingMissedDays.asStateFlow()

    fun toggleHabitPrayer(prayerName: String) {
        ensureTodayRollover()
        val selectedDateKey = habitDateKey
        val updatedMap = _habitPrayers.value.toMutableMap().apply {
            val newVal = !(this[prayerName] ?: false)
            this[prayerName] = newVal
            prefs.edit().putBoolean("prayer_habit_${selectedDateKey}_$prayerName", newVal).apply()
        }
        _habitPrayers.value = updatedMap

        val completedCount = updatedMap.values.count { it }
        val newStreak = computeCurrentStreak()
        prefs.edit()
            .putInt("prayer_record_${selectedDateKey}_completed", completedCount)
            .putBoolean("user_has_logged_$selectedDateKey", true)
            .putString("prayer_habit_last_active_date", selectedDateKey)
            .putInt("current_prayer_streak", newStreak)
            .apply()

        if (!prefs.contains("prayer_tracking_start_date")) {
            prefs.edit().putString("prayer_tracking_start_date", selectedDateKey).apply()
        }

        _currentStreak.value = newStreak
        _consistencyHistory.value = computeConsistencyHistory(updatedMap)
        syncAutomaticQadaLedger()
        _pendingMissedDays.value = computePendingMissedDays()

        // Synchronize all home screen widgets instantly
        try {
            NoorUpWidgetProvider.updateAllWidgets(getApplication())
        } catch (e: Exception) {
            Log.e("NoorUpViewModel", "Failed updating widget after habit toggle", e)
        }
    }

    fun refreshConsistencyHistory() {
        ensureTodayRollover()
        _consistencyHistory.value = computeConsistencyHistory()
        _currentStreak.value = computeCurrentStreak()
        syncAutomaticQadaLedger()
        _pendingMissedDays.value = computePendingMissedDays()
        _totalMadeUpQada.value = prefs.getInt("qada_made_up_total", 0)
    }

    fun transferMissedPrayersToQada(dateKey: String) {
        val prayerMapping = listOf(
            "ফজর" to "Fajr",
            "যোহর" to "Dhuhr",
            "আসর" to "Asr",
            "মাগরিব" to "Maghrib",
            "এশা" to "Isha"
        )
        val alreadyTransferred = prefs.getBoolean("prayer_transferred_qada_$dateKey", false)
        if (alreadyTransferred) return

        val missedList = mutableListOf<String>()
        for ((bn, en) in prayerMapping) {
            val completed = prefs.getBoolean("prayer_habit_${dateKey}_$bn", false)
            if (!completed) {
                missedList.add(en)
                if (bn == "এশা") {
                    missedList.add("Witr")
                }
            }
        }

        if (missedList.isNotEmpty()) {
            val cur = _qadaCounts.value.toMutableMap()
            for (missed in missedList) {
                val updated = (cur[missed] ?: 0) + 1
                cur[missed] = updated
                prefs.edit().putInt("qada_count_$missed", updated).apply()
            }
            _qadaCounts.value = cur
        }
        prefs.edit().putBoolean("prayer_transferred_qada_$dateKey", true).apply()
        _pendingMissedDays.value = computePendingMissedDays()
    }

    // --- 2. Ambient Quran Focus Mode (verse-by-verse tadabbur, functional) ---
    private val _isAmbientFocusActive = MutableStateFlow(false)
    val isAmbientFocusActive: StateFlow<Boolean> = _isAmbientFocusActive.asStateFlow()

    private val _focusSurahNumber = MutableStateFlow(prefs.getInt("focus_surah", 1).coerceIn(1, 114))
    val focusSurahNumber: StateFlow<Int> = _focusSurahNumber.asStateFlow()

    private val _focusVerseNumber = MutableStateFlow(prefs.getInt("focus_verse", 1).coerceAtLeast(1))
    val focusVerseNumber: StateFlow<Int> = _focusVerseNumber.asStateFlow()

    private val _focusVerse = MutableStateFlow<Verse?>(null)
    val focusVerse: StateFlow<Verse?> = _focusVerse.asStateFlow()

    private val _focusFontScale = MutableStateFlow(prefs.getFloat("focus_font_scale", 1f).coerceIn(0.8f, 1.6f))
    val focusFontScale: StateFlow<Float> = _focusFontScale.asStateFlow()

    private val _focusShowTranslation = MutableStateFlow(prefs.getBoolean("focus_show_translation", true))
    val focusShowTranslation: StateFlow<Boolean> = _focusShowTranslation.asStateFlow()

    private val _focusSessionCount = MutableStateFlow(0)
    val focusSessionCount: StateFlow<Int> = _focusSessionCount.asStateFlow()

    /** Canonical Ayat count for a Surah, from the single Quran data source. */
    private fun focusVerseCount(surahNumber: Int): Int =
        com.example.ui.noorup.quran.QuranRepository.getSurahMeta(surahNumber)?.totalVerses ?: 0

    private fun resolveFocusVerse(): Verse? {
        val surah = _surahs.value.find { it.number == _focusSurahNumber.value } ?: return null
        val total = focusVerseCount(_focusSurahNumber.value)
        if (total <= 0) return null
        val clamped = _focusVerseNumber.value.coerceIn(1, total)
        if (clamped != _focusVerseNumber.value) _focusVerseNumber.value = clamped
        return surah.verses.find { it.number == clamped }
    }

    private fun persistFocusPosition() {
        prefs.edit()
            .putInt("focus_surah", _focusSurahNumber.value)
            .putInt("focus_verse", _focusVerseNumber.value)
            .apply()
    }

    /** Enter focus on a specific surah/verse (defaults to last position). Loads authentic text if needed. */
    fun enterFocus(surahNumber: Int? = null, verseNumber: Int? = null, resetSession: Boolean = true) {
        if (surahNumber != null) _focusSurahNumber.value = surahNumber.coerceIn(1, 114)
        val requestedSurah = _focusSurahNumber.value
        val total = focusVerseCount(requestedSurah)
        if (total <= 0) return
        // An explicit Surah + Ayat always wins over the last persisted position;
        // only a bare enterFocus() (session resume) reuses the saved position.
        _focusVerseNumber.value = (verseNumber ?: _focusVerseNumber.value).coerceIn(1, total)
        val requestedVerse = _focusVerseNumber.value
        // Never show unverified content: null renders the loading indicator
        // until the authentic bundled verses land (or an error is reported).
        _focusVerse.value = null
        _quranLoadError.value = false
        _isFocusLoading.value = true
        if (resetSession) _focusSessionCount.value = 0
        _isAmbientFocusActive.value = true
        persistFocusPosition()
        // Ensure authentic offline text is loaded for this surah (cancel stale loads).
        focusLoadJob?.cancel()
        focusLoadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>().applicationContext
                val full = com.example.ui.noorup.quran.QuranRepository.loadFullSurah(ctx, requestedSurah)
                if (_focusSurahNumber.value != requestedSurah) return@launch
                if (full.isNotEmpty() && com.example.ui.noorup.quran.QuranRepository.isAuthenticVerseList(full, total)) {
                    val enriched = full.map { v ->
                        v.copy(
                            isBookmarked = prefs.getBoolean("bm_${requestedSurah}_${v.number}", v.isBookmarked),
                            isMemorized = prefs.getBoolean("mem_${requestedSurah}_${v.number}", v.isMemorized)
                        )
                    }
                    if (_focusSurahNumber.value != requestedSurah) return@launch
                    val cur = _surahs.value
                    _surahs.value = cur.map { s -> if (s.number == requestedSurah) s.copy(verses = enriched) else s }
                    if (_focusSurahNumber.value == requestedSurah && _focusVerseNumber.value == requestedVerse) {
                        _focusVerse.value = enriched.find { it.number == requestedVerse }
                    }
                    if (_focusSurahNumber.value == requestedSurah) _quranLoadError.value = false
                } else {
                    // Missing, partial, or invalid bundle: keep the spinner
                    // replaced by an error + retry instead of fabricated text.
                    if (_focusSurahNumber.value == requestedSurah) _quranLoadError.value = true
                }
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "enterFocus load failed", e)
                if (_focusSurahNumber.value == requestedSurah) _quranLoadError.value = true
            } finally {
                if (_focusSurahNumber.value == requestedSurah) _isFocusLoading.value = false
            }
        }
    }

    /**
     * Enter Focus from the reader for a normally opened Surah. Always starts at
     * Ayat 1 so a bookmark or a previous session position can never leak in.
     */
    fun enterFocusAtSurahStart(surahNumber: Int) {
        enterFocus(surahNumber, 1)
    }

    /** Enter Focus from a saved bookmark at its exact Surah + Ayat. */
    fun enterFocusAtBookmark(surahNumber: Int, verseNumber: Int) {
        enterFocus(surahNumber, verseNumber)
    }

    /** Retry the current focus verse load after a failure. */
    fun retryFocusLoad() {
        enterFocus(_focusSurahNumber.value, _focusVerseNumber.value, resetSession = false)
    }

    fun toggleAmbientFocus() {
        if (_isAmbientFocusActive.value) exitFocus() else enterFocus()
    }

    fun exitFocus() {
        _isAmbientFocusActive.value = false
        persistFocusPosition()
    }

    fun focusNextVerse() {
        val total = focusVerseCount(_focusSurahNumber.value)
        if (total <= 0) return
        if (_focusVerseNumber.value < total) {
            _focusVerseNumber.value += 1
            _focusVerse.value = resolveFocusVerse()
            _focusSessionCount.value += 1
            persistFocusPosition()
            // Surah not loaded yet: ensure a load is in flight so the verse
            // resolves instead of leaving the spinner stuck.
            if (_focusVerse.value == null) enterFocus(_focusSurahNumber.value, _focusVerseNumber.value, resetSession = false)
        } else if (_focusSurahNumber.value < 114) {
            // Auto-advance to next surah without wiping the just-incremented count.
            val nextSurah = _focusSurahNumber.value + 1
            _focusSurahNumber.value = nextSurah
            _focusVerseNumber.value = 1
            _focusVerse.value = resolveFocusVerse()
            _focusSessionCount.value += 1
            persistFocusPosition()
            enterFocus(nextSurah, 1, resetSession = false)
        }
    }

    fun focusPrevVerse() {
        if (_focusVerseNumber.value > 1) {
            _focusVerseNumber.value -= 1
            _focusVerse.value = resolveFocusVerse()
            persistFocusPosition()
            if (_focusVerse.value == null) enterFocus(_focusSurahNumber.value, _focusVerseNumber.value, resetSession = false)
        } else if (_focusSurahNumber.value > 1) {
            val prevSurah = _focusSurahNumber.value - 1
            val total = focusVerseCount(prevSurah)
            if (total <= 0) return
            _focusSurahNumber.value = prevSurah
            _focusVerseNumber.value = total
            _focusVerse.value = resolveFocusVerse()
            persistFocusPosition()
            // Moving back across a Surah boundary is navigation, not a new
            // session, so the read count and timer keep running.
            enterFocus(prevSurah, total, resetSession = false)
        }
    }

    /** Explicit Ayat navigation inside Focus; refuses anything outside the Surah. */
    fun focusJumpTo(surahNumber: Int, verseNumber: Int): Boolean {
        val total = focusVerseCount(surahNumber)
        if (total <= 0 || verseNumber !in 1..total) return false
        enterFocus(surahNumber, verseNumber, resetSession = false)
        return true
    }

    // --- "Go to Ayat" input validation (shared by the UI and unit tests) ---
    enum class AyatInputError { EMPTY, NOT_A_WHOLE_NUMBER, OUT_OF_RANGE }

    private fun normalizeDigits(input: String): String =
        input.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")

    /**
     * Validates raw "Go to Ayat" input against the selected Surah's canonical
     * Ayat count and navigates there when valid. Empty, negative, decimal,
     * non-numeric and out-of-range values are rejected, never silently clamped.
     *
     * @return null when navigation happened, otherwise the validation error.
     */
    fun goToAyat(surahNumber: Int, input: String): AyatInputError? =
        when (val parsed = parseAyatInput(surahNumber, input)) {
            is AyatParse.Ok -> {
                enterFocus(surahNumber, parsed.number, resetSession = false)
                null
            }
            is AyatParse.Failed -> parsed.error
        }

    /**
     * Validated Ayat jump inside the reader. Scrolls the already-open Surah to
     * the requested Ayat, or opens it there when it is not the current one.
     *
     * @return null when navigation happened, otherwise the validation error.
     */
    fun goToAyatInReader(surahNumber: Int, input: String): AyatInputError? =
        when (val parsed = parseAyatInput(surahNumber, input)) {
            is AyatParse.Ok -> {
                if (_selectedSurahNumber.value != surahNumber) {
                    openSurahAt(surahNumber, parsed.number)
                } else {
                    _pendingReaderAyat.value = parsed.number
                    persistReaderPosition(surahNumber, parsed.number)
                }
                null
            }
            is AyatParse.Failed -> parsed.error
        }

    private sealed interface AyatParse {
        data class Ok(val number: Int) : AyatParse
        data class Failed(val error: AyatInputError) : AyatParse
    }

    private fun parseAyatInput(surahNumber: Int, input: String): AyatParse {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return AyatParse.Failed(AyatInputError.EMPTY)
        if (trimmed.any { it !in '0'..'9' && it !in '০'..'৯' }) {
            return AyatParse.Failed(AyatInputError.NOT_A_WHOLE_NUMBER)
        }
        val number = normalizeDigits(trimmed).toIntOrNull()
            ?: return AyatParse.Failed(AyatInputError.OUT_OF_RANGE)
        val total = focusVerseCount(surahNumber)
        if (total <= 0 || number !in 1..total) {
            return AyatParse.Failed(AyatInputError.OUT_OF_RANGE)
        }
        return AyatParse.Ok(number)
    }

    fun cycleFocusFontScale() {
        val next = when {
            _focusFontScale.value < 1.0f -> 1.2f
            _focusFontScale.value < 1.3f -> 1.5f
            else -> 1.0f
        }
        _focusFontScale.value = next
        prefs.edit().putFloat("focus_font_scale", next).apply()
    }

    fun toggleFocusTranslation() {
        _focusShowTranslation.value = !_focusShowTranslation.value
        prefs.edit().putBoolean("focus_show_translation", _focusShowTranslation.value).apply()
    }

    // --- 3. Smart Travel & Qasr Prayer Mode State (persisted) ---
    private val _isTraveling = MutableStateFlow(prefs.getBoolean("is_traveling", false))
    val isTraveling: StateFlow<Boolean> = _isTraveling.asStateFlow()

    fun toggleTravelMode() {
        val next = !_isTraveling.value
        _isTraveling.value = next
        prefs.edit().putBoolean("is_traveling", next).apply()
    }

    // --- Prayer Consistency & Qada Analytics State ---
    private fun loadQadaCounts(): Map<String, Int> {
        cleanLegacyMockDataIfPresent()
        return ALL_QADA_PRAYERS.associateWith { prayer ->
            maxOf(0, prefs.getInt("qada_count_$prayer", 0))
        }
    }

    private val _qadaCounts = MutableStateFlow(loadQadaCounts())
    val qadaCounts: StateFlow<Map<String, Int>> = _qadaCounts.asStateFlow()

    private val _totalMadeUpQada = MutableStateFlow(prefs.getInt("qada_made_up_total", 0))
    val totalMadeUpQada: StateFlow<Int> = _totalMadeUpQada.asStateFlow()

    /** Records a Qada prayer as fulfilled (user prayed the missed prayer). */
    fun fulfillQada(prayer: String) {
        updateQada(prayer, -1)
    }

    /** Records an additional missed Qada prayer (user missed another prayer). */
    fun recordMissedQada(prayer: String) {
        updateQada(prayer, 1)
    }

    fun updateQada(prayer: String, delta: Int) {
        val current = _qadaCounts.value
        val old = current[prayer] ?: 0

        if (delta < 0) {
            // User wants to mark Qada as fulfilled (decrease pending)
            val toFulfill = minOf(old, -delta)
            if (toFulfill <= 0) {
                // Pending Qada is already zero: do not allow negative count or false fulfillment
                return
            }
            val updated = old - toFulfill
            val newMap = current.toMutableMap().apply { this[prayer] = updated }
            _qadaCounts.value = newMap

            val automatic = prefs.getInt("qada_auto_count_$prayer", 0)
            val manualOffset = updated - automatic
            prefs.edit()
                .putInt("qada_count_$prayer", updated)
                .putInt("qada_manual_offset_$prayer", manualOffset)
                .apply()

            // Update fulfilled counts
            val prayerMadeUp = prefs.getInt("qada_made_up_$prayer", 0) + toFulfill
            val totalMadeUp = prefs.getInt("qada_made_up_total", 0) + toFulfill
            prefs.edit()
                .putInt("qada_made_up_$prayer", prayerMadeUp)
                .putInt("qada_made_up_total", totalMadeUp)
                .apply()
            _totalMadeUpQada.value = totalMadeUp
        } else if (delta > 0) {
            // User wants to record additional missed prayer (increase pending) or reverse a previous fulfillment
            val updated = old + delta
            val newMap = current.toMutableMap().apply { this[prayer] = updated }
            _qadaCounts.value = newMap

            val automatic = prefs.getInt("qada_auto_count_$prayer", 0)
            val manualOffset = updated - automatic
            prefs.edit()
                .putInt("qada_count_$prayer", updated)
                .putInt("qada_manual_offset_$prayer", manualOffset)
                .apply()

            // If user was reversing a fulfillment for this prayer, adjust fulfilled count
            val prayerMadeUp = prefs.getInt("qada_made_up_$prayer", 0)
            if (prayerMadeUp > 0) {
                val toReverse = minOf(prayerMadeUp, delta)
                val newPrayerMadeUp = prayerMadeUp - toReverse
                val currentTotalMadeUp = prefs.getInt("qada_made_up_total", 0)
                val newTotalMadeUp = maxOf(0, currentTotalMadeUp - toReverse)
                prefs.edit()
                    .putInt("qada_made_up_$prayer", newPrayerMadeUp)
                    .putInt("qada_made_up_total", newTotalMadeUp)
                    .apply()
                _totalMadeUpQada.value = newTotalMadeUp
            }
        }
        refreshConsistencyHistory()
    }

    // --- Prayer Alarm Onboarding State ---
    // "Complete" means the user finished the Home flow AND the alarms were
    // verified as armed. Passive startup re-scheduling must never set it, or a
    // brand-new install would look already-configured and never see the banner.
    private val _isPrayerAlarmOnboardingComplete = MutableStateFlow(
        prefs.getBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, false)
    )
    val isPrayerAlarmOnboardingComplete: StateFlow<Boolean> = _isPrayerAlarmOnboardingComplete.asStateFlow()

    private val _isPrayerAlarmPromoDismissed = MutableStateFlow(
        prefs.getBoolean(PrayerAlarmOnboarding.KEY_PROMO_DISMISSED, false)
    )
    val isPrayerAlarmPromoDismissed: StateFlow<Boolean> = _isPrayerAlarmPromoDismissed.asStateFlow()

    private val _isPrayerAlarmNotificationDenied = MutableStateFlow(
        prefs.getBoolean(PrayerAlarmOnboarding.KEY_NOTIFICATION_DENIED, false)
    )
    val isPrayerAlarmNotificationDenied: StateFlow<Boolean> = _isPrayerAlarmNotificationDenied.asStateFlow()

    /** "Later": drop the full banner but keep a compact retry row on Home. */
    fun dismissPrayerAlarmPromo() {
        _isPrayerAlarmPromoDismissed.value = true
        prefs.edit().putBoolean(PrayerAlarmOnboarding.KEY_PROMO_DISMISSED, true).apply()
    }

    /** Remember a denial so the retry routes to Settings instead of re-prompting. */
    fun notePrayerAlarmNotificationDenied() {
        _isPrayerAlarmNotificationDenied.value = true
        prefs.edit().putBoolean(PrayerAlarmOnboarding.KEY_NOTIFICATION_DENIED, true).apply()
    }

    fun clearPrayerAlarmNotificationDenied() {
        _isPrayerAlarmNotificationDenied.value = false
        prefs.edit().putBoolean(PrayerAlarmOnboarding.KEY_NOTIFICATION_DENIED, false).apply()
    }

    private fun completePrayerAlarmOnboarding() {
        _isPrayerAlarmOnboardingComplete.value = true
        _isPrayerAlarmPromoDismissed.value = false
        _isPrayerAlarmNotificationDenied.value = false
        prefs.edit()
            .putBoolean(PrayerAlarmOnboarding.KEY_COMPLETE, true)
            .putBoolean(PrayerAlarmOnboarding.KEY_PROMO_DISMISSED, false)
            .putBoolean(PrayerAlarmOnboarding.KEY_NOTIFICATION_DENIED, false)
            .apply()
    }

    /**
     * Home-screen onboarding: the user explicitly asked for prayer alarms.
     * Enables the existing reminder preference, reuses the existing scheduler and
     * only reports success once alarms are actually armed in the future.
     */
    fun enablePrayerAlarms(context: Context, onResult: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        // Re-check the platform gates: the UI state can lag a trip to Settings.
        if (!PrayerAlarmOnboarding.notificationsGranted(appContext)) {
            notePrayerAlarmNotificationDenied()
            onResult(false)
            return
        }
        if (!PrayerAlarmOnboarding.exactAlarmAccessGranted(appContext)) {
            onResult(false)
            return
        }
        // Explicit opt-in: turn the master reminder preference on. Individual
        // prayer toggles and the user's notification preferences stay untouched.
        _isNotificationEnabled.value = true
        prefs.edit().putBoolean("prayer_notifications_enabled", true).apply()
        viewModelScope.launch(Dispatchers.IO) {
            val verified = try {
                schedulePrayerAlarmsBlocking(appContext)
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "enablePrayerAlarms scheduling failed", e)
                false
            }
            if (!verified) {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }
            // Daily garden/hadith reminders were cancelled by a prior disable. Use
            // the app context: this coroutine can outlive the UI's context.
            try { PrayerNotificationScheduler.scheduleDailyTasks(appContext) } catch (_: Exception) { }
            withContext(Dispatchers.Main) {
                completePrayerAlarmOnboarding()
                onResult(true)
            }
        }
    }

    /** Schedules the prayer alarms synchronously on the caller's thread and verifies them. */
    private fun schedulePrayerAlarmsBlocking(appContext: Context): Boolean {
        val loc = _selectedCityLocation.value
        PrayerNotificationScheduler.scheduleAllPrayerAlerts(
            context = appContext,
            latitude = loc.latitude,
            longitude = loc.longitude,
            method = _calculationMethod.value,
            juristic = _juristicMethod.value,
            isEnglish = _isEnglish.value,
            zoneId = loc.zoneId
        )
        return PrayerNotificationScheduler.hasVerifiedScheduledAlarms(appContext)
    }

    // --- Native Local Prayer Notifications State & Management ---
    private val _isNotificationEnabled = MutableStateFlow(
        prefs.getBoolean("prayer_notifications_enabled", true)
    )
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    // Per-prayer toggles (Fajr/Dhuhr/Asr/Maghrib/Isha). Sehri follows Fajr, Iftar follows Maghrib.
    private val _perPrayerEnabled = MutableStateFlow(loadPerPrayerEnabled())
    val perPrayerEnabled: StateFlow<Map<String, Boolean>> = _perPrayerEnabled.asStateFlow()

    private fun loadPerPrayerEnabled(): Map<String, Boolean> {
        val keys = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        return keys.associateWith { prefs.getBoolean("prayer_enabled_$it", true) }
    }

    fun setPrayerEnabled(key: String, enabled: Boolean, context: Context) {
        prefs.edit().putBoolean("prayer_enabled_$key", enabled).apply()
        _perPrayerEnabled.value = _perPrayerEnabled.value.toMutableMap().apply { this[key] = enabled }
        if (_isNotificationEnabled.value) schedulePrayerNotifications(context, { /* onComplete */ })
    }

    /** Sehri-end follows the Fajr alert and Iftar follows Maghrib; turning one on also enables alerts overall. */
    fun setRamadanAlert(prayerKey: String, enabled: Boolean, context: Context) {
        if (enabled && !_isNotificationEnabled.value) toggleNotifications(true, context)
        setPrayerEnabled(prayerKey, enabled, context)
    }

    fun toggleNotifications(enabled: Boolean, context: Context) {
        _isNotificationEnabled.value = enabled
        prefs.edit().putBoolean("prayer_notifications_enabled", enabled).apply()
        if (enabled) {
            val appContext = context.applicationContext
            // User-initiated from the alarm screen: successful scheduling also
            // finishes onboarding, so returning users are not re-prompted.
            viewModelScope.launch(Dispatchers.IO) {
                try { PrayerNotificationScheduler.scheduleDailyTasks(appContext) } catch (_: Exception) { }
                val verified = try {
                    schedulePrayerAlarmsBlocking(appContext)
                } catch (e: Exception) {
                    Log.e("NoorUpViewModel", "toggleNotifications scheduling failed", e)
                    false
                }
                if (verified) withContext(Dispatchers.Main) { completePrayerAlarmOnboarding() }
            }
        } else {
            PrayerNotificationScheduler.cancelAllPrayerAlerts(context)
        }
    }

    fun schedulePrayerNotifications(context: Context, onComplete: (() -> Unit)? = null) {
        // Guard: LaunchedEffect(Unit) fires on every onCreate (rotation/theme) —
        // never re-arm after the user disabled alerts.
        if (!prefs.getBoolean("prayer_notifications_enabled", true)) return
        if (!_isNotificationEnabled.value) return
        // AlarmManager setAlarmClock x5 is slow IPC — never block the UI thread.
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Passive re-arm only: completing onboarding here would hide the
                // Home prompt on a fresh install (default preference is enabled).
                schedulePrayerAlarmsBlocking(appContext)
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "schedulePrayerNotifications failed", e)
            } finally {
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }
        }
    }

    fun sendTestNotification(context: Context) {
        PrayerNotificationScheduler.sendInstantTestNotification(context, _isEnglish.value)
    }

    fun scheduleExactTestAlarm(context: Context, seconds: Int = 5) {
        PrayerNotificationScheduler.scheduleExactTestAlarmInSeconds(context, seconds, _isEnglish.value)
    }

    fun isExactAlarmReady(context: Context): Boolean {
        return PrayerNotificationScheduler.isExactAlarmPermissionGranted(context)
    }

    fun triggerGardenReminderTest(context: Context) {
        PrayerNotificationScheduler.triggerGardenReminderNowForTesting(context, _isEnglish.value)
    }

    fun triggerHadithReminderTest(context: Context) {
        PrayerNotificationScheduler.triggerDailyHadithNowForTesting(context, _isEnglish.value)
    }

    /** Prune unbounded daily keys (~8/day → ~2900/yr): keep last 90 days. */
    private fun pruneOldDailyKeys() {
        try {
            val all = prefs.all.keys.toList()
            val cutoff = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -90) }.timeInMillis
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val editor = prefs.edit()
            var removed = 0
            // Orphaned keys of the removed Family Zikr feature: nothing reads them
            // anymore, so drop them once instead of carrying fake data forever.
            // (Garden/hadith reminder dates are NOT here — still live guards.)
            for (dead in listOf(
                "family_members_data_json",
                "family_seed_purge_v1",
                "my_family_pairing_code"
            )) {
                if (prefs.contains(dead)) {
                    editor.remove(dead)
                    removed++
                }
            }
            for (key in all) {
                // Keys embed yyyy-MM-dd: prayer_habit_YYYY-MM-DD_*, prayer_record_*_completed, etc.
                val m = Regex("(\\d{4}-\\d{2}-\\d{2})").find(key) ?: continue
                try {
                    val t = sdf.parse(m.groupValues[1])?.time ?: continue
                    if (t < cutoff) {
                        editor.remove(key)
                        removed++
                        if (removed > 500) break
                    }
                } catch (_: Exception) { }
            }
            if (removed > 0) editor.apply()
        } catch (_: Exception) { }
    }

    init {
        habitDateKey = getTodayDateKey()
        recalculateSolarTimes()
        loadHadithData()
        refreshConsistencyHistory()
        viewModelScope.launch(Dispatchers.IO) {
            try { pruneOldDailyKeys() } catch (_: Exception) { }
            try { refreshAvailableCities() } catch (_: Exception) { }
        }
    }
}
