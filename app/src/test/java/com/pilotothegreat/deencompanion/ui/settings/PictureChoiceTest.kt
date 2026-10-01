package com.pilotothegreat.deencompanion.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The picture cards that replaced segmented buttons: each one writes its value, and they are looked at. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class PictureChoiceTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(dark: Boolean) {
        compose.setContent {
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        var language by remember { mutableStateOf("ar") }
                        Text("Language", style = MaterialTheme.typography.bodyLarge)
                        PictureChoice(listOf(AppLanguage.SYSTEM, "en", "ar"), language, { language = it }, {
                            when (it) { AppLanguage.SYSTEM -> "System"; "en" -> "English"; else -> "العربية" }
                        }) { tag, chosen -> LanguagePicture(tag, chosen) }

                        var scale by remember { mutableStateOf(1.15f) }
                        Text("Text size", style = MaterialTheme.typography.bodyLarge)
                        PictureChoice(listOf(1f, 1.15f, 1.3f, 1.5f), scale, { scale = it }, { "×$it" }) { s, chosen -> TextSizePicture(s, chosen) }

                        var asr by remember { mutableStateOf(AsrSchool.STANDARD) }
                        Text("Asr time", style = MaterialTheme.typography.bodyLarge)
                        PictureChoice(AsrSchool.entries, asr, { asr = it }, { if (it == AsrSchool.HANAFI) "Hanafi" else "Standard" }) { school, chosen ->
                            AsrPicture(school, chosen)
                        }

                        var reminder by remember { mutableStateOf(10) }
                        Text("Reminder before prayer", style = MaterialTheme.typography.bodyLarge)
                        PictureChoice(listOf(0, 5, 10, 15, 20, 30), reminder, { reminder = it }, { if (it == 0) "Off" else "$it min" }) { m, chosen ->
                            MinutesPicture(m, if (m == 0) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive, chosen)
                        }

                        var silence by remember { mutableStateOf(0) }
                        Text("Silence during prayer", style = MaterialTheme.typography.bodyLarge)
                        PictureChoice(listOf(0, 10, 15, 20, 30), silence, { silence = it }, { if (it == 0) "Off" else "$it min" }) { m, chosen ->
                            MinutesPicture(m, if (m == 0) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff, chosen)
                        }
                    }
                }
            }
        }
    }

    @Test fun choosingACardWritesItsValue() {
        val writes = mutableListOf<AsrSchool>()
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                PictureChoice(AsrSchool.entries, AsrSchool.STANDARD, { writes += it }, { it.name }) { school, chosen -> AsrPicture(school, chosen) }
            }
        }
        compose.onNodeWithText(AsrSchool.HANAFI.name).performClick()
        compose.onNodeWithText(AsrSchool.STANDARD.name).performClick()
        assertEquals(listOf(AsrSchool.HANAFI, AsrSchool.STANDARD), writes)
    }

    @Test fun drawnByDay() {
        show(dark = false)
        shoot("pictures-light")
    }

    @Test
    @Config(qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun drawnInArabicAtNight() {
        show(dark = true)
        shoot("pictures-ar-dark")
    }

    private fun shoot(name: String) {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots/settings").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
