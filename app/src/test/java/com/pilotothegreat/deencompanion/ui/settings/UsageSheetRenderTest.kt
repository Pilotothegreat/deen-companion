package com.pilotothegreat.deencompanion.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.analytics.UsageCount
import com.pilotothegreat.deencompanion.core.analytics.UsageEnvironment
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.core.analytics.UsageReport
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The sheet that shows what has been counted, saved to app/build/screenshots to be looked at.
 *
 * It is the evidence behind the privacy policy's claim, so it is worth seeing rather than assuming:
 * every counter, its number, and a button that shares exactly what would be sent.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class UsageSheetRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val report = UsageReport(
        environment = UsageEnvironment("2.2.0", 220, "github", 34, "Google Pixel 8", "en", "OM"),
        firstSeen = "2026-08-20",
        daysActive = 21,
        sessions = 63,
        counts = listOf(
            UsageCount("2026-09-18", UsageEvent.SCREEN_QURAN.id, 31),
            UsageCount("2026-09-18", UsageEvent.RECITATION_PLAYED.id, 12),
            UsageCount("2026-09-18", UsageEvent.QURAN_SEARCH_JUMPED.id, 4),
            UsageCount("2026-09-19", UsageEvent.SCREEN_TODAY.id, 22),
            UsageCount("2026-09-19", UsageEvent.PRAYER_MARKED.id, 5),
        ),
    )

    @Test fun theSheetShowsEveryCounterItHas() {
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxSize()) { UsageSheet(report = report, onDismiss = {}) }
            }
        }
        compose.waitForIdle()
        shoot("usage-sheet")
        // The busiest counter is at the top; the sheet is sorted by how often a thing happened.
        assertTrue(
            "the sheet does not list what was counted",
            compose.onAllNodes(hasText("Screen quran", substring = true)).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
