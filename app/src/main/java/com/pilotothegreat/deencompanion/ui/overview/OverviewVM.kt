// FIXED: Record lastPrayerTimeUpdate in recalculatePrayerTimes
package com.pilotothegreat.deencompanion.ui.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.IqamaAlarmManager
import com.pilotothegreat.deencompanion.services.AdhanAlarmManager
import com.pilotothegreat.deencompanion.util.LocationHelper
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
        viewModelScope, SharingStarted.Eagerly, PrayerTimeCalculator.CalculationMethod.OMAN
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

    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount = _tasbihCount.asStateFlow()
    
    private val _tasbihDhikr = MutableStateFlow("سبحان الله")
    val tasbihDhikr = _tasbihDhikr.asStateFlow()

    val tasbihTarget = appPreferenceRepo.tasbihTarget.stateIn(
        viewModelScope, SharingStarted.Eagerly, 33
    )
    val tasbihHistory = appPreferenceRepo.tasbihHistory

    private var writeJob: Job? = null
    private var isDirty = false

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

    private val _prayerTimes = MutableStateFlow(
        PrayerTimeCalculator.calculate(
            LocalDate.now(),
            21.3891,
            39.8579,
            3.0,
            PrayerTimeCalculator.CalculationMethod.OMAN,
            PrayerTimeCalculator.AsrSchool.STANDARD
        )
    )
    val prayerTimes = _prayerTimes.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferenceRepo.tasbihCount.collect {
                if (!isDirty) {
                    _tasbihCount.value = it
                }
            }
        }
        viewModelScope.launch {
            appPreferenceRepo.tasbihDhikr.collect {
                if (!isDirty) {
                    _tasbihDhikr.value = it
                }
            }
        }

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
            val isRobolectric = try {
                Class.forName("org.robolectric.Robolectric") != null
            } catch (e: Exception) {
                false
            }
            if (isRobolectric) return@launch

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
                AdhanAlarmManager.scheduleAllAdhanAlarms(context, appPreferenceRepo)
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
                val appLang = appPreferenceRepo.appLanguage.first()
                val locale = Locale(appLang)
                var loc = LocationHelper.getDeviceLocation(context)
                if (loc == null) {
                    loc = LocationHelper.fetchIpLocation()
                }

                if (loc != null) {
                    // Try to resolve localized city name using Geocoder with app locale
                    var finalCityName = loc.cityName
                    try {
                        val geocoder = android.location.Geocoder(context, locale)
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val locality = address.locality ?: address.subAdminArea
                            val adminArea = address.adminArea
                            val country = address.countryName
                            finalCityName = when {
                                locality != null && country != null -> "$locality, $country"
                                locality != null -> locality
                                adminArea != null && country != null -> "$adminArea, $country"
                                country != null -> country
                                else -> finalCityName
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    appPreferenceRepo.setLatitude(loc.latitude)
                    appPreferenceRepo.setLongitude(loc.longitude)
                    appPreferenceRepo.setCityName(finalCityName)
                    appPreferenceRepo.setTimezoneId(loc.timezoneId)

                    AdhanAlarmManager.scheduleAllAdhanAlarms(context, appPreferenceRepo)
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
        isDirty = true
        val targetVal = tasbihTarget.value
        val currentDhikr = _tasbihDhikr.value
        val nextCount = _tasbihCount.value + 1
        
        if (nextCount >= targetVal) {
            _tasbihCount.value = 0
            val nextDhikr = when (currentDhikr) {
                "سبحان الله" -> "الحمد لله"
                "الحمد لله" -> "الله أكبر"
                else -> "سبحان الله"
            }
            _tasbihDhikr.value = nextDhikr
        } else {
            _tasbihCount.value = nextCount
        }

        writeJob?.cancel()
        writeJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                appPreferenceRepo.setTasbihCount(_tasbihCount.value)
                appPreferenceRepo.setTasbihDhikr(_tasbihDhikr.value)
            }
            isDirty = false
        }
    }

    fun resetTasbih() {
        writeJob?.cancel()
        _tasbihCount.value = 0
        isDirty = false
        viewModelScope.launch {
            appPreferenceRepo.setTasbihCount(0)
        }
    }

    fun setTasbihDhikr(value: String) {
        writeJob?.cancel()
        _tasbihDhikr.value = value
        isDirty = false
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


}
