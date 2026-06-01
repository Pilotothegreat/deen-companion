package com.pilotothegreat.deencompanion.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Main screens
 */

@Serializable
data object OverviewKey : NavKey

@Serializable
data object QuranKey : NavKey

@Serializable
data object HadithKey : NavKey

@Serializable
data object SettingsKey : NavKey

/**
 * Details
 */
@Serializable
data class QuranReaderKey(val surahNumber: Int, val surahName: String, val scrollToVerse: Int? = null, val autoPlay: Boolean = false) : NavKey

@Serializable
data object QiblaKey : NavKey

@Serializable
data object AssistantKey : NavKey

val mainScreens = listOf(OverviewKey, QuranKey, HadithKey)

