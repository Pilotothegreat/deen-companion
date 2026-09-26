package com.pilotothegreat.deencompanion.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.ConnectedChoice
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * What a widget shows, chosen when it is placed and changeable afterwards.
 *
 * It opens on what the widget already has — 2.0 opened on defaults, so pressing Done quietly reset the
 * widget — offers only the options that widget uses, and shows the widget itself, drawn by Glance at the
 * size the launcher gave it, changing as the options do.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancelled unless the reader finishes, so a back press leaves no half-placed widget.
        setResult(Activity.RESULT_CANCELED, resultIntent())
        val kind = WidgetKind.ofProvider(AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind == null) {
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
                WidgetConfigScreen(
                    kind = kind,
                    previewSize = previewSize(kind),
                    load = ::load,
                    render = { config -> render(kind, config) },
                    save = { config -> save(kind, config) },
                    onSaved = {
                        setResult(Activity.RESULT_OK, resultIntent())
                        finish()
                    },
                )
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private suspend fun glanceId() = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

    /** What this widget already has, or the defaults for a widget being placed. */
    private suspend fun load(): WidgetConfig = runCatching { configOf(this, glanceId()) }.getOrDefault(WidgetConfig())

    /** The widget as it will look: its own content, composed by Glance into the views the launcher would show. */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun render(kind: WidgetKind, config: WidgetConfig): RemoteViews {
        val content = kind.content(this, config)
        return GlanceRemoteViews().compose(context = this, size = previewSize(kind)) { content() }.remoteViews
    }

    /** The size the launcher gave the widget, in portrait; the widget's usual size before it has one. */
    private fun previewSize(kind: WidgetKind): DpSize {
        val options = AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else kind.previewSize
    }

    private suspend fun save(kind: WidgetKind, config: WidgetConfig): Boolean = runCatching {
        val id = glanceId()
        updateAppWidgetState(this, PreferencesGlanceStateDefinition, id) { preferences ->
            preferences.toMutablePreferences().apply { WidgetConfig.write(this, config) }
        }
        kind.widget().update(this, id)
    }.onFailure { Timber.w(it, "Couldn't save the widget's settings") }.isSuccess
}

@Composable
internal fun WidgetConfigScreen(
    kind: WidgetKind,
    previewSize: DpSize,
    load: suspend () -> WidgetConfig,
    render: suspend (WidgetConfig) -> RemoteViews,
    save: suspend (WidgetConfig) -> Boolean,
    onSaved: () -> Unit,
) {
    var config by remember { mutableStateOf<WidgetConfig?>(null) }
    LaunchedEffect(Unit) { config = load() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // The launcher opens this on top of the home screen with no other clue to what is being
            // configured, and "Widget" was true of all ten of them.
            TopAppBar(
                title = { Text(stringResource(kind.label)) },
                subtitle = { Text(stringResource(R.string.widget_config)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = config
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                WidgetPreview(current, previewSize, render)
                Text(
                    stringResource(R.string.widget_config_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OptionRows(kind, current) { config = it }
                Button(
                    onClick = {
                        saving = true
                        scope.launch {
                            if (save(current)) {
                                onSaved()
                            } else {
                                saving = false
                                snackbar.showSnackbar(resources.getString(R.string.widget_config_save_failed))
                            }
                        }
                    },
                    enabled = !saving,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(stringResource(R.string.done), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

/** The options this widget uses, as one group of rows, each with its icon in a shape. */
@Composable
private fun OptionRows(kind: WidgetKind, config: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    val locale = currentLocale()
    val rows = buildList<@Composable (ListItemShapes) -> Unit> {
        if (WidgetOption.COLOURS in kind.options) add { shapes ->
            // The wallpaper's colours exist from Android 12; before that there are only two choices.
            val choices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(null, true, false) else listOf(null, false)
            SegmentedListItem(
                shapes = shapes,
                colors = optionRowColors(),
                leadingContent = { OptionBadge(Icons.Rounded.Palette, MaterialShapes.Cookie9Sided.toShape()) },
                supportingContent = {
                    ConnectedChoice(
                        options = choices,
                        selected = config.dynamicColor,
                        onSelect = { onChange(config.copy(dynamicColor = it)) },
                        label = { choice ->
                            stringResource(
                                when (choice) {
                                    null -> R.string.widget_config_follow_app
                                    true -> R.string.widget_config_wallpaper
                                    false -> R.string.widget_config_brand
                                },
                            )
                        },
                        modifier = Modifier.padding(top = Spacing.small),
                        contentPadding = PaddingValues(horizontal = Spacing.small, vertical = Spacing.small),
                    )
                },
            ) { Text(stringResource(R.string.widget_config_colours)) }
        }
        if (WidgetOption.TRANSPARENCY in kind.options) add { shapes ->
            val slider = rememberSliderState(config.transparency, (MAX_TRANSPARENCY * 10).roundToInt() - 1, 0f..MAX_TRANSPARENCY)
            SegmentedListItem(
                shapes = shapes,
                colors = optionRowColors(),
                leadingContent = { OptionBadge(Icons.Rounded.Opacity, MaterialShapes.Clover4Leaf.toShape()) },
                trailingContent = {
                    Text(
                        stringResource(R.string.widget_config_percent, Formatters.number((slider.value * 100).roundToInt(), locale)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                supportingContent = {
                    // The percentage follows the thumb; the preview, which Glance has to draw again, waits for the finger to lift.
                    Slider(
                        state = slider,
                        onValueChange = { slider.value = it },
                        onValueChangeFinished = { onChange(config.copy(transparency = (slider.value * 10).roundToInt() / 10f)) },
                    )
                },
            ) { Text(stringResource(R.string.widget_config_transparency)) }
        }
        if (WidgetOption.IQAMA in kind.options) add { shapes ->
            SegmentedListItem(
                checked = config.showIqama,
                onCheckedChange = { onChange(config.copy(showIqama = it)) },
                shapes = shapes,
                colors = optionRowColors(),
                leadingContent = { OptionBadge(Icons.Rounded.Schedule, MaterialShapes.Sunny.toShape()) },
                trailingContent = { Switch(checked = config.showIqama, onCheckedChange = null) },
            ) { Text(stringResource(R.string.widget_config_iqama)) }
        }
        if (WidgetOption.ATHKAR in kind.options) add { shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = optionRowColors(),
                leadingContent = { OptionBadge(Icons.Rounded.AutoAwesome, MaterialShapes.Flower.toShape()) },
                supportingContent = {
                    ConnectedChoice(
                        options = listOf(null, AthkarIds.MORNING, AthkarIds.EVENING),
                        selected = config.pinnedAthkar,
                        onSelect = { onChange(config.copy(pinnedAthkar = it)) },
                        label = { choice ->
                            stringResource(
                                when (choice) {
                                    AthkarIds.MORNING -> R.string.athkar_morning_reminder
                                    AthkarIds.EVENING -> R.string.athkar_evening_reminder
                                    else -> R.string.widget_config_suggested
                                },
                            )
                        },
                        modifier = Modifier.padding(top = Spacing.small),
                        contentPadding = PaddingValues(horizontal = Spacing.small, vertical = Spacing.small),
                    )
                },
            ) { Text(stringResource(R.string.widget_config_athkar)) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { index, row -> row(ListItemDefaults.segmentedShapes(index, rows.size)) }
    }
}

/**
 * One surface for every option row, selected or not.
 *
 * A row carrying a switch is a selectable row, which M3 tints when it is on, and next to the plain
 * rows above it that read as one row of four being highlighted for no reason.
 */
@Composable
private fun optionRowColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
)

@Composable
private fun OptionBadge(icon: ImageVector, shape: Shape) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
    }
}

/**
 * The widget itself, over a wash of colour standing in for the wallpaper so its transparency shows. The
 * preview is a picture: touches stop at its frame instead of opening the app behind this screen.
 */
@Composable
private fun WidgetPreview(config: WidgetConfig, size: DpSize, render: suspend (WidgetConfig) -> RemoteViews) {
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(config) {
        views = runCatching { render(config) }.onFailure { Timber.w(it, "Couldn't draw the widget preview") }.getOrNull()
    }
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer)))
                .padding(Spacing.xlarge),
            contentAlignment = Alignment.Center,
        ) {
            val width = size.width.coerceAtMost(maxWidth)
            val current = views
            Box(Modifier.width(width).height(size.height), contentAlignment = Alignment.Center) {
                if (current == null) {
                    LoadingIndicator()
                } else {
                    AndroidView(
                        factory = { context -> PictureFrame(context) },
                        update = { frame ->
                            frame.removeAllViews()
                            frame.addView(current.apply(frame.context, frame))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Holds the preview's views and keeps every touch from reaching them. */
private class PictureFrame(context: Context) : FrameLayout(context) {
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
}

private const val MAX_TRANSPARENCY = 0.9f
