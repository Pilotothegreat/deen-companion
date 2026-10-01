package com.pilotothegreat.deencompanion.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QiblaUi(
    val latitude: Double,
    val longitude: Double,
    val bearing: Double,
    val distanceKm: Double,
    val cityName: String?,
    val isDefaultLocation: Boolean,
)

class QiblaViewModel(
    settings: SettingsRepository,
    private val location: LocationRepository,
) : ViewModel() {

    val state: StateFlow<QiblaUi?> = settings.settings
        .map { s ->
            val lat = s.location.latitude
            val lon = s.location.longitude
            QiblaUi(lat, lon, QiblaMath.bearing(lat, lon), QiblaMath.distanceKm(lat, lon), s.location.cityName, s.location.isDefault)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _results = MutableSharedFlow<LocationRepository.Result>(extraBufferCapacity = 1)
    val refreshResults: SharedFlow<LocationRepository.Result> = _results.asSharedFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _results.emit(location.refresh())
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
