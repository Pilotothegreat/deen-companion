package com.pilotothegreat.deencompanion.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
fun DeenTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlack: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DeenDarkColors
        else -> DeenLightColors
    }
    val colors = if (darkTheme && pureBlack) base.withPureBlackSurfaces() else base
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val typography = remember(arabic) { deenTypography(arabic) }

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content,
    )
}

/** Black background with container steps kept slightly lifted so elevation stays readable. */
private fun ColorScheme.withPureBlackSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(Color.Black, surfaceContainerLow, 0.5f),
    surfaceContainer = lerp(Color.Black, surfaceContainer, 0.55f),
    surfaceContainerHigh = lerp(Color.Black, surfaceContainerHigh, 0.6f),
    surfaceContainerHighest = lerp(Color.Black, surfaceContainerHighest, 0.65f),
    surfaceBright = lerp(Color.Black, surfaceBright, 0.7f),
)
