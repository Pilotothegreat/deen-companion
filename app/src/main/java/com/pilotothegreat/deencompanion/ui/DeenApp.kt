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
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.ui.athkar.AthkarScreen
import com.pilotothegreat.deencompanion.ui.athkar.AthkarSessionScreen
import com.pilotothegreat.deencompanion.ui.hadith.HadithBookScreen
import com.pilotothegreat.deencompanion.ui.hadith.HadithScreen
import com.pilotothegreat.deencompanion.ui.home.HomeScreen
import com.pilotothegreat.deencompanion.ui.location.LocationPickerScreen
import com.pilotothegreat.deencompanion.ui.navigation.AthkarKey
import com.pilotothegreat.deencompanion.ui.navigation.AthkarSessionKey
import com.pilotothegreat.deencompanion.ui.navigation.FloatingBarClearance
import com.pilotothegreat.deencompanion.ui.navigation.FloatingNavBar
import com.pilotothegreat.deencompanion.ui.navigation.HadithBookKey
import com.pilotothegreat.deencompanion.ui.navigation.HadithKey
import com.pilotothegreat.deencompanion.ui.navigation.HomeKey
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.navigation.ReliabilityKey
import com.pilotothegreat.deencompanion.ui.navigation.LocationKey
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.QiblaKey
import com.pilotothegreat.deencompanion.ui.navigation.QuranKey
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.navigation.SettingsKey
import com.pilotothegreat.deencompanion.ui.navigation.TopLevel
import com.pilotothegreat.deencompanion.ui.qibla.QiblaScreen
import com.pilotothegreat.deencompanion.ui.quran.QuranScreen
import com.pilotothegreat.deencompanion.ui.reader.ReaderScreen
import com.pilotothegreat.deencompanion.ui.reliability.ReliabilityScreen
import com.pilotothegreat.deencompanion.ui.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import com.pilotothegreat.deencompanion.ui.settings.SettingsScreen

/**
 * App shell over the Nav3 back stack. Phones get a floating pill bar that hides while scrolling;
 * wider windows keep a navigation rail. [destination] is a screen requested by a notification or widget.
 */
@Composable
fun DeenApp(settings: AppSettings, destination: NavKey? = null, onDestinationOpened: () -> Unit = {}) {
    val backStack = rememberNavBackStack(HomeKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    LaunchedEffect(destination) {
        destination?.let {
            navigator.open(it)
            onDestinationOpened()
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
                    transitionSpec = { sharedAxis(forward = true, rtl = rtl) },
                    popTransitionSpec = { sharedAxis(forward = false, rtl = rtl) },
                    predictivePopTransitionSpec = { _ -> predictiveBack() },
                    entryProvider = entryProvider {
                        entry<HomeKey>(metadata = TabTransition) {
                            HomeScreen(
                                onOpenSettings = { navigator.navigate(SettingsKey) },
                                onOpenQibla = { navigator.navigate(QiblaKey) },
                                onOpenLocation = { navigator.navigate(LocationKey) },
                                onOpenAthkar = { navigator.navigate(AthkarSessionKey(it)) },
                                onOpenReader = navigator::navigate,
                            )
                        }
                        entry<QuranKey>(metadata = TabTransition) { QuranScreen(onOpenReader = navigator::navigate) }
                        entry<ReaderKey> { key -> ReaderScreen(key, onBack = navigator::back) }
                        entry<AthkarKey>(metadata = TabTransition) {
                            AthkarScreen(onOpenCategory = { navigator.navigate(AthkarSessionKey(it)) })
                        }
                        entry<AthkarSessionKey> { key -> AthkarSessionScreen(key, onBack = navigator::back) }
                        entry<HadithKey>(metadata = TabTransition) {
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
                        modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

/** Switching tabs fades through: the old tab fades out, the new one fades and grows in. */
private val TabTransition: Map<String, Any> = NavDisplay.transitionSpec {
    (fadeIn(tween(durationMillis = 210, delayMillis = 90)) + scaleIn(tween(durationMillis = 210, delayMillis = 90), initialScale = 0.96f)) togetherWith
        fadeOut(tween(durationMillis = 90))
}

/** Shared-axis slide on a spring for opening and closing screens; mirrored in right-to-left layouts. */
private fun sharedAxis(forward: Boolean, rtl: Boolean): ContentTransform {
    val direction = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
    val slide = spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    return (slideInHorizontally(slide) { direction * it / 5 } + fadeIn(tween(durationMillis = 210, delayMillis = 60))) togetherWith
        (slideOutHorizontally(slide) { -direction * it / 5 } + fadeOut(tween(durationMillis = 90)))
}

/** While swiping back, the leaving screen shrinks away and the previous one fades in. */
private fun predictiveBack(): ContentTransform =
    fadeIn(tween(durationMillis = 200)) togetherWith (scaleOut(targetScale = 0.9f) + fadeOut())
