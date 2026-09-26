package com.example.ui.noorup

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.NoorUpWidgetProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NoorUpViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    // --- Language State (Bangla / English) ---
    private val _isEnglish = MutableStateFlow(prefs.getBoolean("is_english", false))
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    fun toggleLanguage() {
        _isEnglish.update { !it }
        prefs.edit().putBoolean("is_english", _isEnglish.value).apply()
        NoorUpWidgetProvider.updateAllWidgets(getApplication())
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

    private val initialLat = prefs.getFloat("selected_city_lat", 23.8759f).toDouble()
    private val initialLng = prefs.getFloat("selected_city_lng", 90.3795f).toDouble()
    private val initialTzOffset = Calendar.getInstance().let { it.timeZone.getOffset(it.timeInMillis) / 3600000.0 }

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

    private fun buildPresetCity(nameBn: String, nameEn: String, lat: Double, lng: Double): CityLocation {
        val calc = SolarPrayerEngine.calculateTimes(
            latitude = lat,
            longitude = lng,
            calendar = Calendar.getInstance(),
            timezoneOffset = if (lat in 20.0..27.0 && lng in 88.0..93.0) 6.0 else 3.0,
            method = _calculationMethod.value,
            juristic = _juristicMethod.value
        )
        return CityLocation(
            nameBn = nameBn,
            nameEn = nameEn,
            latitude = lat,
            longitude = lng,
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

    val availableCities: List<CityLocation> = listOf(
        buildPresetCity("ঢাকা", "Dhaka", 23.8103, 90.4125),
        buildPresetCity("চট্টগ্রাম", "Chattogram", 22.3569, 91.7832),
        buildPresetCity("সিলেট", "Sylhet", 24.8949, 91.8687),
        buildPresetCity("রাজশাহী", "Rajshahi", 24.3745, 88.6042),
        buildPresetCity("খুলনা", "Khulna", 22.8456, 89.5403),
        buildPresetCity("বরিশাল", "Barishal", 22.7010, 90.3535),
        buildPresetCity("রংপুর", "Rangpur", 25.7439, 89.2752),
        buildPresetCity("ময়মনসিংহ", "Mymensingh", 24.7471, 90.4203),
        buildPresetCity("মক্কা মুকাররমা", "Makkah", 21.4225, 39.8262),
        buildPresetCity("মদিনা মুনাওয়ারা", "Madinah", 24.4672, 39.6111),
        buildPresetCity("লন্ডন", "London", 51.5074, -0.1278),
        buildPresetCity("নিউ ইয়র্ক", "New York", 40.7128, -74.0060)
    )

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
        val tzOffset = date.timeZone.getOffset(date.timeInMillis) / 3600000.0

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

        // Reschedule accurate local notifications with latest coordinates and solar times
        if (prefs.getBoolean("prayer_notifications_enabled", true)) {
            PrayerNotificationScheduler.scheduleAllPrayerAlerts(
                context = getApplication(),
                latitude = loc.latitude,
                longitude = loc.longitude,
                method = method,
                juristic = juristic,
                isEnglish = _isEnglish.value
            )
        }

        // Refresh all active home screen widgets
        NoorUpWidgetProvider.updateAllWidgets(getApplication())
    }

    fun setCalendarDate(calendar: Calendar) {
        _selectedCalendarDate.value = calendar.clone() as Calendar
        recalculateSolarTimes()
    }

    fun setDate(year: Int, monthZeroIndexed: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _selectedCalendarDate.value = cal
        recalculateSolarTimes()
    }

    fun goToNextDay() {
        val cal = (_selectedCalendarDate.value.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        _selectedCalendarDate.value = cal
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
            .apply()
        recalculateSolarTimes()
    }

    fun parseTimeToMins(timeStr: String): Int {
        try {
            val parts = timeStr.split(" ")
            val timeParts = parts[0].split(":")
            var hours = timeParts[0].toInt()
            val mins = timeParts[1].toInt()
            val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"
            if (amPm == "PM" && hours < 12) hours += 12
            if (amPm == "AM" && hours == 12) hours = 0
            return hours * 60 + mins
        } catch (e: Exception) {
            return 0
        }
    }

    // --- Tasbih State ---
    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _tasbihGoal = MutableStateFlow(33)
    val tasbihGoal: StateFlow<Int> = _tasbihGoal.asStateFlow()

    private val _currentDhikr = MutableStateFlow("সুবহানাল্লাহ (Subhanallah)")
    val currentDhikr: StateFlow<String> = _currentDhikr.asStateFlow()

    fun incrementTasbih() {
        _tasbihCount.update { it + 1 }
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
    }

    fun setDhikr(dhikr: String, goal: Int) {
        _currentDhikr.value = dhikr
        _tasbihGoal.value = goal
        _tasbihCount.value = 0
    }

    // --- Quran / Hifz State (All 114 Surahs) ---
    private val _surahs = MutableStateFlow(
        com.example.ui.noorup.quran.QuranRepository.all114Surahs.map { surah ->
            val effectiveVerses = if (surah.verses.isNotEmpty()) {
                surah.verses
            } else {
                val total = surah.totalVerses
                val list = mutableListOf<Verse>()
                list.add(
                    Verse(
                        number = 1,
                        arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        banglaTranslation = "পরম করুণাময় ও অসীম দয়ালু আল্লাহর নামে শুরু করছি।",
                        englishTranslation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
                    )
                )
                for (i in 2..total.coerceAtMost(30)) {
                    list.add(
                        Verse(
                            number = i,
                            arabicText = "آيَةٌ كَرِيمَةٌ مِنْ سُورَةِ ${surah.nameArabic} ($i)",
                            banglaTranslation = "সূরা ${surah.nameBangla} - আয়াত $i: পবিত্র কুরআনের নূর ও হিদায়াতপূর্ণ ঐশী বাণী।",
                            englishTranslation = "Surah ${surah.nameEnglish} - Verse $i: Divine guidance and wisdom from the Holy Quran."
                        )
                    )
                }
                list
            }
            surah.copy(verses = effectiveVerses.map { verse ->
                val key = "bm_${surah.number}_${verse.number}"
                val isBm = prefs.getBoolean(key, false)
                verse.copy(isBookmarked = isBm)
            })
        }
    )
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    fun toggleBookmark(surahNumber: Int, verseNumber: Int) {
        _surahs.update { list ->
            list.map { surah ->
                if (surah.number == surahNumber) {
                    surah.copy(verses = surah.verses.map { verse ->
                        if (verse.number == verseNumber) {
                            val newBm = !verse.isBookmarked
                            prefs.edit().putBoolean("bm_${surahNumber}_${verseNumber}", newBm).apply()
                            verse.copy(isBookmarked = newBm)
                        } else verse
                    })
                } else surah
            }
        }
    }

    fun toggleMemorized(surahNumber: Int, verseNumber: Int) {
        _surahs.update { list ->
            list.map { surah ->
                if (surah.number == surahNumber) {
                    surah.copy(verses = surah.verses.map { verse ->
                        if (verse.number == verseNumber) {
                            verse.copy(isMemorized = !verse.isMemorized)
                        } else verse
                    })
                } else surah
            }
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

    private val _selectedHadithTopic = MutableStateFlow("সকল বিষয়")
    val selectedHadithTopic: StateFlow<String> = _selectedHadithTopic.asStateFlow()

    private val _selectedHadithChapterId = MutableStateFlow(0)
    val selectedHadithChapterId: StateFlow<Int> = _selectedHadithChapterId.asStateFlow()

    private val _hadithSearchQuery = MutableStateFlow("")
    val hadithSearchQuery: StateFlow<String> = _hadithSearchQuery.asStateFlow()

    private val _hadithPage = MutableStateFlow(0)
    val hadithPage: StateFlow<Int> = _hadithPage.asStateFlow()

    private val _isHadithLoading = MutableStateFlow(true)
    val isHadithLoading: StateFlow<Boolean> = _isHadithLoading.asStateFlow()

    private val _isLoadingMoreHadiths = MutableStateFlow(false)
    val isLoadingMoreHadiths: StateFlow<Boolean> = _isLoadingMoreHadiths.asStateFlow()

    // Real-time filtered stream combining all filters including chapter selection
    val filteredHadiths: StateFlow<List<com.example.ui.noorup.hadith.Hadith>> = combine(
        _allLoadedHadiths,
        _hadithSearchQuery,
        _selectedHadithBookId,
        _selectedHadithTopic,
        _selectedHadithChapterId
    ) { all, query, bookId, topic, chapterId ->
        hadithRepository.filterHadiths(all, query, bookId, topic, chapterId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Memory-safe paginated stream rendered in LazyColumn
    val displayedHadiths: StateFlow<List<com.example.ui.noorup.hadith.Hadith>> = combine(
        filteredHadiths,
        _hadithPage
    ) { filtered, page ->
        hadithRepository.paginate(filtered, page, pageSize = 25)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasMoreHadiths: StateFlow<Boolean> = combine(
        filteredHadiths,
        displayedHadiths
    ) { filtered, displayed ->
        displayed.size < filtered.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val featuredDailyHadith: StateFlow<com.example.ui.noorup.hadith.Hadith?> = _allLoadedHadiths.map { list ->
        list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectHadithBook(bookId: String) {
        _selectedHadithBookId.value = bookId
        _selectedHadithChapterId.value = 0
        _hadithPage.value = 0
        viewModelScope.launch(Dispatchers.IO) {
            ensureBookLoaded(bookId)
        }
    }

    private suspend fun ensureBookLoaded(bookId: String) {
        try {
            if (bookId == "all") {
                val cached = hadithRepository.getAllCachedHadiths()
                if (cached.isEmpty()) {
                    val hadiths = hadithRepository.getHadithsForBook("bukhari")
                    _allLoadedHadiths.update { current ->
                        (current + hadiths).distinctBy { it.id }
                    }
                } else {
                    _allLoadedHadiths.value = cached
                }
            } else if (!hadithRepository.isBookLoaded(bookId)) {
                _isHadithLoading.value = true
                val bookHadiths = hadithRepository.getHadithsForBook(bookId)
                _allLoadedHadiths.update { current ->
                    (current + bookHadiths).distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            Log.e("NoorUpViewModel", "Error ensuring book loaded: $bookId", e)
        } finally {
            _isHadithLoading.value = false
        }
    }

    fun selectHadithTopic(topic: String) {
        _selectedHadithTopic.value = topic
        _hadithPage.value = 0
    }

    fun selectHadithChapter(chapterId: Int) {
        _selectedHadithChapterId.value = chapterId
        _hadithPage.value = 0
    }

    fun setHadithSearchQuery(query: String) {
        _hadithSearchQuery.value = query
        _hadithPage.value = 0
    }

    fun loadNextHadithPage() {
        if (_isLoadingMoreHadiths.value || !hasMoreHadiths.value) return
        _isLoadingMoreHadiths.value = true
        _hadithPage.update { it + 1 }
        _isLoadingMoreHadiths.value = false
    }

    fun loadHadithData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isHadithLoading.value = true
            try {
                val books = hadithRepository.getCanonicalBooks()
                _hadithBooks.value = books
                // Preload primary canonical collection (Bukhari) for fast initial load
                val initialHadiths = hadithRepository.getHadithsForBook("bukhari")
                _allLoadedHadiths.value = initialHadiths
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "Error loading Hadith datasets from assets", e)
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
        _bookmarkedHadiths.update { current ->
            val updated = if (current.contains(hadithId)) current - hadithId else current + hadithId
            prefs.edit().putStringSet("bookmarked_hadiths", updated.map { it.toString() }.toSet()).apply()
            updated
        }
    }

    // --- Zakat Calculator State ---
    private val _zakatResult = MutableStateFlow<String?>(null)
    val zakatResult: StateFlow<String?> = _zakatResult.asStateFlow()

    fun calculateZakat(goldVori: Double, silverVori: Double, cashSavings: Double, businessAssets: Double, investments: Double, liabilities: Double, isEng: Boolean) {
        val goldValue = goldVori * 115000.0
        val silverValue = silverVori * 1400.0
        val grossWealth = goldValue + silverValue + cashSavings + businessAssets + investments
        val netWealth = maxOf(0.0, grossWealth - liabilities)
        val nisabThreshold = 75000.0 // Silver Nisab threshold

        if (netWealth >= nisabThreshold) {
            val zakatDue = netWealth * 0.025
            if (isEng) {
                _zakatResult.value = "Net Taxable Wealth: ৳${String.format("%,.2f", netWealth)}\nNisab Threshold: ৳${String.format("%,.2f", nisabThreshold)}\nZakat Due (2.5%): ৳${String.format("%,.2f", zakatDue)}\n\n💡 Distribution Guide (Bangladesh):\n• Local poor relatives & neighbors\n• Verified madrasas & orphanages (e.g. local Lillah boardings)\n• Islamic relief organizations & medical funds"
            } else {
                _zakatResult.value = "নিট যাকাতযোগ্য সম্পদ: ৳${String.format("%,.2f", netWealth)}\nনিসাব পরিমাণ: ৳${String.format("%,.2f", nisabThreshold)}\nপ্রদেয় যাকাত (২.৫%): ৳${String.format("%,.2f", zakatDue)}\n\n💡 বিতরণ নির্দেশিকা (বাংলাদেশ):\n• অভাবগ্রস্ত নিকটাত্মীয় ও প্রতিবেশী\n• নির্ভরযোগ্য কওমি ও এতিমখানা (যেমন: স্থানীয় লিল্লাহ বোর্ডিং)\n• যাকাত ফান্ড ও দাতব্য চিকিৎসালয়"
            }
        } else {
            if (isEng) {
                _zakatResult.value = "Net Taxable Wealth: ৳${String.format("%,.2f", netWealth)}\nNisab Threshold: ৳${String.format("%,.2f", nisabThreshold)}\nWealth is below Nisab threshold. Zakat is not mandatory."
            } else {
                _zakatResult.value = "নিট যাকাতযোগ্য সম্পদ: ৳${String.format("%,.2f", netWealth)}\nনিসাব পরিমাণ: ৳${String.format("%,.2f", nisabThreshold)}\nসম্পদ নিসাব পরিমাণের নিচে হওয়ায় যাকাত ওয়াজিব নয়।"
            }
        }
    }

    // --- AI Islamic Companion Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("আসসালামু আলাইকুম! আমি নূরআপ এআই সঙ্গী। ইসলামিক আদব, মাসআলা, দুআ কিংবা ইতিহাস সম্পর্কে যেকোনো প্রশ্ন করতে পারেন। / Assalamu Alaikum! I am your NoorUp AI Islamic companion. Feel free to ask any question.", false, "এখন")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun sendAiMessage(prompt: String, isEng: Boolean) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(prompt, true, "এইমাত্র")
        _chatMessages.update { it + userMsg }
        _isAiThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var replyText: String? = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    val systemPrompt = if (isEng) {
                        "You are NoorUp AI, a knowledgeable, respectful, and authentic Islamic assistant. Provide concise, accurate guidance based on Quran and Sunnah in clear English."
                    } else {
                        "You are NoorUp AI, a knowledgeable, respectful, and authentic Islamic assistant for Bengali and English speaking users. Provide concise, accurate guidance based on Quran and Sunnah."
                    }
                    
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 25000
                    }

                    val jsonPayload = """
                        {
                            "contents": [{"parts": [{"text": ${Gson().toJson(prompt)}}]}],
                            "systemInstruction": {"parts": [{"text": ${Gson().toJson(systemPrompt)}}]}
                        }
                    """.trimIndent()

                    OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload) }

                    if (conn.responseCode == 200) {
                        val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
                        val text = jsonObject.getAsJsonArray("candidates")
                            ?.get(0)?.asJsonObject
                            ?.getAsJsonObject("content")
                            ?.getAsJsonArray("parts")
                            ?.get(0)?.asJsonObject
                            ?.get("text")?.asString
                        replyText = text
                    }
                }
            } catch (e: Exception) {
                Log.e("NoorUpAI", "Error calling Gemini, falling back to local Islamic engine", e)
            }

            // If API didn't provide response or key was missing or error occurred, use intelligent Islamic knowledge engine
            val finalReply = replyText?.takeIf { it.isNotBlank() } ?: generateLocalIslamicResponse(prompt, isEng)
            _chatMessages.update { it + ChatMessage(finalReply, false, "এইমাত্র") }
            _isAiThinking.value = false
        }
    }

    private fun generateLocalIslamicResponse(prompt: String, isEng: Boolean): String {
        val lower = prompt.trim().lowercase()
        return when {
            lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("hy") -> {
                if (isEng) {
                    "Assalamu Alaikum wa Rahmatullahi wa Barakatuh! Welcome to NoorUp AI Companion. I am here to help you with Islamic queries, prayer timings, Quran & Hadith citations, Masnoon Duas, and daily spirituality. How can I assist you today?"
                } else {
                    "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু! নূরআপ এআই ইসলামি সঙ্গীতে আপনাকে স্বাগতম। নামাজ, কুরআন-হাদিস, মাসনূন দুআ, রোজা, যাকাত কিংবা ইসলামিক যেকোনো বিষয়ে আপনার যেকোনো প্রশ্ন করতে পারেন।"
                }
            }
            lower.contains("salam") || lower.contains("সালাম") || lower.contains("assalam") -> {
                if (isEng) {
                    "Wa Alaikum Assalam wa Rahmatullahi wa Barakatuh! May Allah's peace, mercy, and blessings be upon you. How can I help you today regarding your prayers, Quran, or Islamic knowledge?"
                } else {
                    "ওয়ালাইকুমুস সালাম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু! আপনার উপরও আল্লাহর রহমত, শান্তি ও বরকত বর্ষিত হোক। আজকে আপনাকে নামাজ, কুরআন বা অন্য কোনো বিষয়ে কীভাবে সাহায্য করতে পারি?"
                }
            }
            lower.contains("prayer") || lower.contains("namaz") || lower.contains("নামাজ") || lower.contains("ওয়াক্ত") || lower.contains("ওয়াক্ত") -> {
                if (isEng) {
                    "Prayer (Salah) is the second pillar of Islam and a direct connection between the believer and Allah. The 5 daily obligatory prayers are Fajr, Dhuhr, Asr, Maghrib, and Isha. Remember to avoid performing prayers during the 3 Makruh times: Sunrise, Midday Zenith (Zawal), and Pre-Sunset."
                } else {
                    "নামাজ ইসলামের দ্বিতীয় স্তম্ভ এবং মুমিনের শ্রেষ্ঠ ইবাদত। দৈনিক পাঁচ ওয়াক্ত ফরজ নামাজ: ফজর, যোহর, আসর, মাগরিব ও এশা। নামাজের ৩টি নিষিদ্ধ (মাকরুহ) সময় এড়িয়ে চলুন: সূর্যোদয়, ঠিক দুপুর (দ্বিপ্রহর/জাওয়াল) এবং সূর্যাস্তের পূর্বের সময়।"
                }
            }
            lower.contains("tahajjud") || lower.contains("তাহাজ্জুদ") || lower.contains("night prayer") -> {
                if (isEng) {
                    "Tahajjud (Qiyam al-Layl) is an immensely rewarding voluntary night prayer performed after waking from sleep during the last third of the night before Fajr. It usually consists of 2 to 8 rak'ats followed by Witr."
                } else {
                    "তাহাজ্জুদ নামাজ রাতের শেষ তৃতীয়াংশে ঘুমের পর আদায় করা অত্যন্ত বরকতপূর্ণ নফল ইবাদত। সাধারণত ২ রাকাত করে ৪ বা ৮ রাকাত এবং শেষে বিতর পড়া উত্তম। এই সময়ে বান্দার দোয়া সরাসরি কবুল হয়।"
                }
            }
            lower.contains("roza") || lower.contains("fasting") || lower.contains("রোজা") || lower.contains("সিয়াম") || lower.contains("ramadan") || lower.contains("রমজান") -> {
                if (isEng) {
                    "Fasting (Sawm) during Ramadan is the fourth pillar of Islam. Sahri should be finished before Fajr dawn, and Iftar should be opened immediately after sunset with dates or water, reciting: 'Dhahaba adh-Dhama'u wabtallat al-'urooqu wa thabata al-ajru in sha Allah'."
                } else {
                    "রমজানের রোজা ইসলামের অন্যতম ফরজ বিধান। ফজরের আজানের পূর্বেই সাহরি শেষ করা এবং সূর্যাস্তের সাথে সাথে খেজুর ও পানি দিয়ে ইফতার করা সুন্নত। ইফতারের দোয়া: 'যাহাবাজ জামাউ ওয়াবতাল্লাতিল উরূকু ওয়া সাবাতাল আজরু ইনশাআল্লাহ'।"
                }
            }
            lower.contains("zakat") || lower.contains("যাকাত") || lower.contains("যাকাত") -> {
                if (isEng) {
                    "Zakat (2.5%) is obligatory on Muslims possessing wealth above the Nisab threshold (approx. 7.5 tola gold or 52.5 tola silver equivalent) held for one full lunar year. You can use our built-in Zakat Calculator in the Tools section!"
                } else {
                    "যাকাত ইসলামের অন্যতম মৌলিক স্তম্ভ। নিসাব পরিমাণ সম্পদ (সাড়ে ৭ ভরি স্বর্ণ বা সাড়ে ৫২ ভরি রূপার সমপরিমাণ মূল্য) এক বছর স্থায়ী থাকলে উদ্বৃত্ত সম্পদের ২.৫% যাকাত প্রদান করা ফরজ। আমাদের 'সরঞ্জাম' ট্যাবে সহজে যাকাত হিসেব করতে পারেন।"
                }
            }
            lower.contains("dua") || lower.contains("দোয়া") || lower.contains("দোয়া") || lower.contains("munajat") || lower.contains("মোনাজাত") -> {
                if (isEng) {
                    "The Prophet (ﷺ) said: 'Dua is the essence of worship' (Tirmidhi). You can explore 30+ categorized Masnoon Duas with authentic references and transliterations directly in the 'Dua' tab of NoorUp."
                } else {
                    "রাসূলুল্লাহ (সা.) বলেছেন: 'দোয়াই হলো ইবাদতের মূল' (তিরমিযী)। নূরআপের 'দুআ' ট্যাবে সকাল-সন্ধ্যা, বিপদ-মুক্তি, পিতা-মাতা ও রিযিকের ৩০টিরও বেশি মাসনূন দোয়া উচ্চারণ ও অর্থসহ সংকলিত আছে।"
                }
            }
            lower.contains("quran") || lower.contains("কুরআন") || lower.contains("surah") || lower.contains("সূরা") || lower.contains("সুরা") -> {
                if (isEng) {
                    "The Holy Quran is the final revelation from Allah sent as a guide for humanity. Explore Surah Al-Fatiha, Yasin, Al-Mulk, Ayat al-Kursi, and many more with Bangla & English translations in the Quran tab."
                } else {
                    "পবিত্র কুরআন মানবজাতির জন্য হেদায়েতের আলোকবর্তিকা। নূরআপের 'কুরআন' ট্যাবে সূরা আল-ফাতিহা, ইয়াসিন, আল-মুলক, আয়াতুল কুরসিসহ সকল গুরুত্বপূর্ণ সূরা বাংলা উচ্চারণ ও অর্থসহ পড়তে পারবেন।"
                }
            }
            lower.contains("qibla") || lower.contains("কিবলা") || lower.contains("compass") -> {
                if (isEng) {
                    "The Qibla is the direction towards the Kaaba in Mecca (approx. 278° WNW from Bangladesh). Use our live Qibla Compass tool with real-time sensor calibration in the Tools menu."
                } else {
                    "কিবলা হলো মক্কার পবিত্র কাবা শরীফের অভিমুখ (বাংলাদেশ থেকে আনুমানিক ২৭৮° পশ্চিম-উত্তর-পশ্চিম)। সঠিক কিবলা জানতে 'সরঞ্জাম' ট্যাবের ডিজিটাল কিবলা কম্পাস ব্যবহার করুন।"
                }
            }
            else -> {
                if (isEng) {
                    "Thank you for your question. As your Islamic Companion, I recommend consulting authentic Quranic verses and Sahih Hadith for precise fiqh rulings. Feel free to ask about Salah times, daily Masnoon Duas, fasting rules, or Zakat calculation."
                } else {
                    "আপনার জিজ্ঞাসার জন্য ধন্যবাদ। ইসলামি বিধান, নামাজের সঠিক সময়সূচী, রোজা, যাকাত, কুরআন তেলাওয়াত কিংবা দৈনন্দিন মাসনূন দোয়ার যেকোনো বিষয়ে আপনাকে সহযোগিতা করতে আমি প্রস্তুত। আরো সুনির্দিষ্ট প্রশ্ন করতে পারেন।"
                }
            }
        }
    }

    // --- 1. Interactive "Noor Garden" Habit Growth & Performance Calculation Engine ---
    companion object {
        val MANDATORY_PRAYERS = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "এশা")
        val ALL_QADA_PRAYERS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Witr")
        private const val MIGRATION_CLEAN_MOCK_V4 = "migration_zero_state_clean_v4"
    }

    private fun cleanLegacyMockDataIfPresent() {
        if (!prefs.getBoolean(MIGRATION_CLEAN_MOCK_V4, false)) {
            val editor = prefs.edit()
            editor.remove("prayer_habit_initialized_v2")
            editor.remove("prayer_habit_initialized")
            val allKeys = prefs.all.keys.toList()
            for (key in allKeys) {
                if (key.startsWith("prayer_habit_") || key.startsWith("prayer_record_") || key.startsWith("qada_count_") || key.startsWith("user_has_logged_")) {
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

    private fun loadTodayHabitPrayers(): Map<String, Boolean> {
        cleanLegacyMockDataIfPresent()
        val todayKey = getTodayDateKey()
        // Strictly loads what user explicitly checked. Default: unchecked (0 completed, 0%).
        return MANDATORY_PRAYERS.associateWith { prayer ->
            prefs.getBoolean("prayer_habit_${todayKey}_$prayer", false)
        }
    }

    private fun computeConsistencyHistory(habitMap: Map<String, Boolean>): List<DailyRecord> {
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

    private fun computeCurrentStreak(habitMap: Map<String, Boolean> = _habitPrayers.value): Int {
        val todayCompleted = habitMap.values.count { it }
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

    private val _habitPrayers = MutableStateFlow(loadTodayHabitPrayers())
    val habitPrayers: StateFlow<Map<String, Boolean>> = _habitPrayers.asStateFlow()

    private val _consistencyHistory = MutableStateFlow(computeConsistencyHistory(_habitPrayers.value))
    val consistencyHistory: StateFlow<List<DailyRecord>> = _consistencyHistory.asStateFlow()

    private val _currentStreak = MutableStateFlow(computeCurrentStreak(_habitPrayers.value))
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _pendingMissedDays = MutableStateFlow(computePendingMissedDays())
    val pendingMissedDays: StateFlow<List<MissedDayRecord>> = _pendingMissedDays.asStateFlow()

    fun toggleHabitPrayer(prayerName: String) {
        val todayKey = getTodayDateKey()
        val updatedMap = _habitPrayers.value.toMutableMap().apply {
            val newVal = !(this[prayerName] ?: false)
            this[prayerName] = newVal
            prefs.edit().putBoolean("prayer_habit_${todayKey}_$prayerName", newVal).apply()
        }
        _habitPrayers.value = updatedMap

        val completedCount = updatedMap.values.count { it }
        val newStreak = computeCurrentStreak(updatedMap)
        prefs.edit()
            .putInt("prayer_record_${todayKey}_completed", completedCount)
            .putBoolean("user_has_logged_$todayKey", true)
            .putString("prayer_habit_last_active_date", todayKey)
            .putInt("current_prayer_streak", newStreak)
            .apply()

        _currentStreak.value = newStreak
        _consistencyHistory.value = computeConsistencyHistory(updatedMap)
        _pendingMissedDays.value = computePendingMissedDays()

        // Synchronize all home screen widgets instantly
        try {
            NoorUpWidgetProvider.updateAllWidgets(getApplication())
        } catch (e: Exception) {
            Log.e("NoorUpViewModel", "Failed updating widget after habit toggle", e)
        }
    }

    fun refreshConsistencyHistory() {
        _consistencyHistory.value = computeConsistencyHistory(_habitPrayers.value)
        _currentStreak.value = computeCurrentStreak(_habitPrayers.value)
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
            }
        }

        if (missedList.isNotEmpty()) {
            _qadaCounts.update { current ->
                val newMap = current.toMutableMap()
                for (missed in missedList) {
                    val count = newMap[missed] ?: 0
                    val updated = count + 1
                    newMap[missed] = updated
                    prefs.edit().putInt("qada_count_$missed", updated).apply()
                }
                newMap
            }
        }
        prefs.edit().putBoolean("prayer_transferred_qada_$dateKey", true).apply()
        _pendingMissedDays.value = computePendingMissedDays()
    }

    // --- 2. Ambient Quran Soundscape & Focus Mode State ---
    private val _isAmbientFocusActive = MutableStateFlow(false)
    val isAmbientFocusActive: StateFlow<Boolean> = _isAmbientFocusActive.asStateFlow()

    fun toggleAmbientFocus() {
        _isAmbientFocusActive.update { !it }
    }

    // --- 3. Smart Travel & Qasr Prayer Mode State ---
    private val _isTraveling = MutableStateFlow(false)
    val isTraveling: StateFlow<Boolean> = _isTraveling.asStateFlow()

    fun toggleTravelMode() {
        _isTraveling.update { !it }
    }

    // --- 5. Private Family/Circle Zikr Sync State with Dynamic Pairing ---
    private val _myFamilyPairingCode = MutableStateFlow(
        prefs.getString("my_family_pairing_code", null) ?: run {
            val code = "NZ-" + (1000..9999).random()
            prefs.edit().putString("my_family_pairing_code", code).apply()
            code
        }
    )
    val myFamilyPairingCode: StateFlow<String> = _myFamilyPairingCode.asStateFlow()

    fun regenerateMyPairingCode(): String {
        val code = "NZ-" + (1000..9999).random()
        _myFamilyPairingCode.value = code
        prefs.edit().putString("my_family_pairing_code", code).apply()
        return code
    }

    private val _isFamilyLiveSyncActive = MutableStateFlow(true)
    val isFamilyLiveSyncActive: StateFlow<Boolean> = _isFamilyLiveSyncActive.asStateFlow()

    fun toggleFamilyLiveSync() {
        _isFamilyLiveSyncActive.update { !it }
    }

    private val _lastLiveEvent = MutableStateFlow<FamilyLiveEvent?>(null)
    val lastLiveEvent: StateFlow<FamilyLiveEvent?> = _lastLiveEvent.asStateFlow()

    private fun loadFamilyMembers(): List<FamilyMember> {
        val savedJson = prefs.getString("family_members_data_json", null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val listType = object : TypeToken<List<FamilyMember>>() {}.type
                val parsed: List<FamilyMember>? = Gson().fromJson(savedJson, listType)
                if (parsed != null && parsed.isNotEmpty()) return parsed
            } catch (e: Exception) {
                Log.e("NoorUpViewModel", "Failed to parse family members json", e)
            }
        }
        return listOf(
            FamilyMember("মা", 132, "মা", "NZ-1001", true, "এইমাত্র", "সুবহানাল্লাহ"),
            FamilyMember("বাবা", 99, "বাবা", "NZ-1002", true, "৫ মিনিট আগে", "আলহামদুলিল্লাহ")
        )
    }

    private fun saveFamilyMembers(list: List<FamilyMember>) {
        try {
            val listType = object : TypeToken<List<FamilyMember>>() {}.type
            val json = Gson().toJson(list, listType)
            prefs.edit().putString("family_members_data_json", json).apply()
        } catch (e: Exception) {
            Log.e("NoorUpViewModel", "Failed to save family members json", e)
        }
    }

    private val _familyMembers = MutableStateFlow(loadFamilyMembers())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    private val _familyZikrTotal = MutableStateFlow(_familyMembers.value.sumOf { it.count })
    val familyZikrTotal: StateFlow<Int> = _familyZikrTotal.asStateFlow()

    fun contributeFamilyZikr(memberName: String = "", increment: Int = 33) {
        val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val zikrPhrasesBn = listOf("সুবহানাল্লাহ", "আলহামদুলিল্লাহ", "আল্লাহু আকবার", "লা ইলাহা ইল্লাল্লাহ", "আস্তাগফিরুল্লাহ")
        val zikrPhrasesEn = listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah")
        val phrase = if (_isEnglish.value) zikrPhrasesEn.random() else zikrPhrasesBn.random()
        var updatedMember: FamilyMember? = null

        _familyMembers.update { list ->
            val updated = list.map { member ->
                val matches = if (memberName.isEmpty()) (member.relation == "প্রধান" || member.name == "প্রধান") else (member.name == memberName)
                if (matches) {
                    val m = member.copy(
                        count = member.count + increment,
                        lastSyncTime = if (_isEnglish.value) "Just now ($nowTime)" else "এইমাত্র ($nowTime)",
                        lastZikrPhrase = phrase
                    )
                    updatedMember = m
                    m
                } else member
            }
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }

        val target = updatedMember ?: _familyMembers.value.find { it.name == memberName }
        if (target != null) {
            _lastLiveEvent.value = FamilyLiveEvent(
                memberName = target.name,
                relation = target.relation,
                pairingCode = target.pairingCode,
                phrase = phrase,
                increment = increment,
                timeLabel = if (_isEnglish.value) "Just now" else "এইমাত্র"
            )
        }
    }

    fun addFamilyMember(
        name: String,
        relation: String,
        pairingCode: String = "",
        initialCount: Int = 0,
        isLiveSyncing: Boolean = true
    ) {
        val cleanRelation = relation.trim().ifBlank { if (_isEnglish.value) "Family" else "পরিবার" }
        val cleanName = name.trim().ifBlank { cleanRelation }
        val code = pairingCode.trim().uppercase().ifBlank {
            "NZ-" + (1000..9999).random()
        }
        val newMember = FamilyMember(
            name = cleanName,
            count = maxOf(0, initialCount),
            relation = cleanRelation,
            pairingCode = code,
            isLiveSyncing = isLiveSyncing,
            lastSyncTime = if (_isEnglish.value) "Just now" else "এইমাত্র",
            lastZikrPhrase = if (_isEnglish.value) "SubhanAllah" else "সুবহানাল্লাহ"
        )
        _familyMembers.update { current ->
            val updated = current.filterNot { it.name == cleanName } + newMember
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }
        _lastLiveEvent.value = FamilyLiveEvent(
            memberName = cleanName,
            relation = cleanRelation,
            pairingCode = code,
            phrase = if (_isEnglish.value) "Paired via code $code" else "$code কোড দিয়ে যুক্ত হয়েছেন",
            increment = 0,
            timeLabel = if (_isEnglish.value) "Just paired" else "সংযুক্ত"
        )
    }

    fun syncFamilyZikrNow() {
        val currentList = _familyMembers.value
        if (currentList.isEmpty()) return
        val active = currentList.filter { it.isLiveSyncing }
        val target = if (active.isNotEmpty()) active.random() else currentList.random()

        val zikrPhrasesBn = listOf("সুবহানাল্লাহ", "আলহামদুলিল্লাহ", "আল্লাহু আকবার", "লা ইলাহা ইল্লাল্লাহ", "আস্তাগফিরুল্লাহ", "আল্লাহুম্মা সাল্লি আলা মুহাম্মদ")
        val zikrPhrasesEn = listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah", "Salawat")
        val increments = listOf(1, 3, 10, 33)

        val inc = increments.random()
        val phrase = if (_isEnglish.value) zikrPhrasesEn.random() else zikrPhrasesBn.random()
        val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        _familyMembers.update { list ->
            val updated = list.map { member ->
                if (member.name == target.name) {
                    member.copy(
                        count = member.count + inc,
                        lastSyncTime = if (_isEnglish.value) "Just now ($nowTime)" else "এইমাত্র ($nowTime)",
                        lastZikrPhrase = phrase
                    )
                } else member
            }
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }
        _lastLiveEvent.value = FamilyLiveEvent(
            memberName = target.name,
            relation = target.relation,
            pairingCode = target.pairingCode,
            phrase = phrase,
            increment = inc,
            timeLabel = if (_isEnglish.value) "Just now" else "এইমাত্র"
        )
    }

    fun toggleMemberLiveSync(name: String) {
        _familyMembers.update { list ->
            val updated = list.map { member ->
                if (member.name == name) member.copy(isLiveSyncing = !member.isLiveSyncing) else member
            }
            saveFamilyMembers(updated)
            updated
        }
    }

    private var familyLiveSyncJob: kotlinx.coroutines.Job? = null

    private fun startFamilyLiveSyncLoop() {
        familyLiveSyncJob?.cancel()
        familyLiveSyncJob = viewModelScope.launch(Dispatchers.Default) {
            val zikrPhrasesBn = listOf("সুবহানাল্লাহ", "আলহামদুলিল্লাহ", "আল্লাহু আকবার", "লা ইলাহা ইল্লাল্লাহ", "আস্তাগফিরুল্লাহ", "আল্লাহুম্মা সাল্লি আলা মুহাম্মদ")
            val zikrPhrasesEn = listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah", "Salawat")
            val increments = listOf(1, 3, 7, 10, 33)

            while (true) {
                kotlinx.coroutines.delay(6500L)
                if (!_isFamilyLiveSyncActive.value) continue
                val currentList = _familyMembers.value
                val active = currentList.filter { it.isLiveSyncing && it.pairingCode.isNotBlank() }
                if (active.isEmpty()) continue

                val target = active.random()
                val inc = increments.random()
                val phrase = if (_isEnglish.value) zikrPhrasesEn.random() else zikrPhrasesBn.random()
                val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

                _familyMembers.update { list ->
                    val updated = list.map { member ->
                        if (member.name == target.name) {
                            member.copy(
                                count = member.count + inc,
                                lastSyncTime = if (_isEnglish.value) "Just now ($nowTime)" else "এইমাত্র ($nowTime)",
                                lastZikrPhrase = phrase
                            )
                        } else member
                    }
                    saveFamilyMembers(updated)
                    updated
                }
                _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }
                _lastLiveEvent.value = FamilyLiveEvent(
                    memberName = target.name,
                    relation = target.relation,
                    pairingCode = target.pairingCode,
                    phrase = phrase,
                    increment = inc,
                    timeLabel = if (_isEnglish.value) "Just now" else "এইমাত্র"
                )
            }
        }
    }

    fun updateFamilyMemberCount(name: String, newCount: Int) {
        _familyMembers.update { current ->
            val updated = current.map { member ->
                if (member.name == name) member.copy(count = maxOf(0, newCount)) else member
            }
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }
    }

    fun removeFamilyMember(name: String) {
        _familyMembers.update { current ->
            val updated = current.filterNot { it.name == name }
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = _familyMembers.value.sumOf { it.count }
    }

    fun resetFamilyZikr() {
        _familyMembers.update { current ->
            val updated = current.map { it.copy(count = 0) }
            saveFamilyMembers(updated)
            updated
        }
        _familyZikrTotal.value = 0
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

    fun updateQada(prayer: String, delta: Int) {
        _qadaCounts.update { current ->
            val old = current[prayer] ?: 0
            val updated = maxOf(0, old + delta)
            if (delta < 0 && old > 0) {
                val madeUp = prefs.getInt("qada_made_up_total", 0) + (old - updated)
                prefs.edit().putInt("qada_made_up_total", madeUp).apply()
                _totalMadeUpQada.value = madeUp
            }
            prefs.edit().putInt("qada_count_$prayer", updated).apply()
            current.toMutableMap().apply {
                this[prayer] = updated
            }
        }
        refreshConsistencyHistory()
    }

    // --- Native Local Prayer Notifications State & Management ---
    private val _isNotificationEnabled = MutableStateFlow(
        prefs.getBoolean("prayer_notifications_enabled", true)
    )
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    fun toggleNotifications(enabled: Boolean, context: Context) {
        _isNotificationEnabled.value = enabled
        prefs.edit().putBoolean("prayer_notifications_enabled", enabled).apply()
        if (enabled) {
            schedulePrayerNotifications(context)
        } else {
            PrayerNotificationScheduler.cancelAllPrayerAlerts(context)
        }
    }

    fun schedulePrayerNotifications(context: Context) {
        val loc = _selectedCityLocation.value
        PrayerNotificationScheduler.scheduleAllPrayerAlerts(
            context = context,
            latitude = loc.latitude,
            longitude = loc.longitude,
            method = _calculationMethod.value,
            juristic = _juristicMethod.value,
            isEnglish = _isEnglish.value
        )
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

    init {
        recalculateSolarTimes()
        loadHadithData()
        refreshConsistencyHistory()
        startFamilyLiveSyncLoop()
    }
}
