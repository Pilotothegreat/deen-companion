package com.pilotothegreat.deencompanion.alarms

import com.pilotothegreat.deencompanion.core.device.OemGuidance
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two alarms that share a PendingIntent id silently overwrite each other, and the only symptom is
 * an alert that never arrives — which nobody reports as a bug, they just stop trusting the app.
 * This release takes the alarm kinds from four to seven, so every combination is enumerated here.
 */
class AlarmRequestCodesTest {

    private val days = 0 until AlarmRequestCodes.DAYS

    @Test fun everyAlarmHasAnIdOfItsOwn() {
        val seen = mutableMapOf<Int, String>()
        for (kind in AlarmKind.entries) {
            for (day in days) {
                for (prayer in Prayer.entries) {
                    val code = AlarmRequestCodes.of(kind, day, prayer)
                    val label = "$kind/$day/$prayer"
                    val clash = seen.put(code, label)
                    assertEquals("$label collides with $clash at $code", null, clash)
                }
            }
        }
        assertEquals(AlarmKind.entries.size * AlarmRequestCodes.DAYS * Prayer.entries.size, seen.size)
    }

    @Test fun theSnoozeSlotIsOutsideTheScheduledDays() {
        // The scheduler only ever uses days 0 and 1, so a snooze parked at day 9 cannot land on one.
        assertTrue(PrayerActionReceiver.SNOOZE_DAY >= 2)
        assertTrue(PrayerActionReceiver.SNOOZE_DAY < AlarmRequestCodes.DAYS)
        assertNotEquals(
            AlarmRequestCodes.of(AlarmKind.ADHAN, 0, Prayer.FAJR),
            AlarmRequestCodes.of(AlarmKind.ADHAN, PrayerActionReceiver.SNOOZE_DAY, Prayer.FAJR),
        )
    }

    @Test fun addingAKindNeverReachesIntoAnotherKindsRange() {
        // A kind's block must be wide enough for every day and prayer it can carry.
        val firstOfAdhan = AlarmRequestCodes.of(AlarmKind.ADHAN, 0, Prayer.entries.first())
        val lastOfPrePrayer = AlarmRequestCodes.of(AlarmKind.PRE_PRAYER, AlarmRequestCodes.DAYS - 1, Prayer.entries.last())
        assertTrue("PRE_PRAYER runs out before ADHAN begins", lastOfPrePrayer < firstOfAdhan)
    }

    @Test fun onlyTheAlarmsThatMustLandOnTheMinuteAskForExactTime() {
        assertTrue(AlarmKind.ADHAN.needsExactTime)
        assertTrue(AlarmKind.IQAMA.needsExactTime)
        assertTrue(AlarmKind.PRE_PRAYER.needsExactTime)
        assertTrue("silence must begin when the prayer does", AlarmKind.SILENCE_START.needsExactTime)
        assertFalse("a reminder a few minutes late costs nothing", AlarmKind.ATHKAR_MORNING.needsExactTime)
        assertFalse(AlarmKind.SILENCE_END.needsExactTime)
    }
}

class OemGuidanceTest {

    @Test fun theManufacturersKnownToKillAlarmsAreRecognised() {
        assertEquals(OemGuidance.XIAOMI, OemGuidance.forManufacturer("Xiaomi"))
        assertEquals(OemGuidance.XIAOMI, OemGuidance.forManufacturer("Redmi"))
        assertEquals(OemGuidance.XIAOMI, OemGuidance.forManufacturer("POCO"))
        assertEquals(OemGuidance.HUAWEI, OemGuidance.forManufacturer("HUAWEI"))
        assertEquals(OemGuidance.HUAWEI, OemGuidance.forManufacturer("HONOR"))
        assertEquals(OemGuidance.OPPO, OemGuidance.forManufacturer("realme"))
        assertEquals(OemGuidance.VIVO, OemGuidance.forManufacturer("vivo"))
        assertEquals(OemGuidance.SAMSUNG, OemGuidance.forManufacturer("samsung"))
    }

    @Test fun anUnknownPhoneGetsGeneralAdviceRatherThanNothing() {
        assertEquals(OemGuidance.OTHER, OemGuidance.forManufacturer("Google"))
        assertFalse("a Pixel needs no extra step", OemGuidance.needsExtraStep("Google"))
        assertTrue(OemGuidance.needsExtraStep("Xiaomi"))
    }

    @Test fun matchingIgnoresHowTheNameIsCased() {
        assertEquals(OemGuidance.SAMSUNG, OemGuidance.forManufacturer("SAMSUNG Electronics"))
    }
}
