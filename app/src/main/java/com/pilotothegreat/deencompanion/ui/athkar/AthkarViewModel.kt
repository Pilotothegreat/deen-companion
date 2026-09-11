package com.pilotothegreat.deencompanion.ui.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarLibrary
import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
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
        combine(settings.settings, athkar.progress, athkar.streak, minutes) { s, progress, streak, _ ->
            val now = ZonedDateTime.now(s.zone)
            val today = now.toLocalDate()
            val library = athkar.library()
            val suggested = AthkarSchedule.suggest(now, DaySchedule.forDate(today, s.prayerConfig))
            AthkarHome(library, progress.on(today), streak.current(today), library.category(suggested) ?: library.core.first())
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Categories whose title or text matches the search, in Arabic or English. */
    val results: StateFlow<List<AthkarCategory>> = _query.debounce(150)
        .mapLatest { query ->
            val folded = ArabicText.normalize(query.trim())
            if (folded.isEmpty()) return@mapLatest emptyList()
            athkar.library().all.filter { category ->
                ArabicText.normalize(category.titleEnglish).contains(folded) ||
                    ArabicText.normalize(category.titleArabic).contains(folded) ||
                    category.items.any {
                        ArabicText.normalize(it.arabic).contains(folded) || ArabicText.normalize(it.translation).contains(folded)
                    }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasbihState: StateFlow<TasbihState> =
        tasbih.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasbihState())

    private val _events = MutableSharedFlow<AthkarEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AthkarEvent> = _events.asSharedFlow()

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun incrementTasbih() {
        viewModelScope.launch {
            if (tasbih.increment().roundCompleted) _events.emit(AthkarEvent.RoundCompleted)
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
