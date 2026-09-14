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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.pilotothegreat.deencompanion.ui.components.MorphBadge
import com.pilotothegreat.deencompanion.ui.components.squashOnPress
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
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
import com.pilotothegreat.deencompanion.ui.theme.Spacing
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
        Row(Modifier.padding(Spacing.xxlarge), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                Text(stringResource(R.string.next_prayer_label), style = MaterialTheme.typography.labelLarge)
                Text(name, style = MaterialTheme.typography.headlineMediumEmphasized)
                Odometer(
                    text = remaining,
                    style = MaterialTheme.typography.displayMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = countdownDescription
                        liveRegion = LiveRegionMode.Polite
                    },
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
    /** Prayers the reader has marked prayed today. */
    prayed: Set<Prayer>,
    /** Days in the last thirty on which anything was marked; 0 hides the line entirely. */
    daysObserved: Int,
    locale: Locale,
    onToggleMute: (Prayer, Boolean) -> Unit,
    onTogglePrayed: (Prayer, Boolean) -> Unit,
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
                leadingContent = {
                    // Tapping the icon marks the prayer prayed. Every tap on the notification's
                    // "Prayed" has been recorded since 1.8.0 and shown nowhere; this is where it
                    // shows, and the only place it can be corrected.
                    if (prayer.isObligatory) {
                        val done = prayer in prayed
                        IconToggleButton(checked = done, onCheckedChange = { onTogglePrayed(prayer, it) }) {
                            Icon(
                                imageVector = if (done) Icons.Rounded.CheckCircle else prayer.icon,
                                contentDescription = stringResource(if (done) R.string.unmark_prayed else R.string.mark_prayed, name),
                                tint = if (done) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                    } else {
                        Icon(prayer.icon, contentDescription = null)
                    }
                },
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
        // Days on which anything was marked, not a score out of five. It appears only once there is
        // something to say, and it says it once, quietly.
        if (daysObserved > 0) {
            Text(
                pluralStringResource(R.plurals.days_observed, daysObserved, Formatters.number(daysObserved, locale)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.large, top = Spacing.small),
            )
        }
    }
}

/** The athkar that fit the time of day, one tap away. */
@Composable
fun AthkarNowCard(category: AthkarCategory, progress: DayProgress, locale: Locale, onOpen: () -> Unit) {
    val fraction by animateFloatAsState(progress.fraction(category), MaterialTheme.motionScheme.slowSpatialSpec(), label = "athkarNow")
    val press = remember { MutableInteractionSource() }
    Card(
        onClick = onOpen,
        modifier = Modifier.squashOnPress(press),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        interactionSource = press,
    ) {
        Row(Modifier.padding(Spacing.xlarge), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
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

@Composable
fun QiblaShortcut(location: SavedLocation, locale: Locale, onOpen: () -> Unit) {
    val bearing = remember(location) { QiblaMath.bearing(location.latitude, location.longitude) }
    val distance = remember(location) { QiblaMath.distanceKm(location.latitude, location.longitude) }
    val press = remember { MutableInteractionSource() }
    Card(
        onClick = onOpen,
        modifier = Modifier.squashOnPress(press),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        interactionSource = press,
    ) {
        Row(Modifier.padding(Spacing.xlarge), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
            // The top bar's Qibla button is this same compass in this same cookie.
            MorphBadge(
                Icons.Rounded.Explore,
                press,
                rest = MaterialShapes.Cookie7Sided,
                pressed = MaterialShapes.Cookie4Sided,
                size = 56.dp,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
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


/**
 * One thing the engine decided is worth saying now. Every occasion, every dua for the weather and
 * every travel note uses this one card, so adding a producer never means adding a screen.
 */
@Composable
fun MomentCard(
    moment: Moment,
    locale: Locale,
    onOpen: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** A named answer, for the few moments that ask a question instead of stating something. */
    answer: Pair<String, () -> Unit>? = null,
    decline: Pair<String, () -> Unit>? = null,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val press = remember { MutableInteractionSource() }
    val content: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.fillMaxWidth().padding(Spacing.xlarge), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                MorphBadge(
                    moment.kind.icon,
                    press,
                    size = 36.dp,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
                Text(stringResource(moment.title), style = MaterialTheme.typography.titleMedium)
            }
            moment.body?.let { body ->
                Text(
                    if (moment.count != null) stringResource(body, Formatters.number(moment.count, locale)) else stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (answer != null || decline != null || onDismiss != null) {
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                    decline?.let { (label, action) -> TextButton(onClick = action) { Text(label) } }
                    if (answer == null && onDismiss != null) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
                    }
                    answer?.let { (label, action) -> TextButton(onClick = action) { Text(label) } }
                }
            }
        }
    }
    // A card that can be sent away can be swiped away. The engine has modelled Dismissal since
    // 1.8.0 and the only way to act on it was a text button in the corner.
    val swipeable: @Composable (@Composable () -> Unit) -> Unit = { card ->
        if (onDismiss == null) {
            card()
        } else {
            val state = rememberSwipeToDismissBoxState(
                positionalThreshold = { it * SWIPE_AWAY },
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) onDismiss()
                    true
                },
            )
            SwipeToDismissBox(
                state = state,
                backgroundContent = {},
                content = { card() },
            )
        }
    }
    swipeable {
        if (onOpen != null) {
            Card(
                onClick = onOpen,
                modifier = modifier.squashOnPress(press),
                shape = MaterialTheme.shapes.extraLarge,
                colors = colors,
                interactionSource = press,
                content = content,
            )
        } else {
            Card(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, colors = colors, content = content)
        }
    }
}

/** How far a card must travel before letting go sends it away. */
private const val SWIPE_AWAY = 0.5f

private val MomentKind.icon: ImageVector
    get() = when (this) {
        MomentKind.OCCASION -> Icons.Rounded.NightsStay
        MomentKind.NATURE -> Icons.Rounded.Cloud
        MomentKind.TRAVEL -> Icons.Rounded.Flight
        MomentKind.PLAN -> Icons.AutoMirrored.Rounded.MenuBook
        MomentKind.MAINTENANCE -> Icons.Rounded.Build
    }
