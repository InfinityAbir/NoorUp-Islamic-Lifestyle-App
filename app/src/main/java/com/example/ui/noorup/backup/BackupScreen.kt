package com.example.ui.noorup.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.noorup.GlassCard
import com.example.ui.noorup.NoorUpViewModel
import com.example.ui.noorup.toBanglaDigits
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Returns a function that obtains a Drive appDataFolder access token (showing Google's consent
 * screen when needed) and passes it to [onToken].
 */
@Composable
private fun rememberDriveAuth(onToken: (String) -> Unit, onError: (BackupMessage) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) { onError(BackupMessage.AUTH); return@rememberLauncherForActivityResult }
        try {
            val auth = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
            auth.accessToken?.let(onToken) ?: onError(BackupMessage.AUTH)
        } catch (e: ApiException) {
            onError(BackupMessage.AUTH)
        }
    }
    return {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveAppData.SCOPE)))
            .build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { auth ->
                val pi = auth.pendingIntent
                if (auth.hasResolution() && pi != null) {
                    launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    auth.accessToken?.let(onToken) ?: onError(BackupMessage.AUTH)
                }
            }
            .addOnFailureListener { e ->
                // Status 10 (DEVELOPER_ERROR) = no OAuth client registered for this package + signing key.
                val code = (e as? ApiException)?.statusCode
                onError(if (code == 10 || code == 12500) BackupMessage.NOT_CONFIGURED else BackupMessage.AUTH)
            }
    }
}

@Composable
fun BackupRestoreView(viewModel: NoorUpViewModel) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val manager = viewModel.backup
    val state by manager.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var confirmRestoreWith by remember { mutableStateOf<(() -> Unit)?>(null) }
    val passOk = pass.length >= 8

    val signIn = rememberDriveAuth(
        onToken = { token -> pendingDrive?.invoke(token); pendingDrive = null },
        onError = { pendingDrive = null; manager.report(it) }
    )
    fun withDriveToken(action: (String) -> Unit) { pendingDrive = action; signIn() }

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch { manager.saveToFile(uri, pass.toCharArray()) }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestoreWith = { scope.launch { manager.restoreFromFile(uri, pass.toCharArray()) } }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text(if (isEnglish) "Backup & Restore" else "ব্যাকআপ ও রিস্টোর", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (isEnglish) "Saves your bookmarks, Hifz progress, Qada and prayer habit history, Tasbih targets and counts, Ramadan and family trackers, and settings. It is encrypted on this phone with your passphrase before it leaves, and stored in a private Google Drive area only this app can open."
                else "আপনার বুকমার্ক, হিফজ অগ্রগতি, কাযা ও নামাজের অভ্যাসের ইতিহাস, তাসবীহ লক্ষ্য ও সংখ্যা, রমজান ও পারিবারিক ট্র্যাকার এবং সেটিংস সংরক্ষণ করে। বাইরে যাওয়ার আগে আপনার পাসফ্রেজ দিয়ে এই ফোনেই এনক্রিপ্ট হয় এবং গুগল ড্রাইভের একটি ব্যক্তিগত এলাকায় থাকে যা শুধু এই অ্যাপ খুলতে পারে।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = pass, onValueChange = { pass = it.take(64) },
                label = { Text(if (isEnglish) "Backup passphrase (8+ characters)" else "ব্যাকআপ পাসফ্রেজ (৮+ অক্ষর)") },
                singleLine = true,
                visualTransformation = if (showPass) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = showPass, onCheckedChange = { showPass = it })
                Text(if (isEnglish) "Show passphrase" else "পাসফ্রেজ দেখান", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                if (isEnglish) "Only you know this passphrase. It is never stored or sent, so a forgotten passphrase cannot be recovered."
                else "এই পাসফ্রেজ শুধু আপনিই জানেন। এটি কখনো সংরক্ষণ বা পাঠানো হয় না, তাই ভুলে গেলে ফিরে পাওয়া যায় না।",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary
            )
        }

        GlassCard {
            val last = state.lastBackupMs
            Text(
                if (last > 0) {
                    val t = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(last))
                    (if (isEnglish) "Last Drive backup: " else "সর্বশেষ ড্রাইভ ব্যাকআপ: ") + (if (isEnglish) t else t.toBanglaDigits())
                } else (if (isEnglish) "No Drive backup yet" else "এখনো কোনো ড্রাইভ ব্যাকআপ নেই"),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { withDriveToken { token -> scope.launch { manager.backupToDrive(token, pass.toCharArray()) } } },
                enabled = passOk && !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (isEnglish) "Backup to Google Drive" else "গুগল ড্রাইভে ব্যাকআপ")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    confirmRestoreWith = {
                        withDriveToken { token -> scope.launch { manager.restoreFromDrive(token, pass.toCharArray()) } }
                    }
                },
                enabled = passOk && !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (isEnglish) "Restore data" else "ডেটা রিস্টোর")
            }
            if (state.busy) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isEnglish) "Working…" else "কাজ চলছে…", fontSize = 12.sp)
                }
            }
            state.message.takeIf { it != BackupMessage.NONE }?.let { m ->
                Spacer(Modifier.height(8.dp))
                val ok = m == BackupMessage.BACKUP_OK || m == BackupMessage.RESTORE_OK || m == BackupMessage.FILE_SAVED
                Text(
                    messageText(m, isEnglish), fontSize = 12.sp,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        GlassCard {
            Text(if (isEnglish) "Backup file (no Google needed)" else "ব্যাকআপ ফাইল (গুগল ছাড়া)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            Text(
                if (isEnglish) "Same encrypted backup, saved to a file you choose (for example on your phone or a USB drive)."
                else "একই এনক্রিপ্টেড ব্যাকআপ, আপনার পছন্দের ফাইলে (যেমন ফোন বা ইউএসবি ড্রাইভে) সংরক্ষণ।",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { createFile.launch("noorup_backup.enc") }, enabled = passOk && !state.busy, shape = RoundedCornerShape(12.dp)) {
                    Text(if (isEnglish) "Save to file" else "ফাইলে সংরক্ষণ", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }, enabled = passOk && !state.busy, shape = RoundedCornerShape(12.dp)) {
                    Text(if (isEnglish) "Restore from file" else "ফাইল থেকে রিস্টোর", fontSize = 12.sp)
                }
            }
        }
    }

    confirmRestoreWith?.let { go ->
        AlertDialog(
            onDismissRequest = { confirmRestoreWith = null },
            title = { Text(if (isEnglish) "Replace data on this phone?" else "এই ফোনের ডেটা বদলাবেন?") },
            text = {
                Text(
                    if (isEnglish) "Restoring overwrites the matching bookmarks, progress and settings on this phone with the backup. The app will restart."
                    else "রিস্টোর করলে এই ফোনের মিলে যাওয়া বুকমার্ক, অগ্রগতি ও সেটিংস ব্যাকআপ দিয়ে বদলে যাবে। অ্যাপ আবার চালু হবে।"
                )
            },
            confirmButton = { TextButton(onClick = { confirmRestoreWith = null; go() }) { Text(if (isEnglish) "Restore" else "রিস্টোর") } },
            dismissButton = { TextButton(onClick = { confirmRestoreWith = null }) { Text(if (isEnglish) "Cancel" else "বাতিল") } }
        )
    }

    if (state.restartNeeded) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (isEnglish) "Data restored" else "ডেটা রিস্টোর হয়েছে") },
            text = { Text(if (isEnglish) "Restart NoorUp to load your restored data." else "রিস্টোর করা ডেটা লোড করতে নূরআপ আবার চালু করুন।") },
            confirmButton = {
                TextButton(onClick = {
                    (context as? Activity)?.let { com.example.RestartActivity.restartApp(it) }
                }) { Text(if (isEnglish) "Restart now" else "এখনই চালু করুন") }
            }
        )
    }
}

// Holder for the action waiting on the Google consent screen result.
private var pendingDrive: ((String) -> Unit)? = null

private fun messageText(m: BackupMessage, en: Boolean): String = when (m) {
    BackupMessage.BACKUP_OK -> if (en) "Backup saved to Google Drive." else "গুগল ড্রাইভে ব্যাকআপ সংরক্ষিত হয়েছে।"
    BackupMessage.RESTORE_OK -> if (en) "Data restored." else "ডেটা রিস্টোর হয়েছে।"
    BackupMessage.FILE_SAVED -> if (en) "Backup file saved." else "ব্যাকআপ ফাইল সংরক্ষিত হয়েছে।"
    BackupMessage.NO_BACKUP -> if (en) "No backup found in this Google account." else "এই গুগল অ্যাকাউন্টে কোনো ব্যাকআপ পাওয়া যায়নি।"
    BackupMessage.WRONG_PASSPHRASE -> if (en) "Wrong passphrase, or the backup is damaged." else "ভুল পাসফ্রেজ, অথবা ব্যাকআপ ক্ষতিগ্রস্ত।"
    BackupMessage.NETWORK -> if (en) "Couldn't reach Google Drive. Check your internet connection and try again." else "গুগল ড্রাইভে যুক্ত হওয়া যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।"
    BackupMessage.AUTH -> if (en) "Google sign-in or Drive permission was not granted." else "গুগল সাইন-ইন বা ড্রাইভ অনুমতি দেওয়া হয়নি।"
    BackupMessage.NOT_CONFIGURED -> if (en) "Google Drive sign-in isn't set up for this build yet. Use the backup file option meanwhile." else "এই বিল্ডে গুগল ড্রাইভ সাইন-ইন এখনো সেট করা হয়নি। আপাতত ব্যাকআপ ফাইল ব্যবহার করুন।"
    BackupMessage.FILE_ERROR -> if (en) "That file couldn't be read as a NoorUp backup." else "ফাইলটি নূরআপ ব্যাকআপ হিসেবে পড়া যায়নি।"
    BackupMessage.NONE -> ""
}
