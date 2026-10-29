package com.example.ui.noorup.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.QuranArabicText
import com.example.ui.noorup.hadith.Hadith
import com.example.ui.noorup.toBanglaDigits
import kotlinx.coroutines.delay

@Composable
fun GlobalSearchScreen(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val context = LocalContext.current
    val duas = viewModel.searchableDuas
    val indexProgress by HadithSearchIndex.progress.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    var results by remember { mutableStateOf(SearchResults()) }
    var searching by remember { mutableStateOf(false) }
    var openHadith by remember { mutableStateOf<SearchHit.HadithHit?>(null) }
    val focus = remember { FocusRequester() }

    BackHandler { viewModel.closeGlobalSearch() }

    LaunchedEffect(Unit) {
        HadithSearchIndex.ensureBuilt(context, viewModel.hadithRepository)
        runCatching { focus.requestFocus() }
    }

    // Debounced; also re-runs as hadith books finish indexing.
    LaunchedEffect(query, filter, indexProgress) {
        if (query.trim().length < 2) {
            results = SearchResults(); searching = false; return@LaunchedEffect
        }
        searching = true
        delay(250)
        results = GlobalSearchEngine.search(
            context, viewModel.hadithRepository, duas, query, filter
        ) { viewModel.offlineKnowledgeAnswer(it, isEnglish) }
        searching = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeGlobalSearch() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Close search" else "অনুসন্ধান বন্ধ করুন")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            if (isEnglish) "Search Quran, Hadith, Duas…" else "কুরআন, হাদিস, দুআ খুঁজুন…",
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Clear" else "মুছুন")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focus)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(SearchFilter.entries.toList()) { f ->
                    val count = when (f) {
                        SearchFilter.ALL -> results.quran.size + results.hadith.size + results.duas.size
                        SearchFilter.QURAN -> results.quran.size
                        SearchFilter.HADITH -> results.hadith.size
                        SearchFilter.DUAS -> results.duas.size
                    }
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = {
                            val name = when (f) {
                                SearchFilter.ALL -> if (isEnglish) "All" else "সব"
                                SearchFilter.QURAN -> if (isEnglish) "Quran" else "কুরআন"
                                SearchFilter.HADITH -> if (isEnglish) "Hadith" else "হাদিস"
                                SearchFilter.DUAS -> if (isEnglish) "Duas" else "দুআ"
                            }
                            // Counts only describe the running search, so show one on the selected pill only.
                            val n = if (query.trim().length >= 2 && filter == f) " ${if (isEnglish) count else count.toBanglaDigits()}" else ""
                            Text(name + n, fontSize = 13.sp)
                        }
                    )
                }
            }

            if (indexProgress.first < indexProgress.second && filter != SearchFilter.QURAN && filter != SearchFilter.DUAS) {
                Text(
                    if (isEnglish) "Preparing offline hadith index (${indexProgress.first}/${indexProgress.second} books)… results will grow as it finishes."
                    else "অফলাইন হাদিস সূচি তৈরি হচ্ছে (${indexProgress.first.toBanglaDigits()}/${indexProgress.second.toBanglaDigits()} গ্রন্থ)… শেষ হলে ফলাফল বাড়বে।",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { indexProgress.first / indexProgress.second.coerceAtLeast(1).toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            if (searching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            val queryReady = query.trim().length >= 2
            when {
                !queryReady -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                    Text(
                        if (isEnglish) "Type at least 2 letters. Try \"mercy\", \"2:255\", \"রহমত\", a narrator's name or \"anxiety\"."
                        else "কমপক্ষে ২টি অক্ষর লিখুন। যেমন \"রহমত\", \"2:255\", রাবীর নাম বা \"দুশ্চিন্তা\"।",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                results.isEmpty && !searching -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                    Text(
                        if (isEnglish) "No matches for \"$query\"." else "\"$query\" এর জন্য কিছু পাওয়া যায়নি।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                    )
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (results.quran.isNotEmpty()) {
                        item { SectionHeader(if (isEnglish) "Quran" else "কুরআন") }
                        items(results.quran, key = { it.key }) { hit ->
                            QuranHitCard(hit, isEnglish) { viewModel.openQuranFromSearch(hit.surah, hit.ayah) }
                        }
                    }
                    if (results.hadith.isNotEmpty()) {
                        item { SectionHeader(if (isEnglish) "Hadith" else "হাদিস") }
                        items(results.hadith, key = { it.key }) { hit ->
                            HadithHitCard(hit, isEnglish) { openHadith = hit }
                        }
                    }
                    if (results.duas.isNotEmpty()) {
                        item { SectionHeader(if (isEnglish) "Masnoon Duas" else "মাসনূন দুআ") }
                        items(results.duas, key = { it.key }) { hit ->
                            GlassCard(Modifier.clickable { viewModel.openDuaFromSearch(hit.dua, isEnglish) }) {
                                Text(
                                    if (isEnglish) hit.dua.titleEnglish else hit.dua.titleBangla,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (isEnglish) hit.dua.categoryEnglish else hit.dua.categoryBangla,
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    if (isEnglish) hit.dua.meaningEnglish else hit.dua.meaningBangla,
                                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    results.knowledge?.let { k ->
                        item { SectionHeader(if (isEnglish) "Assistant knowledge (offline)" else "সহকারীর জ্ঞানভাণ্ডার (অফলাইন)") }
                        item {
                            GlassCard(Modifier.clickable { viewModel.openAssistantFromSearch(k.query, isEnglish) }) {
                                Text(
                                    k.preview, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (isEnglish) "Ask the Assistant →" else "সহকারীকে জিজ্ঞাসা করুন →",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    openHadith?.let { hit ->
        HadithDetailDialog(viewModel, hit, isEnglish, onDismiss = { openHadith = null }, onOpenLibrary = {
            openHadith = null
            viewModel.openHadithLibraryFromSearch(hit.bookId, hit.number)
        })
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun QuranHitCard(hit: SearchHit.QuranHit, isEnglish: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.clickable(onClick = onClick)) {
        val sName = if (isEnglish) hit.surahNameEn else hit.surahNameBn
        val ref = if (isEnglish) "${hit.surah}:${hit.ayah}" else "${hit.surah.toBanglaDigits()}:${hit.ayah.toBanglaDigits()}"
        Text(
            if (hit.isSurahMatch) (if (isEnglish) "Surah $sName" else "সূরা $sName") else "$sName • $ref",
            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        QuranArabicText(
            arabic = hit.verse.arabicText, ayahNumber = hit.ayah, fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
        )
        Text(
            if (isEnglish) hit.verse.englishTranslation else hit.verse.banglaTranslation,
            fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hit.matchedField == "transliteration" && hit.verse.banglaTransliteration != null) {
            Text(hit.verse.banglaTransliteration!!, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HadithHitCard(hit: SearchHit.HadithHit, isEnglish: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.clickable(onClick = onClick)) {
        Text(
            "${if (isEnglish) hit.bookNameEn else hit.bookNameBn} • ${if (isEnglish) hit.number else hit.number.toBanglaDigits()}",
            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary
        )
        if (hit.narrator.isNotBlank()) {
            Text(hit.narrator, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(if (isEnglish) hit.snippetEn else hit.snippetBn, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HadithDetailDialog(
    viewModel: NoorUpViewModel,
    hit: SearchHit.HadithHit,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    var full by remember(hit.key) { mutableStateOf<Hadith?>(null) }
    var failed by remember(hit.key) { mutableStateOf(false) }
    LaunchedEffect(hit.key) {
        val loaded = runCatching {
            viewModel.hadithRepository.getHadithsForBook(hit.bookId).firstOrNull { it.id == hit.id }
        }.getOrNull()
        if (loaded == null) failed = true else full = loaded
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${if (isEnglish) hit.bookNameEn else hit.bookNameBn} • ${if (isEnglish) hit.number else hit.number.toBanglaDigits()}",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val h = full
                when {
                    h != null -> {
                        if (h.arabicText.isNotBlank()) {
                            Text(h.arabicText, fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                        Text(if (isEnglish) h.translationEnglish.ifBlank { h.translationBangla } else h.translationBangla.ifBlank { h.translationEnglish }, fontSize = 14.sp, lineHeight = 21.sp)
                        if (h.narrator.isNotBlank()) Text("— ${h.narrator}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (h.reference.isNotBlank()) Text(h.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(h.grade, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    failed -> Text(if (isEnglish) hit.snippetEn else hit.snippetBn, fontSize = 14.sp)
                    else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenLibrary) { Text(if (isEnglish) "Open in Library" else "লাইব্রেরিতে খুলুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "Close" else "বন্ধ") }
        }
    )
}
