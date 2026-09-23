package com.pilotothegreat.deencompanion.ui.home

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.ui.components.MorphBadge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.reliability.hasReliabilityProblem
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.moment.MomentEngine
import com.pilotothegreat.deencompanion.core.moment.Dismissal
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
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
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
    onOpenLocation: () -> Unit,
    onOpenAthkar: (String) -> Unit,
    onOpenReader: (ReaderKey) -> Unit,
    onOpenReliability: () -> Unit,
    onUpdateAvailable: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    // Ticks every second. Read only inside the hero, so the second hand redraws one card rather than
    // the whole of Today; the rest reads what changes once a prayer, through derivedStateOf.
    val countdownState = viewModel.countdown.collectAsStateWithLifecycle()
    val hasCountdown by remember { derivedStateOf { countdownState.value != null } }
    val nextAdhan by remember { derivedStateOf { countdownState.value?.next } }
    val athkarNow by viewModel.athkarNow.collectAsStateWithLifecycle()
    val prayed by viewModel.prayedToday.collectAsStateWithLifecycle()
    val daysObserved by viewModel.daysObserved.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var permissions by remember { mutableStateOf(PermissionStatus.of(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = PermissionStatus.of(context)
        viewModel.onResume()
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
                HomeEvent.LocationUnavailable -> scope.launch {
                    snackbar.showSnackbar(resources.getString(R.string.location_unavailable))
                }
                HomeEvent.LocationPermissionMissing -> scope.launch {
                    snackbar.showSnackbar(resources.getString(R.string.location_permission_needed))
                }
                is HomeEvent.UpdateAvailable -> onUpdateAvailable()
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
                        // Where the times are for, and pressed to change it: the location card at the bottom
                        // of Today said the same thing a long scroll away.
                        LocationChip("${gregorianDate(locale, current.today.date)} · $city", onOpenLocation)
                    }
                },
                actions = {
                    // The same compass, in the same cookie, as the Qibla card further down.
                    val qiblaPress = remember { MutableInteractionSource() }
                    val qiblaLabel = stringResource(R.string.qibla_compass)
                    IconButton(
                        onClick = onOpenQibla,
                        interactionSource = qiblaPress,
                        modifier = Modifier.semantics { contentDescription = qiblaLabel },
                    ) {
                        MorphBadge(
                            Icons.Rounded.Explore,
                            qiblaPress,
                            rest = MaterialShapes.Cookie7Sided,
                            pressed = MaterialShapes.Cookie4Sided,
                            size = 36.dp,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = LocalBottomBarPadding.current)) },
    ) { padding ->
        if (current == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (permissions.location) viewModel.refreshLocation()
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
                contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.small, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                // At most one thing to go and fix. A column of permission cards above the prayer
                // times is what teaches people to stop reading this screen.
                val repairs = buildList<@Composable () -> Unit> {
                    if (!permissions.location && current.settings.location.isDefault) {
                        add {
                            PermissionCard(
                                icon = Icons.Rounded.MyLocation,
                                title = stringResource(R.string.permission_location_title),
                                body = stringResource(R.string.permission_location_body),
                                actionLabel = stringResource(R.string.allow),
                                onAction = { locationPermission.launch(LOCATION_PERMISSIONS) },
                                secondaryActionLabel = stringResource(R.string.choose_city),
                                onSecondaryAction = onOpenLocation,
                            )
                        }
                    }
                    if (current.settings.notificationsEnabled && !permissions.notifications) {
                        add {
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
                            )
                        }
                    } else if (current.settings.notificationsEnabled && !permissions.exactAlarms) {
                        add {
                            PermissionCard(
                                icon = Icons.Rounded.Alarm,
                                title = stringResource(R.string.permission_exact_title),
                                body = stringResource(R.string.permission_exact_body),
                                actionLabel = stringResource(R.string.open_settings),
                                onAction = { context.startSafely(SystemIntents.exactAlarms(context)) },
                            )
                        }
                    }
                    // Everything else that can silence an alert — battery optimisation, a blocked
                    // channel, a manufacturer's own killer — has no single action to offer, so it
                    // points at the screen that explains each one. Checked here rather than
                    // re-derived, since ReliabilityScreen already knows the whole list.
                    if (isEmpty() && current.settings.notificationsEnabled &&
                        hasReliabilityProblem(context, permissions.exactAlarms)
                    ) {
                        add {
                            PermissionCard(
                                icon = Icons.Rounded.NotificationsActive,
                                title = stringResource(R.string.reliability_card_title),
                                body = stringResource(R.string.reliability_card_body),
                                actionLabel = stringResource(R.string.reliability),
                                onAction = onOpenReliability,
                            )
                        }
                    }
                }
                repairs.firstOrNull()?.let { repair ->
                    item(key = "repair") { Box(springItem()) { repair() } }
                }
                items(cards.take(MomentEngine.TODAY_CARDS - repairs.take(1).size), key = { it.id }) { moment ->
                    MomentCard(
                        moment = moment,
                        locale = locale,
                        onOpen = moment.athkarCategory?.let { category -> { onOpenAthkar(category) } },
                        onDismiss = if (moment.dismissal == Dismissal.NONE) null else { { viewModel.dismiss(moment) } },
                        modifier = springItem(),
                        // Travel is the one thing the app suspects but never decides: it asks.
                        answer = when (moment.id) {
                            "travel-ask" -> stringResource(R.string.travel_yes) to { viewModel.setTravelling(true) }
                            "travel-duas" -> null
                            else -> null
                        },
                        decline = when (moment.id) {
                            "travel-ask" -> stringResource(R.string.travel_no) to { viewModel.setTravelling(false) }
                            "travel-qasr" -> stringResource(R.string.travel_end) to { viewModel.setTravelling(false) }
                            else -> null
                        },
                    )
                }
                if (hasCountdown) {
                    item(key = "hero") { countdownState.value?.let { NextPrayerHero(it, locale, springItem()) } }
                }
                item(key = "times") {
                    Box(springItem()) {
                        PrayerTimesCard(
                            schedule = current.today,
                            nextPrayer = nextAdhan?.takeIf { it.adhan.toLocalDate() == current.today.date }?.prayer,
                            muted = current.settings.mutedPrayers,
                            notificationsEnabled = current.settings.notificationsEnabled,
                            prayed = prayed,
                            daysObserved = daysObserved,
                            locale = locale,
                            onToggleMute = viewModel::setMuted,
                            onTogglePrayed = viewModel::setPrayed,
                        )
                    }
                }
                athkarNow?.let { now ->
                    item(key = "athkar") {
                        Box(springItem()) {
                            AthkarNowCard(now.category, now.progress, locale, onOpen = { onOpenAthkar(now.category.id) })
                        }
                    }
                }
                item(key = "qibla") { Box(springItem()) { QiblaShortcut(current.settings.location, locale, onOpenQibla) } }
            }
        }
    }
}

private fun gregorianDate(locale: Locale, date: LocalDate): String =
    Formatters.pattern(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(date)

/** The date and the place under the Hijri date, pressed to choose another place; its pin morphs as it is pressed. */
@Composable
private fun LocationChip(text: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    Row(
        Modifier
            .clip(CircleShape)
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button) {
                haptics.click()
                onClick()
            }
            .padding(end = Spacing.medium, top = Spacing.hair, bottom = Spacing.hair),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        MorphBadge(
            Icons.Rounded.LocationOn,
            press,
            rest = MaterialShapes.Clover4Leaf,
            pressed = MaterialShapes.Cookie4Sided,
            size = 28.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Cards arrive, leave and trade places on the expressive springs rather than the list's default tween. */
@Composable
private fun LazyItemScope.springItem(): Modifier {
    val motion = MaterialTheme.motionScheme
    return Modifier.animateItem(
        fadeInSpec = motion.defaultEffectsSpec(),
        placementSpec = motion.defaultSpatialSpec(),
        fadeOutSpec = motion.fastEffectsSpec(),
    )
}
