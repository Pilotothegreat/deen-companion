package com.pilotothegreat.deencompanion.widget

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An unconfigured widget must behave exactly as widgets did before this existed, otherwise the
 * update changes what is already on people's home screens.
 */
class WidgetConfigTest {

    @Test fun aWidgetNeverConfiguredFollowsTheApp() {
        val config = WidgetConfig.from(emptyPreferences())
        assertNull("colours follow the app's own setting", config.dynamicColor)
        assertNull("so does the clock", config.twentyFourHour)
        assertNull("and the athkar are whatever fits now", config.pinnedAthkar)
        assertEquals("opaque", 0f, config.transparency, 0f)
        assertTrue("iqama stays visible where it fits", config.showIqama)
    }

    @Test fun choicesSurviveAWriteAndARead() {
        val chosen = WidgetConfig(
            dynamicColor = true,
            transparency = 0.4f,
            showIqama = false,
            twentyFourHour = false,
            pinnedAthkar = AthkarIds.SLEEP,
        )
        val preferences = mutablePreferencesOf().apply { WidgetConfig.write(this, chosen) }
        assertEquals(chosen, WidgetConfig.from(preferences))
    }

    @Test fun clearingAChoiceGoesBackToFollowingTheApp() {
        val preferences = mutablePreferencesOf().apply {
            WidgetConfig.write(this, WidgetConfig(dynamicColor = false, pinnedAthkar = AthkarIds.MORNING))
            WidgetConfig.write(this, WidgetConfig())
        }
        val config = WidgetConfig.from(preferences)
        assertNull(config.dynamicColor)
        assertNull(config.pinnedAthkar)
    }

    @Test fun twoWidgetsCanBeConfiguredDifferently() {
        val beside = mutablePreferencesOf().apply { WidgetConfig.write(this, WidgetConfig(transparency = 0.8f)) }
        val alone = mutablePreferencesOf().apply { WidgetConfig.write(this, WidgetConfig(transparency = 0f)) }
        assertEquals(0.8f, WidgetConfig.from(beside).transparency, 0.001f)
        assertEquals(0f, WidgetConfig.from(alone).transparency, 0.001f)
    }
}
