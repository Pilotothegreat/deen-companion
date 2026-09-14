package com.pilotothegreat.deencompanion.ui.athkar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarItem
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.navigation.AthkarSessionKey
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import com.pilotothegreat.deencompanion.ui.theme.animatedPolygonShape
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

/** One athkar list: a page per dhikr, counted with a big button or by tapping the card. */
@Composable
fun AthkarSessionScreen(
    key: AthkarSessionKey,
    onBack: () -> Unit,
    viewModel: AthkarSessionViewModel = koinViewModel { parametersOf(key) },
) {
    val state by viewModel.session.collectAsStateWithLifecycle()
    val session = state ?: return LoadingBox()
    val locale = currentLocale()
    val haptics = rememberHaptics()
    val reducedMotion = rememberReducedMotion()
    val category = session.category
    val items = category.items
    // The meaning and the transliteration belong to the session, not to a settings screen: someone
    // reading in Arabic wants neither, everyone else usually wants both, and either way the answer
    // is one tap away in the bar above.
    var showTranslation by rememberSaveable(locale) { mutableStateOf(!locale.isArabic) }
    var showTransliteration by rememberSaveable(locale) { mutableStateOf(!locale.isArabic) }
    // Open where the session was left if it was left part-way, and otherwise at the first item not
    // finished today; the extra last page is the finish screen.
    val firstOpen = remember(category.id) {
        session.resumeAt?.takeIf { !session.progress.isDone(category, items[it]) }
            ?: items.indexOfFirst { !session.progress.isDone(category, it) }.takeIf { it >= 0 }
            ?: items.size
    }
    val pager = rememberPagerState(initialPage = firstOpen) { items.size + 1 }
    LaunchedEffect(pager, category.id) {
        snapshotFlow { pager.settledPage }.collect { if (it < items.size) viewModel.onPage(it) }
    }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            haptics.confirm()
            delay(if (reducedMotion) 150 else 450)
            val next = when (event) {
                is SessionEvent.ItemCompleted -> event.index + 1
                SessionEvent.CategoryCompleted -> items.size
            }
            // Only advance if the reader is still on the item that was just finished.
            if (event !is SessionEvent.ItemCompleted || pager.currentPage == event.index) {
                pager.animateScrollToPage(next, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.title(locale)) },
                subtitle = {
                    Text(
                        stringResource(
                            R.string.athkar_progress,
                            Formatters.number(session.progress.completedItems(category), locale),
                            Formatters.number(items.size, locale),
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    IconToggleButton(checked = showTranslation, onCheckedChange = { showTranslation = it }) {
                        Icon(Icons.Rounded.Translate, contentDescription = stringResource(R.string.athkar_show_translation))
                    }
                    IconToggleButton(checked = showTransliteration, onCheckedChange = { showTransliteration = it }) {
                        Icon(Icons.Rounded.Abc, contentDescription = stringResource(R.string.athkar_show_transliteration))
                    }
                    IconButton(onClick = { confirmReset = true }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.athkar_reset))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val fraction by animateFloatAsState(session.progress.fraction(category), MaterialTheme.motionScheme.slowSpatialSpec(), label = "session")
            LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large, vertical = Spacing.small))
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Spacing.large),
                pageSpacing = 12.dp,
            ) { page ->
                if (page == items.size) {
                    FinishPage(reducedMotion, onBack)
                } else {
                    DhikrPage(
                        item = items[page],
                        count = session.progress.count(category.id, items[page].id),
                        session = session,
                        showTranslation = showTranslation,
                        showTransliteration = showTransliteration,
                        locale = locale,
                        onCount = {
                            haptics.tick()
                            viewModel.count(page)
                        },
                    )
                }
            }
            val page = pager.currentPage
            if (page < items.size) {
                val item = items[page]
                CounterButton(
                    count = session.progress.count(category.id, item.id),
                    target = item.count,
                    locale = locale,
                    onCount = {
                        haptics.tick()
                        viewModel.count(page)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(vertical = Spacing.large),
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.athkar_reset)) },
            text = { Text(stringResource(R.string.athkar_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    confirmReset = false
                }) { Text(stringResource(R.string.athkar_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DhikrPage(
    item: AthkarItem,
    count: Int,
    session: AthkarSession,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    locale: Locale,
    onCount: () -> Unit,
) {
    val done = count >= item.count
    val countLabel = stringResource(R.string.athkar_count_action)
    var showDetails by rememberSaveable(item.id) { mutableStateOf(false) }
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onCount)
            .semantics { onClick(label = countLabel) { onCount(); true } },
    ) {
        // Short athkar sit in the middle of the card; long ones scroll.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.xlarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                item.note(locale)?.let { note ->
                    Text(note, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    item.arabic,
                    fontFamily = Amiri,
                    fontSize = ARABIC_SP.sp,
                    lineHeight = (ARABIC_SP * 1.9f).sp,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // English stays left-to-right even inside the Arabic UI.
                if (showTransliteration && item.transliteration.isNotBlank()) {
                    Text(
                        item.transliteration,
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showTranslation && item.translation.isNotBlank()) {
                    Text(
                        item.translation,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val virtue = item.virtue(locale)
                val source = item.source(locale)
                if (virtue != null || source != null) {
                    TextButton(onClick = { showDetails = !showDetails }) { Text(stringResource(R.string.athkar_virtue_source)) }
                    AnimatedVisibility(visible = showDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            virtue?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            source?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Big thumb-reach counter; its shape squishes on press and becomes a check when the dhikr is done. */
@Composable
private fun CounterButton(count: Int, target: Int, locale: Locale, onCount: () -> Unit, modifier: Modifier = Modifier) {
    val done = count >= target
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = animatedPolygonShape(
        target = when {
            done -> MaterialShapes.SoftBurst
            pressed -> MaterialShapes.Cookie9Sided
            else -> MaterialShapes.Circle
        },
        spec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val progress by animateFloatAsState(count / target.toFloat(), MaterialTheme.motionScheme.defaultSpatialSpec(), label = "counter")
    val countLabel = stringResource(R.string.athkar_count_action)
    val countState = stringResource(R.string.athkar_progress, Formatters.number(count, locale), Formatters.number(target, locale))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(shape)
                    .background(if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    .clickable(interactionSource = interaction, indication = ripple(), enabled = !done, onClick = onCount)
                    .semantics {
                        contentDescription = countLabel
                        stateDescription = countState
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(targetState = done, label = "counterContent") { finished ->
                    if (finished) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(44.dp))
                    } else {
                        // Counts down the repetitions left, like beads still to be passed.
                        Text(
                            Formatters.number(target - count, locale),
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        Text(
            stringResource(if (done) R.string.athkar_done else R.string.athkar_left),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FinishPage(reducedMotion: Boolean, onBack: () -> Unit) {
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.3f) }
    val rotation = remember { Animatable(if (reducedMotion) 0f else -45f) }
    LaunchedEffect(Unit) {
        if (reducedMotion) return@LaunchedEffect
        coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            launch { rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)) }
        }
    }
    Column(
        Modifier.fillMaxSize().padding(Spacing.xxlarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xlarge, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(160.dp).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().clip(MaterialShapes.SoftBurst.toShape()).background(MaterialTheme.colorScheme.primaryContainer))
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(64.dp))
        }
        Text(
            stringResource(R.string.athkar_finished),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        FilledTonalButton(onClick = onBack) { Text(stringResource(R.string.athkar_back)) }
    }
}

/** The Arabic of a dhikr, in sp so it follows the phone's text size like everything else in the app. */
private const val ARABIC_SP = 28
