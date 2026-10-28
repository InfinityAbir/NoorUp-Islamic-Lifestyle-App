package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.noorup.AiChatClient
import com.example.ui.noorup.AiChatException
import com.example.ui.noorup.AiFailure
import com.example.ui.noorup.ChatMessage
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.buildChatPayload
import com.example.ui.noorup.parseChatResponse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Islamic Assistant AI path: request shape, response mapping, and offline fallback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiChatClientTest {

  private class FakeClient(
    override val isConfigured: Boolean = true,
    private val result: Result<String>
  ) : AiChatClient {
    var lastQuestion: String? = null
    var lastHistory: List<ChatMessage> = emptyList()
    var lastNotes: String? = null
    var calls = 0

    override suspend fun ask(
      question: String,
      history: List<ChatMessage>,
      isEnglish: Boolean,
      referenceNotes: String
    ): Result<String> {
      calls++
      lastQuestion = question
      lastHistory = history
      lastNotes = referenceNotes
      return result
    }
  }

  private fun vm(client: AiChatClient): NoorUpViewModel =
    NoorUpViewModel(ApplicationProvider.getApplicationContext<Application>()).also { it.aiChat = client }

  private fun ask(viewModel: NoorUpViewModel, prompt: String, isEng: Boolean): String {
    val before = viewModel.chatMessages.value.size
    viewModel.sendAiMessage(prompt, isEng)
    val deadline = System.currentTimeMillis() + 15000
    while (viewModel.chatMessages.value.size < before + 2 && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
    assertEquals(before + 2, viewModel.chatMessages.value.size)
    return viewModel.chatMessages.value.last().text
  }

  @Test
  fun `payload carries tool, language, question and role-tagged history`() {
    val history = listOf(
      ChatMessage("How many rakats is Fajr?", true, "t"),
      ChatMessage("2 fard.", false, "t")
    )
    val json = JSONObject(buildChatPayload("  And Sunnah?  ", history, isEnglish = false))
    assertEquals("chat", json.getString("tool"))
    val fields = json.getJSONObject("fields")
    assertEquals("And Sunnah?", fields.getString("question"))
    assertEquals("bn", fields.getString("lang"))
    val turns = fields.getJSONArray("history")
    assertEquals("user", turns.getJSONObject(0).getString("role"))
    assertEquals("model", turns.getJSONObject(1).getString("role"))
    assertEquals("en", JSONObject(buildChatPayload("q", emptyList(), true)).getJSONObject("fields").getString("lang"))
  }

  @Test
  fun `payload carries vetted reference notes`() {
    val fields = JSONObject(buildChatPayload("wudu?", emptyList(), true, " Wudu is half of faith (Sahih Muslim). "))
      .getJSONObject("fields")
    assertEquals("Wudu is half of faith (Sahih Muslim).", fields.getString("notes"))
    assertEquals("", JSONObject(buildChatPayload("q", emptyList(), true)).getJSONObject("fields").getString("notes"))
  }

  @Test
  fun `known topics send their vetted offline answer as notes, unknown topics send none`() {
    val client = FakeClient(result = Result.success("AI"))
    val viewModel = vm(client)
    ask(viewModel, "how to do wudu", true)
    assertTrue(client.lastNotes!!.contains("Sahih Muslim"))
    ask(viewModel, "xyzqwerty nothing here 12345", true)
    assertEquals("", client.lastNotes)
  }

  @Test
  fun `payload caps very long questions`() {
    val fields = JSONObject(buildChatPayload("a".repeat(5000), emptyList(), true)).getJSONObject("fields")
    assertEquals(1000, fields.getString("question").length)
  }

  @Test
  fun `response codes map to typed failures`() {
    assertEquals("salam", parseChatResponse(200, """{"text":" salam "}""").getOrThrow())
    fun failure(code: Int, body: String = "{}") =
      (parseChatResponse(code, body).exceptionOrNull() as AiChatException).failure
    assertEquals(AiFailure.OTHER, failure(200, """{"text":""}"""))
    assertEquals(AiFailure.OTHER, failure(200, "not json"))
    assertEquals(AiFailure.LIMIT, failure(429, """{"error":{"code":"quota"}}"""))
    assertEquals(AiFailure.LIMIT, failure(429))
    assertEquals(AiFailure.TOO_FAST, failure(429, """{"error":{"code":"rate_limited"}}"""))
    assertEquals(AiFailure.TOO_LONG, failure(413, """{"error":{"code":"too_large"}}"""))
    assertEquals(AiFailure.BUSY, failure(502))
    assertEquals(AiFailure.BUSY, failure(504))
    assertEquals(AiFailure.OTHER, failure(400, """{"error":{"code":"bad_fields"}}"""))
    assertEquals(AiFailure.OTHER, failure(503, """{"error":{"code":"not_configured"}}"""))
  }

  @Test
  fun `ai answer is shown and recent history is sent without the greeting`() {
    val client = FakeClient(result = Result.success("AI says hello"))
    val viewModel = vm(client)
    assertEquals("AI says hello", ask(viewModel, "What is zakat?", true))
    assertEquals("What is zakat?", client.lastQuestion)
    assertTrue("greeting must not be sent as history", client.lastHistory.isEmpty())

    ask(viewModel, "And fitrah?", true)
    assertEquals(listOf("What is zakat?", "AI says hello"), client.lastHistory.map { it.text })
  }

  @Test
  fun `failure keeps the offline answer for known topics and adds a notice`() {
    val client = FakeClient(result = Result.failure(AiChatException(AiFailure.NETWORK)))
    val reply = ask(vm(client), "how to do wudu", true)
    assertTrue(reply.contains("Wudu"))
    assertTrue(reply.contains("No internet connection"))
  }

  @Test
  fun `failure on an unknown topic shows only the notice`() {
    val client = FakeClient(result = Result.failure(AiChatException(AiFailure.LIMIT)))
    val reply = ask(vm(client), "xyzqwerty nothing here 12345", true)
    assertTrue(reply.contains("limit"))
    assertFalse(reply.contains("Thank you for your question"))
  }

  @Test
  fun `every failure shows a plain message with no status codes in both languages`() {
    for (failure in AiFailure.values()) {
      for (english in listOf(true, false)) {
        val client = FakeClient(result = Result.failure(AiChatException(failure)))
        val reply = ask(vm(client), "xyzqwerty nothing here 12345", english)
        assertTrue("$failure/$english should show a message", reply.length > 20)
        assertFalse("$failure/$english leaks a code: $reply", Regex("\b(400|401|403|404|413|429|500|502|503|504)\b|HTTP|Exception").containsMatchIn(reply))
      }
    }
  }

  @Test
  fun `slow connection and rate limit each get their own wording`() {
    fun reply(failure: AiFailure) =
      ask(vm(FakeClient(result = Result.failure(AiChatException(failure)))), "xyzqwerty nothing here 12345", true)
    assertTrue(reply(AiFailure.TIMEOUT).contains("slow or unstable"))
    assertTrue(reply(AiFailure.TOO_FAST).contains("too quickly"))
    assertTrue(reply(AiFailure.LIMIT).contains("limit"))
    assertTrue(reply(AiFailure.TOO_LONG).contains("too long"))
    assertTrue(reply(AiFailure.NETWORK).contains("No internet"))
  }

  @Test
  fun `unconfigured client never calls the network and answers offline`() {
    val client = FakeClient(isConfigured = false, result = Result.success("should not be used"))
    val reply = ask(vm(client), "how to do wudu", true)
    assertEquals(0, client.calls)
    assertTrue(reply.contains("Wudu"))
    assertFalse(reply.contains("internet"))
  }
}
