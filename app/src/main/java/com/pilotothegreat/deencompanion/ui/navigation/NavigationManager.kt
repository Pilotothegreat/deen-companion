package com.pilotothegreat.deencompanion.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.pilotothegreat.deencompanion.ui.hadith.Hadith
import com.pilotothegreat.deencompanion.ui.overview.Overview
import com.pilotothegreat.deencompanion.ui.quran.Quran
import com.pilotothegreat.deencompanion.ui.quran.QuranReader
import com.pilotothegreat.deencompanion.ui.settings.Settings
import com.pilotothegreat.deencompanion.ui.theme.navBarShadow
import com.pilotothegreat.deencompanion.util.TOP_BAR_HEIGHT
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, KoinExperimentalAPI::class)
@Composable
fun NavigationManager() {
    val navigator: Navigator = koinInject()
    val backStack = navigator.backStack

    var showBottomBar by remember { mutableStateOf(false) }
    val currentEntry = navigator.backStack.lastOrNull()

    LaunchedEffect(currentEntry) {
        showBottomBar = mainScreens.contains(currentEntry)
    }

    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ScreenOffset

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding + TOP_BAR_HEIGHT,
            bottom = bottomPadding + if (showBottomBar) toolbarOffset else 8.dp
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically {it} + fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = slideOutVertically {it} + fadeOut(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                ) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier.navBarShadow(),
                        expanded = true,
                        content = {
                            NavigationButton(navigator, OverviewKey, "Today", Icons.Default.Today)
                            NavigationButton(navigator, QuranKey, "Quran", Icons.Default.MenuBook)
                            NavigationButton(navigator, HadithKey, "Hadith", Icons.Default.LibraryBooks)
                        },
                    )
                }
            }
        }
    ) {
        NavDisplay(
            backStack = navigator.backStack,
            onBack = { navigator.goBack() },
            entryProvider = entryProvider {
                entry<OverviewKey> { Overview(paddingValues) }
                entry<QuranKey> { Quran(paddingValues) }
                entry<HadithKey> { Hadith(paddingValues) }
                entry<SettingsKey> { Settings(paddingValues) }
                entry<QuranReaderKey> { QuranReader(it.surahNumber, it.surahName, it.scrollToVerse, it.autoPlay) }
            },
            transitionSpec = {
                if (backStack.size == 1) fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith fadeOut(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                else {
                    slideInHorizontally { it } togetherWith
                    slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                }
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith
                slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = {
                slideInHorizontally { -it/2 } + scaleIn(initialScale = 0.7f) + fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith
                slideOutHorizontally { it }
            }
        )
    }
}

@Composable
fun NavigationButton(navigator: Navigator, route: NavKey, name: String, icon: ImageVector) {
    val selected = navigator.current == route
    val horizontalPadding by animateDpAsState(if (selected) 24.dp else 12.dp)
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            scale.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
    }

    Button (
        colors =
            if (navigator.current == route){
                ButtonDefaults.filledTonalButtonColors()
            } else {
                ButtonDefaults.textButtonColors()
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            navigator.setTo(route)
        },
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = horizontalPadding),
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        Row {
            Icon(
                imageVector = icon,
                contentDescription = route.toString()
            )
            AnimatedVisibility(navigator.current == route) {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = name
                )
            }
        }
    }
}
