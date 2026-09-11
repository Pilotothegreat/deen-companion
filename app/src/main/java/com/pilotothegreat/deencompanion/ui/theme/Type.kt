@file:OptIn(ExperimentalTextApi::class)

package com.pilotothegreat.deencompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import com.pilotothegreat.deencompanion.R

private fun googleSans(weight: Int) = Font(
    R.font.google_sans_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** UI typeface. Arabic glyphs fall back to the system's Arabic UI font. */
val GoogleSansFlex = FontFamily(googleSans(400), googleSans(500), googleSans(600), googleSans(700), googleSans(800))

/** King Fahd Complex Uthmanic Hafs script, used only for Quran text. */
val UthmanicHafs = FontFamily(Font(R.font.uthmanic_hafs))

/** Classical naskh for hadith and other Arabic prose. */
val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

/**
 * The M3 Expressive type scale set in Google Sans Flex. Arabic script needs more vertical room
 * than Latin, so line heights grow when the UI is in Arabic.
 */
fun deenTypography(arabic: Boolean): Typography {
    val base = Typography()
    fun TextStyle.adapt(): TextStyle = copy(
        fontFamily = GoogleSansFlex,
        lineHeight = if (arabic && lineHeight.isSpecified) lineHeight * 1.3f else lineHeight,
    )
    return Typography(
        displayLarge = base.displayLarge.adapt(),
        displayMedium = base.displayMedium.adapt(),
        displaySmall = base.displaySmall.adapt(),
        headlineLarge = base.headlineLarge.adapt(),
        headlineMedium = base.headlineMedium.adapt(),
        headlineSmall = base.headlineSmall.adapt(),
        titleLarge = base.titleLarge.adapt(),
        titleMedium = base.titleMedium.adapt(),
        titleSmall = base.titleSmall.adapt(),
        bodyLarge = base.bodyLarge.adapt(),
        bodyMedium = base.bodyMedium.adapt(),
        bodySmall = base.bodySmall.adapt(),
        labelLarge = base.labelLarge.adapt(),
        labelMedium = base.labelMedium.adapt(),
        labelSmall = base.labelSmall.adapt(),
        displayLargeEmphasized = base.displayLargeEmphasized.adapt(),
        displayMediumEmphasized = base.displayMediumEmphasized.adapt(),
        displaySmallEmphasized = base.displaySmallEmphasized.adapt(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.adapt(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.adapt(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.adapt(),
        titleLargeEmphasized = base.titleLargeEmphasized.adapt(),
        titleMediumEmphasized = base.titleMediumEmphasized.adapt(),
        titleSmallEmphasized = base.titleSmallEmphasized.adapt(),
        bodyLargeEmphasized = base.bodyLargeEmphasized.adapt(),
        bodyMediumEmphasized = base.bodyMediumEmphasized.adapt(),
        bodySmallEmphasized = base.bodySmallEmphasized.adapt(),
        labelLargeEmphasized = base.labelLargeEmphasized.adapt(),
        labelMediumEmphasized = base.labelMediumEmphasized.adapt(),
        labelSmallEmphasized = base.labelSmallEmphasized.adapt(),
    )
}
