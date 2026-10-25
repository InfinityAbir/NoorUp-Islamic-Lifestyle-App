package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.NoorUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AI companion knowledge coverage: every documented topic must produce a real,
 * non-blank answer in the requested language — never the generic fallback.
 * Guards the keyword-routing order (specific branches before general ones).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiKnowledgeRegressionTest {

  private fun vm(): NoorUpViewModel =
    NoorUpViewModel(ApplicationProvider.getApplicationContext<Application>())

  /** Sends [prompt] and returns the assistant's reply text. */
  private fun ask(viewModel: NoorUpViewModel, prompt: String, isEng: Boolean): String {
    val before = viewModel.chatMessages.value.size
    viewModel.sendAiMessage(prompt, isEng)
    val deadline = System.currentTimeMillis() + 15000
    while (viewModel.chatMessages.value.size < before + 2) {
      if (System.currentTimeMillis() > deadline) break
      Thread.sleep(50)
    }
    val messages = viewModel.chatMessages.value
    assertEquals(
      "No reply arrived for prompt: $prompt",
      before + 2, messages.size
    )
    return messages.last().text
  }

  // prompt to expected snippet that must appear in the reply (routing proof)
  private fun topics(): List<Triple<String, Boolean, String>> = listOf(
    Triple("আসসালামু আলাইকুম", false, "ওয়ালাইকুমুস"),
    Triple("hello", true, "Alaikum"),
    Triple("ওযুর নিয়ম", false, "ওযু"),
    Triple("how to do wudu", true, "Wudu"),
    Triple("ফরজ গোসল", false, "গোসল"),
    Triple("তায়াম্মুম", false, "তায়াম্মুম"),
    Triple("জুমার নামাজ", false, "জুমা"),
    Triple("ঈদের নামাজ", false, "ঈদ"),
    Triple("তারাবি কয় রাকাত", false, "তারাবি"),
    Triple("বিতর নামাজ", false, "বিতর"),
    Triple("জানাজার নিয়ম", false, "জানাজা"),
    Triple("সফরে কসর", false, "কসর"),
    Triple("কাজা নামাজ", false, "কাজা"),
    Triple("সুন্নত নামাজ", false, "সুন্নত"),
    Triple("তাহাজ্জুদ নামাজের ফযিলত", false, "তাহাজ্জুদ"),
    Triple("tahajjud prayer", true, "Tahajjud"),
    Triple("আজান শুনলে", false, "আজান"),
    Triple("সাহু সিজদা", false, "সাহু"),
    Triple("ইস্তিখারা", false, "ইস্তিখারা"),
    Triple("নামাজের ওয়াক্ত", false, "পাঁচ ওয়াক্ত"),
    Triple("prayer times", true, "Salah"),
    Triple("সেহরি ও ইফতার", false, "সেহরি"),
    Triple("fidya rules", true, "Fidya"),
    Triple("সোম বৃহস্পতি রোজা", false, "নফল"),
    Triple("what breaks fast", true, "break the fast"),
    Triple("লাইলাতুল কদর", false, "কদর"),
    Triple("ইতিকাফ", false, "ইতিকাফ"),
    Triple("রমজানের রোজা", false, "রমজান"),
    Triple("ফিতরা কখন দিব", false, "ফিতর"),
    Triple("যাকাতের নিসাব", false, "যাকাত"),
    Triple("জিলহজের আমল", false, "জিলহজ"),
    Triple("হজ ফরজ", false, "হজ"),
    Triple("ওমরাহ কিভাবে", false, "ওমরাহ"),
    Triple("তিলাওয়াতের সিজদা", false, "সিজদা"),
    Triple("তাজবীদ শিখতে চাই", false, "তাজবীদ"),
    Triple("কুরআন হিফজ", false, "হিফজ"),
    Triple("আয়াতুল কুরসি", false, "কুরসি"),
    Triple("সূরা কাহফ", false, "কাহফ"),
    Triple("সূরা মুলক", false, "মুলক"),
    Triple("সূরা ইয়াসিন", false, "ইয়াসিন"),
    Triple("বাকারার শেষ দুই আয়াত", false, "বাকারা"),
    Triple("সূরা ইখলাস", false, "ইখলাস"),
    Triple("কুরআন শিখুন", false, "কুরআন"),
    Triple("পিতামাতার হক", false, "পিতামাতা"),
    Triple("সাইয়্যিদুল ইস্তিগফার", false, "ইস্তিগফার"),
    Triple("সকাল-সন্ধ্যার আমল", false, "সকাল-সন্ধ্যা"),
    Triple("ঘুমের দোয়া", false, "ঘুম"),
    Triple("খাওয়ার দোয়া", false, "বিসমিল্লাহ"),
    Triple("সফরের দোয়া", false, "সফর"),
    Triple("মসজিদে ঢোকার দোয়া", false, "মসজিদ"),
    Triple("ঋণ থেকে মুক্তি", false, "ঋণ"),
    Triple("তসবীহ গণনা", false, "তসবীহ"),
    Triple("দরুদ শরীফ", false, "দরুদ"),
    Triple("মাসনূন দোয়া", false, "দুআ"),
    Triple("ইসলামের স্তম্ভ", false, "স্তম্ভ"),
    Triple("ঈমানের রুকন", false, "ঈমান"),
    Triple("কালিমা", false, "কালিমা"),
    Triple("আল্লাহর ৯৯ নাম", false, "৯৯"),
    Triple("ফেরেশতা", false, "ফেরেশতা"),
    Triple("আখিরাত", false, "শেষ দিবস"),
    Triple("তাকদীর", false, "তাকদীর"),
    Triple("শিরক কী", false, "শিরক"),
    Triple("তওবা কিভাবে", false, "তওবা"),
    Triple("নবীজীর জীবন", false, "৬১০"),
    Triple("খলিফা কারা", false, "আবু বকর"),
    Triple("হিজরত", false, "হিজরত"),
    Triple("বদর যুদ্ধ", false, "বদর"),
    Triple("মক্কা বিজয়", false, "বিজয়"),
    Triple("প্রতিবেশীর হক", false, "প্রতিবেশী"),
    Triple("মিথ্যা বলা", false, "মিথ্যা"),
    Triple("রাগ নিয়ন্ত্রণ", false, "রাগ"),
    Triple("গীবত", false, "গীবত"),
    Triple("সদকা দান", false, "সদকা"),
    Triple("হালাল রিজিক", false, "রিজিক"),
    Triple("বিয়ে", false, "নিকাহ"),
    Triple("সন্তান লালন", false, "সন্তান"),
    Triple("বদনজর থেকে বাঁচা", false, "বদনজর"),
    Triple("খারাপ স্বপ্ন", false, "স্বপ্ন"),
    Triple("আশুরার রোজা", false, "আশুরা"),
    Triple("haram food", true, "halal"),
    Triple("হিজাব", false, "হিজাব"),
    Triple("qibla direction", true, "Qibla"),
  )

  @Test
  fun `every topic produces its own answer, never the fallback`() {
    val viewModel = vm()
    val fallback = ask(viewModel, "xyzqwerty nothing here 12345", false)
    assertTrue(fallback.isNotBlank())
    for ((prompt, isEng, snippet) in topics()) {
      val reply = ask(viewModel, prompt, isEng)
      assertTrue("Blank reply for: $prompt", reply.isNotBlank())
      assertTrue("Too short for: $prompt", reply.length > 30)
      assertNotEquals("Fallback shown for: $prompt", fallback, reply)
      assertTrue(
        "Reply for '$prompt' does not look topical (missing '$snippet'): $reply",
        reply.contains(snippet)
      )
    }
  }
}
