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
import com.pilotothegreat.deencompanion.core.text.Numerals
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
 * end-to-end smoke test: every screen must compose without crashing, and back navigation works.
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
    fun englishLight() = walkThrough(
        "en",
        Labels("Today", "Quran", "Athkar", "Hadith", "Qibla", "Settings", "Back", "Al-Fatihah", "Sahih al-Bukhari", "Morning"),
    )

    @Test
    @Config(sdk = [30], qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun arabicDark() = walkThrough(
        "ar",
        Labels("اليوم", "القرآن", "الأذكار", "الحديث", "القبلة", "الإعدادات", "رجوع", "سورة الفاتحة", "صحيح البخاري", "أذكار الصباح"),
    )

    private data class Labels(
        val today: String,
        val quran: String,
        val athkar: String,
        val hadith: String,
        val qibla: String,
        val settings: String,
        val back: String,
        val firstSurah: String,
        val firstHadithBook: String,
        val morning: String,
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
        goBack(labels)

        tab(labels.hadith)
        waitForText(labels.firstHadithBook)
        shoot("$prefix-05-hadith")

        tab(labels.athkar)
        waitForText(labels.morning)
        shoot("$prefix-06-athkar")
        compose.onAllNodesWithText(labels.morning).onFirst().performClick()
        waitForDescription(string(R.string.athkar_count_action))
        shoot("$prefix-07-athkar-session")
        compose.onAllNodesWithContentDescription(string(R.string.athkar_count_action)).onFirst().performClick()
        // The first morning dhikr is said once, so one tap finishes it and the subtitle counts it.
        val locale = compose.activity.resources.configuration.locales[0]
        waitForText(compose.activity.getString(R.string.athkar_progress, Numerals.format(1, locale), Numerals.format(26, locale)))
        shoot("$prefix-08-athkar-counted")
        goBack(labels)
        scrollDown()
        shoot("$prefix-09-athkar-scrolled")

        tab(labels.today)
        scrollUntilText(labels.qibla)
        compose.onAllNodesWithText(labels.qibla).onFirst().performClick()
        waitForText(string(R.string.qibla_bearing_label))
        shoot("$prefix-10-qibla")
        goBack(labels)

        compose.onAllNodesWithContentDescription(labels.settings).onFirst().performClick()
        settle()
        shoot("$prefix-11-settings")
        scrollDown()
        shoot("$prefix-12-settings-scrolled")
        scrollDown()
        shoot("$prefix-13-settings-bottom")

        // System back leaves Settings and brings the navigation bar back.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText(labels.hadith)
    }

    /** Taps the top bar's back arrow and waits for the navigation bar to return. */
    private fun goBack(labels: Labels) {
        compose.onAllNodesWithContentDescription(labels.back).onFirst().performClick()
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

    /**
     * Swipes until [text] is composed. performScrollToNode would wait for layout frames that never
     * come while the test clock is paused.
     */
    private fun scrollUntilText(text: String) {
        repeat(8) {
            if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            scrollDown()
        }
        error("\"$text\" not found after scrolling")
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
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

    /** Lets background loading finish and animations run without waiting on infinite ones. */
    private fun settle(millis: Long = 1_500) {
        repeat((millis / 100).toInt()) {
            Thread.sleep(20)
            compose.mainClock.advanceTimeBy(100)
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = compose.activity.getString(id)

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
