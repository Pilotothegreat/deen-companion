package com.pilotothegreat.deencompanion.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The reader as someone uses it: pages drawn with the shipped text and font, saved to
 * app/build/screenshots to be looked at, and the gestures that matter — a swipe turns the page, a tap
 * gives the ayah, a long press opens its menu. 2.0 shipped a pinch detector that swallowed every swipe;
 * only a real swipe on the real screen catches that.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w400dp-h880dp-xxhdpi")
class ReaderRenderTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = stopKoin()

    private val quran: Quran by lazy { runBlocking { GlobalContext.get().get<QuranRepository>().quran() } }

    @Test fun thePrintedPagesRender() {
        var page by mutableIntStateOf(PAGES.first())
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxSize()) {
                    MushafPageLines(
                        lines = quran.pages[page - 1].lines,
                        quran = quran,
                        highlight = null,
                        bookmarks = emptySet(),
                        onTap = {},
                        onLongPress = {},
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        PAGES.forEach { number ->
            page = number
            compose.waitForIdle()
            shoot("mushaf-%03d".format(number))
        }
    }

    @Test fun aTapGivesTheAyahAndALongPressGivesTheSameAyah() {
        var tapped by mutableStateOf<Verse?>(null)
        var pressed by mutableStateOf<Verse?>(null)
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                Box(Modifier.fillMaxSize()) {
                    MushafPageLines(
                        lines = quran.pages[49].lines,
                        quran = quran,
                        highlight = null,
                        bookmarks = emptySet(),
                        onTap = { tapped = it },
                        onLongPress = { pressed = it },
                    )
                }
            }
        }
        // A line that closes an ayah carries a no-break space before the number.
        val line = compose.onAllNodes(hasText(NBSP, substring = true)).fetchSemanticsNodes().first().boundsInRoot
        compose.onRoot().performTouchInput { click(line.center) }
        compose.waitForIdle()
        val tap = requireNotNull(tapped) { "a tap on an ayah gives nothing" }
        assertEquals("the tapped ayah is not on the page", 50, quran.pageOf(tap.surah, tap.number))
        assertNull("a tap was taken for a long press", pressed)

        compose.onRoot().performTouchInput { longClick(line.center) }
        compose.waitForIdle()
        assertEquals("a long press found another ayah than a tap on the same spot", tap, pressed)
    }

    @Test fun swipingTurnsThePageAndALongPressOpensTheMenu() {
        val key = ReaderKey(page = 1)
        showReader(key)
        waitForText("Al-Fatihah")

        // Page 2 lies to the left of page 1, as in a printed mushaf, so the finger moves right.
        compose.onRoot().performTouchInput { swipeRight() }
        waitForText(pageLabel(2))
        shoot("reader-page-002")

        // A long press on an ayah of the page now showing opens that ayah's menu.
        // The pages either side stay composed; an unplaced one reports empty bounds at the origin.
        val width = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        val onScreen = compose.onAllNodes(hasText(NBSP + "٣", substring = true)).fetchSemanticsNodes()
            .first { it.boundsInRoot.width > 0f && it.boundsInRoot.left >= 0f && it.boundsInRoot.right <= width }
        compose.onRoot().performTouchInput { longClick(onScreen.boundsInRoot.center) }
        waitForText("Repeat this ayah")
        shoot("reader-ayah-menu")
    }

    @Test fun swipingTheOtherWayGoesBack() {
        showReader(ReaderKey(page = 3))
        waitForText(pageLabel(3))
        compose.onRoot().performTouchInput { swipeLeft() }
        waitForText(pageLabel(2))
        compose.onRoot().performTouchInput { swipeLeft() }
        waitForText("Al-Fatihah")
    }

    private fun showReader(key: ReaderKey) {
        val koin = GlobalContext.get()
        val viewModel = ReaderViewModel(key, koin.get(), koin.get(), koin.get(), koin.get())
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                ReaderScreen(key = key, onBack = {}, viewModel = viewModel)
            }
        }
    }

    /** The top bar's subtitle begins "Page 2 · Juz 1"; the dot keeps page 2 from matching page 293. */
    private fun pageLabel(page: Int) = "Page $page ·"

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val NBSP = " "

        /** The opening, a surah's start, a page full of pause marks, a sajdah page, and the last page. */
        val PAGES = listOf(1, 2, 50, 187, 293, 604)
    }
}
