package com.pilotothegreat.deencompanion.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.pilotothegreat.deencompanion.R
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object QuranKey : NavKey
@Serializable data object HadithKey : NavKey
@Serializable data object QiblaKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data object LocationKey : NavKey

/** Opens the mushaf at [page]; [surah]/[ayah] (when non-zero) mark the ayah to highlight. */
@Serializable data class ReaderKey(val page: Int, val surah: Int = 0, val ayah: Int = 0) : NavKey

@Serializable data class HadithBookKey(val bookId: String) : NavKey

enum class TopLevel(
    val key: NavKey,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(HomeKey, R.string.today, Icons.Outlined.WbSunny, Icons.Rounded.WbSunny),
    QURAN(QuranKey, R.string.quran, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Rounded.MenuBook),
    HADITH(HadithKey, R.string.hadith, Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Rounded.LibraryBooks),
    QIBLA(QiblaKey, R.string.qibla_compass, Icons.Outlined.Explore, Icons.Rounded.Explore),
}

/**
 * Back stack rules: Home is always the root, each other tab sits directly on top of it, and
 * detail screens stack above their tab. Back from a tab therefore returns Home.
 */
class Navigator(val backStack: NavBackStack<NavKey>) {

    val currentTab: TopLevel
        get() = backStack.lastOrNull { key -> TopLevel.entries.any { it.key == key } }
            ?.let { key -> TopLevel.entries.first { it.key == key } }
            ?: TopLevel.HOME

    val isOnTopLevel: Boolean
        get() = TopLevel.entries.any { it.key == backStack.lastOrNull() }

    fun selectTab(tab: TopLevel) {
        if (backStack.lastOrNull() == tab.key) return
        backStack.clear()
        backStack.add(HomeKey)
        if (tab != TopLevel.HOME) backStack.add(tab.key)
    }

    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
