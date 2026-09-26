package com.pilotothegreat.deencompanion

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
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
        Labels("Today", "Quran", "Athkar", "Hadith", "Qibla", "Settings", "Back", "Al-Fatihah", "Sahih al-Bukhari", "Morning", "Skip"),
    )

    @Test
    @Config(sdk = [30], qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun arabicDark() = walkThrough(
        "ar",
        Labels("اليوم", "القرآن", "الأذكار", "الحديث", "القبلة", "الإعدادات", "رجوع", "سورة الفاتحة", "صحيح البخاري", "أذكار الصباح", "تخطّي"),
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
        val onboardingSkip: String,
    )

    private fun walkThrough(prefix: String, labels: Labels) {
        compose.mainClock.autoAdvance = false
        // A fresh install starts at first run, which is also the only place this gets exercised.
        waitForText(labels.onboardingSkip)
        shoot("$prefix-00-onboarding")
        compose.onAllNodesWithText(labels.onboardingSkip).onFirst().performClick()
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
        writeOwnList(prefix, labels)

        tab(labels.today)
        scrollUntilText(labels.qibla)
        compose.onAllNodesWithText(labels.qibla).onFirst().performClick()
        waitForText(string(R.string.qibla_bearing_label))
        shoot("$prefix-10-qibla")
        goBack(labels)

        compose.onAllNodesWithContentDescription(labels.settings).onFirst().performClick()
        settle()
        shoot("$prefix-11-settings")
        repeat(3) { nudgeDown() }
        shoot("$prefix-11a-settings-notifications")
        repeat(3) { nudgeDown() }
        shoot("$prefix-11b-settings-appearance")
        scrollDown()
        shoot("$prefix-12-settings-scrolled")
        scrollDown()
        shoot("$prefix-13-settings-bottom")

        // System back leaves Settings and brings the navigation bar back.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText(labels.hadith)
    }

    /** Writes a list of one's own, counts it, and opens it again to edit. */
    private fun writeOwnList(prefix: String, labels: Labels) {
        val title = if (prefix == "ar") "بعد الجمعة" else "After Jumu'ah"
        scrollUntilText(string(R.string.athkar_new_list), up = true)
        shoot("$prefix-09a-athkar-mine-empty")
        compose.onAllNodesWithText(string(R.string.athkar_new_list)).onFirst().performClick()
        waitForText(string(R.string.athkar_list_title))
        shoot("$prefix-09b-athkar-editor-new")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput(title)
        fields[1].performTextInput("أَسْتَغْفِرُ اللهَ وَأَتُوبُ إِلَيْهِ")
        fields[2].performTextInput(if (prefix == "ar") "بعد صلاة الجمعة" else "After the Friday prayer")
        fields[3].performTextReplacement("100")
        compose.onAllNodesWithText(string(R.string.athkar_add_dhikr)).onFirst().performClick()
        settle()
        compose.onAllNodes(hasSetTextAction())[4].performTextInput("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ")
        settle()
        shoot("$prefix-09c-athkar-editor-filled")
        compose.onAllNodesWithText(string(R.string.save)).onFirst().performClick()
        waitForText(labels.hadith)
        scrollUntilText(title, up = true)
        shoot("$prefix-09d-athkar-mine")
        compose.onAllNodesWithText(title).onFirst().performClick()
        waitForDescription(string(R.string.athkar_count_action))
        compose.onAllNodesWithContentDescription(string(R.string.athkar_count_action)).onFirst().performClick()
        settle()
        shoot("$prefix-09e-athkar-mine-session")
        compose.onAllNodesWithContentDescription(string(R.string.athkar_edit_list)).onFirst().performClick()
        waitForText(string(R.string.athkar_edit_list))
        shoot("$prefix-09f-athkar-editor-edit")
        // Nothing changed, so back leaves without asking, and back again returns to the tab.
        compose.onAllNodesWithContentDescription(labels.back).onFirst().performClick()
        waitForDescription(string(R.string.athkar_count_action))
        goBack(labels)
        // Each tab keeps its place: away to the Quran and back, the list is still in view.
        tab(labels.quran)
        tab(labels.athkar)
        waitForText(title)
        shoot("$prefix-09g-athkar-kept-its-place")
    }

    /** Taps the top bar's back arrow and waits for the navigation bar to return. */
    private fun goBack(labels: Labels) {
        compose.onAllNodesWithContentDescription(labels.back).onFirst().performClick()
        waitForText(labels.hadith)
    }

    private fun tab(label: String) {
        val node = compose.onAllNodesWithText(label).onFirst()
        // The floating bar hides after scrolling down; a small swipe down brings it back.
        if (runCatching { node.assertIsDisplayed() }.isFailure) {
            compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeDown() }
            settle()
        }
        node.performClick()
        settle()
    }

    private fun scrollUp() {
        compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeDown() }
        settle()
    }

    private fun nudgeDown() {
        compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput {
            swipe(center, center.copy(y = center.y - height / 4f), durationMillis = 600)
        }
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
    private fun scrollUntilText(text: String, up: Boolean = false) {
        if (up) repeat(4) { scrollUp() }
        repeat(if (up) 12 else 8) {
            if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            // From the top, short steps: a full swipe flings past a section that is one screen down.
            if (up) nudgeDown() else scrollDown()
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
