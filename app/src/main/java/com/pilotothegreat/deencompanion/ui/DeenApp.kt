package com.pilotothegreat.deencompanion.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.ui.hadith.HadithBookScreen
import com.pilotothegreat.deencompanion.ui.hadith.HadithScreen
import com.pilotothegreat.deencompanion.ui.home.HomeScreen
import com.pilotothegreat.deencompanion.ui.navigation.HadithBookKey
import com.pilotothegreat.deencompanion.ui.navigation.HadithKey
import com.pilotothegreat.deencompanion.ui.navigation.HomeKey
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.QiblaKey
import com.pilotothegreat.deencompanion.ui.navigation.QuranKey
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.navigation.SettingsKey
import com.pilotothegreat.deencompanion.ui.navigation.TopLevel
import com.pilotothegreat.deencompanion.ui.qibla.QiblaScreen
import com.pilotothegreat.deencompanion.ui.quran.QuranScreen
import com.pilotothegreat.deencompanion.ui.reader.ReaderScreen
import com.pilotothegreat.deencompanion.ui.settings.SettingsScreen

/** App shell: an adaptive navigation suite (bar on phones, rail on wide screens) over the Nav3 back stack. */
@Composable
fun DeenApp(settings: AppSettings) {
    val backStack = rememberNavBackStack(HomeKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    val suiteType = if (navigator.isOnTopLevel) {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        navigationSuiteType = suiteType,
        navigationItems = {
            TopLevel.entries.forEach { tab ->
                val selected = navigator.currentTab == tab
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { navigator.selectTab(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                    navigationSuiteType = suiteType,
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = navigator::back,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeKey> {
                    HomeScreen(
                        onOpenSettings = { navigator.navigate(SettingsKey) },
                        onOpenQibla = { navigator.selectTab(TopLevel.QIBLA) },
                    )
                }
                entry<QuranKey> { QuranScreen(onOpenReader = navigator::navigate) }
                entry<ReaderKey> { key -> ReaderScreen(key, onBack = navigator::back) }
                entry<HadithKey> { HadithScreen(onOpenBook = { navigator.navigate(HadithBookKey(it)) }) }
                entry<HadithBookKey> { key -> HadithBookScreen(key, onBack = navigator::back) }
                entry<QiblaKey> { QiblaScreen() }
                entry<SettingsKey> { SettingsScreen(settings = settings, onBack = navigator::back) }
            },
        )
    }
}
