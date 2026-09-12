package com.pilotothegreat.deencompanion.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.moment.MomentKind
import com.pilotothegreat.deencompanion.core.moment.Moment
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.core.text.Inspiration
import com.pilotothegreat.deencompanion.data.settings.SavedLocation
import com.pilotothegreat.deencompanion.ui.athkar.athkarIcon
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.location.locationStatus
import com.pilotothegreat.deencompanion.ui.quran.verseReference
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import com.pilotothegreat.deencompanion.ui.theme.animatedPolygonShape
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import com.pilotothegreat.deencompanion.ui.theme.shape
import java.util.Locale
import kotlin.math.roundToInt

/** Next prayer with a live countdown; the shape morphs as each prayer arrives. */
@Composable
fun NextPrayerHero(countdown: Countdown, locale: Locale, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val next = countdown.next
    val name = stringResource(next.prayer.nameRes)
    val remaining = Formatters.countdown(countdown.remaining, locale)
    val adhanTime = Formatters.time(context, next.adhan.toLocalTime(), locale)
    val iqama = next.iqama?.takeIf { it != next.adhan }
    val shape = animatedPolygonShape(next.prayer.shape)
    val countdownDescription = stringResource(R.string.cd_countdown, name, remaining)

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        // One focus stop for screen readers: label, prayer, time left and times together.
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.next_prayer_label), style = MaterialTheme.typography.labelLarge)
                Text(name, style = MaterialTheme.typography.headlineMediumEmphasized)
                Odometer(
                    text = remaining,
                    style = MaterialTheme.typography.displayMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.clearAndSetSemantics { contentDescription = countdownDescription },
                )
                Text(stringResource(R.string.adhan_at, adhanTime), style = MaterialTheme.typography.bodyLarge)
                if (iqama != null) {
                    Text(
                        stringResource(R.string.iqama_at, Formatters.time(context, iqama.toLocalTime(), locale)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Box(
                modifier = Modifier.size(88.dp).clip(shape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(next.prayer.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
            }
        }
    }
}

/** Each changed character rolls up into place like an odometer; plain text when animations are off. */
@Composable
private fun Odometer(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) {
        Text(text, style = style, modifier = modifier)
        return
    }
    val slide = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    // Clock times read left to right in Arabic too.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier) {
            text.forEachIndexed { index, char ->
                // Keyed from the end so the seconds keep their slot when the hours lose a digit.
                key(text.length - index) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            (slideInVertically(slide) { it } + fadeIn()) togetherWith
                                (slideOutVertically(slide) { -it } + fadeOut()) using SizeTransform(clip = true)
                        },
                        label = "odometer",
                    ) { Text(it.toString(), style = style) }
                }
            }
        }
    }
}

@Composable
fun PrayerTimesCard(
    schedule: PrayerSchedule,
    nextPrayer: Prayer?,
    muted: Set<Prayer>,
    notificationsEnabled: Boolean,
    locale: Locale,
    onToggleMute: (Prayer, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val rowCount = Prayer.entries.size + 1
    val defaultColors = ListItemDefaults.segmentedColors()
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        Prayer.entries.forEachIndexed { index, prayer ->
            val isNext = prayer == nextPrayer
            val name = stringResource(prayer.nameRes)
            val adhan = schedule.adhan.getValue(prayer)
            val iqama = schedule.iqama[prayer]?.takeIf { it != adhan }
            // The highlight fades across to the next prayer when one passes.
            val container by animateColorAsState(
                if (isNext) MaterialTheme.colorScheme.secondaryContainer else defaultColors.containerColor,
                MaterialTheme.motionScheme.slowEffectsSpec(),
                label = "nextPrayer",
            )
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index, rowCount),
                colors = if (isNext) {
                    ListItemDefaults.segmentedColors(containerColor = container, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                } else {
                    ListItemDefaults.segmentedColors(containerColor = container)
                },
                leadingContent = { Icon(prayer.icon, contentDescription = null) },
                supportingContent = iqama?.let {
                    { Text(stringResource(R.string.iqama_at, Formatters.time(context, it.toLocalTime(), locale))) }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Formatters.time(context, adhan.toLocalTime(), locale),
                            style = if (isNext) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.titleMedium,
                        )
                        if (prayer.isObligatory && notificationsEnabled) {
                            val isMuted = prayer in muted
                            IconToggleButton(checked = !isMuted, onCheckedChange = { onToggleMute(prayer, !it) }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                                    contentDescription = stringResource(if (isMuted) R.string.unmute_prayer else R.string.mute_prayer, name),
                                )
                            }
                        }
                    }
                },
            ) {
                Text(name, style = if (isNext) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.titleMedium)
            }
        }
        // Night times for qiyam, after the five prayers.
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(rowCount - 1, rowCount),
            leadingContent = { Icon(Icons.Rounded.NightsStay, contentDescription = null) },
            supportingContent = {
                Text(stringResource(R.string.middle_of_night_at, Formatters.time(context, schedule.middleOfNight.toLocalTime(), locale)))
            },
            trailingContent = {
                Text(Formatters.time(context, schedule.lastThirdOfNight.toLocalTime(), locale), style = MaterialTheme.typography.titleMedium)
            },
        ) {
            Text(stringResource(R.string.last_third_of_night), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** The athkar that fit the time of day, one tap away. */
@Composable
fun AthkarNowCard(category: AthkarCategory, progress: DayProgress, locale: Locale, onOpen: () -> Unit) {
    val fraction by animateFloatAsState(progress.fraction(category), MaterialTheme.motionScheme.slowSpatialSpec(), label = "athkarNow")
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.16f),
                )
                Icon(athkarIcon(category.id), contentDescription = null)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.athkar_now), style = MaterialTheme.typography.labelLarge)
                Text(category.title(locale), style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    if (progress.isComplete(category)) {
                        stringResource(R.string.athkar_done_today)
                    } else {
                        stringResource(
                            R.string.athkar_progress,
                            Formatters.number(progress.completedItems(category), locale),
                            Formatters.number(category.items.size, locale),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Today's ayah in the mushaf script; opens the mushaf at it. */
@Composable
fun VerseOfDayCard(verse: VerseOfDay, locale: Locale, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.verse_of_the_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
            Text(
                text = verse.verse.text,
                fontFamily = UthmanicHafs,
                fontSize = 24.sp,
                lineHeight = 44.sp,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!locale.isArabic) {
                Text(verse.verse.standaloneTranslation, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                verseReference(verse.surah, verse.verse.number, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
fun QiblaShortcut(location: SavedLocation, locale: Locale, onOpen: () -> Unit) {
    val bearing = remember(location) { QiblaMath.bearing(location.latitude, location.longitude) }
    val distance = remember(location) { QiblaMath.distanceKm(location.latitude, location.longitude) }
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = MaterialShapes.Cookie7Sided.toShape(),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Explore, contentDescription = null) }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.qibla_compass), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.qibla_summary,
                        Formatters.number(bearing.roundToInt(), locale),
                        Formatters.number(distance.roundToInt(), locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Where and how prayer times are calculated; opens the location picker. */
@Composable
fun LocationCard(location: SavedLocation, methodLabel: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = MaterialShapes.Clover4Leaf.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.LocationOn, contentDescription = null) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(location.cityName ?: stringResource(R.string.default_location), style = MaterialTheme.typography.titleMedium)
                if (!location.isDefault) Text(locationStatus(location), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.calculated_with, methodLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun InspirationCard(inspiration: Inspiration, locale: Locale) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.daily_inspiration), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                text = inspiration.arabic,
                fontFamily = Amiri,
                fontSize = 24.sp,
                lineHeight = 42.sp,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!locale.isArabic) {
                Text(inspiration.english, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
            Text(
                inspiration.source(locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}


/**
 * One thing the engine decided is worth saying now. Every occasion, every dua for the weather and
 * every travel note uses this one card, so adding a producer never means adding a screen.
 */
@Composable
fun MomentCard(moment: Moment, locale: Locale, onOpen: (() -> Unit)?, onDismiss: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val content: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(moment.kind.icon, contentDescription = null)
                Text(stringResource(moment.title), style = MaterialTheme.typography.titleMedium)
            }
            moment.body?.let { body ->
                Text(
                    if (moment.count != null) stringResource(body, Formatters.number(moment.count, locale)) else stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.dismiss)) }
            }
        }
    }
    if (onOpen != null) {
        Card(onClick = onOpen, modifier = modifier, shape = MaterialTheme.shapes.extraLarge, colors = colors, content = content)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, colors = colors, content = content)
    }
}

private val MomentKind.icon: ImageVector
    get() = when (this) {
        MomentKind.OCCASION -> Icons.Rounded.NightsStay
        MomentKind.NATURE -> Icons.Rounded.Cloud
        MomentKind.TRAVEL -> Icons.Rounded.Flight
        MomentKind.PLAN -> Icons.AutoMirrored.Rounded.MenuBook
        MomentKind.MAINTENANCE -> Icons.Rounded.Build
    }
