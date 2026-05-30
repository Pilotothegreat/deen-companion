// FIXED: Real-time countdown counter loop, location detection warning card, Qibla card subtitle, arabic font support, and last updated timestamp
package com.pilotothegreat.deencompanion.ui.overview

import android.content.Context
import android.os.Vibrator
import android.os.Build
import android.os.VibrationEffect
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.SettingsKey
import com.pilotothegreat.deencompanion.ui.navigation.QiblaKey
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.ui.theme.googleSans
import com.pilotothegreat.deencompanion.util.CategoryTitleText
import com.pilotothegreat.deencompanion.util.EqualHeightRow
import com.pilotothegreat.deencompanion.util.MiniCard
import com.pilotothegreat.deencompanion.util.MiniCardState
import com.pilotothegreat.deencompanion.util.PageTitle
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import com.pilotothegreat.deencompanion.util.toLocaleHourString
import com.pilotothegreat.deencompanion.util.px
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.filled.CompassCalibration
import java.time.chrono.HijrahDate
import java.util.Locale
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class NextPrayer(
    val name: String,
    val remainingTimeStr: String,
    val timeStr: String,
    val durationSeconds: Long
)

fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
    val phiVal = Math.toRadians(latitude)
    val lambdaVal = Math.toRadians(longitude)
    val phiK = Math.toRadians(21.4225) // Makkah lat
    val lambdaK = Math.toRadians(39.8262) // Makkah lon

    val y = Math.sin(lambdaK - lambdaVal)
    val x = Math.cos(phiVal) * Math.sin(phiK) - Math.sin(phiVal) * Math.cos(phiK) * Math.cos(lambdaK - lambdaVal)
    var qiblaAngle = Math.toDegrees(Math.atan2(y, x))
    if (qiblaAngle < 0) {
        qiblaAngle += 360.0
    }
    return qiblaAngle
}

data class Inspiration(val en: String, val ar: String, val ref: String)

private val inspirations = listOf(
    Inspiration("So verily, with hardship, there is ease.", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "Quran 94:5"),
    Inspiration("Indeed, Allah is with the patient.", "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "Quran 2:153"),
    Inspiration("And He found you lost and guided you.", "وَوَجَدَكَ ضَالًّا فَهَدَىٰ", "Quran 93:7"),
    Inspiration("Call upon Me; I will answer you.", "ادْعُونِي أَسْتَجِبْ لَكُمْ", "Quran 40:60"),
    Inspiration("My mercy encompasses all things.", "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ", "Quran 7:156"),
    Inspiration("Remember Me; I will remember you.", "فَاذْكُرُونِي أَذْكُرْكُمْ", "Quran 2:152"),
    Inspiration("Allah does not burden a soul beyond that it can bear.", "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Quran 2:286"),
    Inspiration("Indeed, actions are but by intentions.", "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Bukhari & Muslim"),
    Inspiration("The best of you are those who learn the Quran and teach it.", "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "Bukhari"),
    Inspiration("A good word is charity.", "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ", "Bukhari & Muslim")
)

fun calculateNextPrayer(
    times: PrayerTimeCalculator.PrayerTimes,
    timezoneId: String,
    context: Context
): NextPrayer {
    val zoneId = try { ZoneId.of(timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
    val now = LocalDateTime.now(zoneId)
    val localTime = now.toLocalTime()

    val prayers = listOf(
        Pair("Fajr", times.fajr),
        Pair("Sunrise", times.sunrise),
        Pair("Dhuhr", times.dhuhr),
        Pair("Asr", times.asr),
        Pair("Maghrib", times.maghrib),
        Pair("Isha", times.isha)
    )

    var nextName = "Fajr"
    var nextTime = times.fajr
    var nextDateTime = LocalDateTime.of(now.toLocalDate(), times.fajr)

    if (localTime.isAfter(times.isha)) {
        nextName = "Fajr"
        nextTime = times.fajr
        nextDateTime = LocalDateTime.of(now.toLocalDate().plusDays(1), times.fajr)
    } else {
        for (p in prayers) {
            if (localTime.isBefore(p.second)) {
                nextName = p.first
                nextTime = p.second
                nextDateTime = LocalDateTime.of(now.toLocalDate(), p.second)
                break
            }
        }
    }

    val duration = Duration.between(now, nextDateTime)
    val totalSeconds = duration.seconds
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    val remainingStr = when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes >= 5 -> "${minutes}m ${seconds}s"
        else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    val timeStr = nextTime.toLocaleHourString(context)
    return NextPrayer(nextName, remainingStr, timeStr, totalSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Overview(
    paddingValues: PaddingValues,
) {
    val viewModel: OverviewVM = koinViewModel()
    val navigator: Navigator = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val context = LocalContext.current
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")
    val coroutineScope = rememberCoroutineScope()

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val hazeState = rememberHazeState()
    val scrollState = rememberScrollState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshLocation(context)
        onPauseOrDispose {}
    }

    val times by viewModel.prayerTimes.collectAsState(initial = viewModel.prayerTimes.value)
    val tz by viewModel.timezoneId.collectAsState(initial = "Asia/Riyadh")
    val city by viewModel.cityName.collectAsState(initial = "")
    val lat by viewModel.latitude.collectAsState(initial = 21.3891)
    val lon by viewModel.longitude.collectAsState(initial = 39.8579)
    var nextPrayer by remember { mutableStateOf(NextPrayer("Fajr", "--", "--", 0L)) }
    var showTasbihSheet by remember { mutableStateOf(false) }

    val launchCount by appPreferenceRepo.appLaunchCount.collectAsState(initial = 0)
    val donationDismissed by appPreferenceRepo.donationPromptDismissed.collectAsState(initial = false)
    val lastPromptTime by appPreferenceRepo.lastDonationPromptShowTime.collectAsState(initial = 0L)
    
    var showDonationDialog by remember { mutableStateOf(false) }
    var showDonationBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(launchCount, donationDismissed, lastPromptTime) {
        val now = System.currentTimeMillis()
        val daysSinceLastPrompt = (now - lastPromptTime) / (1000L * 60 * 60 * 24)
        if (launchCount >= 3 && !donationDismissed && daysSinceLastPrompt >= 3) {
            showDonationDialog = true
        }
    }
    
    val currentTimes by rememberUpdatedState(times)
    val currentTz by rememberUpdatedState(tz)
    val currentContext by rememberUpdatedState(context)
    
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lang, lifecycleOwner) {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(true) {
                nextPrayer = calculateNextPrayer(currentTimes, currentTz, currentContext)
                if (isRobolectric) break
                val delayMs = if (nextPrayer.durationSeconds < 3600) 1000L else 10000L
                kotlinx.coroutines.delay(delayMs)
            }
        }
    }

    val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val paddingTop = paddingValues.calculateTopPadding()
    val paddingBottom = paddingValues.calculateBottomPadding()

    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(horizontal = paddingSide)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.height(paddingTop - 8.dp))

        // Date Header
        val locale = remember(lang) { Locale(lang) }
        val gregFormatter = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
        val hijriFormatter = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
        val gregDateStr = remember(locale) { LocalDate.now().format(gregFormatter) }
        val hijriMethod by appPreferenceRepo.hijriCalendarMethod.collectAsState(initial = com.pilotothegreat.deencompanion.database.HijriMethod.UMM_AL_QURA)
        val hijriDateStr = remember(locale, hijriMethod) { 
            try {
                var hijri = HijrahDate.now()
                if (hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL) {
                    hijri = HijrahDate.from(LocalDate.now().plusDays(1))
                }
                hijri.format(hijriFormatter) + " AH"
            } catch (e: Exception) {
                ""
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = gregDateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.secondary
            )
            if (hijriDateStr.isNotEmpty()) {
                Text(
                    text = hijriDateStr,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )
            }
            if (city.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        modifier = Modifier.size(12.dp), tint = colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(city, style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                }
            }
        }

        // Location Not Detected Warning Card
        val hasLocationPermission = remember(context) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        var warningDismissed by remember { mutableStateOf(false) }
        val showWarning = !warningDismissed && (city.isEmpty() || (lat == 21.3891 && lon == 39.8579) || !hasLocationPermission)

        if (showWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        viewModel.refreshLocation(context)
                        warningDismissed = true
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.location_not_detected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.location_makkah_fallback),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Ramadan Hilal Card
        val dismissedYear by appPreferenceRepo.dismissedRamadanHilalYear.collectAsState(initial = 0)
        val currentHijri = remember(hijriMethod) {
            try {
                var hijri = HijrahDate.now()
                if (hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL) {
                    hijri = HijrahDate.from(LocalDate.now().plusDays(1))
                }
                hijri
            } catch (e: Exception) {
                null
            }
        }
        val shabanMonth = 8
        val isShaban = currentHijri?.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) == shabanMonth
        val shabanDay = currentHijri?.get(java.time.temporal.ChronoField.DAY_OF_MONTH) ?: 0
        val isHilalPeriod = isShaban && shabanDay in 20..29
        val hijriYear = currentHijri?.get(java.time.temporal.ChronoField.YEAR) ?: 0
        val showHilalCard = isHilalPeriod && dismissedYear != hijriYear

        if (showHilalCard) {
            val daysUntilRamadan = 30 - shabanDay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1B41), // Deep Indigo
                                    Color(0xFF2C2F75), // Mid Indigo
                                    Color(0xFF4C1C5C)  // Dark Purple/Indigo
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ramadan_approaching),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC5A059) // Warm gold primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.days_until_ramadan, daysUntilRamadan),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        appPreferenceRepo.setDismissedRamadanHilalYear(hijriYear)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.ramadan_hilal_sighting_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier
                                .clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://mara.gov.om"))
                                    context.startActivity(intent)
                                }
                                .background(Color(0xFFC5A059).copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                text = "mara.gov.om",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }
        }

        if (windowSizeClass.isWidthAtLeastBreakpoint(400)) {
            EqualHeightRow (
                modifier = Modifier.padding(horizontal = 16.dp),
                first = {
                    Column (Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        HeroItems(scrollState, viewModel, nextPrayer)
                    }
                },
                second = {
                    Column (Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewItems(viewModel, nextPrayer.name, navigator, onTasbihClick = { showTasbihSheet = true })
                    }
                },
                spacing = 16.dp
            )
        } else {
            HeroItems(scrollState, viewModel, nextPrayer)
            OverviewItems(viewModel, nextPrayer.name, navigator, onTasbihClick = { showTasbihSheet = true })
        }
        Box(Modifier.height(paddingBottom - 8.dp))
    }
    PageTitle(false, hazeState, stringResource(R.string.app_name)) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { navigator.goTo(SettingsKey) }
        ) {
            Icon(
                painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings)
            )
        }
    }

    if (showTasbihSheet) {
        TasbihBottomSheet(viewModel = viewModel, onDismiss = { showTasbihSheet = false })
    }

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDonationDialog = false
                coroutineScope.launch {
                    appPreferenceRepo.setLastDonationPromptShowTime(System.currentTimeMillis())
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFC8102E)
                    )
                    Text(
                        text = stringResource(R.string.donation_dialog_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.donation_dialog_desc))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF556B2F).copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.palestine_cause_support),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF556B2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF556B2F)
                    ),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Phone", "91904926")
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, context.getString(R.string.copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                        
                        showDonationDialog = false
                        showDonationBottomSheet = true
                    }
                ) {
                    Text(stringResource(R.string.donate_now), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDonationDialog = false
                        coroutineScope.launch {
                            appPreferenceRepo.setDonationPromptDismissed(true)
                        }
                    }
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        )
    }

    if (showDonationBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDonationBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            com.pilotothegreat.deencompanion.ui.settings.PaymentBottomSheetContent(
                context = context,
                onDismiss = { showDonationBottomSheet = false }
            )
        }
    }
}

@Composable
private fun HeroItems(scrollState: ScrollState, viewModel: OverviewVM, nextPrayer: NextPrayer) {
    OverviewHero(scrollState, viewModel, nextPrayer)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewHero(scrollState: ScrollState, viewModel: OverviewVM, nextPrayer: NextPrayer) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val offset by animateFloatAsState(if (pressed) 132.dp.px else 116.dp.px)

    val scheme = colorScheme
    val shape1 = Cookie12Sided.toPath()
    val shapeScale = 336.dp.px
    val iconScale = remember { Animatable(shapeScale) }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(65000, easing = LinearEasing)
        )
    )

    val shape1Transformed = remember(iconScale.value, rotation) {
        val path = Path().apply {
            addPath(shape1)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-0.5f, -0.5f)
            postScale(iconScale.value, iconScale.value)
            postRotate(rotation)
        }
        path.asAndroidPath().transform(matrix)
        path
    }
    val shape2Transformed = remember(iconScale.value, rotation) {
        val path = Path().apply {
            addPath(shape1)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-0.5f, -0.5f)
            postScale(iconScale.value, iconScale.value)
            postRotate(-rotation + 360f / 24)
        }
        path.asAndroidPath().transform(matrix)
        path
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = scrollState.value * 0.4f
                }
                .drawWithContent {
                    val a = size.width / 2 - offset
                    val b = size.width / 2 + offset

                    drawCircle(Brush.radialGradient(listOf(scheme.primaryContainer.copy(alpha = 0.6f), Color.Transparent)))
                    translate(a, b) { drawPath(shape1Transformed, scheme.surface.copy(alpha = 0.5f)) }
                    translate(b, a) { drawPath(shape2Transformed, scheme.surface.copy(alpha = 0.5f)) }
                }
        )
        val localizedPrayerName = when (nextPrayer.name) {
            "Fajr" -> stringResource(R.string.fajr)
            "Sunrise" -> stringResource(R.string.sunrise)
            "Dhuhr" -> stringResource(R.string.dhuhr)
            "Asr" -> stringResource(R.string.asr)
            "Maghrib" -> stringResource(R.string.maghrib)
            "Isha" -> stringResource(R.string.isha)
            else -> nextPrayer.name
        }
        val nextPrayerText = stringResource(R.string.next_prayer, localizedPrayerName)
        val nextAtText = stringResource(R.string.next_at, nextPrayer.timeStr)

        Column(modifier = Modifier.align(Alignment.Center)) {
            val width by animateFloatAsState(
                targetValue = if (pressed) 60f else 30f,
                animationSpec = spring()
            )
            val weight by animateFloatAsState(if (pressed) 800f else 400f, spring())
            val fontFamily1 = remember(weight, width) { googleSans(weight = weight, width = width, roundness = 100f) }
            val fontFamily2 = remember(weight, width) { googleSans(weight = weight + 200f, width = width + 70f, roundness = 50f) }

            LaunchedEffect(pressed) {
                if (pressed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 68.sp)) {
                        append(nextPrayer.remainingTimeStr)
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 24.sp)) {
                        appendLine()
                        append(nextPrayerText)
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily2, fontSize = 16.sp)) {
                        append(nextAtText)
                    }
                }
            )
        }
    }
}

@Composable
fun OverviewItems(viewModel: OverviewVM, nextPrayerName: String, navigator: Navigator, onTasbihClick: () -> Unit) {
    val times by viewModel.prayerTimes.collectAsState()
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val fajrOffset by viewModel.fajrIqamaOffset.collectAsState(initial = 15)
    val dhuhrOffset by viewModel.dhuhrIqamaOffset.collectAsState(initial = 15)
    val asrOffset by viewModel.asrIqamaOffset.collectAsState(initial = 15)
    val maghribOffset by viewModel.maghribIqamaOffset.collectAsState(initial = 10)
    val ishaOffset by viewModel.ishaIqamaOffset.collectAsState(initial = 15)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryTitleText(stringResource(R.string.prayer_times))

        val localizedPrayerName = when (nextPrayerName) {
            "Fajr" -> stringResource(R.string.fajr)
            "Sunrise" -> stringResource(R.string.sunrise)
            "Dhuhr" -> stringResource(R.string.dhuhr)
            "Asr" -> stringResource(R.string.asr)
            "Maghrib" -> stringResource(R.string.maghrib)
            "Isha" -> stringResource(R.string.isha)
            else -> nextPrayerName
        }

        val prayers = listOf(
            Triple(stringResource(R.string.fajr),   times.fajr,    fajrOffset),
            Triple(stringResource(R.string.sunrise), times.sunrise, null),
            Triple(stringResource(R.string.dhuhr),  times.dhuhr,   dhuhrOffset),
            Triple(stringResource(R.string.asr),    times.asr,     asrOffset),
            Triple(stringResource(R.string.maghrib),times.maghrib, maghribOffset),
            Triple(stringResource(R.string.isha),   times.isha,    ishaOffset)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (prayer in prayers) {
                    val (name, time, offset) = prayer
                    val isNext = name == localizedPrayerName  // highlight next prayer
                    val bg = if (isNext) colorScheme.primaryContainer else Color.Transparent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNext) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = time.toLocaleHourString(context),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isNext) colorScheme.onPrimaryContainer else colorScheme.primary
                            )
                            if (offset != null) {
                                val iqamaTime = time.plusMinutes(offset.toLong())
                                Text(
                                    text = stringResource(R.string.iqama_time, iqamaTime.toLocaleHourString(context)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isNext) colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else colorScheme.secondary
                                )
                            }
                        }
                    }
                    if (name != stringResource(R.string.isha)) {
                        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Add "Last Updated" timestamp here
        val lastUpdated by appPreferenceRepo.lastPrayerTimeUpdate.collectAsState(initial = 0L)
        val lastUpdatedStr = remember(lastUpdated) {
            if (lastUpdated == 0L) "--"
            else {
                val instant = java.time.Instant.ofEpochMilli(lastUpdated)
                val ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                ldt.toLocalTime().toLocaleHourString(context)
            }
        }
        Text(
            text = stringResource(R.string.last_updated, lastUpdatedStr),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        QiblaCompactCard(viewModel, navigator)

        // Daily Verse Card
        val inspirationIndex = remember { LocalDate.now().dayOfYear % inspirations.size }
        val currentInspiration = inspirations[inspirationIndex]
        CategoryTitleText(stringResource(R.string.daily_inspiration))
        
        val quote = if (lang == "ar") currentInspiration.ar else currentInspiration.en
        val arabicFontFamily = com.pilotothegreat.deencompanion.ui.theme.arabicFontFamily
        Box(
            modifier = Modifier
                .card()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = if (lang == "ar") androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = if (lang == "ar") arabicFontFamily else null,
                        fontSize = if (lang == "ar") 22.sp else 16.sp,
                        lineHeight = if (lang == "ar") 32.sp else 20.sp
                    ),
                    color = colorScheme.onSurface,
                    textAlign = if (lang == "ar") TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "— ${currentInspiration.ref}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.secondary,
                    modifier = Modifier.align(if (lang == "ar") Alignment.Start else Alignment.End)
                )
            }
        }

        // Tasbih Card
        val count by viewModel.tasbihCount.collectAsState(initial = 0)
        val haptic = LocalHapticFeedback.current
        val tasbihScale = remember { Animatable(1f) }
        LaunchedEffect(count) {
            if (count > 0) {
                tasbihScale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                tasbihScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            }
        }
        
        CategoryTitleText(stringResource(R.string.tasbih_counter))
        val dhikr by viewModel.tasbihDhikr.collectAsState(initial = "سبحان الله")
        val target by viewModel.tasbihTarget.collectAsState(initial = 33)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = tasbihScale.value
                    scaleY = tasbihScale.value
                }
                .card()
                .clickable {
                    onTasbihClick()
                }
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(dhikr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.tasbih_target, target), style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "$count / $target",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Open Tasbih",
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QiblaCompactCard(viewModel: OverviewVM, navigator: Navigator) {
    val lat by viewModel.latitude.collectAsState(initial = 21.3891)
    val lon by viewModel.longitude.collectAsState(initial = 39.8579)
    val angle = remember(lat, lon) { calculateQiblaDirection(lat, lon) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { navigator.goTo(QiblaKey) },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = colorScheme.primary)
                Text(stringResource(R.string.qibla_direction), style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format(Locale.US, "%.1f°", angle),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.clockwise_from_true_north),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }
                val primaryColor = colorScheme.primary
                Canvas(Modifier.size(24.dp)) {
                    val arrowWidth = 4.dp.toPx()
                    rotate(angle.toFloat()) {
                        drawPath(Path().apply {
                            moveTo(size.width/2, 0f)
                            lineTo(size.width/2+arrowWidth, size.height)
                            lineTo(size.width/2-arrowWidth, size.height)
                            close()
                        }, color = primaryColor)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihBottomSheet(
    viewModel: OverviewVM,
    onDismiss: () -> Unit
) {
    val count by viewModel.tasbihCount.collectAsState(initial = 0)
    val dhikr by viewModel.tasbihDhikr.collectAsState(initial = "سبحان الله")
    val target by viewModel.tasbihTarget.collectAsState(initial = 33)
    val history by viewModel.tasbihHistory.collectAsState(initial = emptySet())
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    val tapScale = remember { Animatable(1f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tasbih_counter),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Select Dhikr
            Text(
                text = stringResource(R.string.tasbih_select_dhikr),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            val dhikrPresets = listOf("سبحان الله", "الحمد لله", "لا إله إلا الله", "الله أكبر")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dhikrPresets.forEach { preset ->
                    FilterChip(
                        selected = dhikr == preset,
                        onClick = { viewModel.setTasbihDhikr(preset) },
                        label = { Text(preset, fontSize = 12.sp) }
                    )
                }
            }

            // Target presets
            Text(
                text = stringResource(R.string.tasbih_target, target),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            val targetPresets = listOf(33, 99, 100)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                targetPresets.forEach { preset ->
                    FilterChip(
                        selected = target == preset,
                        onClick = { viewModel.setTasbihTarget(preset) },
                        label = { Text(preset.toString()) }
                    )
                }
                // Custom target option
                var showCustomTargetDialog by remember { mutableStateOf(false) }
                FilterChip(
                    selected = !targetPresets.contains(target),
                    onClick = { showCustomTargetDialog = true },
                    label = { Text(stringResource(R.string.tasbih_custom)) }
                )

                if (showCustomTargetDialog) {
                    var inputVal by remember { mutableStateOf(target.toString()) }
                    AlertDialog(
                        onDismissRequest = { showCustomTargetDialog = false },
                        title = { Text(stringResource(R.string.tasbih_custom)) },
                        text = {
                            OutlinedTextField(
                                value = inputVal,
                                onValueChange = { inputVal = it.filter { char -> char.isDigit() } },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newTarget = inputVal.toIntOrNull() ?: 33
                                viewModel.setTasbihTarget(newTarget)
                                showCustomTargetDialog = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomTargetDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Huge tap button with progress indicator
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = tapScale.value
                        scaleY = tapScale.value
                    }
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        
                        // Spring animation
                        scope.launch {
                            tapScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessHigh))
                            tapScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }

                        val nextCount = count + 1
                        if (nextCount >= target) {
                            // Milestone reached!
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val timings = longArrayOf(0, 100, 50, 100, 50, 100)
                                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                                }
                            } catch (e: Exception) {}
                            
                            val timestamp = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US))
                            viewModel.addTasbihHistoryItem("${target}x $dhikr • $timestamp")
                            viewModel.resetTasbih()
                        } else {
                            viewModel.incrementTasbih()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Circular progress ring on the edge
                CircularProgressIndicator(
                    progress = { count.toFloat() / target.toFloat() },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dhikr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ $target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History section
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearTasbihHistory() }) {
                        Text(stringResource(R.string.reset_count), color = MaterialTheme.colorScheme.error)
                    }
                }
                
                // Show last 3 items from history
                val historyList = history.toList().sortedDescending().take(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    historyList.forEach { historyItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = historyItem,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

