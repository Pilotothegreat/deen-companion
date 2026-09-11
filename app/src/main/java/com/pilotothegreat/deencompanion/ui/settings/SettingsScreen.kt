package com.pilotothegreat.deencompanion.ui.settings

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.Defaults
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.LOCATION_PERMISSIONS
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.hasLocationPermission
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.common.labelRes
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.components.ChoiceDialog
import com.pilotothegreat.deencompanion.ui.components.ConnectedChoice
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

private const val REPOSITORY_URL = "https://github.com/Pilotothegreat/deen-companion"
private const val PRIVACY_URL = "https://github.com/Pilotothegreat/deen-companion/blob/main/PRIVACY.md"

private typealias SettingsRow = @Composable (ListItemShapes) -> Unit

private sealed interface SettingsDialog {
    data object Method : SettingsDialog
    data object Asr : SettingsDialog
    data object ReciterChoice : SettingsDialog
    data class Iqama(val prayer: Prayer) : SettingsDialog
}

@Composable
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val live by viewModel.settings.collectAsStateWithLifecycle()
    val s = live ?: settings
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val refreshingLocation by viewModel.isRefreshingLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showSupport by rememberSaveable { mutableStateOf(false) }
    var exactAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    LifecycleResumeEffect(Unit) {
        exactAllowed = viewModel.canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.refreshLocation()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(viewModel) {
        viewModel.locationResults.collect { result ->
            when (result) {
                LocationRepository.Result.UPDATED -> Unit
                LocationRepository.Result.PERMISSION_MISSING -> snackbar.showSnackbar(resources.getString(R.string.location_permission_needed))
                LocationRepository.Result.UNAVAILABLE -> snackbar.showSnackbar(resources.getString(R.string.location_unavailable))
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(key = "location") {
                SettingsGroup(
                    stringResource(R.string.settings_location),
                    listOf(
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.LocationOn, stringResource(R.string.current_location),
                                s.location.cityName ?: stringResource(R.string.default_location),
                                onClick = {
                                    if (hasLocationPermission(context) || s.useIpLocationFallback) viewModel.refreshLocation()
                                    else locationPermission.launch(LOCATION_PERMISSIONS)
                                },
                                trailing = {
                                    if (refreshingLocation) LoadingIndicator(Modifier.size(24.dp))
                                    else Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh_location))
                                },
                            )
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Public, stringResource(R.string.ip_location_fallback_title),
                                stringResource(R.string.ip_location_fallback_desc), s.useIpLocationFallback, viewModel::setUseIpLocationFallback,
                            )
                        },
                    ),
                )
            }

            item(key = "prayer") {
                SettingsGroup(
                    stringResource(R.string.prayer_times),
                    listOf(
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Calculate, stringResource(R.string.calculation_method), stringResource(s.method.labelRes)) {
                                dialog = SettingsDialog.Method
                            }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Brightness5, stringResource(R.string.asr_juristic_method), stringResource(s.asrSchool.labelRes)) {
                                dialog = SettingsDialog.Asr
                            }
                        },
                        { shapes -> HijriAdjustmentRow(shapes, s, viewModel::setHijriAdjustment) },
                    ),
                )
            }

            item(key = "iqama") {
                SettingsGroup(
                    stringResource(R.string.iqama),
                    Prayer.obligatory.map { prayer ->
                        { shapes: ListItemShapes ->
                            NavRow(shapes, prayer.icon, stringResource(prayer.nameRes), iqamaSummary(s.iqama.getValue(prayer))) {
                                dialog = SettingsDialog.Iqama(prayer)
                            }
                        }
                    },
                )
            }

            item(key = "notifications") {
                val rows = buildList<SettingsRow> {
                    add { shapes ->
                        SwitchRow(
                            shapes, Icons.Rounded.Notifications, stringResource(R.string.prayer_notifications),
                            stringResource(R.string.prayer_notifications_desc), s.notificationsEnabled,
                        ) { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setNotificationsEnabled(enabled)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.Alarm, stringResource(R.string.exact_alarms),
                                stringResource(if (exactAllowed) R.string.exact_alarms_granted else R.string.exact_alarms_denied),
                            ) { context.startSafely(SystemIntents.exactAlarms(context)) }
                        }
                    }
                    add { shapes ->
                        NavRow(shapes, Icons.Rounded.VolumeUp, stringResource(R.string.adhan_sound), stringResource(R.string.sound_desc), trailing = { OpenIcon() }) {
                            context.startSafely(SystemIntents.channel(context, Notifications.CHANNEL_ADHAN))
                        }
                    }
                    add { shapes ->
                        NavRow(shapes, Icons.Rounded.NotificationsActive, stringResource(R.string.iqama_sound), stringResource(R.string.sound_desc), trailing = { OpenIcon() }) {
                            context.startSafely(SystemIntents.channel(context, Notifications.CHANNEL_IQAMA))
                        }
                    }
                }
                SettingsGroup(stringResource(R.string.notifications), rows)
            }

            item(key = "quran") {
                SettingsGroup(
                    stringResource(R.string.quran_settings),
                    listOf(
                        { shapes -> FontSizeRow(shapes, s.quranFontSize, viewModel::setQuranFontSize) },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.RecordVoiceOver, stringResource(R.string.reciter), stringResource(s.reciter.label)) {
                                dialog = SettingsDialog.ReciterChoice
                            }
                        },
                    ),
                )
            }

            item(key = "appearance") {
                val rows = buildList<SettingsRow> {
                    add { shapes ->
                        ContentRow(shapes, Icons.Rounded.Palette, stringResource(R.string.theme)) {
                            ConnectedChoice(
                                options = ThemeMode.entries,
                                selected = s.themeMode,
                                onSelect = viewModel::setThemeMode,
                                label = { stringResource(it.labelRes) },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.ColorLens, stringResource(R.string.dynamic_color),
                                stringResource(R.string.dynamic_color_desc), s.dynamicColor, viewModel::setDynamicColor,
                            )
                        }
                    }
                    add { shapes ->
                        SwitchRow(
                            shapes, Icons.Rounded.DarkMode, stringResource(R.string.pure_black),
                            stringResource(R.string.pure_black_desc), s.pureBlack, viewModel::setPureBlack,
                        )
                    }
                    add { shapes ->
                        ContentRow(shapes, Icons.Rounded.Language, stringResource(R.string.language)) {
                            ConnectedChoice(
                                options = listOf(AppLanguage.SYSTEM) + AppLanguage.supported,
                                selected = s.appLanguage,
                                onSelect = viewModel::setLanguage,
                                label = { languageLabel(it) },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                SettingsGroup(stringResource(R.string.appearance), rows)
            }

            item(key = "about") {
                SettingsGroup(
                    stringResource(R.string.about),
                    listOf(
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.Info, stringResource(R.string.app_name), versionSummary(updateState),
                                trailing = {
                                    when (updateState) {
                                        UpdateChecker.State.Checking -> LoadingIndicator(Modifier.size(24.dp))
                                        is UpdateChecker.State.Available -> Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                                        else -> Unit
                                    }
                                },
                            ) {
                                if (updateState is UpdateChecker.State.Available) context.startSafely(viewModel.updateIntent())
                                else viewModel.checkForUpdates()
                            }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Favorite, stringResource(R.string.support_development), stringResource(R.string.support_development_desc)) {
                                showSupport = true
                            }
                        },
                        { shapes ->
                            SegmentedListItem(
                                onClick = { context.startSafely(SystemIntents.url(REPOSITORY_URL)) },
                                shapes = shapes,
                                leadingContent = { Icon(painterResource(R.drawable.github), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                supportingContent = { Text(stringResource(R.string.source_code_desc)) },
                                trailingContent = { OpenIcon() },
                            ) { Text(stringResource(R.string.source_code)) }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Policy, stringResource(R.string.privacy_policy), null, trailing = { OpenIcon() }) {
                                context.startSafely(SystemIntents.url(PRIVACY_URL))
                            }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.AppSettingsAlt, stringResource(R.string.app_info), stringResource(R.string.app_info_desc)) {
                                context.startSafely(SystemIntents.appDetails(context))
                            }
                        },
                    ),
                )
            }
        }
    }

    when (val current = dialog) {
        SettingsDialog.Method -> ChoiceDialog(
            title = stringResource(R.string.calculation_method),
            options = CalculationMethod.entries,
            selected = s.method,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::setMethod,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Asr -> ChoiceDialog(
            title = stringResource(R.string.asr_juristic_method),
            options = AsrSchool.entries,
            selected = s.asrSchool,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::setAsrSchool,
            onDismiss = { dialog = null },
        )
        SettingsDialog.ReciterChoice -> ChoiceDialog(
            title = stringResource(R.string.reciter),
            options = Reciter.entries,
            selected = s.reciter,
            label = { stringResource(it.label) },
            onSelect = viewModel::setReciter,
            onDismiss = { dialog = null },
        )
        is SettingsDialog.Iqama -> IqamaDialog(
            prayer = current.prayer,
            current = s.iqama.getValue(current.prayer),
            onSave = { viewModel.setIqama(current.prayer, it) },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
    if (showSupport) SupportSheet(onDismiss = { showSupport = false })
}

@Composable
private fun SettingsGroup(title: String, rows: List<SettingsRow>) {
    Column {
        SectionHeader(title, Modifier.padding(start = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, row -> row(ListItemDefaults.segmentedShapes(index, rows.size)) }
        }
    }
}

@Composable
private fun NavRow(
    shapes: ListItemShapes,
    icon: ImageVector,
    title: String,
    summary: String?,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = trailing,
    ) { Text(title) }
}

@Composable
private fun SwitchRow(
    shapes: ListItemShapes,
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    ) { Text(title) }
}

@Composable
private fun ContentRow(shapes: ListItemShapes, icon: ImageVector, title: String, content: @Composable () -> Unit) {
    SegmentedListItem(
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = content,
    ) { Text(title) }
}

@Composable
private fun OpenIcon() = Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)

@Composable
private fun HijriAdjustmentRow(shapes: ListItemShapes, settings: AppSettings, onChange: (Int) -> Unit) {
    val locale = currentLocale()
    val preview = HijriCalendar.date(LocalDate.now(settings.zone), settings.hijriAdjustment)
        ?.let { Numerals.localize(HijriCalendar.format(it, locale), locale) }
        .orEmpty()
    val value = settings.hijriAdjustment
    val range = Defaults.HIJRI_ADJUSTMENT_RANGE
    ContentRow(shapes, Icons.Rounded.CalendarMonth, stringResource(R.string.hijri_adjustment)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(preview, modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease))
            }
            Text(
                Numerals.localize(if (value > 0) "+$value" else value.toString(), locale),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase))
            }
        }
    }
}

@Composable
private fun FontSizeRow(shapes: ListItemShapes, size: Int, onChange: (Int) -> Unit) {
    var value by remember(size) { mutableFloatStateOf(size.toFloat()) }
    val range = Defaults.QURAN_FONT_RANGE
    ContentRow(shapes, Icons.Rounded.TextFields, stringResource(R.string.arabic_text_size)) {
        Column {
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onChange(value.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / 2 - 1,
            )
            Text(
                "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                fontFamily = UthmanicHafs,
                fontSize = value.sp,
                lineHeight = (value * 1.8f).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IqamaDialog(prayer: Prayer, current: IqamaSetting, onSave: (IqamaSetting) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = currentLocale()
    var fixed by remember { mutableStateOf(current.fixed) }
    var minutes by remember { mutableIntStateOf(current.offsetMinutes) }
    val time = rememberTimePickerState(current.fixedTime.hour, current.fixedTime.minute, DateFormat.is24HourFormat(context))
    val range = Defaults.IQAMA_OFFSET_RANGE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.iqama_for, stringResource(prayer.nameRes))) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ConnectedChoice(
                    options = listOf(false, true),
                    selected = fixed,
                    onSelect = { fixed = it },
                    label = { stringResource(if (it) R.string.iqama_fixed_mode else R.string.iqama_offset_mode) },
                )
                if (fixed) {
                    TimeInput(state = time)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { minutes = (minutes - 5).coerceAtLeast(range.first) }) {
                            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease))
                        }
                        Text(
                            pluralStringResource(R.plurals.minutes, minutes, Formatters.number(minutes, locale)),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        FilledTonalIconButton(onClick = { minutes = (minutes + 5).coerceAtMost(range.last) }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase))
                        }
                    }
                    Text(stringResource(R.string.iqama_offset_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(IqamaSetting(fixed = fixed, offsetMinutes = minutes, fixedTime = LocalTime.of(time.hour, time.minute)))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun iqamaSummary(setting: IqamaSetting): String {
    val context = LocalContext.current
    val locale = currentLocale()
    return if (setting.fixed) {
        stringResource(R.string.iqama_fixed_at, Formatters.time(context, setting.fixedTime, locale))
    } else {
        pluralStringResource(R.plurals.iqama_minutes_after, setting.offsetMinutes, Formatters.number(setting.offsetMinutes, locale))
    }
}

@Composable
private fun versionSummary(state: UpdateChecker.State): String {
    val version = stringResource(R.string.version, BuildConfig.VERSION_NAME)
    val status = when (state) {
        UpdateChecker.State.Idle -> null
        UpdateChecker.State.Checking -> stringResource(R.string.update_checking)
        UpdateChecker.State.UpToDate -> stringResource(R.string.up_to_date)
        UpdateChecker.State.Failed -> stringResource(R.string.update_check_failed)
        is UpdateChecker.State.Available ->
            state.version?.let { stringResource(R.string.update_available_version, it) } ?: stringResource(R.string.update_available_generic)
    }
    return listOfNotNull(version, status).joinToString(" · ")
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    "en" -> stringResource(R.string.language_english)
    else -> stringResource(R.string.language_arabic)
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
