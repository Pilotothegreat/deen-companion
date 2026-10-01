package com.pilotothegreat.deencompanion.ui.home

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerEngine
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Today's prayer list in the afternoon: the ones gone step back, Asr is next, the rest ahead. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class, qualifiers = "en-w400dp-h880dp-xxhdpi")
class PrayerTimesCardRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(dark: Boolean, name: String) {
        val date = LocalDate.parse("2026-09-27")
        val times = PrayerEngine.calculate(date, 23.5841, 58.4078, ZoneId.of("Asia/Muscat"), CalculationMethod.OMAN)
        val schedule = PrayerSchedule(date, times.adhan, emptyMap(), times.middleOfNight, times.lastThirdOfNight)
        compose.setContent {
            DeenTheme(darkTheme = dark, dynamicColor = false, pureBlack = false) {
                Surface(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) {
                        PrayerTimesCard(
                            schedule = schedule, nextPrayer = Prayer.ASR, muted = emptySet(), notificationsEnabled = true,
                            prayed = setOf(Prayer.FAJR), daysObserved = 0, locale = Locale.ENGLISH,
                            onToggleMute = { _, _ -> }, onTogglePrayed = { _, _ -> },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(File("build/screenshots").apply { mkdirs() }, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun afternoonLight() = shoot(dark = false, name = "prayer-list-afternoon")

    @Test fun afternoonDark() = shoot(dark = true, name = "prayer-list-afternoon-dark")
}
