package com.pilotothegreat.deencompanion.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import com.pilotothegreat.deencompanion.core.text.Inspiration
import com.pilotothegreat.deencompanion.data.settings.SavedLocation
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.common.labelRes
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import com.pilotothegreat.deencompanion.ui.theme.animatedPolygonShape
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.next_prayer_label), style = MaterialTheme.typography.labelLarge)
                Text(name, style = MaterialTheme.typography.headlineMediumEmphasized)
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.displayMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.semantics { contentDescription = countdownDescription },
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
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        Prayer.entries.forEachIndexed { index, prayer ->
            val isNext = prayer == nextPrayer
            val name = stringResource(prayer.nameRes)
            val adhan = schedule.adhan.getValue(prayer)
            val iqama = schedule.iqama[prayer]?.takeIf { it != adhan }
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index, Prayer.entries.size),
                colors = if (isNext) {
                    ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    ListItemDefaults.segmentedColors()
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
    }
}

@Composable
fun TasbihCard(
    state: TasbihState,
    locale: Locale,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onTargetChange: (Int) -> Unit,
    onDhikrChange: (Dhikr) -> Unit,
) {
    val target = TasbihEngine.roundTarget(state)
    val progress by animateFloatAsState(
        targetValue = state.count / target.toFloat(),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tasbihProgress",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonShape = animatedPolygonShape(
        target = if (pressed) MaterialShapes.Cookie12Sided else MaterialShapes.Circle,
        spec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val tapDescription = stringResource(R.string.cd_tasbih_button)
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tasbih_counter), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onReset) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.reset_tasbih))
                }
            }
            Box {
                AssistChip(
                    onClick = { menuOpen = true },
                    label = { Text(stringResource(state.dhikr.labelRes)) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Dhikr.entries.forEach { dhikr ->
                        DropdownMenuItem(
                            text = { Text(stringResource(dhikr.labelRes)) },
                            onClick = {
                                onDhikrChange(dhikr)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(buttonShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(interactionSource = interaction, indication = ripple(), onClick = onTap)
                        .semantics {
                            contentDescription = tapDescription
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Formatters.number(state.count, locale),
                            style = MaterialTheme.typography.displayMediumEmphasized,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            stringResource(R.string.tasbih_of_target, Formatters.number(target, locale)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasbihEngine.targets.forEach { value ->
                    FilterChip(
                        selected = state.target == value,
                        onClick = { onTargetChange(value) },
                        label = { Text(Formatters.number(value, locale)) },
                    )
                }
            }
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

@Composable
fun RamadanCard(daysLeft: Int, locale: Locale, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null)
                Text(stringResource(R.string.ramadan_approaching), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                pluralStringResource(R.plurals.days_until_ramadan, daysLeft, Formatters.number(daysLeft, locale)),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            Text(stringResource(R.string.ramadan_sighting_note), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.dismiss)) }
        }
    }
}
