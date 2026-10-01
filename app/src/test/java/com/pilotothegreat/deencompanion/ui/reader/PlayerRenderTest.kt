package com.pilotothegreat.deencompanion.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.playback.PlaybackState
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The recitation bar and the reciter sheet drawn to pictures, in both languages, saved to
 * app/build/screenshots to be looked at.
 *
 * The skip buttons are the reason this exists: which way they point depends on the direction the
 * screen is laid out in, and no assertion reads a picture of an arrow.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w400dp-h880dp-xxhdpi")
class PlayerRenderTest {

    @get:Rule
    val compose = createComposeRule()

    // The test application starts Koin as the app does, and it cannot be started twice in one sandbox.
    @After
    fun tearDown() = stopKoin()

    private val state = PlaybackState(
        surah = 2,
        ayah = 5,
        verseCount = 286,
        reciter = Reciter.MISHARY,
        isPlaying = true,
        repeatMode = RepeatMode.AYAH,
    )

    @Test fun theBarInEnglish() = bar("player-bar-en", "Al-Baqarah")

    @Test
    @Config(qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun theBarInArabic() = bar("player-bar-ar", "سورة البقرة", dark = true)

    /** Twenty reciters, and below them a sleep timer that has to stay reachable. */
    @Test fun theSheetReachesTheSleepTimer() {
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxSize()) {
                    ReciterSheet(
                        state = state,
                        onReciter = {},
                        onSleepTimer = {},
                        onClearDownloads = {},
                        onDismiss = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        shoot("reciter-sheet-en")
        assertTrue(
            "the last reciter never composes, so the sheet cannot be scrolled to the sleep timer",
            compose.onAllNodes(hasText("Mohamed Al-Tablawi")).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun bar(name: String, title: String, dark: Boolean = false) {
        compose.setContent {
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        PlayerBar(
                            state = state,
                            title = title,
                            awayFromRecitation = true,
                            onTogglePlay = {},
                            onPrevious = {},
                            onNext = {},
                            onStop = {},
                            onRepeat = {},
                            onReciter = {},
                            onBackToRecitation = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        shoot(name)
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
