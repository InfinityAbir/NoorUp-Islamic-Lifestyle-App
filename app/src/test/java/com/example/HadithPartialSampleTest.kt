package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.hadith.HadithBookFileJson
import com.example.ui.noorup.hadith.HadithRepository
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Release-blocker regressions (v1.2.0): the muwatta and riyadussalihin bundles
 * hold small verified samples, so their library cards must say so and must not
 * show canonical-collection totals. Guards against inflated counts reappearing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HadithPartialSampleTest {

  private val gson = Gson()

  private fun context(): Context = ApplicationProvider.getApplicationContext()

  private fun bundledHadithCount(assetFile: String): Int {
    context().assets.open(assetFile).bufferedReader(Charsets.UTF_8).use { reader ->
      val parsed = gson.fromJson(reader, HadithBookFileJson::class.java)
      return parsed?.hadiths?.size ?: 0
    }
  }

  @Test
  fun `partial bundles contain exactly the verified sample size`() {
    assertEquals(3, bundledHadithCount("hadith/muwatta.json"))
    assertEquals(3, bundledHadithCount("hadith/riyadussalihin.json"))
  }

  @Test
  fun `partial books are flagged and honestly labeled`() {
    val repo = HadithRepository(context())
    val muwatta = repo.defaultCanonicalBooks.find { it.id == "muwatta" }!!
    val riyad = repo.defaultCanonicalBooks.find { it.id == "riyadussalihin" }!!

    assertTrue(muwatta.isPartialSample)
    assertTrue(riyad.isPartialSample)

    for (book in listOf(muwatta, riyad)) {
      assertTrue(
        "${book.id} label must mark the partial sample, got: ${book.totalHadithsBn}",
        book.totalHadithsBn.contains("আংশিক")
      )
      assertFalse(
        "${book.id} must not show an inflated canonical total",
        book.totalHadithsBn.contains("১,৮৫৮") || book.totalHadithsBn.contains("১,৮৯৬")
      )
    }
    assertFalse(
      "Muwatta chapters must not claim 61, got: ${muwatta.totalChaptersBn}",
      muwatta.totalChaptersBn.contains("৬১")
    )
    assertFalse(
      "Riyadussalihin chapters must not claim 371, got: ${riyad.totalChaptersBn}",
      riyad.totalChaptersBn.contains("৩৭১")
    )
  }

  @Test
  fun `partial labels parse to the true available count`() {
    val repo = HadithRepository(context())
    for (id in listOf("muwatta", "riyadussalihin")) {
      val book = repo.defaultCanonicalBooks.find { it.id == id }!!
      assertEquals("$id parsed count must equal bundled count", 3, book.totalHadith)
    }
  }

  @Test
  fun `complete books are not flagged partial`() {
    val repo = HadithRepository(context())
    val partial = repo.defaultCanonicalBooks.filter { it.isPartialSample }.map { it.id }.toSet()
    assertEquals(setOf("muwatta", "riyadussalihin"), partial)
  }

  @Test
  fun `every configured asset exists and non-partial bundles are substantial`() {
    val repo = HadithRepository(context())
    for (book in repo.defaultCanonicalBooks) {
      assertTrue("Asset file must be configured for ${book.id}", book.assetFileName.isNotBlank())
      // Note: openFd() fails for compressed assets (JSON), so stream them.
      val length = context().assets.open(book.assetFileName).use { it.readBytes().size }
      assertTrue("Asset ${book.assetFileName} must exist and be non-empty", length > 0)
      if (!book.isPartialSample) {
        // Smoke guard against a future stub substitution for the big six:
        // partial samples are ~9KB, real collections are megabytes.
        assertTrue(
          "Non-partial asset ${book.assetFileName} looks like a stub (${length} bytes)",
          length > 10_000
        )
      }
    }
  }
}
