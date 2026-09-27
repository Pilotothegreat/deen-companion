package com.pilotothegreat.deencompanion.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.provideContent

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

/**
 * Stamped into a widget's own state by [refresh], so a running Glance session sees its state change
 * and loads again. Without it an update reached a live session only as a recomposition of what it had
 * already loaded — a new iqama setting, transparency or prayer time waited for the session to expire.
 */
private val REFRESH_TICK = androidx.datastore.preferences.core.longPreferencesKey("widget_refresh_tick")

/**
 * [load]s the widget's content for its current configuration and app data, and loads it again each
 * time the widget's state changes: when it is reconfigured, or when [refresh] stamps a new tick.
 * The first load happens before composition so the first frame is never empty.
 */
internal suspend fun androidx.glance.appwidget.GlanceAppWidget.provideFresh(
    context: android.content.Context,
    id: androidx.glance.GlanceId,
    load: suspend (android.content.Context, WidgetConfig) -> @androidx.compose.runtime.Composable () -> Unit,
): Nothing {
    val first = load(context, configOf(context, id))
    provideContent {
        val state = androidx.glance.currentState<Preferences>()
        var content by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(first) }
        var seen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Preferences?>(null) }
        androidx.compose.runtime.LaunchedEffect(state) {
            // The first state is the one [first] was loaded for; only a change needs another load.
            if (seen != null) content = runCatching { load(context, WidgetConfig.from(state)) }.getOrDefault(content)
            seen = state
        }
        content()
    }
}

/** Redraws one placed widget with everything loaded afresh. */
internal suspend fun androidx.glance.appwidget.GlanceAppWidget.refresh(context: android.content.Context, id: androidx.glance.GlanceId) {
    androidx.glance.appwidget.state.updateAppWidgetState(context, id) { it[REFRESH_TICK] = System.nanoTime() }
    update(context, id)
}

/** Redraws every placed copy of this widget with everything loaded afresh. */
internal suspend fun androidx.glance.appwidget.GlanceAppWidget.refreshAll(context: android.content.Context) {
    androidx.glance.appwidget.GlanceAppWidgetManager(context).getGlanceIds(javaClass).forEach { refresh(context, it) }
}

