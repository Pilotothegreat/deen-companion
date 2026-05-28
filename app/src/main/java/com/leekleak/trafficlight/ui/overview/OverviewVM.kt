package com.leekleak.trafficlight.ui.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.services.IqamaAlarmManager
import com.leekleak.trafficlight.util.LocationHelper
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class OverviewVM(
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation = _isRefreshingLocation.asStateFlow()

    val calcMethod = appPreferenceRepo.calcMethod
    val asrSchool = appPreferenceRepo.asrSchool

    val cityName = appPreferenceRepo.cityName
    val latitude = appPreferenceRepo.latitude
    val longitude = appPreferenceRepo.longitude
    val timezoneId = appPreferenceRepo.timezoneId

    val tasbihCount = appPreferenceRepo.tasbihCount

    val fajrIqamaOffset = appPreferenceRepo.fajrIqamaOffset
    val dhuhrIqamaOffset = appPreferenceRepo.dhuhrIqamaOffset
    val asrIqamaOffset = appPreferenceRepo.asrIqamaOffset
    val maghribIqamaOffset = appPreferenceRepo.maghribIqamaOffset
    val ishaIqamaOffset = appPreferenceRepo.ishaIqamaOffset

    val prayerTimes = combine(
        latitude,
        longitude,
        timezoneId,
        calcMethod,
        asrSchool
    ) { lat, lon, tz, method, school ->
        val zoneId = try { ZoneId.of(tz) } catch (e: Exception) { ZoneId.systemDefault() }
        val date = LocalDate.now(zoneId)
        val zonedDateTime = date.atStartOfDay(zoneId)
        val offsetHours = zonedDateTime.offset.totalSeconds / 3600.0

        PrayerTimeCalculator.calculate(
            date = date,
            latitude = lat,
            longitude = lon,
            timezoneOffsetHours = offsetHours,
            method = method,
            asrSchool = school
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PrayerTimeCalculator.calculate(
            LocalDate.now(),
            21.3891,
            39.8579,
            3.0,
            PrayerTimeCalculator.CalculationMethod.MWL,
            PrayerTimeCalculator.AsrSchool.STANDARD
        )
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