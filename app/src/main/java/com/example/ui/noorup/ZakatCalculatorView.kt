package com.example.ui.noorup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class NisabBasis {
    SILVER,
    GOLD
}

enum class WeightUnit {
    VORI,
    GRAM
}

enum class FitrahCommodity(
    val nameEn: String,
    val nameBn: String,
    val ratePerPerson: Double,
    val measureEn: String,
    val measureBn: String,
    val iconEmoji: String
) {
    FLOUR("Flour / Wheat", "আটা / গম", 115.0, "1.65 kg (Minimum standard)", "১.৬৫ কেজি (ন্যূনতম মান)", "🌾"),
    BARLEY("Barley (Yob)", "যব", 400.0, "3.3 kg", "৩.৩ কেজি", "🌿"),
    RAISINS("Raisins (Kishmish)", "কিশমিশ", 2100.0, "3.3 kg", "৩.৩ কেজি", "🍇"),
    DATES("Dates (Khejoor)", "খেজুর", 2400.0, "3.3 kg", "৩.৩ কেজি", "🌴"),
    CHEESE("Cheese (Panir)", "পনির", 2970.0, "3.3 kg", "৩.৩ কেজি", "🧀")
}

@Composable
fun ZakatCalculatorView(
    viewModel: NoorUpViewModel,
    modifier: Modifier = Modifier
) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = if (isEnglish) {
                    listOf("Zakat 2.5%", "Fitrah & Sadaqah", "8 Quranic Heads")
                } else {
                    listOf("যাকাত ২.৫%", "ফিতরা ও সদকা", "কুরআনের ৮ খাত")
                }

                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) EmeraldGlow else Color(0xFF047857)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { selectedSubTab = index }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) {
                                Color.White
                            } else {
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedSubTab,
            label = "zakat_tab_transition"
        ) { tab ->
            when (tab) {
                0 -> ZakatCalculatorCoreTab(isEnglish = isEnglish)
                1 -> FitrahAndSadaqahTab(isEnglish = isEnglish)
                2 -> ZakatRulingsAndHeadsTab(isEnglish = isEnglish)
            }
        }
    }
}

@Composable
private fun ZakatCalculatorCoreTab(isEnglish: Boolean) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var nisabBasis by remember { mutableStateOf(NisabBasis.SILVER) }
    var goldRatePerVoriStr by remember { mutableStateOf("130000") }
    var silverRatePerVoriStr by remember { mutableStateOf("2000") }
    var showRatesCustomizer by remember { mutableStateOf(false) }

    var goldUnit by remember { mutableStateOf(WeightUnit.VORI) }
    var goldAmountStr by remember { mutableStateOf("") }

    var silverUnit by remember { mutableStateOf(WeightUnit.VORI) }
    var silverAmountStr by remember { mutableStateOf("") }

    var cashInHandStr by remember { mutableStateOf("") }
    var bankSavingsStr by remember { mutableStateOf("") }
    var businessStockStr by remember { mutableStateOf("") }
    var investmentsSharesStr by remember { mutableStateOf("") }
    var loanReceivablesStr by remember { mutableStateOf("") }

    var shortTermDebtsStr by remember { mutableStateOf("") }
    var pendingBillsExpensesStr by remember { mutableStateOf("") }

    val goldRate = parseNumberSafe(goldRatePerVoriStr).coerceAtLeast(1000.0)
    val silverRate = parseNumberSafe(silverRatePerVoriStr).coerceAtLeast(100.0)

    val goldInput = parseNumberSafe(goldAmountStr)
    val silverInput = parseNumberSafe(silverAmountStr)

    val goldValue = if (goldUnit == WeightUnit.VORI) {
        goldInput * goldRate
    } else {
        (goldInput / 11.664) * goldRate
    }

    val silverValue = if (silverUnit == WeightUnit.VORI) {
        silverInput * silverRate
    } else {
        (silverInput / 11.664) * silverRate
    }

    val cashInHand = parseNumberSafe(cashInHandStr)
    val bankSavings = parseNumberSafe(bankSavingsStr)
    val businessStock = parseNumberSafe(businessStockStr)
    val investments = parseNumberSafe(investmentsSharesStr)
    val loanReceivables = parseNumberSafe(loanReceivablesStr)

    val grossAssets = goldValue + silverValue + cashInHand + bankSavings + businessStock + investments + loanReceivables

    val shortTermDebts = parseNumberSafe(shortTermDebtsStr)
    val pendingBills = parseNumberSafe(pendingBillsExpensesStr)
    val totalLiabilities = shortTermDebts + pendingBills

    val netWealth = maxOf(0.0, grossAssets - totalLiabilities)

    val nisabThreshold = if (nisabBasis == NisabBasis.SILVER) {
        52.5 * silverRate
    } else {
        7.5 * goldRate
    }

    val isZakatObligatory = netWealth >= nisabThreshold && netWealth > 0.0
    val zakatDue = if (isZakatObligatory) netWealth * 0.025 else 0.0
    val monthlyInstallment = zakatDue / 12.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 6.dp else 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = if (isZakatObligatory) EmeraldGlow.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) {
                if (isZakatObligatory) Color(0xFF06281E) else Color(0xFF0F172A)
            } else {
                if (isZakatObligatory) Color(0xFFECFDF5) else Color(0xFFFFFFFF)
            },
            border = BorderStroke(
                1.5.dp,
                if (isZakatObligatory) EmeraldGlow.copy(alpha = 0.6f) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isZakatObligatory) EmeraldGlow.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isZakatObligatory) EmeraldGlow else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isEnglish) "Zakat Obligation Status" else "যাকাত প্রদেয় স্থিতি",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isZakatObligatory) {
                                    if (isEnglish) "Zakat is Fard (Obligatory)" else "যাকাত প্রদেয় (ওয়াজিব/ফরজ)"
                                } else if (grossAssets > 0) {
                                    if (isEnglish) "Below Nisab Threshold" else "নিসাব সীমার নিচে (অব্যাহতি)"
                                } else {
                                    if (isEnglish) "Enter Wealth to Calculate" else "সম্পদের বিবরণ লিখুন"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isZakatObligatory) {
                                    if (isDark) EmeraldLight else Color(0xFF047857)
                                } else {
                                    if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            goldAmountStr = ""
                            silverAmountStr = ""
                            cashInHandStr = ""
                            bankSavingsStr = ""
                            businessStockStr = ""
                            investmentsSharesStr = ""
                            loanReceivablesStr = ""
                            shortTermDebtsStr = ""
                            pendingBillsExpensesStr = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x66000000) else Color(0x66E2E8F0))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Total Zakat Payable (2.5%)" else "মোট প্রদেয় যাকাত (২.৫%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        if (isZakatObligatory) {
                            Text(
                                text = if (isEnglish) "৳${formatCurrency(monthlyInstallment)} / month" else "বা ৳${formatCurrency(monthlyInstallment)} / মাস",
                                fontSize = 11.sp,
                                color = if (isDark) EmeraldLight.copy(alpha = 0.9f) else Color(0xFF047857)
                            )
                        }
                    }

                    Text(
                        text = if (isZakatObligatory) "৳ ${formatCurrency(zakatDue)}" else "৳ 0.00",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isZakatObligatory) {
                            if (isDark) EmeraldGlow else Color(0xFF047857)
                        } else {
                            if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        }
                    )
                }

                if (grossAssets > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ProfessionalSummaryRow(
                            label = if (isEnglish) "Gross Assets" else "মোট স্থাবর/অস্থাবর সম্পদ",
                            value = "৳ ${formatCurrency(grossAssets)}",
                            isDark = isDark
                        )
                        if (totalLiabilities > 0) {
                            ProfessionalSummaryRow(
                                label = if (isEnglish) "Allowable Debts Deducted" else "বাদযোগ্য তাৎক্ষণিক ঋণ",
                                value = "- ৳ ${formatCurrency(totalLiabilities)}",
                                isDark = isDark,
                                isNegative = true
                            )
                        }
                        ProfessionalSummaryRow(
                            label = if (isEnglish) "Net Zakatable Wealth" else "নিট যাকাতযোগ্য সম্পদ",
                            value = "৳ ${formatCurrency(netWealth)}",
                            isDark = isDark,
                            isHighlighted = true
                        )
                        ProfessionalSummaryRow(
                            label = if (isEnglish) "Nisab Benchmark (${if (nisabBasis == NisabBasis.SILVER) "Silver" else "Gold"})" else "নিসাব মান (${if (nisabBasis == NisabBasis.SILVER) "রৌপ্য" else "স্বর্ণ"})",
                            value = "৳ ${formatCurrency(nisabThreshold)}",
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                val summaryText = buildString {
                                    appendLine(if (isEnglish) "🕌 NoorUp Zakat Calculation Summary" else "🕌 নূরআপ যাকাত হিসাব বিবরণী")
                                    appendLine("--------------------------------")
                                    appendLine(if (isEnglish) "Gross Assets: ৳${formatCurrency(grossAssets)}" else "মোট সম্পদ: ৳${formatCurrency(grossAssets)}")
                                    appendLine(if (isEnglish) "Deductible Liabilities: ৳${formatCurrency(totalLiabilities)}" else "বাদযোগ্য ঋণ: ৳${formatCurrency(totalLiabilities)}")
                                    appendLine(if (isEnglish) "Net Wealth: ৳${formatCurrency(netWealth)}" else "নিট যাকাতযোগ্য সম্পদ: ৳${formatCurrency(netWealth)}")
                                    appendLine(if (isEnglish) "Nisab Standard (${if (nisabBasis == NisabBasis.SILVER) "Silver" else "Gold"}): ৳${formatCurrency(nisabThreshold)}" else "নিসাব মান: ৳${formatCurrency(nisabThreshold)}")
                                    appendLine(if (isEnglish) "Zakat Due (2.5%): ৳${formatCurrency(zakatDue)}" else "প্রদেয় যাকাত (২.৫%): ৳${formatCurrency(zakatDue)}")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Zakat Summary", summaryText))
                                Toast.makeText(context, if (isEnglish) "Summary copied to clipboard!" else "হিসাবের বিবরণ কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEnglish) "Copy Calculation Breakdown" else "হিসাব বিবরণী কপি করুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Nisab Benchmark Standard" else "নিসাব ভিত্তি মান ও বাজারদর",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isEnglish) "Silver threshold recommended for mixed wealth" else "নগদ টাকা ও মিশ্র সম্পদের জন্য রৌপ্য মান সতর্কতামূলক",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    TextButton(
                        onClick = { showRatesCustomizer = !showRatesCustomizer },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isDark) EmeraldLight else Color(0xFF047857)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showRatesCustomizer) (if (isEnglish) "Hide" else "লুকান") else (if (isEnglish) "Edit Rates" else "দর সংশোধন"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) EmeraldLight else Color(0xFF047857)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NisabOptionCard(
                        title = if (isEnglish) "Silver Nisab (52.5 Vori)" else "রৌপ্য নিসাব (৫২.৫ ভরি)",
                        subtitle = "612.36 grams",
                        amountText = "৳ ${formatCurrency(52.5 * silverRate)}",
                        isRecommended = true,
                        isSelected = nisabBasis == NisabBasis.SILVER,
                        onClick = { nisabBasis = NisabBasis.SILVER },
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    NisabOptionCard(
                        title = if (isEnglish) "Gold Nisab (7.5 Vori)" else "স্বর্ণ নিসাব (৭.৫ ভরি)",
                        subtitle = "87.48 grams",
                        amountText = "৳ ${formatCurrency(7.5 * goldRate)}",
                        isRecommended = false,
                        isSelected = nisabBasis == NisabBasis.GOLD,
                        onClick = { nisabBasis = NisabBasis.GOLD },
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(visible = showRatesCustomizer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Adjust Local Market Rates (৳ per Vori / 11.664g)" else "স্থানীয় বাজারদর পরিবর্তন (প্রতি ভরি / ১১.৬৬৪ গ্রাম)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfessionalInputField(
                                value = goldRatePerVoriStr,
                                onValueChange = { goldRatePerVoriStr = it },
                                label = if (isEnglish) "Gold Rate (৳/Vori)" else "স্বর্ণের দর (৳/ভরি)",
                                icon = Icons.Default.MonetizationOn,
                                iconTint = Color(0xFFF59E0B),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )

                            ProfessionalInputField(
                                value = silverRatePerVoriStr,
                                onValueChange = { silverRatePerVoriStr = it },
                                label = if (isEnglish) "Silver Rate (৳/Vori)" else "রৌপ্যের দর (৳/ভরি)",
                                icon = Icons.Default.Stars,
                                iconTint = Color(0xFF94A3B8),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) EmeraldGlow.copy(alpha = 0.2f) else Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("১", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) EmeraldLight else Color(0xFF047857))
                    }
                    Text(
                        text = if (isEnglish) "1. Zakatable Assets & Savings" else "১. যাকাতযোগ্য সম্পদ ও সঞ্চয়",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfessionalInputField(
                            value = goldAmountStr,
                            onValueChange = { goldAmountStr = it },
                            label = if (isEnglish) "Gold (${if (goldUnit == WeightUnit.VORI) "Vori" else "Grams"})" else "স্বর্ণ (${if (goldUnit == WeightUnit.VORI) "ভরি" else "গ্রাম"})",
                            icon = Icons.Default.MonetizationOn,
                            iconTint = Color(0xFFF59E0B),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    goldUnit = if (goldUnit == WeightUnit.VORI) WeightUnit.GRAM else WeightUnit.VORI
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (goldUnit == WeightUnit.VORI) "ভরি" else "গ্রাম",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) EmeraldLight else Color(0xFF047857)
                                )
                            }
                        }
                    }
                    if (goldValue > 0) {
                        Text(
                            text = "↳ ${if (isEnglish) "Estimated Value" else "আনুমানিক মূল্য"}: ৳${formatCurrency(goldValue)}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfessionalInputField(
                            value = silverAmountStr,
                            onValueChange = { silverAmountStr = it },
                            label = if (isEnglish) "Silver (${if (silverUnit == WeightUnit.VORI) "Vori" else "Grams"})" else "রৌপ্য (${if (silverUnit == WeightUnit.VORI) "ভরি" else "গ্রাম"})",
                            icon = Icons.Default.Stars,
                            iconTint = Color(0xFF94A3B8),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    silverUnit = if (silverUnit == WeightUnit.VORI) WeightUnit.GRAM else WeightUnit.VORI
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (silverUnit == WeightUnit.VORI) "ভরি" else "গ্রাম",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) EmeraldLight else Color(0xFF047857)
                                )
                            }
                        }
                    }
                    if (silverValue > 0) {
                        Text(
                            text = "↳ ${if (isEnglish) "Estimated Value" else "আনুমানিক মূল্য"}: ৳${formatCurrency(silverValue)}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfessionalInputField(
                        value = cashInHandStr,
                        onValueChange = { cashInHandStr = it },
                        label = if (isEnglish) "Cash in Hand (৳)" else "হাতে নগদ টাকা (৳)",
                        icon = Icons.Default.Payments,
                        iconTint = Color(0xFF10B981),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    ProfessionalInputField(
                        value = bankSavingsStr,
                        onValueChange = { bankSavingsStr = it },
                        label = if (isEnglish) "Bank / FDR (৳)" else "ব্যাংক / সঞ্চয় (৳)",
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFF3B82F6),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfessionalInputField(
                        value = businessStockStr,
                        onValueChange = { businessStockStr = it },
                        label = if (isEnglish) "Business Stock (৳)" else "ব্যবসার পণ্য/স্টক (৳)",
                        icon = Icons.Default.Storefront,
                        iconTint = Color(0xFF8B5CF6),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    ProfessionalInputField(
                        value = investmentsSharesStr,
                        onValueChange = { investmentsSharesStr = it },
                        label = if (isEnglish) "Shares & Bonds (৳)" else "শেয়ার ও প্রাইজবন্ড (৳)",
                        icon = Icons.Default.TrendingUp,
                        iconTint = Color(0xFFEC4899),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                ProfessionalInputField(
                    value = loanReceivablesStr,
                    onValueChange = { loanReceivablesStr = it },
                    label = if (isEnglish) "Recoverable Loans / Receivables (৳)" else "ফেরতযোগ্য পাওনা টাকা (৳)",
                    icon = Icons.Default.ReceiptLong,
                    iconTint = Color(0xFF14B8A6),
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("২", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                    Text(
                        text = if (isEnglish) "2. Deductible Immediate Liabilities" else "২. বাদযোগ্য তাৎক্ষণিক দেনা ও দায়",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfessionalInputField(
                        value = shortTermDebtsStr,
                        onValueChange = { shortTermDebtsStr = it },
                        label = if (isEnglish) "Debts Due Now (৳)" else "প্রদেয় ঋণ/দেনা (৳)",
                        icon = Icons.Default.MoneyOff,
                        iconTint = Color(0xFFEF4444),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    ProfessionalInputField(
                        value = pendingBillsExpensesStr,
                        onValueChange = { pendingBillsExpensesStr = it },
                        label = if (isEnglish) "Unpaid Bills (৳)" else "বকেয়া বিল/বেতন (৳)",
                        icon = Icons.Default.PendingActions,
                        iconTint = Color(0xFFF97316),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FitrahAndSadaqahTab(isEnglish: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var familyMembersStr by remember { mutableStateOf("4") }
    var selectedCommodity by remember { mutableStateOf(FitrahCommodity.FLOUR) }

    var monthlyIncomeStr by remember { mutableStateOf("") }
    var sadaqahPercentage by remember { mutableFloatStateOf(2.5f) }

    val members = parseNumberSafe(familyMembersStr).toInt().coerceAtLeast(1)
    val totalFitrah = members * selectedCommodity.ratePerPerson

    val monthlyIncome = parseNumberSafe(monthlyIncomeStr)
    val monthlySadaqah = (monthlyIncome * (sadaqahPercentage / 100.0))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) EmeraldGlow.copy(alpha = 0.2f) else Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌾", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = if (isEnglish) "Sadaqatul Fitr Calculator" else "সদকাতুল ফিতর ক্যালকুলেটর",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isEnglish) "Due before Eid prayer for all family members" else "ঈদের নামাজের পূর্বে পরিবারের প্রত্যেকের পক্ষ থেকে প্রদেয়",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                ProfessionalInputField(
                    value = familyMembersStr,
                    onValueChange = { familyMembersStr = it },
                    label = if (isEnglish) "Number of Family Members (Persons)" else "পরিবারের সদস্য সংখ্যা (জন)",
                    icon = Icons.Default.People,
                    iconTint = if (isDark) EmeraldLight else Color(0xFF047857),
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isEnglish) "Select Commodity Standard:" else "ফিতরার খাদ্যদ্রব্য ও ইসলামিক ফাউন্ডেশন মান:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitrahCommodity.values().forEach { commodity ->
                        val isSelected = selectedCommodity == commodity
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCommodity = commodity },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (isDark) EmeraldGlow.copy(alpha = 0.15f) else Color(0xFFECFDF5)
                            } else {
                                if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                1.2.dp,
                                if (isSelected) EmeraldGlow else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(commodity.iconEmoji, fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = if (isEnglish) commodity.nameEn else commodity.nameBn,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (isDark) EmeraldLight else Color(0xFF047857)
                                            } else {
                                                if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                            }
                                        )
                                        Text(
                                            text = if (isEnglish) commodity.measureEn else commodity.measureBn,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                Text(
                                    text = "৳ ${commodity.ratePerPerson.toInt()} /জন",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) EmeraldGlow else Color(0xFF047857)
                                    } else {
                                        if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    }
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF06281E) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Total Fitrah Due ($members persons)" else "মোট প্রদেয় ফিতরা ($members জনের জন্য)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = if (isEnglish) "${selectedCommodity.nameEn} rate" else "${selectedCommodity.nameBn} হারে",
                                fontSize = 11.sp,
                                color = if (isDark) EmeraldLight else Color(0xFF047857)
                            )
                        }

                        Text(
                            text = "৳ ${formatCurrency(totalFitrah)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) EmeraldGlow else Color(0xFF047857)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤲", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = if (isEnglish) "Voluntary Sadaqah Budgeting" else "নফল সদকা ও নিয়মিত দান লক্ষ্যমাত্রা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isEnglish) "Build consistent charity habits from your monthly income" else "আপনার মাসিক আয় থেকে নিয়মিত দানের একটি সুন্দর অভ্যাস গড়ে তুলুন",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                ProfessionalInputField(
                    value = monthlyIncomeStr,
                    onValueChange = { monthlyIncomeStr = it },
                    label = if (isEnglish) "Monthly Income (৳)" else "মাসিক মোট আয় (৳)",
                    icon = Icons.Default.MonetizationOn,
                    iconTint = Color(0xFF3B82F6),
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )

                if (monthlyIncome > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Charity Budget Ratio" else "দানের অনুপাত শতকরা:",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "${String.format("%.1f", sadaqahPercentage)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }

                        Slider(
                            value = sadaqahPercentage,
                            onValueChange = { sadaqahPercentage = it },
                            valueRange = 1f..10f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF172554) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Recommended Monthly Sadaqah:" else "প্রস্তাবিত মাসিক সদকা লক্ষ্য:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "৳ ${formatCurrency(monthlySadaqah)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = if (isEnglish) {
                            "✨ Prophet Muhammad (ﷺ) said: \"Charity does not decrease wealth, and Allah increases the honor of who forgives...\" (Sahih Muslim 2588)"
                        } else {
                            "✨ রাসুলুল্লাহ (ﷺ) বলেছেন: \"সদকা সম্পদ কমায় না, বরং ক্ষমা প্রদর্শনকারীকে আল্লাহ সম্মান বৃদ্ধি করে দেন...\" (সহীহ মুসলিম ২৫৮৮)"
                        },
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZakatRulingsAndHeadsTab(isEnglish: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val heads = listOf(
        Pair("১. ফুকারা (দরিদ্র)", "যার কোনো সম্পদ বা জীবিকার সংস্থান নেই।"),
        Pair("২. মাসাকিন (অসহায় / অভাবী)", "যার কিছু আয় আছে কিন্তু মৌলিক প্রয়োজন মেটানোর জন্য অপর্যাপ্ত।"),
        Pair("৩. আল-আ'মিলীন (যাকাত সংগ্রাহক)", "যাকাত সংগ্রহ ও বিতরণে নিয়োজিত ব্যক্তিগণ।"),
        Pair("৪. মুআল্লাফাতুল কুলুব (চিত্তাকর্ষক)", "ইসলামের প্রতি অনুরাগী বা নওমুসলিমদের সহায়তা।"),
        Pair("৫. আর-রিকাব (দাসত্ব ও বন্দিদশা মুক্তি)", "অন্যায় বন্দিদশা বা দাসত্ব থেকে মুক্ত করার জন্য।"),
        Pair("৬. আল-গারিমীন (ঋণগ্রস্ত ব্যক্তি)", "যে ব্যক্তি বৈধ প্রয়োজনে ঋণগ্রস্ত হয়ে পরিশোধে অক্ষম।"),
        Pair("৭. ফী সাবিলিল্লাহ (আল্লাহর পথে)", "দ্বীনের প্রচার, ইসলামী দাওয়াত ও শিক্ষা কার্যক্রমে।"),
        Pair("৮. ইবনুস সাবিল (মুসাফির)", "সফর অবস্থায় সহায়-সম্বলহীন হয়ে পড়া পথিক।")
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (isDark) EmeraldLight else Color(0xFF047857),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEnglish) "8 Quranic Categories (Surah At-Tawbah 9:60)" else "কুরআনে বর্ণিত যাকাতের ৮টি খাত (সূরা তাওবাহ্ : ৬০)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }

                Text(
                    text = if (isEnglish) "Allah has strictly defined the eight legitimate recipients of Zakat in the Holy Quran:"
                    else "আল্লাহ তাআলা পবিত্র কুরআনে যাকাত ব্যয়ের জন্য সুনির্দিষ্ট ৮টি খাত নির্ধারণ করে দিয়েছেন:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    heads.forEachIndexed { index, pair ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) EmeraldGlow.copy(alpha = 0.2f) else Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) EmeraldLight else Color(0xFF047857)
                                    )
                                }
                                Column {
                                    Text(
                                        pair.first,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        pair.second,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF450A0A).copy(alpha = 0.3f) else Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEnglish) "Who CANNOT Receive Zakat" else "যাদের যাকাত দেওয়া সম্পূর্ণ নিষেধ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444)
                    )
                }

                val exclusions = if (isEnglish) {
                    listOf(
                        "• Direct ascendants: Parents, Grandparents",
                        "• Direct descendants: Children, Grandchildren",
                        "• Husband and Wife to each other",
                        "• Wealthy persons possessing Nisab threshold",
                        "• Descendants of Prophet Muhammad ﷺ (Banu Hashim)",
                        "• Mosque construction / general charity projects (requires personal ownership transfer)"
                    )
                } else {
                    listOf(
                        "• নিজের মূল উৎস: পিতা, মাতা, দাদা, দাদী, নানা, নানী",
                        "• নিজের ঔরসজাত শাখা: পুত্র, কন্যা, নাতি, নাতনী",
                        "• স্বামী ও স্ত্রী একে অপরকে",
                        "• নিসাব পরিমাণ সম্পদের মালিক ধনী ব্যক্তি",
                        "• রাসুলুল্লাহ (ﷺ)-এর বংশধর (সৈয়দ / বনু হাশিম পরিবার)",
                        "• মসজিদ নির্মাণ, রাস্তাঘাট বা কবরস্থান সংস্কার (যাকাতে ব্যক্তির মালিকানা হস্তান্তর আবশ্যক)"
                    )
                }

                exclusions.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NisabOptionCard(
    title: String,
    subtitle: String,
    amountText: String,
    isRecommended: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            if (isDark) EmeraldGlow.copy(alpha = 0.15f) else Color(0xFFECFDF5)
        } else {
            if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
        },
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) EmeraldGlow else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        if (isDark) EmeraldLight else Color(0xFF047857)
                    } else {
                        if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isRecommended) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldGlow)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("★", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Text(
                text = amountText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    if (isDark) EmeraldGlow else Color(0xFF047857)
                } else {
                    if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                }
            )
        }
    }
}

@Composable
private fun ProfessionalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDark) EmeraldLight else Color(0xFF047857),
            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
            focusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFFFFFFF),
            unfocusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFF8FAFC),
            focusedLabelColor = if (isDark) EmeraldLight else Color(0xFF047857),
            unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        ),
        modifier = modifier
    )
}

@Composable
private fun ProfessionalSummaryRow(
    label: String,
    value: String,
    isDark: Boolean,
    isHighlighted: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHighlighted) {
                if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            } else {
                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            }
        )

        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isNegative) {
                Color(0xFFEF4444)
            } else if (isHighlighted) {
                if (isDark) EmeraldLight else Color(0xFF047857)
            } else {
                if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            }
        )
    }
}

private fun parseNumberSafe(input: String): Double {
    if (input.isBlank()) return 0.0
    val normalized = input.trim()
        .replace('০', '0')
        .replace('১', '1')
        .replace('২', '2')
        .replace('৩', '3')
        .replace('৪', '4')
        .replace('৫', '5')
        .replace('৬', '6')
        .replace('৭', '7')
        .replace('৮', '8')
        .replace('৯', '9')
        .replace(",", "")
    return normalized.toDoubleOrNull() ?: 0.0
}

private fun formatCurrency(amount: Double): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        formatter.format(amount)
    } catch (e: Exception) {
        String.format("%.2f", amount)
    }
}
