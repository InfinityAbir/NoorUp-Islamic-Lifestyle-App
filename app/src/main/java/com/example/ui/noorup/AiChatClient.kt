package com.example.ui.noorup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Why an AI answer could not be produced; the assistant falls back to its offline answers. */
enum class AiFailure {
    /** No internet connection (airplane mode, no data, DNS failure). */
    NETWORK,
    /** Connected, but the connection is too slow or dropped before an answer came back. */
    TIMEOUT,
    /** This device is asking too fast (per-device or per-IP rate limit). */
    TOO_FAST,
    /** The shared free AI quota is used up for now. */
    LIMIT,
    /** The AI service is overloaded or temporarily down. */
    BUSY,
    /** The question or chat history is too long for the service. */
    TOO_LONG,
    /** Anything else (bad request, service not configured, empty answer). */
    OTHER
}

class AiChatException(val failure: AiFailure) : Exception(failure.name)

/** Source of AI answers for the Islamic Assistant. */
interface AiChatClient {
    /** False when no proxy URL was built into the app: the assistant then stays fully offline. */
    val isConfigured: Boolean

    /**
     * [referenceNotes] is vetted offline text (with Quran/Hadith references) for the topic, or empty.
     * The proxy gives it to the model so answers stay consistent with the app's own sources.
     */
    suspend fun ask(
        question: String,
        history: List<ChatMessage>,
        isEnglish: Boolean,
        referenceNotes: String = ""
    ): Result<String>
}

/**
 * Talks to the NoorUp AI proxy (Cloudflare Worker in `ai-proxy/`), which holds the Gemini key and the
 * system prompt. The app only sends the question, the language and a short chat history.
 */
class ProxyAiChatClient(
    private val proxyUrl: String,
    private val deviceIdProvider: () -> String,
) : AiChatClient {

    override val isConfigured: Boolean get() = proxyUrl.isNotBlank()

    override suspend fun ask(
        question: String,
        history: List<ChatMessage>,
        isEnglish: Boolean,
        referenceNotes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(AiChatException(AiFailure.OTHER))
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(proxyUrl.trimEnd('/') + "/v1/ai").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-device-id", deviceIdProvider())
            }
            connection.outputStream.use { it.write(buildChatPayload(question, history, isEnglish, referenceNotes).toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            parseChatResponse(code, body)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(AiChatException(AiFailure.TIMEOUT))
        } catch (e: IOException) {
            Result.failure(AiChatException(AiFailure.NETWORK))
        } catch (e: Exception) {
            Result.failure(AiChatException(AiFailure.OTHER))
        } finally {
            connection?.disconnect()
        }
    }
}

private const val MAX_QUESTION_CHARS = 1000
private const val MAX_HISTORY_CHARS = 1500
private const val MAX_NOTES_CHARS = 1500

/** Request body for the proxy's `chat` tool. Pure function so it can be unit tested. */
internal fun buildChatPayload(
    question: String,
    history: List<ChatMessage>,
    isEnglish: Boolean,
    referenceNotes: String = ""
): String {
    val turns = JSONArray()
    history.forEach { message ->
        turns.put(
            JSONObject()
                .put("role", if (message.isUser) "user" else "model")
                .put("text", message.text.take(MAX_HISTORY_CHARS))
        )
    }
    val fields = JSONObject()
        .put("question", question.trim().take(MAX_QUESTION_CHARS))
        .put("lang", if (isEnglish) "en" else "bn")
        .put("history", turns)
        .put("notes", referenceNotes.trim().take(MAX_NOTES_CHARS))
    return JSONObject().put("tool", "chat").put("fields", fields).toString()
}

/** Maps the proxy's HTTP answer to reply text or a typed failure. The user never sees status codes. */
internal fun parseChatResponse(code: Int, body: String): Result<String> {
    if (code in 200..299) {
        val text = runCatching { JSONObject(body).optString("text", "") }.getOrDefault("").trim()
        return if (text.isNotEmpty()) Result.success(text) else Result.failure(AiChatException(AiFailure.OTHER))
    }
    val reason = runCatching { JSONObject(body).getJSONObject("error").optString("code") }.getOrDefault("")
    val failure = when {
        reason == "rate_limited" -> AiFailure.TOO_FAST
        code == 429 -> AiFailure.LIMIT
        code == 413 || reason == "too_large" -> AiFailure.TOO_LONG
        reason == "not_configured" -> AiFailure.OTHER
        code in 502..504 -> AiFailure.BUSY
        else -> AiFailure.OTHER
    }
    return Result.failure(AiChatException(failure))
}
