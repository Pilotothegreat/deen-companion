package com.pilotothegreat.deencompanion.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.ui.components.toComposePath
import com.pilotothegreat.deencompanion.ui.theme.DeenDarkColors
import com.pilotothegreat.deencompanion.ui.theme.DeenLightColors
import com.pilotothegreat.deencompanion.ui.theme.Spacing

/** What the theme picker shows as chosen. */
internal data class ThemeState(val mode: ThemeMode, val wallpaper: Boolean, val pureBlack: Boolean)

/**
 * The theme, laid out as Traffic Light (leekleak/traffic-light, GPLv3) lays out its own: a panel for the
 * wallpaper's colours beside one for Bilal's, each with Light and Dark cards over a wide Auto card, and a
 * chosen card that rounds its corners and turns and grows its shapes. Bilal adds one thing: while Dark or
 * Auto is chosen, an OLED card slides in beside Auto, and Auto gives up part of its width to it.
 *
 * It replaces a Theme row and two switches beside it, which between them said the same thing three times.
 */
@Composable
internal fun ThemePicker(
    state: ThemeState,
    onTheme: (mode: ThemeMode, wallpaper: Boolean) -> Unit,
    onPureBlack: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    wallpaperAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
) {
    val wallpaperChosen = wallpaperAvailable && state.wallpaper
    val scroll = rememberScrollState()
    // The chosen panel comes into view. maxValue is a key so this runs again once the row has been
    // measured and there is somewhere to scroll to.
    LaunchedEffect(wallpaperChosen, scroll.maxValue) {
        scroll.animateScrollTo(if (wallpaperChosen) 0 else scroll.maxValue)
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val panelWidth = if (wallpaperAvailable) PANEL_WIDTH else maxWidth - Spacing.medium * 2
        Row(
            Modifier.horizontalScroll(scroll).padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            if (wallpaperAvailable) {
                ThemePanel(wallpaper = true, state, chosen = wallpaperChosen, panelWidth, onTheme, onPureBlack)
            }
            ThemePanel(wallpaper = false, state, chosen = !wallpaperChosen, panelWidth, onTheme, onPureBlack)
        }
    }
}

@Composable
private fun ThemePanel(
    wallpaper: Boolean,
    state: ThemeState,
    chosen: Boolean,
    width: Dp,
    onTheme: (ThemeMode, Boolean) -> Unit,
    onPureBlack: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val (light, dark) = if (wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
    } else {
        DeenLightColors to DeenDarkColors
    }
    val systemDark = isSystemInDarkTheme()
    val mode = state.mode.takeIf { chosen }
    val motion = MaterialTheme.motionScheme
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            stringResource(if (wallpaper) R.string.theme_wallpaper else R.string.theme_bilal),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            PreviewCard(stringResource(R.string.theme_light), Icons.Rounded.LightMode, light, mode == ThemeMode.LIGHT, Modifier.weight(1f)) {
                onTheme(ThemeMode.LIGHT, wallpaper)
            }
            PreviewCard(stringResource(R.string.theme_dark), Icons.Rounded.DarkMode, dark, mode == ThemeMode.DARK, Modifier.weight(1f)) {
                onTheme(ThemeMode.DARK, wallpaper)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            WideCard(
                label = stringResource(R.string.theme_system),
                icon = Icons.Rounded.BrightnessAuto,
                scheme = if (systemDark) dark else light,
                backdrop = null,
                selected = mode == ThemeMode.SYSTEM,
                role = Role.RadioButton,
                modifier = Modifier.weight(1f),
            ) { onTheme(ThemeMode.SYSTEM, wallpaper) }
            // OLED black only means something at night, so it is offered only beside a dark or automatic
            // choice, springing in and taking part of Auto's width.
            AnimatedVisibility(
                visible = chosen && state.mode != ThemeMode.LIGHT,
                enter = expandHorizontally(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                exit = shrinkHorizontally(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                Row {
                    Spacer(Modifier.width(Spacing.small))
                    WideCard(
                        label = stringResource(R.string.theme_pure_black),
                        icon = Icons.Rounded.Contrast,
                        scheme = dark,
                        backdrop = Color.Black,
                        selected = state.pureBlack,
                        role = Role.Switch,
                        modifier = Modifier.width(OLED_WIDTH),
                    ) { onPureBlack(!state.pureBlack) }
                }
            }
        }
    }
}

/** Light or Dark: a small picture of the theme above its name. */
@Composable
private fun PreviewCard(label: String, icon: ImageVector, scheme: ColorScheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val motion = cardMotion(selected, restTurn = 0f, chosenTurn = -10f)
    Column(
        modifier.then(cardModifier(selected, Role.RadioButton, onClick)).padding(Spacing.hair),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(4.dp)
                .drawBehind {
                    val turn = motion.turn()
                    rotate(-turn) { drawRoundRect(scheme.background, cornerRadius = CornerRadius(motion.corner().dp.toPx())) }
                    val shapePx = motion.shape().dp.toPx()
                    val step = size.width / 7f
                    drawShape(PRIMARY_SHAPE, Offset(step * 2, size.height / 2), shapePx, turn * 2, scheme.primary)
                    drawShape(TERTIARY_SHAPE, Offset(step * 5, size.height / 2), shapePx, -turn * 2, scheme.tertiary)
                },
        )
        CardLabel(label, icon, selected, Modifier.padding(vertical = 6.dp))
    }
}

/** Auto, and OLED beside it: the name over the theme's shapes. */
@Composable
private fun WideCard(
    label: String,
    icon: ImageVector,
    scheme: ColorScheme,
    backdrop: Color?,
    selected: Boolean,
    role: Role,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    // Smaller shapes than Traffic Light's, held to the edges: Auto narrows when OLED arrives, and at full
    // size the shapes covered its name.
    val motion = cardMotion(selected, restTurn = -15f, chosenTurn = -5f, restShape = 28f, chosenShape = 32f)
    Box(
        modifier
            .then(cardModifier(selected, role, onClick))
            .height(WIDE_HEIGHT)
            .drawBehind {
                if (backdrop != null) {
                    // The black is the picture: OLED needs no shapes over it.
                    inset(4.dp.toPx()) { drawRoundRect(backdrop, cornerRadius = CornerRadius(motion.corner().dp.toPx())) }
                } else {
                    val shapePx = motion.shape().dp.toPx()
                    val edge = 26.dp.toPx()
                    drawShape(PRIMARY_SHAPE, Offset(edge, size.height / 2), shapePx, motion.turn() * 2, scheme.primary)
                    drawShape(TERTIARY_SHAPE, Offset(size.width - edge, size.height / 2), shapePx, -motion.turn() * 2, scheme.tertiary)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CardLabel(label, icon, selected, color = if (backdrop != null) scheme.onSurface else null)
    }
}

@Composable
private fun CardLabel(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, color: Color? = null) {
    val tint = color ?: if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun cardModifier(selected: Boolean, role: Role, onClick: () -> Unit): Modifier {
    val haptics = rememberHaptics()
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val press = {
        haptics.click()
        onClick()
    }
    return Modifier
        .clip(MaterialTheme.shapes.large)
        .background(container)
        .then(
            if (role == Role.Switch) {
                Modifier.toggleable(value = selected, role = role, onValueChange = { press() })
            } else {
                Modifier.selectable(selected = selected, role = role, onClick = press)
            },
        )
}

/** How a card moves when chosen; read while drawing, so only the drawing is redone each frame. */
private class CardMotion(val corner: () -> Float, val turn: () -> Float, val shape: () -> Float)

@Composable
private fun cardMotion(
    selected: Boolean,
    restTurn: Float,
    chosenTurn: Float,
    restShape: Float = 42f,
    chosenShape: Float = 48f,
): CardMotion {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val corner by animateFloatAsState(if (selected) 38f else 12f, spec, label = "corner")
    val turn by animateFloatAsState(if (selected) chosenTurn else restTurn, spec, label = "turn")
    val shape by animateFloatAsState(if (selected) chosenShape else restShape, spec, label = "shape")
    return CardMotion({ corner }, { turn }, { shape })
}

private fun DrawScope.drawShape(polygon: RoundedPolygon, centre: Offset, sizePx: Float, degrees: Float, color: Color) {
    val path = polygon.toComposePath(Size(sizePx, sizePx))
    translate(centre.x - sizePx / 2, centre.y - sizePx / 2) {
        rotate(degrees, pivot = Offset(sizePx / 2, sizePx / 2)) { drawPath(path, color) }
    }
}

private val PANEL_WIDTH = 264.dp
private val OLED_WIDTH = 104.dp
private val WIDE_HEIGHT = 56.dp
private val PRIMARY_SHAPE = MaterialShapes.Cookie9Sided
private val TERTIARY_SHAPE = MaterialShapes.Clover4Leaf
