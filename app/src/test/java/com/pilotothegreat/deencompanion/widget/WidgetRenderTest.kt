package com.pilotothegreat.deencompanion.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

/**
 * The widgets drawn to pictures, at the sizes a launcher and a One UI stack hand out, saved to
 * app/build/screenshots/widgets to be looked at. The content tests say a prayer is there; only a picture
 * says it can be read.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class, qualifiers = "en-xxhdpi")
class WidgetRenderTest {

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

    private val times = PrayerTimesState(
        dynamic = false,
        title = "29 Rabi' I 1448 AH",
        place = "Muscat, Oman",
        rows = listOf(
            TimesRow(Prayer.FAJR, "Fajr", "4:36", "5:01", isNext = false),
            TimesRow(Prayer.DHUHR, "Dhuhr", "12:09", "12:34", isNext = false),
            TimesRow(Prayer.ASR, "Asr", "3:35", "3:55", isNext = true),
            TimesRow(Prayer.MAGHRIB, "Maghrib", "6:20", "6:30", isNext = false),
            TimesRow(Prayer.ISHA, "Isha", "7:30", "7:50", isNext = false),
        ),
    )

    @Test fun theNextPrayer() = listOf(DpSize(320.dp, 250.dp), DpSize(250.dp, 200.dp), DpSize(320.dp, 150.dp), DpSize(250.dp, 110.dp), DpSize(200.dp, 70.dp), DpSize(120.dp, 60.dp), DpSize(100.dp, 60.dp))
        .forEach { shoot("next-prayer", it) { NextPrayerContent(nextPrayer) } }

    @Test fun thePrayerTimes() = listOf(DpSize(320.dp, 300.dp), DpSize(250.dp, 220.dp), DpSize(320.dp, 150.dp), DpSize(250.dp, 100.dp), DpSize(180.dp, 130.dp), DpSize(180.dp, 60.dp))
        .forEach { shoot("prayer-times", it) { PrayerTimesContent(times) } }

    @Test fun theOthers() {
        val athkar = AthkarWidgetState(
            dynamic = false,
            label = "Now",
            categoryId = "evening",
            title = "Evening",
            status = "3 of 24",
            fraction = 0.125f,
            meters = listOf(AthkarMeter("Morning", 1f), AthkarMeter("Evening athkar", 0.125f)),
        )
        // 100×60 is the smallest any of these can be resized to.
        listOf(DpSize(200.dp, 180.dp), DpSize(180.dp, 90.dp), DpSize(100.dp, 60.dp)).forEach { shoot("athkar", it) { AthkarContent(athkar) } }
        val tasbih = TasbihWidgetState(false, "Subhan Allah", "12", "of 33", "Count dhikr")
        listOf(DpSize(200.dp, 110.dp), DpSize(120.dp, 120.dp), DpSize(120.dp, 70.dp), DpSize(100.dp, 60.dp)).forEach { shoot("tasbih", it) { TasbihContent(tasbih) } }
        val moment = MomentWidgetState(false, "Iftar in an hour", "Maghrib at 6:20 PM", hasMoment = true, athkarCategory = null)
        listOf(DpSize(250.dp, 120.dp), DpSize(180.dp, 70.dp), DpSize(100.dp, 60.dp)).forEach { shoot("moment", it) { MomentContent(moment) } }
        val verse = QuoteState(false, "Verse of the day", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship will be ease", "Ash-Sharh 94:5", Intent())
        listOf(DpSize(250.dp, 180.dp), DpSize(180.dp, 100.dp), DpSize(100.dp, 60.dp)).forEach { shoot("verse", it) { QuoteContent(verse) } }
    }

    /** Right to left, Arabic-Indic digits and the dark scheme: where a pill or a badge lands on the wrong side. */
    @Test
    @Config(qualifiers = "ar-night-xxhdpi")
    fun inArabicAtNight() {
        val next = nextPrayer.copy(
            label = "الصلاة القادمة",
            name = "العصر",
            adhan = "الأذان ٣:٣٥ م",
            iqama = "الإقامة ٣:٥٥ م",
            day = listOf(
                TimeCell("الفجر", "٤:٣٦", isNext = false),
                TimeCell("الظهر", "١٢:٠٩", isNext = false),
                TimeCell("العصر", "٣:٣٥", isNext = true),
                TimeCell("المغرب", "٦:٢٠", isNext = false),
                TimeCell("العشاء", "٧:٣٠", isNext = false),
            ),
        )
        listOf(DpSize(320.dp, 250.dp), DpSize(200.dp, 70.dp), DpSize(100.dp, 60.dp)).forEach { shoot("ar-next-prayer", it) { NextPrayerContent(next) } }
        val table = times.copy(
            title = "٢٩ ربيع الأول ١٤٤٨ هـ",
            place = "مسقط، عُمان",
            rows = listOf(
                TimesRow(Prayer.FAJR, "الفجر", "٤:٣٦", "٥:٠١", isNext = false),
                TimesRow(Prayer.DHUHR, "الظهر", "١٢:٠٩", "١٢:٣٤", isNext = false),
                TimesRow(Prayer.ASR, "العصر", "٣:٣٥", "٣:٥٥", isNext = true),
                TimesRow(Prayer.MAGHRIB, "المغرب", "٦:٢٠", "٦:٣٠", isNext = false),
                TimesRow(Prayer.ISHA, "العشاء", "٧:٣٠", "٧:٥٠", isNext = false),
            ),
        )
        listOf(DpSize(320.dp, 300.dp), DpSize(320.dp, 150.dp), DpSize(180.dp, 60.dp)).forEach { shoot("ar-prayer-times", it) { PrayerTimesContent(table) } }
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun shoot(name: String, size: DpSize, content: @Composable () -> Unit) {
        val views = runBlocking {
            GlanceRemoteViews().compose(context, size) { BilalWidgetTheme(dynamic = false) { content() } }.remoteViews
        }
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).roundToInt()
        val height = (size.height.value * density).roundToInt()
        val frame = FrameLayout(context)
        frame.addView(views.apply(context, frame))
        frame.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        frame.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // A mid-tone wallpaper, so the widget's own edges and transparency show.
        Canvas(bitmap).also { it.drawColor(WALLPAPER) }.let { frame.draw(it) }
        val dir = File("build/screenshots/widgets").apply { mkdirs() }
        File(dir, "$name-${size.width.value.roundToInt()}x${size.height.value.roundToInt()}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val WALLPAPER = 0xFF5E7389.toInt()
    }
}
