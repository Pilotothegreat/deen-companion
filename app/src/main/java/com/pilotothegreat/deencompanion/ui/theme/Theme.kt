package com.pilotothegreat.deencompanion.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import org.koin.compose.koinInject


@Composable
fun Theme(
    content: @Composable () -> Unit
) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val theme by appPreferenceRepo.theme.collectAsState(Theme.AutoMaterial)
    val appLang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")
    val amoledBlackMode by appPreferenceRepo.amoledBlackMode.collectAsState(false)
    val isDark = theme.isDark()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            var activity: android.app.Activity? = null
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) {
                    activity = context
                    break
                }
                context = context.baseContext
            }
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    val baseColors = theme.getColors()
    val colors = if (isDark && amoledBlackMode) {
        baseColors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            scrim = Color.Black,
            surfaceVariant = Color.Black,
            outline = Color(0xFF303030),
            outlineVariant = Color(0xFF303030)
        )
    } else {
        baseColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = if (appLang.startsWith("ar")) ArabicTypography else AppTypography
    ) { content() }
}

enum class Theme {
    AutoMaterial,
    LightMaterial,
    DarkMaterial,
    Auto,
    Light,
    Dark;

    @Composable
    fun getColors(): ColorScheme {
        val context = LocalContext.current
        val darkTheme = isSystemInDarkTheme()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return when (this) {
                AutoMaterial -> if (darkTheme) darkColorScheme() else lightColorScheme()
                LightMaterial -> lightColorScheme()
                DarkMaterial -> darkColorScheme()
                Auto -> if (darkTheme) darkScheme else lightScheme
                Light -> lightScheme
                Dark -> darkScheme
            }
        }

        return when (this) {
            AutoMaterial -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            LightMaterial -> dynamicLightColorScheme(context)
            DarkMaterial -> dynamicDarkColorScheme(context)
            Auto -> if (darkTheme) darkScheme else lightScheme
            Light -> lightScheme
            Dark -> darkScheme
        }
    }

    @Composable
    fun getName(): String {
        return when (this) {
            AutoMaterial, Auto -> stringResource(R.string.auto)
            LightMaterial, Light -> stringResource(R.string.light)
            DarkMaterial, Dark -> stringResource(R.string.dark)
        }
    }

    @Composable
    fun isDark(): Boolean {
        val darkTheme = isSystemInDarkTheme()
        return (
            darkTheme && this == AutoMaterial ||
            darkTheme && this == Auto ||
            this == DarkMaterial ||
            this == Dark
        )
    }
}


@Composable
fun Modifier.card(): Modifier {
    return this
        .shadow(
            elevation = 6.dp,
            shape = MaterialTheme.shapes.large,
            clip = false,
            ambientColor = Color(0x44C5A028),
            spotColor = Color(0x44C5A028)
        )
        .clip(MaterialTheme.shapes.large)
        .background(colorScheme.surfaceContainer)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.navBarShadow(): Modifier {
    return this.shadow(2.dp, MaterialTheme.shapes.extraLargeIncreased)
}

val backgrounds = listOf(null, R.drawable.background_1, R.drawable.background_2, R.drawable.background_3, R.drawable.background_4)

internal val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)
internal val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

