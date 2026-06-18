package com.pilotothegreat.deencompanion.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.theme.Theme
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.util.CategoryTitleSmallText
import com.pilotothegreat.deencompanion.util.PageTitle
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import com.pilotothegreat.deencompanion.util.categoryTitleSmall
import com.pilotothegreat.deencompanion.util.px
import com.pilotothegreat.deencompanion.util.toLocaleHourString
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

data class OmaniBankApp(
    val name: String,
    val packageName: String,
    val initials: String,
    val color: Color
)

val omaniBankApps = listOf(
    OmaniBankApp("Bank Muscat", "com.ducont.muscatbank", "BM", Color(0xFFC70039)),
    OmaniBankApp("bm Wallet", "app.banking.bankmuscat", "bm", Color(0xFFC70039)),
    OmaniBankApp("NBO", "om.nbo.nbo", "NBO", Color(0xFF003F88)),
    OmaniBankApp("Bank Dhofar", "com.bankdhofar.mobilebanking", "BD", Color(0xFFD62246)),
    OmaniBankApp("Sohar International", "com.BankSoharMB", "SI", Color(0xFFC5A059)),
    OmaniBankApp("Oman Arab Bank", "com.oab.mobile", "OAB", Color(0xFF006D77)),
    OmaniBankApp("Ahli Bank", "com.ahlibank", "AB", Color(0xFF8D5B4C))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(paddingValues: PaddingValues) {
    val viewModel: SettingsVM = koinViewModel()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    val lang by viewModel.appLanguage.collectAsState()

    var showCalcMenu by remember { mutableStateOf(false) }
    var showAsrMenu by remember { mutableStateOf(false) }

    // Payment Bottom Sheet state
    var showPaymentSheet by remember { mutableStateOf(false) }
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .hazeSource(hazeState),
        contentPadding = paddingValues
    ) {
        // --- Location Settings ---
        categoryTitleSmall { stringResource(R.string.prayer_calculations) }
        item {
            val useIpFallback by viewModel.useIpLocationFallback.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.ip_location_fallback_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.ip_location_fallback_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useIpFallback,
                    onCheckedChange = { viewModel.setUseIpLocationFallback(it) }
                )
            }
        }

        // --- Prayer Times calculation settings ---
        categoryTitleSmall { stringResource(R.string.calculation_method) }

        item {
            val calcMethod by viewModel.calcMethod.collectAsState()
            Box(modifier = Modifier.fillMaxWidth()) {
                Preference(
                    title = stringResource(R.string.calculation_method),
                    summary = when (calcMethod) {
                        PrayerTimeCalculator.CalculationMethod.OMAN -> stringResource(R.string.calc_oman)
                        PrayerTimeCalculator.CalculationMethod.MWL -> stringResource(R.string.calc_mwl)
                        PrayerTimeCalculator.CalculationMethod.ISNA -> stringResource(R.string.calc_isna)
                        PrayerTimeCalculator.CalculationMethod.EGYPT -> stringResource(R.string.calc_egypt)
                        PrayerTimeCalculator.CalculationMethod.MAKKAH -> stringResource(R.string.calc_makkah)
                        PrayerTimeCalculator.CalculationMethod.KARACHI -> stringResource(R.string.calc_karachi)
                        PrayerTimeCalculator.CalculationMethod.JAFARI -> stringResource(R.string.calc_jafari)
                        PrayerTimeCalculator.CalculationMethod.TEHRAN -> stringResource(R.string.calc_tehran)
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
                            text = {
                                val label = when (method) {
                                    PrayerTimeCalculator.CalculationMethod.OMAN -> stringResource(R.string.calc_oman)
                                    PrayerTimeCalculator.CalculationMethod.MWL -> stringResource(R.string.calc_mwl)
                                    PrayerTimeCalculator.CalculationMethod.ISNA -> stringResource(R.string.calc_isna)
                                    PrayerTimeCalculator.CalculationMethod.EGYPT -> stringResource(R.string.calc_egypt)
                                    PrayerTimeCalculator.CalculationMethod.MAKKAH -> stringResource(R.string.calc_makkah)
                                    PrayerTimeCalculator.CalculationMethod.KARACHI -> stringResource(R.string.calc_karachi)
                                    PrayerTimeCalculator.CalculationMethod.JAFARI -> stringResource(R.string.calc_jafari)
                                    PrayerTimeCalculator.CalculationMethod.TEHRAN -> stringResource(R.string.calc_tehran)
                                }
                                Text(label)
                            },
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
                    title = stringResource(R.string.asr_juristic_method),
                    summary = when (asrSchool) {
                        PrayerTimeCalculator.AsrSchool.STANDARD -> stringResource(R.string.asr_standard)
                        PrayerTimeCalculator.AsrSchool.HANAFI -> stringResource(R.string.asr_hanafi)
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
                            text = {
                                val label = when (school) {
                                    PrayerTimeCalculator.AsrSchool.STANDARD -> stringResource(R.string.asr_standard)
                                    PrayerTimeCalculator.AsrSchool.HANAFI -> stringResource(R.string.asr_hanafi)
                                }
                                Text(label)
                            },
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
        categoryTitleSmall { stringResource(R.string.iqama_offsets) }
        item {
            var offsetsExpanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth().card()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { offsetsExpanded = !offsetsExpanded }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(R.drawable.clock), contentDescription = null, tint = colorScheme.primary)
                        Column {
                            Text(stringResource(R.string.iqama_offsets), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.minutes_after_adhan), style = MaterialTheme.typography.bodySmall, color = colorScheme.secondary)
                        }
                    }
                    Icon(
                        if (offsetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(visible = offsetsExpanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val context = LocalContext.current
                        val fajrOffset by viewModel.fajrIqamaOffset.collectAsState()
                        val dhuhrOffset by viewModel.dhuhrIqamaOffset.collectAsState()
                        val asrOffset by viewModel.asrIqamaOffset.collectAsState()
                        val maghribOffset by viewModel.maghribIqamaOffset.collectAsState()
                        val ishaOffset by viewModel.ishaIqamaOffset.collectAsState()

                        val fajrIsFixed by viewModel.fajrIqamaIsFixed.collectAsState()
                        val dhuhrIsFixed by viewModel.dhuhrIqamaIsFixed.collectAsState()
                        val asrIsFixed by viewModel.asrIqamaIsFixed.collectAsState()
                        val maghribIsFixed by viewModel.maghribIqamaIsFixed.collectAsState()
                        val ishaIsFixed by viewModel.ishaIqamaIsFixed.collectAsState()

                        val fajrIqamaTimeVal by viewModel.fajrIqamaTime.collectAsState()
                        val dhuhrIqamaTimeVal by viewModel.dhuhrIqamaTime.collectAsState()
                        val asrIqamaTimeVal by viewModel.asrIqamaTime.collectAsState()
                        val maghribIqamaTimeVal by viewModel.maghribIqamaTime.collectAsState()
                        val ishaIqamaTimeVal by viewModel.ishaIqamaTime.collectAsState()

                        IqamaConfigRow(
                            context = context,
                            title = stringResource(R.string.fajr),
                            isFixed = fajrIsFixed,
                            offset = fajrOffset,
                            fixedTime = fajrIqamaTimeVal,
                            lang = lang,
                            onModeChange = { viewModel.setFajrIqamaIsFixed(it) },
                            onOffsetChange = { viewModel.setFajrIqamaOffset(it) },
                            onTimeChange = { viewModel.setFajrIqamaTime(it) }
                        )
                        IqamaConfigRow(
                            context = context,
                            title = stringResource(R.string.dhuhr),
                            isFixed = dhuhrIsFixed,
                            offset = dhuhrOffset,
                            fixedTime = dhuhrIqamaTimeVal,
                            lang = lang,
                            onModeChange = { viewModel.setDhuhrIqamaIsFixed(it) },
                            onOffsetChange = { viewModel.setDhuhrIqamaOffset(it) },
                            onTimeChange = { viewModel.setDhuhrIqamaTime(it) }
                        )
                        IqamaConfigRow(
                            context = context,
                            title = stringResource(R.string.asr),
                            isFixed = asrIsFixed,
                            offset = asrOffset,
                            fixedTime = asrIqamaTimeVal,
                            lang = lang,
                            onModeChange = { viewModel.setAsrIqamaIsFixed(it) },
                            onOffsetChange = { viewModel.setAsrIqamaOffset(it) },
                            onTimeChange = { viewModel.setAsrIqamaTime(it) }
                        )
                        IqamaConfigRow(
                            context = context,
                            title = stringResource(R.string.maghrib),
                            isFixed = maghribIsFixed,
                            offset = maghribOffset,
                            fixedTime = maghribIqamaTimeVal,
                            lang = lang,
                            onModeChange = { viewModel.setMaghribIqamaIsFixed(it) },
                            onOffsetChange = { viewModel.setMaghribIqamaOffset(it) },
                            onTimeChange = { viewModel.setMaghribIqamaTime(it) }
                        )
                        IqamaConfigRow(
                            context = context,
                            title = stringResource(R.string.isha),
                            isFixed = ishaIsFixed,
                            offset = ishaOffset,
                            fixedTime = ishaIqamaTimeVal,
                            lang = lang,
                            onModeChange = { viewModel.setIshaIqamaIsFixed(it) },
                            onOffsetChange = { viewModel.setIshaIqamaOffset(it) },
                            onTimeChange = { viewModel.setIshaIqamaTime(it) }
                        )
                    }
                }
            }
        }

        // --- Notifications settings ---
        categoryTitleSmall { stringResource(R.string.notifications) }
        item {
            val notification by viewModel.notification.collectAsState()
            val notificationPermissionCallback = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.setNotification(isGranted)
            }

            SwitchPreference(
                title = stringResource(R.string.iqama_reminders),
                summary = stringResource(R.string.iqama_reminders_summary),
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

        // --- Notifications > Sound ---
        categoryTitleSmall { stringResource(R.string.notifications_sound) }
        item {
            val volume by appPreferenceRepo.notificationVolume.collectAsState(initial = 80)
            VolumeAdjustmentRow(
                title = stringResource(R.string.notification_volume),
                value = volume,
                onValueChange = { newVal ->
                    scope.launch {
                        appPreferenceRepo.setNotificationVolume(newVal)
                    }
                }
            )
        }

        // --- Hijri Calendar settings ---
        categoryTitleSmall { stringResource(R.string.hijri_calendar) }
        item {
            val hijriMethod by appPreferenceRepo.hijriCalendarMethod.collectAsState(initial = com.pilotothegreat.deencompanion.database.HijriMethod.UMM_AL_QURA)
            SwitchPreference(
                title = stringResource(R.string.regional_moon_sighting),
                summary = stringResource(R.string.regional_moon_sighting_summary),
                icon = painterResource(R.drawable.calendar_month),
                value = hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL,
                onValueChanged = { isChecked ->
                    scope.launch {
                        appPreferenceRepo.setHijriCalendarMethod(
                            if (isChecked) com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL
                            else com.pilotothegreat.deencompanion.database.HijriMethod.UMM_AL_QURA
                        )
                    }
                }
            )
        }


        // --- Quran Font Size settings ---
        categoryTitleSmall { stringResource(R.string.quran_customization) }
        item {
            val quranFontSize by viewModel.quranArabicFontSize.collectAsState()
            FontSizeAdjustmentRow(
                title = stringResource(R.string.arabic_text_size),
                value = quranFontSize,
                onValueChange = { viewModel.setQuranArabicFontSize(it) }
            )
        }
        item {
            val quranFontSize by viewModel.quranArabicFontSize.collectAsState()
            val arabicFontFamily = com.pilotothegreat.deencompanion.ui.theme.arabicFontFamily
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
        item {
            val lang by viewModel.appLanguage.collectAsState()
            LanguagePreferenceContainer(
                currentLang = lang,
                onLangChanged = { viewModel.setAppLanguage(it) }
            )
        }

        // --- UI / Theme settings ---
        categoryTitleSmall { stringResource(R.string.app_theme) }
        item {
            val currentTheme by viewModel.theme.collectAsState()
            val amoledBlackMode by viewModel.amoledBlackMode.collectAsState()
            val scroll = rememberScrollState(0)
            val panelWidth = 272.dp.px.toInt()
            LaunchedEffect(currentTheme) {
                scroll.animateScrollTo(
                    panelWidth * (currentTheme.ordinal / 3),
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .card()
                        .clickable { viewModel.setAmoledBlackMode(!amoledBlackMode) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.amoled_black_mode_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.amoled_black_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = amoledBlackMode,
                        onCheckedChange = { viewModel.setAmoledBlackMode(it) }
                    )
                }
            }
        }

        // --- About Section ---
        categoryTitleSmall { stringResource(R.string.about) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.about_credits),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.secondary
                    )
                }
            }
        }
        item {
            val activity = LocalActivity.current
            val updateAvailable by viewModel.updateAvailable.collectAsState()
            val updateState by viewModel.updateState.collectAsState()
            val lastChecked by viewModel.lastCheckedTimestamp.collectAsState()

            val lastCheckedStr = remember(lastChecked) {
                if (lastChecked == 0L) "--"
                else {
                    val instant = java.time.Instant.ofEpochMilli(lastChecked)
                    val ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    ldt.toLocalTime().toLocaleHourString(context)
                }
            }

            val summaryText = when (updateState) {
                SettingsVM.UpdateState.CHECKING -> stringResource(R.string.checking_updates)
                SettingsVM.UpdateState.UPDATE_AVAILABLE -> stringResource(R.string.update_available, updateAvailable ?: "")
                SettingsVM.UpdateState.UP_TO_DATE -> stringResource(R.string.up_to_date) + " • " + stringResource(R.string.last_checked, lastCheckedStr)
                SettingsVM.UpdateState.FAILED -> stringResource(R.string.update_check_failed) + " • " + stringResource(R.string.last_checked, lastCheckedStr)
                else -> stringResource(R.string.check_for_updates) + " • " + stringResource(R.string.last_checked, lastCheckedStr)
            }

            NavigatePreference(
                title = stringResource(R.string.check_for_updates),
                summary = summaryText,
                icon = painterResource(R.drawable.version),
                onClick = {
                    if (updateState == SettingsVM.UpdateState.UPDATE_AVAILABLE) {
                        if (viewModel.isPlayStoreInstall) {
                            activity?.let { viewModel.startPlayStoreUpdate(it) }
                        } else {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Pilotothegreat/deen-companion/releases/latest"))
                            context.startActivity(intent)
                        }
                    } else {
                        viewModel.checkForUpdates(force = true)
                    }
                }
            )
        }
        categoryTitleSmall { stringResource(R.string.support_developer) }
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(vertical = 4.dp)
                    .drawBehind {
                        // Kufiya pattern background
                        val step = 16.dp.toPx()
                        val strokeWidth = 1.dp.toPx()
                        val lineColor = Color.LightGray.copy(alpha = 0.08f)
                        var offset = 0f
                        while (offset < size.width + size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(offset, 0f),
                                end = Offset(0f, offset),
                                strokeWidth = strokeWidth
                            )
                            drawLine(
                                color = lineColor,
                                start = Offset(size.width - offset, 0f),
                                end = Offset(size.width, offset),
                                strokeWidth = strokeWidth
                            )
                            offset += step
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFC8102E))
                            Text(stringResource(R.string.support_deen_companion),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onTertiaryContainer)
                        }
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF556B2F).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF556B2F).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = stringResource(R.string.palestine_badge),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF556B2F)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.support_deen_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )

                    // Palestine cause note
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = stringResource(R.string.palestine_cause_support),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF556B2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    // Phone number card with bottom sheet flow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.tertiary.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.foundation.LocalIndication.current
                            ) {
                                // 1. Copy mobile number to clipboard
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Phone", "91904926"))
                                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                // 2. Trigger bottom sheet
                                showPaymentSheet = true
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.bank_transfer_phone_pay),
                                style = MaterialTheme.typography.labelMedium, color = colorScheme.onTertiaryContainer)
                            Text("91904926",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onTertiaryContainer)
                            Text(stringResource(R.string.payment_tap_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                        }
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                            tint = colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
        item {
            NavigatePreference(
                title = stringResource(R.string.github_repository),
                summary = stringResource(R.string.github_repository_summary),
                icon = painterResource(R.drawable.github),
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Pilotothegreat/deen-companion"))
                    context.startActivity(intent)
                }
            )
        }
        item {
            NavigatePreference(
                title = stringResource(R.string.application_settings),
                summary = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                icon = painterResource(R.drawable.version),
                onClick = { viewModel.openAppSettings(activity) }
            )
        }
    }

    // Modal Bottom Sheet Overlay
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = paymentSheetState,
            containerColor = colorScheme.surfaceContainer,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PaymentBottomSheetContent(
                context = context,
                onDismiss = { showPaymentSheet = false }
            )
        }
    }

    PageTitle(true, hazeState, stringResource(R.string.settings))
}

@Composable
fun PaymentBottomSheetContent(
    context: Context,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .drawBehind {
                // Kufiya pattern background
                val step = 20.dp.toPx()
                val strokeWidth = 1.dp.toPx()
                val lineColor = Color.LightGray.copy(alpha = 0.05f)
                var offset = 0f
                while (offset < size.width + size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(offset, 0f),
                        end = Offset(0f, offset),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width - offset, 0f),
                        end = Offset(size.width, offset),
                        strokeWidth = strokeWidth
                    )
                    offset += step
                }
            }
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.payment_sheet_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary
            )
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFFC8102E).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8102E).copy(alpha = 0.3f))
            ) {
                Text(
                    text = stringResource(R.string.palestine_badge),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFC8102E)
                )
            }
        }

        Text(
            text = stringResource(R.string.payment_sheet_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )

        // Palestine warning card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF556B2F).copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFC8102E),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.palestine_cause_support),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF556B2F)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            omaniBankApps.forEach { bank ->
                val isInstalled = remember(bank.packageName) { isPackageInstalled(context, bank.packageName) }
                val bankInteractionSource = remember { MutableInteractionSource() }
                val bankPressed by bankInteractionSource.collectIsPressedAsState()
                val bankScale by animateFloatAsState(
                    targetValue = if (bankPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = bankScale
                            scaleY = bankScale
                        }
                        .clickable(
                            interactionSource = bankInteractionSource,
                            indication = androidx.compose.foundation.LocalIndication.current
                        ) {
                            if (isInstalled) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(bank.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            } else {
                                val fallbackMsg = context.getString(R.string.bank_not_installed, bank.name)
                                Toast.makeText(context, fallbackMsg, Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInstalled) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isInstalled) 1.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Circular Initials Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (isInstalled) bank.color else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bank.initials,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Bank name & installation status subtitle
                            Column {
                                Text(
                                    text = bank.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isInstalled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = if (isInstalled) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (isInstalled) "Installed • جاهز" else "Not Installed • غير مثبت",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isInstalled) colorScheme.primary else colorScheme.outline
                                )
                            }
                        }

                        // Icon action
                        if (isInstalled) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = stringResource(R.string.open_bank_app, bank.name),
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy number",
                                tint = colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    } catch (e: Exception) {
        false
    }
}

@Composable
fun IqamaConfigRow(
    context: Context,
    title: String,
    isFixed: Boolean,
    offset: Int,
    fixedTime: String,
    lang: String,
    onModeChange: (Boolean) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val containerColorOffset = if (!isFixed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val textColorOffset = if (!isFixed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                val containerColorFixed = if (isFixed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val textColorFixed = if (isFixed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Button(
                    onClick = { onModeChange(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = containerColorOffset, contentColor = textColorOffset),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "نسبية" else "Offset",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Button(
                    onClick = { onModeChange(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = containerColorFixed, contentColor = textColorFixed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "ثابتة" else "Fixed",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isFixed) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == "ar") "وقت الإقامة الثابت:" else "Fixed Iqama Time:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Button(
                    onClick = {
                        val parts = fixedTime.split(":")
                        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val formatted = String.format(Locale.US, "%02d:%02d", hour, minute)
                                onTimeChange(formatted)
                            },
                            initialHour,
                            initialMinute,
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = fixedTime, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == "ar") "بعد الأذان بـ:" else "After Adhan:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (offset > 0) onOffsetChange(offset - 5) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = stringResource(R.string.minutes_format, offset),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (offset < 60) onOffsetChange(offset + 5) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
    }
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.minutes_after_adhan_detail), style = MaterialTheme.typography.bodyMedium, color = colorScheme.secondary)
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
                text = stringResource(R.string.minutes_format, value),
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
            Text(text = stringResource(R.string.arabic_text_size_sp), style = MaterialTheme.typography.bodyMedium, color = colorScheme.secondary)
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

@Composable
fun VolumeAdjustmentRow(
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
            Text(text = stringResource(R.string.volume_percent, value), style = MaterialTheme.typography.bodyMedium, color = colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
