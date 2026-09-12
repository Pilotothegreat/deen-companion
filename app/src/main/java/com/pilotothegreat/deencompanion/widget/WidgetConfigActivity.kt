package com.pilotothegreat.deencompanion.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import kotlinx.coroutines.launch

/**
 * What a widget shows, chosen when it is placed and changeable afterwards.
 *
 * Everything here is optional: an unconfigured widget follows the app's own settings, so placing one
 * and never opening this screen produces exactly the behaviour the widgets had before.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancelled unless the reader finishes, so a back press leaves no half-placed widget.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            // The launcher opens this on its own, before the app's settings are loaded, so it
            // follows the system's own dark setting rather than waiting on them.
            DeenTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                pureBlack = false,
            ) {
                WidgetConfigScreen(onDone = ::save)
            }
        }
    }

    private fun save(config: WidgetConfig) {
        lifecycleScope.launch {
            val glanceId = androidx.glance.appwidget.GlanceAppWidgetManager(this@WidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) { preferences ->
                preferences.toMutablePreferences().apply { WidgetConfig.write(this, config) }
            }
            runCatching { WidgetUpdater.updateAll(this@WidgetConfigActivity) }
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@Composable
private fun WidgetConfigScreen(onDone: (WidgetConfig) -> Unit) {
    var dynamic by remember { mutableStateOf<Boolean?>(null) }
    var transparency by remember { mutableStateOf(0f) }
    var showIqama by remember { mutableStateOf(true) }
    var pinned by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_config)) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.widget_config_intro), style = MaterialTheme.typography.bodyMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.widget_config_colours), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dynamic == null,
                        onClick = { dynamic = null },
                        label = { Text(stringResource(R.string.widget_config_follow_app)) },
                    )
                    FilterChip(
                        selected = dynamic == true,
                        onClick = { dynamic = true },
                        label = { Text(stringResource(R.string.widget_config_wallpaper)) },
                    )
                    FilterChip(
                        selected = dynamic == false,
                        onClick = { dynamic = false },
                        label = { Text(stringResource(R.string.widget_config_brand)) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.widget_config_transparency), style = MaterialTheme.typography.titleSmall)
                Slider(value = transparency, onValueChange = { transparency = it }, steps = 3)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = showIqama, onCheckedChange = { showIqama = it })
                Text(stringResource(R.string.widget_config_iqama), style = MaterialTheme.typography.bodyLarge)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.widget_config_athkar), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pinned == null,
                        onClick = { pinned = null },
                        label = { Text(stringResource(R.string.widget_config_suggested)) },
                    )
                    FilterChip(
                        selected = pinned == AthkarIds.MORNING,
                        onClick = { pinned = AthkarIds.MORNING },
                        label = { Text(stringResource(R.string.athkar_morning_reminder)) },
                    )
                    FilterChip(
                        selected = pinned == AthkarIds.EVENING,
                        onClick = { pinned = AthkarIds.EVENING },
                        label = { Text(stringResource(R.string.athkar_evening_reminder)) },
                    )
                }
            }

            Button(
                onClick = {
                    onDone(
                        WidgetConfig(
                            dynamicColor = dynamic,
                            transparency = transparency,
                            showIqama = showIqama,
                            pinnedAthkar = pinned,
                        ),
                    )
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text(stringResource(R.string.done)) }
        }
    }
}
