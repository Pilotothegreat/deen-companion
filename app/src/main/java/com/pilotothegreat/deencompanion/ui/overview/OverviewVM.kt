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

    private var hasCheckedDonationThisSession = false

    fun checkAndShowDonation(onShow: () -> Unit) {
        if (hasCheckedDonationThisSession) return
        hasCheckedDonationThisSession = true
        viewModelScope.launch {
            val count = appPreferenceRepo.appLaunchCount.first()
            val dismissed = appPreferenceRepo.donationPromptDismissed.first()
            val lastShow = appPreferenceRepo.lastDonationPromptShowTime.first()
            val now = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000

            if (count >= 5 && !dismissed && (now - lastShow >= sevenDaysInMillis)) {
                onShow()
            }
        }
    }

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation = _isRefreshingLocation.asStateFlow()

    private val _locationWarningDismissed = MutableStateFlow(false)
    val locationWarningDismissed = _locationWarningDismissed.asStateFlow()

    fun dismissLocationWarning() {
        _locationWarningDismissed.value = true
    }

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
    val tasbihDhikr = appPreferenceRepo.tasbihDhikr
    val tasbihTarget = appPreferenceRepo.tasbihTarget
    val tasbihHistory = appPreferenceRepo.tasbihHistory

    val fajrIqamaOffset = appPreferenceRepo.fajrIqamaOffset
    val dhuhrIqamaOffset = appPreferenceRepo.dhuhrIqamaOffset
    val asrIqamaOffset = appPreferenceRepo.asrIqamaOffset
    val maghribIqamaOffset = appPreferenceRepo.maghribIqamaOffset
    val ishaIqamaOffset = appPreferenceRepo.ishaIqamaOffset

    val fajrIqamaIsFixed = appPreferenceRepo.fajrIqamaIsFixed
    val dhuhrIqamaIsFixed = appPreferenceRepo.dhuhrIqamaIsFixed
    val asrIqamaIsFixed = appPreferenceRepo.asrIqamaIsFixed
    val maghribIqamaIsFixed = appPreferenceRepo.maghribIqamaIsFixed
    val ishaIqamaIsFixed = appPreferenceRepo.ishaIqamaIsFixed

    val fajrIqamaTime = appPreferenceRepo.fajrIqamaTime
    val dhuhrIqamaTime = appPreferenceRepo.dhuhrIqamaTime
    val asrIqamaTime = appPreferenceRepo.asrIqamaTime
    val maghribIqamaTime = appPreferenceRepo.maghribIqamaTime
    val ishaIqamaTime = appPreferenceRepo.ishaIqamaTime

    val lastPrayerTimeUpdate = appPreferenceRepo.lastPrayerTimeUpdate
    val hijriOffset = appPreferenceRepo.hijriOffset

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

        // Midnight scheduler to recalculate prayer times automatically overnight
        viewModelScope.launch {
            while (true) {
                try {
                    val tz = timezoneId.value
                    val zoneId = try { ZoneId.of(tz) } catch (e: Exception) { ZoneId.systemDefault() }
                    val now = java.time.ZonedDateTime.now(zoneId)
                    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                    val delayMs = java.time.Duration.between(now, nextMidnight).toMillis()
                    
                    // Add a 5-second buffer to ensure the date has fully rolled over
                    kotlinx.coroutines.delay(delayMs + 5000)
                    
                    val currentLat = latitude.first()
                    val currentLon = longitude.first()
                    recalculatePrayerTimes(currentLat, currentLon)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback delay in case of exception to avoid infinite busy loop
                    kotlinx.coroutines.delay(60000)
                }
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
            _locationWarningDismissed.value = false
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
            appPreferenceRepo.incrementTasbihCount()
        }
    }

    fun resetTasbih() {
        viewModelScope.launch {
            appPreferenceRepo.setTasbihCount(0)
        }
    }

    fun setTasbihDhikr(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setTasbihDhikr(value)
        }
    }

    fun setTasbihTarget(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setTasbihTarget(value)
        }
    }

    fun addTasbihHistoryItem(item: String) {
        viewModelScope.launch {
            appPreferenceRepo.addTasbihHistoryItem(item)
        }
    }

    fun clearTasbihHistory() {
        viewModelScope.launch {
            appPreferenceRepo.clearTasbihHistory()
        }
    }

    fun setHijriOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setHijriOffset(value)
        }
    }
}
