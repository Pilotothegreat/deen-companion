package com.pilotothegreat.deencompanion

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.settings.LocationSource
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the raw Play Store screenshots in a clean state: location permission granted and a city
 * chosen, so no setup cards or "(default)" labels show. Frames are 9:16 (1200 x 2133) and land in
 * app/build/store-screenshots; scripts/store/frame_screenshots.py adds captions.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreenshotTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @After
    fun tearDown() = stopKoin()

    @Test
    @Config(sdk = [30], qualifiers = "en-w400dp-h711dp-xxhdpi")
    fun english() = capture(
        "en",
        Labels("Muscat, Oman", "Athkar", "Quran", "Hadith", "Qibla", "Morning", "Al-Fatihah", "Sahih al-Bukhari"),
    )

    @Test
    @Config(sdk = [30], qualifiers = "ar-w400dp-h711dp-night-xxhdpi")
    fun arabic() = capture(
        "ar",
        Labels("مسقط، عُمان", "الأذكار", "القرآن", "الحديث", "القبلة", "أذكار الصباح", "سورة الفاتحة", "صحيح البخاري"),
    )

    private data class Labels(
        val city: String,
        val athkar: String,
        val quran: String,
        val hadith: String,
        val qibla: String,
        val morning: String,
        val firstSurah: String,
        val firstHadithBook: String,
    )

    private fun capture(prefix: String, labels: Labels) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        runBlocking {
            GlobalContext.get().get<SettingsRepository>()
                .setLocation(23.5880, 58.3829, labels.city, "Asia/Muscat", "OM", LocationSource.MANUAL)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.mainClock.autoAdvance = false
            waitForText(labels.city, substring = true)
            shoot("$prefix-1-today")
            scrollDown()
            shoot("$prefix-2-today-daily")

            tab(labels.athkar)
            waitForText(labels.morning)
            shoot("$prefix-3-athkar")
            compose.onAllNodesWithText(labels.morning).onFirst().performClick()
            waitForDescription(app.getString(R.string.athkar_count_action))
            shoot("$prefix-4-athkar-session")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForText(labels.quran)

            tab(labels.quran)
            waitForText(labels.firstSurah)
            compose.onAllNodesWithText(labels.firstSurah).onFirst().performClick()
            settle(3_000)
            shoot("$prefix-5-reader")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForText(labels.hadith)

            tab(labels.hadith)
            waitForText(labels.firstHadithBook)
            shoot("$prefix-6-hadith")
            // No Qibla shot: Robolectric has no compass sensor, so that screen only shows its "no compass" notice.
        }
    }

    private fun tab(label: String) {
        val node = compose.onAllNodesWithText(label).onFirst()
        // The floating bar hides after scrolling down; a swipe down brings it back.
        if (runCatching { node.assertIsDisplayed() }.isFailure) {
            compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeDown() }
            settle()
        }
        node.performClick()
        settle()
    }

    private fun scrollDown() {
        compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        settle()
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
        settle()
    }

    private fun waitForDescription(description: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
        settle()
    }

    /** Lets loading finish and animations run without waiting on infinite ones. */
    private fun settle(millis: Long = 1_500) {
        repeat((millis / 100).toInt()) {
            Thread.sleep(20)
            compose.mainClock.advanceTimeBy(100)
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val dir = File("build/store-screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
