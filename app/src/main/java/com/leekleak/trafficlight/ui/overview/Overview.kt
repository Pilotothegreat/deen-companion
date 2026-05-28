package com.leekleak.trafficlight.ui.overview

import android.content.Context
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.SettingsKey
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.EqualHeightRow
import com.leekleak.trafficlight.util.MiniCard
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import com.leekleak.trafficlight.util.toLocaleHourString
import com.leekleak.trafficlight.util.px
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

@Composable
fun Overview(
    paddingValues: PaddingValues,
) {
    val viewModel: OverviewVM = koinViewModel()
    val navigator: Navigator = koinInject()
    val context = LocalContext.current

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val hazeState = rememberHazeState()
    val scrollState = rememberScrollState()

    LifecycleResumeEffect(Unit) {
        // Automatically check/refresh location on start
        viewModel.refreshLocation(context)
        onPauseOrDispose {}
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
        val gregFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()) }
        val hijriFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()) }
        val gregDateStr = remember { LocalDate.now().format(gregFormatter) }
        val hijriDateStr = remember { 
            try {
                val hijri = HijrahDate.now()
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
        }

        if (windowSizeClass.isWidthAtLeastBreakpoint(400)) {
            EqualHeightRow (
                modifier = Modifier.padding(horizontal = 16.dp),
                first = {
                    Column (Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        HeroItems(scrollState, viewModel)
                    }
                },
                second = {
                    Column (Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewItems(viewModel)
                    }
                },
                spacing = 16.dp
            )
        } else {
            HeroItems(scrollState, viewModel)
            OverviewItems(viewModel)
        }
        Box(Modifier.height(paddingBottom - 8.dp))
    }
    PageTitle(false, hazeState, "Deen") {
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
}

@Composable
private fun HeroItems(scrollState: ScrollState, viewModel: OverviewVM) {
    OverviewHero(scrollState, viewModel)
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LocationCard(viewModel)
        CalculationsCard(viewModel)
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QiblaCard(viewModel)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewHero(scrollState: ScrollState, viewModel: OverviewVM) {
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

    val times by viewModel.prayerTimes.collectAsState(initial = viewModel.prayerTimes.value)
    val tz by viewModel.timezoneId.collectAsState(initial = "Asia/Dubai")

    var nextPrayer by remember { mutableStateOf(NextPrayer("Fajr", "--", "--", 0L)) }
    // Timer to update countdown dynamically (every second if close)
    LaunchedEffect(times, tz) {
        while(true) {
            nextPrayer = calculateNextPrayer(times, tz, context)
            val delayMs = if (nextPrayer.durationSeconds < 3600) 1000L else 10000L
            kotlinx.coroutines.delay(delayMs)
        }
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
private fun RowScope.LocationCard(viewModel: OverviewVM) {
    val city by viewModel.cityName.collectAsState(initial = "Dubai, UAE")
    val refreshing by viewModel.isRefreshingLocation.collectAsState(initial = false)
    val context = LocalContext.current

    MiniCard(
        state = MiniCardState.NEUTRAL,
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.LocationOn),
        title = "Location"
    ) { fontFamily ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                fontFamily = fontFamily,
                text = city,
                fontSize = 16.sp,
                maxLines = 1
            )
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { viewModel.refreshLocation(context) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.CalculationsCard(viewModel: OverviewVM) {
    val method by viewModel.calcMethod.collectAsState(initial = PrayerTimeCalculator.CalculationMethod.MWL)
    MiniCard(
        state = MiniCardState.NEUTRAL,
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Schedule),
        title = "Method"
    ) { fontFamily ->
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontFamily = fontFamily,
            text = method.name,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RowScope.QiblaCard(viewModel: OverviewVM) {
    val lat by viewModel.latitude.collectAsState(initial = 21.3891)
    val lon by viewModel.longitude.collectAsState(initial = 39.8579)
    val angle = remember(lat, lon) { calculateQiblaDirection(lat, lon) }
    val scheme = colorScheme
    MiniCard(
        state = MiniCardState.NEUTRAL,
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.CompassCalibration),
        title = stringResource(R.string.qibla_direction)
    ) { fontFamily ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                fontFamily = fontFamily,
                text = String.format(Locale.US, "%.1f° from North", angle),
                fontSize = 16.sp
            )
            Canvas(modifier = Modifier.size(24.dp)) {
                rotate(angle.toFloat()) {
                    val path = Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width / 2 + 4.dp.toPx(), size.height)
                        lineTo(size.width / 2 - 4.dp.toPx(), size.height)
                        close()
                    }
                    drawPath(path, color = scheme.primary)
                }
            }
        }
    }
}

@Composable
fun OverviewItems(viewModel: OverviewVM) {
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
        Box(
            modifier = Modifier
                .card()
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerRow(stringResource(R.string.fajr), times.fajr, context, fajrOffset)
                PrayerRow(stringResource(R.string.sunrise), times.sunrise, context, null)
                PrayerRow(stringResource(R.string.dhuhr), times.dhuhr, context, dhuhrOffset)
                PrayerRow(stringResource(R.string.asr), times.asr, context, asrOffset)
                PrayerRow(stringResource(R.string.maghrib), times.maghrib, context, maghribOffset)
                PrayerRow(stringResource(R.string.isha), times.isha, context, ishaOffset)
            }
        }

        // Daily Verse Card
        val inspirationIndex = remember { LocalDate.now().dayOfYear % inspirations.size }
        val currentInspiration = inspirations[inspirationIndex]
        CategoryTitleText(stringResource(R.string.daily_inspiration))
        
        val quote = if (lang == "ar") currentInspiration.ar else currentInspiration.en
        val arabicFontFamily = remember { FontFamily(Font(R.font.scheherazade_new)) }
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
        CategoryTitleText(stringResource(R.string.tasbih_counter))
        Box(
            modifier = Modifier
                .card()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    viewModel.incrementTasbih()
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
                    Text(stringResource(R.string.tasbih_count_label), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.tasbih_tap_hint), style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.resetTasbih()
                    }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset Count")
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerRow(name: String, time: LocalTime, context: Context, iqamaOffset: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = time.toLocaleHourString(context),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary
            )
            if (iqamaOffset != null) {
                val iqamaTime = time.plusMinutes(iqamaOffset.toLong())
                Text(
                    text = stringResource(R.string.iqama_time, iqamaTime.toLocaleHourString(context)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.secondary
                )
            }
        }
    }
}
