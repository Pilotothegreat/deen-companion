package com.pilotothegreat.deencompanion.ui.quran

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The search field against the shipped mushaf, saved to app/build/screenshots to be looked at.
 *
 * "Ayat al-Kursi" is the case worth a picture: the name is nowhere in the ayah, so what the screen
 * shows for it is either the ayah itself at the top of the list or nothing at all.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w400dp-h880dp-xxhdpi")
class QuranSearchRenderTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = stopKoin()

    @Test fun aPassageByName() = search("ayat al kursi", "Al-Baqarah 2:255", "search-ayat-al-kursi")

    @Test fun aReference() = search("2:255", "Al-Baqarah 2:255", "search-reference")

    @Test fun aJuz() = search("juz 30", "An-Naba 78:1", "search-juz")

    /** A sentence names nothing, so the list is the ordinary text search and nothing above it. */
    @Test fun aSentence() = search("show me the verse about patience", "Ayahs", "search-sentence")

    @Test
    @Config(qualifiers = "ar-w400dp-h880dp-xxhdpi")
    fun aPassageInArabic() = search("آية الكرسي", "٢:٢٥٥", "search-ar-ayat-al-kursi")

    private fun search(query: String, expected: String, name: String) {
        val koin = GlobalContext.get()
        val viewModel = QuranViewModel(koin.get(), koin.get(), koin.get())
        compose.setContent {
            DeenTheme(darkTheme = false, dynamicColor = false, pureBlack = false) {
                QuranScreen(onOpenReader = {}, viewModel = viewModel)
            }
        }
        compose.waitForIdle()
        viewModel.onQueryChange(query)
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText(expected, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        shoot(name)
        assertTrue(
            "\"$query\" found nothing for \"$expected\"",
            compose.onAllNodes(hasText(expected, substring = true)).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun shoot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
