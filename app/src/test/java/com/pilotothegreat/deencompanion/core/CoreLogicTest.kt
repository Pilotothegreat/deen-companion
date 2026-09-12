package com.pilotothegreat.deencompanion.core

import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.core.update.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class QiblaMathTest {
    @Test fun bearingFromLondon() = assertEquals(118.99, QiblaMath.bearing(51.5074, -0.1278), 0.2)

    @Test fun bearingFromNewYork() = assertEquals(58.48, QiblaMath.bearing(40.7128, -74.0060), 0.2)

    @Test fun distanceAtKaabaIsZero() =
        assertEquals(0.0, QiblaMath.distanceKm(QiblaMath.KAABA_LATITUDE, QiblaMath.KAABA_LONGITUDE), 0.001)

    @Test fun alignmentWrapsAroundNorth() {
        assertTrue(QiblaMath.isAligned(heading = 358.0, bearing = 2.0, toleranceDegrees = 5.0))
        assertFalse(QiblaMath.isAligned(heading = 350.0, bearing = 2.0, toleranceDegrees = 5.0))
    }

    @Test fun unwrapTakesShortestPath() {
        assertEquals(370f, QiblaMath.unwrap(10f, 350f), 0.001f)
        assertEquals(-10f, QiblaMath.unwrap(350f, 0f), 0.001f)
    }
}

class ArabicTextTest {
    @Test fun normalizeStripsDiacriticsAndFoldsLetters() =
        assertEquals("ان الله", ArabicText.normalize("إِنَّ ٱللَّهَ"))

    @Test fun findMatchMapsBackToOriginalText() {
        val text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ"
        val range = ArabicText.findMatch(text, "الله")!!
        assertEquals("ٱللَّهِ", text.substring(range.first, range.last + 1))
    }

    @Test fun findMatchIsCaseInsensitiveForLatin() = assertEquals(4..8, ArabicText.findMatch("The Mercy", "mercy"))

    @Test fun missingMatchIsNull() = assertNull(ArabicText.findMatch("الحمد لله", "رحمن"))

    @Test fun arabicIndicDigits() = assertEquals("١٢٣:٤٥", Numerals.toArabicIndic("123:45"))
}

class TasbihEngineTest {
    @Test fun postPrayerCycleIs33_33_34() {
        var state = TasbihState()
        var taps = 0
        val rounds = mutableListOf<Dhikr>()
        repeat(3) {
            rounds += state.dhikr
            do {
                val step = TasbihEngine.increment(state)
                state = step.state
                taps++
            } while (!step.roundCompleted)
        }
        assertEquals(listOf(Dhikr.SUBHAN_ALLAH, Dhikr.ALHAMDULILLAH, Dhikr.ALLAHU_AKBAR), rounds)
        assertEquals(100, taps)
        assertEquals(TasbihState(), state)
    }

    @Test fun customTargetKeepsDhikr() {
        var state = TasbihState(dhikr = Dhikr.LA_ILAHA_ILLALLAH, target = 100)
        repeat(99) { state = TasbihEngine.increment(state).state }
        assertEquals(99, state.count)
        val step = TasbihEngine.increment(state)
        assertTrue(step.roundCompleted)
        assertEquals(TasbihState(0, Dhikr.LA_ILAHA_ILLALLAH, 100), step.state)
    }

    @Test fun storedValuesAreBackwardCompatible() {
        assertEquals(Dhikr.ALHAMDULILLAH, Dhikr.fromStored("الحمد لله"))
        assertEquals(Dhikr.SUBHAN_ALLAH, Dhikr.fromStored(null))
    }
}

class HijriCalendarTest {
    @Test fun firstWeekOfMarch2026IsRamadan1447() {
        val hijri = HijriCalendar.date(LocalDate.of(2026, 3, 1))!!
        assertEquals(1447, HijriCalendar.year(hijri))
        assertEquals(HijriCalendar.RAMADAN, HijriCalendar.month(hijri))
    }



    @Test fun adjustmentShiftsTheDate() {
        val on = LocalDate.of(2026, 9, 11)
        assertEquals(HijriCalendar.date(on)!!.plus(1, ChronoUnit.DAYS), HijriCalendar.date(on, adjustmentDays = 1))
    }
}

class AppVersionTest {
    @Test fun comparesReleaseTags() {
        assertTrue(AppVersion.isNewer("1.5.43", "v1.6.0"))
        assertTrue(AppVersion.isNewer("1.6", "1.6.1"))
        assertFalse(AppVersion.isNewer("1.6.0", "v1.6.0"))
        assertFalse(AppVersion.isNewer("1.6.0", "1.5.99"))
        assertFalse(AppVersion.isNewer("v1.6.0", "1.6.0-beta"))
    }
}
