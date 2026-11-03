package com.pilotothegreat.deencompanion.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The support sheet, looked at: the bank transfer for Oman and GitHub Sponsors for everyone else. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class SupportSheetRenderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test fun offersSponsorsBesideTheBankTransfer() {
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) { Surface(Modifier.fillMaxWidth()) { SupportContent() } }
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("GitHub Sponsors").onFirst().assertExists()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "support-sheet.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
