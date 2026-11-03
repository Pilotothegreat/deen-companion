package com.pilotothegreat.deencompanion.ui.onboarding

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The first-run sheet, saved to app/build/screenshots to be looked at, in both languages and themes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class SetupSheetRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(dark: Boolean = false, location: Boolean = false, onDone: (Boolean) -> Unit = {}) {
        compose.setContent {
            var share by remember { mutableStateOf(false) }
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxWidth()) {
                    SetupContent(
                        notifications = true,
                        location = location,
                        shareUsage = share,
                        onAllowNotifications = {},
                        onAllowLocation = {},
                        onChooseCity = {},
                        onShareUsage = { share = it },
                        onDone = { onDone(share) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun dataCollectionIsAskedAndStartsOff() {
        var answer: Boolean? = null
        show(onDone = { answer = it })
        shoot("setup-sheet")
        compose.onNodeWithText("Share usage data").assertIsOff()
        compose.onNodeWithText("Share usage data").performClick()
        compose.onNodeWithText("Share usage data").assertIsOn()
        compose.onNodeWithText("Continue").performClick()
        assertEquals(true, answer)
    }

    @Test fun dark() {
        show(dark = true, location = true)
        shoot("setup-sheet-dark")
    }

    @Config(qualifiers = "ar-w400dp-h880dp-xxhdpi")
    @Test fun arabic() {
        show()
        shoot("setup-sheet-ar")
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
