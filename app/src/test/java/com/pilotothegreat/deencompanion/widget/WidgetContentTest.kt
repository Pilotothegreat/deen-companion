package com.pilotothegreat.deencompanion.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Checks what each widget shows at its small and large sizes. */
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
            TimesRow("Fajr", "4:36", "5:01", isNext = false),
            TimesRow("Dhuhr", "12:09", "12:34", isNext = false),
            TimesRow("Asr", "3:35", "3:55", isNext = true),
            TimesRow("Maghrib", "6:20", "6:30", isNext = false),
            TimesRow("Isha", "7:30", "7:50", isNext = false),
        ),
    )

    @Test
    fun nextPrayerCardAddsTheDaysTimesWhenLarge() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(NextPrayerWidget.FULL)
        provideComposable { BilalWidgetTheme(dynamic = false) { NextPrayerContent(nextPrayer) } }
        onNode(hasText("Next prayer")).assertExists()
        onNode(hasText("Iqama 3:55 PM")).assertExists()
        onNode(hasText("Maghrib")).assertExists()
        onAllNodes(hasText("Asr")).assertCountEquals(2)
    }

    @Test
    fun compactNextPrayerKeepsOnlyNameAndCountdown() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(NextPrayerWidget.COMPACT)
        provideComposable { BilalWidgetTheme(dynamic = false) { NextPrayerContent(nextPrayer) } }
        onNode(hasText("Asr")).assertExists()
        onAllNodes(hasText("Adhan 3:35 PM")).assertCountEquals(0)
        onAllNodes(hasText("Maghrib")).assertCountEquals(0)
    }

    @Test
    fun prayerTimesTableShowsIqamaOnlyWhenWide() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(PrayerTimesWidget.LIST_WIDE)
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("Muscat, Oman")).assertExists()
        onNode(hasText("3:55")).assertExists()
    }

    @Test
    fun narrowPrayerTimesTableLeavesIqamaOut() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(PrayerTimesWidget.LIST)
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("Isha")).assertExists()
        onAllNodes(hasText("3:55")).assertCountEquals(0)
    }

    @Test
    fun wideShortPrayerTimesUsesOneLineHeader() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(PrayerTimesWidget.COLUMNS)
        provideComposable { BilalWidgetTheme(dynamic = false) { PrayerTimesContent(times) } }
        onNode(hasText("29 Rabi' I 1448 AH · Muscat, Oman")).assertExists()
    }

    @Test
    fun verseShowsTranslationOnlyWhenTall() = runGlanceAppWidgetUnitTest {
        val verse = QuoteState(false, "Verse of the day", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship will be ease", "Ash-Sharh 94:5", Intent())
        setContext(context)
        setAppWidgetSize(QUOTE_SHORT)
        provideComposable { BilalWidgetTheme(dynamic = false) { QuoteContent(verse) } }
        onNode(hasText("Ash-Sharh 94:5")).assertExists()
        onAllNodes(hasText("For indeed, with hardship will be ease")).assertCountEquals(0)
    }

    @Test
    fun athkarShowsMorningAndEveningWhenTall() = runGlanceAppWidgetUnitTest {
        val state = AthkarWidgetState(
            dynamic = false,
            label = "Now",
            categoryId = "evening",
            title = "Evening",
            status = "3 of 24",
            fraction = 0.125f,
            meters = listOf(AthkarMeter("Morning", 1f), AthkarMeter("Evening athkar", 0.125f)),
        )
        setContext(context)
        setAppWidgetSize(AthkarWidget.TALL)
        provideComposable { BilalWidgetTheme(dynamic = false) { AthkarContent(state) } }
        onNode(hasText("3 of 24")).assertExists()
        onNode(hasText("Morning")).assertExists()
        onNode(hasText("Evening athkar")).assertExists()
    }

    @Test
    fun tasbihButtonIsLabelledForScreenReaders() = runGlanceAppWidgetUnitTest {
        val state = TasbihWidgetState(false, "Subhan Allah", "12", "of 33", "Count dhikr")
        setContext(context)
        setAppWidgetSize(TasbihWidget.WIDE)
        provideComposable { BilalWidgetTheme(dynamic = false) { TasbihContent(state) } }
        onNode(hasText("12")).assertExists()
        onNode(hasContentDescription("Count dhikr")).assertExists()
    }
}
