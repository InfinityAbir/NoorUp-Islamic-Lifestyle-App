package com.example.ui.noorup.audio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.toBanglaDigits

private fun formatTime(ms: Long, bn: Boolean): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val s = "%d:%02d".format(total / 60, total % 60)
    return if (bn) s.toBanglaDigits() else s
}

private fun num(n: Int, isEnglish: Boolean) = if (isEnglish) n.toString() else n.toBanglaDigits()

/** Floating bottom control bar, shown above the navigation bar while a recitation session is active. */
@Composable
fun QuranAudioBar(controller: QuranAudioController, isEnglish: Boolean, modifier: Modifier = Modifier) {
    val s by controller.state.collectAsState()
    if (!s.active) return

    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableStateOf(0f) }
    val duration = s.durationMs.coerceAtLeast(1L)
    val position = if (scrubbing) scrubValue else s.positionMs.toFloat().coerceIn(0f, duration.toFloat())

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${s.surahTitle} • ${if (isEnglish) "Ayah" else "আয়াত"} ${num(s.ayah, isEnglish)}/${num(s.totalAyahs, isEnglish)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when {
                            s.error == AudioError.NETWORK ->
                                if (isEnglish) "No internet — download this surah to listen offline." else "ইন্টারনেট নেই — অফলাইনে শুনতে সূরাটি ডাউনলোড করুন।"
                            s.error == AudioError.BANGLA_VOICE_MISSING ->
                                if (isEnglish) "No Bengali voice installed on this device — Bangla track skipped." else "ডিভাইসে বাংলা ভয়েস নেই — বাংলা অডিও বাদ দেওয়া হয়েছে।"
                            s.error == AudioError.GENERIC ->
                                if (isEnglish) "Playback problem. Tap play to retry." else "প্লেব্যাকে সমস্যা। আবার চেষ্টা করতে প্লে চাপুন।"
                            s.lang == AudioLang.BN -> if (isEnglish) "Bangla translation" else "বাংলা অনুবাদ"
                            s.lang == AudioLang.EN -> if (isEnglish) "English translation" else "ইংরেজি অনুবাদ"
                            else -> if (isEnglish) s.reciter.nameEn else s.reciter.nameBn
                        },
                        color = if (s.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { controller.stop() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Close player" else "প্লেয়ার বন্ধ করুন")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(position.toLong(), !isEnglish), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = position,
                    onValueChange = { scrubbing = true; scrubValue = it },
                    onValueChangeFinished = { controller.seekTo(scrubValue.toLong()); scrubbing = false },
                    valueRange = 0f..duration.toFloat(),
                    enabled = s.durationMs > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp).height(28.dp)
                )
                Text(formatTime(s.durationMs, !isEnglish), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop: off -> repeat ayah N times -> loop surah
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { controller.cycleLoop() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (s.loopMode == LoopMode.AYAH) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = if (isEnglish) "Loop mode" else "লুপ মোড",
                            tint = if (s.loopMode == LoopMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (s.loopMode == LoopMode.AYAH) {
                        TextButton(
                            onClick = {
                                val steps = listOf(2, 3, 5, 10, 20)
                                val next = steps.firstOrNull { it > s.ayahRepeatTarget } ?: steps.first()
                                controller.setRepeatTarget(next)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                "${num(s.ayahRepeatDone + 1, isEnglish)}/${num(s.ayahRepeatTarget, isEnglish)}×",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (s.loopMode == LoopMode.SURAH) {
                        Text(if (isEnglish) "Surah" else "সূরা", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { controller.previous() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = if (isEnglish) "Previous ayah" else "আগের আয়াত")
                    }
                    FilledIconButton(onClick = { controller.togglePlayPause() }, modifier = Modifier.size(48.dp)) {
                        if (s.isBuffering && s.isPlaying.not() && s.error == null) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                if (s.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (s.isPlaying) (if (isEnglish) "Pause" else "বিরতি") else (if (isEnglish) "Play" else "চালু করুন")
                            )
                        }
                    }
                    IconButton(onClick = { controller.next() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = if (isEnglish) "Next ayah" else "পরের আয়াত")
                    }
                }

                TextButton(
                    onClick = {
                        val speeds = QuranAudioController.SPEEDS
                        val idx = speeds.indexOfFirst { it == s.speed }.coerceAtLeast(0)
                        controller.setSpeed(speeds[(idx + 1) % speeds.size])
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val label = if (s.speed == 1f) "1.0" else s.speed.toString().trimEnd('0')
                    Text(
                        (if (isEnglish) label else label.toBanglaDigits()) + "x",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/** Reader-side panel: choose what to hear, start the surah, download it for offline listening. */
@Composable
fun QuranAudioPanel(
    controller: QuranAudioController,
    surah: Int,
    totalAyahs: Int,
    surahTitle: String,
    isEnglish: Boolean
) {
    val s by controller.state.collectAsState()
    val dl by controller.download.collectAsState()
    val context = LocalContext.current
    var confirmMobileData by remember { mutableStateOf(false) }
    val downloaded = controller.isDownloaded(surah) || (dl.completedSurah == surah)
    val downloadingThis = dl.running && dl.surah == surah

    GlassCard {
        Text(
            if (isEnglish) "Audio recitation" else "অডিও তিলাওয়াত",
            color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaybackMode.entries.forEach { m ->
                FilterChip(
                    selected = s.mode == m,
                    onClick = { controller.setMode(m) },
                    label = { Text(if (isEnglish) m.titleEn else m.titleBn, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }
        if (s.mode != PlaybackMode.ARABIC_ONLY) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEnglish) "Translation:" else "অনুবাদ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TranslationLang.entries.forEach { t ->
                    FilterChip(
                        selected = s.translation == t,
                        onClick = { controller.setTranslation(t) },
                        label = { Text(if (isEnglish) t.titleEn else t.titleBn, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
        }
        if (s.mode != PlaybackMode.TRANSLATION_ONLY) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Reciter.entries.forEach { r ->
                    FilterChip(
                        selected = s.reciter == r,
                        onClick = { controller.setReciter(r) },
                        label = { Text(if (isEnglish) r.nameEn else r.nameBn, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
        }
        if (s.mode != PlaybackMode.ARABIC_ONLY) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.translation == TranslationLang.ENGLISH) {
                    if (isEnglish) "English audio is a human recording of the Sahih International translation (read by Ibrahim Walk)."
                    else "ইংরেজি অডিও Sahih International অনুবাদের মানুষের কণ্ঠে রেকর্ডিং (ইব্রাহিম ওয়াক)।"
                } else {
                    if (isEnglish) "Bangla audio is read aloud by your phone's Bengali voice from the translation."
                    else "বাংলা অডিও অনুবাদ থেকে আপনার ফোনের বাংলা ভয়েসে পড়া হয়।"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { controller.play(surah, 1, totalAyahs, surahTitle) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isEnglish) "Play Surah" else "সূরা শুনুন", fontSize = 13.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    when {
                        !controller.hasDownloadableAudio() -> {}
                        downloadingThis -> controller.cancelDownload()
                        downloaded -> {}
                        !QuranAudioCache.isOnUnmeteredNetwork(context) -> confirmMobileData = true
                        else -> controller.downloadSurah(surah, totalAyahs)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    when {
                        downloaded -> Icons.Default.CheckCircle
                        downloadingThis -> Icons.Default.Close
                        else -> Icons.Default.Download
                    },
                    contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    when {
                        !controller.hasDownloadableAudio() -> if (isEnglish) "Works offline" else "অফলাইনে চলে"
                        downloaded -> if (isEnglish) "Offline ready" else "অফলাইন প্রস্তুত"
                        downloadingThis -> "${num(dl.done, isEnglish)}/${num(dl.total, isEnglish)}"
                        else -> if (isEnglish) "Download audio" else "অডিও ডাউনলোড"
                    },
                    fontSize = 13.sp, maxLines = 1
                )
            }
        }
        if (downloadingThis) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (dl.total > 0) dl.done / dl.total.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (dl.failed && dl.surah == surah) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (isEnglish) "Download failed — check your internet connection and try again."
                else "ডাউনলোড ব্যর্থ — ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।",
                color = MaterialTheme.colorScheme.error, fontSize = 11.sp
            )
        }
    }

    if (confirmMobileData) {
        AlertDialog(
            onDismissRequest = { confirmMobileData = false },
            title = { Text(if (isEnglish) "Not on Wi-Fi" else "ওয়াই-ফাই সংযুক্ত নেই") },
            text = {
                Text(
                    if (isEnglish) "You're not on an unmetered network. Downloading this surah uses mobile data. Download anyway?"
                    else "আপনি আনমিটার্ড নেটওয়ার্কে নেই। সূরাটি ডাউনলোডে মোবাইল ডেটা খরচ হবে। তবুও ডাউনলোড করবেন?"
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmMobileData = false; controller.downloadSurah(surah, totalAyahs) }) {
                    Text(if (isEnglish) "Download" else "ডাউনলোড")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmMobileData = false }) { Text(if (isEnglish) "Cancel" else "বাতিল") }
            }
        )
    }
}
