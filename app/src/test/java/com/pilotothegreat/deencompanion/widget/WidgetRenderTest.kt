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

    /**
     * One row tall, which every widget can now be resized to (a row is 40dp by Android's own formula),
     * and two columns wide. Each has to keep its point at that size, not a crop of its larger self.
     */
    @Test fun oneRowTall() {
        val sizes = listOf(DpSize(110.dp, 40.dp), DpSize(180.dp, 50.dp), DpSize(250.dp, 50.dp))
        sizes.forEach { shoot("row-next-prayer", it) { NextPrayerContent(nextPrayer) } }
        sizes.forEach { shoot("row-prayer-times", it) { PrayerTimesContent(times) } }
        sizes.forEach { shoot("row-athkar", it) { AthkarContent(AthkarWidgetState(false, "Now", "evening", "Evening", "3 of 24", 0.125f, emptyList())) } }
        sizes.forEach { shoot("row-tasbih", it) { TasbihContent(TasbihWidgetState(false, "Subhan Allah", "12", "of 33", "Count dhikr")) } }
        sizes.forEach { shoot("row-moment", it) { MomentContent(MomentWidgetState(false, "Iftar in an hour", "Maghrib at 6:20 PM", hasMoment = true, athkarCategory = null)) } }
        sizes.forEach { shoot("row-qibla", it) { QiblaContent(QiblaWidgetState(false, "Qibla", 294.3f, "294°", "from north", "2,158 km to Makkah")) } }
        sizes.forEach { shoot("row-date", it) { DateContent(DateWidgetState(false, "29 Rabi' I 1448 AH", "Thursday", "18 September 2026")) } }
        sizes.forEach { shoot("row-verse", it) { QuoteContent(QuoteState(false, "Verse of the day", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship will be ease", "Ash-Sharh 94:5", Intent())) } }
        sizes.forEach {
            shoot("row-khatma", it) {
                KhatmaContent(KhatmaWidgetState(false, "Khatma", "3 pages to read today", "128 of 604 pages", 0.21f, hasPlan = true, open = Intent()))
            }
        }
    }

    /** Half see-through, over the test's wallpaper colour, which has to show through the card. */
    @Test fun transparent() {
        shoot("transparent-next-prayer", DpSize(250.dp, 180.dp)) { NextPrayerContent(nextPrayer, WidgetConfig(transparency = 0.5f)) }
        shoot("transparent-prayer-times", DpSize(250.dp, 220.dp)) { PrayerTimesContent(times, WidgetConfig(transparency = 0.5f)) }
        shoot("clear-prayer-times", DpSize(250.dp, 220.dp)) { PrayerTimesContent(times, WidgetConfig(transparency = 1f)) }
        shoot("iqama-prayer-times", DpSize(180.dp, 100.dp)) { PrayerTimesContent(times) }
    }

    @Test fun theOthers() {
        val athkar = AthkarWidgetState(
            dynamic = false,
            label = "Now",
            categoryId = "evening",
            title = "Evening",
            status = "3 of 24",
            fraction = 0.125f,
            // The category in the heading is not repeated as a meter, so an evening widget offers the morning.
            meters = listOf(AthkarMeter("Morning", 1f)),
        )
        // 100×40 is the smallest any of these can be resized to; oneRowTall covers the single row.
        listOf(DpSize(200.dp, 180.dp), DpSize(180.dp, 90.dp), DpSize(100.dp, 60.dp)).forEach { shoot("athkar", it) { AthkarContent(athkar) } }
        val tasbih = TasbihWidgetState(false, "Subhan Allah", "12", "of 33", "Count dhikr")
        listOf(DpSize(200.dp, 110.dp), DpSize(120.dp, 120.dp), DpSize(120.dp, 70.dp), DpSize(100.dp, 60.dp)).forEach { shoot("tasbih", it) { TasbihContent(tasbih) } }
        val moment = MomentWidgetState(false, "Iftar in an hour", "Maghrib at 6:20 PM", hasMoment = true, athkarCategory = null)
        listOf(DpSize(250.dp, 120.dp), DpSize(180.dp, 70.dp), DpSize(100.dp, 60.dp)).forEach { shoot("moment", it) { MomentContent(moment) } }
        val verse = QuoteState(false, "Verse of the day", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship will be ease", "Ash-Sharh 94:5", Intent())
        listOf(DpSize(250.dp, 180.dp), DpSize(180.dp, 100.dp), DpSize(100.dp, 60.dp)).forEach { shoot("verse", it) { QuoteContent(verse) } }
    }

    @Test fun theNewOnes() {
        val qibla = QiblaWidgetState(false, "Qibla", 294.3f, "294°", "from north", "2,158 km to Makkah")
        listOf(DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(180.dp, 90.dp), DpSize(100.dp, 60.dp))
            .forEach { shoot("qibla", it) { QiblaContent(qibla) } }
        val khatma = KhatmaWidgetState(
            dynamic = false,
            label = "Khatma",
            headline = "3 pages to read today",
            detail = "128 of 604 pages · 24 days left",
            fraction = 0.21f,
            hasPlan = true,
            open = Intent(),
        )
        listOf(DpSize(250.dp, 110.dp), DpSize(180.dp, 90.dp), DpSize(100.dp, 60.dp)).forEach { shoot("khatma", it) { KhatmaContent(khatma) } }
        // No plan yet: the same widget offers to begin one, and has no bar to show.
        shoot("khatma-empty", DpSize(250.dp, 110.dp)) {
            KhatmaContent(khatma.copy(headline = "Start a khatma", detail = "Read the whole mushaf by a date you choose", hasPlan = false))
        }
        val date = DateWidgetState(false, "29 Rabi' I 1448 AH", "Thursday", "18 September 2026")
        listOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(100.dp, 60.dp)).forEach { shoot("date", it) { DateContent(date) } }
    }

    /**
     * The qibla arrow at the four quarters: north up, east right, south down, west left.
     *
     * A bearing is the whole point of that widget, and an arrow drawn a quarter turn out is worse than
     * no arrow at all. Only a picture says which way it points.
     */
    @Test fun theQiblaArrowPointsWhereItIsTold() {
        listOf(0f, 90f, 180f, 270f).forEach { degrees ->
            val state = QiblaWidgetState(false, "Qibla", degrees, "${degrees.toInt()}°", "from north", "2,158 km to Makkah")
            shoot("qibla-at-${degrees.toInt()}", DpSize(250.dp, 150.dp)) { QiblaContent(state) }
        }
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
        val qibla = QiblaWidgetState(false, "القبلة", 294.3f, "٢٩٤°", "من الشمال", "٢٬١٥٨ كم إلى مكة")
        shoot("ar-qibla", DpSize(250.dp, 110.dp)) { QiblaContent(qibla) }
        val khatma = KhatmaWidgetState(false, "الختمة", "٣ صفحات لليوم", "١٢٨ من ٦٠٤ صفحة · بقي ٢٤ يومًا", 0.21f, true, Intent())
        shoot("ar-khatma", DpSize(250.dp, 110.dp)) { KhatmaContent(khatma) }
        val date = DateWidgetState(false, "٢٩ ربيع الأول ١٤٤٨ هـ", "الخميس", "١٨ سبتمبر ٢٠٢٦")
        shoot("ar-date", DpSize(250.dp, 110.dp)) { DateContent(date) }
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
