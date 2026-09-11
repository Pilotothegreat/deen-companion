package com.pilotothegreat.deencompanion.ui.home

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.ui.common.LOCATION_PERMISSIONS
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.canScheduleExactAlarms
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.hasLocationPermission
import com.pilotothegreat.deencompanion.ui.common.labelRes
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.components.PermissionCard
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

private data class PermissionStatus(val location: Boolean, val notifications: Boolean, val exactAlarms: Boolean) {
    companion object {
        fun of(context: Context) = PermissionStatus(
            location = hasLocationPermission(context),
            notifications = Notifications.canPost(context),
            exactAlarms = canScheduleExactAlarms(context),
        )
    }
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val countdown by viewModel.countdown.collectAsStateWithLifecycle()
    val tasbih by viewModel.tasbihState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = currentLocale()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var permissions by remember { mutableStateOf(PermissionStatus.of(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = PermissionStatus.of(context)
        onPauseOrDispose { }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissions = PermissionStatus.of(context)
        if (result.values.any { it }) viewModel.refreshLocation()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissions = PermissionStatus.of(context)
        // After repeated denials the system no longer shows the prompt, so open settings instead.
        if (!granted) context.startSafely(SystemIntents.appNotifications(context))
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.RoundCompleted -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                HomeEvent.LocationUnavailable -> scope.launch {
                    snackbar.showSnackbar(resources.getString(R.string.location_unavailable))
                }
                HomeEvent.LocationPermissionMissing -> scope.launch {
                    snackbar.showSnackbar(resources.getString(R.string.location_permission_needed))
                }
                is HomeEvent.TasbihReset -> scope.launch {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.tasbih_reset_done),
                        actionLabel = resources.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTasbih(event.previous)
                }
                HomeEvent.UpdateAvailable -> scope.launch {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.update_available_short),
                        actionLabel = resources.getString(R.string.update_action),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) context.startSafely(viewModel.updateIntent())
                }
            }
        }
    }

    val current = content
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    val title = current?.hijri?.let {
                        stringResource(R.string.hijri_date, Numerals.localize(HijriCalendar.format(it, locale), locale))
                    } ?: stringResource(R.string.today)
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitle = {
                    if (current != null) {
                        val city = current.settings.location.cityName ?: stringResource(R.string.default_location)
                        Text("${gregorianDate(locale, current.today.date)} · $city", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (current == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (permissions.location || current.settings.useIpLocationFallback) viewModel.refreshLocation()
                else locationPermission.launch(LOCATION_PERMISSIONS)
            },
            state = pullState,
            modifier = Modifier.padding(padding),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!permissions.location && current.settings.location.isDefault) {
                    item(key = "permission-location") {
                        PermissionCard(
                            icon = Icons.Rounded.MyLocation,
                            title = stringResource(R.string.permission_location_title),
                            body = stringResource(R.string.permission_location_body),
                            actionLabel = stringResource(R.string.allow),
                            onAction = { locationPermission.launch(LOCATION_PERMISSIONS) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (current.settings.notificationsEnabled && !permissions.notifications) {
                    item(key = "permission-notifications") {
                        PermissionCard(
                            icon = Icons.Rounded.Notifications,
                            title = stringResource(R.string.permission_notifications_title),
                            body = stringResource(R.string.permission_notifications_body),
                            actionLabel = stringResource(R.string.allow),
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    context.startSafely(SystemIntents.appNotifications(context))
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                } else if (current.settings.notificationsEnabled && !permissions.exactAlarms) {
                    item(key = "permission-exact") {
                        PermissionCard(
                            icon = Icons.Rounded.Alarm,
                            title = stringResource(R.string.permission_exact_title),
                            body = stringResource(R.string.permission_exact_body),
                            actionLabel = stringResource(R.string.open_settings),
                            onAction = { context.startSafely(SystemIntents.exactAlarms(context)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                val ramadanDays = current.daysUntilRamadan
                if (current.showRamadanCard && ramadanDays != null && current.hijri != null) {
                    item(key = "ramadan") {
                        RamadanCard(
                            daysLeft = ramadanDays,
                            locale = locale,
                            onDismiss = { viewModel.dismissRamadan(HijriCalendar.year(current.hijri)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                countdown?.let { cd -> item(key = "hero") { NextPrayerHero(cd, locale) } }
                item(key = "times") {
                    PrayerTimesCard(
                        schedule = current.today,
                        nextPrayer = countdown?.next?.takeIf { it.adhan.toLocalDate() == current.today.date }?.prayer,
                        muted = current.settings.mutedPrayers,
                        notificationsEnabled = current.settings.notificationsEnabled,
                        locale = locale,
                        onToggleMute = viewModel::setMuted,
                    )
                }
                item(key = "tasbih") {
                    TasbihCard(
                        state = tasbih,
                        locale = locale,
                        onTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            viewModel.incrementTasbih()
                        },
                        onReset = viewModel::resetTasbih,
                        onTargetChange = viewModel::setTasbihTarget,
                        onDhikrChange = viewModel::setDhikr,
                    )
                }
                item(key = "qibla") { QiblaShortcut(current.settings.location, locale, onOpenQibla) }
                item(key = "inspiration") { InspirationCard(current.inspiration, locale) }
                item(key = "method") {
                    Text(
                        text = stringResource(R.string.calculated_with, stringResource(current.settings.method.labelRes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun gregorianDate(locale: Locale, date: LocalDate): String {
    val pattern = if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale)).format(date)
}
