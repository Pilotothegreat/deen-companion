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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.QuietDuringPrayer
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.Defaults
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.common.labelRes
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.components.ChoiceDialog
import com.pilotothegreat.deencompanion.ui.components.ConnectedChoice
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.location.locationStatus
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

private const val REPOSITORY_URL = "https://github.com/Pilotothegreat/deen-companion"
private const val PRIVACY_URL = "https://github.com/Pilotothegreat/deen-companion/blob/main/PRIVACY.md"

private typealias SettingsRow = @Composable (ListItemShapes) -> Unit

private sealed interface SettingsDialog {
    data object Method : SettingsDialog
    data object Asr : SettingsDialog
    data object HighLatitude : SettingsDialog
    data object FineTune : SettingsDialog
    data object ReciterChoice : SettingsDialog
    data object AudioCache : SettingsDialog
    data object Calamity : SettingsDialog
    data class Iqama(val prayer: Prayer) : SettingsDialog
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenReliability: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val live by viewModel.settings.collectAsStateWithLifecycle()
    val s = live ?: settings
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val credits by viewModel.quranCredits.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.audioCacheBytes.collectAsStateWithLifecycle()
    val cacheSummary = remember(cacheBytes) { Formatters.megabytes(cacheBytes) }
    LaunchedEffect(Unit) { viewModel.refreshAudioCacheSize() }
    val context = LocalContext.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showSupport by rememberSaveable { mutableStateOf(false) }
    var exactAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    LifecycleResumeEffect(Unit) {
        exactAllowed = viewModel.canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
                            val city = s.location.cityName ?: stringResource(R.string.default_location)
                            val status = locationStatus(s.location)
                            NavRow(
                                shapes, Icons.Rounded.LocationOn, stringResource(R.string.current_location),
                                // The default location's name already says it's the default.
                                if (s.location.isDefault) city else "$city · $status",
                                onClick = onOpenLocation,
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
                            val methodName = stringResource(s.effectiveMethod.labelRes)
                            NavRow(
                                shapes, Icons.Rounded.Calculate, stringResource(R.string.calculation_method),
                                if (s.methodAuto) stringResource(R.string.method_automatic_desc, methodName) else methodName,
                            ) { dialog = SettingsDialog.Method }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Brightness5, stringResource(R.string.asr_juristic_method), stringResource(s.asrSchool.labelRes)) {
                                dialog = SettingsDialog.Asr
                            }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.Tune, stringResource(R.string.fine_tune), adjustmentSummary(s.adjustments)) {
                                dialog = SettingsDialog.FineTune
                            }
                        },
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.NightsStay, stringResource(R.string.high_latitude_rule), stringResource(s.highLatitude.labelRes)) {
                                dialog = SettingsDialog.HighLatitude
                            }
                        },
                        { shapes -> HijriAdjustmentRow(shapes, s, viewModel::setHijriAdjustment) },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.WbTwilight, stringResource(R.string.hijri_maghrib),
                                stringResource(R.string.hijri_maghrib_desc), s.smart.hijriDayStartsAtMaghrib,
                                viewModel::setHijriDayStartsAtMaghrib,
                            )
                        },
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
                        NavRow(
                            shapes, Icons.Rounded.BatteryAlert, stringResource(R.string.reliability),
                            stringResource(R.string.reliability_intro),
                        ) { onOpenReliability() }
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
                    add { shapes ->
                        ContentRow(shapes, Icons.Rounded.Schedule, stringResource(R.string.pre_reminder)) {
                            ConnectedChoice(
                                options = Defaults.PRE_REMINDER_CHOICES,
                                selected = s.sounds.preReminderMinutes,
                                onSelect = viewModel::setPreReminder,
                                label = {
                                    if (it == 0) stringResource(R.string.pre_reminder_off)
                                    else pluralStringResource(R.plurals.minutes, it, Formatters.number(it, locale))
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    add { shapes ->
                        // Do Not Disturb can only be changed with an explicit grant, so the row says so
                        // rather than silently doing nothing.
                        val allowed = QuietDuringPrayer.isAllowed(context)
                        ContentRow(shapes, Icons.Rounded.DoNotDisturbOn, stringResource(R.string.silence_during_prayer)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (allowed) stringResource(R.string.silence_during_prayer_desc)
                                    else stringResource(R.string.silence_needs_permission),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ConnectedChoice(
                                    options = Defaults.SILENCE_CHOICES,
                                    selected = s.sounds.silenceMinutes,
                                    onSelect = { minutes ->
                                        if (minutes > 0 && !allowed) context.startSafely(SystemIntents.doNotDisturbAccess())
                                        else viewModel.setSilenceMinutes(minutes)
                                    },
                                    label = {
                                        if (it == 0) stringResource(R.string.pre_reminder_off)
                                        else pluralStringResource(R.plurals.minutes, it, Formatters.number(it, locale))
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
                SettingsGroup(stringResource(R.string.notifications), rows)
            }

            item(key = "athkar") {
                SettingsGroup(
                    stringResource(R.string.athkar),
                    listOf(
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Alarm, stringResource(R.string.athkar_reminders),
                                stringResource(R.string.athkar_reminders_desc), s.athkarReminders,
                            ) { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.setAthkarReminders(enabled)
                            }
                        },
                        { shapes ->
                            FontSizeRow(
                                shapes, s.athkarFontSize, viewModel::setAthkarFontSize,
                                range = Defaults.ATHKAR_FONT_RANGE,
                                title = stringResource(R.string.athkar_text_size),
                                fontFamily = Amiri,
                                sample = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                            )
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Translate, stringResource(R.string.athkar_show_translation),
                                stringResource(R.string.athkar_translation_desc), s.athkarShowTranslation ?: !locale.isArabic,
                                viewModel::setAthkarShowTranslation,
                            )
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Abc, stringResource(R.string.athkar_show_transliteration),
                                stringResource(R.string.athkar_transliteration_desc), s.athkarShowTransliteration,
                                viewModel::setAthkarShowTransliteration,
                            )
                        },
                    ),
                )
            }

            item(key = "smart") {
                SettingsGroup(
                    stringResource(R.string.smart_features),
                    listOf(
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Cloud, stringResource(R.string.smart_weather),
                                stringResource(R.string.smart_weather_desc), s.smart.weather, viewModel::setSmartWeather,
                            )
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Flight, stringResource(R.string.smart_travel),
                                stringResource(R.string.smart_travel_desc, Formatters.number(s.smart.safarKm, locale)),
                                s.smart.travel, viewModel::setSmartTravel,
                            )
                        },
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.Home, stringResource(R.string.smart_home),
                                stringResource(if (s.smart.hasHome) R.string.smart_home_set else R.string.smart_home_unset),
                            ) { viewModel.anchorHomeHere() }
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Public, stringResource(R.string.smart_events),
                                stringResource(R.string.smart_events_desc), s.smart.naturalEvents,
                                viewModel::setNaturalEvents,
                            )
                        },
                        { shapes ->
                            // The app suggests; you decide what counts as a calamity.
                            NavRow(
                                shapes, Icons.Rounded.VolunteerActivism, stringResource(R.string.calamity_mode),
                                if (s.smart.calamityActive(System.currentTimeMillis())) {
                                    stringResource(R.string.calamity_mode_on, Formatters.date(s.smart.calamityUntil, s.zone, locale))
                                } else {
                                    stringResource(R.string.calamity_mode_off)
                                },
                            ) { dialog = SettingsDialog.Calamity }
                        },
                    ),
                )
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
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Repeat, stringResource(R.string.continuous_playback),
                                stringResource(R.string.continuous_playback_desc), s.quran.continuousPlayback,
                                viewModel::setContinuousPlayback,
                            )
                        },
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.CloudDownload, stringResource(R.string.audio_cache),
                                stringResource(R.string.audio_cache_desc, Formatters.number(s.quran.audioCacheMb, locale), cacheSummary),
                            ) { dialog = SettingsDialog.AudioCache }
                        },
                    ),
                )
            }

            item(key = "accessibility") {
                SettingsGroup(
                    stringResource(R.string.accessibility),
                    listOf(
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Accessibility, stringResource(R.string.simple_mode),
                                stringResource(R.string.simple_mode_desc), s.accessibility.simpleMode,
                                viewModel::setSimpleMode,
                            )
                        },
                        { shapes ->
                            ContentRow(shapes, Icons.Rounded.FormatSize, stringResource(R.string.text_scale)) {
                                ConnectedChoice(
                                    options = TEXT_SCALES,
                                    selected = TEXT_SCALES.minByOrNull { kotlin.math.abs(it - s.accessibility.textScale) } ?: 1f,
                                    onSelect = viewModel::setTextScale,
                                    label = { stringResource(R.string.text_scale_value, Formatters.decimal(it, locale)) },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        },
                        { shapes ->
                            ContentRow(shapes, Icons.Rounded.Contrast, stringResource(R.string.contrast)) {
                                ConnectedChoice(
                                    options = ContrastMode.entries,
                                    selected = s.accessibility.contrast,
                                    onSelect = viewModel::setContrast,
                                    label = { stringResource(it.labelRes) },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        },
                        { shapes ->
                            ContentRow(shapes, Icons.Rounded.Animation, stringResource(R.string.reduce_motion)) {
                                ConnectedChoice(
                                    options = ReduceMotion.entries,
                                    selected = s.accessibility.reduceMotion,
                                    onSelect = viewModel::setReduceMotion,
                                    label = { stringResource(it.labelRes) },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.TouchApp, stringResource(R.string.large_targets),
                                stringResource(R.string.large_targets_desc), s.accessibility.largeTouchTargets,
                                viewModel::setLargeTouchTargets,
                            )
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Vibration, stringResource(R.string.haptics),
                                stringResource(R.string.haptics_desc), s.accessibility.haptics, viewModel::setHaptics,
                            )
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
                    listOfNotNull<SettingsRow>(
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
                        if (BuildConfig.SUPPORT_SHEET) supportRow { showSupport = true } else null,
                        // Both licences ask to be named with a link back to the source.
                        credits?.let { c ->
                            @Composable { shapes: ListItemShapes ->
                                NavRow(shapes, Icons.AutoMirrored.Rounded.MenuBook, c.text.name, c.edition, trailing = { OpenIcon() }) {
                                    context.startSafely(SystemIntents.url(c.text.source))
                                }
                            }
                        },
                        credits?.let { c ->
                            @Composable { shapes: ListItemShapes ->
                                NavRow(
                                    shapes, Icons.Rounded.Translate, c.translation.name,
                                    stringResource(R.string.translated_by, c.translation.translator, c.translation.license),
                                    trailing = { OpenIcon() },
                                ) { context.startSafely(SystemIntents.url(c.translation.source)) }
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
            // null stands for "Automatic".
            options = listOf<CalculationMethod?>(null) + CalculationMethod.entries,
            selected = if (s.methodAuto) null else s.method,
            label = { method -> method?.let { stringResource(it.labelRes) } ?: stringResource(R.string.method_automatic) },
            onSelect = { method -> if (method == null) viewModel.setMethodAuto() else viewModel.setMethod(method) },
            onDismiss = { dialog = null },
        )
        SettingsDialog.HighLatitude -> ChoiceDialog(
            title = stringResource(R.string.high_latitude_rule),
            options = HighLatitudeMode.entries,
            selected = s.highLatitude,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::setHighLatitude,
            onDismiss = { dialog = null },
        )
        SettingsDialog.FineTune -> FineTuneDialog(
            adjustments = s.adjustments,
            onChange = viewModel::setAdjustment,
            onReset = viewModel::resetAdjustments,
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
        SettingsDialog.Calamity -> CalamityDialog(
            active = s.smart.calamityActive(System.currentTimeMillis()),
            onChoose = { days ->
                viewModel.setCalamityDays(days)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.AudioCache -> AudioCacheDialog(
            currentMb = s.quran.audioCacheMb,
            usedBytes = cacheBytes,
            onSelect = viewModel::setAudioCacheMb,
            onClear = viewModel::clearAudioCache,
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

/** The "Support development" row. The Google Play build leaves it out (BuildConfig.SUPPORT_SHEET). */
private fun supportRow(onOpen: () -> Unit): SettingsRow = { shapes ->
    NavRow(shapes, Icons.Rounded.Favorite, stringResource(R.string.support_development), stringResource(R.string.support_development_desc)) {
        onOpen()
    }
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
private fun FontSizeRow(
    shapes: ListItemShapes,
    size: Int,
    onChange: (Int) -> Unit,
    range: IntRange = Defaults.QURAN_FONT_RANGE,
    title: String = stringResource(R.string.arabic_text_size),
    fontFamily: FontFamily = UthmanicHafs,
    sample: String = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
) {
    var value by remember(size) { mutableFloatStateOf(size.toFloat()) }
    ContentRow(shapes, Icons.Rounded.TextFields, title) {
        Column {
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onChange(value.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / 2 - 1,
            )
            Text(
                sample,
                fontFamily = fontFamily,
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

/** Minutes with a sign, isolated so "+3" keeps its order inside Arabic text. */
private fun signedMinutes(minutes: Int, locale: Locale): String =
    Formatters.ltr(Numerals.localize(if (minutes > 0) "+$minutes" else minutes.toString(), locale))

@Composable
private fun adjustmentSummary(adjustments: Map<Prayer, Int>): String {
    if (adjustments.isEmpty()) return stringResource(R.string.fine_tune_none)
    val locale = currentLocale()
    val names = Prayer.entries.associateWith { stringResource(it.nameRes) }
    val minutes = adjustments.mapValues { stringResource(R.string.minutes_signed, signedMinutes(it.value, locale)) }
    return Prayer.entries.filter { it in adjustments }.joinToString(" · ") { "${names.getValue(it)} ${minutes.getValue(it)}" }
}

@Composable
private fun FineTuneDialog(
    adjustments: Map<Prayer, Int>,
    onChange: (Prayer, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    val range = Defaults.ADJUSTMENT_RANGE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fine_tune)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.fine_tune_desc), style = MaterialTheme.typography.bodyMedium)
                Prayer.entries.forEach { prayer ->
                    val value = adjustments[prayer] ?: 0
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(prayer.nameRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onChange(prayer, value - 1) }, enabled = value > range.first) {
                            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease))
                        }
                        Text(
                            stringResource(R.string.minutes_signed, signedMinutes(value, locale)),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 64.dp),
                        )
                        IconButton(onClick = { onChange(prayer, value + 1) }, enabled = value < range.last) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.fine_tune_done)) } },
        dismissButton = { TextButton(onClick = onReset, enabled = adjustments.isNotEmpty()) { Text(stringResource(R.string.reset)) } },
    )
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

/** How much of the phone recitation may use, and a way to hand it back. */
@Composable
private fun AudioCacheDialog(
    currentMb: Int,
    usedBytes: Long,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_cache)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.audio_cache_explainer))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AUDIO_CACHE_CHOICES.forEach { mb ->
                        FilterChip(
                            selected = mb == currentMb,
                            onClick = { onSelect(mb) },
                            label = { Text(stringResource(R.string.megabytes, Formatters.number(mb, locale))) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.audio_cache_used, Formatters.megabytes(usedBytes)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.audio_cache_clear)) } },
    )
}

private val AUDIO_CACHE_CHOICES = listOf(128, 256, 512, 1024)

/**
 * "Times of calamity" is a mode you turn on, with an end date, rather than a feed the app decides
 * for you. No neutral source of "what is happening" exists, one maintainer could not moderate one,
 * and an app meant to pull people out of the feed should not become one.
 */
@Composable
private fun CalamityDialog(active: Boolean, onChoose: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calamity_mode)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.calamity_body))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = false, onClick = { onChoose(7) }, label = { Text(stringResource(R.string.calamity_for_week)) })
                    FilterChip(selected = false, onClick = { onChoose(30) }, label = { Text(stringResource(R.string.calamity_for_month)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        dismissButton = if (active) {
            { TextButton(onClick = { onChoose(0) }) { Text(stringResource(R.string.calamity_turn_off)) } }
        } else {
            null
        },
    )
}

/** A short ladder rather than a slider: three named steps are easier to choose between than sixty. */
private val TEXT_SCALES = listOf(1f, 1.15f, 1.3f, 1.5f)
