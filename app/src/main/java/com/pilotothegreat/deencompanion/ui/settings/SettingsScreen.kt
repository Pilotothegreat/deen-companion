package com.pilotothegreat.deencompanion.ui.settings

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import com.pilotothegreat.deencompanion.data.backup.BackupRepository
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
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
import androidx.core.net.toUri
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
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
import com.pilotothegreat.deencompanion.ui.components.MorphBadge
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.location.locationStatus
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private const val REPOSITORY_URL = "https://github.com/Pilotothegreat/deen-companion"
private const val SPONSORS_URL = "https://github.com/sponsors/Pilotothegreat"
private const val PRIVACY_URL = "https://github.com/Pilotothegreat/deen-companion/blob/main/PRIVACY.md"

private typealias SettingsRow = @Composable (ListItemShapes) -> Unit

private sealed interface SettingsDialog {
    data object Method : SettingsDialog
    data object Asr : SettingsDialog
    data object HighLatitude : SettingsDialog
    data object FineTune : SettingsDialog
    data object ReciterChoice : SettingsDialog
    data object Reset : SettingsDialog
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

    // Storage Access Framework, so the file lands wherever the reader keeps things and the app never
    // asks for a storage permission it would otherwise have no use for.
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }
    val context = LocalContext.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbar.showSnackbar(resources.getString(it))
            viewModel.clearBackupMessage()
        }
    }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showSupport by rememberSaveable { mutableStateOf(false) }
    var showUpdate by rememberSaveable { mutableStateOf(false) }
    var showIqama by rememberSaveable { mutableStateOf(false) }
    var showRestore by rememberSaveable { mutableStateOf(false) }
    var showSounds by rememberSaveable { mutableStateOf(false) }
    var pickingFor by rememberSaveable { mutableStateOf<Prayer?>(null) }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val prayer = pickingFor ?: return@rememberLauncherForActivityResult
        pickingFor = null
        if (result.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.setAdhanSound(prayer, uri?.toString() ?: SoundSettings.SILENT)
    }
    var exactAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    // Both are granted in Android's own settings, so both are read again on the way back from there.
    var silenceAllowed by remember { mutableStateOf(QuietDuringPrayer.isAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        exactAllowed = viewModel.canScheduleExactAlarms()
        silenceAllowed = QuietDuringPrayer.isAllowed(context)
        onPauseOrDispose { }
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
            item(key = "prayer") {
                SettingsGroup(
                    stringResource(R.string.prayer_times),
                    listOf(
                        { shapes ->
                            val city = s.location.cityName ?: stringResource(R.string.default_location)
                            NavRow(shapes, Icons.Rounded.LocationOn, stringResource(R.string.current_location), city) { onOpenLocation() }
                        },
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
                        { shapes ->
                            // Five prayers behind one row: the iqama matters, but not enough to be a
                            // third of everything Settings shows.
                            NavRow(shapes, Icons.Rounded.Groups, stringResource(R.string.iqama), iqamaOverview(s.iqama)) {
                                showIqama = true
                            }
                        },
                        { shapes -> HijriAdjustmentRow(shapes, s, viewModel::setHijriAdjustment) },
                    ),
                )
            }

            item(key = "alerts") {
                val rows = buildList<SettingsRow> {
                    add { shapes ->
                        SwitchRow(
                            shapes, Icons.Rounded.Notifications, stringResource(R.string.prayer_notifications),
                            stringResource(R.string.prayer_notifications_desc), s.notificationsEnabled,
                            viewModel::setNotificationsEnabled,
                        )
                    }
                    add { shapes ->
                        NavRow(
                            shapes,
                            Icons.Rounded.VolumeUp,
                            stringResource(R.string.adhan_sound),
                            stringResource(R.string.adhan_sound_desc),
                        ) { showSounds = true }
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
                                modifier = Modifier.padding(top = Spacing.small),
                            )
                        }
                    }
                    // Which prayers it applies to only matters once it is on, and asking before
                    // then is a question about nothing.
                    if (s.sounds.preReminderMinutes > 0) {
                        add { shapes ->
                            ContentRow(shapes, Icons.Rounded.NotificationsActive, stringResource(R.string.pre_reminder_which)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier.padding(top = Spacing.small),
                                ) {
                                    Prayer.obligatory.forEach { prayer ->
                                        val chosen = prayer in s.sounds.preReminderPrayers
                                        FilterChip(
                                            selected = chosen,
                                            onClick = {
                                                viewModel.setPreReminderPrayers(
                                                    if (chosen) s.sounds.preReminderPrayers - prayer
                                                    else s.sounds.preReminderPrayers + prayer,
                                                )
                                            },
                                            label = { Text(stringResource(prayer.nameRes)) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    add { shapes ->
                        ContentRow(shapes, Icons.Rounded.DoNotDisturbOn, stringResource(R.string.silence_during_prayer)) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                                Text(
                                    if (silenceAllowed) stringResource(R.string.silence_during_prayer_desc)
                                    else stringResource(R.string.silence_needs_permission),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (silenceAllowed) {
                                    ConnectedChoice(
                                        options = Defaults.SILENCE_CHOICES,
                                        selected = s.sounds.silenceMinutes,
                                        onSelect = viewModel::setSilenceMinutes,
                                        label = {
                                            if (it == 0) stringResource(R.string.pre_reminder_off)
                                            else pluralStringResource(R.plurals.minutes, it, Formatters.number(it, locale))
                                        },
                                        modifier = Modifier.padding(top = Spacing.small),
                                    )
                                } else {
                                    // Durations it could not honour were offered here, and choosing one threw the reader
                                    // into Android's settings without a word. Now the row says what it needs and asks.
                                    FilledTonalButton(
                                        onClick = { context.startSafely(SystemIntents.doNotDisturbAccess()) },
                                        shapes = ButtonDefaults.shapes(),
                                        modifier = Modifier.padding(top = Spacing.small),
                                    ) { Text(stringResource(R.string.silence_grant)) }
                                }
                            }
                        }
                    }
                    add { shapes ->
                        SwitchRow(
                            shapes, Icons.Rounded.Alarm, stringResource(R.string.athkar_reminders),
                            stringResource(R.string.athkar_reminders_desc), s.athkarReminders,
                            viewModel::setAthkarReminders,
                        )
                    }
                    add { shapes ->
                        NavRow(
                            shapes, Icons.Rounded.BatteryAlert, stringResource(R.string.reliability),
                            stringResource(if (exactAllowed) R.string.reliability_summary_ok else R.string.reliability_summary_problem),
                        ) { onOpenReliability() }
                    }
                }
                SettingsGroup(stringResource(R.string.notifications), rows)
            }

            item(key = "reading") {
                SettingsGroup(
                    stringResource(R.string.reading),
                    listOf(
                        { shapes ->
                            NavRow(shapes, Icons.Rounded.RecordVoiceOver, stringResource(R.string.reciter), stringResource(s.reciter.label)) {
                                dialog = SettingsDialog.ReciterChoice
                            }
                        },
                    ),
                )
            }

            item(key = "appearance") {
                SettingsGroup(
                    stringResource(R.string.appearance),
                    listOf(
                        // The theme, the wallpaper's colours and OLED black in one picker. 1.9.0 folded two of
                        // them out of existence, and 2.0 brought them back as switches beside a row.
                        { shapes ->
                            Surface(shape = shapes.shape, color = MaterialTheme.colorScheme.surfaceContainer) {
                                ThemePicker(
                                    state = ThemeState(s.themeMode, s.dynamicColor, s.pureBlack),
                                    onTheme = viewModel::setTheme,
                                    onPureBlack = viewModel::setPureBlack,
                                )
                            }
                        },
                        { shapes ->
                            ContentRow(shapes, Icons.Rounded.Language, stringResource(R.string.language)) {
                                ConnectedChoice(
                                    options = listOf(AppLanguage.SYSTEM) + AppLanguage.supported,
                                    selected = s.appLanguage,
                                    onSelect = viewModel::setLanguage,
                                    label = { languageLabel(it) },
                                    modifier = Modifier.padding(top = Spacing.small),
                                )
                            }
                        },
                        { shapes ->
                            ContentRow(shapes, Icons.Rounded.FormatSize, stringResource(R.string.text_scale)) {
                                ConnectedChoice(
                                    options = TEXT_SCALES,
                                    selected = TEXT_SCALES.minByOrNull { kotlin.math.abs(it - s.accessibility.textScale) } ?: 1f,
                                    onSelect = viewModel::setTextScale,
                                    label = { stringResource(it.labelRes) },
                                    modifier = Modifier.padding(top = Spacing.small),
                                )
                            }
                        },
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.Accessibility, stringResource(R.string.simple_mode),
                                stringResource(R.string.simple_mode_desc), s.accessibility.simpleMode,
                                viewModel::setSimpleMode,
                            )
                        },
                    ),
                )
            }

            item(key = "data") {
                val counting by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
                SettingsGroup(
                    stringResource(R.string.general),
                    listOf(
                        // The one analytics control a user needs: withdrawing the consent given at
                        // first run. What is counted is for the developer, not a feature to browse.
                        { shapes ->
                            SwitchRow(
                                shapes, Icons.Rounded.QueryStats, stringResource(R.string.setup_usage),
                                stringResource(R.string.setup_usage_desc),
                                counting,
                                viewModel::setAnalytics,
                            )
                        },
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.Save, stringResource(R.string.backup_export),
                                stringResource(R.string.backup_export_desc),
                            ) { exportFile.launch(BackupRepository.FILENAME) }
                        },
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.Restore, stringResource(R.string.backup_import),
                                stringResource(R.string.backup_import_desc),
                            ) { showRestore = true }
                        },
                        { shapes ->
                            NavRow(
                                shapes, Icons.Rounded.DeleteSweep, stringResource(R.string.reset_app),
                                stringResource(R.string.reset_app_desc),
                            ) { dialog = SettingsDialog.Reset }
                        },
                    ),
                )
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
                                // The notes come first. Going to a web page to find out what a
                                // download changes, after agreeing to it, is backwards.
                                if (updateState is UpdateChecker.State.Available) showUpdate = true
                                else viewModel.checkForUpdates()
                            }
                        },
                        // The GitHub build keeps its bank sheet. The Play build links out instead:
                        // Play's payments policy generally requires Play Billing for payments to the
                        // developer, and the carve-out for donations is a link that receives nothing
                        // in return, which is exactly what this is.
                        if (BuildConfig.SUPPORT_SHEET) {
                            supportRow { showSupport = true }
                        } else {
                            { shapes: ListItemShapes ->
                                NavRow(
                                    shapes, Icons.Rounded.Favorite, stringResource(R.string.support_development),
                                    stringResource(R.string.support_link_desc), trailing = { OpenIcon() },
                                ) { context.startSafely(SystemIntents.url(SPONSORS_URL)) }
                            }
                        },
                        // One row for everything the licences ask to be named, rather than three.
                        credits?.let { c ->
                            @Composable { shapes: ListItemShapes ->
                                NavRow(
                                    shapes, Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.credits),
                                    stringResource(R.string.credits_desc, c.text.name, c.translation.name, c.translation.translator),
                                    trailing = { OpenIcon() },
                                ) { context.startSafely(SystemIntents.url(c.text.source)) }
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
        SettingsDialog.Reset -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.reset_confirm)) },
            text = { Text(stringResource(R.string.reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetEverything()
                    dialog = null
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        is SettingsDialog.Iqama -> IqamaDialog(
            prayer = current.prayer,
            current = s.iqama.getValue(current.prayer),
            onSave = { viewModel.setIqama(current.prayer, it) },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    (updateState as? UpdateChecker.State.Available)?.takeIf { showUpdate }?.let { available ->
        UpdatePrompt(available, viewModel, onDismiss = { showUpdate = false })
    }
    if (showSounds) {
        AdhanSoundSheet(
            sounds = s.sounds,
            onSystem = { viewModel.setAdhanSound(it, SoundSettings.SYSTEM_SOUND) },
            onSilent = { viewModel.setAdhanSound(it, SoundSettings.SILENT) },
            onPick = { prayer, title ->
                pickingFor = prayer
                val current = s.sounds.adhanFor(prayer).takeIf { it.startsWith("content://") }?.toUri()
                pickSound.launch(SystemIntents.pickAdhanSound(title, current))
            },
            onDismiss = { showSounds = false },
        )
    }

    if (showRestore) {
        val autoBackups by viewModel.autoBackups.collectAsStateWithLifecycle()
        RestoreSheet(
            backups = autoBackups,
            onRestore = {
                showRestore = false
                viewModel.restoreAutoBackup(it.path)
            },
            onChooseFile = {
                showRestore = false
                importFile.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = { showRestore = false },
        )
    }

    if (showIqama) {
        IqamaSheet(
            iqama = s.iqama,
            onEdit = { dialog = SettingsDialog.Iqama(it) },
            onDismiss = { showIqama = false },
        )
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
        SectionHeader(title, Modifier.padding(start = Spacing.hair))
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
    val interaction = remember { MutableInteractionSource() }
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = { MorphBadge(icon, interaction) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = trailing,
        interactionSource = interaction,
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
    val interaction = remember { MutableInteractionSource() }
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        // A row that is on fills with the badge's usual colour, so its badge takes the primary one.
        leadingContent = {
            MorphBadge(
                icon,
                interaction,
                containerColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        interactionSource = interaction,
    ) { Text(title) }
}

@Composable
private fun ContentRow(shapes: ListItemShapes, icon: ImageVector, title: String, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    SegmentedListItem(
        shapes = shapes,
        leadingContent = { MorphBadge(icon, interaction) },
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xlarge)) {
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
                            modifier = Modifier.padding(horizontal = Spacing.xlarge),
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
                    Row(Modifier.fillMaxWidth().padding(top = Spacing.small), verticalAlignment = Alignment.CenterVertically) {
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

/** A short ladder rather than a slider: four named steps are easier to choose between than sixty. */
private val TEXT_SCALES = listOf(1f, 1.15f, 1.3f, 1.5f)

@get:StringRes
private val Float.labelRes: Int
    get() = when {
        this <= 1f -> R.string.text_scale_default
        this <= 1.15f -> R.string.text_scale_large
        this <= 1.3f -> R.string.text_scale_larger
        else -> R.string.text_scale_largest
    }

/**
 * The sound each prayer's adhan plays.
 *
 * 1.9.0 promised this row and shipped a deep link into the system's notification-channel settings
 * instead, which is a different app's screen, in a different language, describing a channel sound
 * this app deliberately does not use — the adhan plays as alarm audio so that it is heard through
 * Do Not Disturb. The picker offered here is the system's own, which previews as it scrolls, and
 * asks for alarm sounds because those are the ones the player can actually honour.
 */
@Composable
private fun AdhanSoundSheet(
    sounds: SoundSettings,
    onSystem: (Prayer) -> Unit,
    onSilent: (Prayer) -> Unit,
    onPick: (Prayer, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<Prayer?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(start = Spacing.large, end = Spacing.large, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.adhan_sound_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.medium),
            )
            Prayer.obligatory.forEachIndexed { index, prayer ->
                NavRow(
                    ListItemDefaults.segmentedShapes(index, Prayer.obligatory.size),
                    prayer.icon,
                    stringResource(prayer.nameRes),
                    stringResource(soundLabel(sounds.adhanFor(prayer))),
                ) { editing = prayer }
            }
        }
    }
    editing?.let { prayer ->
        val title = stringResource(prayer.nameRes)
        val current = soundLabel(sounds.adhanFor(prayer))
        ChoiceDialog(
            title = title,
            options = SOUND_CHOICES,
            selected = if (current in SOUND_CHOICES) current else R.string.adhan_sound_pick,
            label = { stringResource(it) },
            onSelect = { choice ->
                when (choice) {
                    R.string.adhan_sound_system -> onSystem(prayer)
                    R.string.adhan_sound_silent -> onSilent(prayer)
                    else -> onPick(prayer, title)
                }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/** The three answers, in the order they are worth offering. */
private val SOUND_CHOICES = listOf(
    R.string.adhan_sound_system,
    R.string.adhan_sound_pick,
    R.string.adhan_sound_silent,
)

@StringRes
private fun soundLabel(sound: String): Int = when (sound) {
    SoundSettings.SYSTEM_SOUND -> R.string.adhan_sound_system
    SoundSettings.SILENT -> R.string.adhan_sound_silent
    else -> R.string.adhan_sound_custom
}

/**
 * Where a restore comes from.
 *
 * A file the reader exported by hand is still offered, but it is no longer the only answer: the
 * copies Bilal writes for itself each week come first, because the people who lose a khatma are
 * exactly the people who never pressed the export button.
 */
@Composable
private fun RestoreSheet(
    backups: List<AutoBackup>,
    onRestore: (AutoBackup) -> Unit,
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(start = Spacing.large, end = Spacing.large, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.backup_auto_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.medium),
            )
            if (backups.isEmpty()) {
                Text(
                    stringResource(R.string.backup_auto_none),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.medium),
                )
            } else {
                backups.forEachIndexed { index, entry ->
                    NavRow(
                        ListItemDefaults.segmentedShapes(index, backups.size),
                        Icons.Rounded.Restore,
                        DateFormat.getDateFormat(context).format(entry.savedAt) + " · " +
                            DateFormat.getTimeFormat(context).format(entry.savedAt),
                        Formatters.number((entry.bytes / 1024).toInt().coerceAtLeast(1), locale) + " kB",
                    ) { onRestore(entry) }
                }
            }
            NavRow(
                ListItemDefaults.segmentedShapes(0, 1),
                Icons.AutoMirrored.Rounded.OpenInNew,
                stringResource(R.string.backup_choose_file),
                null,
                onClick = onChooseFile,
            )
        }
    }
}

/** The five prayers' iqama times, one tap in from the prayer-times group. */
@Composable
private fun IqamaSheet(iqama: Map<Prayer, IqamaSetting>, onEdit: (Prayer) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = Spacing.large, end = Spacing.large, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            Text(
                stringResource(R.string.iqama_sheet_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.medium),
            )
            Prayer.obligatory.forEachIndexed { index, prayer ->
                NavRow(
                    ListItemDefaults.segmentedShapes(index, Prayer.obligatory.size),
                    prayer.icon,
                    stringResource(prayer.nameRes),
                    iqamaSummary(iqama.getValue(prayer)),
                ) { onEdit(prayer) }
            }
        }
    }
}

/** "25 minutes after the adhan", or how many prayers differ from the rest. */
@Composable
private fun iqamaOverview(iqama: Map<Prayer, IqamaSetting>): String {
    val distinct = iqama.values.distinct()
    return if (distinct.size == 1) iqamaSummary(distinct.first()) else stringResource(R.string.iqama_varies)
}
