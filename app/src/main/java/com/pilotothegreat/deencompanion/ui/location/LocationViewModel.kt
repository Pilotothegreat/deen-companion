package com.pilotothegreat.deencompanion.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.data.location.City
import com.pilotothegreat.deencompanion.data.location.CityIndex
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LocationEvent {
    data object Done : LocationEvent
    data object Unavailable : LocationEvent
    data object PermissionMissing : LocationEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LocationViewModel(
    repository: SettingsRepository,
    private val location: LocationRepository,
    private val cities: CityIndex,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Search results, or the biggest cities in the current country while the query is empty. */
    val results: StateFlow<List<City>?> = combine(
        _query.debounce(120),
        repository.settings.map { it.location.countryCode }.distinctUntilChanged(),
    ) { query, country -> query to country }
        .mapLatest { (query, country) -> if (query.isBlank()) cities.suggestions(country) else cities.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _events = MutableSharedFlow<LocationEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<LocationEvent> = _events.asSharedFlow()

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun choose(city: City) {
        viewModelScope.launch {
            location.chooseCity(city)
            _events.emit(LocationEvent.Done)
        }
    }

    fun useDeviceLocation() {
        if (_isLocating.value) return
        viewModelScope.launch {
            _isLocating.value = true
            try {
                _events.emit(
                    when (location.refresh()) {
                        LocationRepository.Result.UPDATED -> LocationEvent.Done
                        LocationRepository.Result.PERMISSION_MISSING -> LocationEvent.PermissionMissing
                        LocationRepository.Result.UNAVAILABLE -> LocationEvent.Unavailable
                    },
                )
            } finally {
                _isLocating.value = false
            }
        }
    }
}
