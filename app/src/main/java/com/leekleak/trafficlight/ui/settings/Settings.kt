package com.leekleak.trafficlight.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.px
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun Settings(paddingValues: PaddingValues) {
    val viewModel: SettingsVM = koinViewModel()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    var showCalcMenu by remember { mutableStateOf(false) }
    var showAsrMenu by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .hazeSource(hazeState),
        contentPadding = paddingValues
    ) {
        // --- Prayer Times calculation settings ---
        categoryTitleSmall { "Prayer Calculations" }
        item {
            val calcMethod by viewModel.calcMethod.collectAsState()
            Box(modifier = Modifier.fillMaxWidth()) {
                Preference(
                    title = "Calculation Method",
                    summary = when (calcMethod) {
                        PrayerTimeCalculator.CalculationMethod.MWL -> "Muslim World League"
                        PrayerTimeCalculator.CalculationMethod.ISNA -> "Islamic Society of North America"
                        PrayerTimeCalculator.CalculationMethod.EGYPT -> "Egyptian General Authority of Survey"
                        PrayerTimeCalculator.CalculationMethod.MAKKAH -> "Umm al-Qura University, Makkah"
                        PrayerTimeCalculator.CalculationMethod.KARACHI -> "University of Islamic Sciences, Karachi"
                        PrayerTimeCalculator.CalculationMethod.JAFARI -> "Shia Ithna Ashari (Ja'fari)"
                        PrayerTimeCalculator.CalculationMethod.TEHRAN -> "Institute of Geophysics, University of Tehran"
                    },
                    icon = painterResource(R.drawable.clock),
                    onClick = { showCalcMenu = true }
                )
                DropdownMenu(
                    expanded = showCalcMenu,
                    onDismissRequest = { showCalcMenu = false }
                ) {
                    PrayerTimeCalculator.CalculationMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name) },
                            onClick = {
                                viewModel.setCalcMethod(method)
                                showCalcMenu = false
                            }
                        )
                    }
                }
            }
        }
        item {
            val asrSchool by viewModel.asrSchool.collectAsState()
            Box(modifier = Modifier.fillMaxWidth()) {
                Preference(
                    title = "Asr Juristic Method",
                    summary = when (asrSchool) {
                        PrayerTimeCalculator.AsrSchool.STANDARD -> "Standard (Shafi'i, Maliki, Hanbali)"
                        PrayerTimeCalculator.AsrSchool.HANAFI -> "Hanafi School"
                    },
                    icon = painterResource(R.drawable.calendar_month),
                    onClick = { showAsrMenu = true }
                )
                DropdownMenu(
                    expanded = showAsrMenu,
                    onDismissRequest = { showAsrMenu = false }
                ) {
                    PrayerTimeCalculator.AsrSchool.values().forEach { school ->
                        DropdownMenuItem(
                            text = { Text(school.name) },
                            onClick = {
                                viewModel.setAsrSchool(school)
                                showAsrMenu = false
                            }
                        )
                    }
                }
            }
        }

        // --- Iqama Offset settings ---
        categoryTitleSmall { "Iqama Offsets" }
        item {
            val fajrOffset by viewModel.fajrIqamaOffset.collectAsState()
            OffsetAdjustmentRow(
                title = "Fajr Iqama",
                value = fajrOffset,
                onValueChange = { viewModel.setFajrIqamaOffset(it) }
            )
        }
        item {
            val dhuhrOffset by viewModel.dhuhrIqamaOffset.collectAsState()
            OffsetAdjustmentRow(
                title = "Dhuhr Iqama",
                value = dhuhrOffset,
                onValueChange = { viewModel.setDhuhrIqamaOffset(it) }
            )
        }
        item {
            val asrOffset by viewModel.asrIqamaOffset.collectAsState()
            OffsetAdjustmentRow(
                title = "Asr Iqama",
                value = asrOffset,
                onValueChange = { viewModel.setAsrIqamaOffset(it) }
            )
        }
        item {
            val maghribOffset by viewModel.maghribIqamaOffset.collectAsState()
            OffsetAdjustmentRow(
                title = "Maghrib Iqama",
                value = maghribOffset,
                onValueChange = { viewModel.setMaghribIqamaOffset(it) }
            )
        }
        item {
            val ishaOffset by viewModel.ishaIqamaOffset.collectAsState()
            OffsetAdjustmentRow(
                title = "Isha Iqama",
                value = ishaOffset,
                onValueChange = { viewModel.setIshaIqamaOffset(it) }
            )
        }

        // --- Notifications settings ---
        categoryTitleSmall { "Notifications" }
        item {
            val notification by viewModel.notification.collectAsState()
            val notificationPermissionCallback = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.setNotification(isGranted)
            }

            SwitchPreference(
                title = "Iqama Reminders",
                summary = "Send alarm notifications when Iqama time begins",
                icon = painterResource(R.drawable.notification),
                value = notification,
                onValueChanged = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionCallback.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        viewModel.setNotification(isChecked)
                    }
                }
            )
        }

        // --- Quran Font Size settings ---
        categoryTitleSmall { "Quran Customization" }
        item {
            val quranFontSize by viewModel.quranArabicFontSize.collectAsState()
            FontSizeAdjustmentRow(
                title = "Arabic Text Size",
                value = quranFontSize,
                onValueChange = { viewModel.setQuranArabicFontSize(it) }
            )
        }
        item {
            val quranFontSize by viewModel.quranArabicFontSize.collectAsState()
            val arabicFontFamily = remember { FontFamily(Font(R.font.scheherazade_new)) }
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = quranFontSize.sp,
                lineHeight = (quranFontSize * 1.6f).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .card()
                    .padding(12.dp),
                fontFamily = arabicFontFamily
            )
        }

        // --- Language settings ---
        categoryTitleSmall { "Language / اللغة" }
        item {
            val lang by viewModel.appLanguage.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .card()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App Language", style = MaterialTheme.typography.titleMedium)
                    Text("لغة التطبيق", style = MaterialTheme.typography.bodySmall, color = colorScheme.secondary)
                }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = lang == "en",
                        onClick = { viewModel.setAppLanguage("en") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("EN") }
                    SegmentedButton(
                        selected = lang == "ar",
                        onClick = { viewModel.setAppLanguage("ar") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("AR") }
                }
            }
        }

        // --- UI / Theme settings ---
        categoryTitleSmall { "App Theme" }
        item {
            val currentTheme by viewModel.theme.collectAsState()
            val scroll = rememberScrollState(0)
            val panelWidth = 272.dp.px.toInt()
            LaunchedEffect(currentTheme) {
                scroll.animateScrollTo(panelWidth * (currentTheme.ordinal / 3), tween())
            }
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .card()
                    .horizontalScroll(scroll)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreferenceContainer(currentTheme, true) { viewModel.setTheme(it) }
                ThemePreferenceContainer(currentTheme, false) { viewModel.setTheme(it) }
            }
        }

        // --- About Section ---
        categoryTitleSmall { "About" }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Deen Companion",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "An open-source Islamic companion app for Muslims.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Based on the UI/UX of Traffic Light by leekleak. Built with Material Design 3 and Jetpack Compose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.secondary
                    )
                }
            }
        }
        item {
            NavigatePreference(
                title = "Source App",
                summary = "Based on github.com/leekleak/traffic-light",
                icon = painterResource(R.drawable.version),
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/leekleak/traffic-light"))
                    context.startActivity(intent)
                }
            )
        }
        item {
            NavigatePreference(
                title = "Application Settings",
                summary = "Version ${BuildConfig.VERSION_NAME}",
                icon = painterResource(R.drawable.version),
                onClick = { viewModel.openAppSettings(activity) }
            )
        }
    }

    PageTitle(true, hazeState, "Settings")
}

@Composable
fun OffsetAdjustmentRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .card()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Minutes after Adhan", style = MaterialTheme.typography.bodyMedium, color = colorScheme.secondary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > 0) onValueChange(value - 5) },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                text = String.format(Locale.US, "%d min", value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (value < 60) onValueChange(value + 5) },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FontSizeAdjustmentRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .card()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Arabic text size in sp", style = MaterialTheme.typography.bodyMedium, color = colorScheme.secondary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > 16) onValueChange(value - 2) },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                text = String.format(Locale.US, "%d sp", value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (value < 48) onValueChange(value + 2) },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}