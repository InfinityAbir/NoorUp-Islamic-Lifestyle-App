package com.example

import android.net.Uri
import com.example.ui.noorup.audio.AudioIds
import com.example.ui.noorup.audio.AudioLang
import com.example.ui.noorup.audio.PlaybackMode
import com.example.ui.noorup.audio.Reciter
import com.example.ui.noorup.backup.BackupCodec
import com.example.ui.noorup.backup.WrongPassphraseException
import com.example.ui.noorup.family.FamilyDhikrLogic
import com.example.ui.noorup.family.FamilyGoal
import com.example.ui.noorup.family.FamilyLink
import com.example.ui.noorup.ramadan.RamadanLogic
import com.example.ui.noorup.ramadan.RamadanLogic.FastStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewFeaturesTest {

    // --- audio ---

    @Test
    fun `everyayah url is zero padded`() {
        assertEquals("https://everyayah.com/data/Alafasy_128kbps/002255.mp3", Reciter.ALAFASY.ayahUrl(2, 255))
        assertEquals("https://everyayah.com/data/Husary_128kbps/001001.mp3", Reciter.HUSARY.ayahUrl(1, 1))
    }

    @Test
    fun `english translation uses the real recording and bangla uses on device speech`() {
        assertEquals(
            "https://everyayah.com/data/English/Sahih_Intnl_Ibrahim_Walk_192kbps/112004.mp3",
            com.example.ui.noorup.audio.TranslationLang.ENGLISH.ayahUrl(112, 4)
        )
        assertNull(com.example.ui.noorup.audio.TranslationLang.BANGLA.ayahUrl(112, 4))
        assertEquals(Triple(AudioLang.EN, 2, 255), AudioIds.parse(AudioIds.mediaId(AudioLang.EN, 2, 255)))
    }

    @Test
    fun `media ids round trip and reject garbage`() {
        val id = AudioIds.mediaId(AudioLang.BN, 36, 12)
        assertEquals(Triple(AudioLang.BN, 36, 12), AudioIds.parse(id))
        assertNull(AudioIds.parse("xx:1:1"))
        assertNull(AudioIds.parse("ar:1"))
        assertNull(AudioIds.parse(null))
    }

    @Test
    fun `dual mode interleaves two items per ayah`() {
        assertEquals(2, PlaybackMode.DUAL.itemsPerAyah)
        assertEquals(1, PlaybackMode.ARABIC_ONLY.itemsPerAyah)
        assertEquals(1, PlaybackMode.TRANSLATION_ONLY.itemsPerAyah)
        assertEquals(PlaybackMode.TRANSLATION_ONLY, PlaybackMode.fromName("BANGLA_ONLY")) // old saved value
    }

    // --- ramadan ---

    @Test
    fun `night starts at maghrib`() {
        assertEquals(26, RamadanLogic.nightNumber(26, afterMaghrib = false))
        assertEquals(27, RamadanLogic.nightNumber(26, afterMaghrib = true))
        assertTrue(RamadanLogic.isOddLastTenNight(27))
        assertFalse(RamadanLogic.isOddLastTenNight(26))
        assertFalse(RamadanLogic.isOddLastTenNight(19))
    }

    @Test
    fun `fasting streak ignores an unmarked today but stops at a miss`() {
        val s = mapOf(1 to FastStatus.FASTED, 2 to FastStatus.FASTED, 3 to FastStatus.MISSED, 4 to FastStatus.FASTED, 5 to FastStatus.FASTED)
        assertEquals(2, RamadanLogic.streak(s, 5))
        assertEquals(2, RamadanLogic.streak(s, 6)) // day 6 not marked yet
        assertEquals(0, RamadanLogic.streak(s, 3))
    }

    @Test
    fun `fitrah amount scales with members grain and price`() {
        assertEquals(4 * 1.75 * 60, RamadanLogic.fitrAmount(4, RamadanLogic.Grain.WHEAT, 60.0), 0.001)
        assertEquals(0.0, RamadanLogic.fitrAmount(-3, RamadanLogic.Grain.DATES, 600.0), 0.001)
    }

    @Test
    fun `int map storage round trips`() {
        val m = mapOf(3 to 8, 1 to 20)
        assertEquals(m, RamadanLogic.decodeIntMap(RamadanLogic.encodeIntMap(m)))
        assertTrue(RamadanLogic.decodeIntMap("junk,1:x,2:3").containsKey(2))
    }

    // --- family dhikr ---

    private val goal = FamilyGoal("abc12345", "SubhanAllah", 10000, "আব্বু & Co")

    @Test
    fun `family link round trips including unicode names`() {
        val link = FamilyLink.build(goal, "আম্মু", 1200, 5, "2026-10-29")
        val p = FamilyLink.parse(Uri.parse(link))
        assertNotNull(p)
        assertEquals("abc12345", p!!.goalId)
        assertEquals("আব্বু & Co", p.createdBy)
        assertEquals("আম্মু", p.member)
        assertEquals(1200, p.count)
        assertEquals(5, p.prayers)
        assertEquals("2026-10-29", p.dateKey)
    }

    @Test
    fun `a whole shared chat message can be pasted`() {
        val link = FamilyLink.build(goal, "আম্মু", 1200, 5, "2026-10-29")
        val message = "আম্মু-এর নূরআপ প্রগ্রেস: খুলুন:\n$link\n\nCan't tap the link? Copy this whole message, then open NoorUp."
        assertEquals(link, FamilyLink.extract(message))
        assertEquals(1200, FamilyLink.parseText(message)!!.count)
        // Trailing sentence punctuation from a chat app is not part of the link.
        assertEquals(link, FamilyLink.extract("see $link."))
        assertNull(FamilyLink.extract("no link in here"))
    }

    @Test
    fun `family link rejects foreign or malformed links`() {
        assertNull(FamilyLink.parse(Uri.parse("https://example.com/dhikr?id=1&t=x&g=5")))
        assertNull(FamilyLink.parse(Uri.parse("noorup://dhikr?id=1&g=5")))
        assertNull(FamilyLink.parseText("not a link"))
    }

    @Test
    fun `merge keeps the highest count per member and is idempotent`() {
        val a = FamilyLink.parse(Uri.parse(FamilyLink.build(goal, "Ammu", 500)))!!
        val once = FamilyDhikrLogic.merge(emptyList(), a)
        val twice = FamilyDhikrLogic.merge(once, a)
        assertEquals(once, twice)
        val lower = FamilyLink.parse(Uri.parse(FamilyLink.build(goal, "Ammu", 100)))!!
        assertEquals(500, FamilyDhikrLogic.merge(twice, lower).single().contributions["Ammu"])
        val higher = FamilyLink.parse(Uri.parse(FamilyLink.build(goal, "Ammu", 900)))!!
        assertEquals(900, FamilyDhikrLogic.merge(twice, higher).single().total)
    }

    @Test
    fun `an edited goal on another phone replaces title and target but keeps counts`() {
        val local = FamilyDhikrLogic.merge(emptyList(), FamilyLink.parse(Uri.parse(FamilyLink.build(goal, "Ammu", 300)))!!)
        val edited = goal.copy(title = "Hasbunallah", target = 5000, rev = 1)
        val incoming = FamilyLink.parse(Uri.parse(FamilyLink.build(edited, "Abbu", 100)))!!
        val merged = FamilyDhikrLogic.merge(local, incoming).single()
        assertEquals("Hasbunallah", merged.title)
        assertEquals(5000, merged.target)
        assertEquals(400, merged.total)
        // An older revision never overwrites a newer one.
        val stale = FamilyLink.parse(Uri.parse(FamilyLink.build(goal, "Abbu", 100)))!!
        assertEquals("Hasbunallah", FamilyDhikrLogic.merge(listOf(merged), stale).single().title)
    }

    @Test
    fun `family streak counts consecutive fully prayed days`() {
        val prev = mapOf("2026-10-29" to "2026-10-28", "2026-10-28" to "2026-10-27", "2026-10-27" to "2026-10-26")
        val checkIns = mapOf(
            "2026-10-29" to mapOf("A" to 5, "B" to 5),
            "2026-10-28" to mapOf("A" to 5, "B" to 5),
            "2026-10-27" to mapOf("A" to 5, "B" to 3)
        )
        assertEquals(2, FamilyDhikrLogic.familyStreak(checkIns, "2026-10-29") { prev.getValue(it) })
        // Today not complete yet: yesterday's streak survives.
        val partial = checkIns + ("2026-10-29" to mapOf("A" to 2, "B" to 5))
        assertEquals(1, FamilyDhikrLogic.familyStreak(partial, "2026-10-29") { prev.getValue(it) })
    }

    // --- backup ---

    @Test
    fun `backup snapshot keeps user data and drops device specific keys`() {
        val all = mapOf<String, Any>(
            "bm_2_255" to true, "tasbih_count" to 12, "ramadan_fast_1448" to "1:1", "is_english" to true,
            "ai_device_id" to "secret-device", "selected_tab" to 3, "last_prayer_post_id" to 7
        )
        val snap = BackupCodec.snapshot(all)
        assertEquals(setOf("bm_2_255", "tasbih_count", "ramadan_fast_1448", "is_english"), snap.prefs.keys)
    }

    @Test
    fun `backup encrypts and decrypts with the right passphrase only`() {
        val snap = BackupCodec.snapshot(mapOf("bm_1_1" to true, "tasbih_goal" to 100))
        val blob = BackupCodec.encrypt(snap, "correct horse".toCharArray())
        assertFalse(String(blob).contains("bm_1_1")) // payload is not readable in the envelope
        assertEquals(snap.prefs, BackupCodec.decrypt(blob, "correct horse".toCharArray()).prefs)
        try {
            BackupCodec.decrypt(blob, "wrong pass!".toCharArray())
            throw AssertionError("expected WrongPassphraseException")
        } catch (_: WrongPassphraseException) { }
    }

    @Test
    fun `backup refuses non whitelisted keys on restore`() {
        val tampered = com.example.ui.noorup.backup.BackupSnapshot(
            prefs = mapOf(
                "ai_device_id" to com.example.ui.noorup.backup.BackupEntry("s", "evil"),
                "bm_3_4" to com.example.ui.noorup.backup.BackupEntry("b", "true"),
                "tasbih_goal" to com.example.ui.noorup.backup.BackupEntry("i", "not-a-number")
            )
        )
        val prefs = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("restore_test", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        assertEquals(1, BackupCodec.apply(tampered, editor))
        editor.commit()
        assertTrue(prefs.getBoolean("bm_3_4", false))
        assertFalse(prefs.contains("ai_device_id"))
        assertFalse(prefs.contains("tasbih_goal"))
    }
}
