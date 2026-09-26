package com.example

import com.example.ui.noorup.CalculationMethod
import com.example.ui.noorup.HijriDateCalculator
import com.example.ui.noorup.JuristicMethod
import com.example.ui.noorup.SolarPrayerEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGregorianDateCalculation() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val result = HijriDateCalculator.calculateGregorianDate(cal)
    assertEquals(25, result.day)
    assertEquals(9, result.month)
    assertEquals(2026, result.year)
    assertEquals("২৫ সেপ্টেম্বর ২০২৬", result.fullDateBn)
    assertEquals("25 September 2026", result.fullDateEn)
  }

  @Test
  fun testHijriDateDynamicCalculation() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val result = HijriDateCalculator.calculateHijriDate(cal)
    assertTrue("Hijri day should be between 1 and 30", result.day in 1..30)
    assertTrue("Hijri month should be between 1 and 12", result.month in 1..12)
    assertEquals(1448, result.year)
    assertTrue("Bangla date should contain year in Bengali digits", result.fullDateBn.contains("১৪৪৮"))
    assertTrue("Bangla month name should be non-empty", result.monthNameBn.isNotEmpty())
    assertTrue("English date should contain 1448", result.fullDateEn.contains("1448"))
  }

  @Test
  fun testBanglaDigitsConversion() {
    assertEquals("০১২৩৪৫৬৭৮৯", HijriDateCalculator.toBanglaDigits("0123456789"))
    assertEquals("২৫", HijriDateCalculator.toBanglaDigits("25"))
    assertEquals("২০২৬", HijriDateCalculator.toBanglaDigits("2026"))
  }

  @Test
  fun testSolarPrayerEngineChronologicalOrder() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val times = SolarPrayerEngine.calculateTimes(
      latitude = 23.8759,
      longitude = 90.3795,
      calendar = cal,
      timezoneOffset = 6.0,
      method = CalculationMethod.KARACHI,
      juristic = JuristicMethod.HANAFI
    )

    val fajrMins = NoorUpWidgetProvider.parseTimeToMins(times.fajrStart)
    val sunriseMins = NoorUpWidgetProvider.parseTimeToMins(times.sunrise)
    val dhuhrMins = NoorUpWidgetProvider.parseTimeToMins(times.dhuhrStart)
    val asrMins = NoorUpWidgetProvider.parseTimeToMins(times.asrStart)
    val maghribMins = NoorUpWidgetProvider.parseTimeToMins(times.maghribStart)
    val ishaMins = NoorUpWidgetProvider.parseTimeToMins(times.ishaStart)

    // Verify Fajr -> Sunrise -> Dhuhr -> Asr -> Maghrib -> Isha order
    assertTrue("Fajr ($fajrMins) must be before Sunrise ($sunriseMins)", fajrMins < sunriseMins)
    assertTrue("Sunrise ($sunriseMins) must be before or equal to Dhuhr ($dhuhrMins)", sunriseMins <= dhuhrMins)
    assertTrue("Dhuhr ($dhuhrMins) must be before Asr ($asrMins)", dhuhrMins < asrMins)
    assertTrue("Asr ($asrMins) must be before Maghrib ($maghribMins)", asrMins < maghribMins)
    assertTrue("Maghrib ($maghribMins) must be before Isha ($ishaMins)", maghribMins < ishaMins)

    // Verify non-overlapping 1-minute interval rule
    val fajrEndMins = NoorUpWidgetProvider.parseTimeToMins(times.fajrEnd)
    val dhuhrEndMins = NoorUpWidgetProvider.parseTimeToMins(times.dhuhrEnd)
    val asrEndMins = NoorUpWidgetProvider.parseTimeToMins(times.asrEnd)
    val maghribEndMins = NoorUpWidgetProvider.parseTimeToMins(times.maghribEnd)

    assertEquals("Fajr should end 1 min before Sunrise", (sunriseMins - 1 + 1440) % 1440, fajrEndMins)
    assertEquals("Dhuhr should end 1 min before Asr start", (asrMins - 1 + 1440) % 1440, dhuhrEndMins)
    assertEquals("Asr should end 1 min before Maghrib start", (maghribMins - 1 + 1440) % 1440, asrEndMins)
    assertEquals("Maghrib should end 1 min before Isha start", (ishaMins - 1 + 1440) % 1440, maghribEndMins)

    // Verify AM/PM strings
    assertTrue("Fajr must be AM", times.fajrStart.contains("AM"))
    assertTrue("Sunrise must be AM", times.sunrise.contains("AM"))
    assertTrue("Dhuhr must be PM or AM", times.dhuhrStart.contains("PM") || times.dhuhrStart.contains("AM"))
    assertTrue("Asr must be PM", times.asrStart.contains("PM"))
    assertTrue("Maghrib must be PM", times.maghribStart.contains("PM"))
    assertTrue("Isha must be PM", times.ishaStart.contains("PM"))
  }

  @Test
  fun testTimeFormattingUtilities() {
    // English
    assertEquals("04:30", NoorUpWidgetProvider.formatShortTime("04:30 AM", true))
    assertEquals("04:30 AM", NoorUpWidgetProvider.formatTimeWithAmPm("04:30 AM", true))
    assertEquals("12:05 PM", NoorUpWidgetProvider.formatTimeWithAmPm("12:05 PM", true))
    assertEquals("07:33 PM", NoorUpWidgetProvider.formatTimeWithAmPm("07:33 PM", true))

    // Bangla
    assertEquals("০৪:৩০", NoorUpWidgetProvider.formatShortTime("04:30 AM", false))
    assertEquals("০৪:৩০ AM", NoorUpWidgetProvider.formatTimeWithAmPm("04:30 AM", false))
    assertEquals("১২:০৫ PM", NoorUpWidgetProvider.formatTimeWithAmPm("12:05 PM", false))

    // Rem time
    assertEquals("1h 15m", NoorUpWidgetProvider.formatRemTime(75, true))
    assertEquals("১ ঘ. ১৫ মি.", NoorUpWidgetProvider.formatRemTime(75, false))
    assertEquals("45m", NoorUpWidgetProvider.formatRemTime(45, true))
    assertEquals("৪৫ মি.", NoorUpWidgetProvider.formatRemTime(45, false))
  }

  @Test
  fun testMoonPhaseAndMilestones() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    val moon = HijriDateCalculator.getMoonPhase(cal)
    assertTrue("Illumination percent should be between 0 and 100", moon.illuminationPercent in 0..100)
    assertTrue("Moon emoji should be non-empty", moon.moonEmoji.isNotEmpty())

    val hijri = HijriDateCalculator.calculateHijriDate(cal)
    val milestones = HijriDateCalculator.getDynamicIslamicMilestones(hijri)
    assertTrue("Milestones list should contain items", milestones.isNotEmpty())
    assertTrue("Milestones should have non-empty titles", milestones.all { it.titleBn.isNotEmpty() && it.titleEn.isNotEmpty() })

    val grid = HijriDateCalculator.getHijriMonthGrid(hijri.year, hijri.month)
    assertTrue("Hijri month grid should have 29 or 30 days", grid.size in 28..31)
  }

  @Test
  fun testBangladeshLocationHijriDate() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 25)
    }

    // Dhaka, Bangladesh
    val isBD = HijriDateCalculator.isBangladeshLocation(23.8103, 90.4125, "Dhaka, Bangladesh")
    assertTrue("Dhaka should be detected as Bangladesh", isBD)

    val hijriBD = HijriDateCalculator.calculateHijriDate(cal, isBangladesh = true)
    assertTrue("Bangladesh standard flag should be true", hijriBD.isBangladeshStandard)
    assertTrue("Bangladesh label should mention Islamic Foundation", hijriBD.standardLabelBn.contains("ইসলামিক ফাউন্ডেশন"))

    // Riyadh, Saudi Arabia
    val isBDRiyadh = HijriDateCalculator.isBangladeshLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia")
    assertFalse("Riyadh should not be detected as Bangladesh", isBDRiyadh)

    // Verify regional offset difference between Bangladesh and Umm al-Qura
    val hijriGlobal = HijriDateCalculator.calculateHijriDate(cal, isBangladesh = false)
    assertFalse("Global standard should not be flagged as Bangladesh", hijriGlobal.isBangladeshStandard)
    assertEquals("Bangladesh Hijri day should be 1 day behind Umm al-Qura on same Gregorian date",
      (hijriGlobal.day - 1), hijriBD.day)
  }

  @Test
  fun testFamilyPairingDataModelAndSerialization() {
    val member = com.example.ui.noorup.FamilyMember(
      name = "মা",
      count = 132,
      relation = "মা",
      pairingCode = "NZ-1001",
      isLiveSyncing = true,
      lastSyncTime = "এইমাত্র",
      lastZikrPhrase = "সুবহানাল্লাহ"
    )

    assertEquals("মা", member.name)
    assertEquals(132, member.count)
    assertEquals("NZ-1001", member.pairingCode)
    assertTrue(member.isLiveSyncing)

    // Test JSON serialization & deserialization for local persistence
    val gson = com.google.gson.Gson()
    val list = listOf(member)
    val json = gson.toJson(list)
    assertNotNull(json)
    assertTrue(json.contains("NZ-1001"))

    val listType = object : com.google.gson.reflect.TypeToken<List<com.example.ui.noorup.FamilyMember>>() {}.type
    val restored: List<com.example.ui.noorup.FamilyMember> = gson.fromJson(json, listType)
    assertEquals(1, restored.size)
    assertEquals("মা", restored[0].name)
    assertEquals(132, restored[0].count)
    assertEquals("NZ-1001", restored[0].pairingCode)

    // Test FamilyLiveEvent creation
    val event = com.example.ui.noorup.FamilyLiveEvent(
      memberName = member.name,
      relation = member.relation,
      pairingCode = member.pairingCode,
      phrase = "সুবহানাল্লাহ",
      increment = 33,
      timeLabel = "এইমাত্র"
    )
    assertEquals("মা", event.memberName)
    assertEquals(33, event.increment)
    assertEquals("NZ-1001", event.pairingCode)
  }
}
