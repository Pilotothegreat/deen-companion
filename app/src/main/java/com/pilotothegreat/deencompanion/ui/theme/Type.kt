// FIXED: Add Amiri Google Font, local amiri fallback fonts, and globally exposed arabicFontFamily
package com.pilotothegreat.deencompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val nunitoFont = GoogleFont("Nunito")
val amiriFont = GoogleFont("Amiri")

val arabicFontFamily = FontFamily(
    Font(googleFont = amiriFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = amiriFont, fontProvider = provider, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.Font(R.font.amiri_regular, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.amiri_bold, weight = FontWeight.Bold),
)

val uthmaniFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.scheherazade_new, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.scheherazade_new, weight = FontWeight.Bold),
)

val nunitoFontFamily = FontFamily(
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Bold),
    // Local fallbacks for offline:
    androidx.compose.ui.text.font.Font(R.font.nunito_regular, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.nunito_medium, weight = FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.nunito_semibold, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.nunito_bold, weight = FontWeight.Bold),
    // Arabic fallbacks:
    androidx.compose.ui.text.font.Font(R.font.scheherazade_new, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.scheherazade_new, weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = nunitoFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

val ArabicTypography = Typography(
    displayLarge = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 74.sp),
    displayMedium = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 45.sp, lineHeight = 58.sp),
    displaySmall = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 44.sp),
    bodyMedium = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 36.sp),
    bodySmall = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 32.sp),
    labelLarge = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = uthmaniFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 18.sp)
)

