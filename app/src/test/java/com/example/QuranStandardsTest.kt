package com.example

import com.example.ui.noorup.BASMALA_TEXT
import com.example.ui.noorup.stripLeadingBasmala
import com.example.ui.noorup.surahHasBasmalaHeader
import com.example.ui.noorup.quran.JuzIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranStandardsTest {

    @Test
    fun `juz table has 30 ascending starts`() {
        assertEquals(30, JuzIndex.STARTS.size)
        JuzIndex.STARTS.zipWithNext().forEach { (a, b) ->
            assertTrue(b.surah > a.surah || (b.surah == a.surah && b.ayah > a.ayah))
        }
    }

    @Test
    fun `juzOf maps boundaries`() {
        assertEquals(1, JuzIndex.juzOf(1, 1))
        assertEquals(1, JuzIndex.juzOf(2, 141))
        assertEquals(2, JuzIndex.juzOf(2, 142))
        assertEquals(30, JuzIndex.juzOf(114, 6))
    }

    @Test
    fun `basmala header applies to every surah except Fatihah and Tawbah`() {
        assertFalse(surahHasBasmalaHeader(1))
        assertFalse(surahHasBasmalaHeader(9))
        assertTrue(surahHasBasmalaHeader(2))
        assertTrue(surahHasBasmalaHeader(114))
    }

    @Test
    fun `fused basmala is split out of ayah 1 only`() {
        val fused = "$BASMALA_TEXT الٓمٓ"
        assertEquals("الٓمٓ", stripLeadingBasmala(2, 1, fused))
        // Al-Fatihah ayah 1 IS the basmala; later ayahs and other surahs are untouched.
        assertEquals("﻿$BASMALA_TEXT", stripLeadingBasmala(1, 1, "﻿$BASMALA_TEXT"))
        assertEquals(fused, stripLeadingBasmala(2, 2, fused))
        assertEquals(fused, stripLeadingBasmala(9, 1, fused))
    }
}
