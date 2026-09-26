package com.pilotothegreat.deencompanion.ui.athkar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.athkar.AthkarItem
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.ui.navigation.AthkarEditorKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/** One dhikr as it is being written; [id] survives edits so today's count stays with it. */
data class DraftDhikr(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val note: String = "",
    val count: Int = 1,
)

data class AthkarDraft(val id: String, val title: String, val items: List<DraftDhikr>, val isNew: Boolean) {

    /** A list needs a name and something in it to be worth keeping. */
    val canSave: Boolean get() = title.isNotBlank() && items.any { it.text.isNotBlank() }

    fun toCategory(): AthkarCategory = AthkarCategory(
        id = id,
        titleEnglish = title.trim(),
        titleArabic = title.trim(),
        items = items.filter { it.text.isNotBlank() }.map {
            AthkarItem(
                id = it.id,
                arabic = it.text.trim(),
                translation = "",
                transliteration = "",
                count = it.count.coerceIn(1, AthkarRepository.MAX_COUNT),
                noteEnglish = it.note.trim(),
                noteArabic = it.note.trim(),
            )
        },
    )

    companion object {
        fun blank() = AthkarDraft(AthkarIds.newCustom(), "", listOf(DraftDhikr()), isNew = true)

        fun of(category: AthkarCategory) = AthkarDraft(
            id = category.id,
            title = category.titleArabic,
            items = category.items.map { DraftDhikr(it.id, it.arabic, it.noteArabic, it.count) },
            isNew = false,
        )
    }
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
}

/**
 * The draft is Compose state rather than a flow: text fields fed from a flow lose the cursor when
 * an update arrives a frame late, and a draft is only ever read by the one screen writing it.
 */
class AthkarEditorViewModel(
    key: AthkarEditorKey,
    private val athkar: AthkarRepository,
) : ViewModel() {

    var draft by mutableStateOf<AthkarDraft?>(null)
        private set

    private var original: AthkarDraft? = null

    val isDirty: Boolean get() = draft != null && draft != original

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val existing = key.categoryId?.let { id -> athkar.custom.first().firstOrNull { it.id == id } }
            val loaded = existing?.let(AthkarDraft::of) ?: AthkarDraft.blank()
            original = loaded
            draft = loaded
        }
    }

    fun setTitle(title: String) = edit { copy(title = title) }

    fun setText(index: Int, text: String) = editItem(index) { copy(text = text) }

    fun setNote(index: Int, note: String) = editItem(index) { copy(note = note) }

    fun setCount(index: Int, count: Int) = editItem(index) { copy(count = count.coerceIn(1, AthkarRepository.MAX_COUNT)) }

    fun add() = edit { copy(items = items + DraftDhikr()) }

    fun remove(index: Int) = edit { copy(items = items.filterIndexed { i, _ -> i != index }.ifEmpty { listOf(DraftDhikr()) }) }

    /** Moves the dhikr at [index] one place up ([by] = -1) or down ([by] = 1). */
    fun move(index: Int, by: Int) = edit {
        val to = index + by
        if (to !in items.indices) return@edit this
        copy(items = items.toMutableList().apply { add(to, removeAt(index)) })
    }

    fun save() {
        val current = draft?.takeIf { it.canSave } ?: return
        viewModelScope.launch {
            athkar.saveCustom(current.toCategory())
            original = current
            _events.emit(EditorEvent.Saved)
        }
    }

    fun delete() {
        val current = draft ?: return
        viewModelScope.launch {
            if (!current.isNew) athkar.deleteCustom(current.id)
            _events.emit(EditorEvent.Deleted)
        }
    }

    private inline fun edit(change: AthkarDraft.() -> AthkarDraft) {
        draft = draft?.change()
    }

    private inline fun editItem(index: Int, crossinline change: DraftDhikr.() -> DraftDhikr) = edit {
        copy(items = items.mapIndexed { i, item -> if (i == index) item.change() else item })
    }
}
