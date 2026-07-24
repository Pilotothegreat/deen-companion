@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.pilotothegreat.deencompanion.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pilotothegreat.deencompanion.R
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
import com.pilotothegreat.deencompanion.ui.hadith.HadithBookReader
import com.pilotothegreat.deencompanion.ui.overview.Overview
import com.pilotothegreat.deencompanion.ui.qibla.Qibla
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

    val motionScheme = MaterialTheme.motionScheme
    val effectsSpec = remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }

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
                    enter = slideInVertically {it} + fadeIn(effectsSpec),
                    exit = slideOutVertically {it} + fadeOut(effectsSpec)
                ) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier.navBarShadow(),
                        expanded = true,
                        content = {
                            // M3 Expressive: ButtonGroup provides connected shape morphing
                            // on selection automatically via ButtonGroupDefaults.
                            ButtonGroup {
                                NavigationButton(
                                    navigator = navigator,
                                    route = OverviewKey,
                                    name = stringResource(R.string.today),
                                    icon = Icons.Default.Today,
                                    buttonShapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                )
                                NavigationButton(
                                    navigator = navigator,
                                    route = QuranKey,
                                    name = stringResource(R.string.quran),
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    buttonShapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                                )
                                NavigationButton(
                                    navigator = navigator,
                                    route = HadithKey,
                                    name = stringResource(R.string.hadith),
                                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                                    buttonShapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                )
                            }
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
                entry<HadithReaderKey> { HadithBookReader(it.bookId) }
                entry<QiblaKey> { Qibla() }
            },
            transitionSpec = {
                if (backStack.size == 1) fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
                else {
                    slideInHorizontally { it } togetherWith
                    slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                }
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn(effectsSpec) togetherWith
                slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it / 3 } +
                 scaleIn(initialScale = 0.88f, animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                 fadeIn(effectsSpec)) togetherWith
                (slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it } +
                 scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                 fadeOut(effectsSpec))
            }
        )
    }
}

// M3 Expressive: ToggleButton inside a ButtonGroup.
// Shapes are provided by the caller via ButtonGroupDefaults.connectedLeading/Middle/TrailingButtonShapes()
// so there is no need for manual RoundedCornerShape animation.
@Composable
fun NavigationButton(
    navigator: Navigator,
    route: NavKey,
    name: String,
    icon: ImageVector,
    buttonShapes: ToggleButtonShapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
) {
    val selected = navigator.current == route
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }

    // Spring bounce on selection for tactile emphasis (SKILL.md: motion must communicate state)
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(selected) {
        if (selected) {
            scale.animateTo(targetValue = 1.12f, animationSpec = spatialSpec)
            scale.animateTo(targetValue = 1f, animationSpec = spatialSpec)
        }
    }

    ToggleButton(
        checked = selected,
        onCheckedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!selected) {
                if (route == OverviewKey) {
                    navigator.setTo(OverviewKey)
                } else {
                    androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                        navigator.backStack.clear()
                        navigator.backStack.add(OverviewKey)
                        navigator.backStack.add(route)
                    }
                }
            }
        },
        shapes = buttonShapes,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = if (selected) 20.dp else 12.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        Row {
            BadgedBox(
                badge = {
                    if (route == HadithKey) {
                        Badge(containerColor = MaterialTheme.colorScheme.error)
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = route.toString()
                )
            }
            // Slide the label in/out when selected — expressive label reveal
            AnimatedVisibility(selected) {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = name
                )
            }
        }
    }
}
