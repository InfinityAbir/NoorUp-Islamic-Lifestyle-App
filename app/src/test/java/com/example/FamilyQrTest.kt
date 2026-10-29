package com.example

import android.net.Uri
import com.example.ui.noorup.family.FamilyDhikrLogic
import com.example.ui.noorup.family.FamilyGoal
import com.example.ui.noorup.family.FamilyLink
import com.example.ui.noorup.family.qrBitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Draws the family invite QR exactly as the app does, then reads it back like a scanner would. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FamilyQrTest {

    private fun decode(text: String): String {
        val bmp = qrBitmap(text)
        val pixels = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
        val source = RGBLuminanceSource(bmp.width, bmp.height, pixels)
        return MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test
    fun `invite qr decodes back to the same link and joins the goal`() {
        val goal = FamilyGoal("a1b2c3d4", "সুবহানাল্লাহ", 100000, "আব্বু রহমান", rev = 2)
        val link = FamilyLink.build(goal)
        val scanned = decode(link)
        assertEquals(link, scanned)

        val payload = FamilyLink.parseText(scanned)!!
        val joined = FamilyDhikrLogic.merge(emptyList(), payload).single()
        assertEquals("সুবহানাল্লাহ", joined.title)
        assertEquals(100000, joined.target)
        assertEquals(2, joined.rev)
    }

    @Test
    fun `progress qr with a long unicode name still decodes`() {
        val goal = FamilyGoal("a1b2c3d4", "আস্তাগফিরুল্লাহ", 50000, "মোহাম্মদ আব্দুল্লাহ আল মামুন")
        val link = FamilyLink.build(goal, "ফাতেমা খাতুন", 4321, 5, "2026-10-29")
        assertEquals(link, decode(link))
        assertEquals(4321, FamilyLink.parse(Uri.parse(decode(link)))!!.count)
    }
}
