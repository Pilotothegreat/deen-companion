package com.pilotothegreat.deencompanion.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.pilotothegreat.deencompanion.data.settings.AccessibilitySettings

@Composable
fun DeenTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlack: Boolean,
    accessibility: AccessibilitySettings = AccessibilitySettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DeenDarkColors
        else -> DeenLightColors
    }
    val resolved = Accessibility.from(accessibility, rememberSystemReducedMotion())
    val withContrast = if (resolved.highContrast) base.withHigherContrast(darkTheme) else base
    val colors = if (darkTheme && pureBlack) withContrast.withPureBlackSurfaces() else withContrast
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    // The reader's own scale multiplies the system's, for people who want more than its slider gives.
    val typography = remember(arabic, resolved.textScale) { deenTypography(arabic).scaledBy(resolved.textScale) }

    CompositionLocalProvider(LocalAccessibility provides resolved) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            // Expressive motion is decorative; without it the app still works, it just stops dancing.
            motionScheme = if (resolved.reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            typography = typography,
            content = content,
        )
    }
}

/** Pushes text and outlines away from their backgrounds for readers who need the separation. */
private fun ColorScheme.withHigherContrast(darkTheme: Boolean): ColorScheme {
    val toward = if (darkTheme) Color.White else Color.Black
    return copy(
        onSurface = lerp(onSurface, toward, 0.35f),
        onSurfaceVariant = lerp(onSurfaceVariant, toward, 0.45f),
        onBackground = lerp(onBackground, toward, 0.35f),
        outline = lerp(outline, toward, 0.35f),
        outlineVariant = lerp(outlineVariant, toward, 0.35f),
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
