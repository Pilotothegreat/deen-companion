package com.pilotothegreat.deencompanion.ui.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarLibrary
import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.moment.MomentRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.tasbih.TasbihRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

data class AthkarHome(
    val library: AthkarLibrary,
    val progress: DayProgress,
    val streak: Int,
    /** The category that fits the time of day. */
    val suggested: AthkarCategory,
)

sealed interface AthkarEvent {
    data object RoundCompleted : AthkarEvent
    data class TasbihReset(val previous: TasbihState) : AthkarEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AthkarViewModel(
    private val athkar: AthkarRepository,
    settings: SettingsRepository,
    private val moments: MomentRepository,
    private val tasbih: TasbihRepository,
) : ViewModel() {

    /** Re-evaluates the suggestion as the time of day moves on. */
    private val minutes = flow {
        while (true) {
            emit(Unit)
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }

    val state: StateFlow<AthkarHome?> =
        combine(settings.settings, athkar.progress, athkar.streak, moments.suggestedAthkar, athkar.libraryFlow) { s, progress, streak, suggested, library ->
            val now = ZonedDateTime.now(s.zone)
            val today = now.toLocalDate()
            AthkarHome(library, progress.on(today), streak.current(today), library.category(suggested) ?: library.core.first())
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Categories whose title or text matches the search, in Arabic or English. */
    val results: StateFlow<List<AthkarCategory>> = combine(_query.debounce(150), athkar.libraryFlow, ::Pair)
        .mapLatest { (query, library) ->
            val folded = ArabicText.normalize(query.trim())
            if (folded.isEmpty()) return@mapLatest emptyList()
            library.all.filter { category ->
                ArabicText.normalize(category.titleEnglish).contains(folded) ||
                    ArabicText.normalize(category.titleArabic).contains(folded) ||
                    category.items.any {
                        ArabicText.normalize(it.arabic).contains(folded) || ArabicText.normalize(it.translation).contains(folded)
                    }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The count as tapped, shown before it is saved. A tap used to wait for the write to disk to
     * come back before the number moved, which is a beat behind the haptic under the finger.
     */
    private val tapped = MutableStateFlow<TasbihState?>(null)
    private val writing = AtomicInteger()

    val tasbihState: StateFlow<TasbihState> =
        combine(tasbih.state, tapped) { saved, mine -> if (writing.get() > 0 && mine != null) mine else saved }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasbihState())

    private val _events = MutableSharedFlow<AthkarEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AthkarEvent> = _events.asSharedFlow()

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun incrementTasbih() {
        // The same rules the repository applies, run here first; its writes are serial, so the
        // saved count arrives at the same number.
        // Two taps inside one frame must not both start from the same number.
        val base = tapped.value?.takeIf { writing.get() > 0 } ?: tasbihState.value
        val step = TasbihEngine.increment(base)
        writing.incrementAndGet()
        tapped.value = step.state
        if (step.roundCompleted) _events.tryEmit(AthkarEvent.RoundCompleted)
        viewModelScope.launch {
            try {
                tasbih.increment()
            } finally {
                writing.decrementAndGet()
            }
        }
    }

    fun resetTasbih() {
        viewModelScope.launch {
            val previous = tasbih.state.first()
            tasbih.reset()
            if (previous.count > 0) _events.emit(AthkarEvent.TasbihReset(previous))
        }
    }

    fun restoreTasbih(state: TasbihState) {
        viewModelScope.launch { tasbih.restore(state) }
    }

    fun setTasbihTarget(target: Int) {
        viewModelScope.launch { tasbih.setTarget(target) }
    }

    fun setDhikr(dhikr: Dhikr) {
        viewModelScope.launch { tasbih.setDhikr(dhikr) }
    }
}
