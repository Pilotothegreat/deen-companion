package com.pilotothegreat.deencompanion.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What each widget shows at the size it is given. Sizes include the short heights a One UI stack hands
 * out, where a layout drawn for a bigger box lost its bottom rows.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class WidgetContentTest {

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

    @Test
    fun aLargeNextPrayerWidgetShowsEverything() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(300.dp, 250.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { NextPrayerContent(nextPrayer) } }
        onNode(hasText("Next prayer")).assertExists()
        onNode(hasText("Iqama 3:55 PM")).assertExists()
        onNode(hasText("Maghrib")).assertExists()
        onAllNodes(hasText("Asr")).assertCountEquals(2)
    }

    @Test
    fun aShortNextPrayerWidgetLeavesTheStripOutRatherThanCroppingIt() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 150.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { NextPrayerContent(nextPrayer) } }
        onNode(hasText("Asr")).assertExists()
        onNode(hasText("Adhan 3:35 PM")).assertExists()
        onAllNodes(hasText("Maghrib")).assertCountEquals(0)
    }

    @Test
    fun aCompactNextPrayerWidgetKeepsOnlyNameAndCountdown() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(120.dp, 60.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { NextPrayerContent(nextPrayer) } }
        onNode(hasText("Asr")).assertExists()
        onAllNodes(hasText("Adhan 3:35 PM")).assertCountEquals(0)
        onAllNodes(hasText("Maghrib")).assertCountEquals(0)
    }

    @Test
    fun thePrayerTimesWidgetShowsAllFiveTimesAtEverySize() {
        val sizes = listOf(
            DpSize(180.dp, 60.dp), DpSize(180.dp, 100.dp), DpSize(180.dp, 130.dp), DpSize(250.dp, 100.dp),
            DpSize(180.dp, 160.dp), DpSize(250.dp, 170.dp), DpSize(250.dp, 220.dp), DpSize(320.dp, 300.dp),
        )
        sizes.forEach { size ->
            runGlanceAppWidgetUnitTest {
                setContext(context)
                setAppWidgetSize(size)
                provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
                times.rows.forEach { row -> onNode(hasText(row.time)).assertExists() }
            }
        }
    }

    @Test
    fun aTallPrayerTimesWidgetNamesEveryPrayer() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(180.dp, 170.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        times.rows.forEach { row -> onNode(hasText(row.name)).assertExists() }
    }

    @Test
    fun prayerTimesShowIqamaOnlyWhenWide() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 220.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("Muscat, Oman")).assertExists()
        onNode(hasText("3:55")).assertExists()
    }

    @Test
    fun aThreeCellListStillShowsIqama() = runGlanceAppWidgetUnitTest {
        // The iqama used to need 250dp, the widget's own minimum; a launcher a few dp short hid it.
        setContext(context)
        setAppWidgetSize(DpSize(180.dp, 220.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("Isha")).assertExists()
        onNode(hasText("3:55")).assertExists()
    }

    @Test
    fun theStripShowsIqamaWhenItFits() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(180.dp, 100.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("3:55")).assertExists()
    }

    @Test
    fun iqamaTurnedOffStaysOff() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(320.dp, 300.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times, WidgetConfig(showIqama = false)) } }
        onAllNodes(hasText("3:55")).assertCountEquals(0)
    }

    @Test
    fun wideShortPrayerTimesUseAOneLineHeader() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 100.dp))
        // With iqama on, the iqama line takes the room the header would have had.
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times, WidgetConfig(showIqama = false)) } }
        onNode(hasText("29 Rabi' I 1448 AH · Muscat, Oman")).assertExists()
    }

    @Test
    fun theVerseShowsItsTranslationOnlyWhenTall() {
        val verse = QuoteState(false, "Verse of the day", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship will be ease", "Ash-Sharh 94:5", Intent())
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(180.dp, 100.dp))
            provideComposable { BilalWidgetTheme(dynamic = false) { QuoteContent(verse) } }
            onNode(hasText("Ash-Sharh 94:5")).assertExists()
            onAllNodes(hasText("For indeed, with hardship will be ease")).assertCountEquals(0)
        }
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(250.dp, 220.dp))
            provideComposable { BilalWidgetTheme(dynamic = false) { QuoteContent(verse) } }
            onNode(hasText("For indeed, with hardship will be ease")).assertExists()
        }
    }

    @Test
    fun athkarShowMorningAndEveningOnlyWhenThereIsRoom() {
        val state = AthkarWidgetState(
            dynamic = false,
            label = "Now",
            categoryId = "evening",
            title = "Evening",
            status = "3 of 24",
            fraction = 0.125f,
            meters = listOf(AthkarMeter("Morning", 1f), AthkarMeter("Evening athkar", 0.125f)),
        )
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(180.dp, 200.dp))
            provideComposable { BilalWidgetTheme(dynamic = false) { AthkarContent(state) } }
            onNode(hasText("3 of 24")).assertExists()
            onNode(hasText("Morning")).assertExists()
            onNode(hasText("Evening athkar")).assertExists()
        }
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(180.dp, 90.dp))
            provideComposable { BilalWidgetTheme(dynamic = false) { AthkarContent(state) } }
            onNode(hasText("Evening")).assertExists()
            onAllNodes(hasText("Evening athkar")).assertCountEquals(0)
        }
    }

    @Test
    fun theTasbihButtonIsLabelledForScreenReadersAtEverySize() {
        val state = TasbihWidgetState(false, "Subhan Allah", "12", "of 33", "Count dhikr")
        listOf(DpSize(180.dp, 100.dp), DpSize(120.dp, 120.dp), DpSize(120.dp, 70.dp)).forEach { size ->
            runGlanceAppWidgetUnitTest {
                setContext(context)
                setAppWidgetSize(size)
                provideComposable { BilalWidgetTheme(dynamic = false) { TasbihContent(state) } }
                onNode(hasText("12")).assertExists()
                onNode(hasContentDescription("Count dhikr")).assertExists()
            }
        }
    }

    @Test
    fun theMomentWidgetShowsItsTitleAndBody() = runGlanceAppWidgetUnitTest {
        val state = MomentWidgetState(false, "Iftar in an hour", "Maghrib at 6:20 PM", hasMoment = true, athkarCategory = null)
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 120.dp))
        provideComposable { BilalWidgetTheme(dynamic = false) { MomentContent(state) } }
        onNode(hasText("Iftar in an hour")).assertExists()
        onNode(hasText("Maghrib at 6:20 PM")).assertExists()
    }
}
