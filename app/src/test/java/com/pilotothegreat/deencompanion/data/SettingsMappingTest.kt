package com.pilotothegreat.deencompanion.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.IqamaRule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.LegacySettingsMigration
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.settings.toAppSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SettingsMappingTest {

    @Test fun emptyStoreUsesDefaults() {
        val settings = emptyPreferences().toAppSettings()
        assertTrue(settings.location.isDefault)
        assertEquals(CalculationMethod.OMAN, settings.method)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.dynamicColor)
        assertTrue(settings.notificationsEnabled)
        assertEquals(IqamaRule.Offset(25), settings.iqama.getValue(Prayer.FAJR).rule)
        assertEquals("Asia/Muscat", settings.zone.id)
    }

    @Test fun legacyValuesAreMigrated() = runTest {
        val legacy = preferencesOf(
            stringPreferencesKey("theme") to "Dark",
            stringPreferencesKey("hijri_method") to "REGIONAL",
            intPreferencesKey("app_launch_count") to 7,
            stringSetPreferencesKey("muted_prayers") to setOf("Asr"),
            booleanPreferencesKey("dhuhr_iqama_is_fixed") to true,
            stringPreferencesKey("dhuhr_iqama_time") to "13:05",
            stringPreferencesKey("calc_method") to "MWL",
        )
        assertTrue(LegacySettingsMigration.shouldMigrate(legacy))

        val migrated = LegacySettingsMigration.migrate(legacy)
        val settings = migrated.toAppSettings()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.dynamicColor)
        assertEquals(1, settings.hijriAdjustment)
        assertEquals(setOf(Prayer.ASR), settings.mutedPrayers)
        assertEquals(IqamaRule.Fixed(LocalTime.of(13, 5)), settings.iqama.getValue(Prayer.DHUHR).rule)
        assertEquals(CalculationMethod.MWL, settings.method)
        assertNull(migrated[intPreferencesKey("app_launch_count")])
        assertFalse(LegacySettingsMigration.shouldMigrate(migrated))
    }
}
