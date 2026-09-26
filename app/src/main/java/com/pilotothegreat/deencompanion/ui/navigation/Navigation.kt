package com.pilotothegreat.deencompanion.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.data.analytics.Analytics
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.common.AthkarIcon
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object QuranKey : NavKey
@Serializable data object HadithKey : NavKey
@Serializable data object QiblaKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data object LocationKey : NavKey
@Serializable data object ReliabilityKey : NavKey
@Serializable data object AthkarKey : NavKey

/** One athkar category, counted item by item. */
@Serializable data class AthkarSessionKey(val categoryId: String) : NavKey

/** Writes a list of the reader's own athkar: a new one when [categoryId] is null. */
@Serializable data class AthkarEditorKey(val categoryId: String? = null) : NavKey

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
    ATHKAR(AthkarKey, R.string.athkar, AthkarIcon, AthkarIcon),
    HADITH(HadithKey, R.string.hadith, Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Rounded.LibraryBooks),
}

/**
 * Back stack rules: each tab keeps a stack of its own, so leaving a tab and coming back finds it as
 * it was left: the same screen, scrolled to the same place. Home is always the root under the tab
 * on screen, so back from a tab's first screen returns Home.
 *
 * Until 2.2 there was one stack, cleared on every tab switch, and each tab started over each time.
 */
class Navigator(
    private val stacks: Map<TopLevel, NavBackStack<NavKey>>,
    private val tab: MutableState<TopLevel>,
) {

    /**
     * Which screens were opened, counted in the one place every screen is opened from.
     *
     * Instrumenting each screen instead would mean twenty call sites, nineteen of which stay right
     * and one of which is forgotten the next time a screen is added.
     */
    private fun countScreen(key: NavKey) {
        val event = when (key) {
            HomeKey -> UsageEvent.SCREEN_TODAY
            QuranKey -> UsageEvent.SCREEN_QURAN
            HadithKey -> UsageEvent.SCREEN_HADITH
            AthkarKey -> UsageEvent.SCREEN_ATHKAR
            QiblaKey -> UsageEvent.SCREEN_QIBLA
            SettingsKey -> UsageEvent.SCREEN_SETTINGS
            is ReaderKey -> UsageEvent.SCREEN_READER
            is AthkarSessionKey -> UsageEvent.ATHKAR_SESSION_STARTED
            else -> return
        }
        Analytics.record(event)
    }

    val currentTab: TopLevel get() = tab.value

    private val stack: NavBackStack<NavKey> get() = stacks.getValue(tab.value)

    /** What is on screen, bottom to top: Home, then the current tab's own stack above it. */
    val visible: List<NavKey> get() = if (tab.value == TopLevel.HOME) stack else listOf(HomeKey) + stack

    val isOnTopLevel: Boolean get() = stack.size == 1

    /** Switches to [tab] as it was left; choosing the tab already shown returns it to its first screen. */
    fun selectTab(tab: TopLevel) {
        if (tab == this.tab.value) {
            popToRoot(stack)
            return
        }
        countScreen(stacks.getValue(tab).last())
        this.tab.value = tab
    }

    fun navigate(key: NavKey) {
        countScreen(key)
        stack.add(key)
    }

    /** Opens [key] fresh on top of the tab it belongs to, e.g. from a notification or widget. */
    fun open(key: NavKey) {
        val target = when (key) {
            is AthkarSessionKey, is AthkarEditorKey -> TopLevel.ATHKAR
            is ReaderKey -> TopLevel.QURAN
            else -> TopLevel.entries.firstOrNull { it.key == key } ?: TopLevel.HOME
        }
        tab.value = target
        popToRoot(stack)
        if (TopLevel.entries.none { it.key == key }) navigate(key) else countScreen(key)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else if (tab.value != TopLevel.HOME) tab.value = TopLevel.HOME
    }

    private fun popToRoot(stack: NavBackStack<NavKey>) {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
