package com.pilotothegreat.deencompanion.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * What a single placed widget was told to show.
 *
 * Per widget rather than per app, because two copies of the same widget on two screens are usually
 * placed for different reasons — one beside the clock, one on a page of its own — and a setting
 * buried in the app cannot tell them apart.
 */
data class WidgetConfig(
    /** Follow the wallpaper's colours (true) or Bilal's (false) instead of the app's own choice (null). */
    val dynamicColor: Boolean? = null,
    /** 0f is opaque; higher values let the wallpaper through. */
    val transparency: Float = 0f,
    val showIqama: Boolean = true,
    /** An athkar category pinned in place of the one the engine suggests. */
    val pinnedAthkar: String? = null,
) {
    companion object {
        private val DYNAMIC = stringPreferencesKey("widget_dynamic")
        private val TRANSPARENCY = floatPreferencesKey("widget_transparency")
        private val SHOW_IQAMA = booleanPreferencesKey("widget_show_iqama")
        private val PINNED_ATHKAR = stringPreferencesKey("widget_pinned_athkar")

        /** Unset values fall back to the app's own settings, which is what an unconfigured widget wants. */
        fun from(preferences: Preferences) = WidgetConfig(
            dynamicColor = preferences[DYNAMIC]?.toBooleanStrictOrNull(),
            transparency = preferences[TRANSPARENCY] ?: 0f,
            showIqama = preferences[SHOW_IQAMA] ?: true,
            pinnedAthkar = preferences[PINNED_ATHKAR],
        )

        fun write(preferences: MutablePreferences, config: WidgetConfig) {
            config.dynamicColor?.let { preferences[DYNAMIC] = it.toString() } ?: preferences.remove(DYNAMIC)
            preferences[TRANSPARENCY] = config.transparency
            preferences[SHOW_IQAMA] = config.showIqama
            config.pinnedAthkar?.let { preferences[PINNED_ATHKAR] = it } ?: preferences.remove(PINNED_ATHKAR)
        }
    }
}

/** The configuration of one placed widget, or the defaults when it was never configured. */
internal suspend fun configOf(context: android.content.Context, id: androidx.glance.GlanceId): WidgetConfig =
    runCatching {
        WidgetConfig.from(
            androidx.glance.appwidget.state.getAppWidgetState(
                context,
                androidx.glance.state.PreferencesGlanceStateDefinition,
                id,
            ),
        )
    }.getOrDefault(WidgetConfig())
