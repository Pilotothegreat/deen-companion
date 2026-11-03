package com.pilotothegreat.deencompanion.ui.athkar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.navigation.AthkarEditorKey
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

/**
 * Writes one of the reader's own athkar lists: a name, then each dhikr with how many times to say
 * it. What is saved is counted in the same session screen as the bundled lists.
 */
@Composable
fun AthkarEditorScreen(
    key: AthkarEditorKey,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: AthkarEditorViewModel = koinViewModel { parametersOf(key) },
) {
    val haptics = rememberHaptics()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved -> {
                    haptics.confirm()
                    onBack()
                }
                EditorEvent.Deleted -> onDeleted()
            }
        }
    }

    // Leaving with unsaved words asks first; leaving with nothing changed just leaves.
    BackHandler(enabled = viewModel.isDirty) { confirmDiscard = true }
    val leave = { if (viewModel.isDirty) confirmDiscard = true else onBack() }

    val draft = viewModel.draft ?: return LoadingBox()
    AthkarEditorContent(
        draft = draft,
        onBack = leave,
        onSave = viewModel::save,
        onDelete = { confirmDelete = true },
        onTitle = viewModel::setTitle,
        onText = viewModel::setText,
        onNote = viewModel::setNote,
        onCount = { index, count ->
            haptics.tick()
            viewModel.setCount(index, count)
        },
        onMove = { index, by ->
            haptics.tick()
            viewModel.move(index, by)
        },
        onRemove = { index ->
            haptics.reject()
            viewModel.remove(index)
        },
        onAdd = {
            haptics.click()
            viewModel.add()
        },
    )

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.athkar_discard_title)) },
            text = { Text(stringResource(R.string.athkar_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.athkar_discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.athkar_keep_editing)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.athkar_delete_list)) },
            text = { Text(stringResource(R.string.athkar_delete_list_confirm, draft.title.trim())) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    haptics.reject()
                    viewModel.delete()
                }) { Text(stringResource(R.string.athkar_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** The editor without its view model, so it can be rendered and looked at in a test. */
@Composable
internal fun AthkarEditorContent(
    draft: AthkarDraft,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTitle: (String) -> Unit,
    onText: (Int, String) -> Unit,
    onNote: (Int, String) -> Unit,
    onCount: (Int, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val locale = currentLocale()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(if (draft.isNew) R.string.athkar_new_list else R.string.athkar_edit_list)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    if (!draft.isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.athkar_delete_list))
                        }
                    }
                    Button(
                        onClick = onSave,
                        enabled = draft.canSave,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = Spacing.small),
                    ) { Text(stringResource(R.string.save)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.small, bottom = Spacing.xxlarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            item(key = "title") {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitle,
                    label = { Text(stringResource(R.string.athkar_list_title)) },
                    placeholder = { Text(stringResource(R.string.athkar_list_title_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(draft.items, key = { _, item -> item.id }) { index, item ->
                DhikrCard(
                    number = index + 1,
                    item = item,
                    locale = locale,
                    first = index == 0,
                    last = index == draft.items.lastIndex,
                    onText = { onText(index, it) },
                    onNote = { onNote(index, it) },
                    onCount = { onCount(index, it) },
                    onMove = { by -> onMove(index, by) },
                    onRemove = { onRemove(index) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                FilledTonalButton(
                    onClick = onAdd,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().animateItem(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.small))
                    Text(stringResource(R.string.athkar_add_dhikr))
                }
            }
        }
    }
}

@Composable
private fun DhikrCard(
    number: Int,
    item: DraftDhikr,
    locale: Locale,
    first: Boolean,
    last: Boolean,
    onText: (String) -> Unit,
    onNote: (String) -> Unit,
    onCount: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.athkar_dhikr_number, Formatters.number(number, locale)),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = { onMove(-1) }, enabled = !first) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.athkar_move_up))
                }
                IconButton(onClick = { onMove(1) }, enabled = !last) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.athkar_move_down))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.athkar_remove_dhikr))
                }
            }
            // Written in Arabic most of the time, so it is set the way the session will show it.
            OutlinedTextField(
                value = item.text,
                onValueChange = onText,
                label = { Text(stringResource(R.string.athkar_dhikr_text)) },
                textStyle = TextStyle(
                    fontFamily = Amiri,
                    fontSize = 22.sp,
                    lineHeight = 40.sp,
                    textDirection = TextDirection.Content,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                minLines = 2,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = item.note,
                onValueChange = onNote,
                label = { Text(stringResource(R.string.athkar_dhikr_note)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            CountStepper(item.count, locale, onCount)
        }
    }
}

/** How many times: a number that can be typed, with a step either side of it. */
@Composable
private fun CountStepper(count: Int, locale: Locale, onCount: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            stringResource(R.string.athkar_dhikr_repeat),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(onClick = { onCount(count - 1) }, enabled = count > 1) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease))
        }
        // Its own text, so the field can be emptied on the way to a new number; a count arriving
        // from the buttons replaces it.
        var typed by rememberSaveable(count) { mutableStateOf(Formatters.number(count, locale)) }
        OutlinedTextField(
            value = typed,
            onValueChange = { value ->
                // Digits in either script.
                val digits = value.mapNotNull { it.digitToIntOrNull() }
                typed = value.filter { it.isDigit() }.take(3)
                val number = digits.take(3).fold(0) { n, d -> n * 10 + d }
                if (number >= 1) onCount(number)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.width(88.dp),
        )
        FilledTonalIconButton(onClick = { onCount(count + 1) }, enabled = count < AthkarRepository.MAX_COUNT) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase))
        }
    }
}
