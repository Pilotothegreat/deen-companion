package com.pilotothegreat.deencompanion.ui.athkar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarLibrary
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.EmptyState
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.components.SearchField
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.theme.pressScale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun AthkarScreen(onOpenCategory: (String) -> Unit, viewModel: AthkarViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val tasbih by viewModel.tasbihState.collectAsStateWithLifecycle()
    val locale = currentLocale()
    val haptics = rememberHaptics()
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AthkarEvent.RoundCompleted -> haptics.confirm()
                is AthkarEvent.TasbihReset -> scope.launch {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.tasbih_reset_done),
                        actionLabel = resources.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTasbih(event.previous)
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeFlexibleTopAppBar(title = { Text(stringResource(R.string.athkar)) }, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = LocalBottomBarPadding.current)) },
    ) { padding ->
        val home = state ?: run {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.small, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            item(key = "search") {
                SearchField(query, viewModel::onQueryChange, stringResource(R.string.search_athkar_hint))
            }
            if (query.isNotBlank()) {
                item(key = "results") {
                    if (results.isEmpty()) {
                        EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.athkar_no_results))
                    } else {
                        CategoryList(results, home.library, home.progress, locale, onOpenCategory)
                    }
                }
                return@LazyColumn
            }
            item(key = "now") {
                NowCard(home.suggested, home.progress, home.streak, locale, onOpen = { onOpenCategory(home.suggested.id) })
            }
            item(key = "core") { CoreGrid(home.library.core, home.progress, locale, onOpenCategory) }
            item(key = "tasbih") {
                TasbihCard(
                    state = tasbih,
                    locale = locale,
                    onTap = {
                        haptics.tick()
                        viewModel.incrementTasbih()
                    },
                    onReset = viewModel::resetTasbih,
                    onTargetChange = viewModel::setTasbihTarget,
                    onDhikrChange = viewModel::setDhikr,
                )
            }
            home.library.groups.forEach { group ->
                item(key = "group-${group.id}") {
                    Column {
                        SectionHeader(group.title(locale), Modifier.padding(start = Spacing.hair).semantics { heading() })
                        CategoryList(group.categories, home.library, home.progress, locale, onOpenCategory)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowCard(category: AthkarCategory, progress: DayProgress, streak: Int, locale: Locale, onOpen: () -> Unit) {
    val fraction by animateFloatAsState(progress.fraction(category), MaterialTheme.motionScheme.slowSpatialSpec(), label = "now")
    val done = progress.isComplete(category)
    val started = progress.fraction(category) > 0f
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onOpen,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().pressScale(interaction),
    ) {
        Row(Modifier.padding(Spacing.xxlarge), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.athkar_now), style = MaterialTheme.typography.labelLarge)
                Text(category.title(locale), style = MaterialTheme.typography.headlineMediumEmphasized)
                Text(
                    if (done) {
                        stringResource(R.string.athkar_done_today)
                    } else {
                        stringResource(
                            R.string.athkar_progress,
                            Formatters.number(progress.completedItems(category), locale),
                            Formatters.number(category.items.size, locale),
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (streak > 0) StreakPill(streak, locale)
                Spacer(Modifier.size(4.dp))
                Button(onClick = onOpen) {
                    Text(
                        stringResource(
                            when {
                                done -> R.string.athkar_again
                                started -> R.string.athkar_continue
                                else -> R.string.athkar_start
                            },
                        ),
                    )
                }
            }
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                )
                Icon(athkarIcon(category.id), contentDescription = null, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun StreakPill(days: Int, locale: Locale) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = Spacing.hair),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.hair),
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(pluralStringResource(R.plurals.athkar_streak, days, Formatters.number(days, locale)), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Two tiles per row; an odd last tile takes the full width. */
@Composable
private fun CoreGrid(categories: List<AthkarCategory>, progress: DayProgress, locale: Locale, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        categories.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                row.forEach { category -> CoreTile(category, progress, locale, { onOpen(category.id) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CoreTile(category: AthkarCategory, progress: DayProgress, locale: Locale, onClick: () -> Unit, modifier: Modifier) {
    val (container, content) = athkarColors(category.id, MaterialTheme.colorScheme)
    val fraction by animateFloatAsState(progress.fraction(category), MaterialTheme.motionScheme.slowSpatialSpec(), label = "tile")
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.pressScale(interaction),
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = athkarShape(category.id).toShape(),
                    color = content,
                    contentColor = container,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(athkarIcon(category.id), contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (progress.isComplete(category)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.athkar_done))
                }
            }
            Text(category.title(locale), style = MaterialTheme.typography.titleMediumEmphasized)
            // The default track matches some tile colors, so it's tinted from the tile's content instead.
            LinearWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = content,
                trackColor = content.copy(alpha = 0.2f),
            )
            Text(
                stringResource(
                    R.string.athkar_progress,
                    Formatters.number(progress.completedItems(category), locale),
                    Formatters.number(category.items.size, locale),
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<AthkarCategory>,
    library: AthkarLibrary,
    progress: DayProgress,
    locale: Locale,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        categories.forEachIndexed { index, category ->
            val group = library.groups.firstOrNull { g -> g.categories.any { it.id == category.id } }
            SegmentedListItem(
                onClick = { onOpen(category.id) },
                shapes = ListItemDefaults.segmentedShapes(index, categories.size),
                leadingContent = { Icon(athkarIcon(category.id, group?.id), contentDescription = null) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.athkar_items, category.items.size, Formatters.number(category.items.size, locale)))
                },
                trailingContent = {
                    if (progress.isComplete(category)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.athkar_done))
                    }
                },
            ) { Text(category.title(locale)) }
        }
    }
}
