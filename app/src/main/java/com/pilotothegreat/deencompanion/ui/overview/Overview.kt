// FIXED: Real-time countdown counter loop, location detection warning card, Qibla card subtitle, arabic font support, and last updated timestamp
package com.pilotothegreat.deencompanion.ui.overview

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Vibrator
import android.os.Build
import android.os.VibrationEffect
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material.icons.filled.Mic
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.SettingsKey
import com.pilotothegreat.deencompanion.ui.navigation.QiblaKey
import androidx.compose.ui.text.style.TextOverflow
import com.pilotothegreat.deencompanion.util.TOP_BAR_HEIGHT
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material.icons.filled.CompassCalibration
import java.time.chrono.HijrahDate
import java.util.Locale
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberCookie12SidedShape(): Shape {
    val path = Cookie12Sided.toPath()
    return remember(path) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val newPath = Path().apply {
                    addPath(path)
                }
                val matrix = android.graphics.Matrix().apply {
                    postScale(size.width, size.height)
                }
                newPath.asAndroidPath().transform(matrix)
                return Outline.Generic(newPath)
            }
        }
    }
}

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

    val y = Math.sin(lambdaK - lambdaVal) * Math.cos(phiK)
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

fun getLocalizedInspirationRef(ref: String, lang: String): String {
    if (lang != "ar") return ref
    return ref.replace("Quran", "القرآن")
        .replace("Bukhari & Muslim", "البخاري ومسلم")
        .replace("Bukhari", "البخاري")
        .replace("1", "١")
        .replace("2", "٢")
        .replace("3", "٣")
        .replace("4", "٤")
        .replace("5", "٥")
        .replace("6", "٦")
        .replace("7", "٧")
        .replace("8", "٨")
        .replace("9", "٩")
        .replace("0", "٠")
}

fun String.toArabicNumerals(): String {
    return this.replace("1", "١")
        .replace("2", "٢")
        .replace("3", "٣")
        .replace("4", "٤")
        .replace("5", "٥")
        .replace("6", "٦")
        .replace("7", "٧")
        .replace("8", "٨")
        .replace("9", "٩")
        .replace("0", "٠")
}

fun calculateNextPrayer(
    times: PrayerTimeCalculator.PrayerTimes,
    timezoneId: String,
    context: Context
): NextPrayer {
    val zoneId = ZoneId.systemDefault()
    val now = LocalDateTime.now(zoneId)
    val localTime = now.toLocalTime()

    val prayers = listOf(
        Pair("Fajr", times.fajr),
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

    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val lang = sharedPrefs.getString("app_language", "ar") ?: "ar"

    val remainingStr = if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    val finalRemainingStr = remainingStr

    val timeStr = nextTime.toLocaleHourString(context)
    return NextPrayer(nextName, finalRemainingStr, timeStr, totalSeconds)
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

    // Date Header setup moved to top-level scope
    val locale = remember(lang) { Locale(lang) }
    val gregFormatter = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
    val hijriFormatter = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val gregDateStr = remember(locale) { LocalDate.now().format(gregFormatter) }
    val hijriMethod by appPreferenceRepo.hijriCalendarMethod.collectAsState(initial = com.pilotothegreat.deencompanion.database.HijriMethod.UMM_AL_QURA)
    val hijriDateStr = remember(locale, hijriMethod) { 
        try {
            val baseDays = if (hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL) 1L else 0L
            val targetLocalDate = LocalDate.now().plusDays(baseDays)
            val hijri = HijrahDate.from(targetLocalDate)
            val formatted = hijri.format(hijriFormatter) + if (locale.language == "ar") " هـ" else " AH"
            if (hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL) {
                val label = if (locale.language == "ar") " (قد يختلف حسب الرؤية المحلية)" else " (May differ by local sighting)"
                formatted + label
            } else {
                formatted
            }
        } catch (e: Exception) {
            ""
        }
    }

    var hasLocationPermission by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        viewModel.refreshLocation(context)
        onPauseOrDispose {
        }
    }

    val times by viewModel.prayerTimes.collectAsState(initial = viewModel.prayerTimes.value)
    val tz by viewModel.timezoneId.collectAsState(initial = "Asia/Riyadh")
    val city by viewModel.cityName.collectAsState(initial = "")
    val lat by viewModel.latitude.collectAsState(initial = 21.3891)
    val lon by viewModel.longitude.collectAsState(initial = 39.8579)
    var nextPrayer by remember { mutableStateOf(NextPrayer("Fajr", "--", "--", 0L)) }

    var shownThisSession by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showDonationBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shownThisSession) {
            val count = appPreferenceRepo.appLaunchCount.first()
            val dismissed = appPreferenceRepo.donationPromptDismissed.first()
            val lastShow = appPreferenceRepo.lastDonationPromptShowTime.first()
            val now = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000

            if (count >= 5 && !dismissed && (now - lastShow >= sevenDaysInMillis)) {
                showDonationDialog = true
                shownThisSession = true
            }
        }
    }
    
    val currentTimes by rememberUpdatedState(times)
    val currentTz by rememberUpdatedState(tz)
    val currentContext by rememberUpdatedState(context)
    
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lang, lifecycleOwner, times, tz) {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(true) {
                nextPrayer = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    calculateNextPrayer(currentTimes, currentTz, currentContext)
                }
                if (isRobolectric) break
                val delayMs = 1000L
                kotlinx.coroutines.delay(delayMs)
            }
        }
    }

    val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val paddingTop = paddingValues.calculateTopPadding()
    val paddingBottom = paddingValues.calculateBottomPadding()

    val isRefreshing by viewModel.isRefreshingLocation.collectAsState()
    val pullState = rememberPullToRefreshState()
    val cookie12SidedShape = rememberCookie12SidedShape()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.refreshLocation(context)
            },
            state = pullState,
            indicator = {
                val trigger = pullState.distanceFraction
                val isRobolectric = remember {
                    try {
                        Class.forName("org.robolectric.Robolectric") != null
                    } catch (e: Exception) {
                        false
                    }
                }
                val refreshingRotation = if (isRobolectric) {
                    0f
                } else {
                    val rotationTransition = rememberInfiniteTransition(label = "pull_to_refresh_rotation")
                    val rot by rotationTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )
                    rot
                }

                if (trigger > 0f || isRefreshing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(56.dp)
                            .graphicsLayer {
                                translationY = if (isRefreshing) {
                                    24.dp.toPx()
                                } else {
                                    ((trigger * 80.dp.toPx()) - 56.dp.toPx()).coerceAtLeast(0f)
                                }
                                scaleX = if (isRefreshing) 1f else trigger.coerceIn(0f, 1f)
                                scaleY = if (isRefreshing) 1f else trigger.coerceIn(0f, 1f)
                                rotationZ = if (isRefreshing) refreshingRotation else trigger * 360f
                            }
                            .clip(cookie12SidedShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
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



        // Location Not Detected Warning Card
        val locationWarningDismissed by viewModel.locationWarningDismissed.collectAsState()
        val showWarning = !locationWarningDismissed && (city.isEmpty() || (lat == 21.3891 && lon == 39.8579) || !hasLocationPermission)

        if (showWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        viewModel.refreshLocation(context)
                        viewModel.dismissLocationWarning()
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
                val baseDays = if (hijriMethod == com.pilotothegreat.deencompanion.database.HijriMethod.REGIONAL) 1L else 0L
                val targetLocalDate = LocalDate.now().plusDays(baseDays)
                HijrahDate.from(targetLocalDate)
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
                        OverviewItems(viewModel, nextPrayer.name, navigator)
                    }
                },
                spacing = 16.dp
            )
        } else {
            HeroItems(scrollState, viewModel, nextPrayer)
            OverviewItems(viewModel, nextPrayer.name, navigator)
        }
        Box(Modifier.height(paddingBottom - 8.dp))
    }
}
    PageTitle(false, hazeState, "") {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(
                    text = hijriDateStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val displayedCity = if (city == "Makkah, Saudi Arabia" || city.isEmpty()) {
                    stringResource(R.string.default_location)
                } else {
                    city
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = com.pilotothegreat.deencompanion.util.localizeCityName(displayedCity, lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = { navigator.goTo(SettingsKey) }
            ) {
                Icon(
                    painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showDonationDialog = false
                            coroutineScope.launch {
                                appPreferenceRepo.setLastDonationPromptShowTime(System.currentTimeMillis())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.later))
                    }
                    TextButton(
                        onClick = {
                            showDonationDialog = false
                            coroutineScope.launch {
                                appPreferenceRepo.setDonationPromptDismissed(true)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.never_show_again))
                    }
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

    val isRobolectric = remember {
        try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }
    }
    val rotation = if (isRobolectric) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "hero_rotation")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(65000, easing = LinearEasing)
            ),
            label = "rotation"
        )
        rot
    }

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

    val cdCountdown = stringResource(R.string.cd_prayer_countdown)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .semantics { contentDescription = cdCountdown }
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
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 82.sp)) {
                        append(nextPrayer.remainingTimeStr)
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 26.sp)) {
                        appendLine()
                        append(nextPrayerText)
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily2, fontSize = 18.sp)) {
                        append(nextAtText)
                    }
                }
            )
        }
    }
}

@Composable
fun OverviewItems(viewModel: OverviewVM, nextPrayerName: String, navigator: Navigator) {
    val times by viewModel.prayerTimes.collectAsState()
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val fajrOffset by viewModel.fajrIqamaOffset.collectAsState(initial = 25)
    val dhuhrOffset by viewModel.dhuhrIqamaOffset.collectAsState(initial = 25)
    val asrOffset by viewModel.asrIqamaOffset.collectAsState(initial = 20)
    val maghribOffset by viewModel.maghribIqamaOffset.collectAsState(initial = 10)
    val ishaOffset by viewModel.ishaIqamaOffset.collectAsState(initial = 20)

    val fajrIsFixed by viewModel.fajrIqamaIsFixed.collectAsState(initial = false)
    val dhuhrIsFixed by viewModel.dhuhrIqamaIsFixed.collectAsState(initial = true)
    val asrIsFixed by viewModel.asrIqamaIsFixed.collectAsState(initial = false)
    val maghribIsFixed by viewModel.maghribIqamaIsFixed.collectAsState(initial = false)
    val ishaIsFixed by viewModel.ishaIqamaIsFixed.collectAsState(initial = false)

    val fajrIqamaTimeVal by viewModel.fajrIqamaTime.collectAsState(initial = "05:15")
    val dhuhrIqamaTimeVal by viewModel.dhuhrIqamaTime.collectAsState(initial = "12:50")
    val asrIqamaTimeVal by viewModel.asrIqamaTime.collectAsState(initial = "15:45")
    val maghribIqamaTimeVal by viewModel.maghribIqamaTime.collectAsState(initial = "18:45")
    val ishaIqamaTimeVal by viewModel.ishaIqamaTime.collectAsState(initial = "20:15")

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

        val fajrIqama = if (fajrIsFixed) {
            try { LocalTime.parse(fajrIqamaTimeVal) } catch (e: Exception) { times.fajr.plusMinutes(fajrOffset.toLong()) }
        } else {
            times.fajr.plusMinutes(fajrOffset.toLong())
        }
        val dhuhrIqama = if (dhuhrIsFixed) {
            try { LocalTime.parse(dhuhrIqamaTimeVal) } catch (e: Exception) { times.dhuhr.plusMinutes(dhuhrOffset.toLong()) }
        } else {
            times.dhuhr.plusMinutes(dhuhrOffset.toLong())
        }
        val asrIqama = if (asrIsFixed) {
            try { LocalTime.parse(asrIqamaTimeVal) } catch (e: Exception) { times.asr.plusMinutes(asrOffset.toLong()) }
        } else {
            times.asr.plusMinutes(asrOffset.toLong())
        }
        val maghribIqama = if (maghribIsFixed) {
            try { LocalTime.parse(maghribIqamaTimeVal) } catch (e: Exception) { times.maghrib.plusMinutes(maghribOffset.toLong()) }
        } else {
            times.maghrib.plusMinutes(maghribOffset.toLong())
        }
        val ishaIqama = if (ishaIsFixed) {
            try { LocalTime.parse(ishaIqamaTimeVal) } catch (e: Exception) { times.isha.plusMinutes(ishaOffset.toLong()) }
        } else {
            times.isha.plusMinutes(ishaOffset.toLong())
        }

        val prayers = listOf(
            Triple(stringResource(R.string.fajr),   times.fajr,    fajrIqama),
            Triple(stringResource(R.string.sunrise), times.sunrise, null),
            Triple(stringResource(R.string.dhuhr),  times.dhuhr,   dhuhrIqama),
            Triple(stringResource(R.string.asr),    times.asr,     asrIqama),
            Triple(stringResource(R.string.maghrib),times.maghrib, maghribIqama),
            Triple(stringResource(R.string.isha),   times.isha,    ishaIqama)
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
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isNext) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Bold
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = time.toLocaleHourString(context),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isNext) colorScheme.onPrimaryContainer else colorScheme.primary
                            )
                            if (offset != null) {
                                Text(
                                    text = stringResource(R.string.iqama_time, offset.toLocaleHourString(context)),
                                    style = MaterialTheme.typography.titleMedium,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiveQiblaCompassCard(
                viewModel = viewModel,
                navigator = navigator,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            TasbihDialCard(
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

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
                if (lang == "ar") {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "\"$quote\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                fontFamily = arabicFontFamily,
                                fontSize = 22.sp,
                                lineHeight = 32.sp
                            ),
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        ),
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = "— ${getLocalizedInspirationRef(currentInspiration.ref, lang)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.secondary,
                    modifier = Modifier.align(if (lang == "ar") Alignment.Start else Alignment.End)
                )
            }
        }

    }
}

private fun getSmoothRotation(target: Float, current: Float): Float {
    var diff = (target - current) % 360f
    if (diff < -180f) diff += 360f
    if (diff > 180f) diff -= 360f
    return current + diff
}

@Composable
fun LiveQiblaCompassCard(
    viewModel: OverviewVM,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")
    val lat by viewModel.latitude.collectAsState(initial = 21.3891)
    val lon by viewModel.longitude.collectAsState(initial = 39.8579)
    val qiblaBearing = remember(lat, lon) { calculateQiblaDirection(lat, lon).toFloat() }
    val cookie12SidedShape = rememberCookie12SidedShape()
    val cdQibla = stringResource(R.string.cd_qibla_compass)

    val declination = remember(lat, lon) {
        try {
            val geoField = android.hardware.GeomagneticField(
                lat.toFloat(),
                lon.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            geoField.declination
        } catch (e: Exception) {
            0f
        }
    }

    val hasCompass = remember(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }

    var rawHeading by remember { mutableStateOf(0f) }
    var smoothHeading by remember { mutableStateOf(0f) }

    DisposableEffect(context, lat, lon) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val heading = (azimuth + declination + 360f) % 360f
                    rawHeading = heading
                    smoothHeading = getSmoothRotation(heading, smoothHeading)
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravity = event.values.clone()
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagnetic = event.values.clone()
                    }
                    val g = gravity
                    val m = geomagnetic
                    if (g != null && m != null) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, g, m)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            val heading = (azimuth + declination + 360f) % 360f
                            rawHeading = heading
                            smoothHeading = getSmoothRotation(heading, smoothHeading)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (hasCompass) {
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                if (accelerometer != null) {
                    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                }
                if (magnetometer != null) {
                    sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
                }
            }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedHeading by animateFloatAsState(
        targetValue = smoothHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val relativeAngle = qiblaBearing - animatedHeading

    val isAligned = remember(relativeAngle) {
        val rel = (relativeAngle + 360f) % 360f
        rel < 8f || rel > 352f
    }

    val vibrator = remember(context) { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            } catch (e: Exception) {}
        }
        wasAligned = isAligned
    }

    val needleScale by animateFloatAsState(
        targetValue = if (isAligned) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alignmentScale by animateFloatAsState(
        targetValue = if (isAligned) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val outlineVariantColor = colorScheme.outlineVariant
    val secondaryColor = colorScheme.secondary.copy(alpha = 0.5f)
    val primaryColor = colorScheme.primary
    val outlineColor = colorScheme.outline
    val onPrimaryContainerColor = colorScheme.onPrimaryContainer

    val compassRingColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF2E7D32) else outlineVariantColor
    )
    val needleColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF2E7D32) else primaryColor
    )

    Card(
        modifier = modifier
            .semantics { contentDescription = cdQibla }
            .graphicsLayer {
                scaleX = cardScale * alignmentScale
                scaleY = cardScale * alignmentScale
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.qibla_compass),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!hasCompass) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == "ar") "البوصلة غير متوفرة" else "Compass Unavailable",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(cookie12SidedShape)
                        .background(colorScheme.primaryContainer.copy(alpha = if (isAligned) 0.22f else 0.12f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAligned) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                radius = (size.minDimension / 2) + 4.dp.toPx()
                            )
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        drawCircle(
                            color = compassRingColor,
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = relativeAngle
                                scaleX = needleScale
                                scaleY = needleScale
                            }
                    ) {
                        val radius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        drawLine(
                            color = secondaryColor,
                            start = center,
                            end = Offset(center.x, center.y - radius + 8.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )

                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - radius)
                            lineTo(center.x - 6.dp.toPx(), center.y)
                            lineTo(center.x + 6.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(
                            path = needlePath,
                            color = needleColor
                        )

                        val southPath = Path().apply {
                            moveTo(center.x, center.y + radius)
                            lineTo(center.x - 6.dp.toPx(), center.y)
                            lineTo(center.x + 6.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(
                            path = southPath,
                            color = outlineColor
                        )

                        drawCircle(
                            color = onPrimaryContainerColor,
                            radius = 4.dp.toPx()
                        )
                    }
                }
            }

            Text(
                text = if (lang == "ar") {
                    if (isAligned) "محاذٍ للقبلة!" else "${com.pilotothegreat.deencompanion.ui.quran.toArabicNumerals(qiblaBearing.toInt())}°"
                } else {
                    if (isAligned) "Aligned!" else stringResource(R.string.degrees_symbol, qiblaBearing)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isAligned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TasbihDialCard(
    viewModel: OverviewVM,
    modifier: Modifier = Modifier
) {
    val count by viewModel.tasbihCount.collectAsState(initial = 0)
    val dhikr by viewModel.tasbihDhikr.collectAsState(initial = "سبحان الله")
    val target by viewModel.tasbihTarget.collectAsState(initial = 33)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val localizedDhikr = remember(dhikr) {
        when (dhikr) {
            "سبحان الله" -> context.getString(R.string.tasbih_dhikr_subhanallah)
            "الحمد لله" -> context.getString(R.string.tasbih_dhikr_alhamdulillah)
            "لا إله إلا الله" -> context.getString(R.string.tasbih_dhikr_lailahaillallah)
            "الله أكبر" -> context.getString(R.string.tasbih_dhikr_allahuakbar)
            else -> dhikr
        }
    }

    val progress = remember(count, target) {
        count.toFloat() / target.toFloat().coerceAtLeast(1f)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dhikr Title
            Text(
                text = localizedDhikr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Bouncy Dial Circle (Tap Area)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(4.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .semantics { contentDescription = context.getString(R.string.cd_tasbih_button) }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        scope.launch {
                            scale.animateTo(0.85f, spring(stiffness = Spring.StiffnessHigh))
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        val nextCount = count + 1
                        if (nextCount >= target) {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(120)
                                }
                            } catch (e: Exception) {}
                        }
                        viewModel.incrementTasbih()
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeWidth = 6.dp
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Controls Row (Cycle Dhikr, Reset Count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cycle Dhikr Button
                IconButton(
                    onClick = {
                        val nextDhikr = when (dhikr) {
                            "سبحان الله" -> "الحمد لله"
                            "الحمد لله" -> "الله أكبر"
                            else -> "سبحان الله" // AllahuAkbar loops back to SubhanAllah (Sunnah 3-dhikr cycle)
                        }
                        viewModel.setTasbihDhikr(nextDhikr)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cycle Dhikr",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Reset Button
                IconButton(
                    onClick = { viewModel.resetTasbih() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Reset Count",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

