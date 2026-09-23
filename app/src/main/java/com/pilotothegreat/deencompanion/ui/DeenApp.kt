package com.pilotothegreat.deencompanion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.settings.WhatsNewSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.ui.athkar.AthkarScreen
import com.pilotothegreat.deencompanion.ui.athkar.AthkarSessionScreen
import com.pilotothegreat.deencompanion.ui.hadith.HadithBookScreen
import com.pilotothegreat.deencompanion.ui.hadith.HadithScreen
import com.pilotothegreat.deencompanion.ui.home.HomeScreen
import com.pilotothegreat.deencompanion.ui.location.LocationPickerScreen
import com.pilotothegreat.deencompanion.ui.theme.LocalAccessibility
import com.pilotothegreat.deencompanion.ui.navigation.AthkarKey
import com.pilotothegreat.deencompanion.ui.navigation.AthkarSessionKey
import com.pilotothegreat.deencompanion.ui.navigation.FloatingBarClearance
import com.pilotothegreat.deencompanion.ui.navigation.FloatingNavBar
import com.pilotothegreat.deencompanion.ui.navigation.HadithBookKey
import com.pilotothegreat.deencompanion.ui.navigation.HadithKey
import com.pilotothegreat.deencompanion.ui.navigation.HomeKey
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.navigation.LocationKey
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.onboarding.SetupSheet
import com.pilotothegreat.deencompanion.ui.navigation.QiblaKey
import com.pilotothegreat.deencompanion.ui.navigation.QuranKey
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.navigation.ReliabilityKey
import com.pilotothegreat.deencompanion.ui.navigation.SettingsKey
import com.pilotothegreat.deencompanion.ui.navigation.TopLevel
import com.pilotothegreat.deencompanion.ui.qibla.QiblaScreen
import com.pilotothegreat.deencompanion.ui.quran.QuranScreen
import com.pilotothegreat.deencompanion.ui.reader.ReaderScreen
import com.pilotothegreat.deencompanion.ui.reliability.ReliabilityScreen
import com.pilotothegreat.deencompanion.ui.settings.SettingsScreen
import com.pilotothegreat.deencompanion.ui.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
/**
 * App shell over the Nav3 back stack. Phones get a floating pill bar that hides while scrolling;
 * wider windows keep a navigation rail. [destination] is a screen requested by a notification or widget.
 */
@Composable
fun DeenApp(settings: AppSettings, destination: NavKey? = null, onDestinationOpened: () -> Unit = {}) {
    // First run: a single sheet over Today asking for what the app needs, rather than a tour in
    // front of it. Someone upgrading has already made these choices, and a saved location is the
    // proof: asking them again would be the update introducing itself as a stranger.
    var setup by rememberSaveable(settings.onboardingCompleted) {
        mutableStateOf(!settings.onboardingCompleted && settings.location.isDefault)
    }
    var openCityPicker by rememberSaveable { mutableStateOf(false) }
    if (setup) {
        val setupViewModel: SettingsViewModel = koinViewModel()
        fun finish(shareUsage: Boolean, withCityPicker: Boolean) {
            setupViewModel.setAnalytics(shareUsage)
            setupViewModel.completeOnboarding()
            openCityPicker = withCityPicker
            setup = false
        }
        SetupSheet(onChooseCity = { finish(shareUsage = false, withCityPicker = true) }, onDone = { finish(it, withCityPicker = false) })
    }

    // Once, on the first launch of a new version. A changelog that keeps reappearing is an advert.
    val versionCode = BuildConfig.VERSION_CODE
    var showWhatsNew by rememberSaveable(versionCode) {
        mutableStateOf(!setup && settings.lastSeenVersionCode in 1 until versionCode)
    }
    val settingsViewModel: SettingsViewModel = koinViewModel()
    LaunchedEffect(versionCode) { settingsViewModel.markVersionSeen(versionCode) }
    if (showWhatsNew) {
        WhatsNewSheet(
            version = BuildConfig.VERSION_NAME,
            notes = stringResource(R.string.whats_new_body),
            onDismiss = { showWhatsNew = false },
        )
    }
    val backStack = rememberNavBackStack(HomeKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    LaunchedEffect(destination) {
        destination?.let {
            navigator.open(it)
            onDestinationOpened()
        }
    }
    // "Choose a city" during first run lands on the picker rather than dropping someone on Today
    // beside the very prompt they just answered.
    LaunchedEffect(openCityPicker) {
        if (openCityPicker) {
            navigator.navigate(LocationKey)
            openCityPicker = false
        }
    }

    val adaptive = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val compact = adaptive == NavigationSuiteType.ShortNavigationBarCompact ||
        adaptive == NavigationSuiteType.ShortNavigationBarMedium ||
        adaptive == NavigationSuiteType.NavigationBar
    val onTopLevel = navigator.isOnTopLevel
    val floatingBar = compact && onTopLevel
    val railType = if (!compact && onTopLevel) adaptive else NavigationSuiteType.None
    val barScroll = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = FloatingToolbarExitDirection.Bottom)
    // Every screen change, including coming back to a tab, starts with the bar showing.
    LaunchedEffect(backStack.lastOrNull()) { barScroll.state.offset = 0f }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Navigation is the one place the app kept moving in Simple mode: the theme swaps the motion
    // scheme, but a transition spec is read once when the entry is declared, so it never noticed.
    val pagesDue by settingsViewModel.khatmaPagesDue.collectAsStateWithLifecycle()
    val stillMotion = LocalAccessibility.current.reduceMotion
    val tabMotion = if (stillMotion) StillTabTransition else TabTransition

    NavigationSuiteScaffold(
        navigationSuiteType = railType,
        navigationItems = {
            TopLevel.entries.forEach { tab ->
                val selected = navigator.currentTab == tab
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { navigator.selectTab(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                    navigationSuiteType = railType,
                )
            }
        },
    ) {
        CompositionLocalProvider(LocalBottomBarPadding provides if (floatingBar) FloatingBarClearance else 0.dp) {
            Box(Modifier.fillMaxSize().nestedScroll(barScroll)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = navigator::back,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = { sharedAxis(forward = true, rtl = rtl, still = stillMotion) },
                    popTransitionSpec = { sharedAxis(forward = false, rtl = rtl, still = stillMotion) },
                    predictivePopTransitionSpec = { _ -> predictiveBack(still = stillMotion) },
                    entryProvider = entryProvider {
                        entry<HomeKey>(metadata = tabMotion) {
                            HomeScreen(
                                onOpenSettings = { navigator.navigate(SettingsKey) },
                                onOpenQibla = { navigator.navigate(QiblaKey) },
                                onOpenLocation = { navigator.navigate(LocationKey) },
                                onOpenAthkar = { navigator.navigate(AthkarSessionKey(it)) },
                                onOpenReliability = { navigator.navigate(ReliabilityKey) },
                                onOpenReader = navigator::navigate,
                            )
                        }
                        entry<QuranKey>(metadata = tabMotion) { QuranScreen(onOpenReader = navigator::navigate) }
                        entry<ReaderKey> { key -> ReaderScreen(key, onBack = navigator::back) }
                        entry<AthkarKey>(metadata = tabMotion) {
                            AthkarScreen(onOpenCategory = { navigator.navigate(AthkarSessionKey(it)) })
                        }
                        entry<AthkarSessionKey> { key -> AthkarSessionScreen(key, onBack = navigator::back) }
                        entry<HadithKey>(metadata = tabMotion) {
                            HadithScreen(onOpenBook = { navigator.navigate(HadithBookKey(it)) })
                        }
                        entry<HadithBookKey> { key -> HadithBookScreen(key, onBack = navigator::back) }
                        entry<QiblaKey> { QiblaScreen(onBack = navigator::back) }
                        entry<SettingsKey> {
                            SettingsScreen(
                                settings = settings,
                                onBack = navigator::back,
                                onOpenLocation = { navigator.navigate(LocationKey) },
                                onOpenReliability = { navigator.navigate(ReliabilityKey) },
                            )
                        }
                        entry<LocationKey> { LocationPickerScreen(onBack = navigator::back) }
                        entry<ReliabilityKey> {
                            val viewModel: SettingsViewModel = koinViewModel()
                            ReliabilityScreen(canScheduleExact = viewModel.canScheduleExactAlarms(), onBack = navigator::back)
                        }
                    },
                )
                AnimatedVisibility(
                    visible = floatingBar,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    FloatingNavBar(
                        current = navigator.currentTab,
                        onSelect = navigator::selectTab,
                        scrollBehavior = barScroll,
                        badges = mapOf(TopLevel.QURAN to pagesDue),
                        modifier = Modifier.navigationBarsPadding().padding(bottom = Spacing.large),
                    )
                }
            }
        }
    }
}

/**
 * Switching tabs fades through: the old tab fades out, the new one fades and grows in.
 *
 * Nothing here reads the motion scheme, because a transition spec is read once when the entry is
 * declared. Reduce-motion is honoured by [still] instead, which the navigator swaps in wholesale —
 * the theme could switch the scheme all it liked and the app still slid and scaled its way between
 * every screen in Simple mode.
 */
private val TabTransition: Map<String, Any> = NavDisplay.transitionSpec {
    (fadeIn(tween(durationMillis = 210, delayMillis = 90)) + scaleIn(tween(durationMillis = 210, delayMillis = 90), initialScale = 0.96f)) togetherWith
        fadeOut(tween(durationMillis = 90))
}

/** The same, with the movement taken out: a plain cross-fade, short enough not to be a wait. */
private val StillTabTransition: Map<String, Any> = NavDisplay.transitionSpec {
    fadeIn(tween(durationMillis = 120)) togetherWith fadeOut(tween(durationMillis = 90))
}

/** Shared-axis slide on a spring for opening and closing screens; mirrored in right-to-left layouts. */
private fun sharedAxis(forward: Boolean, rtl: Boolean, still: Boolean): ContentTransform {
    if (still) return still()
    val direction = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
    val slide = spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    return (slideInHorizontally(slide) { direction * it / 5 } + fadeIn(tween(durationMillis = 210, delayMillis = 60))) togetherWith
        (slideOutHorizontally(slide) { -direction * it / 5 } + fadeOut(tween(durationMillis = 90)))
}

/** While swiping back, the leaving screen shrinks away and the previous one fades in. */
private fun predictiveBack(still: Boolean): ContentTransform =
    if (still) still() else fadeIn(tween(durationMillis = 200)) togetherWith (scaleOut(targetScale = 0.9f) + fadeOut())

/** No movement at all: what "reduce motion" has to mean if it is to mean anything. */
private fun still(): ContentTransform =
    fadeIn(tween(durationMillis = 120)) togetherWith fadeOut(tween(durationMillis = 90))
