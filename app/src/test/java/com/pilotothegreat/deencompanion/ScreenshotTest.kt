package com.pilotothegreat.deencompanion

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Launches the real app and walks through the main screens, saving screenshots to
 * app/build/screenshots so the UI can be reviewed without a device. It also serves as an
 * end-to-end smoke test: every screen must compose without crashing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() = stopKoin()

    @Test
    @Config(sdk = [30], qualifiers = "en-w400dp-h880dp-xxhdpi")
    fun englishLight() = walkThrough("en", Labels("Today", "Quran", "Hadith", "Qibla", "Settings", "Back", "Al-Fatihah", "Sahih al-Bukhari"))

    @Test
    @Config(sdk = [30], qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun arabicDark() = walkThrough("ar", Labels("اليوم", "القرآن", "الحديث", "القبلة", "الإعدادات", "رجوع", "سورة الفاتحة", "صحيح البخاري"))

    private data class Labels(
        val today: String,
        val quran: String,
        val hadith: String,
        val qibla: String,
        val settings: String,
        val back: String,
        val firstSurah: String,
        val firstHadithBook: String,
    )

    private fun walkThrough(prefix: String, labels: Labels) {
        compose.mainClock.autoAdvance = false
        waitForText(labels.today)
        shoot("$prefix-01-today")
        scrollDown()
        shoot("$prefix-02-today-scrolled")

        tab(labels.quran)
        waitForText(labels.firstSurah)
        shoot("$prefix-03-quran")

        compose.onAllNodesWithText(labels.firstSurah).onFirst().performClick()
        settle(3_000)
        shoot("$prefix-04-reader")
        compose.onAllNodesWithContentDescription(labels.back).onFirst().performClick()
        waitForText(labels.hadith)

        tab(labels.hadith)
        waitForText(labels.firstHadithBook)
        shoot("$prefix-05-hadith")

        tab(labels.qibla)
        waitForText(compose.activity.getString(R.string.qibla_bearing_label))
        shoot("$prefix-06-qibla")

        tab(labels.today)
        settle()
        compose.onAllNodesWithContentDescription(labels.settings).onFirst().performClick()
        settle()
        shoot("$prefix-07-settings")
        scrollDown()
        shoot("$prefix-08-settings-scrolled")
        scrollDown()
        shoot("$prefix-09-settings-bottom")

        // System back leaves Settings and brings the navigation bar back.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText(labels.hadith)
    }

    private fun tab(label: String) {
        compose.onAllNodesWithText(label).onFirst().performClick()
        settle()
    }

    private fun scrollDown() {
        compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        settle()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        settle()
    }

    /** Lets background loading finish and animations run without waiting on infinite ones. */
    private fun settle(millis: Long = 1_500) {
        repeat((millis / 100).toInt()) {
            Thread.sleep(20)
            compose.mainClock.advanceTimeBy(100)
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
