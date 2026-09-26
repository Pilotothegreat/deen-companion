package com.pilotothegreat.deencompanion.ui.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.data.analytics.Analytics
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.ui.navigation.AthkarSessionKey
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** Progress as tapped, shown before it is saved, so the number moves under the finger. */
    private val tapped = MutableStateFlow<DayProgress?>(null)
    private val writing = AtomicInteger()

    val session: StateFlow<AthkarSession?> = combine(
        // Live, so an edited list is counted as it now reads.
        athkar.libraryFlow.map { it.category(key.categoryId) },
        athkar.progress,
        settings.settings,
        tapped,
    ) { category, saved, s, mine ->
        category?.let {
            val today = LocalDate.now(s.zone)
            AthkarSession(
                category = it,
                progress = mine?.takeIf { writing.get() > 0 && it.date == today } ?: saved.on(today),
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
        val current = session.value ?: return
        val item = current.category.items.getOrNull(index) ?: return
        // Counted here first with the same rule the repository applies; its writes are serial,
        // so what it saves arrives at the same numbers.
        val before = tapped.value?.takeIf { writing.get() > 0 } ?: current.progress
        if (before.isDone(current.category, item)) return
        val updated = before.increment(current.category.id, item)
        Analytics.record(UsageEvent.ATHKAR_COUNTED)
        writing.incrementAndGet()
        tapped.value = updated
        if (updated.isDone(current.category, item)) {
            if (updated.isComplete(current.category)) Analytics.record(UsageEvent.ATHKAR_SESSION_FINISHED)
            _events.tryEmit(if (updated.isComplete(current.category)) SessionEvent.CategoryCompleted else SessionEvent.ItemCompleted(index))
        }
        viewModelScope.launch {
            try {
                athkar.increment(current.category, item, before.date)
            } finally {
                writing.decrementAndGet()
            }
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
