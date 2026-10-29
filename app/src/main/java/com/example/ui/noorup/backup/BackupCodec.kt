package com.example.ui.noorup.backup

import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** One preference value. [t]: b=Boolean, s=String, i=Int, l=Long, f=Float. */
data class BackupEntry(val t: String = "s", val v: String = "")

data class BackupSnapshot(
    val app: String = "noorup",
    val version: Int = 1,
    val createdAt: Long = 0L,
    val prefs: Map<String, BackupEntry> = emptyMap()
)

/** Outer, unencrypted wrapper holding only what is needed to decrypt. */
data class BackupEnvelope(
    val v: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iter: Int = 210_000,
    val salt: String = "",
    val iv: String = "",
    val ct: String = ""
)

class WrongPassphraseException : Exception("Wrong passphrase or damaged backup")

object BackupCodec {

    /** Exactly the keys worth moving to a new phone. Device-specific and transient keys are left out. */
    private val keyPrefixes = listOf(
        "bm_", "mem_", "dua_bm_", "reader_last_", "focus_",
        "prayer_habit_", "prayer_record_", "prayer_transferred_qada_", "user_has_logged_",
        "qada_", "tasbih_", "ramadan_", "family_",
        "prayer_enabled_", "selected_city_"
    )
    private val exactKeys = setOf(
        "current_prayer_streak", "prayer_tracking_start_date", "prayer_habit_last_active_date",
        "audio_mode", "audio_reciter", "audio_speed", "audio_repeat_target",
        "calculation_method", "juristic_method", "hijri_day_offset",
        "is_dark_mode", "is_english", "is_traveling", "quran_show_transliteration",
        "prayer_notifications_enabled", "tools_tab_order"
    )

    fun isBackedUp(key: String): Boolean = key in exactKeys || keyPrefixes.any { key.startsWith(it) }

    fun snapshot(all: Map<String, *>, now: Long = System.currentTimeMillis()): BackupSnapshot {
        val out = LinkedHashMap<String, BackupEntry>()
        for ((k, v) in all) {
            if (!isBackedUp(k)) continue
            out[k] = when (v) {
                is Boolean -> BackupEntry("b", v.toString())
                is Int -> BackupEntry("i", v.toString())
                is Long -> BackupEntry("l", v.toString())
                is Float -> BackupEntry("f", v.toString())
                is String -> BackupEntry("s", v)
                else -> continue
            }
        }
        return BackupSnapshot(createdAt = now, prefs = out)
    }

    /** Writes a restored snapshot into [editor], only for whitelisted keys with well-formed values. Returns count applied. */
    fun apply(snapshot: BackupSnapshot, editor: SharedPreferences.Editor): Int {
        var n = 0
        for ((k, e) in snapshot.prefs) {
            if (!isBackedUp(k)) continue
            try {
                when (e.t) {
                    "b" -> editor.putBoolean(k, e.v.toBooleanStrict())
                    "i" -> editor.putInt(k, e.v.toInt())
                    "l" -> editor.putLong(k, e.v.toLong())
                    "f" -> editor.putFloat(k, e.v.toFloat())
                    "s" -> editor.putString(k, e.v)
                    else -> continue
                }
                n++
            } catch (_: Exception) { /* skip malformed value */ }
        }
        return n
    }

    private val gson = Gson()
    private val random = SecureRandom()

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    /** JSON of [BackupEnvelope]: AES-256-GCM over the snapshot JSON, key from PBKDF2(passphrase). */
    fun encrypt(snapshot: BackupSnapshot, passphrase: CharArray): ByteArray {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val iter = BackupEnvelope().iter
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, iter), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(gson.toJson(snapshot).toByteArray(Charsets.UTF_8))
        val env = BackupEnvelope(
            iter = iter,
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ct = Base64.encodeToString(ct, Base64.NO_WRAP)
        )
        return gson.toJson(env).toByteArray(Charsets.UTF_8)
    }

    fun decrypt(data: ByteArray, passphrase: CharArray): BackupSnapshot {
        val env = try {
            gson.fromJson(String(data, Charsets.UTF_8), BackupEnvelope::class.java)
        } catch (e: Exception) {
            throw WrongPassphraseException()
        }
        if (env == null || env.ct.isBlank() || env.iter !in 10_000..2_000_000) throw WrongPassphraseException()
        try {
            val salt = Base64.decode(env.salt, Base64.NO_WRAP)
            val iv = Base64.decode(env.iv, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, env.iter), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(Base64.decode(env.ct, Base64.NO_WRAP))
            val snap = gson.fromJson(String(plain, Charsets.UTF_8), BackupSnapshot::class.java)
            if (snap == null || snap.app != "noorup") throw WrongPassphraseException()
            return snap
        } catch (e: WrongPassphraseException) {
            throw e
        } catch (e: Exception) {
            // GCM tag mismatch (wrong passphrase / tampering) and malformed data both land here.
            throw WrongPassphraseException()
        }
    }
}
