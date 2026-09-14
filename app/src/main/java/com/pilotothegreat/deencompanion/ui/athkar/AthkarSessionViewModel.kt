package com.pilotothegreat.deencompanion.ui.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.ui.navigation.AthkarSessionKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AthkarSession(
    val category: AthkarCategory,
    val progress: DayProgress,
    /** The dhikr this session was last left on, or null if it was never left part-way. */
    val resumeAt: Int?,
)

sealed interface SessionEvent {
    /** The item at [index] just reached its count. */
    data class ItemCompleted(val index: Int) : SessionEvent
    data object CategoryCompleted : SessionEvent
}

class AthkarSessionViewModel(
    private val key: AthkarSessionKey,
    private val athkar: AthkarRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val session: StateFlow<AthkarSession?> = combine(
        flow { emit(athkar.library().category(key.categoryId)) },
        athkar.progress,
        settings.settings,
    ) { category, progress, s ->
        category?.let {
            AthkarSession(
                category = it,
                progress = progress.on(LocalDate.now(s.zone)),
                resumeAt = s.athkarPlace.takeIf { place -> place.substringBefore(':') == key.categoryId }
                    ?.substringAfter(':')
                    ?.toIntOrNull()
                    ?.takeIf { index -> index in it.items.indices },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun count(index: Int) {
        viewModelScope.launch {
            val current = session.value ?: return@launch
            val item = current.category.items.getOrNull(index) ?: return@launch
            val updated = athkar.increment(current.category, item, today()) ?: return@launch
            if (!updated.isDone(current.category, item)) return@launch
            _events.emit(if (updated.isComplete(current.category)) SessionEvent.CategoryCompleted else SessionEvent.ItemCompleted(index))
        }
    }

    fun reset() {
        viewModelScope.launch { athkar.reset(key.categoryId, today()) }
    }

    /** Remembers the dhikr on screen, so closing the app mid-session does not lose the place. */
    fun onPage(index: Int) {
        viewModelScope.launch { settings.setAthkarPlace(key.categoryId, index) }
    }



    private suspend fun today(): LocalDate = LocalDate.now(settings.current().zone)
}
