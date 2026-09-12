package com.pilotothegreat.deencompanion.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.Defaults
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
import com.pilotothegreat.deencompanion.data.settings.toAppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** An install that has never opened settings must behave exactly as the app is meant to out of the box. */
class SmartSettingsMappingTest {

    @Test fun defaultsMatchTheShippedBehaviour() {
        val settings = emptyPreferences().toAppSettings()

        assertTrue("the app reacts to the day and the weather by default", settings.smart.reactToTheWorld)
        assertTrue("the Islamic day turns over at Maghrib, which is not a preference", settings.smart.hijriDayStartsAtMaghrib)
        assertFalse("calamity mode stays off until asked for", settings.smart.calamityActive(nowMillis = 1_000))
        assertEquals(Defaults.SAFAR_KM, settings.smart.safarKm)

        assertEquals(SoundSettings.SYSTEM_SOUND, settings.sounds.adhanFor(Prayer.FAJR))
        assertEquals("no early reminder until one is chosen", 0, settings.sounds.preReminderMinutes)
        assertEquals(0, settings.sounds.silenceMinutes)

        assertEquals(RepeatMode.OFF, settings.quran.repeatMode)
        assertEquals(1f, settings.quran.playbackSpeed, 0.001f)

        assertFalse(settings.accessibility.simpleMode)
        assertEquals(ContrastMode.SYSTEM, settings.accessibility.contrast)
        assertEquals(ReduceMotion.SYSTEM, settings.accessibility.reduceMotion)
        assertTrue(settings.accessibility.haptics)
        assertFalse(settings.onboardingCompleted)
        assertEquals(0, settings.lastSeenVersionCode)
    }

    @Test fun storedValuesRoundTrip() {
        val settings = preferencesOf(
            booleanPreferencesKey("react_to_the_world") to false,
            longPreferencesKey("calamity_until") to 5_000L,
            intPreferencesKey("safar_km") to 100,
            stringPreferencesKey("fajr_adhan_sound") to "makkah",
            stringPreferencesKey("asr_adhan_sound") to SoundSettings.SILENT,
            intPreferencesKey("pre_reminder_minutes") to 15,
            stringSetPreferencesKey("pre_reminder_prayers") to setOf("Fajr", "Maghrib"),
            intPreferencesKey("silence_during_prayer_minutes") to 20,
            floatPreferencesKey("quran_playback_speed") to 1.5f,
            stringPreferencesKey("quran_repeat_mode") to "AYAH",
            intPreferencesKey("quran_repeat_count") to 7,
            booleanPreferencesKey("simple_mode") to true,
            floatPreferencesKey("text_scale") to 1.4f,
            stringPreferencesKey("contrast_mode") to "HIGH",
            stringPreferencesKey("reduce_motion") to "ON",
            booleanPreferencesKey("haptics_enabled") to false,
            booleanPreferencesKey("onboarding_completed") to true,
            intPreferencesKey("last_seen_version_code") to 195,
        ).toAppSettings()

        assertFalse(settings.smart.reactToTheWorld)
        assertTrue(settings.smart.calamityActive(nowMillis = 4_000))
        assertFalse(settings.smart.calamityActive(nowMillis = 6_000))
        assertEquals(100, settings.smart.safarKm)

        assertEquals("makkah", settings.sounds.adhanFor(Prayer.FAJR))
        assertEquals(SoundSettings.SILENT, settings.sounds.adhanFor(Prayer.ASR))
        assertEquals(SoundSettings.SYSTEM_SOUND, settings.sounds.adhanFor(Prayer.ISHA))
        assertEquals(15, settings.sounds.preReminderMinutes)
        assertEquals(setOf(Prayer.FAJR, Prayer.MAGHRIB), settings.sounds.preReminderPrayers)
        assertEquals(20, settings.sounds.silenceMinutes)

        assertEquals(1.5f, settings.quran.playbackSpeed, 0.001f)
        assertEquals(RepeatMode.AYAH, settings.quran.repeatMode)
        assertEquals(7, settings.quran.repeatCount)

        assertTrue(settings.accessibility.simpleMode)
        assertEquals(1.4f, settings.accessibility.textScale, 0.001f)
        assertEquals(ContrastMode.HIGH, settings.accessibility.contrast)
        assertEquals(ReduceMotion.ON, settings.accessibility.reduceMotion)
        assertFalse(settings.accessibility.haptics)
        assertTrue(settings.onboardingCompleted)
        assertEquals(195, settings.lastSeenVersionCode)
    }

    @Test fun outOfRangeValuesAreClamped() {
        val settings = preferencesOf(
            intPreferencesKey("safar_km") to 5,
            intPreferencesKey("quran_audio_cache_mb") to 99_999,
            floatPreferencesKey("quran_playback_speed") to 9f,
            floatPreferencesKey("text_scale") to 4f,
            intPreferencesKey("quran_repeat_count") to 0,
        ).toAppSettings()

        assertEquals(Defaults.SAFAR_RANGE.first, settings.smart.safarKm)
        assertEquals(Defaults.AUDIO_CACHE_RANGE.last, settings.quran.audioCacheMb)
        assertEquals(2f, settings.quran.playbackSpeed, 0.001f)
        assertEquals(Defaults.TEXT_SCALE_RANGE.endInclusive, settings.accessibility.textScale, 0.001f)
        assertEquals(1, settings.quran.repeatCount)
    }

    @Test fun unknownEnumNamesFallBackInsteadOfCrashing() {
        val settings = preferencesOf(
            stringPreferencesKey("quran_repeat_mode") to "sideways",
            stringPreferencesKey("contrast_mode") to "",
        ).toAppSettings()

        assertEquals(RepeatMode.OFF, settings.quran.repeatMode)
        assertEquals(ContrastMode.SYSTEM, settings.accessibility.contrast)
    }
}
