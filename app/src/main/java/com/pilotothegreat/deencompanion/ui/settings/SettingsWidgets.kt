package com.pilotothegreat.deencompanion.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import com.pilotothegreat.deencompanion.ui.theme.Theme
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.ui.theme.arabicFontFamily
import com.pilotothegreat.deencompanion.ui.theme.nunitoFontFamily
import com.pilotothegreat.deencompanion.util.CategoryTitleSmallText
import com.pilotothegreat.deencompanion.util.px
import kotlinx.coroutines.launch

@Composable
fun Preference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit = {},
    controls: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .card()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = if (icon != null) 8.dp else 16.dp,
                end = 16.dp,
            )
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                )
            }
        } else {
            Box(modifier = Modifier.size(0.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                Text(text = title)
            }
            if (summary != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalContentColor provides colorScheme.onSurface,
                ) {
                    Text(text = summary)
                }
            }
        }
        if (controls != null) {
            Box(
                modifier = Modifier.padding(start = 24.dp)
            ) {
                controls()
            }
        }
    }
}

@Composable
fun NavigatePreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    Preference(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        onClick = onClick,
        enabled = enabled,
        controls = {
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
            )
        }
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    icon: Painter? = null,
    summary: String? = null,
    value: Boolean,
    enabled: Boolean = true,
    onValueChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    fun onClick(state: Boolean) {
        val feedback = if (state) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        haptic.performHapticFeedback(feedback)
        onValueChanged(state)
    }
    Preference(
        modifier = modifier,
        title = title,
        icon = icon,
        summary = summary,
        enabled = enabled,
        onClick = {
            onClick(!value)
        },
        controls = {
            Switch(
                enabled = enabled, checked = value, onCheckedChange = {
                    onClick(it)
                },
            )
        },
    )
}

@Composable
fun IconPreference(
    title: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .card()
            .clickable {onClick.invoke()}
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center),
            painter = painter,
            contentDescription = title
        )
    }
}

@Composable
fun ThemePreferenceContainer(currentTheme: Theme, material: Boolean, onThemeChanged: (Theme) -> Unit) {
    val themeLight = if (material) Theme.LightMaterial else Theme.Light
    val themeDark = if (material) Theme.DarkMaterial else Theme.Dark
    val themeAuto = if (material) Theme.AutoMaterial else Theme.Auto
    Column {
        CategoryTitleSmallText(if (material) stringResource(R.string.material_theme) else stringResource(R.string.default_theme))
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .width(IntrinsicSize.Max)
                .background(colorScheme.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference(themeLight, themeLight == currentTheme) { onThemeChanged(themeLight) }
                ThemePreference(themeDark, themeDark == currentTheme) { onThemeChanged(themeDark) }
                if (!material) {
                    ThemePreference(Theme.Amoled, Theme.Amoled == currentTheme) { onThemeChanged(Theme.Amoled) }
                }
            }
            ThemeAutoPreference(themeAuto, themeAuto == currentTheme) { onThemeChanged(themeAuto) }
        }
    }
}

private fun getStarPath(): Path {
    return PathParser.createPathFromPathData(
        "M480,600q50,0 85,-35t35,-85q0,-50 -35,-85t-85,-35q-50,0 -85,35t-35,85q0,50 35,85t85,35ZM480,680q-83,0 -141.5,-58.5T280,480q0,-83 58.5,-141.5T480,280q83,0 141.5,58.5T680,480q0,83 -58.5,141.5T480,680ZM80,520q-17,0 -28.5,-11.5T40,480q0,-17 11.5,-28.5T80,440h80q17,0 28.5,11.5T200,480q0,17 -11.5,28.5T160,520L80,520ZM800,520q-17,0 -28.5,-11.5T760,480q0,-17 11.5,-28.5T800,440h80q17,0 28.5,11.5T920,480q0,17 -11.5,28.5T880,520h-80ZM480,200q-17,0 -28.5,-11.5T440,160v-80q0,-17 11.5,-28.5T480,40q17,0 28.5,11.5T520,80v80q0,17 -11.5,28.5T480,200ZM480,920q-17,0 -28.5,-11.5T440,880v-80q0,-17 11.5,-28.5T480,760q17,0 28.5,11.5T520,800v80q0,17 -11.5,28.5T480,920ZM226,282l-43,-42q-12,-11 -11.5,-28t11.5,-29q12,-12 29,-12t28,12l42,43q11,12 11,28t-11,28q-11,12 -27.5,11.5T226,282ZM720,777 L678,734q-11,-12 -11,-28.5t11,-27.5q11,-12 27.5,-11.5T734,678l43,42q12,11 11.5,28T777,777q-12,12 -29,12t-28,-12ZM678,282q-12,-11 -11.5,-27.5T678,226l42,-43q11,-12 28,-11.5t29,11.5q12,12 12,29t-12,28l-43,42q-12,11 -28,11t-28,-11ZM183,777q-12,-12 -12,-29t12,-28l43,-42q12,-11 28.5,-11t27.5,11q12,11 11.5,27.5T282,734l-42,43q-11,12 -28,11.5T183,777ZM480,480Z"
    ).asComposePath()
}

private fun getMoonPath(): Path {
    return PathParser.createPathFromPathData(
        "M480,840q-151,0 -255.5,-104.5T120,480q0,-138 90,-239.5T440,122q13,-2 23,3.5t16,14.5q6,9 6.5,21t-7.5,23q-17,26 -25.5,55t-8.5,61q0,90 63,153t153,63q31,0 61.5,-9t54.5,-25q11,-7 22.5,-6.5T819,481q10,5 15.5,15t3.5,24q-14,138 -117.5,229T480,840ZM480,760q88,0 158,-48.5T740,585q-20,5 -40,8t-40,3q-123,0 -209.5,-86.5T364,300q0,-20 3,-40t8,-40q-78,32 -126.5,102T200,480q0,116 82,198t198,82ZM470,490Z"
    ).asComposePath()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePreference(theme: Theme, enabled: Boolean, onClick: () -> Unit) {
    val scheme = theme.getColors()
    val radiusSmall = 12.dp.px
    val radiusBig = 38.dp.px
    val cornerRadius = remember { Animatable(radiusSmall) }
    val rotation = remember { Animatable(0f) }

    val shape1 = remember { getStarPath() }
    val shape2 = remember { getMoonPath() }
    val iconScaleSmall = 42.dp.px
    val iconScaleBig = 48.dp.px
    val iconScale = remember { Animatable(iconScaleSmall) }

    val shape1Transformed = remember(iconScale.value, rotation.value) {
        val path = Path().apply {
            addPath(shape1)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-480f, -480f)
            postScale(iconScale.value / 960f, iconScale.value / 960f)
            postTranslate(-rotation.value, rotation.value)
        }
        path.asAndroidPath().transform(matrix)
        path
    }

    val shape2Transformed = remember(iconScale.value, rotation.value) {
        val path = Path().apply {
            addPath(shape2)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-480f, -480f)
            postScale(iconScale.value / 960f, iconScale.value / 960f)
            postTranslate(rotation.value, -rotation.value)
        }
        path.asAndroidPath().transform(matrix)
        path
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(enabled) {
        if (enabled) {
            launch { cornerRadius.animateTo(radiusBig) }
            launch { rotation.animateTo(-10f) }
            launch { iconScale.animateTo(iconScaleBig) }
        } else {
            launch { cornerRadius.animateTo(radiusSmall) }
            launch { rotation.animateTo(0f) }
            launch { iconScale.animateTo(iconScaleSmall) }
        }
    }
    Column (
        Modifier
            .card()
            .clickable(onClick = {
                scope.launch { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) }
                onClick()
            })
            .background(if (enabled) colorScheme.surfaceVariant else colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(120.dp, 70.dp)
                .drawBehind {
                    rotate(-rotation.value) {
                        drawRoundRect(
                            color = scheme.background,
                            cornerRadius = CornerRadius(cornerRadius.value)
                        )
                    }
                    val x = size.width / 7f
                    val y = size.height / 2f
                    translate(x * 2, y) {
                        rotate(rotation.value * 2f) {
                            drawPath(shape1Transformed, scheme.primary)
                        }
                    }
                    translate(x * 5, y) {
                        rotate(-rotation.value * 2f) {
                            drawPath(shape2Transformed, scheme.tertiary)
                        }
                    }
                }
                .padding(12.dp)
                .fillMaxWidth(),
        )
        Row (
            Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (theme.isDark()) R.drawable.dark else R.drawable.light),
                contentDescription = null
            )
            Text(
                text = theme.getName(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeAutoPreference(theme: Theme, enabled: Boolean, onClick: () -> Unit) {
    val scheme = theme.getColors()
    val radiusSmall = 12.dp.px
    val radiusBig = 38.dp.px
    val cornerRadius = remember { Animatable(radiusSmall) }
    val rotation = remember { Animatable(-15f) }

    val shape1 = remember { getStarPath() }
    val shape2 = remember { getMoonPath() }
    val iconScaleSmall = 42.dp.px
    val iconScaleBig = 48.dp.px
    val iconScale = remember { Animatable(iconScaleSmall) }

    val shape1Transformed = remember(iconScale.value, rotation.value) {
        val path = Path().apply {
            addPath(shape1)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-480f, -480f)
            postScale(iconScale.value / 960f, iconScale.value / 960f)
        }
        path.asAndroidPath().transform(matrix)
        path
    }

    val shape2Transformed = remember(iconScale.value, rotation.value) {
        val path = Path().apply {
            addPath(shape2)
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-480f, -480f)
            postScale(iconScale.value / 960f, iconScale.value / 960f)
        }
        path.asAndroidPath().transform(matrix)
        path
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(enabled) {
        if (enabled) {
            launch { cornerRadius.animateTo(radiusBig) }
            launch { rotation.animateTo(-5f) }
            launch { iconScale.animateTo(iconScaleBig) }
        } else {
            launch { cornerRadius.animateTo(radiusSmall) }
            launch { rotation.animateTo(-15f) }
            launch { iconScale.animateTo(iconScaleSmall) }
        }
    }
    Box(
        modifier = Modifier
            .card()
            .clickable(onClick = {
                scope.launch { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) }
                onClick()
            })
            .background(if (enabled) colorScheme.surfaceVariant else colorScheme.surfaceContainer)
            .padding(4.dp)
            .drawBehind {
                val x = size.width / 7f
                val y = size.height / 2f
                translate(x * 1, y) {
                    rotate(rotation.value * 2f) {
                        drawPath(shape1Transformed, scheme.primary)
                    }
                }
                translate(x * 6, y) {
                    rotate(-rotation.value * 2f) {
                        drawPath(shape2Transformed, scheme.tertiary)
                    }
                }
            }
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.magic),
                contentDescription = null,
            )
            Text(
                text = theme.getName(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun PermissionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: Painter,
    onHelp: (() -> Unit)? = null,
    actionButton: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row (modifier = modifier
        .card()
        .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, "Icon")
                Text(modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, text = title)
            }
            Text(modifier = Modifier.fillMaxWidth(), text = description)
        }
        Column (verticalArrangement = Arrangement.spacedBy(8.dp)){
            if (onHelp != null) {
                FilledIconButton(
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    enabled = enabled,
                    shape = MaterialTheme.shapes.large,
                    onClick = onHelp,
                ) {
                    Icon(
                        painterResource(R.drawable.help),
                        contentDescription = stringResource(R.string.help),
                    )
                }
            }
            actionButton?.invoke()
        }
    }
}

@Composable
fun PermissionButton(
    icon: Painter,
    contentDescription: String,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    onClick: () -> Unit
) {
    FilledIconButton (
        modifier = Modifier.size(56.dp),
        colors = colors,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun LanguagePreferenceContainer(currentLang: String, onLangChanged: (String) -> Unit) {
    Column {
        CategoryTitleSmallText(stringResource(R.string.app_language_title))
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxWidth()
                .background(colorScheme.surface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguagePreference(
                langCode = "en",
                langName = "English",
                nativeName = "English",
                selected = currentLang == "en",
                onClick = { onLangChanged("en") },
                modifier = Modifier.weight(1f)
            )
            LanguagePreference(
                langCode = "ar",
                langName = "Arabic",
                nativeName = "العربية",
                selected = currentLang == "ar",
                onClick = { onLangChanged("ar") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LanguagePreference(
    langCode: String,
    langName: String,
    nativeName: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val radiusSmall = 12.dp.px
    val radiusBig = 38.dp.px
    val cornerRadius = remember { Animatable(radiusSmall) }
    val textScale = remember { Animatable(1.0f) }

    LaunchedEffect(selected) {
        if (selected) {
            launch { cornerRadius.animateTo(radiusBig) }
            launch { textScale.animateTo(1.15f) }
        } else {
            launch { cornerRadius.animateTo(radiusSmall) }
            launch { textScale.animateTo(1.0f) }
        }
    }

    val primaryColor = colorScheme.primary
    val surfaceColor = colorScheme.surface

    Column(
        modifier = modifier
            .card()
            .clickable(onClick = {
                scope.launch { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) }
                onClick()
            })
            .background(if (selected) colorScheme.surfaceVariant else colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height(70.dp)
                .drawBehind {
                    drawRoundRect(
                        color = if (selected) primaryColor.copy(alpha = 0.1f) else surfaceColor,
                        cornerRadius = CornerRadius(cornerRadius.value)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (langCode == "en") "A" else "ع",
                fontFamily = if (langCode == "en") nunitoFontFamily else arabicFontFamily,
                fontSize = if (langCode == "en") 36.sp else 42.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.graphicsLayer {
                    scaleX = textScale.value
                    scaleY = textScale.value
                }
            )
        }
        
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.checkmark),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = nativeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
