package com.pilotothegreat.deencompanion.ui.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every theme card writes its setting. 1.9.0 shipped the wallpaper and OLED settings with no control that
 * could reach them, so each card is pressed here and what it wrote is checked.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class ThemePickerTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private data class Write(val mode: ThemeMode, val wallpaper: Boolean)

    private fun show(state: ThemeState, writes: MutableList<Any> = mutableListOf(), dark: Boolean = false) {
        compose.setContent {
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                Surface {
                    ThemePicker(
                        state = state,
                        onTheme = { mode, wallpaper -> writes += Write(mode, wallpaper) },
                        onPureBlack = { writes += it },
                        wallpaperAvailable = true,
                    )
                }
            }
        }
    }

    @Test fun everyThemeCardWritesItsModeAndPalette() {
        val writes = mutableListOf<Any>()
        show(ThemeState(ThemeMode.SYSTEM, wallpaper = true, pureBlack = false), writes)
        val cards = listOf(R.string.theme_light to ThemeMode.LIGHT, R.string.theme_dark to ThemeMode.DARK, R.string.theme_system to ThemeMode.SYSTEM)
        cards.forEach { (label, _) ->
            // The wallpaper's panel comes first, Bilal's second.
            compose.onAllNodesWithText(context.getString(label))[0].performScrollTo().performClick()
            compose.onAllNodesWithText(context.getString(label))[1].performScrollTo().performClick()
        }
        assertEquals(cards.flatMap { (_, mode) -> listOf(Write(mode, true), Write(mode, false)) }, writes)
    }

    @Test fun oledIsOfferedBesideDarkAndWritesPureBlack() {
        val writes = mutableListOf<Any>()
        show(ThemeState(ThemeMode.DARK, wallpaper = false, pureBlack = false), writes)
        compose.onNodeWithText(context.getString(R.string.theme_pure_black)).performScrollTo().performClick()
        assertEquals(listOf<Any>(true), writes)
    }

    @Test fun oledIsNotOfferedBesideLight() {
        show(ThemeState(ThemeMode.LIGHT, wallpaper = false, pureBlack = true))
        compose.onAllNodesWithText(context.getString(R.string.theme_pure_black)).assertCountEquals(0)
    }

    @Test fun drawnByDay() {
        show(ThemeState(ThemeMode.LIGHT, wallpaper = false, pureBlack = false))
        shoot("theme-picker-light")
    }

    @Test
    @Config(qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun drawnInArabicAtNight() {
        show(ThemeState(ThemeMode.SYSTEM, wallpaper = false, pureBlack = true), dark = true)
        shoot("theme-picker-ar-auto-oled")
    }

    private fun shoot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots/settings").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
