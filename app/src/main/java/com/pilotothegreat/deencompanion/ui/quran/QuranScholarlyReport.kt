package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.QuranReaderKey
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScholarlyReportSheet(
    currentSurahId: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val surahs = remember { QuranHelper.getSurahs(context) }
    val currentSurah = remember(currentSurahId, surahs) {
        if (currentSurahId != null) surahs.firstOrNull { it.id == currentSurahId } else null
    }
    val sajdahVerses = remember { QuranHelper.getSajdahVerses(context) }

    val direction = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val goldAccent = Color(0xFFD4AF37)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet Header
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
                                .background(goldAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (lang == "ar") "تقرير القرآن الكريم الشامل" else "Scholarly Quran Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == "ar") "تحليل شامل لبنية المصحف والمواضع العلمية" else "Comprehensive analytical metrics & Mushaf structure",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Item 1: Primary Structural Stat Cards (2x2 Grid)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (lang == "ar") "إحصائيات هيكل المصحف" else "Mushaf Structural Totals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = goldAccent
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (lang == "ar") "السور" else "Surahs",
                                    value = if (lang == "ar") toArabicNumerals(114) else "114",
                                    subtitle = if (lang == "ar") "سورة مباركة" else "Total Surahs"
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (lang == "ar") "الآيات" else "Ayahs",
                                    value = if (lang == "ar") toArabicNumerals(6236) else "6,236",
                                    subtitle = if (lang == "ar") "آية كريمة" else "Total Verses"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (lang == "ar") "الأجزاء" else "Juz",
                                    value = if (lang == "ar") toArabicNumerals(30) else "30",
                                    subtitle = if (lang == "ar") "جزءاً شاهداً" else "Standard Juz"
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (lang == "ar") "الصفحات" else "Pages",
                                    value = if (lang == "ar") toArabicNumerals(604) else "604",
                                    subtitle = if (lang == "ar") "صفحة بالرسم العثماني" else "Madinah Hafs Pages"
                                )
                            }
                        }
                    }

                    // Item 2: Meccan vs Medinan Classification
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (lang == "ar") "تصنيف نزول السور (مكية ومدنية)" else "Revelation Classification (Meccan & Medinan)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (lang == "ar") "سور مكية: 86 (75.4%)" else "Meccan: 86 Surahs (75.4%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (lang == "ar") "سور مدنية: 28 (24.6%)" else "Medinan: 28 Surahs (24.6%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 86f / 114f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Item 3: Active Surah Scholarly Breakdown (if available)
                    if (currentSurah != null) {
                        item {
                            val surahSajdahs = currentSurah.verses.filter { QuranHelper.isSajdahVerse(currentSurah.id, it.id) }
                            val startPage = QuranHelper.getMushafPageNumber(currentSurah.id, 1)
                            val endPage = QuranHelper.getMushafPageNumber(currentSurah.id, currentSurah.totalVerses)
                            val juzStart = getJuzNumber(currentSurah.id, 1)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, goldAccent.copy(alpha = 0.4f), MaterialTheme.shapes.medium),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (lang == "ar") "تحليل سورة ${currentSurah.name}" else "Analysis of Surah ${currentSurah.transliteration}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = goldAccent
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            val typeText = if (lang == "ar") {
                                                if (currentSurah.type.lowercase() == "meccan") "مكية" else "مدنية"
                                            } else {
                                                currentSurah.type.replaceFirstChar { it.uppercase() }
                                            }
                                            Text(
                                                text = typeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = goldAccent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val countText = if (lang == "ar") toArabicNumerals(currentSurah.totalVerses) else currentSurah.totalVerses.toString()
                                    val juzText = if (lang == "ar") toArabicNumerals(juzStart) else juzStart.toString()
                                    val pagesText = if (lang == "ar") "${toArabicNumerals(startPage)} - ${toArabicNumerals(endPage)}" else "$startPage - $endPage"

                                    Text(
                                        text = if (lang == "ar") "• عدد الآيات: $countText آية" else "• Total Verses: $countText Ayahs",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (lang == "ar") "• الجزء: $juzText" else "• Juz: $juzText",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (lang == "ar") "• الصفحات بالمصحف: $pagesText" else "• Mushaf Pages: $pagesText",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (surahSajdahs.isNotEmpty()) {
                                        Text(
                                            text = if (lang == "ar") "• آيات السجود بهذه السورة: ${surahSajdahs.size} سجدة" else "• Sajdah Verses in Surah: ${surahSajdahs.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = goldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: Interactive Sajdah Verses Directory (14 Sajdahs)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "ar") "دليل مواضع السجدات (14 سجدة)" else "Sajdah Verses Directory (14 Sajdahs)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = goldAccent
                                )
                                Text(
                                    text = if (lang == "ar") "انقر للانتقال" else "Tap to Jump",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            sajdahVerses.forEach { (surah, verse) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigator.goTo(QuranReaderKey(surah.id, surah.transliteration, scrollToVerse = verse.id))
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "۩",
                                                color = goldAccent,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Column {
                                                Text(
                                                    text = if (lang == "ar") "سورة ${surah.name} (آية ${toArabicNumerals(verse.id)})" else "Surah ${surah.transliteration} (Ayah ${verse.id})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val page = QuranHelper.getMushafPageNumber(surah.id, verse.id)
                                                Text(
                                                    text = if (lang == "ar") "صفحة ${toArabicNumerals(page)}" else "Page $page",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = goldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
