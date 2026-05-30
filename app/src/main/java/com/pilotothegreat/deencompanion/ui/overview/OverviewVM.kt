// FIXED: Record lastPrayerTimeUpdate in recalculatePrayerTimes
package com.pilotothegreat.deencompanion.ui.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.IqamaAlarmManager
import com.pilotothegreat.deencompanion.util.LocationHelper
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

import android.app.Application
import kotlinx.coroutines.flow.distinctUntilChanged

class OverviewVM(
    private val context: Context,
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation = _isRefreshingLocation.asStateFlow()

    val calcMethod = appPreferenceRepo.calcMethod.stateIn(
        viewModelScope, SharingStarted.Eagerly, PrayerTimeCalculator.CalculationMethod.MWL
    )
    val asrSchool = appPreferenceRepo.asrSchool.stateIn(
        viewModelScope, SharingStarted.Eagerly, PrayerTimeCalculator.AsrSchool.STANDARD
    )
    val timezoneId = appPreferenceRepo.timezoneId.stateIn(
        viewModelScope, SharingStarted.Eagerly, "Asia/Riyadh"
    )

    val cityName = appPreferenceRepo.cityName
    val latitude = appPreferenceRepo.latitude
    val longitude = appPreferenceRepo.longitude

    val tasbihCount = appPreferenceRepo.tasbihCount

    val fajrIqamaOffset = appPreferenceRepo.fajrIqamaOffset
    val dhuhrIqamaOffset = appPreferenceRepo.dhuhrIqamaOffset
    val asrIqamaOffset = appPreferenceRepo.asrIqamaOffset
    val maghribIqamaOffset = appPreferenceRepo.maghribIqamaOffset
    val ishaIqamaOffset = appPreferenceRepo.ishaIqamaOffset

    val lastPrayerTimeUpdate = appPreferenceRepo.lastPrayerTimeUpdate

    private val _prayerTimes = MutableStateFlow(
        PrayerTimeCalculator.calculate(
            LocalDate.now(),
            21.3891,
            39.8579,
            3.0,
            PrayerTimeCalculator.CalculationMethod.MWL,
            PrayerTimeCalculator.AsrSchool.STANDARD
        )
    )
    val prayerTimes = _prayerTimes.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    latitude,
                    longitude,
                    timezoneId,
                    calcMethod,
                    asrSchool
                ) { lat, lon, tz, method, school ->
                    CombinedState(lat, lon, tz, method, school)
                }
                .distinctUntilChanged()
                .collect { state ->
                    try {
                        recalculatePrayerTimes(state.lat, state.lon)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun recalculatePrayerTimes(lat: Double, lon: Double) {
        val tz = timezoneId.value
        val zoneId = try { ZoneId.of(tz) } catch (e: Exception) { ZoneId.systemDefault() }
        val date = LocalDate.now(zoneId)
        val zonedDateTime = date.atStartOfDay(zoneId)
        val offsetHours = zonedDateTime.offset.totalSeconds / 3600.0

        val times = PrayerTimeCalculator.calculate(
            date = date,
            latitude = lat,
            longitude = lon,
            timezoneOffsetHours = offsetHours,
            method = calcMethod.value,
            asrSchool = asrSchool.value
        )
        _prayerTimes.value = times

        // Reschedule alarms for new location/methods & record update timestamp
        viewModelScope.launch {
            try {
                appPreferenceRepo.setLastPrayerTimeUpdate(System.currentTimeMillis())
                IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private data class CombinedState(
        val lat: Double,
        val lon: Double,
        val tz: String,
        val method: PrayerTimeCalculator.CalculationMethod,
        val school: PrayerTimeCalculator.AsrSchool
    )

    fun refreshLocation(context: Context) {
        viewModelScope.launch {
            _isRefreshingLocation.value = true
            try {
                var loc = LocationHelper.getDeviceLocation(context)
                if (loc == null) {
                    loc = LocationHelper.fetchIpLocation()
                }

                if (loc != null) {
                    appPreferenceRepo.setLatitude(loc.latitude)
                    appPreferenceRepo.setLongitude(loc.longitude)
                    appPreferenceRepo.setCityName(loc.cityName)
                    appPreferenceRepo.setTimezoneId(loc.timezoneId)

                    IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshingLocation.value = false
            }
        }
    }

    fun incrementTasbih() {
        viewModelScope.launch {
            val current = tasbihCount.first()
            appPreferenceRepo.setTasbihCount(current + 1)
        }
    }

    fun resetTasbih() {
        viewModelScope.launch {
            appPreferenceRepo.setTasbihCount(0)
        }
    }
}
