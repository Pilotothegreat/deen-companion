package com.pilotothegreat.deencompanion.widget

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The screen a widget is set up on, drawn with its live preview and saved to app/build/screenshots/widgets
 * to be looked at: in English by day for the next prayer, in Arabic at night for the athkar.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Not 34: the reader's render test leaves DeenApplication's settings collector running in that sandbox after
// stopping Koin, and its next emission failed whichever test came after.
@Config(sdk = [33], application = Application::class)
class WidgetConfigRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val nextPrayer = NextPrayerState(
        dynamic = false,
        prayer = Prayer.ASR,
        label = "Next prayer",
        name = "Asr",
        countdownTarget = 0L,
        elapsed = 0.4f,
        adhan = "Adhan 3:35 PM",
        iqama = "Iqama 3:55 PM",
        day = listOf(
            TimeCell("Fajr", "4:36", isNext = false),
            TimeCell("Dhuhr", "12:09", isNext = false),
            TimeCell("Asr", "3:35", isNext = true),
            TimeCell("Maghrib", "6:20", isNext = false),
            TimeCell("Isha", "7:30", isNext = false),
        ),
    )

    private val athkar = AthkarWidgetState(
        dynamic = false,
        label = "الآن",
        categoryId = "evening",
        title = "أذكار المساء",
        status = "٣ من ٢٤",
        fraction = 0.125f,
        meters = listOf(AthkarMeter("أذكار الصباح", 1f), AthkarMeter("أذكار المساء", 0.125f)),
    )

    @Test
    @Config(qualifiers = "en-w400dp-h880dp-xxhdpi")
    fun theNextPrayerInEnglishByDay() = shoot("config-next-prayer", WidgetKind.NEXT_PRAYER, dark = false)

    @Test
    @Config(qualifiers = "ar-w400dp-h880dp-night-xxhdpi")
    fun theAthkarInArabicAtNight() = shoot("config-athkar-ar", WidgetKind.ATHKAR_NOW, dark = true)

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun shoot(name: String, kind: WidgetKind, dark: Boolean) {
        val rendered = AtomicBoolean(false)
        compose.setContent {
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                WidgetConfigScreen(
                    kind = kind,
                    previewSize = kind.previewSize,
                    load = { WidgetConfig(transparency = 0.3f) },
                    render = { config ->
                        GlanceRemoteViews().compose(context, kind.previewSize) {
                            BilalWidgetTheme(dynamic = false) { Content(kind, config) }
                        }.remoteViews.also { rendered.set(true) }
                    },
                    save = { true },
                    onSaved = {},
                )
            }
        }
        compose.waitUntil(20_000) { rendered.get() }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(context.getString(R.string.done))).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots/widgets").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable
    private fun Content(kind: WidgetKind, config: WidgetConfig) = when (kind) {
        WidgetKind.ATHKAR_NOW -> AthkarContent(athkar, config)
        else -> NextPrayerContent(nextPrayer, config)
    }
}
