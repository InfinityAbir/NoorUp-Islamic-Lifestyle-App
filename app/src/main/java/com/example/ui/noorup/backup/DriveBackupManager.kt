package com.example.ui.noorup.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

class DriveAuthException(message: String) : IOException(message)

/** Minimal Drive v3 REST client for the hidden per-app `appDataFolder` (no Google API client library needed). */
object DriveAppData {
    const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "noorup_backup.enc"

    data class RemoteFile(val id: String, val modifiedMs: Long)

    private fun open(url: String, token: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun check(c: HttpURLConnection) {
        val code = c.responseCode
        if (code == 401 || code == 403) throw DriveAuthException("Drive auth rejected ($code)")
        if (code !in 200..299) throw IOException("Drive HTTP $code")
    }

    fun find(token: String): RemoteFile? {
        val q = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime)"
        val c = open(url, token, "GET")
        try {
            check(c)
            val files = JsonParser.parseString(c.inputStream.bufferedReader().readText())
                .asJsonObject.getAsJsonArray("files")
            if (files == null || files.size() == 0) return null
            val f = files[0].asJsonObject
            return RemoteFile(f.get("id").asString, Instant.parse(f.get("modifiedTime").asString).toEpochMilli())
        } finally {
            c.disconnect()
        }
    }

    /** Creates the file, or overwrites its content when [existingId] is known. */
    fun upload(token: String, bytes: ByteArray, existingId: String?): RemoteFile {
        val c: HttpURLConnection
        if (existingId != null) {
            // PATCH is not supported by HttpURLConnection; Drive accepts the override header on POST.
            c = open("https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media&fields=id,modifiedTime", token, "POST")
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            c.setRequestProperty("Content-Type", "application/octet-stream")
            c.doOutput = true
            c.outputStream.use { it.write(bytes) }
        } else {
            val boundary = "noorup" + System.nanoTime()
            c = open("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,modifiedTime", token, "POST")
            c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            c.doOutput = true
            val meta = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
            ByteArrayOutputStream().use { body ->
                body.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n".toByteArray())
                body.write("--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
                body.write(bytes)
                body.write("\r\n--$boundary--".toByteArray())
                c.outputStream.use { it.write(body.toByteArray()) }
            }
        }
        try {
            check(c)
            val o = JsonParser.parseString(c.inputStream.bufferedReader().readText()).asJsonObject
            return RemoteFile(o.get("id").asString, Instant.parse(o.get("modifiedTime").asString).toEpochMilli())
        } finally {
            c.disconnect()
        }
    }

    fun download(token: String, id: String): ByteArray {
        val c = open("https://www.googleapis.com/drive/v3/files/$id?alt=media", token, "GET")
        try {
            check(c)
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }
}

enum class BackupMessage {
    NONE, BACKUP_OK, RESTORE_OK, FILE_SAVED, NO_BACKUP, WRONG_PASSPHRASE, NETWORK, AUTH, NOT_CONFIGURED, FILE_ERROR
}

data class BackupUiState(
    val busy: Boolean = false,
    val message: BackupMessage = BackupMessage.NONE,
    val lastBackupMs: Long = 0L,
    val restartNeeded: Boolean = false
)

/** Backup/restore orchestration. State lives here so screen rotation or tab changes never lose a running upload. */
class DriveBackupManager(private val app: Application) {
    private val prefs = app.getSharedPreferences("noorup_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(BackupUiState(lastBackupMs = prefs.getLong("drive_last_backup_ms", 0L)))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun report(message: BackupMessage) = _state.update { it.copy(message = message, busy = false) }
    fun clearMessage() = _state.update { it.copy(message = BackupMessage.NONE) }

    private suspend fun run(block: suspend () -> BackupMessage) {
        _state.update { it.copy(busy = true, message = BackupMessage.NONE) }
        val result = try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: WrongPassphraseException) {
            BackupMessage.WRONG_PASSPHRASE
        } catch (e: DriveAuthException) {
            BackupMessage.AUTH
        } catch (e: IOException) {
            BackupMessage.NETWORK
        } catch (e: Exception) {
            BackupMessage.FILE_ERROR
        }
        _state.update { it.copy(busy = false, message = result) }
    }

    private fun encryptedSnapshot(passphrase: CharArray): ByteArray =
        BackupCodec.encrypt(BackupCodec.snapshot(prefs.all), passphrase)

    /** Applies a decrypted snapshot. commit() (not apply()) because the app restarts right after. */
    private fun restore(snapshot: BackupSnapshot): BackupMessage {
        val editor = prefs.edit()
        val applied = BackupCodec.apply(snapshot, editor)
        if (applied == 0) return BackupMessage.FILE_ERROR
        editor.commit()
        _state.update { it.copy(restartNeeded = true) }
        return BackupMessage.RESTORE_OK
    }

    suspend fun backupToDrive(token: String, passphrase: CharArray) = run {
        withContext(Dispatchers.IO) {
            val bytes = encryptedSnapshot(passphrase)
            val existing = DriveAppData.find(token)
            val saved = DriveAppData.upload(token, bytes, existing?.id)
            prefs.edit().putLong("drive_last_backup_ms", saved.modifiedMs).apply()
            _state.update { it.copy(lastBackupMs = saved.modifiedMs) }
        }
        BackupMessage.BACKUP_OK
    }

    suspend fun restoreFromDrive(token: String, passphrase: CharArray) = run {
        withContext(Dispatchers.IO) {
            val remote = DriveAppData.find(token) ?: return@withContext BackupMessage.NO_BACKUP
            val snapshot = BackupCodec.decrypt(DriveAppData.download(token, remote.id), passphrase)
            restore(snapshot)
        }
    }

    suspend fun saveToFile(uri: Uri, passphrase: CharArray) = run {
        withContext(Dispatchers.IO) {
            val bytes = encryptedSnapshot(passphrase)
            app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: throw IOException("no stream")
        }
        BackupMessage.FILE_SAVED
    }

    suspend fun restoreFromFile(uri: Uri, passphrase: CharArray) = run {
        withContext(Dispatchers.IO) {
            val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("no stream")
            restore(BackupCodec.decrypt(bytes, passphrase))
        }
    }
}
