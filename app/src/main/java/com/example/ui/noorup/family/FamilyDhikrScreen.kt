package com.example.ui.noorup.family

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.toBanglaDigits
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.text.KeyboardOptions

private fun n(v: Int, en: Boolean) = if (en) v.toString() else v.toBanglaDigits()

internal fun qrBitmap(text: String, size: Int = 640): Bitmap {
    // Level L keeps the code as sparse as possible so phone cameras read it easily.
    val hints = mapOf(
        EncodeHintType.MARGIN to 2,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) AColor.BLACK else AColor.WHITE }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private const val CUSTOM_INDEX = -1

private val presets = listOf(
    "سُبْحَانَ ٱللَّهِ" to ("SubhanAllah" to "সুবহানাল্লাহ"),
    "ٱلْحَمْدُ لِلَّهِ" to ("Alhamdulillah" to "আলহামদুলিল্লাহ"),
    "ٱللَّهُ أَكْبَرُ" to ("Allahu Akbar" to "আল্লাহু আকবার"),
    "لَا إِلَٰهَ إِلَّا ٱللَّهُ" to ("La ilaha illallah" to "লা ইলাহা ইল্লাল্লাহ"),
    "أَسْتَغْفِرُ ٱللَّهَ" to ("Astaghfirullah" to "আস্তাগফিরুল্লাহ"),
    "ٱللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ" to ("Salawat" to "দরূদ শরীফ")
)

@Composable
fun FamilyDhikrView(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val goals by viewModel.familyGoals.collectAsState()
    val myName by viewModel.familyMyName.collectAsState()
    val pendingJoin by viewModel.pendingFamilyJoin.collectAsState()
    val joinMessage by viewModel.familyMessage.collectAsState()
    val context = LocalContext.current

    var nameDraft by remember(myName) { mutableStateOf(myName) }
    var presetIndex by remember { mutableStateOf(0) }
    var customTitle by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("10000") }
    var pasteText by remember { mutableStateOf("") }
    var qrFor by remember { mutableStateOf<FamilyGoal?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text(if (isEnglish) "Family Dhikr & Shared Goals" else "পারিবারিক যিকর ও যৌথ লক্ষ্য", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (isEnglish) "No account or server: share a link or QR with your family. Each person counts on their own phone and shares a progress link back to merge into the total."
                else "কোনো অ্যাকাউন্ট বা সার্ভার লাগে না: পরিবারের সাথে লিংক বা QR শেয়ার করুন। প্রত্যেকে নিজের ফোনে গুনে প্রগ্রেস লিংক ফেরত পাঠালে মোট সংখ্যায় যুক্ত হয়।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameDraft, onValueChange = { nameDraft = it.take(40) },
                    label = { Text(if (isEnglish) "Your name" else "আপনার নাম") }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { viewModel.setFamilyMyName(nameDraft) }, enabled = nameDraft.trim() != myName && nameDraft.isNotBlank(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (isEnglish) "Save" else "সংরক্ষণ")
                }
            }
        }

        joinMessage?.let {
            GlassCard {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                TextButton(onClick = { viewModel.clearFamilyMessage() }) { Text(if (isEnglish) "OK" else "ঠিক আছে") }
            }
        }

        if (goals.isEmpty()) {
            Text(
                if (isEnglish) "No family goals yet. Create one above or open a link from a family member."
                else "এখনো কোনো পারিবারিক লক্ষ্য নেই। উপরে তৈরি করুন বা পরিবারের কারও লিংক খুলুন।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        goals.forEach { goal ->
            GoalCard(viewModel, goal, myName, isEnglish, onShowQr = { qrFor = goal })
        }

        FamilyCheckInCard(viewModel)

        // --- create ---
        GlassCard {
            Text(if (isEnglish) "Create a family goal" else "পারিবারিক লক্ষ্য তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { col, p ->
                            val idx = row * 2 + col
                            FilterChip(
                                selected = presetIndex == idx, onClick = { presetIndex = idx },
                                label = { Text(if (isEnglish) p.second.first else p.second.second, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }
                }
            }
            FilterChip(
                selected = presetIndex == CUSTOM_INDEX, onClick = { presetIndex = CUSTOM_INDEX },
                label = { Text(if (isEnglish) "+ Your own dhikr" else "+ নিজের যিকর", fontSize = 12.sp, maxLines = 1) },
                modifier = Modifier.padding(top = 4.dp)
            )
            if (presetIndex == CUSTOM_INDEX) {
                OutlinedTextField(
                    value = customTitle, onValueChange = { customTitle = it.take(60) },
                    label = { Text(if (isEnglish) "Dhikr name (e.g. Hasbunallah)" else "যিকরের নাম (যেমন হাসবুনাল্লাহ)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            OutlinedTextField(
                value = targetText, onValueChange = { targetText = it.filter(Char::isDigit).take(8) },
                label = { Text(if (isEnglish) "Family target (count)" else "পরিবারের লক্ষ্য (সংখ্যা)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val title = if (presetIndex == CUSTOM_INDEX) customTitle.trim()
                    else presets[presetIndex].second.let { if (isEnglish) it.first else it.second }
                    viewModel.createFamilyGoal(title, targetText.toIntOrNull() ?: 0)
                    customTitle = ""
                },
                enabled = myName.isNotBlank() && (targetText.toIntOrNull() ?: 0) > 0 &&
                    (presetIndex != CUSTOM_INDEX || customTitle.isNotBlank()),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEnglish) "Create goal" else "লক্ষ্য তৈরি করুন") }
            if (myName.isBlank()) {
                Text(if (isEnglish) "Save your name first." else "আগে আপনার নাম সংরক্ষণ করুন।", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        // --- join by pasted link ---
        GlassCard {
            Text(if (isEnglish) "Join or update from a link" else "লিংক থেকে যোগ দিন বা হালনাগাদ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                value = pasteText, onValueChange = { pasteText = it },
                label = { Text(if (isEnglish) "Paste the link or the whole message" else "লিংক বা পুরো বার্তা পেস্ট করুন") }, maxLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    // Google's code scanner: no camera permission needed, works inside the app.
                    com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context).startScan()
                        .addOnSuccessListener { code -> code.rawValue?.let { viewModel.handleFamilyText(it) } }
                        .addOnFailureListener {
                            viewModel.reportFamilyMessage(
                                if (isEnglish) "Couldn't open the QR scanner. Use Paste instead, or scan with your camera app."
                                else "QR স্ক্যানার খোলা যায়নি। পেস্ট ব্যবহার করুন বা ক্যামেরা অ্যাপ দিয়ে স্ক্যান করুন।"
                            )
                        }
                },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEnglish) "Scan a family QR code" else "পারিবারিক QR কোড স্ক্যান করুন", fontSize = 12.sp) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.let { pasteText = it.trim() }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (isEnglish) "Paste" else "পেস্ট") }
                Button(
                    onClick = { viewModel.handleFamilyText(pasteText); pasteText = "" },
                    enabled = pasteText.isNotBlank(), shape = RoundedCornerShape(12.dp)
                ) { Text(if (isEnglish) "Apply link" else "লিংক প্রয়োগ করুন") }
            }
        }

    }

    pendingJoin?.let { p ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissFamilyJoin() },
            title = { Text(if (isEnglish) "Join family goal?" else "পারিবারিক লক্ষ্যে যোগ দেবেন?") },
            text = {
                Text(
                    if (isEnglish) "${p.createdBy.ifBlank { "A family member" }} invites you to \"${p.title}\" — target ${n(p.target, true)}."
                    else "${p.createdBy.ifBlank { "পরিবারের একজন সদস্য" }} আপনাকে \"${p.title}\" লক্ষ্যে আমন্ত্রণ জানিয়েছেন — লক্ষ্য ${n(p.target, false)}।"
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.acceptFamilyJoin() }) { Text(if (isEnglish) "Join" else "যোগ দিন") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissFamilyJoin() }) { Text(if (isEnglish) "Not now" else "এখন নয়") } }
        )
    }

    qrFor?.let { goal ->
        val link = FamilyLink.build(goal)
        val bmp = remember(link) { qrBitmap(link) }
        AlertDialog(
            onDismissRequest = { qrFor = null },
            title = { Text(goal.title) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bmp.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(260.dp),
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.None
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isEnglish) "Scan with any camera app to open the invite in NoorUp." else "যেকোনো ক্যামেরা অ্যাপ দিয়ে স্ক্যান করলে নূরআপে আমন্ত্রণ খুলবে।",
                        fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("NoorUp family link", link))
                        android.widget.Toast.makeText(context, if (isEnglish) "Link copied" else "লিংক কপি হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text(if (isEnglish) "Copy link" else "লিংক কপি") }
                }
            },
            confirmButton = { TextButton(onClick = { share(context, viewModel.familyInviteShareText(goal, isEnglish)) }) { Text(if (isEnglish) "Share invite" else "আমন্ত্রণ শেয়ার") } },
            dismissButton = { TextButton(onClick = { qrFor = null }) { Text(if (isEnglish) "Close" else "বন্ধ") } }
        )
    }
}

@Composable
private fun GoalCard(viewModel: NoorUpViewModel, goal: FamilyGoal, myName: String, isEnglish: Boolean, onShowQr: () -> Unit) {
    val context = LocalContext.current
    var custom by remember(goal.id) { mutableStateOf("") }
    var confirmDelete by remember(goal.id) { mutableStateOf(false) }
    var editing by remember(goal.id) { mutableStateOf(false) }
    val mine = goal.contributions[myName] ?: 0

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (goal.isComplete) Text("✅", fontSize = 18.sp)
        }
        Text(
            "${n(goal.total, isEnglish)} / ${n(goal.target, isEnglish)}",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(progress = { goal.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        if (goal.contributions.isNotEmpty()) {
            goal.contributions.entries.sortedByDescending { it.value }.forEach { (name, c) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (name == myName) "$name (${if (isEnglish) "you" else "আপনি"})" else name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(n(c, isEnglish), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            if (isEnglish) "My count: ${n(mine, true)}" else "আমার সংখ্যা: ${n(mine, false)}",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(1, 33, 100).forEach { d ->
                OutlinedButton(onClick = { viewModel.addFamilyCount(goal.id, d) }, enabled = myName.isNotBlank(), shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                    Text("+${n(d, isEnglish)}", fontSize = 12.sp)
                }
            }
            OutlinedTextField(
                value = custom, onValueChange = { custom = it.filter(Char::isDigit).take(6) },
                placeholder = { Text("…", fontSize = 12.sp) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(76.dp)
            )
            Button(
                onClick = { custom.toIntOrNull()?.let { viewModel.addFamilyCount(goal.id, it); custom = "" } },
                enabled = myName.isNotBlank() && custom.isNotBlank(), shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) { Text(if (isEnglish) "Add" else "যোগ", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onShowQr, shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                Text(if (isEnglish) "QR / Invite" else "QR / আমন্ত্রণ", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    val text = viewModel.familyProgressShareText(goal, isEnglish)
                    share(context, text)
                },
                enabled = myName.isNotBlank(), shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) { Text(if (isEnglish) "Share my progress" else "আমার প্রগ্রেস শেয়ার", fontSize = 12.sp, maxLines = 1) }
            TextButton(onClick = { editing = true }) { Text(if (isEnglish) "Edit" else "সম্পাদনা", fontSize = 12.sp) }
            TextButton(onClick = { confirmDelete = true }) { Text(if (isEnglish) "Delete" else "মুছুন", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
    if (editing) {
        var titleDraft by remember(goal.id, goal.rev) { mutableStateOf(goal.title) }
        var targetDraft by remember(goal.id, goal.rev) { mutableStateOf(goal.target.toString()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(if (isEnglish) "Edit family goal" else "পারিবারিক লক্ষ্য সম্পাদনা") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleDraft, onValueChange = { titleDraft = it.take(60) },
                        label = { Text(if (isEnglish) "Dhikr name" else "যিকরের নাম") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = targetDraft, onValueChange = { targetDraft = it.filter(Char::isDigit).take(9) },
                        label = { Text(if (isEnglish) "Family target" else "পরিবারের লক্ষ্য") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        if (isEnglish) "Share the invite again so family members get the change." else "পরিবারের সদস্যরা পরিবর্তন পেতে আমন্ত্রণ আবার শেয়ার করুন।",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.editFamilyGoal(goal.id, titleDraft, targetDraft.toIntOrNull() ?: 0); editing = false },
                    enabled = titleDraft.isNotBlank() && (targetDraft.toIntOrNull() ?: 0) > 0
                ) { Text(if (isEnglish) "Save" else "সংরক্ষণ") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(if (isEnglish) "Cancel" else "বাতিল") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isEnglish) "Delete this goal?" else "লক্ষ্যটি মুছবেন?") },
            text = { Text(if (isEnglish) "This removes it from this phone only." else "এটি শুধু এই ফোন থেকে মুছে যাবে।") },
            confirmButton = { TextButton(onClick = { viewModel.deleteFamilyGoal(goal.id); confirmDelete = false }) { Text(if (isEnglish) "Delete" else "মুছুন") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (isEnglish) "Cancel" else "বাতিল") } }
        )
    }
}

@Composable
private fun FamilyCheckInCard(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val checkIns by viewModel.familyCheckIns.collectAsState()
    val myName by viewModel.familyMyName.collectAsState()
    val goals by viewModel.familyGoals.collectAsState()
    val habit by viewModel.habitPrayers.collectAsState()
    val context = LocalContext.current
    val today = viewModel.familyTodayKey()
    val myPrayers = habit.values.count { it }

    androidx.compose.runtime.LaunchedEffect(myPrayers, myName) { viewModel.familyRecordMyCheckIn() }

    val todayMap = checkIns[today].orEmpty()
    val streak = remember(checkIns) { viewModel.familyStreak() }
    val everyoneDone = todayMap.isNotEmpty() && todayMap.values.all { it >= 5 }
    val behind = todayMap.filter { it.value < 5 && it.key != myName }.keys

    GlassCard {
        Text(if (isEnglish) "Family Prayer Check-in" else "পারিবারিক নামাজ চেক-ইন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (isEnglish) "Family streak: ${n(streak, true)} day${if (streak == 1) "" else "s"} 🔥" else "পারিবারিক ধারাবাহিকতা: ${n(streak, false)} দিন 🔥",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        if (todayMap.isEmpty()) {
            Text(
                if (isEnglish) "Save your name above to start checking in. Your own prayers (from Noor Garden) count automatically."
                else "চেক-ইন শুরু করতে উপরে নাম সংরক্ষণ করুন। নূর বাগানের আপনার নামাজ স্বয়ংক্রিয়ভাবে গণনা হয়।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            todayMap.entries.sortedByDescending { it.value }.forEach { (name, c) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (name == myName) "$name (${if (isEnglish) "you" else "আপনি"})" else name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${n(c, isEnglish)}/${n(5, isEnglish)} ${if (c >= 5) "✅" else "🕌"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    everyoneDone -> if (isEnglish) "Masha Allah! Everyone prayed all five today." else "মাশাআল্লাহ! আজ সবাই পাঁচ ওয়াক্ত পড়েছে।"
                    behind.isNotEmpty() -> if (isEnglish) "Gently encourage ${behind.joinToString()} — a kind reminder helps." else "${behind.joinToString()}-কে নম্রভাবে উৎসাহ দিন — একটি সুন্দর স্মরণ করিয়ে দেওয়া সহায়ক।"
                    else -> if (isEnglish) "Keep going — complete your prayers to keep the family streak alive." else "চালিয়ে যান — পারিবারিক ধারাবাহিকতা ধরে রাখতে নামাজ সম্পূর্ণ করুন।"
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary
            )
        }
        if (goals.isNotEmpty() && myName.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { share(context, viewModel.familyCheckInShareText(goals.first(), isEnglish)) },
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEnglish) "Share today's check-in" else "আজকের চেক-ইন শেয়ার করুন", fontSize = 12.sp) }
        }
    }
}
